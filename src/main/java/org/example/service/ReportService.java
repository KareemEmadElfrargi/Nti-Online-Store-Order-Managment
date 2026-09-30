package org.example.service;

import org.example.dto.CategoryRevenue;
import org.example.dto.CustomerSpend;
import org.example.dto.MonthlySales;
import org.example.model.OrderStatus;
import org.example.model.Product;
import org.example.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryRevenue> revenueByCategory() {
        return reportRepository.revenueByCategory();
    }

    @Transactional(readOnly = true)
    public List<CustomerSpend> topCustomers(int limit) {
        return reportRepository.topCustomers(limit);
    }

    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> ordersPerStatus() {
        return reportRepository.ordersPerStatus();
    }

    @Transactional(readOnly = true)
    public List<Product> productsNeverOrdered() {
        return reportRepository.productsNeverOrdered();
    }

    @Transactional(readOnly = true)
    public List<MonthlySales> monthlySales(int year) {
        return reportRepository.monthlySales(year);
    }

    @Transactional
    public int applyDiscount(String category, double percent) {
        return reportRepository.applyDiscount(category, percent);
    }
}
