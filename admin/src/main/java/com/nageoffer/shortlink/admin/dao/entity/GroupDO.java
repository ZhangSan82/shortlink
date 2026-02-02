package com.nageoffer.shortlink.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 短链接分组实体
 * 对应表: t_group
 */
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@TableName("t_group")
public class GroupDO {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 创建分组用户名
     */
    private String username;

    /**
     * 分组排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     * 注：需要在 MyMetaObjectHandler 中配置自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     * 注：需要在 MyMetaObjectHandler 中配置自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1：已删除
     * 注：MyBatis-Plus 逻辑删除注解，查询时会自动带上 del_flag = 0
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}