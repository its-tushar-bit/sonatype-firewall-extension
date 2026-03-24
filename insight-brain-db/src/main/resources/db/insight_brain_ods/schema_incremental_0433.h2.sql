-- SaaS Compatible
CREATE INDEX IF NOT EXISTS evaluation_queue_worker_id_priority_idx ON evaluation_queue (worker_id, priority);
CREATE INDEX IF NOT EXISTS evaluation_queue_worker_id_update_time_idx ON evaluation_queue (worker_id, update_time DESC);
