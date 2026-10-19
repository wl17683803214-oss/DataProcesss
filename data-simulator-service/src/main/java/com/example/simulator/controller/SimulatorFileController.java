package com.example.simulator.controller;

import com.example.common.response.ApiResponse;
import com.example.simulator.model.UploadedFile;
import com.example.simulator.service.SimulatorFileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** 上传模拟源数据文件，创建与修改仍沿用原有JSON接口。 */
@RestController
@RequestMapping("/simulator/files")
public class SimulatorFileController {

    /** 文件校验与持久化服务。 */
    private final SimulatorFileService files;

    public SimulatorFileController(SimulatorFileService files) {
        this.files = files;
    }

    /** 接收浏览器上传的单个文件，保存成功后返回模拟源配置所需信息。 */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadedFile> upload(@RequestPart("file") MultipartFile file)
            throws IOException {
        // 复用统一响应结构，只有保存完整的文件才交给前端继续创建模拟源。
        return ApiResponse.success(files.upload(file));
    }
}
