package com.nageoffer.shortlink.project.controller;

import com.nageoffer.shortlink.project.service.ShortLinkService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ShortLinkRedirectController {

    @Autowired
    private ShortLinkService shortLinkService;
    /**
     * 短链接跳转
     */
    @GetMapping("/{shorturl}")
    public void restoreUrl(@PathVariable("shorturl") String shorturl, ServletRequest request, ServletResponse response )
    {
        log.info("收到跳转请求: fullShortUrl={}", shorturl);
        shortLinkService.restoreUrl(shorturl,request,response);
    }

}
