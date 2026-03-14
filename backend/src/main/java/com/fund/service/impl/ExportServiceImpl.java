package com.fund.service.impl;

import cn.hutool.core.date.DateUtil;
import com.fund.entity.UserFavorite;
import com.fund.service.ExportService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExportServiceImpl implements ExportService {
    
    private static final String[] HEADERS = {"基金代码", "基金名称", "最新净值", "净值日期", "日涨跌(%)", "近一周(%)", "近一月(%)", "近三月(%)", "近一年(%)", "收藏时间"};
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void exportToCsv(List<UserFavorite> favorites, HttpServletResponse response) {
        try {
            setResponseHeaders(response, "text/csv", "收藏基金_" + DateUtil.today() + ".csv");
            
            OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
            writer.write('\ufeff');
            
            writer.write(String.join(",", HEADERS));
            writer.write("\n");
            
            for (UserFavorite fav : favorites) {
                String[] values = {
                    escapeCsv(fav.getFundCode()),
                    escapeCsv(fav.getFundName()),
                    formatNumber(fav.getNav()),
                    fav.getNavDate() != null ? fav.getNavDate().format(DATE_FORMATTER) : "",
                    formatPercent(fav.getDayGrowth()),
                    formatPercent(fav.getWeekGrowth()),
                    formatPercent(fav.getMonthGrowth()),
                    formatPercent(fav.getThreeMonthGrowth()),
                    formatPercent(fav.getYearGrowth()),
                    fav.getCreateTime() != null ? fav.getCreateTime().format(DATETIME_FORMATTER) : ""
                };
                writer.write(String.join(",", values));
                writer.write("\n");
            }
            
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("导出CSV失败", e);
            throw new RuntimeException("导出CSV失败: " + e.getMessage());
        }
    }
    
    @Override
    public void exportToExcel(List<UserFavorite> favorites, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("收藏基金");
            
            sheet.setColumnWidth(0, 15 * 256);
            sheet.setColumnWidth(1, 35 * 256);
            sheet.setColumnWidth(2, 12 * 256);
            sheet.setColumnWidth(3, 12 * 256);
            for (int i = 4; i < 10; i++) {
                sheet.setColumnWidth(i, 12 * 256);
            }
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int rowNum = 1;
            for (UserFavorite fav : favorites) {
                Row row = sheet.createRow(rowNum++);
                
                createCell(row, 0, fav.getFundCode(), dataStyle);
                createCell(row, 1, fav.getFundName(), dataStyle);
                createNumberCell(row, 2, fav.getNav(), dataStyle);
                createCell(row, 3, fav.getNavDate() != null ? fav.getNavDate().format(DATE_FORMATTER) : "", dataStyle);
                createPercentCell(row, 4, fav.getDayGrowth(), percentStyle);
                createPercentCell(row, 5, fav.getWeekGrowth(), percentStyle);
                createPercentCell(row, 6, fav.getMonthGrowth(), percentStyle);
                createPercentCell(row, 7, fav.getThreeMonthGrowth(), percentStyle);
                createPercentCell(row, 8, fav.getYearGrowth(), percentStyle);
                createCell(row, 9, fav.getCreateTime() != null ? fav.getCreateTime().format(DATETIME_FORMATTER) : "", dataStyle);
            }
            
            setResponseHeaders(response, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                "收藏基金_" + DateUtil.today() + ".xlsx");
            
            OutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
        } catch (IOException e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出Excel失败: " + e.getMessage());
        }
    }
    
    @Override
    public void exportToPdf(List<UserFavorite> favorites, HttpServletResponse response) {
        try {
            setResponseHeaders(response, "application/pdf", "收藏基金_" + DateUtil.today() + ".pdf");
            
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();
            
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(bfChinese, 18, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(bfChinese, 10, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
            com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(bfChinese, 9, com.itextpdf.text.Font.NORMAL);
            
            Paragraph title = new Paragraph("收藏基金列表", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            Paragraph exportTime = new Paragraph("导出时间: " + DateUtil.now(), dataFont);
            exportTime.setAlignment(Element.ALIGN_RIGHT);
            exportTime.setSpacingAfter(10);
            document.add(exportTime);
            
            float[] columnWidths = {1.5f, 3f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1.5f};
            PdfPTable table = new PdfPTable(columnWidths);
            table.setWidthPercentage(100);
            
            for (String header : HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new BaseColor(24, 144, 255));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(8);
                table.addCell(cell);
            }
            
            for (UserFavorite fav : favorites) {
                addPdfCell(table, fav.getFundCode(), dataFont);
                addPdfCell(table, fav.getFundName(), dataFont);
                addPdfCell(table, formatNumber(fav.getNav()), dataFont);
                addPdfCell(table, fav.getNavDate() != null ? fav.getNavDate().format(DATE_FORMATTER) : "", dataFont);
                addPdfCell(table, formatPercent(fav.getDayGrowth()), dataFont, fav.getDayGrowth());
                addPdfCell(table, formatPercent(fav.getWeekGrowth()), dataFont, fav.getWeekGrowth());
                addPdfCell(table, formatPercent(fav.getMonthGrowth()), dataFont, fav.getMonthGrowth());
                addPdfCell(table, formatPercent(fav.getThreeMonthGrowth()), dataFont, fav.getThreeMonthGrowth());
                addPdfCell(table, formatPercent(fav.getYearGrowth()), dataFont, fav.getYearGrowth());
                addPdfCell(table, fav.getCreateTime() != null ? fav.getCreateTime().format(DATETIME_FORMATTER) : "", dataFont);
            }
            
            document.add(table);
            
            Paragraph summary = new Paragraph("共 " + favorites.size() + " 只基金", dataFont);
            summary.setAlignment(Element.ALIGN_RIGHT);
            summary.setSpacingBefore(10);
            document.add(summary);
            
            document.close();
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            throw new RuntimeException("导出PDF失败: " + e.getMessage());
        }
    }
    
    private void setResponseHeaders(HttpServletResponse response, String contentType, String fileName) throws IOException {
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        return style;
    }
    
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }
    
    private void createNumberCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }
    
    private void createPercentCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.divide(new BigDecimal(100)).doubleValue());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }
    
    private void addPdfCell(PdfPTable table, String text, com.itextpdf.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    private void addPdfCell(PdfPTable table, String text, com.itextpdf.text.Font font, BigDecimal growth) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        
        if (growth != null) {
            if (growth.compareTo(BigDecimal.ZERO) > 0) {
                cell.setBackgroundColor(new BaseColor(255, 235, 235));
            } else if (growth.compareTo(BigDecimal.ZERO) < 0) {
                cell.setBackgroundColor(new BaseColor(235, 255, 235));
            }
        }
        
        table.addCell(cell);
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private String formatNumber(BigDecimal value) {
        return value != null ? value.setScale(4, BigDecimal.ROUND_HALF_UP).toString() : "";
    }
    
    private String formatPercent(BigDecimal value) {
        return value != null ? value.setScale(2, BigDecimal.ROUND_HALF_UP).toString() : "";
    }
}
