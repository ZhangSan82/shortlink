package com.nageoffer.shortlink.project.dto.resp;

import lombok.Data;

@Data
public class ShortLinkGroupCountQueryRespDTO {

    /**
     * 短链接分组标识
     */
    private String gid;

    /**
     * 短链接数量
     */
    private Integer shortLinkCount;


}
