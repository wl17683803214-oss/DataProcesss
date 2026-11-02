package com.example.dataadmin.controller;

import com.example.common.formula.FormulaScript;
import com.example.common.response.ApiResponse;
import com.example.common.response.PageResult;
import com.example.dataadmin.dto.processing.FormulaBatchDeleteRequest;
import com.example.dataadmin.service.FormulaScriptService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 公式编号在全系统共享，不设置启用或停用状态。 */
@RestController
@RequestMapping("/processing/formulas")
public class FormulaScriptController {
    private final FormulaScriptService service;

    /** 注入公式管理服务。 */
    public FormulaScriptController(FormulaScriptService service) {
        this.service = service;
    }

    /** 按编号、名称分页查询公式，不返回源码，不保留历史版本。 */
    @GetMapping
    public ApiResponse<PageResult<FormulaScript>> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(service.page(code, name, pageNum, pageSize));
    }

    /** 按编号批量删除无引用公式，任一项无效时整批不删除。 */
    @PostMapping("/batch-delete")
    public ApiResponse<Integer> deleteBatch(
            @javax.validation.Valid @org.springframework.web.bind.annotation.RequestBody
            FormulaBatchDeleteRequest request) {
        return ApiResponse.success(service.deleteBatch(request.getCodes()));
    }

    /** 支持单脚本和多脚本压缩包，任一文件失败则整批回滚。 */
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ApiResponse<List<FormulaScript>> importFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 8 * 1024 * 1024) {
            throw new IllegalArgumentException("请选择不超过8MB的Lua文件或压缩包");
        }
        try {
            return ApiResponse.success(service.importFile(file.getOriginalFilename(), file.getBytes()));
        } catch (IOException exception) {
            throw new IllegalArgumentException("读取公式文件失败", exception);
        }
    }

    /** 按编号导出当前唯一脚本。 */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String code) {
        FormulaScript script = service.get(code);
        return download(script.getCode() + ".lua",
                script.getSource().getBytes(StandardCharsets.UTF_8), "text/plain;charset=UTF-8");
    }

    /** 按编号列表导出压缩包，不指定时导出全部当前公式。 */
    @GetMapping("/export-zip")
    public ResponseEntity<byte[]> exportZip(@RequestParam(required = false) List<String> codes) {
        return download("formulas.zip", service.exportZip(codes), "application/zip");
    }

    /** 下载响应统一使用附件，导出的文件可直接再次导入。 */
    private ResponseEntity<byte[]> download(String filename, byte[] bytes, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }
}
