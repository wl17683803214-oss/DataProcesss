package com.example.dataprocess.service;

import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;

import java.nio.file.Path;
import java.time.LocalDateTime;

/** FEP文件接收完成后的结果处理服务。 */
public interface FepFileResultService {

    /** 查询文件是否已经接收，并唤醒尚未完成的后续处理。 */
    boolean isReceivedAndTriggerRetry(
            CollectInterfaceRuntimeConfig config,
            String fileName,
            long fileLength);

    /** 登记完整本地文件并提交对象上传和消息发布任务。 */
    void recordAndSubmit(
            CollectInterfaceRuntimeConfig config,
            Path completedFile,
            LocalDateTime completedTime) throws Exception;
}
