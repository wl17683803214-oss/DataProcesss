package com.example.simulator.model;

/** 上传完成后交给模拟源创建或编辑表单使用的文件信息。 */
public class UploadedFile {

    /** 保留用户上传时的原文件名。 */
    private final String fileName;
    /** 实际保存的文件大小，单位为字节。 */
    private final long fileSize;
    /** 创建或修改模拟源时需要原样提交的服务器文件路径。 */
    private final String filePath;

    /** 保存一次成功上传的完整结果。 */
    public UploadedFile(String fileName, long fileSize, String filePath) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.filePath = filePath;
    }

    /** 获取用于页面展示的原文件名。 */
    public String getFileName() {
        return fileName;
    }

    /** 获取实际保存的字节数。 */
    public long getFileSize() {
        return fileSize;
    }

    /** 获取创建或编辑时复用的文件路径。 */
    public String getFilePath() {
        return filePath;
    }
}
