package com.mawai.mrmbplus.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * <p>
 * 微信小程序用户表
 * </p>
 *
 * @author mawai
 * @since 2025-06-30
 */
@Getter
@Setter
@ToString
@TableName("mr_user")
@Accessors(chain = true)
@Schema(name = "MrUser", description = "微信小程序用户表")
public class MrUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 微信openid
     */
    @TableField("openid")
    @Schema(description = "微信openid")
    private String openid;

    /**
     * 微信unionid
     */
    @TableField("unionid")
    @Schema(description = "微信unionid")
    private String unionid;

    /**
     * 用户昵称
     */
    @TableField("nickname")
    @Schema(description = "用户昵称")
    private String nickname;

    /**
     * 会话密钥
     */
    @TableField("session_key")
    @Schema(description = "会话密钥")
    private String sessionKey;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    /**
     * 注册时间
     */
    @TableField("register_time")
    @Schema(description = "注册时间")
    private LocalDateTime registerTime;

    /**
     * 状态：0禁用，1正常
     */
    @TableField("status")
    @Schema(description = "状态：0禁用，1正常")
    private Boolean status;
}
