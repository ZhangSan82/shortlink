package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 短链接地域访问统计实体类
 *
 * 用于记录短链接按地域（国家、省、市）的访问统计
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@TableName("t_link_locale_stats")
public class LinkLocaleStatsDO {

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
     * 省份名称
     */
    private String province;

    /**
     * 城市名称
     */
    private String city;

    /**
     * 城市编码（adcode，通常来自高德/百度地图API）
     */
    private String adcode;

    /**
     * 国家标识（如：中国、United States 等）
     */
    private String country;

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