package io.choerodon.iam.api.vo;

import io.swagger.annotations.ApiModelProperty;
import org.hzero.iam.domain.entity.Role;
import org.hzero.starter.keyencrypt.core.Encrypt;

import java.util.Date;
import java.util.List;

public class UserVO {
    @Encrypt
    private Long id;

    @ApiModelProperty(value = "登录名/非必填")
    private String loginName;

    @ApiModelProperty(value = "邮箱/必填")
    private String email;

    @ApiModelProperty(value = "用户名/必填")
    private String realName;

    @ApiModelProperty(value = "手机号/非必填")
    private String phone;

    @ApiModelProperty(value = "头像/非必填")
    private String imageUrl;

    private String profilePhoto;

    @ApiModelProperty(value = "是否启用/非必填")
    private Boolean enabled;

    @ApiModelProperty(value = "是否是LDAP用户/非必填")
    private Boolean ldap;

    @ApiModelProperty(value = "语言/非必填")
    private String language;

    @ApiModelProperty(value = "时区/非必填")
    private String timeZone;


    private List<Role> roles;

    @ApiModelProperty(value = "用户角色名称，多个用英文逗号隔开")
    private String roleNames;

    @ApiModelProperty(value = "用户角色编码,多个用英文逗号隔开")
    private String roleCodes;

    private Date creationDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getLdap() {
        return ldap;
    }

    public void setLdap(Boolean ldap) {
        this.ldap = ldap;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public String getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(String roleCodes) {
        this.roleCodes = roleCodes;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setRoleNames(String roleNames) {
        this.roleNames = roleNames;
    }

    public String getRoleNames() {
        return roleNames;
    }
}
