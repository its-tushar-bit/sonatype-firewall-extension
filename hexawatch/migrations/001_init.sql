CREATE TABLE IF NOT EXISTS extension_config (
  user_code           varchar(255) PRIMARY KEY,
  pass_code           varchar(1024),
  iq_server_url       varchar(1024),
  vrm_id              varchar(64),
  vrm_name            varchar(255),
  selected_repo_ids   jsonb        NOT NULL DEFAULT '[]'::jsonb,
  created_at          timestamptz  NOT NULL DEFAULT now(),
  updated_at          timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS extension_config_updated_at_idx
  ON extension_config (updated_at DESC);
