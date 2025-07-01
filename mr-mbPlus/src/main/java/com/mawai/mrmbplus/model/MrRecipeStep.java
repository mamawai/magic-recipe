package com.mawai.mrmbplus.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * <p>
 * 菜谱步骤表
 * </p>
 *
 * @author mawai
 * @since 2025-06-30
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("mr_recipe_step")
@Schema(name = "MrRecipeStep", description = "菜谱步骤表")
public class MrRecipeStep implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 菜谱ID
     */
    @TableField("recipe_id")
    @Schema(description = "菜谱ID")
    private Long recipeId;

    /**
     * 步骤序号
     */
    @TableField("step_number")
    @Schema(description = "步骤序号")
    private Integer stepNumber;

    /**
     * 步骤描述
     */
    @TableField("description")
    @Schema(description = "步骤描述")
    private String description;

    /**
     * 步骤图片
     */
    @TableField("image_url")
    @Schema(description = "步骤图片")
    private String imageUrl;
}
