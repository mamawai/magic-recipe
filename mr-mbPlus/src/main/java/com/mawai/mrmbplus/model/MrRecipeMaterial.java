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
 * 菜谱食材关联表
 * </p>
 *
 * @author mawai
 * @since 2025-06-30
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("mr_recipe_material")
@Schema(name = "MrRecipeMaterial", description = "菜谱食材关联表")
public class MrRecipeMaterial implements Serializable {

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
     * 食材ID
     */
    @TableField("material_id")
    @Schema(description = "食材ID")
    private Long materialId;

    /**
     * 用量
     */
    @TableField("amount")
    @Schema(description = "用量")
    private String amount;

    /**
     * 排序
     */
    @TableField("order_num")
    @Schema(description = "排序")
    private Integer orderNum;
}
