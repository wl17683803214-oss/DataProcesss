-- 允许同一任务、同一设备内的遥测代号重复，序号仍保持唯一。
BEGIN;
DROP INDEX IF EXISTS uk_parse_rule_task_device_code;
CREATE INDEX IF NOT EXISTS idx_parse_rule_task_device_code
    ON telemetry_parse_rule_config (task_id, device_satellite_id, telemetry_code) WHERE is_deleted = 0;
COMMENT ON COLUMN telemetry_parse_rule_config.telemetry_code IS '遥测代号，允许重复；同帧按序号保留最后一项';
COMMIT;
