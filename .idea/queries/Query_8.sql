SELECT
    u.id    AS userId,
    u.email AS email,
    COUNT(t.id) FILTER (WHERE t.status <> 'DONE') AS pendingCount,
    COUNT(t.id) FILTER (
        WHERE t.status = 'DONE'
            AND t.updated_at >= CURRENT_DATE
            AND t.updated_at <  CURRENT_DATE + INTERVAL '1 day'
        ) AS completedCount
FROM users u
         LEFT JOIN tasks t ON t.user_id = u.id
GROUP BY u.id, u.email;
