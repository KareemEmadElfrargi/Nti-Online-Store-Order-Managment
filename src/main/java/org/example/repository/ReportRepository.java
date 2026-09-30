package org.example.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.dto.CategoryRevenue;
import org.example.dto.CustomerSpend;
import org.example.dto.MonthlySales;
import org.example.model.OrderStatus;
import org.example.model.Product;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReportRepository {

    private static final List<OrderStatus> REVENUE_STATUSES = List.of(OrderStatus.PAID, OrderStatus.SHIPPED);

    @PersistenceContext
    private EntityManager entityManager;

    public List<CategoryRevenue> revenueByCategory() {
        return entityManager.createQuery(
                        "select new org.example.dto.CategoryRevenue(c.name, sum(i.unitPrice * i.quantity)) "
                                + "from OrderItem i join i.order o join i.product p join p.categories c "
                                + "where o.status in :statuses "
                                + "group by c.name order by c.name",
                        CategoryRevenue.class)
                .setParameter("statuses", REVENUE_STATUSES)
                .getResultList();
    }

    public List<CustomerSpend> topCustomers(int limit) {
        return entityManager.createQuery(
                        "select new org.example.dto.CustomerSpend(c.name, sum(i.unitPrice * i.quantity)) "
                                + "from OrderItem i join i.order o join o.customer c "
                                + "where o.status in :statuses "
                                + "group by c.id, c.name "
                                + "order by sum(i.unitPrice * i.quantity) desc",
                        CustomerSpend.class)
                .setParameter("statuses", REVENUE_STATUSES)
                .setMaxResults(limit)
                .getResultList();
    }

    public Map<OrderStatus, Long> ordersPerStatus() {
        List<Object[]> rows = entityManager.createQuery(
                        "select o.status, count(o) from Order o group by o.status", Object[].class)
                .getResultList();
        Map<OrderStatus, Long> result = new EnumMap<>(OrderStatus.class);
        for (Object[] row : rows) {
            result.put((OrderStatus) row[0], (Long) row[1]);
        }
        return result;
    }

    public List<Product> productsNeverOrdered() {
        return entityManager.createQuery(
                        "select p from Product p "
                                + "where not exists (select 1 from OrderItem i where i.product = p) "
                                + "order by p.id",
                        Product.class)
                .getResultList();
    }

    public List<MonthlySales> monthlySales(int year) {
        return entityManager.createQuery(
                        "select new org.example.dto.MonthlySales(month(o.orderedAt), sum(i.unitPrice * i.quantity)) "
                                + "from OrderItem i join i.order o "
                                + "where year(o.orderedAt) = :year and o.status in :statuses "
                                + "group by month(o.orderedAt) order by month(o.orderedAt)",
                        MonthlySales.class)
                .setParameter("year", year)
                .setParameter("statuses", REVENUE_STATUSES)
                .getResultList();
    }

    public int applyDiscount(String category, double percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 0 and 100");
        }
        BigDecimal factor = BigDecimal.ONE.subtract(BigDecimal.valueOf(percent).movePointLeft(2));

        entityManager.flush();
        int updated = entityManager.createQuery(
                        "update Product p set p.price = p.price * :factor, p.version = p.version + 1 "
                                + "where p.id in (select p2.id from Product p2 join p2.categories c "
                                + "where c.name = :category)")
                .setParameter("factor", factor)
                .setParameter("category", category)
                .executeUpdate();
        entityManager.clear();
        return updated;
    }
}
