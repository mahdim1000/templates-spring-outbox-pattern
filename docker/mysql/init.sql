-- MySQL initialization script for Enterprise Outbox Pattern

-- Create indexes for optimal performance
CREATE INDEX IF NOT EXISTS idx_outbox_status_next_retry ON outbox(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_version ON outbox(aggregate_id, version);
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON outbox(created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox(status, created_at);

-- Create a view for monitoring
CREATE OR REPLACE VIEW outbox_stats AS
SELECT 
    status,
    COUNT(*) as count,
    MIN(created_at) as oldest_message,
    MAX(created_at) as newest_message,
    AVG(retry_count) as avg_retry_count
FROM outbox 
GROUP BY status;

-- Grant additional privileges for monitoring
GRANT SELECT ON outbox_stats TO 'outbox_user'@'%'; 