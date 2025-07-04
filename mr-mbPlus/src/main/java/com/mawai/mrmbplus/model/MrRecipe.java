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
 * 菜谱表
 * </p>
 *
 * @author mawai
 * @since 2025-06-30
 */
@Getter
@Setter
@ToString
@TableName("mr_recipe")
@Accessors(chain = true)
@Schema(name = "MrRecipe", description = "菜谱表")
public class MrRecipe implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜谱ID
     */
    @Schema(description = "菜谱ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 菜谱名称
     */
    @TableField("title")
    @Schema(description = "菜谱名称")
    private String title;

    /**
     * 来源网站URL唯一标识
     */
    @TableField("url_id")
    @Schema(description = "来源网站URL唯一标识")
    private String urlId;

    /**
     * 封面图片
     */
    @TableField("cover_img")
    @Schema(description = "封面图片")
    private String coverImg;

    /**
     * 评分
     */
    @TableField("grade")
    @Schema(description = "评分")
    private String grade;

    /**
     * 提示
     */
    @TableField("tip")
    @Schema(description = "提示")
    private String tip;

    /**
     * 收藏数
     */
    @TableField("favorites")
    @Schema(description = "收藏数")
    private Integer favorites;

    /**
     * 来源URL
     */
    @TableField("source_url")
    @Schema(description = "来源URL")
    private String sourceUrl;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
