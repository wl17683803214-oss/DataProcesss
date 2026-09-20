package com.example.dataadmin.service.impl;

import com.example.common.tool.HttpRequestTool;
import com.example.dataadmin.config.BaselineSsoProperties;
import com.example.dataadmin.dto.auth.BaselineIdentity;
import com.example.dataadmin.dto.auth.SsoLoginRequest;
import com.example.dataadmin.service.BaselineSsoClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/** 基线平台统一登录HTTP客户端实现。 */
@Service
public class BaselineSsoClientImpl implements BaselineSsoClient {

    private static final String TOKEN_VALIDATE_PATH =
            "/rpc-api/system/open/token/validate";
    private static final String USER_PAGE_PATH =
            "/rpc-api/system/open/user/page";
    private static final String ROLE_PAGE_PATH =
            "/rpc-api/system/open/role/page";

    private final BaselineSsoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpRequestTool httpRequestTool;

    public BaselineSsoClientImpl(
            BaselineSsoProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        // 统一复用公共HTTP工具，并应用基线平台独立的超时配置。
        this.httpRequestTool = new HttpRequestTool(
                properties.getConnectTimeoutMillis(),
                properties.getReadTimeoutMillis());
    }

    /** 依次完成令牌校验、用户定位和角色筛选。 */
    @Override
    public BaselineIdentity authenticate(SsoLoginRequest request) {
        validateConfiguration();
        validateRequestClient(request);
        validateToken(request);

        // 令牌通过平台校验后才读取身份字段，解码结果本身不作为可信校验依据。
        TokenClaims claims = decodeClaims(request.getTokenId());
        if (hasText(claims.clientId)
                && !properties.getClientId().equals(claims.clientId)) {
            throw new IllegalArgumentException("基线平台令牌不属于当前子系统");
        }

        BaselineUserItem platformUser = findPlatformUser(claims);
        BaselineIdentity identity = new BaselineIdentity();
        identity.setUserId(normalizeId(platformUser.userId));
        identity.setUsername(platformUser.userName.trim());
        identity.setNickname(firstText(platformUser.nickName, platformUser.userName));

        // 本地固定管理员不依赖平台角色目录，避免普通角色覆盖超级管理员关系。
        if ("admin".equalsIgnoreCase(identity.getUsername())) {
            return identity;
        }

        List<BaselineIdentity.BaselineRole> roles = findPlatformRoles(
                platformUser.roleIds);
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("基线平台用户没有有效角色");
        }
        identity.setRoles(roles);
        return identity;
    }

    /** 校验固定客户端配置和平台接口根地址。 */
    private void validateConfiguration() {
        if (!hasText(properties.getApiBaseUrl())) {
            throw new IllegalStateException("基线平台接口地址未配置");
        }
        if (!hasText(properties.getClientId())) {
            throw new IllegalStateException("基线平台客户端编号未配置");
        }
    }

    /** 回调携带客户端编号时必须与服务端固定配置一致。 */
    private void validateRequestClient(SsoLoginRequest request) {
        if (request == null || !hasText(request.getTokenId())) {
            throw new IllegalArgumentException("基线平台登录令牌不能为空");
        }
        if (hasText(request.getClientId())
                && !properties.getClientId().equals(request.getClientId().trim())) {
            throw new IllegalArgumentException("回调客户端编号与当前子系统不一致");
        }
    }

    /** 调用平台接口确认令牌有效并刷新平台会话活跃时间。 */
    private void validateToken(SsoLoginRequest request) {
        TokenValidateRequest body = new TokenValidateRequest();
        body.clientId = properties.getClientId();
        body.tokenId = request.getTokenId().trim();
        body.remoteIp = trimToNull(request.getRemoteIp());
        TokenValidateResponse response = httpRequestTool.postJson(
                endpoint(TOKEN_VALIDATE_PATH), body, TokenValidateResponse.class);
        if (response == null
                || !Integer.valueOf(200).equals(response.code)
                || response.data == null
                || !Boolean.TRUE.equals(response.data.valid)) {
            throw new IllegalArgumentException("基线平台登录令牌无效或已过期");
        }
    }

    /** 从已经通过校验的平台令牌中读取用户身份。 */
    private TokenClaims decodeClaims(String tokenId) {
        String[] sections = tokenId.trim().split("\\.");
        if (sections.length != 3) {
            throw new IllegalArgumentException("基线平台登录令牌格式不正确");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(sections[1]);
            JsonNode claims = objectMapper.readTree(
                    new String(payload, StandardCharsets.UTF_8));
            TokenClaims result = new TokenClaims();
            result.userId = textValue(claims.get("userId"));
            result.userName = textValue(claims.get("userName"));
            result.clientId = textValue(claims.get("clientid"));
            if (!hasText(result.userId) || !hasText(result.userName)) {
                throw new IllegalArgumentException("基线平台令牌缺少用户身份");
            }
            return result;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("基线平台登录令牌内容无法解析", exception);
        }
    }

    /** 使用令牌中的用户编号和用户名精确定位平台用户。 */
    private BaselineUserItem findPlatformUser(TokenClaims claims) {
        UserPageRequest body = new UserPageRequest();
        body.pageNum = 1;
        body.pageSize = 100;
        body.userName = claims.userName;
        body.status = "0";
        UserPageResponse response = httpRequestTool.postJson(
                endpoint(USER_PAGE_PATH), body, UserPageResponse.class);
        if (response == null
                || !Integer.valueOf(200).equals(response.code)
                || response.data == null
                || response.data.rows == null) {
            throw new IllegalStateException("查询基线平台用户失败");
        }
        for (BaselineUserItem item : response.data.rows) {
            if (item != null
                    && claims.userName.equals(item.userName)
                    && claims.userId.equals(normalizeId(item.userId))) {
                if ("1".equals(item.status)) {
                    throw new IllegalArgumentException("基线平台用户已停用");
                }
                return item;
            }
        }
        throw new IllegalArgumentException("基线平台未找到当前登录用户");
    }

    /** 查询角色目录并保留用户当前关联的正常角色。 */
    private List<BaselineIdentity.BaselineRole> findPlatformRoles(
            List<Object> userRoleIds) {
        if (userRoleIds == null || userRoleIds.isEmpty()) {
            return Collections.emptyList();
        }
        RolePageRequest body = new RolePageRequest();
        body.pageNum = 1;
        body.pageSize = 1000;
        body.status = "0";
        RolePageResponse response = httpRequestTool.postJson(
                endpoint(ROLE_PAGE_PATH), body, RolePageResponse.class);
        if (response == null
                || !Integer.valueOf(200).equals(response.code)
                || response.data == null
                || response.data.rows == null) {
            throw new IllegalStateException("查询基线平台角色失败");
        }

        List<String> expectedRoleIds = new ArrayList<String>();
        for (Object roleId : userRoleIds) {
            expectedRoleIds.add(normalizeId(roleId));
        }
        List<BaselineIdentity.BaselineRole> result =
                new ArrayList<BaselineIdentity.BaselineRole>();
        for (BaselineRoleItem item : response.data.rows) {
            String roleId = item == null ? null : normalizeId(item.roleId);
            if (!expectedRoleIds.contains(roleId)
                    || "1".equals(item.status)) {
                continue;
            }
            BaselineIdentity.BaselineRole role =
                    new BaselineIdentity.BaselineRole();
            role.setRoleId(roleId);
            role.setRoleName(firstText(item.roleName, item.roleKey, roleId));
            role.setRoleKey(firstText(item.roleKey, roleId));
            result.add(role);
        }
        return result;
    }

    /** 拼接平台接口地址并统一处理结尾斜线。 */
    private String endpoint(String path) {
        String baseUrl = properties.getApiBaseUrl().trim();
        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) + path
                : baseUrl + path;
    }

    private String textValue(JsonNode value) {
        return value == null || value.isNull() ? null : value.asText();
    }

    private String normalizeId(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** 解码后的平台令牌身份字段。 */
    private static class TokenClaims {
        private String userId;
        private String userName;
        private String clientId;
    }

    /** 平台令牌校验请求。 */
    private static class TokenValidateRequest {
        public String clientId;
        public String tokenId;
        public String remoteIp;
    }

    /** 平台令牌校验响应。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenValidateResponse {
        public Integer code;
        public TokenValidateData data;
    }

    /** 平台令牌有效状态。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenValidateData {
        public Boolean valid;
    }

    /** 平台用户分页请求。 */
    private static class UserPageRequest {
        public Integer pageNum;
        public Integer pageSize;
        public String userName;
        public String status;
    }

    /** 平台用户分页响应。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UserPageResponse {
        public Integer code;
        public UserPageData data;
    }

    /** 平台用户分页数据。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UserPageData {
        public List<BaselineUserItem> rows;
    }

    /** 平台用户信息。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BaselineUserItem {
        public Object userId;
        public String userName;
        public String nickName;
        public String status;
        public List<Object> roleIds;
    }

    /** 平台角色分页请求。 */
    private static class RolePageRequest {
        public Integer pageNum;
        public Integer pageSize;
        public String status;
    }

    /** 平台角色分页响应。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RolePageResponse {
        public Integer code;
        public RolePageData data;
    }

    /** 平台角色分页数据。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RolePageData {
        public List<BaselineRoleItem> rows;
    }

    /** 平台角色信息。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BaselineRoleItem {
        public Object roleId;
        public String roleName;
        public String roleKey;
        public String status;
    }
}
