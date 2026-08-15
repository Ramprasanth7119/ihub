package com.ihub.dao;

import com.ihub.dto.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class AdminDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Platform-wide counters for the admin dashboard.
     *
     * <p>Grouped into four aggregate queries using conditional sums rather than one
     * {@code COUNT(*)} per figure — this used to issue 14 separate round trips on
     * every dashboard load, and the dashboard is the admin console's landing page.</p>
     */
    public PlatformMetricsResponse fetchPlatformMetrics() {
        PlatformMetricsResponse metrics = new PlatformMetricsResponse();

        jdbcTemplate.query("""
            SELECT COUNT(*) AS total,
                   SUM(role = 'CREATOR')  AS creators,
                   SUM(role = 'INVESTOR') AS investors,
                   SUM(role = 'ADMIN')    AS admins,
                   SUM(active = TRUE)     AS active_users,
                   SUM(created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)) AS new_last_30,
                   SUM(created_at >= DATE_SUB(NOW(), INTERVAL 60 DAY)
                       AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)) AS new_prev_30
            FROM users
        """, rs -> {
            metrics.setTotalUsers(rs.getLong("total"));
            metrics.setTotalCreators(rs.getLong("creators"));
            metrics.setTotalInvestors(rs.getLong("investors"));
            metrics.setTotalAdmins(rs.getLong("admins"));
            metrics.setActiveUsers(rs.getLong("active_users"));
            metrics.setNewUsersLast30Days(rs.getLong("new_last_30"));
            metrics.setNewUsersPrevious30Days(rs.getLong("new_prev_30"));
        });

        jdbcTemplate.query("""
            SELECT SUM(status <> 'ARCHIVED')  AS total,
                   SUM(status = 'PUBLISHED')  AS published,
                   SUM(status = 'DRAFT')      AS drafts
            FROM ideas
        """, rs -> {
            metrics.setTotalIdeas(rs.getLong("total"));
            metrics.setPublishedIdeas(rs.getLong("published"));
            metrics.setDraftIdeas(rs.getLong("drafts"));
        });

        jdbcTemplate.query("""
            SELECT COUNT(*) AS total,
                   SUM(status = 'SCHEDULED') AS scheduled,
                   SUM(status = 'ACTIVE')    AS active,
                   SUM(status = 'CLOSED')    AS closed,
                   SUM(status = 'CANCELLED') AS cancelled
            FROM auctions
        """, rs -> {
            metrics.setTotalAuctions(rs.getLong("total"));
            metrics.setScheduledAuctions(rs.getLong("scheduled"));
            metrics.setActiveAuctions(rs.getLong("active"));
            metrics.setClosedAuctions(rs.getLong("closed"));
            metrics.setCancelledAuctions(rs.getLong("cancelled"));
        });

        jdbcTemplate.query("""
            SELECT (SELECT COUNT(*) FROM bids)                     AS bid_count,
                   (SELECT COALESCE(SUM(bid_amount), 0) FROM bids) AS bid_value,
                   (SELECT COUNT(*) FROM auction_winners)          AS decided,
                   (SELECT COALESCE(SUM(winning_bid), 0) FROM auction_winners) AS settled_value,
                   (SELECT COALESCE(MAX(winning_bid), 0) FROM auction_winners) AS highest_win
        """, rs -> {
            metrics.setTotalBids(rs.getLong("bid_count"));
            metrics.setTotalBidValue(rs.getDouble("bid_value"));
            metrics.setCompletedAuctionsWithWinner(rs.getLong("decided"));
            metrics.setSettledValue(rs.getDouble("settled_value"));
            metrics.setHighestWinningBid(rs.getDouble("highest_win"));
        });

        return metrics;
    }

    public List<AdminAuctionSummaryResponse> findRecentAuctions(int limit) {
        String sql = """
            SELECT a.id, a.idea_id, i.title AS idea_title, a.status,
                   a.start_time, a.end_time,
                   (SELECT COUNT(*) FROM bids b WHERE b.auction_id = a.id) AS bid_count,
                   (SELECT MAX(b.bid_amount) FROM bids b WHERE b.auction_id = a.id) AS highest_bid
            FROM auctions a
            INNER JOIN ideas i ON i.id = a.idea_id
            ORDER BY a.created_at DESC
            LIMIT :limit
        """;
        return jdbcTemplate.query(sql, Map.of("limit", limit), (rs, rowNum) -> {
            AdminAuctionSummaryResponse item = new AdminAuctionSummaryResponse();
            item.setId(rs.getLong("id"));
            item.setIdeaId(rs.getLong("idea_id"));
            item.setIdeaTitle(rs.getString("idea_title"));
            item.setStatus(rs.getString("status"));
            item.setStartTime(toDateTime(rs, "start_time"));
            item.setEndTime(toDateTime(rs, "end_time"));
            item.setBidCount(rs.getLong("bid_count"));
            Double highest = rs.getObject("highest_bid", Double.class);
            item.setHighestBid(highest);
            return item;
        });
    }

    public List<AdminAuctionSummaryResponse> findAuctionsForAdmin(String status, String search, int limit, int offset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);

        StringBuilder sql = new StringBuilder("""
            SELECT a.id, a.idea_id, i.title AS idea_title, a.status,
                   a.start_time, a.end_time,
                   (SELECT COUNT(*) FROM bids b WHERE b.auction_id = a.id) AS bid_count,
                   (SELECT MAX(b.bid_amount) FROM bids b WHERE b.auction_id = a.id) AS highest_bid
            FROM auctions a
            INNER JOIN ideas i ON i.id = a.idea_id
            WHERE
        """);
        sql.append(auctionAdminFilter(status, search, params));
        sql.append(" ORDER BY a.created_at DESC, a.id DESC LIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            AdminAuctionSummaryResponse item = new AdminAuctionSummaryResponse();
            item.setId(rs.getLong("id"));
            item.setIdeaId(rs.getLong("idea_id"));
            item.setIdeaTitle(rs.getString("idea_title"));
            item.setStatus(rs.getString("status"));
            item.setStartTime(toDateTime(rs, "start_time"));
            item.setEndTime(toDateTime(rs, "end_time"));
            item.setBidCount(rs.getLong("bid_count"));
            item.setHighestBid(rs.getObject("highest_bid", Double.class));
            return item;
        });
    }

    public AdminAuctionSummaryResponse findAuctionByIdForAdmin(Long auctionId) {
        String sql = """
            SELECT a.id, a.idea_id, i.title AS idea_title, a.status,
                   a.start_time, a.end_time,
                   (SELECT COUNT(*) FROM bids b WHERE b.auction_id = a.id) AS bid_count,
                   (SELECT MAX(b.bid_amount) FROM bids b WHERE b.auction_id = a.id) AS highest_bid
            FROM auctions a
            INNER JOIN ideas i ON i.id = a.idea_id
            WHERE a.id = :auctionId
        """;

        try {
            return jdbcTemplate.queryForObject(sql, Map.of("auctionId", auctionId), (rs, rowNum) -> {
                AdminAuctionSummaryResponse item = new AdminAuctionSummaryResponse();
                item.setId(rs.getLong("id"));
                item.setIdeaId(rs.getLong("idea_id"));
                item.setIdeaTitle(rs.getString("idea_title"));
                item.setStatus(rs.getString("status"));
                item.setStartTime(toDateTime(rs, "start_time"));
                item.setEndTime(toDateTime(rs, "end_time"));
                item.setBidCount(rs.getLong("bid_count"));
                item.setHighestBid(rs.getObject("highest_bid", Double.class));
                return item;
            });
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    public long countAuctionsForAdmin(String status, String search) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = """
            SELECT COUNT(*)
            FROM auctions a
            INNER JOIN ideas i ON i.id = a.idea_id
            WHERE
        """ + auctionAdminFilter(status, search, params);

        Long total = jdbcTemplate.queryForObject(sql, params, Long.class);
        return total != null ? total : 0L;
    }

    /**
     * Shared filter for the admin auction list and its count. The search term matches
     * the idea title or the auction id, and is bound as a parameter rather than
     * concatenated — only the fixed {@code LIKE} fragment is appended to the SQL.
     */
    private String auctionAdminFilter(String status, String search, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder("1=1");

        if (status != null && !status.isBlank()) {
            where.append(" AND a.status = :status");
            params.addValue("status", status.toUpperCase());
        }
        if (search != null && !search.isBlank()) {
            where.append(" AND (i.title LIKE :search OR CAST(a.id AS CHAR) LIKE :search)");
            params.addValue("search", "%" + search.trim() + "%");
        }

        return where.toString();
    }

    public List<AdminUserResponse> findUsersForAdmin(String role, Boolean active, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
            SELECT u.id, u.name, u.email, u.role, u.verified, u.active, u.created_at,
                   (SELECT COUNT(*) FROM ideas i WHERE i.creator_id = u.id) AS idea_count,
                   (SELECT COUNT(*) FROM bids b WHERE b.investor_id = u.id) AS bid_count
            FROM users u
            WHERE 1=1
        """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);

        if (role != null && !role.isBlank()) {
            sql.append(" AND u.role = :role");
            params.addValue("role", role.toUpperCase());
        }
        if (active != null) {
            sql.append(" AND u.active = :active");
            params.addValue("active", active);
        }
        sql.append(" ORDER BY u.created_at DESC LIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            AdminUserResponse user = new AdminUserResponse();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setRole(rs.getString("role"));
            user.setVerified(rs.getBoolean("verified"));
            user.setActive(rs.getBoolean("active"));
            user.setCreatedAt(toDateTime(rs, "created_at"));
            user.setIdeaCount(rs.getLong("idea_count"));
            user.setBidCount(rs.getLong("bid_count"));
            return user;
        });
    }

    public long countUsersForAdmin(String role, Boolean active) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users u WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (role != null && !role.isBlank()) {
            sql.append(" AND u.role = :role");
            params.addValue("role", role.toUpperCase());
        }
        if (active != null) {
            sql.append(" AND u.active = :active");
            params.addValue("active", active);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0;
    }

    public int updateUserActive(Long userId, boolean active) {
        return jdbcTemplate.update(
                "UPDATE users SET active = :active WHERE id = :id",
                Map.of("id", userId, "active", active)
        );
    }

    public boolean isUserActive(Long userId) {
        Boolean active = jdbcTemplate.queryForObject(
                "SELECT active FROM users WHERE id = :id",
                Map.of("id", userId),
                Boolean.class
        );
        return active != null && active;
    }

    public AdminUserResponse findUserByIdForAdmin(Long userId) {
        String sql = """
            SELECT u.id, u.name, u.email, u.role, u.verified, u.active, u.created_at,
                   (SELECT COUNT(*) FROM ideas i WHERE i.creator_id = u.id) AS idea_count,
                   (SELECT COUNT(*) FROM bids b WHERE b.investor_id = u.id) AS bid_count
            FROM users u
            WHERE u.id = :userId
        """;
        try {
            return jdbcTemplate.queryForObject(sql, Map.of("userId", userId), (rs, rowNum) -> {
                AdminUserResponse user = new AdminUserResponse();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setVerified(rs.getBoolean("verified"));
                user.setActive(rs.getBoolean("active"));
                user.setCreatedAt(toDateTime(rs, "created_at"));
                user.setIdeaCount(rs.getLong("idea_count"));
                user.setBidCount(rs.getLong("bid_count"));
                return user;
            });
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<LeaderStatResponse> findTopCreators(int limit) {
        String sql = """
            SELECT u.id AS user_id, u.name, u.email, COUNT(i.id) AS stat_count
            FROM users u
            INNER JOIN ideas i ON i.creator_id = u.id
            WHERE u.role = 'CREATOR'
            GROUP BY u.id, u.name, u.email
            ORDER BY stat_count DESC
            LIMIT :limit
        """;
        return mapLeaderStats(sql, limit);
    }

    public List<LeaderStatResponse> findTopInvestors(int limit) {
        String sql = """
            SELECT u.id AS user_id, u.name, u.email, COUNT(b.id) AS stat_count
            FROM users u
            INNER JOIN bids b ON b.investor_id = u.id
            WHERE u.role = 'INVESTOR'
            GROUP BY u.id, u.name, u.email
            ORDER BY stat_count DESC
            LIMIT :limit
        """;
        return mapLeaderStats(sql, limit);
    }

    private List<LeaderStatResponse> mapLeaderStats(String sql, int limit) {
        return jdbcTemplate.query(sql, Map.of("limit", limit), (rs, rowNum) ->
                new LeaderStatResponse(
                        rs.getLong("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getLong("stat_count")
                ));
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Map.of(), Long.class);
        return result != null ? result : 0;
    }

    private long count(String sql, Map<String, ?> params) {
        Long result = jdbcTemplate.queryForObject(sql, params, Long.class);
        return result != null ? result : 0;
    }

    private LocalDateTime toDateTime(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        var ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    // Idea Management for Admin
    public List<AdminIdeaResponse> findIdeasForAdmin(String status, String category, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
            SELECT i.id, i.creator_id, u.name AS creator_name, u.email AS creator_email,
                   i.title, i.description, i.category, i.base_price, i.max_budget, i.status, i.created_at,
                   (SELECT COUNT(*) FROM auctions a WHERE a.idea_id = i.id) AS auction_count,
                   (SELECT COUNT(*) FROM auctions a WHERE a.idea_id = i.id AND a.status = 'ACTIVE') AS has_active_auction
            FROM ideas i
            INNER JOIN users u ON u.id = i.creator_id
            WHERE 1=1
        """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);

        if (status != null && !status.isBlank()) {
            sql.append(" AND i.status = :status");
            params.addValue("status", status.toUpperCase());
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND i.category = :category");
            params.addValue("category", category);
        }
        sql.append(" ORDER BY i.created_at DESC LIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            AdminIdeaResponse idea = new AdminIdeaResponse();
            idea.setId(rs.getLong("id"));
            idea.setCreatorId(rs.getLong("creator_id"));
            idea.setCreatorName(rs.getString("creator_name"));
            idea.setCreatorEmail(rs.getString("creator_email"));
            idea.setTitle(rs.getString("title"));
            idea.setDescription(rs.getString("description"));
            idea.setCategory(rs.getString("category"));
            idea.setBasePrice(rs.getObject("base_price", Double.class));
            idea.setMaxBudget(rs.getObject("max_budget", Double.class));
            idea.setStatus(rs.getString("status"));
            idea.setCreatedAt(toDateTime(rs, "created_at"));
            idea.setAuctionCount(rs.getLong("auction_count"));
            idea.setHasActiveAuction(rs.getLong("has_active_auction") > 0);
            return idea;
        });
    }

    public AdminIdeaResponse findIdeaByIdForAdmin(Long ideaId) {
        String sql = """
            SELECT i.id, i.creator_id, u.name AS creator_name, u.email AS creator_email,
                   i.title, i.description, i.category, i.base_price, i.max_budget, i.status, i.created_at,
                   (SELECT COUNT(*) FROM auctions a WHERE a.idea_id = i.id) AS auction_count,
                   (SELECT COUNT(*) FROM auctions a WHERE a.idea_id = i.id AND a.status = 'ACTIVE') AS has_active_auction
            FROM ideas i
            INNER JOIN users u ON u.id = i.creator_id
            WHERE i.id = :ideaId
        """;

        try {
            return jdbcTemplate.queryForObject(sql, Map.of("ideaId", ideaId), (rs, rowNum) -> {
                AdminIdeaResponse idea = new AdminIdeaResponse();
                idea.setId(rs.getLong("id"));
                idea.setCreatorId(rs.getLong("creator_id"));
                idea.setCreatorName(rs.getString("creator_name"));
                idea.setCreatorEmail(rs.getString("creator_email"));
                idea.setTitle(rs.getString("title"));
                idea.setDescription(rs.getString("description"));
                idea.setCategory(rs.getString("category"));
                idea.setBasePrice(rs.getObject("base_price", Double.class));
                idea.setMaxBudget(rs.getObject("max_budget", Double.class));
                idea.setStatus(rs.getString("status"));
                idea.setCreatedAt(toDateTime(rs, "created_at"));
                idea.setAuctionCount(rs.getLong("auction_count"));
                idea.setHasActiveAuction(rs.getLong("has_active_auction") > 0);
                return idea;
            });
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    public long countIdeasForAdmin(String status, String category) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ideas i WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (status != null && !status.isBlank()) {
            sql.append(" AND i.status = :status");
            params.addValue("status", status.toUpperCase());
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND i.category = :category");
            params.addValue("category", category);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0;
    }

    public int updateIdeaStatus(Long ideaId, String status) {
        return jdbcTemplate.update(
                "UPDATE ideas SET status = :status WHERE id = :id",
                Map.of("id", ideaId, "status", status.toUpperCase())
        );
    }

    public int deleteIdea(Long ideaId) {
        return jdbcTemplate.update("DELETE FROM ideas WHERE id = :id", Map.of("id", ideaId));
    }

    // Bid Management for Admin
    public List<AdminBidResponse> findBidsForAdmin(Long auctionId, Long investorId, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
            SELECT b.id, b.auction_id, i.title AS idea_title, b.investor_id,
                   u.name AS investor_name, u.email AS investor_email,
                   b.bid_amount, b.created_at, a.status AS auction_status,
                   (SELECT COUNT(*) + 1 FROM bids b2 WHERE b2.auction_id = b.auction_id AND b2.bid_amount > b.bid_amount) AS rank
            FROM bids b
            INNER JOIN auctions a ON a.id = b.auction_id
            INNER JOIN ideas i ON i.id = a.idea_id
            INNER JOIN users u ON u.id = b.investor_id
            WHERE 1=1
        """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);

        if (auctionId != null) {
            sql.append(" AND b.auction_id = :auctionId");
            params.addValue("auctionId", auctionId);
        }
        if (investorId != null) {
            sql.append(" AND b.investor_id = :investorId");
            params.addValue("investorId", investorId);
        }
        sql.append(" ORDER BY b.created_at DESC LIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            AdminBidResponse bid = new AdminBidResponse();
            bid.setId(rs.getLong("id"));
            bid.setAuctionId(rs.getLong("auction_id"));
            bid.setIdeaTitle(rs.getString("idea_title"));
            bid.setInvestorId(rs.getLong("investor_id"));
            bid.setInvestorName(rs.getString("investor_name"));
            bid.setInvestorEmail(rs.getString("investor_email"));
            bid.setBidAmount(rs.getDouble("bid_amount"));
            bid.setCreatedAt(toDateTime(rs, "created_at"));
            bid.setAuctionStatus(rs.getString("auction_status"));
            bid.setRank(rs.getInt("rank"));
            return bid;
        });
    }

    public long countBidsForAdmin(Long auctionId, Long investorId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM bids b WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (auctionId != null) {
            sql.append(" AND b.auction_id = :auctionId");
            params.addValue("auctionId", auctionId);
        }
        if (investorId != null) {
            sql.append(" AND b.investor_id = :investorId");
            params.addValue("investorId", investorId);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0;
    }

    /**
     * Records an administrative action.
     *
     * <p>Uses {@link MapSqlParameterSource} rather than {@code Map.of}, which throws
     * on null values — platform-wide actions such as a settings change or a search
     * reindex have no entity id, and the IP address can be absent too.</p>
     */
    public void createAuditLog(Long adminId, String action, String entityType, Long entityId, String details, String ipAddress) {
        String sql = """
            INSERT INTO admin_audit_logs (admin_id, action, entity_type, entity_id, details, ip_address)
            VALUES (:adminId, :action, :entityType, :entityId, :details, :ipAddress)
        """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("adminId", adminId)
                .addValue("action", action)
                .addValue("entityType", entityType)
                .addValue("entityId", entityId)
                .addValue("details", details)
                .addValue("ipAddress", ipAddress));
    }

    public List<AdminAuditLogResponse> findAuditLogs(String action, String entityType, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
            SELECT al.id, al.admin_id, u.name AS admin_name, u.email AS admin_email,
                   al.action, al.entity_type, al.entity_id, al.details, al.ip_address, al.created_at
            FROM admin_audit_logs al
            INNER JOIN users u ON u.id = al.admin_id
            WHERE 1=1
        """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);

        if (action != null && !action.isBlank()) {
            sql.append(" AND al.action = :action");
            params.addValue("action", action);
        }
        if (entityType != null && !entityType.isBlank()) {
            sql.append(" AND al.entity_type = :entityType");
            params.addValue("entityType", entityType);
        }
        sql.append(" ORDER BY al.created_at DESC LIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            AdminAuditLogResponse log = new AdminAuditLogResponse();
            log.setId(rs.getLong("id"));
            log.setAdminId(rs.getLong("admin_id"));
            log.setAdminName(rs.getString("admin_name"));
            log.setAdminEmail(rs.getString("admin_email"));
            log.setAction(rs.getString("action"));
            log.setEntityType(rs.getString("entity_type"));
            log.setEntityId(rs.getObject("entity_id", Long.class));
            log.setDetails(rs.getString("details"));
            log.setIpAddress(rs.getString("ip_address"));
            log.setCreatedAt(toDateTime(rs, "created_at"));
            return log;
        });
    }

    public long countAuditLogs(String action, String entityType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM admin_audit_logs WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (action != null && !action.isBlank()) {
            sql.append(" AND action = :action");
            params.addValue("action", action);
        }
        if (entityType != null && !entityType.isBlank()) {
            sql.append(" AND entity_type = :entityType");
            params.addValue("entityType", entityType);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0;
    }

    // Dashboard Charts
    public DashboardChartData getDashboardChartData() {
        DashboardChartData data = new DashboardChartData();
        
        // Ideas by Category
        String categorySql = """
            SELECT category, COUNT(*) AS count
            FROM ideas
            WHERE status != 'ARCHIVED'
            GROUP BY category
            ORDER BY count DESC
        """;
        data.setIdeasByCategory(jdbcTemplate.query(categorySql, Map.of(), (rs, rowNum) -> {
            DashboardChartData.CategoryData item = new DashboardChartData.CategoryData();
            item.setCategory(rs.getString("category"));
            item.setCount(rs.getLong("count"));
            return item;
        }));

        // Auction Status Distribution
        String statusSql = """
            SELECT status, COUNT(*) AS count
            FROM auctions
            GROUP BY status
            ORDER BY count DESC
        """;
        data.setAuctionStatusDistribution(jdbcTemplate.query(statusSql, Map.of(), (rs, rowNum) -> {
            DashboardChartData.StatusData item = new DashboardChartData.StatusData();
            item.setStatus(rs.getString("status"));
            item.setCount(rs.getLong("count"));
            return item;
        }));

        // Monthly Auctions
        String monthlySql = """
            SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS count
            FROM auctions
            WHERE created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH)
            GROUP BY month
            ORDER BY month ASC
        """;
        data.setMonthlyAuctions(jdbcTemplate.query(monthlySql, Map.of(), (rs, rowNum) -> {
            DashboardChartData.MonthlyData item = new DashboardChartData.MonthlyData();
            item.setMonth(rs.getString("month"));
            item.setCount(rs.getLong("count"));
            return item;
        }));

        // Top Investors
        String investorSql = """
            SELECT u.id AS investor_id, u.name AS investor_name,
                   COUNT(b.id) AS bid_count,
                   COALESCE(SUM(b.bid_amount), 0) AS total_investment
            FROM users u
            INNER JOIN bids b ON b.investor_id = u.id
            WHERE u.role = 'INVESTOR'
            GROUP BY u.id, u.name
            ORDER BY total_investment DESC
            LIMIT 10
        """;
        data.setTopInvestors(jdbcTemplate.query(investorSql, Map.of(), (rs, rowNum) -> {
            DashboardChartData.TopInvestorData item = new DashboardChartData.TopInvestorData();
            item.setInvestorId(rs.getLong("investor_id"));
            item.setInvestorName(rs.getString("investor_name"));
            item.setBidCount(rs.getLong("bid_count"));
            item.setTotalInvestment(rs.getDouble("total_investment"));
            return item;
        }));

        return data;
    }

    // Auction Settings
    public void saveAuctionSettings(Long ideaId, Double minBid, Double reservePrice, String description) {
        String sql = """
            INSERT INTO auction_settings (idea_id, min_bid, reserve_price, description)
            VALUES (:ideaId, :minBid, :reservePrice, :description)
            ON DUPLICATE KEY UPDATE
                min_bid = :minBid,
                reserve_price = :reservePrice,
                description = :description,
                updated_at = CURRENT_TIMESTAMP
        """;
        jdbcTemplate.update(sql, Map.of(
                "ideaId", ideaId,
                "minBid", minBid,
                "reservePrice", reservePrice,
                "description", description
        ));
    }

    public Map<String, Object> getAuctionSettings(Long ideaId) {
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT * FROM auction_settings WHERE idea_id = :ideaId",
                    Map.of("ideaId", ideaId)
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    // Check for duplicate active auction
    public boolean hasActiveAuctionForIdea(Long ideaId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auctions WHERE idea_id = :ideaId AND status = 'ACTIVE'",
                Map.of("ideaId", ideaId),
                Long.class
        );
        return count != null && count > 0;
    }

    // Update auction status
    public int updateAuctionStatus(Long auctionId, String status) {
        return jdbcTemplate.update(
                "UPDATE auctions SET status = :status WHERE id = :id",
                Map.of("id", auctionId, "status", status.toUpperCase())
        );
    }

    // Update auction details
    public int updateAuctionDetails(Long auctionId, LocalDateTime startTime, LocalDateTime endTime, Double minBidIncrement) {
        StringBuilder sql = new StringBuilder("UPDATE auctions SET ");
        MapSqlParameterSource params = new MapSqlParameterSource("id", auctionId);
        java.util.List<String> updates = new java.util.ArrayList<>();

        if (startTime != null) {
            updates.add("start_time = :startTime");
            params.addValue("startTime", startTime);
        }
        if (endTime != null) {
            updates.add("end_time = :endTime");
            params.addValue("endTime", endTime);
        }
        if (minBidIncrement != null) {
            updates.add("min_bid_increment = :minBidIncrement");
            params.addValue("minBidIncrement", minBidIncrement);
        }

        if (updates.isEmpty()) {
            return 0;
        }

        sql.append(String.join(", ", updates)).append(" WHERE id = :id");
        return jdbcTemplate.update(sql.toString(), params);
    }

    /**
     * Revokes every outstanding refresh token for a user. Called when an admin
     * suspends an account so existing sessions cannot be silently refreshed.
     */
    public int revokeRefreshTokensForUser(Long userId) {
        return jdbcTemplate.update(
                "UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = :userId AND revoked = FALSE",
                Map.of("userId", userId)
        );
    }

    /** Auctions that produced a winner, newest first, for the admin winners report. */
    public List<AuctionWinnerSummaryResponse> findWinners(int limit, int offset) {
        String sql = """
            SELECT aw.auction_id, aw.winner_id, aw.winning_bid, aw.created_at AS decided_at,
                   w.name AS winner_name, w.email AS winner_email,
                   i.id AS idea_id, i.title AS idea_title, i.category AS idea_category,
                   c.id AS creator_id, c.name AS creator_name,
                   a.end_time,
                   (SELECT COUNT(*) FROM bids b WHERE b.auction_id = aw.auction_id) AS bid_count
            FROM auction_winners aw
            INNER JOIN auctions a ON a.id = aw.auction_id
            INNER JOIN ideas i ON i.id = a.idea_id
            INNER JOIN users w ON w.id = aw.winner_id
            INNER JOIN users c ON c.id = i.creator_id
            ORDER BY aw.created_at DESC, aw.auction_id DESC
            LIMIT :limit OFFSET :offset
        """;

        return jdbcTemplate.query(sql, Map.of("limit", limit, "offset", offset), (rs, rowNum) -> {
            AuctionWinnerSummaryResponse response = new AuctionWinnerSummaryResponse();
            response.setAuctionId(rs.getLong("auction_id"));
            response.setIdeaId(rs.getLong("idea_id"));
            response.setIdeaTitle(rs.getString("idea_title"));
            response.setIdeaCategory(rs.getString("idea_category"));
            response.setCreatorId(rs.getLong("creator_id"));
            response.setCreatorName(rs.getString("creator_name"));
            response.setWinnerId(rs.getLong("winner_id"));
            response.setWinnerName(rs.getString("winner_name"));
            response.setWinnerEmail(rs.getString("winner_email"));
            response.setWinningBid(rs.getDouble("winning_bid"));
            response.setBidCount(rs.getInt("bid_count"));
            response.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
            response.setDecidedAt(toLocalDateTime(rs.getTimestamp("decided_at")));
            return response;
        });
    }

    public long countWinners() {
        return count("SELECT COUNT(*) FROM auction_winners");
    }

    /**
     * Current auction status per idea, for rebuilding the search index in one pass
     * instead of querying per document.
     */
    public Map<Long, String> findLatestAuctionStatusByIdea() {
        String sql = """
            SELECT a.idea_id, a.status
            FROM auctions a
            INNER JOIN (
                SELECT idea_id, MAX(id) AS latest_id
                FROM auctions
                GROUP BY idea_id
            ) newest ON newest.latest_id = a.id
        """;

        Map<Long, String> statuses = new java.util.HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            statuses.put(rs.getLong("idea_id"), rs.getString("status"));
        });
        return statuses;
    }

    private static LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
