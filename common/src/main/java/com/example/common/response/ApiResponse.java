package com.example.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
/** 所有服务统一使用的接口响应结构。 */
public class ApiResponse<T> {

    private Integer code;

    private String message;

    private T data;

    public ApiResponse() {
    }

    private ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(200, "操作成功", data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<T>(200, "操作成功", null);
    }

    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<T>(code, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
