-- SaaS Compatible
CREATE INDEX IF NOT EXISTS evaluation_queue_priority_worker_id_null_idx ON evaluation_queue (priority) WHERE worker_id IS NULL;
CREATE INDEX IF NOT EXISTS evaluation_queue_worker_id_update_time_worker_id_not_null_idx ON evaluation_queue (worker_id, update_time DESC) WHERE worker_id IS NOT NULL;
