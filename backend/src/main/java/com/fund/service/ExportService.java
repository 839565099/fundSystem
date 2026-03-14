package com.fund.service;

import com.fund.entity.UserFavorite;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface ExportService {
    
    void exportToCsv(List<UserFavorite> favorites, HttpServletResponse response);
    
    void exportToExcel(List<UserFavorite> favorites, HttpServletResponse response);
    
    void exportToPdf(List<UserFavorite> favorites, HttpServletResponse response);
}
