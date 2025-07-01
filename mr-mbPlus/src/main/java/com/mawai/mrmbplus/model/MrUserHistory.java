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
 * 用户浏览历史表
 * </p>
 *
 * @author mawai
 * @since 2025-06-30
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("mr_user_history")
@Schema(name = "MrUserHistory", description = "用户浏览历史表")
public class MrUserHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 菜谱ID
     */
    @TableField("recipe_id")
    @Schema(description = "菜谱ID")
    private Long recipeId;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    @Schema(description = "浏览次数")
    private Integer viewCount;

    /**
     * 最后浏览时间
     */
    @TableField("view_time")
    @Schema(description = "最后浏览时间")
    private LocalDateTime viewTime;
}
