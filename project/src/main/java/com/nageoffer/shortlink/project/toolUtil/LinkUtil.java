package com.nageoffer.shortlink.project.toolUtil;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.util.Date;
import java.util.Optional;

import static com.nageoffer.shortlink.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_VALUE_TIME;

/**
 * 短链接工具类
 */
public class LinkUtil {
    public static long getLinkCacheValidTime(Date validTime) {
        return Optional.ofNullable(validTime)
                .map(each-> DateUtil.between(new Date(),each,DateUnit.MS))
                .orElse(DEFAULT_CACHE_VALUE_TIME);



    }

    /**
     * 从 HttpServletRequest 中获取客户端真实 IP 地址
     * 优先级顺序处理常见的代理头，按最可靠到次可靠排序
     *
     * @param request HttpServletRequest 对象
     * @return 客户端真实 IP 地址，如果获取失败返回 "unknown"
     */
    public static String getRealIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // 常见的代理头，按优先级排序
        String[] headers = {
                "X-Forwarded-For",           // 最常用，负载均衡/代理服务器会添加
                "Proxy-Client-IP",           // Apache http 代理/负载均衡
                "WL-Proxy-Client-IP",        // WebLogic 代理
                "HTTP_CLIENT_IP",            // 部分代理
                "HTTP_X_FORWARDED_FOR",      // 部分代理大小写变体
                "X-Real-IP"                  // Nginx 常用配置 proxy_set_header X-Real-IP $remote_addr;
        };

        String ip = null;

        // 依次尝试各个代理头
        for (String header : headers) {
            ip = request.getHeader(header);
            if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能有多个 IP，用逗号分隔，取第一个（最原始的客户端 IP）
                int index = ip.indexOf(",");
                if (index != -1) {
                    ip = ip.substring(0, index).trim();
                }
                return ip;
            }
        }

        // 以上代理头都没有时，使用 request.getRemoteAddr()
        // 这通常是直连或最后一级代理的 IP
        ip = request.getRemoteAddr();

        // 127.0.0.1 / ::1 转为 IPv4 格式
        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    public static String getOs(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        if (StringUtils.isBlank(userAgent)) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac os x") || userAgent.contains("macintosh")) {
            return "macOS";
        } else if (userAgent.contains("linux") && userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone") || userAgent.contains("ipad") || userAgent.contains("ipod")) {
            return "iOS";
        } else if (userAgent.contains("linux")) {
            return "Linux";
        } else if (userAgent.contains("unix")) {
            return "Unix";
        } else {
            return "Unknown";
        }
    }

    /**
     * 从 HttpServletRequest 中获取用户浏览器信息
     * 通过解析 User-Agent 头来判断常见浏览器
     *
     * @param request HttpServletRequest 对象
     * @return 用户浏览器字符串，如果无法识别返回 "Unknown"
     */
    public static String getBrowser(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        if (StringUtils.isBlank(userAgent)) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("edg/")) {
            return "Microsoft Edge";
        } else if (userAgent.contains("chrome") && userAgent.contains("safari")) {
            return "Google Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Mozilla Firefox";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            return "Apple Safari";
        } else if (userAgent.contains("trident") || userAgent.contains("msie")) {
            return "Internet Explorer";
        } else if (userAgent.contains("opera")) {
            return "Opera";
        } else {
            return "Unknown";
        }
    }

    /**
     * 获取用户访问设备
     *
     * @param request 请求
     * @return 访问设备
     */
    public static String getDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("mobile")) {
            return "Mobile";
        }
        return "PC";
    }

    /**
     * 获取用户访问网络
     *
     * @param request 请求
     * @return 访问设备
     */
    public static String getNetwork(HttpServletRequest request) {
        String actualIp = getRealIp(request);
        // 这里简单判断IP地址范围，您可能需要更复杂的逻辑
        // 例如，通过调用IP地址库或调用第三方服务来判断网络类型
        return actualIp.startsWith("192.168.") || actualIp.startsWith("10.") ? "WIFI" : "Mobile";
    }

    /**
     * 获取原始链接中的域名
     * 如果原始链接包含 www 开头的话需要去掉
     *
     * @param url 创建或者修改短链接的原始链接
     * @return 原始链接中的域名
     */
    public static String extractDomain(String url) {
        String domain = null;
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (StrUtil.isNotBlank(host)) {
                domain = host;
                if (domain.startsWith("www.")) {
                    domain = host.substring(4);
                }
            }
        } catch (Exception ignored) {
        }
        return domain;
    }
}
