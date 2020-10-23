-- Since 1.101
DROP TABLE persisted_promote_scan_result;

DELETE FROM QRTZ_SIMPLE_TRIGGERS WHERE TRIGGER_NAME='PersistedPromoteScanResultCleaner';
DELETE FROM QRTZ_TRIGGERS WHERE TRIGGER_NAME='PersistedPromoteScanResultCleaner';
DELETE FROM QRTZ_JOB_DETAILS WHERE JOB_CLASS_NAME='com.sonatype.insight.brain.api.v2.service.PersistedPromoteScanResultCleaner';
