package com.nageoffer.shortlink.project.controller;


import com.nageoffer.shortlink.project.common.convention.result.Result;
import com.nageoffer.shortlink.project.common.convention.result.Results;
import com.nageoffer.shortlink.project.service.UrlTitleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UrlTitleController {

    @Autowired
    private UrlTitleService urlTitleService;

    /**
     * 根据url获取对应网站的标题
     */
    @GetMapping("/api/shortlink/v1/title")
    public Result<String> getTitleByUrl(@RequestParam("url") String url) {

        return Results.success(urlTitleService.getTitleByUrl(url));

    }


}
