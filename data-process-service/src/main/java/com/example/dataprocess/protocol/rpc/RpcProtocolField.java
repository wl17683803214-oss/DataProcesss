package com.example.dataprocess.protocol.rpc;

/** RPC协议配置中的单个字段。 */
public class RpcProtocolField {

    /** FrameRequest字段名。 */
    private String field;
    /** 字段中文名称。 */
    private String fieldName;
    /** 字段数据类型。 */
    private String dataType;
    /** 字段配置值，允许接收JSON中的字符串、数字和布尔值。 */
    private Object value;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
