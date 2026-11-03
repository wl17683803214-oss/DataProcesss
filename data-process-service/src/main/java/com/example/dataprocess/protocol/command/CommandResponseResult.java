package com.example.dataprocess.protocol.command;

import SatDataCenter.DataExchange.TmTc.TelecommandMessages.CommandExecResult;

/** 协议结果码及中文含义；协议数值与现有消息枚举数值分别维护。 */
public enum CommandResponseResult {

    /** 协议零表示成功，对应消息枚举一。 */
    SUCCESS("0", "success", "成功", CommandExecResult.CM_SENDING_RESULT_SUCCESS),
    /** 协议一表示失败，对应消息枚举二。 */
    FAILURE("1", "failure", "失败", CommandExecResult.CM_SENDING_RESULT_FAIL),
    /** 超时按业务约定写入未指定，不额外填写消息详情。 */
    TIMEOUT("2", "timeout", "超时", CommandExecResult.CM_SENDING_RESULT_UNSPECIFIED);

    /** 转换成字符串后的协议结果码。 */
    private final String protocolCode;
    /** 业务枚举英文值。 */
    private final String value;
    /** 日志展示使用的中文名称。 */
    private final String label;
    /** 已有消息枚举，避免重复定义消息协议。 */
    private final CommandExecResult protoResult;

    /** 保存协议码、业务值、中文名称及消息枚举的对应关系。 */
    CommandResponseResult(String protocolCode, String value, String label,
            CommandExecResult protoResult) {
        this.protocolCode = protocolCode;
        this.value = value;
        this.label = label;
        this.protoResult = protoResult;
    }

    /** 读取英文业务值。 */
    public String getValue() {
        return value;
    }

    /** 读取中文展示名称。 */
    public String getLabel() {
        return label;
    }

    /** 读取用于组装消息的已有枚举。 */
    public CommandExecResult getProtoResult() {
        return protoResult;
    }

    /** 将协议结果字符串映射到明确的业务含义，拒绝未知结果。 */
    public static CommandResponseResult fromProtocolCode(String protocolCode) {
        // 逐项匹配协议码，避免把协议数值直接当成消息枚举数值。
        for (CommandResponseResult result : values()) {
            if (result.protocolCode.equals(protocolCode)) {
                return result;
            }
        }
        throw new IllegalArgumentException("指令响应结果码不正确：" + protocolCode);
    }

    /** 从组装后的消息结果恢复中文含义，未指定按本协议表示超时。 */
    public static CommandResponseResult fromProtoResult(CommandExecResult protoResult) {
        // 使用已有映射生成日志名称，保证日志与消息组装规则一致。
        for (CommandResponseResult result : values()) {
            if (result.protoResult == protoResult) {
                return result;
            }
        }
        throw new IllegalArgumentException("指令响应消息结果不正确");
    }
}
