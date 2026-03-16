package com.trade.util;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ResponseUtil {

    private ResponseUtil() {
    }

    public static void setCsvDownloadHeader(HttpServletResponse response, String fileName) throws Exception {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");

        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
        // RFC 5987
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }
}
