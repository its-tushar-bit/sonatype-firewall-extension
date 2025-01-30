-- Since 1.187
-- SaaS Compatible

CREATE TABLE roi_configuration (
  roi_configuration_id varchar(50) NOT NULL,
  currency char(3) NOT NULL,
  developer_hourly_rate numeric,
  fix_rate_hours int,
  security_violation_critical_enabled boolean,
  security_violation_critical_value numeric,
  security_violation_high_enabled boolean,
  security_violation_high_value numeric,
  security_violation_medium_enabled boolean,
  security_violation_medium_value numeric,
  security_violation_low_enabled boolean,
  security_violation_low_value numeric,
  supply_chain_attacks_blocked_value numeric,
  namespace_attacks_blocked_value numeric,
  safe_components_auto_selected_value numeric,
  waived_policies_counted boolean,

  CONSTRAINT roi_configuration_id_pk PRIMARY KEY (roi_configuration_id),
  CONSTRAINT roi_configuration_currency_uk UNIQUE (currency)
);

CREATE TABLE roi_configuration_default_values (
  roi_configuration_default_values_id varchar(50) NOT NULL,
  currency char(3) NOT NULL,
  developer_hourly_rate_default_value numeric,
  developer_hourly_rate_minimum_value numeric,
  fix_rate_hours_default_value int,
  fix_rate_hours_minimum_value int,
  security_violation_critical_default_value numeric,
  security_violation_critical_minimum_value numeric,
  security_violation_critical_enabled boolean,
  security_violation_high_default_value numeric,
  security_violation_high_minimum_value numeric,
  security_violation_high_enabled boolean,
  security_violation_medium_default_value numeric,
  security_violation_medium_minimum_value numeric,
  security_violation_medium_enabled boolean,
  security_violation_low_default_value numeric,
  security_violation_low_minimum_value numeric,
  security_violation_low_enabled boolean,
  supply_chain_attacks_blocked_default_value numeric,
  supply_chain_attacks_blocked_minimum_value numeric,
  namespace_attacks_blocked_default_value numeric,
  namespace_attacks_blocked_minimum_value numeric,
  safe_components_auto_selected_default_value numeric,
  safe_components_auto_selected_minimum_value numeric,
  waived_policies_counted boolean,

  CONSTRAINT roi_configuration_default_values_id_pk PRIMARY KEY (roi_configuration_default_values_id),
  CONSTRAINT roi_configuration_default_values_currency_uk UNIQUE (currency)
);

INSERT INTO roi_configuration_default_values(
  roi_configuration_default_values_id,
  currency,
  developer_hourly_rate_default_value,
  developer_hourly_rate_minimum_value,
  fix_rate_hours_default_value,
  fix_rate_hours_minimum_value,
  security_violation_critical_default_value,
  security_violation_critical_minimum_value,
  security_violation_critical_enabled,
  security_violation_high_default_value,
  security_violation_high_minimum_value,
  security_violation_high_enabled,
  security_violation_medium_default_value,
  security_violation_medium_minimum_value,
  security_violation_medium_enabled,
  security_violation_low_default_value,
  security_violation_low_minimum_value,
  security_violation_low_enabled,
  supply_chain_attacks_blocked_default_value,
  supply_chain_attacks_blocked_minimum_value,
  namespace_attacks_blocked_default_value,
  namespace_attacks_blocked_minimum_value,
  safe_components_auto_selected_default_value,
  safe_components_auto_selected_minimum_value,
  waived_policies_counted
) VALUES ('dd97d70ae24a446ab65c525b6f2b4786', 'USD', 100, 50, 3600,  1440, 12000, 6000, true, 24000, 12000, true, 72000, 36000, false, 144000, 72000, false, 4350000, 500000, 35000, 10000, 25000, 5000, false);
