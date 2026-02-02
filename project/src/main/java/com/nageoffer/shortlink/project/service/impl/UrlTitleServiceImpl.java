package com.nageoffer.shortlink.project.service.impl;

import com.nageoffer.shortlink.project.service.UrlTitleService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;

@Slf4j
@Service
public class UrlTitleServiceImpl implements UrlTitleService {

    /**
     * 根据url获取对应网站的标题
     */
    @Override
    public String getTitleByUrl(String url) {
        try {
            // 1. 发送连接
            Document doc = Jsoup.connect(url)
                    // 模拟浏览器 User-Agent，防止被反爬虫拦截
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(5000) // 设置超时时间 5秒
                    .get();

            // 2. 获取标题
            String title = doc.title();

            // 3. 判空处理
            if (title == null || title.isEmpty()) {
                return "未知标题";
            }
            return title;

        } catch (SocketTimeoutException e) {
            log.warn("请求超时: " + url);
            return "访问超时";
        } catch (Exception e) {
            e.printStackTrace();
            return "获取失败";
        }
    }
}
