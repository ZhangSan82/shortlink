package com.nageoffer.shortlink.project.dao.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接跳转类
 * 对应表 t_link_goto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_link_goto")
public class ShortLinkGotoDO {

    /**
     * id
     */
    private Long id;

    /**
     * 完整短链接
     */
    String fullShortUrl;
    /**
     * 分组标识
     */
    String gid;


}
