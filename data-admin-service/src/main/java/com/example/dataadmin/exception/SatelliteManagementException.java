package com.example.dataadmin.exception;

/** 卫星目录管理远程调用异常。 */
public class SatelliteManagementException extends RuntimeException {

    /** 接口响应使用的状态码。 */
    private final int responseCode;

    public SatelliteManagementException(int responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    public SatelliteManagementException(
            int responseCode,
            String message,
            Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
