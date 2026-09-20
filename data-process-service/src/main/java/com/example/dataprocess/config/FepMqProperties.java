package com.example.dataprocess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 文件完成消息发送配置。 */
@Component
@ConfigurationProperties(prefix = "data-processing.mq")
public class FepMqProperties {

    /** 文件完成消息主题。 */
    private String fileTopic;
    /** 文件完成消息标签。 */
    private String fileTag;

    public String getFileTopic() {
        return fileTopic;
    }

    public void setFileTopic(String fileTopic) {
        this.fileTopic = fileTopic;
    }

    public String getFileTag() {
        return fileTag;
    }

    public void setFileTag(String fileTag) {
        this.fileTag = fileTag;
    }
}
