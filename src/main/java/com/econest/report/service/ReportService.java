package com.econest.report.service;

import com.econest.report.dao.ReportDao;

/** Business logic for the Reports module. */
public class ReportService {

    private final ReportDao dao;

    public ReportService(ReportDao dao) {
        this.dao = dao;
    }

    // TODO: monthly sales summary, low-stock alerts, export
}
