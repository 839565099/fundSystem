package com.fund.controller;

import com.fund.entity.UserFavorite;
import com.fund.service.ExportService;
import com.fund.service.UserFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final UserFavoriteService userFavoriteService;

    @GetMapping("/favorites/{format}")
    public void exportFavorites(
            @PathVariable String format,
            @RequestAttribute Long userId,
            HttpServletResponse response) throws IOException {

        List<UserFavorite> favorites = userFavoriteService.getUserFavoritesWithFundInfo(userId);

        if (favorites == null || favorites.isEmpty()) {
            writeErrorResponse(response, "暂无收藏数据可导出");
            return;
        }

        switch (format.toLowerCase()) {
            case "csv":
                exportService.exportToCsv(favorites, response);
                break;
            case "excel":
            case "xlsx":
                exportService.exportToExcel(favorites, response);
                break;
            case "pdf":
                exportService.exportToPdf(favorites, response);
                break;
            default:
                writeErrorResponse(response, "不支持的导出格式: " + format);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":400,\"message\":\"" + message + "\"}");
    }
}
