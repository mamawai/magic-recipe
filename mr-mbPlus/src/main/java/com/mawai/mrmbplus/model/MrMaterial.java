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
 * 食材表
 * </p>
 *
 * @author mawai
 * @since 2025-06-30
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("mr_material")
@Schema(name = "MrMaterial", description = "食材表")
public class MrMaterial implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 食材ID
     */
    @Schema(description = "食材ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 来源网站URL唯一标识
     */
    @TableField("url_id")
    @Schema(description = "来源网站URL唯一标识")
    private String urlId;

    /**
     * 食材名称
     */
    @TableField("name")
    @Schema(description = "食材名称")
    private String name;

    /**
     * 食材分类
     */
    @TableField("category")
    @Schema(description = "食材分类")
    private String category;
}
