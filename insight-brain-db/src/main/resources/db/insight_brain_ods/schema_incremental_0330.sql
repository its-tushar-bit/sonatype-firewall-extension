-- Since 1.172
-- SaaS Compatible
CREATE TABLE call_flow_analysis_config (
   call_flow_analysis_config_id varchar(50) NOT NULL,
   enabled boolean NOT NULL,
   namespaces_json text NULL,
   algorithm varchar(255),
   thread_count int,
   owner_id varchar(50) NOT NULL,
   CONSTRAINT call_flow_analysis_config_id_pk PRIMARY KEY (call_flow_analysis_config_id),
   CONSTRAINT call_flow_analysis_config_owner_uk UNIQUE (owner_id)
);
