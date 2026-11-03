package com.example.dataadmin.service.impl;

import com.example.dataadmin.config.SatelliteManagementRpcProperties;
import com.example.dataadmin.dto.satellite.SatelliteEnabledRequest;
import com.example.dataadmin.dto.satellite.SatelliteInfoRequest;
import com.example.dataadmin.dto.satellite.SatelliteUpdateRequest;
import com.example.dataadmin.entity.SatelliteConfig;
import com.example.dataadmin.enums.BusinessEnums.Enabled;
import com.example.dataadmin.exception.SatelliteManagementException;
import com.example.dataadmin.grpc.SatelliteManagementGrpcClient;
import com.example.dataadmin.mapper.SatelliteConfigMapper;
import com.example.dataadmin.service.SatelliteManagementService;
import com.example.dataadmin.storage.SatelliteTableStorage;
import com.example.dataadmin.vo.satellite.SatelliteInfoVO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tm.processing.v1.Tm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** 本地档案使用事务管理，远程失败时不提交本地配置。 */
@Service
public class SatelliteManagementServiceImpl implements SatelliteManagementService {
    /** 本地卫星档案映射。 */
    private final SatelliteConfigMapper mapper;
    /** 已有远程客户端，统一复用连接、超时及异常处理。 */
    private final SatelliteManagementGrpcClient grpcClient;
    /** 参数表文件保存及路径处理。 */
    private final SatelliteTableStorage storage;
    /** 复用项目已有的配置解析器。 */
    private final ObjectMapper objectMapper;
    /** 配置文件允许的最大字节数。 */
    private final int maxUploadFileSize;

    /** 注入本地持久化及已有远程能力。 */
    public SatelliteManagementServiceImpl(SatelliteConfigMapper mapper,
            SatelliteManagementGrpcClient grpcClient, SatelliteTableStorage storage,
            ObjectMapper objectMapper, SatelliteManagementRpcProperties properties) {
        this.mapper = mapper;
        this.grpcClient = grpcClient;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.maxUploadFileSize = properties.getMaxUploadFileSize();
    }

    @Override
    public List<SatelliteInfoVO> listSatellites(String name, String code, String enable) {
        // 第一步：只在传入状态筛选时校验英文枚举值。
        String status = normalize(enable);
        if (status != null) {
            Enabled.fromStorageValue(status);
        }
        // 第二步：列表查询不加载配置原文，逐项转换中文状态。
        List<SatelliteInfoVO> result = new ArrayList<>();
        for (SatelliteConfig satellite : mapper.findList(normalize(name), normalize(code), status)) {
            result.add(toVO(satellite));
        }
        return result;
    }

    @Override
    public SatelliteInfoVO getSatellite(Long id) {
        // 详情始终按主键定位，重复卫星代号不会选错记录。
        return toVO(requireSatellite(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSatellite(SatelliteInfoRequest request) {
        // 第一步：校验基础字段，参数表存在时解析为本地绝对路径。
        SatelliteConfig satellite = fromRequest(request);
        satellite.setTablePath(normalize(request.getTablePath()) == null
                ? null : storage.existingPath(request.getTablePath()));
        // 第二步：先取得事务锁并插入未提交记录，确保本地数据能写入再调用远程服务。
        mapper.lockManagement();
        mapper.insert(satellite);
        // 第三步：配置参数表时立即调用远程服务；默认禁用的空档案仅在本地建立。
        if (satellite.getTablePath() != null) {
            sendTable(satellite);
        }
        if (satellite.getTablePath() != null || isEnabled(satellite)) {
            sendEnabled(satellite);
        }
        return satellite.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSatellite(SatelliteUpdateRequest request) {
        // 第一步：锁定管理事务，按主键读取当前档案并保留配置原文。
        mapper.lockManagement();
        SatelliteConfig current = requireSatellite(request.getId());
        SatelliteConfig changed = fromRequest(request);
        // 编辑未提交状态时保留原值，避免仅改名称却意外禁用卫星。
        if (request.getEnable() == null) {
            changed.setEnable(current.getEnable());
        }
        changed.setId(current.getId());
        changed.setJsonContent(current.getJsonContent());
        String path = request.getTablePath() == null ? current.getTablePath() : normalize(request.getTablePath());
        changed.setTablePath(path);
        // 第二步：名称和代号均可修改，按新代号重新下发已有配置。
        boolean identityChanged = !current.getCode().equals(changed.getCode());
        boolean jsonChanged = current.getJsonContent() != null
                && (identityChanged || !current.getName().equals(changed.getName()));
        boolean tableChanged = path != null
                && (identityChanged || jsonChanged || !Objects.equals(path, current.getTablePath()));
        if (tableChanged) {
            changed.setTablePath(storage.existingPath(path));
        }
        // 所有本地文件检查完成后才开始远程下发，避免无效路径引发部分远程修改。
        if (jsonChanged) {
            sendJson(changed);
        }
        if (tableChanged) {
            sendTable(changed);
        }
        // 第三步：配置下发后恢复目标启用状态，远程任一步失败均回滚本地修改。
        if (jsonChanged || tableChanged || !current.getEnable().equals(changed.getEnable())
                || (identityChanged && isEnabled(changed))) {
            sendEnabled(changed);
        }
        requireUpdated(mapper.update(changed));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSatellites(List<Long> ids) {
        // 第一步：只合并重复提交的同一主键，不对卫星名称及代号做去重。
        if (ids == null || ids.isEmpty() || ids.contains(null)) {
            throw new IllegalArgumentException("请选择要删除的卫星");
        }
        Set<Long> uniqueIds = new TreeSet<>(ids);
        List<Long> selected = new ArrayList<>(uniqueIds);
        // 第二步：在同一事务锁内检查所有记录存在，避免部分删除。
        mapper.lockManagement();
        List<Long> existing = mapper.findIds(selected);
        if (existing.size() != selected.size()) {
            uniqueIds.removeAll(existing);
            throw new IllegalArgumentException("以下卫星记录不存在，整批未删除：" + uniqueIds);
        }
        // 第三步：只删除本地档案，保留远程可能仍引用的共享参数表文件。
        int deleted = mapper.deleteByIds(selected);
        if (deleted != selected.size()) {
            throw new IllegalStateException("卫星记录发生变化，整批未删除，请刷新重试");
        }
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setSatelliteEnabled(SatelliteEnabledRequest request) {
        // 第一步：从同一条本地档案提取远程代号，防止重复代号导致本地批量误改。
        Enabled status = Enabled.fromStorageValue(request.getEnable());
        mapper.lockManagement();
        SatelliteConfig satellite = requireSatellite(request.getId());
        satellite.setEnable(status.getStorageValue());
        // 第二步：立即下发远程状态，成功后才提交本地状态。
        sendEnabled(satellite);
        requireUpdated(mapper.update(satellite));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadTable(Long id, String tablePath) {
        // 第一步：校验实际文件存在，使用本地主键读取当前卫星。
        String localPath = storage.existingPath(tablePath);
        mapper.lockManagement();
        SatelliteConfig satellite = requireSatellite(id);
        satellite.setTablePath(localPath);
        // 第二步：只传本地文件路径及文件名，下发成功后保存本地档案。
        sendTable(satellite);
        sendEnabled(satellite);
        requireUpdated(mapper.update(satellite));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadJson(Long id, String fileName, byte[] content) {
        // 第一步：校验文件编码及配置语法，保留原有文本格式。
        String json = readJson(fileName, content);
        mapper.lockManagement();
        SatelliteConfig satellite = requireSatellite(id);
        satellite.setJsonContent(json);
        // 参数表存在时先确认仍可读取，避免配置已上传才发现本地文件失效。
        if (satellite.getTablePath() != null) {
            satellite.setTablePath(storage.existingPath(satellite.getTablePath()));
        }
        // 第二步：立即下发完整配置；存在本地参数表时再次下发本地路径。
        sendJson(satellite);
        if (satellite.getTablePath() != null) {
            sendTable(satellite);
        }
        sendEnabled(satellite);
        // 第三步：远程成功后保存配置原文，不拆分遥测通道等字段。
        requireUpdated(mapper.update(satellite));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean synchronizeBaselineSatellite(String name, String code, String tablePath) {
        // 第一步：只要求名称及代号完整，可选参数表缺失时保留现有文件地址。
        String normalizedName = requireText(name, "基线卫星名称不能为空");
        String normalizedCode = requireText(code, "基线卫星代号不能为空");
        String localPath = normalize(tablePath);
        if (localPath != null) {
            localPath = storage.existingPath(localPath);
        }
        // 第二步：复用管理事务锁，在锁内按代号重新检查，避免并发同步重复新增。
        mapper.lockManagement();
        long existingCount = mapper.countByCode(normalizedCode);
        if (existingCount == 0) {
            // 第三步：没有匹配记录时只新增本地档案，状态默认禁用，不调用任何远程接口。
            SatelliteConfig satellite = new SatelliteConfig();
            satellite.setName(normalizedName);
            satellite.setCode(normalizedCode);
            satellite.setEnable(Enabled.DISABLED.getStorageValue());
            satellite.setTablePath(localPath);
            requireUpdated(mapper.insert(satellite));
            return true;
        }
        // 第四步：基线覆盖全部同代号档案的已提供字段，保留各自主键、状态及配置原文。
        int updated = mapper.updateBaselineByCode(normalizedName, normalizedCode, localPath);
        if (updated != existingCount) {
            throw new IllegalStateException("卫星档案在同步期间发生变化，本条覆盖已回滚，请重试");
        }
        return false;
    }

    /** 将请求转换为英文状态的本地实体。 */
    private SatelliteConfig fromRequest(SatelliteInfoRequest request) {
        // 仅校验必填字段及枚举合法性，不附加名称或代号唯一限制。
        if (request == null) {
            throw new IllegalArgumentException("卫星信息不能为空");
        }
        SatelliteConfig satellite = new SatelliteConfig();
        satellite.setName(requireText(request.getName(), "卫星名称不能为空"));
        satellite.setCode(requireText(request.getCode(), "卫星代号不能为空"));
        satellite.setEnable(request.getEnable() == null ? Enabled.DISABLED.getStorageValue()
                : Enabled.fromStorageValue(request.getEnable()).getStorageValue());
        return satellite;
    }

    /** 转换中文状态，保留英文值用于前端编辑回填。 */
    private SatelliteInfoVO toVO(SatelliteConfig satellite) {
        // 复用已有属性复制能力，列表不加载配置原文。
        SatelliteInfoVO result = new SatelliteInfoVO();
        BeanUtils.copyProperties(satellite, result);
        result.setEnable(Enabled.fromStorageValue(satellite.getEnable()).getLabel());
        result.setEnableValue(satellite.getEnable());
        return result;
    }

    /** 按本地主键取得档案，不使用卫星代号查询单条记录。 */
    private SatelliteConfig requireSatellite(Long id) {
        // 缺少主键与记录不存在分别使用清晰的中文提示。
        if (id == null) {
            throw new IllegalArgumentException("卫星主键不能为空");
        }
        SatelliteConfig satellite = mapper.findById(id);
        if (satellite == null) {
            throw new SatelliteManagementException(404, "未找到指定的卫星档案");
        }
        return satellite;
    }

    /** 上传参数表配置，路径为我们实际保存的文件绝对路径。 */
    private void sendTable(SatelliteConfig satellite) {
        grpcClient.uploadTable(Tm.UploadTableRequest.newBuilder()
                .setSatCode(satellite.getCode())
                .setTablePath(satellite.getTablePath())
                .setFileName(storage.fileName(satellite.getTablePath()))
                .build());
    }

    /** 上传配置原文，卫星名称及代号来自选中的本地档案。 */
    private void sendJson(SatelliteConfig satellite) {
        grpcClient.uploadJson(Tm.UploadJsonRequest.newBuilder()
                .setSatCode(satellite.getCode())
                .setSatName(satellite.getName())
                .setJsonContent(satellite.getJsonContent())
                .build());
    }

    /** 将英文枚举状态转换为远程协议中的布尔开关。 */
    private void sendEnabled(SatelliteConfig satellite) {
        grpcClient.setSatelliteEnabled(Tm.SetSatEnabledRequest.newBuilder()
                .setCode(satellite.getCode()).setEnable(isEnabled(satellite)).build());
    }

    /** 复用已有启用枚举，不单独声明卫星状态枚举。 */
    private boolean isEnabled(SatelliteConfig satellite) {
        return Enabled.ENABLED.getStorageValue().equals(satellite.getEnable());
    }

    /** 严格读取配置文本，避免无效编码替换字符后污染原文。 */
    private String readJson(String fileName, byte[] content) {
        // 第一步：检查文件存在及实际大小，资源限制沿用已有配置。
        requireText(fileName, "请选择卫星配置文件");
        if (content == null || content.length == 0 || content.length > maxUploadFileSize) {
            throw new IllegalArgumentException("配置文件不能为空或超过配置的文件大小");
        }
        String text;
        try {
            // 第二步：支持带字节顺序标记的文件，配置内容保持原有缩进与换行。
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content)).toString();
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("卫星配置文件必须使用UTF-8编码", exception);
        }
        try {
            // 第三步：只检查完整配置语法，不限制其中业务字段及重复的卫星代号。
            JsonNode parsed = objectMapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(text);
            if (parsed == null || parsed.isMissingNode()) {
                throw new IllegalArgumentException("卫星配置文件内容不能为空");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("卫星配置文件不是有效JSON", exception);
        }
        return text;
    }

    /** 修改影响行数必须为一，防止并发删除后静默成功。 */
    private void requireUpdated(int count) {
        if (count != 1) {
            throw new IllegalStateException("卫星档案已变化，请刷新重试");
        }
    }

    /** 获取必填文本，不增加业务唯一性限制。 */
    private String requireText(String value, String message) {
        String text = normalize(value);
        if (text == null) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }

    /** 可选文本的空串统一转为空值。 */
    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
