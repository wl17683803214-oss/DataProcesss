package com.example.simulator.service;

import com.example.simulator.model.UploadedFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** 模拟源上传文件的校验与持久化服务。 */
public interface SimulatorFileService {

    /** 保存上传文件并返回现有模拟源配置可以直接使用的路径。 */
    UploadedFile upload(MultipartFile file) throws IOException;
}
