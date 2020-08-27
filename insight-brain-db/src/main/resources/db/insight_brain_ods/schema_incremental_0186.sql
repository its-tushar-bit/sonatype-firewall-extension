-- Since 1.98
CREATE TABLE persisted_user_session (
  persisted_user_session_id varchar(50) NOT NULL,
  session_json text NOT NULL,
  CONSTRAINT persisted_user_session_pk PRIMARY KEY (persisted_user_session_id)
);
