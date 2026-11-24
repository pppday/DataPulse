package com.pulseos.datapulse.service;

import com.pulseos.datapulse.dto.ExportExcelRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 查询结果流式导出 Excel (.xlsx) 报表服务接口。 */
public interface ExcelExportService {
    /**
     * 将查询分析数据流式导出为企业级高颜值 Excel (.xlsx) 表格并写入 HTTP 响应
     *
     * @param request  导出请求参数（包含列名、行数据、商业洞察与元数据）
     * @param response HTTP 响应对象
     */
    void exportToResponse(ExportExcelRequest request, HttpServletResponse response);
}
