package com.nageoffer.shortlink.project.service;

public interface UrlTitleService {

    /**
     * 根据url获取对应网站的标题
     */
    String getTitleByUrl(String url);
}
