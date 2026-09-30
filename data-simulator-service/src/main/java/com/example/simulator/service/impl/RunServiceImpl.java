package com.example.simulator.service.impl;

import com.example.simulator.enums.ProtocolProfile;
import com.example.simulator.enums.RunStatus;
import com.example.simulator.config.SimulatorProperties;
import com.example.simulator.service.RunService;
import com.example.simulator.service.SourceService;
import com.example.simulator.model.*;
import com.example.simulator.mapper.SimulatorMapper;
import com.example.simulator.protocol.PdxpEncoder;
import com.example.common.protocol.fep.FepProtocolTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** 启动前的短事务，提交后才执行网络发送。 */
@Service
public class RunServiceImpl implements RunService {
    private final SourceService sources;
    private final SimulatorMapper mapper;
    private final SimulatorProperties properties;
    private final ObjectMapper json;

    public RunServiceImpl(SourceService sources, SimulatorMapper mapper,
                          SimulatorProperties properties, ObjectMapper json) {
        // 通过服务接口及公共组件准备执行，不依赖其他服务实现类。
        this.sources = sources;
        this.mapper = mapper;
        this.properties = properties;
        this.json = json;
    }

    /** 原子检查配置及活动记录，创建不可变执行快照。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreparedRun prepare(long sourceId) throws Exception {
        // 在当前事务中锁定配置并检查服务器文件是否可读。
        SourceConfig config = sources.requireEditable(sourceId);
        Path path = Paths.get(config.filePath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("源文件不存在或不可读");
        }
        config.filePath = path.toString();
        long length = Files.size(path);
        // 按协议校验帧长度或文件交换约束。
        ProtocolProfile profile = ProtocolProfile.of(config.transferProtocol);
        if (profile == ProtocolProfile.PDXP) {
            config.pdxp = mapper.findPdxp(sourceId);
            if (config.pdxp == null) {
                throw new IllegalArgumentException("PDXP参数缺失");
            }
            PdxpEncoder.validate(config.pdxp);
            if (length == 0 || length % config.pdxp.inputFrameLength != 0) {
                throw new IllegalArgumentException("PDXP文件必须非空且大小为配置帧长度的整数倍");
            }
        } else {
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("FEP文件超过协议长度上限");
            }
            FepProtocolTool.buildSendRequest(path.getFileName().toString(), (int) length);
        }
        // 公共配置只在启动时复制，运行中不重新加载配置文件。
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("source", config);
        if (profile == ProtocolProfile.FEP) {
            Map<String, Object> fep = new LinkedHashMap<>();
            fep.put("dataUnitLength", properties.getDataUnitLength());
            fep.put("connectTimeoutMillis", properties.getConnectTimeoutMillis());
            fep.put("responseTimeoutMillis", properties.getResponseTimeoutMillis());
            snapshot.put("fep", fep);
        }
        // 保存启动记录，将配置与记录交给事务提交后的发送流程。
        RunRecord run = new RunRecord();
        run.sourceId = sourceId;
        run.statusCode = RunStatus.STARTING.code;
        run.configSnapshot = json.writeValueAsString(snapshot);
        mapper.insertRun(run);
        return new PreparedRun(config, run);
    }

}

