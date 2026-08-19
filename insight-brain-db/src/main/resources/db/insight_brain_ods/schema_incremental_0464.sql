-- SaaS Compatible
-- Composite (scan_id, application_id) index supporting the LEFT JOIN in
-- ConsumptionEventDAO.historyByStageByWindows. See that method for the
-- fan-out rationale and why both columns are required in the JOIN.

CREATE INDEX IF NOT EXISTS policy_evaluation_scan_app_idx
  ON policy_evaluation (scan_id, application_id);
