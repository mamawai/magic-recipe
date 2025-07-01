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
 * 分类表
 * </p>
 *
 * @author mawai
 * @since 2025-06-30
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("mr_category")
@Schema(name = "MrCategory", description = "分类表")
public class MrCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    @TableField("name")
    @Schema(description = "分类名称")
    private String name;

    /**
     * 父分类ID
     */
    @TableField("parent_id")
    @Schema(description = "父分类ID")
    private Long parentId;

    /**
     * 分类级别
     */
    @TableField("level")
    @Schema(description = "分类级别")
    private int level;

    /**
     * 排序
     */
    @TableField("cat_id")
    @Schema(description = "分类id，用于搜索该分类的菜品，可为空")
    private String catId;
}
