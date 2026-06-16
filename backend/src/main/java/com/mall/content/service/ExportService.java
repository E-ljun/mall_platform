package com.mall.content.service;

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

        String title = escapeHtml(copy.getTitle() != null ? copy.getTitle() : "营销文案");
        String content = escapeHtml(copy.getContent() != null ? copy.getContent() : "");
        String contentHtml = content.replace("\n", "<br/>");
        String tagsHtml = escapeHtml(hashtags).replace(",", "&nbsp;&nbsp;");

        String html = """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head><meta charset="UTF-8">
                <style>
                  @page { size: A4; margin: 2cm; }
                  body { font-family: "SimSun", "Microsoft YaHei", "PingFang SC", sans-serif; font-size: 14px; line-height: 1.8; color: #333; }
                  .header { border-bottom: 3px solid #1677ff; padding-bottom: 16px; margin-bottom: 24px; }
                  .platform { display: inline-block; background: #1677ff; color: #fff; padding: 4px 12px; border-radius: 4px; font-size: 12px; margin-bottom: 12px; }
                  h1 { font-size: 22px; margin: 12px 0 8px; color: #111; }
                  .content { font-size: 15px; margin: 20px 0; }
                  .tags { margin-top: 20px; padding: 12px; background: #f5f7fa; border-radius: 6px; color: #666; font-size: 13px; }
                  .footer { margin-top: 40px; padding-top: 16px; border-top: 1px solid #eee; font-size: 12px; color: #999; }
                </style></head>
                <body>
                  <div class="header">
                    <div class="platform">%s</div>
                    <h1>%s</h1>
                  </div>
                  <div class="content">%s</div>
                  %s
                  <div class="footer">导出时间：%s &nbsp;|&nbsp; 智能商品详情与营销文案生成系统</div>
                </body>
                </html>
                """.formatted(
                escapeHtml(platformLabel), title, contentHtml,
                tagsHtml.isEmpty() ? "" : "<div class=\"tags\"><strong>标签：</strong>" + tagsHtml + "</div>",
                copy.getUpdatedAt() != null ? copy.getUpdatedAt().format(DATE_FMT) : ""
        );

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PDF 生成失败: " + e.getMessage(), e);
        }
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
