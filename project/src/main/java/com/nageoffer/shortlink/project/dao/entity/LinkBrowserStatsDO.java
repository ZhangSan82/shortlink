package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 短链接浏览器访问统计实体类
 *
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@TableName("t_link_browser_stats")
public class LinkBrowserStatsDO {

    /**
     * ID，自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 统计日期
     */
    private Date date;

    /**
     * 当日访问量（PV）
     */
    private Integer cnt;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 删除标识：0-未删除，1-已删除
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;

}