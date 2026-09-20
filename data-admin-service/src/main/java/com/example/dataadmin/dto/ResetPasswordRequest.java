package com.example.dataadmin.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 管理员重置用户密码请求参数。
 */
public class ResetPasswordRequest {

    /** 用户主键。 */
    @NotNull(message = "用户主键不能为空")
    /** 需要重置密码的用户 ID。 */
    private Long id;

    /** 新密码。 */
    @NotBlank(message = "新密码不能为空")
    /** 重置后的明文密码，服务端会加密保存。 */
    private String password;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
