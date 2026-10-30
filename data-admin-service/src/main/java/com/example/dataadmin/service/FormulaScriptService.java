package com.example.dataadmin.service;

import com.example.common.formula.FormulaScript;
import com.example.common.formula.LuaFormulaEngine;
import com.example.common.response.PageResult;
import com.example.dataadmin.mapper.FormulaScriptMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** 公式按编号唯一保存，覆盖导入和引用检查后的批量删除均使用事务。 */
@Service
public class FormulaScriptService {
    /** 上传包和解压内容最多八兆，避免压缩包无限展开。 */
    private static final int MAX_PACKAGE = 8 * 1024 * 1024;
    /** 单批最多一百个公式。 */
    private static final int MAX_FILES = 100;
    private final FormulaScriptMapper mapper;
    /** 删除成功后复用已有同步队列刷新运行快照。 */
    private final CollectInterfaceRuntimeSyncService runtimeSyncService;

    /** 注入已有MyBatis数据访问方式。 */
    public FormulaScriptService(FormulaScriptMapper mapper, CollectInterfaceRuntimeSyncService runtimeSyncService) {
        this.mapper = mapper;
        this.runtimeSyncService = runtimeSyncService;
    }

    /** 查询时不返回整份源码，源码通过导出接口取得。 */
    public PageResult<FormulaScript> page(String code, String name, Integer pageNum, Integer pageSize) {
        // 沿用参数配置分页规则，页码从1开始，每页最多200条。
        int number = pageNum == null ? 1 : pageNum;
        int size = pageSize == null ? 20 : pageSize;
        if (number < 1 || size < 1 || size > 200) {
            throw new IllegalArgumentException("页码必须大于零，每页数量必须为1到200");
        }
        String filterCode = normalize(code);
        String filterName = normalize(name);
        long total = mapper.countPage(filterCode, filterName);
        List<FormulaScript> scripts = total == 0 ? new ArrayList<>()
                : mapper.findPage(filterCode, filterName, size, (long) (number - 1) * size);
        return new PageResult<>(number, size, total, scripts);
    }

    /** 文件读取、编号去重和语法校验全部成功后，才覆盖整批公式。 */
    @Transactional(rollbackFor = Exception.class)
    public List<FormulaScript> importFile(String filename, byte[] content) {
        if (content == null || content.length == 0 || content.length > MAX_PACKAGE) {
            throw new IllegalArgumentException("导入文件不能为空且不能超过8MB");
        }
        List<FormulaScript> scripts = new ArrayList<>();
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".lua")) {
                scripts.add(parse(content));
            } else if (lower.endsWith(".zip")) {
                // 压缩包只读取内存，不将不可信路径写入文件系统。
                try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                    ZipEntry entry;
                    int total = 0;
                    int entries = 0;
                    while ((entry = zip.getNextEntry()) != null) {
                        if (++entries > 200) { throw new IllegalArgumentException("压缩包条目不能超过200个"); }
                        if (entry.isDirectory()) { continue; }
                        if (!entry.getName().toLowerCase(Locale.ROOT).endsWith(".lua")) {
                            throw new IllegalArgumentException("压缩包只能包含Lua文件：" + entry.getName());
                        }
                        byte[] source = readLimited(zip);
                        total += source.length;
                        if (total > MAX_PACKAGE || scripts.size() >= MAX_FILES) {
                            throw new IllegalArgumentException("解压内容超过8MB或公式超过100个");
                        }
                        scripts.add(parse(source));
                    }
                }
            } else {
                throw new IllegalArgumentException("只支持导入.lua或.zip文件");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("公式文件读取失败，请检查编码及压缩包格式", exception);
        }
        if (scripts.isEmpty()) { throw new IllegalArgumentException("未找到可导入的公式"); }
        Set<String> codes = new HashSet<>();
        for (FormulaScript script : scripts) {
            if (!codes.add(script.getCode())) {
                throw new IllegalArgumentException("同一批次公式编号重复：" + script.getCode());
            }
        }
        // 多实例导入及删除使用同一事务锁，避免批次交叉覆盖。
        mapper.lockManagement();
        for (FormulaScript script : scripts) {
            FormulaScript current = mapper.findByCode(script.getCode());
            if (current != null && current.getSource().equals(script.getSource())) {
                // 相同源码不重复更新，保留当前创建和修改时间。
                script.setCreateTime(current.getCreateTime());
                script.setUpdateTime(current.getUpdateTime());
            } else {
                // 同编号仅更新当前源码，首次导入时间保持不变。
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                script.setCreateTime(current == null ? now : current.getCreateTime());
                script.setUpdateTime(now);
                mapper.upsert(script);
            }
            script.setSource(null);
        }
        return scripts;
    }

    /** 单个公式始终读取当前唯一脚本。 */
    public FormulaScript get(String code) {
        String normalized = normalize(code);
        if (normalized == null) {
            throw new IllegalArgumentException("公式编号不能为空");
        }
        FormulaScript script = mapper.findByCode(normalized);
        if (script == null) { throw new IllegalArgumentException("未找到公式：" + normalized); }
        return script;
    }

    /** 任一公式不存在或仍被有效参数引用时整批回滚，不进行部分删除。 */
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(List<String> codes) {
        // 完整校验请求后再加锁，重复编号只计一次。
        if (codes == null || codes.isEmpty() || codes.size() > MAX_FILES) {
            throw new IllegalArgumentException("单次删除数量必须为1到100个");
        }
        Set<String> unique = new java.util.TreeSet<>();
        for (String code : codes) {
            String normalized = normalize(code);
            if (normalized == null || !normalized.matches("[0-9]{1,20}")) {
                throw new IllegalArgumentException("公式编号必须是1到20位数字");
            }
            unique.add(normalized);
        }
        List<String> requested = new ArrayList<>(unique);
        mapper.lockManagement();
        List<String> existing = mapper.findExistingCodes(requested);
        if (existing.size() != requested.size()) {
            unique.removeAll(existing);
            throw new IllegalArgumentException("以下公式不存在，整批未删除：" + String.join("、", unique));
        }
        // 检查期间阻止参数并发写入，保证检查与删除处于同一事务。
        mapper.lockParameterReferences();
        List<String> referenced = mapper.findReferencedCodes(requested);
        if (!referenced.isEmpty()) {
            throw new IllegalArgumentException("以下公式被参数配置引用，整批未删除：" + String.join("、", referenced));
        }
        int deleted = mapper.deleteByCodes(requested);
        if (deleted != requested.size()) {
            throw new IllegalArgumentException("公式记录发生变化，整批未删除，请刷新后重试");
        }
        // 提交后才通知后台刷新，已开始的帧仍完成原有快照。
        runtimeSyncService.syncAfterCommit();
        return deleted;
    }

    /** 手工输入原码完成试算，返回结果类型中文名称及精确数值文本。 */
    public java.util.Map<String, Object> preview(
            com.example.dataadmin.dto.processing.FormulaPreviewRequest request) {
        FormulaScript script = get(request.getCode());
        String hex = request.getRawHex().replaceAll("\\s+", "");
        if (hex.isEmpty() || (hex.length() & 1) != 0 || !hex.matches("[0-9a-fA-F]+")
                || hex.length() / 2 != (request.getBitWidth() + 7L) / 8L) {
            throw new IllegalArgumentException("原码必须是与位宽一致的完整十六进制字节");
        }
        byte[] raw = new byte[hex.length() / 2];
        for (int index = 0; index < raw.length; index++) {
            raw[index] = (byte) Integer.parseInt(hex.substring(index * 2, index * 2 + 2), 16);
        }
        // 非整字节字段必须和实际取位器一样，将末字节未使用的高位清零。
        int usedBits = request.getBitWidth() % 8;
        if (usedBits != 0 && ((raw[raw.length - 1] & 255) >>> usedBits) != 0) {
            throw new IllegalArgumentException("原码末字节未使用的高位必须为零");
        }
        LuaFormulaEngine.Result value = LuaFormulaEngine.execute(
                LuaFormulaEngine.compile(script.getSource()), raw, request.getBitWidth(), request.getProcessParam());
        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("code", script.getCode());
        // 查询展示复用协议枚举映射中文，不另建重复类型枚举。
        String label;
        switch (value.getType()) {
            case VALUE_TYPE_FLOAT: label = "单精度"; break;
            case VALUE_TYPE_DOUBLE: label = "双精度"; break;
            case VALUE_TYPE_TEXT: label = "文本"; break;
            case VALUE_TYPE_TIME: label = "时间"; break;
            default: label = "原码"; break;
        }
        response.put("valueType", label);
        response.put("value", value.getValue() == null ? null
                : value.getType() == SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType.VALUE_TYPE_FLOAT
                ? (double) value.getValue().floatValue() : value.getValue().doubleValue());
        response.put("valueText", value.getText() != null ? value.getText()
                : value.getValue() == null ? null : value.getValue().toPlainString());
        return response;
    }

    /** 批量导出当前脚本，不指定编号时有界读取全部公式。 */
    public byte[] exportZip(List<String> codes) {
        List<FormulaScript> scripts = new ArrayList<>();
        if (codes == null || codes.isEmpty()) {
            scripts = mapper.findForExport(MAX_FILES + 1);
        } else {
            if (codes.size() > MAX_FILES) { throw new IllegalArgumentException("单次最多导出100个公式"); }
            Set<String> unique = new HashSet<>();
            for (String code : codes) {
                FormulaScript script = get(code);
                if (unique.add(script.getCode())) { scripts.add(script); }
            }
        }
        if (scripts.isEmpty() || scripts.size() > MAX_FILES) {
            throw new IllegalArgumentException("导出公式数量必须为1到100个");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                for (FormulaScript script : scripts) {
                    // 文件名只使用受控编号，避免名称形成非法路径。
                    zip.putNextEntry(new ZipEntry(script.getCode() + ".lua"));
                    zip.write(script.getSource().getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("公式导出失败", exception);
        }
    }

    /** 严格UTF-8解码，允许文本编辑器添加的文件头标记。 */
    private FormulaScript parse(byte[] bytes) throws IOException {
        if (bytes.length > LuaFormulaEngine.MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("单个公式不能超过64KB");
        }
        String source = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        if (source.startsWith("\uFEFF")) { source = source.substring(1); }
        LuaFormulaEngine.Compiled compiled = LuaFormulaEngine.compile(source);
        FormulaScript script = new FormulaScript();
        script.setCode(compiled.getCode());
        script.setName(compiled.getName());
        script.setSource(source);
        return script;
    }

    /** 在解压过程中限制单文件大小，不信任压缩包声明的长度。 */
    private byte[] readLimited(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = zip.read(buffer)) != -1) {
            if (output.size() + count > LuaFormulaEngine.MAX_SOURCE_BYTES) {
                throw new IllegalArgumentException("单个公式解压后不能超过64KB");
            }
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    /** 空字符串统一表示未指定筛选编号。 */
    private String normalize(String code) {
        return code == null || code.trim().isEmpty() ? null : code.trim();
    }
}
