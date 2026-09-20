package com.example.dataprocess.dto;

/** 文件接收完成后发送到消息队列的数据。 */
public class FepFileMessage {

    /** 需求编号。 */
    private String requirementId = "";
    /** 需求名称。 */
    private String requirementName = "";
    /** 需求描述。 */
    private String requirementDesc = "";
    /** 需求版本。 */
    private String version = "";
    /** 对象存储中的文件访问地址。 */
    private String fileUrl;
    /** 小写文件后缀，不包含点号。 */
    private String fileType;
    /** 文件关联的基础信息。 */
    private BaseInfo baseInfo;

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getRequirementName() {
        return requirementName;
    }

    public void setRequirementName(String requirementName) {
        this.requirementName = requirementName;
    }

    public String getRequirementDesc() {
        return requirementDesc;
    }

    public void setRequirementDesc(String requirementDesc) {
        this.requirementDesc = requirementDesc;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public BaseInfo getBaseInfo() {
        return baseInfo;
    }

    public void setBaseInfo(BaseInfo baseInfo) {
        this.baseInfo = baseInfo;
    }

    /** 文件关联的发送和接收信息。 */
    public static class BaseInfo {

        /** 业务名称。 */
        private String businessName = "";
        /** 发送方编号。 */
        private String senderId = "";
        /** 发送方名称。 */
        private String senderName = "";
        /** 接收方编号。 */
        private String receiverId = "";
        /** 接收方名称。 */
        private String receiverName = "";
        /** 文件接收完成时间。 */
        private String time;

        public String getBusinessName() {
            return businessName;
        }

        public void setBusinessName(String businessName) {
            this.businessName = businessName;
        }

        public String getSenderId() {
            return senderId;
        }

        public void setSenderId(String senderId) {
            this.senderId = senderId;
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }

        public String getReceiverId() {
            return receiverId;
        }

        public void setReceiverId(String receiverId) {
            this.receiverId = receiverId;
        }

        public String getReceiverName() {
            return receiverName;
        }

        public void setReceiverName(String receiverName) {
            this.receiverName = receiverName;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }
}
