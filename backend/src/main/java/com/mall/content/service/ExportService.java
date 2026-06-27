package com.mall.content.service;

import com.mall.content.config.MallProperties;
import com.mall.content.domain.entity.MarketingCopy;
import com.mall.content.domain.enums.MarketingPlatform;
import com.mall.content.mapper.MarketingCopyMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final MarketingCopyMapper marketingCopyMapper;
    private final MallProperties mallProperties;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ==================== TXT ====================

    public byte[] exportCopyAsTxt(MarketingCopy copy) {
        StringBuilder sb = new StringBuilder();
        if (copy.getTitle() != null) {
            sb.append(copy.getTitle()).append("\n\n");
        }
        sb.append(copy.getContent());
        if (copy.getHashtags() != null && !copy.getHashtags().isBlank()) {
            sb.append("\n\n").append(copy.getHashtags().replace("[", "").replace("]", "").replace("\"", ""));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==================== Markdown ====================

    public byte[] exportCopyAsMarkdown(MarketingCopy copy) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(copy.getTitle() != null ? copy.getTitle() : "营销文案").append("\n\n");
        sb.append(copy.getContent()).append("\n");
        if (copy.getHashtags() != null && !copy.getHashtags().isBlank()) {
            sb.append("\n**标签：** ").append(copy.getHashtags()).append("\n");
        }
        sb.append("\n---\n*平台：").append(copy.getPlatform()).append("*\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==================== PDF (openhtmltopdf) ====================

    public byte[] exportCopyAsPdf(MarketingCopy copy) {
        String platformLabel = MarketingPlatform.valueOf(copy.getPlatform()).label();
        String hashtags = "";
        if (copy.getHashtags() != null && !copy.getHashtags().isBlank()) {
            hashtags = copy.getHashtags().replace("[", "").replace("]", "").replace("\"", "");
        }

        String title = copy.getTitle() != null ? copy.getTitle() : "营销文案";
        String content = copy.getContent() != null ? copy.getContent() : "";

        String html = buildPdfHtml(platformLabel, title, content, hashtags,
                copy.getUpdatedAt() != null ? copy.getUpdatedAt().format(DATE_FMT) : "");

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // 加载中文字体（从配置读取路径）
            String[] fontPaths = mallProperties.getPdf().getFontPaths().split(",");
            String fontFamily = mallProperties.getPdf().getFontFamily();
            boolean fontLoaded = false;
            for (String fp : fontPaths) {
                java.io.File f = new java.io.File(fp.trim());
                if (f.exists()) {
                    builder.useFont(f, fontFamily);
                    fontLoaded = true;
                    break;
                }
            }
            if (!fontLoaded) {
                // 尝试用系统默认，中文可能乱码但至少不报错
                builder.useFont(new java.io.File("."), fontFamily);
            }

            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PDF 生成失败: " + e.getMessage(), e);
        }
    }

    private String buildPdfHtml(String platformLabel, String title, String content, String hashtags, String dateStr) {
        String safePlatform = escapeHtml(platformLabel);
        String safeTitle = escapeHtml(title);
        String contentHtml = escapeHtml(content).replace("\n", "<br/>");
        String tagsStr = escapeHtml(hashtags).replace(",", "&#160;&#160;");

        String color = "#1677ff";
        if ("XIAOHONGSHU".equalsIgnoreCase(platformLabel)) color = "#ff4757";
        else if ("TAOBAO".equalsIgnoreCase(platformLabel)) color = "#ff6b35";
        else if ("DOUYIN".equalsIgnoreCase(platformLabel)) color = "#00d4aa";

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head><meta charset="UTF-8"/>
                <style>
                  @page { size: A4; margin: 2cm 2.2cm; }
                  body { font-family: "%s", sans-serif; font-size: 14px; line-height: 2; color: #222; }
                  .header { border-bottom: 3px solid %s; padding-bottom: 18px; margin-bottom: 28px; }
                  .badge { display: inline-block; background: %s; color: #fff; padding: 4px 14px; border-radius: 4px; font-size: 12px; margin-bottom: 14px; }
                  h1 { font-size: 24px; margin: 14px 0 6px; color: #111; letter-spacing: 1px; }
                  .content { font-size: 15px; margin: 24px 0; }
                  .tags { margin-top: 24px; padding: 14px 16px; background: #f5f7fa; border-radius: 8px; color: #555; font-size: 13px; }
                  .tags strong { color: #333; }
                  .footer { margin-top: 48px; padding-top: 18px; border-top: 1px solid #ddd; font-size: 12px; color: #999; }
                  .footer span { margin: 0 6px; }
                </style></head>
                <body>
                  <div class="header">
                    <div class="badge">%s</div>
                    <h1>%s</h1>
                  </div>
                  <div class="content">%s</div>
                  %s
                  <div class="footer"><span>导出时间：%s</span> <span>|</span> <span>智能商品详情与营销文案生成系统</span></div>
                </body>
                </html>
                """.formatted(
                mallProperties.getPdf().getFontFamily(),
                color, color,
                safePlatform, safeTitle, contentHtml,
                tagsStr.isEmpty() ? "" : "<div class=\"tags\"><strong>🏷 标签：</strong>" + tagsStr + "</div>",
                dateStr
        );
    }

    // ==================== Batch TXT ====================

    public byte[] exportCopiesBatchTxt(List<Long> copyIds) {
        StringBuilder sb = new StringBuilder();
        for (Long id : copyIds) {
            MarketingCopy copy = marketingCopyMapper.selectById(id);
            if (copy == null) continue;
            sb.append("========== 文案 #").append(id).append(" ==========\n");
            sb.append(new String(exportCopyAsTxt(copy), StandardCharsets.UTF_8)).append("\n\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==================== Batch Excel (Apache POI) ====================

    public byte[] exportCopiesBatchExcel(List<Long> copyIds) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("营销文案导出");

            // header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "平台", "版本号", "标题", "文案内容", "标签", "是否收藏", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // data rows
            int rowIdx = 1;
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setWrapText(true);
            dataStyle.setVerticalAlignment(VerticalAlignment.TOP);

            for (Long id : copyIds) {
                MarketingCopy copy = marketingCopyMapper.selectById(id);
                if (copy == null) continue;

                Row row = sheet.createRow(rowIdx++);
                String platformLabel = MarketingPlatform.valueOf(copy.getPlatform()).label();

                setCell(row, 0, copy.getId() != null ? String.valueOf(copy.getId()) : "");
                setCell(row, 1, platformLabel);
                setCell(row, 2, copy.getVariantNo() != null ? String.valueOf(copy.getVariantNo()) : "");
                setCell(row, 3, copy.getTitle() != null ? copy.getTitle() : "");
                setCell(row, 4, copy.getContent() != null ? copy.getContent() : "");
                setCell(row, 5, copy.getHashtags() != null ? copy.getHashtags().replace("[", "").replace("]", "").replace("\"", "") : "");
                setCell(row, 6, copy.getIsFavorite() != null && copy.getIsFavorite() == 1 ? "★" : "");
                setCell(row, 7, copy.getCreatedAt() != null ? copy.getCreatedAt().format(DATE_FMT) : "");

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // auto-size columns (capped)
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                if (width > 15000) sheet.setColumnWidth(i, 12000); // cap wide columns
                if (width < 3000) sheet.setColumnWidth(i, 3000);   // floor narrow columns
            }

            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                workbook.write(os);
                return os.toByteArray();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Excel 生成失败: " + e.getMessage(), e);
        }
    }

    private void setCell(Row row, int col, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
