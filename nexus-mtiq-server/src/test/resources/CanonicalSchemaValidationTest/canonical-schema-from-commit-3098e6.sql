--
-- This is a dump file with the baseline canonical schema for an MTIQ tenant. This was obtained from a tenant
-- created using the IQ 166-SNAPSHOT version(https://github.com/sonatype/insight-brain/commit/3098e603667094c408ff6150b463a3fa31e2a42e)
-- and reverting the changes introduced by the scripts' schema_incremental_0306(https://github.com/sonatype/insight-brain/commit/20c13389d0a6590e2659c6a537eaeab7ca0a9a57),
-- schema_incremental_0305(https://github.com/sonatype/insight-brain/commit/fb2a446dab49daaa0ea6b3b9b2e6093fb9640a04) and
-- schema_incremental_0304(https://github.com/sonatype/insight-brain/commit/3fbed9cea32f5a80ec8b7b627346a795422d801e#diff-476a73dd1ec5e25093c5bd00bfb10a42262331fb888f1d9bb76531958e29dc96).
-- To be more precise, the schema versions for the different data stores are the next.
--  | Data Store                      | Version |
--  | insight_brain_third_party_scans | 13      |
--  | insight_brain_aggregation       | 13      |
--  | insight_brain_ods               | 303     |
--
-- IMPORTANT NOTES:
-- * For an MTIQ tenant, the insight_brain_third_party_scans, insight_brain_aggregation and insight_brain_ods,
-- data stores are part of the same schema, that means that each tenant will have the scripts of those
-- three data stores. The insight_brain_dm data store is considered global data and is part of the 'global' tenant,
-- so you won't see any table from that data store in this file
-- * Keep in mind this canonical schema should not be modified unless there is a good reason for that. This will check
-- that all schemas for tenants migrated from schema version <strong>303</strong> to the latest schema version are
-- are equal to the schema of any new tenant.
--

--
-- PostgreSQL database dump
--

-- Dumped from database version 14.0
-- Dumped by pg_dump version 14.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: t_TENANT; Type: SCHEMA; Schema: -; Owner: testuser
--

CREATE SCHEMA t_TENANT;


ALTER SCHEMA t_TENANT OWNER TO testuser;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: aggregate_file; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.aggregate_file (
                                         aggregate_file_id character varying(50) NOT NULL,
                                         application_component_id character varying(50) NOT NULL,
                                         hash character varying(20) NOT NULL,
                                         pathnames text
);


ALTER TABLE t_TENANT.aggregate_file OWNER TO testuser;

--
-- Name: application; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.application (
                                      application_id character varying(50) NOT NULL,
                                      public_id character varying(200) NOT NULL,
                                      public_id_lowercase character varying(200) NOT NULL,
                                      name character varying(200) NOT NULL,
                                      name_lowercase_no_whitespace character varying(200) NOT NULL,
                                      organization_id character varying(50) NOT NULL,
                                      contact_internal_name character varying(60),
                                      policy_violation_grandfathering_enabled boolean,
                                      repository_connection_enabled boolean DEFAULT false,
                                      artifactory_connection_enabled boolean
);


ALTER TABLE t_TENANT.application OWNER TO testuser;

--
-- Name: application_component; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.application_component (
                                                application_component_id character varying(50) NOT NULL,
                                                application_id character varying(50) NOT NULL,
                                                stage_type_id character varying(30) NOT NULL,
                                                "time" timestamp without time zone NOT NULL,
                                                hash character varying(20) NOT NULL,
                                                component_id_format character varying(50),
                                                component_id_coordinates_json character varying(1000),
                                                match_state_id character varying(20) NOT NULL,
                                                identification_source_id character varying(20) NOT NULL,
                                                proprietary boolean DEFAULT false NOT NULL,
                                                pathnames text
);


ALTER TABLE t_TENANT.application_component OWNER TO testuser;

--
-- Name: application_component_license; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.application_component_license (
                                                        application_component_license_id character varying(50) NOT NULL,
                                                        application_component_id character varying(50) NOT NULL,
                                                        effective_license_id character varying(1000) NOT NULL
);


ALTER TABLE t_TENANT.application_component_license OWNER TO testuser;

--
-- Name: application_tag; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.application_tag (
                                          application_tag_id character varying(50) NOT NULL,
                                          application_id character varying(50) NOT NULL,
                                          tag_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.application_tag OWNER TO testuser;

--
-- Name: artifactory_connection; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.artifactory_connection (
                                                 artifactory_connection_id character varying(50) NOT NULL,
                                                 owner_id character varying(50) NOT NULL,
                                                 base_url character varying(2048) NOT NULL,
                                                 username character varying(255),
                                                 password text
);


ALTER TABLE t_TENANT.artifactory_connection OWNER TO testuser;

--
-- Name: attribution_report_template; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.attribution_report_template (
                                                      attribution_report_template_id character varying(50) NOT NULL,
                                                      template_name character varying(250) NOT NULL,
                                                      document_title character varying(250) NOT NULL,
                                                      document_header character varying(500),
                                                      document_footer character varying(500),
                                                      include_table_of_contents boolean DEFAULT true,
                                                      include_standard_license_texts boolean DEFAULT true,
                                                      include_appendix boolean DEFAULT true,
                                                      include_inner_source boolean DEFAULT false,
                                                      include_sonatype_special_licenses boolean DEFAULT false,
                                                      last_updated_at timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.attribution_report_template OWNER TO testuser;

--
-- Name: auto_unquarantine_policy_condition_type; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.auto_unquarantine_policy_condition_type (
    condition_type_id character varying(100) NOT NULL
);


ALTER TABLE t_TENANT.auto_unquarantine_policy_condition_type OWNER TO testuser;

--
-- Name: component_copyright; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.component_copyright (
                                              component_copyright_id character varying(50) NOT NULL,
                                              component_id_format character varying(10) NOT NULL,
                                              component_id_coordinates_json character varying(1000) NOT NULL,
                                              owner_id character varying(50) NOT NULL,
                                              legal_content_hash character varying(64) NOT NULL,
                                              last_updated_by_username character varying(256) NOT NULL,
                                              last_updated_at timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.component_copyright OWNER TO testuser;

--
-- Name: component_label; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.component_label (
                                          component_label_id character varying(50) NOT NULL,
                                          owner_id character varying(50) NOT NULL,
                                          label_id character varying(50) NOT NULL,
                                          hash character varying(20) NOT NULL
);


ALTER TABLE t_TENANT.component_label OWNER TO testuser;

--
-- Name: component_legal_file; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.component_legal_file (
                                               component_legal_file_id character varying(50) NOT NULL,
                                               component_id_format character varying(10) NOT NULL,
                                               component_id_coordinates_json character varying(1000) NOT NULL,
                                               owner_id character varying(50) NOT NULL,
                                               type character varying(20) NOT NULL,
                                               legal_content_hash character varying(64) NOT NULL,
                                               last_updated_by_username character varying(256) NOT NULL,
                                               last_updated_at timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.component_legal_file OWNER TO testuser;

--
-- Name: component_obligation; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.component_obligation (
                                               component_obligation_id character varying(50) NOT NULL,
                                               component_id_format character varying(10) NOT NULL,
                                               component_id_coordinates_json character varying(1000) NOT NULL,
                                               owner_id character varying(50) NOT NULL,
                                               obligation_name character varying(256) NOT NULL,
                                               comment character varying(1000),
                                               status character varying(20) NOT NULL,
                                               legal_content_hash character varying(64) NOT NULL,
                                               last_updated_by_username character varying(256) NOT NULL,
                                               last_updated_at timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.component_obligation OWNER TO testuser;

--
-- Name: component_obligation_attribution; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.component_obligation_attribution (
                                                           component_obligation_attribution_id character varying(50) NOT NULL,
                                                           component_id_format character varying(10) NOT NULL,
                                                           component_id_coordinates_json character varying(1000) NOT NULL,
                                                           owner_id character varying(50) NOT NULL,
                                                           obligation_name character varying(256),
                                                           content character varying(1000) NOT NULL,
                                                           legal_content_hash character varying(64) NOT NULL,
                                                           last_updated_by_username character varying(256) NOT NULL,
                                                           last_updated_at timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.component_obligation_attribution OWNER TO testuser;

--
-- Name: component_source_link; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.component_source_link (
                                                component_source_link_id character varying(50) NOT NULL,
                                                component_id_format character varying(10) NOT NULL,
                                                component_id_coordinates_json character varying(1000) NOT NULL,
                                                owner_id character varying(50) NOT NULL,
                                                last_updated_by_username character varying(256) NOT NULL,
                                                last_updated_at timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.component_source_link OWNER TO testuser;

--
-- Name: coordinate_license; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.coordinate_license (
                                             coordinate_license_id character varying(50) NOT NULL,
                                             file_coordinate_id character varying(50) NOT NULL,
                                             license_id character varying(50) NOT NULL,
                                             name character varying(50),
                                             url character varying(200)
);


ALTER TABLE t_TENANT.coordinate_license OWNER TO testuser;

--
-- Name: coordinate_security; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.coordinate_security (
                                              coordinate_security_id character varying(50) NOT NULL,
                                              file_coordinate_id character varying(50) NOT NULL,
                                              ref_id character varying(20) NOT NULL,
                                              description text,
                                              link character varying(200),
                                              severity double precision NOT NULL,
                                              fixed_by character varying(200),
                                              vulnerability_source character varying(15),
                                              severity_description character varying(15),
                                              attack_vector character varying(100),
                                              rating_method character varying(10),
                                              cwes text,
                                              recommendations text,
                                              advisories text
);


ALTER TABLE t_TENANT.coordinate_security OWNER TO testuser;

--
-- Name: copyright_override; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.copyright_override (
                                             copyright_override_id character varying(50) NOT NULL,
                                             original_content_hash character varying(64),
                                             content_hash character varying(64) NOT NULL,
                                             content character varying(1000) NOT NULL,
                                             status character varying(20) NOT NULL,
                                             component_copyright_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.copyright_override OWNER TO testuser;

--
-- Name: crowd_configuration; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.crowd_configuration (
                                              crowd_configuration_id character varying(50) NOT NULL,
                                              server_url character varying(2048) NOT NULL,
                                              application_name character varying(255) NOT NULL,
                                              application_password character varying(255) NOT NULL
);


ALTER TABLE t_TENANT.crowd_configuration OWNER TO testuser;

--
-- Name: dashboard_filter; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.dashboard_filter (
                                           dashboard_filter_id character varying(50) NOT NULL,
                                           username character varying(60) NOT NULL,
                                           username_lowercase character varying(60) NOT NULL,
                                           realm_id character varying(50),
                                           name character varying(60) NOT NULL,
                                           name_lowercase_no_whitespace character varying(60) NOT NULL,
                                           based_on_filter_name character varying(60),
                                           acknowledged boolean DEFAULT false NOT NULL,
                                           filter_json text NOT NULL
);


ALTER TABLE t_TENANT.dashboard_filter OWNER TO testuser;

--
-- Name: data_retention_policy; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.data_retention_policy (
                                                data_retention_policy_id character varying(50) NOT NULL,
                                                owner_id character varying(50) NOT NULL,
                                                context_id character varying(30) NOT NULL,
                                                purging_enabled boolean NOT NULL,
                                                max_count smallint,
                                                max_age_in_days smallint
);


ALTER TABLE t_TENANT.data_retention_policy OWNER TO testuser;

--
-- Name: deleted_tenant; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.deleted_tenant (
                                         tenant_slug character varying(61) NOT NULL,
                                         delete_requested_timestamp bigint NOT NULL
);


ALTER TABLE t_TENANT.deleted_tenant OWNER TO testuser;

--
-- Name: file_coordinate; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.file_coordinate (
                                          file_coordinate_id character varying(50) NOT NULL,
                                          hash character varying(20) NOT NULL,
                                          source character varying(100) NOT NULL,
                                          format character varying(50) NOT NULL,
                                          name character varying(300) NOT NULL,
                                          version character varying(200) NOT NULL,
                                          third_party_file_id character varying(50) NOT NULL,
                                          package_url character varying(1000)
);


ALTER TABLE t_TENANT.file_coordinate OWNER TO testuser;

--
-- Name: hash_component_identifier; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.hash_component_identifier (
                                                    hash_component_identifier_id character varying(50) NOT NULL,
                                                    hash character varying(20) NOT NULL,
                                                    component_id_format character varying(10) NOT NULL,
                                                    component_id_coordinates_json character varying(1000) NOT NULL,
                                                    comment character varying(1000),
                                                    create_time timestamp without time zone
);


ALTER TABLE t_TENANT.hash_component_identifier OWNER TO testuser;

--
-- Name: inner_source_component; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.inner_source_component (
                                                 inner_source_component_id character varying(50) NOT NULL,
                                                 application_id character varying(50) NOT NULL,
                                                 package_url character varying(1000) NOT NULL,
                                                 latest_version character varying(200)
);


ALTER TABLE t_TENANT.inner_source_component OWNER TO testuser;

--
-- Name: jira_configuration; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.jira_configuration (
                                             jira_configuration_id character varying(50) NOT NULL,
                                             url character varying(2048) NOT NULL,
                                             username character varying(255),
                                             password character varying(2000),
                                             custom_fields_json character varying(8192)
);


ALTER TABLE t_TENANT.jira_configuration OWNER TO testuser;

--
-- Name: label; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.label (
                                label_id character varying(50) NOT NULL,
                                owner_id character varying(50) NOT NULL,
                                label character varying(50) NOT NULL,
                                label_lowercase character varying(50) NOT NULL,
                                color character varying(20) NOT NULL,
                                description character varying(255)
);


ALTER TABLE t_TENANT.label OWNER TO testuser;

--
-- Name: last_policy_evaluation; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.last_policy_evaluation (
                                                 policy_evaluation_id character varying(50) NOT NULL,
                                                 application_id character varying(50) NOT NULL,
                                                 stage_type_id character varying(30) NOT NULL
);


ALTER TABLE t_TENANT.last_policy_evaluation OWNER TO testuser;

--
-- Name: ldap_connection; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.ldap_connection (
                                          ldap_connection_id character varying(50) NOT NULL,
                                          ldap_server_id character varying(50) NOT NULL,
                                          protocol character varying(5) NOT NULL,
                                          hostname character varying(255) NOT NULL,
                                          port integer NOT NULL,
                                          search_base character varying(255),
                                          referral_ignored boolean DEFAULT false NOT NULL,
                                          authentication_method character varying(10) NOT NULL,
                                          sasl_realm character varying(255),
                                          system_username character varying(255),
                                          system_password character varying(255),
                                          connection_timeout smallint,
                                          retry_delay smallint
);


ALTER TABLE t_TENANT.ldap_connection OWNER TO testuser;

--
-- Name: ldap_server; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.ldap_server (
                                      ldap_server_id character varying(50) NOT NULL,
                                      name character varying(60) NOT NULL,
                                      name_lowercase_no_whitespace character varying(60) NOT NULL,
                                      priority integer NOT NULL
);


ALTER TABLE t_TENANT.ldap_server OWNER TO testuser;

--
-- Name: ldap_usermapping; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.ldap_usermapping (
                                           ldap_usermapping_id character varying(50) NOT NULL,
                                           ldap_server_id character varying(50) NOT NULL,
                                           user_basedn character varying(255),
                                           user_subtree boolean NOT NULL,
                                           user_object_class character varying(255) NOT NULL,
                                           user_filter character varying(255),
                                           user_id_attribute character varying(255) NOT NULL,
                                           user_realname_attribute character varying(255) NOT NULL,
                                           user_email_attribute character varying(255) NOT NULL,
                                           user_password_attribute character varying(255),
                                           group_mapping_type character varying(10) NOT NULL,
                                           group_basedn character varying(255),
                                           group_subtree boolean NOT NULL,
                                           group_object_class character varying(255),
                                           group_id_attribute character varying(255),
                                           group_member_attribute character varying(255),
                                           group_member_format character varying(255),
                                           user_memberofgroup_attribute character varying(255),
                                           dynamic_group_search_enabled boolean DEFAULT true NOT NULL
);


ALTER TABLE t_TENANT.ldap_usermapping OWNER TO testuser;

--
-- Name: legal_file_override; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.legal_file_override (
                                              legal_file_override_id character varying(50) NOT NULL,
                                              original_content_hash character varying(64),
                                              content_hash character varying(64) NOT NULL,
                                              content text NOT NULL,
                                              status character varying(20) NOT NULL,
                                              component_legal_file_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.legal_file_override OWNER TO testuser;

--
-- Name: license_override; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.license_override (
                                           license_override_id character varying(50) NOT NULL,
                                           owner_id character varying(50) NOT NULL,
                                           component_id_format character varying(10) NOT NULL,
                                           component_id_coordinates_json character varying(1000) NOT NULL,
                                           status character varying(20) NOT NULL,
                                           comment character varying(1000)
);


ALTER TABLE t_TENANT.license_override OWNER TO testuser;

--
-- Name: license_override_license; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.license_override_license (
                                                   license_override_license_id character varying(50) NOT NULL,
                                                   license_override_id character varying(50) NOT NULL,
                                                   license_id character varying(1000) NOT NULL
);


ALTER TABLE t_TENANT.license_override_license OWNER TO testuser;

--
-- Name: license_threat_group; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.license_threat_group (
                                               license_threat_group_id character varying(50) NOT NULL,
                                               owner_id character varying(50) NOT NULL,
                                               name character varying(60) NOT NULL,
                                               name_lowercase_no_whitespace character varying(60) NOT NULL,
                                               threat_level smallint NOT NULL
);


ALTER TABLE t_TENANT.license_threat_group OWNER TO testuser;

--
-- Name: license_threat_group_license; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.license_threat_group_license (
                                                       license_threat_group_license_id character varying(50) NOT NULL,
                                                       owner_id character varying(50) NOT NULL,
                                                       license_threat_group_id character varying(50) NOT NULL,
                                                       license_id character varying(1000) NOT NULL
);


ALTER TABLE t_TENANT.license_threat_group_license OWNER TO testuser;

--
-- Name: lock; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.lock (
    lock_id character varying(1100) NOT NULL
);


ALTER TABLE t_TENANT.lock OWNER TO testuser;

--
-- Name: mail_configuration; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.mail_configuration (
                                             mail_configuration_id character varying(50) NOT NULL,
                                             hostname character varying(255) NOT NULL,
                                             port integer NOT NULL,
                                             username character varying(255),
                                             password character varying(255),
                                             ssl_enabled boolean NOT NULL,
                                             start_tls_enabled boolean NOT NULL,
                                             system_email character varying(255) NOT NULL
);


ALTER TABLE t_TENANT.mail_configuration OWNER TO testuser;

--
-- Name: membership_mapping; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.membership_mapping (
                                             membership_mapping_id character varying(50) NOT NULL,
                                             context_id character varying(50) NOT NULL,
                                             role_id character varying(50) NOT NULL,
                                             member_name character varying(200) NOT NULL,
                                             member_type character varying(20) NOT NULL
);


ALTER TABLE t_TENANT.membership_mapping OWNER TO testuser;

--
-- Name: migration_tracker; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.migration_tracker (
                                            migration_tracker_id character varying(100) NOT NULL,
                                            version integer,
                                            configuration character varying(1000)
);


ALTER TABLE t_TENANT.migration_tracker OWNER TO testuser;

--
-- Name: organization; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.organization (
                                       organization_id character varying(50) NOT NULL,
                                       parent_organization_id character varying(50),
                                       name character varying(200) NOT NULL,
                                       name_lowercase_no_whitespace character varying(200) NOT NULL,
                                       policy_violation_grandfathering_enabled boolean,
                                       allow_policy_violation_grandfathering_override boolean DEFAULT true NOT NULL,
                                       repository_connection_enabled boolean DEFAULT false,
                                       allow_repository_connection_override boolean DEFAULT true NOT NULL,
                                       artifactory_connection_enabled boolean,
                                       allow_artifactory_connection_override boolean DEFAULT true NOT NULL
);


ALTER TABLE t_TENANT.organization OWNER TO testuser;

--
-- Name: perpetual_lock; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.perpetual_lock (
                                         perpetual_lock_id character varying(1100) NOT NULL,
                                         owner character varying(50),
                                         expiration_time timestamp without time zone
);


ALTER TABLE t_TENANT.perpetual_lock OWNER TO testuser;

--
-- Name: persisted_policy_evaluation_polling_result; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.persisted_policy_evaluation_polling_result (
                                                                     persisted_policy_evaluation_polling_result_id character varying(50) NOT NULL,
                                                                     application_id character varying(50) NOT NULL,
                                                                     status_id character varying(50) NOT NULL,
                                                                     policy_evaluation_polling_result_json text NOT NULL,
                                                                     create_time timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.persisted_policy_evaluation_polling_result OWNER TO testuser;

--
-- Name: persisted_promote_scan_result; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.persisted_promote_scan_result (
                                                        persisted_promote_scan_result_id character varying(50) NOT NULL,
                                                        application_id character varying(50) NOT NULL,
                                                        status character varying(50) NOT NULL,
                                                        scan_id character varying(50),
                                                        error_message character varying(1000),
                                                        create_time timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.persisted_promote_scan_result OWNER TO testuser;

--
-- Name: persisted_scan_ticket; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.persisted_scan_ticket (
                                                persisted_scan_ticket_id character varying(50) NOT NULL,
                                                application_id character varying(50) NOT NULL,
                                                scan_id character varying(50),
                                                state_id character varying(50) NOT NULL,
                                                error_id character varying(50),
                                                create_time timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.persisted_scan_ticket OWNER TO testuser;

--
-- Name: persisted_user_session; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.persisted_user_session (
                                                 persisted_user_session_id character varying(50) NOT NULL,
                                                 session_json text NOT NULL
);


ALTER TABLE t_TENANT.persisted_user_session OWNER TO testuser;

--
-- Name: policy; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.policy (
                                 policy_id character varying(50) NOT NULL,
                                 owner_id character varying(50) NOT NULL,
                                 name character varying(60) NOT NULL,
                                 name_lowercase_no_whitespace character varying(60) NOT NULL,
                                 threat_level smallint NOT NULL,
                                 policy_violation_grandfathering_allowed boolean NOT NULL,
                                 content text NOT NULL,
                                 drools_code text NOT NULL
);


ALTER TABLE t_TENANT.policy OWNER TO testuser;

--
-- Name: policy_evaluation; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.policy_evaluation (
                                            policy_evaluation_id character varying(50) NOT NULL,
                                            application_id character varying(50) NOT NULL,
                                            stage_type_id character varying(30) NOT NULL,
                                            scan_id character varying(50) NOT NULL,
                                            reevaluation boolean DEFAULT false NOT NULL,
                                            for_monitoring boolean DEFAULT false NOT NULL,
                                            for_obsolete_scan boolean DEFAULT false NOT NULL,
                                            "time" timestamp without time zone NOT NULL,
                                            commit_hash character varying(128),
                                            initiator character varying(60) NOT NULL,
                                            scan_trigger_type character varying(50) NOT NULL,
                                            client_scan_type character varying(50)
);


ALTER TABLE t_TENANT.policy_evaluation OWNER TO testuser;

--
-- Name: policy_monitoring; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.policy_monitoring (
                                            policy_monitoring_id character varying(50) NOT NULL,
                                            owner_id character varying(50) NOT NULL,
                                            stage_type_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.policy_monitoring OWNER TO testuser;

--
-- Name: policy_tag; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.policy_tag (
                                     policy_tag_id character varying(50) NOT NULL,
                                     policy_id character varying(50) NOT NULL,
                                     tag_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.policy_tag OWNER TO testuser;

--
-- Name: policy_violation; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.policy_violation (
                                           policy_violation_id character varying(50) NOT NULL,
                                           application_id character varying(50) NOT NULL,
                                           stage_type_id character varying(30) NOT NULL,
                                           policy_id character varying(50) NOT NULL,
                                           policy_name character varying(60) NOT NULL,
                                           threat_level smallint NOT NULL,
                                           threat_category character varying(20) NOT NULL,
                                           hash character varying(20),
                                           component_id_format character varying(50),
                                           component_id_coordinates_json character varying(1000),
                                           filename character varying(1000),
                                           constraint_facts_json text NOT NULL,
                                           action_type_id character varying(20),
                                           open_time timestamp without time zone NOT NULL,
                                           waive_time timestamp without time zone,
                                           grandfather_time timestamp without time zone,
                                           fix_time timestamp without time zone,
                                           policy_waiver_id character varying(50),
                                           policy_waiver_comment character varying(1000),
                                           seen_by_primary_evaluation boolean NOT NULL,
                                           seen_by_monitoring_evaluation boolean NOT NULL,
                                           grandfather_applied boolean DEFAULT false NOT NULL
);


ALTER TABLE t_TENANT.policy_violation OWNER TO testuser;

--
-- Name: policy_violation_aggregation; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.policy_violation_aggregation (
                                                       policy_violation_aggregation_id character varying(50) NOT NULL,
                                                       application_id character varying(50) NOT NULL,
                                                       time_period_start timestamp without time zone NOT NULL,
                                                       time_period_end timestamp without time zone,
                                                       mttr_low_threat bigint,
                                                       mttr_moderate_threat bigint,
                                                       mttr_severe_threat bigint,
                                                       mttr_critical_threat bigint,
                                                       fixed_count_security_low_threat integer NOT NULL,
                                                       fixed_count_security_moderate_threat integer NOT NULL,
                                                       fixed_count_security_severe_threat integer NOT NULL,
                                                       fixed_count_security_critical_threat integer NOT NULL,
                                                       fixed_count_license_low_threat integer NOT NULL,
                                                       fixed_count_license_moderate_threat integer NOT NULL,
                                                       fixed_count_license_severe_threat integer NOT NULL,
                                                       fixed_count_license_critical_threat integer NOT NULL,
                                                       fixed_count_quality_low_threat integer NOT NULL,
                                                       fixed_count_quality_moderate_threat integer NOT NULL,
                                                       fixed_count_quality_severe_threat integer NOT NULL,
                                                       fixed_count_quality_critical_threat integer NOT NULL,
                                                       fixed_count_other_low_threat integer NOT NULL,
                                                       fixed_count_other_moderate_threat integer NOT NULL,
                                                       fixed_count_other_severe_threat integer NOT NULL,
                                                       fixed_count_other_critical_threat integer NOT NULL,
                                                       waived_count_security_low_threat integer NOT NULL,
                                                       waived_count_security_moderate_threat integer NOT NULL,
                                                       waived_count_security_severe_threat integer NOT NULL,
                                                       waived_count_security_critical_threat integer NOT NULL,
                                                       waived_count_license_low_threat integer NOT NULL,
                                                       waived_count_license_moderate_threat integer NOT NULL,
                                                       waived_count_license_severe_threat integer NOT NULL,
                                                       waived_count_license_critical_threat integer NOT NULL,
                                                       waived_count_quality_low_threat integer NOT NULL,
                                                       waived_count_quality_moderate_threat integer NOT NULL,
                                                       waived_count_quality_severe_threat integer NOT NULL,
                                                       waived_count_quality_critical_threat integer NOT NULL,
                                                       waived_count_other_low_threat integer NOT NULL,
                                                       waived_count_other_moderate_threat integer NOT NULL,
                                                       waived_count_other_severe_threat integer NOT NULL,
                                                       waived_count_other_critical_threat integer NOT NULL,
                                                       discovered_count_security_low_threat integer NOT NULL,
                                                       discovered_count_security_moderate_threat integer NOT NULL,
                                                       discovered_count_security_severe_threat integer NOT NULL,
                                                       discovered_count_security_critical_threat integer NOT NULL,
                                                       discovered_count_license_low_threat integer NOT NULL,
                                                       discovered_count_license_moderate_threat integer NOT NULL,
                                                       discovered_count_license_severe_threat integer NOT NULL,
                                                       discovered_count_license_critical_threat integer NOT NULL,
                                                       discovered_count_quality_low_threat integer NOT NULL,
                                                       discovered_count_quality_moderate_threat integer NOT NULL,
                                                       discovered_count_quality_severe_threat integer NOT NULL,
                                                       discovered_count_quality_critical_threat integer NOT NULL,
                                                       discovered_count_other_low_threat integer NOT NULL,
                                                       discovered_count_other_moderate_threat integer NOT NULL,
                                                       discovered_count_other_severe_threat integer NOT NULL,
                                                       discovered_count_other_critical_threat integer NOT NULL,
                                                       evaluation_count integer NOT NULL,
                                                       time_period character varying(20) DEFAULT 'MONTH'::character varying NOT NULL,
                                                       open_count_security_low_threat integer NOT NULL,
                                                       open_count_security_moderate_threat integer NOT NULL,
                                                       open_count_security_severe_threat integer NOT NULL,
                                                       open_count_security_critical_threat integer NOT NULL,
                                                       open_count_license_low_threat integer NOT NULL,
                                                       open_count_license_moderate_threat integer NOT NULL,
                                                       open_count_license_severe_threat integer NOT NULL,
                                                       open_count_license_critical_threat integer NOT NULL,
                                                       open_count_quality_low_threat integer NOT NULL,
                                                       open_count_quality_moderate_threat integer NOT NULL,
                                                       open_count_quality_severe_threat integer NOT NULL,
                                                       open_count_quality_critical_threat integer NOT NULL,
                                                       open_count_other_low_threat integer NOT NULL,
                                                       open_count_other_moderate_threat integer NOT NULL,
                                                       open_count_other_severe_threat integer NOT NULL,
                                                       open_count_other_critical_threat integer NOT NULL
);


ALTER TABLE t_TENANT.policy_violation_aggregation OWNER TO testuser;

--
-- Name: policy_waiver; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.policy_waiver (
                                        policy_waiver_id character varying(50) NOT NULL,
                                        hash character varying(20),
                                        policy_id character varying(50) NOT NULL,
                                        owner_id character varying(50) NOT NULL,
                                        constraint_facts_json text,
                                        associated_package_url character varying(1000),
                                        component_match_strategy character varying(30),
                                        comment character varying(1000),
                                        create_time timestamp without time zone NOT NULL,
                                        expiry_time timestamp without time zone,
                                        creator_id character varying(60) DEFAULT NULL::character varying,
                                        creator_name character varying(210) DEFAULT NULL::character varying,
                                        component_upgrade_available boolean
);


ALTER TABLE t_TENANT.policy_waiver OWNER TO testuser;

--
-- Name: product_license; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.product_license (
                                          product_license_id character varying(50) NOT NULL,
                                          license_key character varying(8192) NOT NULL,
                                          license_details character varying(8192)
);


ALTER TABLE t_TENANT.product_license OWNER TO testuser;

--
-- Name: proprietary_component_name_pattern; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.proprietary_component_name_pattern (
                                                             proprietary_component_name_pattern_id character varying(50) NOT NULL,
                                                             format character varying(50) NOT NULL,
                                                             namespace_pattern character varying(200) NOT NULL,
                                                             name_pattern character varying(300) NOT NULL,
                                                             repository_id character varying(50) NOT NULL,
                                                             enabled boolean DEFAULT true NOT NULL
);


ALTER TABLE t_TENANT.proprietary_component_name_pattern OWNER TO testuser;

--
-- Name: proprietary_config; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.proprietary_config (
                                             proprietary_config_id character varying(50) NOT NULL,
                                             owner_id character varying(50) NOT NULL,
                                             packages_json text,
                                             regexes_json text
);


ALTER TABLE t_TENANT.proprietary_config OWNER TO testuser;

--
-- Name: proxy_server_configuration; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.proxy_server_configuration (
                                                     proxy_server_configuration_id character varying(50) NOT NULL,
                                                     hostname character varying(255) NOT NULL,
                                                     port integer NOT NULL,
                                                     username character varying(255),
                                                     password character varying(255),
                                                     exclude_hosts character varying(500)
);


ALTER TABLE t_TENANT.proxy_server_configuration OWNER TO testuser;

--
-- Name: qrtz_blob_triggers; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_blob_triggers (
                                             sched_name character varying(120) NOT NULL,
                                             trigger_name character varying(200) NOT NULL,
                                             trigger_group character varying(200) NOT NULL,
                                             blob_data bytea
);


ALTER TABLE t_TENANT.qrtz_blob_triggers OWNER TO testuser;

--
-- Name: qrtz_calendars; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_calendars (
                                         sched_name character varying(120) NOT NULL,
                                         calendar_name character varying(200) NOT NULL,
                                         calendar bytea NOT NULL
);


ALTER TABLE t_TENANT.qrtz_calendars OWNER TO testuser;

--
-- Name: qrtz_cron_triggers; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_cron_triggers (
                                             sched_name character varying(120) NOT NULL,
                                             trigger_name character varying(200) NOT NULL,
                                             trigger_group character varying(200) NOT NULL,
                                             cron_expression character varying(120) NOT NULL,
                                             time_zone_id character varying(80)
);


ALTER TABLE t_TENANT.qrtz_cron_triggers OWNER TO testuser;

--
-- Name: qrtz_fired_triggers; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_fired_triggers (
                                              sched_name character varying(120) NOT NULL,
                                              entry_id character varying(95) NOT NULL,
                                              trigger_name character varying(200) NOT NULL,
                                              trigger_group character varying(200) NOT NULL,
                                              instance_name character varying(200) NOT NULL,
                                              fired_time bigint NOT NULL,
                                              sched_time bigint NOT NULL,
                                              priority integer NOT NULL,
                                              state character varying(16) NOT NULL,
                                              job_name character varying(200),
                                              job_group character varying(200),
                                              is_nonconcurrent boolean,
                                              requests_recovery boolean
);


ALTER TABLE t_TENANT.qrtz_fired_triggers OWNER TO testuser;

--
-- Name: qrtz_job_details; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_job_details (
                                           sched_name character varying(120) NOT NULL,
                                           job_name character varying(200) NOT NULL,
                                           job_group character varying(200) NOT NULL,
                                           description character varying(250),
                                           job_class_name character varying(250) NOT NULL,
                                           is_durable boolean NOT NULL,
                                           is_nonconcurrent boolean NOT NULL,
                                           is_update_data boolean NOT NULL,
                                           requests_recovery boolean NOT NULL,
                                           job_data bytea
);


ALTER TABLE t_TENANT.qrtz_job_details OWNER TO testuser;

--
-- Name: qrtz_locks; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_locks (
                                     sched_name character varying(120) NOT NULL,
                                     lock_name character varying(40) NOT NULL
);


ALTER TABLE t_TENANT.qrtz_locks OWNER TO testuser;

--
-- Name: qrtz_paused_trigger_grps; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_paused_trigger_grps (
                                                   sched_name character varying(120) NOT NULL,
                                                   trigger_group character varying(200) NOT NULL
);


ALTER TABLE t_TENANT.qrtz_paused_trigger_grps OWNER TO testuser;

--
-- Name: qrtz_scheduler_state; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_scheduler_state (
                                               sched_name character varying(120) NOT NULL,
                                               instance_name character varying(200) NOT NULL,
                                               last_checkin_time bigint NOT NULL,
                                               checkin_interval bigint NOT NULL
);


ALTER TABLE t_TENANT.qrtz_scheduler_state OWNER TO testuser;

--
-- Name: qrtz_simple_triggers; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_simple_triggers (
                                               sched_name character varying(120) NOT NULL,
                                               trigger_name character varying(200) NOT NULL,
                                               trigger_group character varying(200) NOT NULL,
                                               repeat_count bigint NOT NULL,
                                               repeat_interval bigint NOT NULL,
                                               times_triggered bigint NOT NULL
);


ALTER TABLE t_TENANT.qrtz_simple_triggers OWNER TO testuser;

--
-- Name: qrtz_simprop_triggers; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_simprop_triggers (
                                                sched_name character varying(120) NOT NULL,
                                                trigger_name character varying(200) NOT NULL,
                                                trigger_group character varying(200) NOT NULL,
                                                str_prop_1 character varying(512),
                                                str_prop_2 character varying(512),
                                                str_prop_3 character varying(512),
                                                int_prop_1 integer,
                                                int_prop_2 integer,
                                                long_prop_1 bigint,
                                                long_prop_2 bigint,
                                                dec_prop_1 numeric(13,4),
                                                dec_prop_2 numeric(13,4),
                                                bool_prop_1 boolean,
                                                bool_prop_2 boolean
);


ALTER TABLE t_TENANT.qrtz_simprop_triggers OWNER TO testuser;

--
-- Name: qrtz_triggers; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.qrtz_triggers (
                                        sched_name character varying(120) NOT NULL,
                                        trigger_name character varying(200) NOT NULL,
                                        trigger_group character varying(200) NOT NULL,
                                        job_name character varying(200) NOT NULL,
                                        job_group character varying(200) NOT NULL,
                                        description character varying(250),
                                        next_fire_time bigint,
                                        prev_fire_time bigint,
                                        priority integer,
                                        trigger_state character varying(16) NOT NULL,
                                        trigger_type character varying(8) NOT NULL,
                                        start_time bigint NOT NULL,
                                        end_time bigint,
                                        calendar_name character varying(200),
                                        misfire_instr smallint,
                                        job_data bytea
);


ALTER TABLE t_TENANT.qrtz_triggers OWNER TO testuser;

--
-- Name: quarantined_component_access; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.quarantined_component_access (
                                                       quarantined_component_access_id character varying(50) NOT NULL,
                                                       repository_id character varying(50) NOT NULL,
                                                       repository_component_id character varying(50) NOT NULL,
                                                       generate_time timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.quarantined_component_access OWNER TO testuser;

--
-- Name: repository; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.repository (
                                     repository_id character varying(50) NOT NULL,
                                     repository_manager_id character varying(50) NOT NULL,
                                     public_id character varying(500) NOT NULL,
                                     repository_type character varying(10) DEFAULT 'proxy'::character varying NOT NULL,
                                     audit_enabled boolean DEFAULT true NOT NULL,
                                     quarantine_enabled boolean DEFAULT false NOT NULL,
                                     policy_compliant_component_selection_enabled boolean DEFAULT false NOT NULL,
                                     namespace_confusion_protection_enabled boolean DEFAULT false NOT NULL,
                                     format character varying(50),
                                     last_manual_configure_time timestamp without time zone
);


ALTER TABLE t_TENANT.repository OWNER TO testuser;

--
-- Name: repository_client_configuration; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.repository_client_configuration (
                                                          repository_client_configuration_id character varying(50) NOT NULL,
                                                          connection_timeout smallint,
                                                          socket_timeout smallint
);


ALTER TABLE t_TENANT.repository_client_configuration OWNER TO testuser;

--
-- Name: repository_component; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.repository_component (
                                               repository_component_id character varying(50) NOT NULL,
                                               repository_id character varying(50) NOT NULL,
                                               pathname character varying(1000) NOT NULL,
                                               "time" timestamp without time zone NOT NULL,
                                               hash character varying(20) NOT NULL,
                                               component_id_format character varying(10),
                                               component_id_coordinates_json character varying(1000),
                                               display_name character varying(1000),
                                               match_state_id character varying(20) NOT NULL,
                                               identification_source_id character varying(20) NOT NULL,
                                               last_evaluation_time timestamp without time zone NOT NULL,
                                               quarantine_time timestamp without time zone,
                                               unquarantine_time timestamp without time zone,
                                               analyzer_features_json character varying(1000),
                                               auto_unquarantined boolean
);


ALTER TABLE t_TENANT.repository_component OWNER TO testuser;

--
-- Name: repository_connection; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.repository_connection (
                                                repository_connection_id character varying(50) NOT NULL,
                                                owner_id character varying(50) NOT NULL,
                                                base_url character varying(2048) NOT NULL,
                                                format character varying(50) DEFAULT 'GENERIC'::character varying NOT NULL,
                                                username character varying(255),
                                                password character varying(255)
);


ALTER TABLE t_TENANT.repository_connection OWNER TO testuser;

--
-- Name: repository_identified_component; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.repository_identified_component (
                                                          hash character varying(64) NOT NULL,
                                                          component_id_format character varying(10) NOT NULL,
                                                          component_id_coordinates_json character varying(1000) NOT NULL,
                                                          create_time timestamp without time zone NOT NULL,
                                                          last_access_time timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.repository_identified_component OWNER TO testuser;

--
-- Name: repository_manager; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.repository_manager (
                                             repository_manager_id character varying(50) NOT NULL,
                                             instance_id character varying(50) NOT NULL,
                                             user_agent character varying(300),
                                             configured boolean DEFAULT true NOT NULL,
                                             configure_time timestamp without time zone
);


ALTER TABLE t_TENANT.repository_manager OWNER TO testuser;

--
-- Name: repository_migration; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.repository_migration (
                                               repository_migration_id character varying(50) NOT NULL,
                                               repository_id character varying(50) NOT NULL,
                                               state character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.repository_migration OWNER TO testuser;

--
-- Name: repository_policy_violation; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.repository_policy_violation (
                                                      repository_policy_violation_id character varying(50) NOT NULL,
                                                      repository_id character varying(50) NOT NULL,
                                                      pathname character varying(1000) NOT NULL,
                                                      "time" timestamp without time zone NOT NULL,
                                                      policy_id character varying(50) NOT NULL,
                                                      policy_name character varying(60) NOT NULL,
                                                      threat_level smallint NOT NULL,
                                                      threat_category character varying(20) NOT NULL,
                                                      hash character varying(20),
                                                      component_id_format character varying(10),
                                                      component_id_coordinates_json character varying(1000),
                                                      constraint_facts_json text NOT NULL,
                                                      action_type_id character varying(20),
                                                      waived boolean DEFAULT false NOT NULL,
                                                      active boolean DEFAULT true NOT NULL,
                                                      policy_waiver_id character varying(50),
                                                      policy_waiver_comment character varying(1000),
                                                      waive_time timestamp without time zone
);


ALTER TABLE t_TENANT.repository_policy_violation OWNER TO testuser;

--
-- Name: reverse_proxy_authentication_configuration; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.reverse_proxy_authentication_configuration (
                                                                     reverse_proxy_authentication_configuration_id character varying(50) NOT NULL,
                                                                     enabled boolean NOT NULL,
                                                                     username_header character varying(255) NOT NULL,
                                                                     csrf_protection_disabled boolean NOT NULL,
                                                                     logout_url character varying(2048)
);


ALTER TABLE t_TENANT.reverse_proxy_authentication_configuration OWNER TO testuser;

--
-- Name: role; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.role (
                               role_id character varying(50) NOT NULL,
                               name character varying(60) NOT NULL,
                               name_lowercase_no_whitespace character varying(60) NOT NULL,
                               sort_order integer NOT NULL,
                               description character varying(255) NOT NULL,
                               global boolean NOT NULL,
                               built_in boolean DEFAULT false NOT NULL
);


ALTER TABLE t_TENANT.role OWNER TO testuser;

--
-- Name: role_permission; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.role_permission (
                                          role_permission_id character varying(50) NOT NULL,
                                          role_id character varying(50) NOT NULL,
                                          permission character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.role_permission OWNER TO testuser;

--
-- Name: saml_configuration; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.saml_configuration (
                                             saml_configuration_id character varying(50) NOT NULL,
                                             configuration_json text NOT NULL,
                                             keystore bytea NOT NULL,
                                             keystore_password_obfuscated character varying(200) NOT NULL
);


ALTER TABLE t_TENANT.saml_configuration OWNER TO testuser;

--
-- Name: saml_group; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.saml_group (
                                     saml_group_id character varying(50) NOT NULL,
                                     name character varying(2048) NOT NULL
);


ALTER TABLE t_TENANT.saml_group OWNER TO testuser;

--
-- Name: saml_user; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.saml_user (
                                    saml_user_id character varying(50) NOT NULL,
                                    username character varying(60) NOT NULL,
                                    first_name character varying(100),
                                    last_name character varying(100),
                                    email character varying(255),
                                    groups text
);


ALTER TABLE t_TENANT.saml_user OWNER TO testuser;

--
-- Name: saml_user_group; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.saml_user_group (
                                          saml_user_group_id character varying(50) NOT NULL,
                                          saml_user_id character varying(50) NOT NULL,
                                          saml_group_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.saml_user_group OWNER TO testuser;

--
-- Name: schema_version; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.schema_version (
                                         data_store_id character varying(32) NOT NULL,
                                         schema_version integer NOT NULL
);


ALTER TABLE t_TENANT.schema_version OWNER TO testuser;

--
-- Name: search_index_change; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.search_index_change (
                                              search_index_change_id character varying(50) NOT NULL,
                                              change_type character varying(100) NOT NULL,
                                              change_data character varying(2000) NOT NULL
);


ALTER TABLE t_TENANT.search_index_change OWNER TO testuser;

--
-- Name: source_control; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.source_control (
                                         source_control_id character varying(50) NOT NULL,
                                         owner_id character varying(50) NOT NULL,
                                         repository_url character varying(2048),
                                         normalized_repository_url character varying(2048),
                                         repository_ssh_url character varying(2048),
                                         username character varying(256),
                                         token character varying(512),
                                         provider character varying(20),
                                         base_branch character varying(243),
                                         ssh_enabled boolean DEFAULT false,
                                         remediation_pull_requests_enabled boolean,
                                         status_checks_enabled boolean,
                                         pull_request_commenting_enabled boolean,
                                         source_control_evaluations_enabled boolean,
                                         source_control_scan_target character varying(1000),
                                         pull_request_poll_time timestamp without time zone,
                                         pull_request_error_count integer DEFAULT 0 NOT NULL,
                                         commit_status_enabled boolean
);


ALTER TABLE t_TENANT.source_control OWNER TO testuser;

--
-- Name: source_control_configuration; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.source_control_configuration (
                                                       source_control_configuration_id character varying(50) NOT NULL,
                                                       clone_directory character varying(1000) NOT NULL,
                                                       git_implementation character varying(20),
                                                       pr_comment_purge_window integer,
                                                       pr_event_purge_window integer,
                                                       git_executable character varying(1000),
                                                       git_timeout_seconds integer DEFAULT 0 NOT NULL,
                                                       commit_username character varying(256),
                                                       commit_email character varying(256),
                                                       use_username_in_repository_clone_url boolean DEFAULT false NOT NULL,
                                                       default_branch_monitoring_start_time character varying(5),
                                                       default_branch_monitoring_interval_hours integer DEFAULT 24 NOT NULL,
                                                       pull_request_monitoring_interval_seconds integer DEFAULT 60 NOT NULL
);


ALTER TABLE t_TENANT.source_control_configuration OWNER TO testuser;

--
-- Name: source_control_default_branch_commit_history; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.source_control_default_branch_commit_history (
                                                                       source_control_default_branch_commit_history_id character varying(50) NOT NULL,
                                                                       application_id character varying(50) NOT NULL,
                                                                       commit_hash character varying(128) NOT NULL,
                                                                       commit_time timestamp without time zone NOT NULL,
                                                                       policy_evaluation_id character varying(50),
                                                                       create_time timestamp without time zone NOT NULL,
                                                                       update_time timestamp without time zone
);


ALTER TABLE t_TENANT.source_control_default_branch_commit_history OWNER TO testuser;

--
-- Name: source_control_event; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.source_control_event (
                                               source_control_event_id character varying(50) NOT NULL,
                                               instance_id character varying(50),
                                               application_id character varying(50) NOT NULL,
                                               event_type character varying(50) NOT NULL,
                                               event_priority integer DEFAULT 2 NOT NULL,
                                               event_status character varying(50) NOT NULL,
                                               event_status_details character varying(2048),
                                               event_error_details text,
                                               commit_hash character varying(128),
                                               base_commit_hash character varying(128),
                                               policy_evaluation_id character varying(50),
                                               policy_evaluation_outcome character varying(20),
                                               critical_component_count integer DEFAULT 0 NOT NULL,
                                               severe_component_count integer DEFAULT 0 NOT NULL,
                                               moderate_component_count integer DEFAULT 0 NOT NULL,
                                               scan_id character varying(50),
                                               stage_type_id character varying(30),
                                               component_id_format character varying(50),
                                               component_id_coordinates_json character varying(1000),
                                               branch_name character varying(512),
                                               scan_targets_json text,
                                               base_branch_name character varying(512),
                                               remediation_version character varying(100),
                                               pull_request_contents text,
                                               pull_request_number integer,
                                               scm_username character varying(255),
                                               initiator character varying(60),
                                               create_time timestamp without time zone NOT NULL,
                                               start_time timestamp without time zone,
                                               complete_time timestamp without time zone,
                                               status_id character varying(50),
                                               user_agent character varying(255),
                                               scan_trigger_type character varying(50)
);


ALTER TABLE t_TENANT.source_control_event OWNER TO testuser;

--
-- Name: source_control_organization_import_event; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.source_control_organization_import_event (
                                                                   source_control_organization_import_event_id character varying(50) NOT NULL,
                                                                   organization_id character varying(50) NOT NULL,
                                                                   source_control_host_url character varying(2048) NOT NULL,
                                                                   desired_sub_organization_count integer NOT NULL,
                                                                   import_limit integer NOT NULL,
                                                                   import_status character varying(20) NOT NULL,
                                                                   import_success_count integer NOT NULL,
                                                                   import_failure_count integer NOT NULL,
                                                                   start_time timestamp without time zone NOT NULL,
                                                                   last_updated_time timestamp without time zone NOT NULL,
                                                                   import_errors text
);


ALTER TABLE t_TENANT.source_control_organization_import_event OWNER TO testuser;

--
-- Name: source_control_pull_request; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.source_control_pull_request (
                                                      source_control_pull_request_id character varying(50) NOT NULL,
                                                      repository_url character varying(2048) NOT NULL,
                                                      pull_request_id integer NOT NULL,
                                                      head_commit_hash character varying(128) NOT NULL,
                                                      base_commit_hash character varying(128),
                                                      branch_name character varying(512) NOT NULL,
                                                      base_branch_name character varying(512),
                                                      create_time timestamp without time zone NOT NULL,
                                                      last_check_time timestamp without time zone NOT NULL,
                                                      last_detected_update_time timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.source_control_pull_request OWNER TO testuser;

--
-- Name: source_control_pull_request_comment; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.source_control_pull_request_comment (
                                                              source_control_pull_request_comment_id character varying(50) NOT NULL,
                                                              application_id character varying(50) NOT NULL,
                                                              component_hash character varying(20),
                                                              pathname character varying(1000),
                                                              pull_request_id integer NOT NULL,
                                                              pull_request_comment_id integer NOT NULL,
                                                              pull_request_comment_version integer,
                                                              source_policy_evaluation_id character varying(50) NOT NULL,
                                                              target_policy_evaluation_id character varying(50) NOT NULL,
                                                              create_time timestamp without time zone NOT NULL,
                                                              update_time timestamp without time zone,
                                                              content_hash character varying(40)
);


ALTER TABLE t_TENANT.source_control_pull_request_comment OWNER TO testuser;

--
-- Name: source_control_pull_request_result; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.source_control_pull_request_result (
                                                             source_control_pull_request_result_id character varying(50) NOT NULL,
                                                             application_id character varying(50) NOT NULL,
                                                             pull_request_result_json text NOT NULL
);


ALTER TABLE t_TENANT.source_control_pull_request_result OWNER TO testuser;

--
-- Name: source_link_override; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.source_link_override (
                                               source_link_override_id character varying(50) NOT NULL,
                                               content character varying(1000) NOT NULL,
                                               original_content character varying(1000) NOT NULL,
                                               status character varying(20) NOT NULL,
                                               component_source_link_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.source_link_override OWNER TO testuser;

--
-- Name: success_metrics_report; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.success_metrics_report (
                                                 success_metrics_report_id character varying(50) NOT NULL,
                                                 username character varying(60) NOT NULL,
                                                 name character varying(60) NOT NULL,
                                                 name_lowercase_no_whitespace character varying(60) NOT NULL,
                                                 scope_json text NOT NULL,
                                                 create_time timestamp without time zone NOT NULL,
                                                 include_latest_data boolean DEFAULT true NOT NULL
);


ALTER TABLE t_TENANT.success_metrics_report OWNER TO testuser;

--
-- Name: success_metrics_report_data; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.success_metrics_report_data (
                                                      success_metrics_report_data_id character varying(50) NOT NULL,
                                                      last_updated timestamp without time zone NOT NULL,
                                                      included_application_ids_json text NOT NULL,
                                                      month_count smallint NOT NULL,
                                                      active_application_count integer NOT NULL,
                                                      chart_data_json text NOT NULL
);


ALTER TABLE t_TENANT.success_metrics_report_data OWNER TO testuser;

--
-- Name: sv_override; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.sv_override (
                                      sv_override_id character varying(50) NOT NULL,
                                      owner_id character varying(50) NOT NULL,
                                      hash character varying(20) NOT NULL,
                                      source character varying(10) NOT NULL,
                                      reference_id character varying(20) NOT NULL,
                                      status character varying(20) NOT NULL,
                                      comment character varying(1000)
);


ALTER TABLE t_TENANT.sv_override OWNER TO testuser;

--
-- Name: system_configuration_property; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.system_configuration_property (
                                                        system_configuration_property_id character varying(50) NOT NULL,
                                                        name character varying(50) NOT NULL,
                                                        value character varying(500) NOT NULL
);


ALTER TABLE t_TENANT.system_configuration_property OWNER TO testuser;

--
-- Name: system_notice; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.system_notice (
                                        system_notice_id character varying(50) NOT NULL,
                                        message character varying(500) NOT NULL,
                                        enabled boolean NOT NULL
);


ALTER TABLE t_TENANT.system_notice OWNER TO testuser;

--
-- Name: tag; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.tag (
                              tag_id character varying(50) NOT NULL,
                              organization_id character varying(50) NOT NULL,
                              name character varying(60) NOT NULL,
                              name_lowercase_no_whitespace character varying(60) NOT NULL,
                              description character varying(255) NOT NULL,
                              color character varying(20) NOT NULL
);


ALTER TABLE t_TENANT.tag OWNER TO testuser;

--
-- Name: tenant_metadata; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.tenant_metadata (
                                          tenant_metadata_id character varying(50) NOT NULL,
                                          application_id character varying(50) NOT NULL,
                                          application_name character varying(100) NOT NULL,
                                          connection_id character varying(50) NOT NULL,
                                          connection_name character varying(100) NOT NULL
);


ALTER TABLE t_TENANT.tenant_metadata OWNER TO testuser;

--
-- Name: test_table; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.test_table (
                                     test_table_id character varying(50) NOT NULL,
                                     name character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.test_table OWNER TO testuser;

--
-- Name: third_party_file; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.third_party_file (
                                           third_party_file_id character varying(50) NOT NULL,
                                           filename character varying(1000),
                                           create_time timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.third_party_file OWNER TO testuser;

--
-- Name: third_party_scan; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.third_party_scan (
                                           third_party_scan_id character varying(50) NOT NULL,
                                           third_party_file_id character varying(50) NOT NULL,
                                           scan_request_id character varying(50) NOT NULL,
                                           scan_id character varying(50),
                                           create_time timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.third_party_scan OWNER TO testuser;

--
-- Name: third_party_vulnerability; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.third_party_vulnerability (
                                                    third_party_vulnerability_id character varying(50) NOT NULL,
                                                    ref_id character varying(20) NOT NULL,
                                                    description text,
                                                    link character varying(200),
                                                    severity double precision NOT NULL,
                                                    fixed_by character varying(200),
                                                    vulnerability_source character varying(15),
                                                    severity_description character varying(15),
                                                    attack_vector character varying(100),
                                                    rating_method character varying(10),
                                                    cwes text,
                                                    recommendations text,
                                                    advisories text,
                                                    update_time timestamp without time zone
);


ALTER TABLE t_TENANT.third_party_vulnerability OWNER TO testuser;

--
-- Name: user; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT."user" (
                                 user_id character varying(50) NOT NULL,
                                 username character varying(60) NOT NULL,
                                 username_lowercase character varying(60) NOT NULL,
                                 password character varying(128) NOT NULL,
                                 first_name character varying(100) NOT NULL,
                                 last_name character varying(100) NOT NULL,
                                 email character varying(255) NOT NULL
);


ALTER TABLE t_TENANT."user" OWNER TO testuser;

--
-- Name: user_filter; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.user_filter (
                                      user_filter_id character varying(50) NOT NULL,
                                      username character varying(60) NOT NULL,
                                      username_lowercase character varying(60) NOT NULL,
                                      realm_id character varying(50) NOT NULL,
                                      name character varying(60) NOT NULL,
                                      name_lowercase_no_whitespace character varying(60) NOT NULL,
                                      filter_json text NOT NULL,
                                      based_on_filter_name character varying(60),
                                      filter_type character varying(100) NOT NULL
);


ALTER TABLE t_TENANT.user_filter OWNER TO testuser;

--
-- Name: user_ide_policy_evaluation; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.user_ide_policy_evaluation (
                                                     user_ide_policy_evaluation_id character varying(50) NOT NULL,
                                                     username character varying(200) NOT NULL,
                                                     last_evaluation_time timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.user_ide_policy_evaluation OWNER TO testuser;

--
-- Name: user_token; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.user_token (
                                     user_token_id character varying(50) NOT NULL,
                                     username character varying(200) NOT NULL,
                                     user_code character varying(128) NOT NULL,
                                     pass_code character varying(128) NOT NULL,
                                     realm_id character varying(50) NOT NULL,
                                     create_time timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.user_token OWNER TO testuser;

--
-- Name: user_viewed_product_notification; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.user_viewed_product_notification (
                                                           user_viewed_product_notification_id character varying(50) NOT NULL,
                                                           username character varying(60) NOT NULL,
                                                           username_lowercase character varying(60) NOT NULL,
                                                           realm_id character varying(50),
                                                           notification_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.user_viewed_product_notification OWNER TO testuser;

--
-- Name: vulnerability_custom_cvss_severity; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.vulnerability_custom_cvss_severity (
                                                             vulnerability_custom_cvss_severity_id character varying(50) NOT NULL,
                                                             owner_id character varying(50) NOT NULL,
                                                             refid character varying(20) NOT NULL,
                                                             component_id_format character varying(10),
                                                             component_id_coordinates_json character varying(1000),
                                                             severity double precision NOT NULL,
                                                             last_updated_by_username character varying(256) NOT NULL,
                                                             last_updated_at timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.vulnerability_custom_cvss_severity OWNER TO testuser;

--
-- Name: vulnerability_custom_cvss_severity_tag; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.vulnerability_custom_cvss_severity_tag (
                                                                 vulnerability_custom_cvss_severity_tag_id character varying(50) NOT NULL,
                                                                 vulnerability_custom_cvss_severity_id character varying(50) NOT NULL,
                                                                 tag_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.vulnerability_custom_cvss_severity_tag OWNER TO testuser;

--
-- Name: vulnerability_custom_cvss_vector; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.vulnerability_custom_cvss_vector (
                                                           vulnerability_custom_cvss_vector_id character varying(50) NOT NULL,
                                                           owner_id character varying(50) NOT NULL,
                                                           refid character varying(20) NOT NULL,
                                                           component_id_format character varying(10),
                                                           component_id_coordinates_json character varying(1000),
                                                           vector character varying(200) NOT NULL,
                                                           last_updated_by_username character varying(256) NOT NULL,
                                                           last_updated_at timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.vulnerability_custom_cvss_vector OWNER TO testuser;

--
-- Name: vulnerability_custom_cvss_vector_tag; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.vulnerability_custom_cvss_vector_tag (
                                                               vulnerability_custom_cvss_vector_tag_id character varying(50) NOT NULL,
                                                               vulnerability_custom_cvss_vector_id character varying(50) NOT NULL,
                                                               tag_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.vulnerability_custom_cvss_vector_tag OWNER TO testuser;

--
-- Name: vulnerability_custom_cwe; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.vulnerability_custom_cwe (
                                                   vulnerability_custom_cwe_id character varying(50) NOT NULL,
                                                   owner_id character varying(50) NOT NULL,
                                                   refid character varying(20) NOT NULL,
                                                   component_id_format character varying(10),
                                                   component_id_coordinates_json character varying(1000),
                                                   cwe character varying(50) NOT NULL,
                                                   last_updated_by_username character varying(256) NOT NULL,
                                                   last_updated_at timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.vulnerability_custom_cwe OWNER TO testuser;

--
-- Name: vulnerability_custom_cwe_tag; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.vulnerability_custom_cwe_tag (
                                                       vulnerability_custom_cwe_tag_id character varying(50) NOT NULL,
                                                       vulnerability_custom_cwe_id character varying(50) NOT NULL,
                                                       tag_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.vulnerability_custom_cwe_tag OWNER TO testuser;

--
-- Name: vulnerability_custom_remediation; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.vulnerability_custom_remediation (
                                                           vulnerability_custom_remediation_id character varying(50) NOT NULL,
                                                           owner_id character varying(50) NOT NULL,
                                                           refid character varying(20) NOT NULL,
                                                           component_id_format character varying(10),
                                                           component_id_coordinates_json character varying(1000),
                                                           remediation character varying(3000) NOT NULL,
                                                           last_updated_by_username character varying(256) NOT NULL,
                                                           last_updated_at timestamp without time zone NOT NULL
);


ALTER TABLE t_TENANT.vulnerability_custom_remediation OWNER TO testuser;

--
-- Name: vulnerability_custom_remediation_tag; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.vulnerability_custom_remediation_tag (
                                                               vulnerability_custom_remediation_tag_id character varying(50) NOT NULL,
                                                               vulnerability_custom_remediation_id character varying(50) NOT NULL,
                                                               tag_id character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.vulnerability_custom_remediation_tag OWNER TO testuser;

--
-- Name: vulnerability_group; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.vulnerability_group (
                                              vulnerability_group_id character varying(50) NOT NULL,
                                              owner_id character varying(50) NOT NULL,
                                              name character varying(60) NOT NULL,
                                              name_lowercase_no_whitespace character varying(60) NOT NULL
);


ALTER TABLE t_TENANT.vulnerability_group OWNER TO testuser;

--
-- Name: vulnerability_group_vulnerability; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.vulnerability_group_vulnerability (
                                                            vulnerability_group_vulnerability_id character varying(50) NOT NULL,
                                                            vulnerability_group_id character varying(50) NOT NULL,
                                                            vulnerability_refid character varying(100) NOT NULL
);


ALTER TABLE t_TENANT.vulnerability_group_vulnerability OWNER TO testuser;

--
-- Name: webhook; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.webhook (
                                  webhook_id character varying(50) NOT NULL,
                                  url character varying(2048) NOT NULL,
                                  description character varying(2048),
                                  secret_key character varying(512)
);


ALTER TABLE t_TENANT.webhook OWNER TO testuser;

--
-- Name: webhook_event_type; Type: TABLE; Schema: t_TENANT; Owner: testuser
--

CREATE TABLE t_TENANT.webhook_event_type (
                                             webhook_id character varying(50) NOT NULL,
                                             event_type character varying(50) NOT NULL
);


ALTER TABLE t_TENANT.webhook_event_type OWNER TO testuser;

--
-- Name: aggregate_file aggregate_file_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.aggregate_file
    ADD CONSTRAINT aggregate_file_pk PRIMARY KEY (aggregate_file_id);


--
-- Name: aggregate_file aggregate_file_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.aggregate_file
    ADD CONSTRAINT aggregate_file_uk UNIQUE (application_component_id, hash);


--
-- Name: application_component_license application_component_license_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application_component_license
    ADD CONSTRAINT application_component_license_pk PRIMARY KEY (application_component_license_id);


--
-- Name: application_component_license application_component_license_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application_component_license
    ADD CONSTRAINT application_component_license_uk UNIQUE (application_component_id, effective_license_id);


--
-- Name: application_component application_component_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application_component
    ADD CONSTRAINT application_component_pk PRIMARY KEY (application_component_id);


--
-- Name: application_component application_component_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application_component
    ADD CONSTRAINT application_component_uk UNIQUE (application_id, stage_type_id, hash);


--
-- Name: application application_name_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application
    ADD CONSTRAINT application_name_uk UNIQUE (name_lowercase_no_whitespace);


--
-- Name: application application_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application
    ADD CONSTRAINT application_pk PRIMARY KEY (application_id);


--
-- Name: application_tag application_tag_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application_tag
    ADD CONSTRAINT application_tag_pk PRIMARY KEY (application_tag_id);


--
-- Name: application_tag application_tag_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application_tag
    ADD CONSTRAINT application_tag_uk UNIQUE (application_id, tag_id);


--
-- Name: application application_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application
    ADD CONSTRAINT application_uk UNIQUE (public_id_lowercase);


--
-- Name: artifactory_connection artifactory_connection_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.artifactory_connection
    ADD CONSTRAINT artifactory_connection_pk PRIMARY KEY (artifactory_connection_id);


--
-- Name: attribution_report_template attribution_report_template_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.attribution_report_template
    ADD CONSTRAINT attribution_report_template_pkey PRIMARY KEY (attribution_report_template_id);


--
-- Name: auto_unquarantine_policy_condition_type auto_unquarantine_policy_condition_type_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.auto_unquarantine_policy_condition_type
    ADD CONSTRAINT auto_unquarantine_policy_condition_type_pk PRIMARY KEY (condition_type_id);


--
-- Name: component_copyright component_copyright_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_copyright
    ADD CONSTRAINT component_copyright_pk PRIMARY KEY (component_copyright_id);


--
-- Name: component_copyright component_copyright_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_copyright
    ADD CONSTRAINT component_copyright_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json);


--
-- Name: component_label component_label_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_label
    ADD CONSTRAINT component_label_pk PRIMARY KEY (component_label_id);


--
-- Name: component_label component_label_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_label
    ADD CONSTRAINT component_label_uk UNIQUE (owner_id, hash, label_id);


--
-- Name: component_legal_file component_legal_file_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_legal_file
    ADD CONSTRAINT component_legal_file_pk PRIMARY KEY (component_legal_file_id);


--
-- Name: component_legal_file component_legal_file_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_legal_file
    ADD CONSTRAINT component_legal_file_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json, type);


--
-- Name: component_obligation_attribution component_obligation_attribution_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_obligation_attribution
    ADD CONSTRAINT component_obligation_attribution_pk PRIMARY KEY (component_obligation_attribution_id);


--
-- Name: component_obligation component_obligation_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_obligation
    ADD CONSTRAINT component_obligation_pk PRIMARY KEY (component_obligation_id);


--
-- Name: component_obligation component_obligation_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_obligation
    ADD CONSTRAINT component_obligation_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json, obligation_name);


--
-- Name: component_source_link component_source_link_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_source_link
    ADD CONSTRAINT component_source_link_pk PRIMARY KEY (component_source_link_id);


--
-- Name: component_source_link component_source_link_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_source_link
    ADD CONSTRAINT component_source_link_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json);


--
-- Name: coordinate_security coordinate_security_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.coordinate_security
    ADD CONSTRAINT coordinate_security_pk PRIMARY KEY (coordinate_security_id);


--
-- Name: coordinate_security coordinate_security_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.coordinate_security
    ADD CONSTRAINT coordinate_security_uk UNIQUE (file_coordinate_id, ref_id);


--
-- Name: copyright_override copyright_override_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.copyright_override
    ADD CONSTRAINT copyright_override_pk PRIMARY KEY (copyright_override_id);


--
-- Name: crowd_configuration crowd_configuration_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.crowd_configuration
    ADD CONSTRAINT crowd_configuration_pk PRIMARY KEY (crowd_configuration_id);


--
-- Name: dashboard_filter dashboard_filter_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.dashboard_filter
    ADD CONSTRAINT dashboard_filter_pk PRIMARY KEY (dashboard_filter_id);


--
-- Name: dashboard_filter dashboard_filter_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.dashboard_filter
    ADD CONSTRAINT dashboard_filter_uk UNIQUE (username_lowercase, realm_id, name_lowercase_no_whitespace);


--
-- Name: data_retention_policy data_retention_policy_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.data_retention_policy
    ADD CONSTRAINT data_retention_policy_pk PRIMARY KEY (data_retention_policy_id);


--
-- Name: data_retention_policy data_retention_policy_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.data_retention_policy
    ADD CONSTRAINT data_retention_policy_uk UNIQUE (owner_id, context_id);


--
-- Name: deleted_tenant deleted_tenant_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.deleted_tenant
    ADD CONSTRAINT deleted_tenant_pk PRIMARY KEY (tenant_slug);


--
-- Name: file_coordinate file_coordinate_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.file_coordinate
    ADD CONSTRAINT file_coordinate_pk PRIMARY KEY (file_coordinate_id);


--
-- Name: hash_component_identifier hash_component_identifier_component_id_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.hash_component_identifier
    ADD CONSTRAINT hash_component_identifier_component_id_uk UNIQUE (component_id_format, component_id_coordinates_json);


--
-- Name: hash_component_identifier hash_component_identifier_hash_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.hash_component_identifier
    ADD CONSTRAINT hash_component_identifier_hash_uk UNIQUE (hash);


--
-- Name: hash_component_identifier hash_component_identifier_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.hash_component_identifier
    ADD CONSTRAINT hash_component_identifier_pk PRIMARY KEY (hash_component_identifier_id);


--
-- Name: repository_identified_component hash_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_identified_component
    ADD CONSTRAINT hash_pk PRIMARY KEY (hash);


--
-- Name: inner_source_component inner_source_component_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.inner_source_component
    ADD CONSTRAINT inner_source_component_pk PRIMARY KEY (inner_source_component_id);


--
-- Name: inner_source_component inner_source_component_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.inner_source_component
    ADD CONSTRAINT inner_source_component_uk UNIQUE (package_url);


--
-- Name: jira_configuration jira_configuration_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.jira_configuration
    ADD CONSTRAINT jira_configuration_pk PRIMARY KEY (jira_configuration_id);


--
-- Name: label label_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.label
    ADD CONSTRAINT label_pk PRIMARY KEY (label_id);


--
-- Name: label label_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.label
    ADD CONSTRAINT label_uk UNIQUE (owner_id, label_lowercase);


--
-- Name: last_policy_evaluation last_policy_evaluation_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.last_policy_evaluation
    ADD CONSTRAINT last_policy_evaluation_pk PRIMARY KEY (policy_evaluation_id);


--
-- Name: last_policy_evaluation last_policy_evaluation_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.last_policy_evaluation
    ADD CONSTRAINT last_policy_evaluation_uk UNIQUE (application_id, stage_type_id);


--
-- Name: ldap_connection ldap_connection_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.ldap_connection
    ADD CONSTRAINT ldap_connection_pk PRIMARY KEY (ldap_connection_id);


--
-- Name: ldap_connection ldap_connection_server_id_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.ldap_connection
    ADD CONSTRAINT ldap_connection_server_id_uk UNIQUE (ldap_server_id);


--
-- Name: ldap_server ldap_server_name_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.ldap_server
    ADD CONSTRAINT ldap_server_name_uk UNIQUE (name_lowercase_no_whitespace);


--
-- Name: ldap_server ldap_server_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.ldap_server
    ADD CONSTRAINT ldap_server_pk PRIMARY KEY (ldap_server_id);


--
-- Name: ldap_server ldap_server_priority_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.ldap_server
    ADD CONSTRAINT ldap_server_priority_uk UNIQUE (priority);


--
-- Name: ldap_usermapping ldap_usermapping_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.ldap_usermapping
    ADD CONSTRAINT ldap_usermapping_pk PRIMARY KEY (ldap_usermapping_id);


--
-- Name: ldap_usermapping ldap_usermapping_server_id_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.ldap_usermapping
    ADD CONSTRAINT ldap_usermapping_server_id_uk UNIQUE (ldap_server_id);


--
-- Name: legal_file_override legal_file_override_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.legal_file_override
    ADD CONSTRAINT legal_file_override_pk PRIMARY KEY (legal_file_override_id);


--
-- Name: coordinate_license license_coordinate_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.coordinate_license
    ADD CONSTRAINT license_coordinate_pk PRIMARY KEY (coordinate_license_id);


--
-- Name: coordinate_license license_coordinate_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.coordinate_license
    ADD CONSTRAINT license_coordinate_uk UNIQUE (license_id, file_coordinate_id);


--
-- Name: license_override_license license_override_license_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.license_override_license
    ADD CONSTRAINT license_override_license_pk PRIMARY KEY (license_override_license_id);


--
-- Name: license_override_license license_override_license_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.license_override_license
    ADD CONSTRAINT license_override_license_uk UNIQUE (license_override_id, license_id);


--
-- Name: license_override license_override_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.license_override
    ADD CONSTRAINT license_override_pk PRIMARY KEY (license_override_id);


--
-- Name: license_override license_override_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.license_override
    ADD CONSTRAINT license_override_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json);


--
-- Name: license_threat_group_license license_threat_group_license_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.license_threat_group_license
    ADD CONSTRAINT license_threat_group_license_pk PRIMARY KEY (license_threat_group_license_id);


--
-- Name: license_threat_group_license license_threat_group_license_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.license_threat_group_license
    ADD CONSTRAINT license_threat_group_license_uk UNIQUE (license_threat_group_id, license_id);


--
-- Name: license_threat_group license_threat_group_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.license_threat_group
    ADD CONSTRAINT license_threat_group_pk PRIMARY KEY (license_threat_group_id);


--
-- Name: license_threat_group license_threat_group_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.license_threat_group
    ADD CONSTRAINT license_threat_group_uk UNIQUE (owner_id, name_lowercase_no_whitespace);


--
-- Name: lock lock_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.lock
    ADD CONSTRAINT lock_pk PRIMARY KEY (lock_id);


--
-- Name: mail_configuration mail_configuration_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.mail_configuration
    ADD CONSTRAINT mail_configuration_pk PRIMARY KEY (mail_configuration_id);


--
-- Name: membership_mapping membership_mapping_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.membership_mapping
    ADD CONSTRAINT membership_mapping_pk PRIMARY KEY (membership_mapping_id);


--
-- Name: membership_mapping membership_mapping_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.membership_mapping
    ADD CONSTRAINT membership_mapping_uk UNIQUE (context_id, role_id, member_name, member_type);


--
-- Name: migration_tracker migration_tracker_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.migration_tracker
    ADD CONSTRAINT migration_tracker_pk PRIMARY KEY (migration_tracker_id);


--
-- Name: user_viewed_product_notification notification_viewed_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.user_viewed_product_notification
    ADD CONSTRAINT notification_viewed_pk PRIMARY KEY (user_viewed_product_notification_id);


--
-- Name: user_viewed_product_notification notification_viewed_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.user_viewed_product_notification
    ADD CONSTRAINT notification_viewed_uk UNIQUE (notification_id, username_lowercase, realm_id);


--
-- Name: organization organization_name_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.organization
    ADD CONSTRAINT organization_name_uk UNIQUE (name_lowercase_no_whitespace);


--
-- Name: organization organization_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.organization
    ADD CONSTRAINT organization_pk PRIMARY KEY (organization_id);


--
-- Name: perpetual_lock perpetual_lock_id_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.perpetual_lock
    ADD CONSTRAINT perpetual_lock_id_pk PRIMARY KEY (perpetual_lock_id);


--
-- Name: persisted_policy_evaluation_polling_result persisted_policy_evaluation_polling_result_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.persisted_policy_evaluation_polling_result
    ADD CONSTRAINT persisted_policy_evaluation_polling_result_pk PRIMARY KEY (persisted_policy_evaluation_polling_result_id);


--
-- Name: persisted_policy_evaluation_polling_result persisted_policy_evaluation_polling_result_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.persisted_policy_evaluation_polling_result
    ADD CONSTRAINT persisted_policy_evaluation_polling_result_uk UNIQUE (application_id, status_id);


--
-- Name: persisted_promote_scan_result persisted_promote_scan_result_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.persisted_promote_scan_result
    ADD CONSTRAINT persisted_promote_scan_result_pk PRIMARY KEY (persisted_promote_scan_result_id);


--
-- Name: persisted_scan_ticket persisted_scan_ticket_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.persisted_scan_ticket
    ADD CONSTRAINT persisted_scan_ticket_pk PRIMARY KEY (persisted_scan_ticket_id);


--
-- Name: persisted_user_session persisted_user_session_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.persisted_user_session
    ADD CONSTRAINT persisted_user_session_pk PRIMARY KEY (persisted_user_session_id);


--
-- Name: policy_evaluation policy_evaluation_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_evaluation
    ADD CONSTRAINT policy_evaluation_pk PRIMARY KEY (policy_evaluation_id);


--
-- Name: policy_monitoring policy_monitoring_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_monitoring
    ADD CONSTRAINT policy_monitoring_pk PRIMARY KEY (policy_monitoring_id);


--
-- Name: policy_monitoring policy_monitoring_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_monitoring
    ADD CONSTRAINT policy_monitoring_uk UNIQUE (owner_id);


--
-- Name: policy policy_name_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy
    ADD CONSTRAINT policy_name_uk UNIQUE (owner_id, name_lowercase_no_whitespace);


--
-- Name: policy policy_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy
    ADD CONSTRAINT policy_pk PRIMARY KEY (policy_id);


--
-- Name: policy_tag policy_tag_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_tag
    ADD CONSTRAINT policy_tag_pk PRIMARY KEY (policy_tag_id);


--
-- Name: policy_tag policy_tag_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_tag
    ADD CONSTRAINT policy_tag_uk UNIQUE (policy_id, tag_id);


--
-- Name: policy_violation_aggregation policy_violation_aggregation_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_violation_aggregation
    ADD CONSTRAINT policy_violation_aggregation_pk PRIMARY KEY (policy_violation_aggregation_id);


--
-- Name: policy_violation_aggregation policy_violation_aggregation_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_violation_aggregation
    ADD CONSTRAINT policy_violation_aggregation_uk UNIQUE (application_id, time_period_start, time_period);


--
-- Name: policy_violation policy_violation_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_violation
    ADD CONSTRAINT policy_violation_pk PRIMARY KEY (policy_violation_id);


--
-- Name: policy_waiver policy_waiver_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_waiver
    ADD CONSTRAINT policy_waiver_pk PRIMARY KEY (policy_waiver_id);


--
-- Name: product_license product_license_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.product_license
    ADD CONSTRAINT product_license_pk PRIMARY KEY (product_license_id);


--
-- Name: proprietary_component_name_pattern proprietary_component_name_pattern_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.proprietary_component_name_pattern
    ADD CONSTRAINT proprietary_component_name_pattern_pk PRIMARY KEY (proprietary_component_name_pattern_id);


--
-- Name: proprietary_component_name_pattern proprietary_component_name_pattern_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.proprietary_component_name_pattern
    ADD CONSTRAINT proprietary_component_name_pattern_uk UNIQUE (format, namespace_pattern, name_pattern, repository_id);


--
-- Name: proprietary_config proprietary_config_owner_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.proprietary_config
    ADD CONSTRAINT proprietary_config_owner_uk UNIQUE (owner_id);


--
-- Name: proprietary_config proprietary_config_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.proprietary_config
    ADD CONSTRAINT proprietary_config_pk PRIMARY KEY (proprietary_config_id);


--
-- Name: proxy_server_configuration proxy_server_configuration_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.proxy_server_configuration
    ADD CONSTRAINT proxy_server_configuration_pk PRIMARY KEY (proxy_server_configuration_id);


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_calendars qrtz_calendars_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_calendars
    ADD CONSTRAINT qrtz_calendars_pkey PRIMARY KEY (sched_name, calendar_name);


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_fired_triggers qrtz_fired_triggers_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_fired_triggers
    ADD CONSTRAINT qrtz_fired_triggers_pkey PRIMARY KEY (sched_name, entry_id);


--
-- Name: qrtz_job_details qrtz_job_details_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_job_details
    ADD CONSTRAINT qrtz_job_details_pkey PRIMARY KEY (sched_name, job_name, job_group);


--
-- Name: qrtz_locks qrtz_locks_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_locks
    ADD CONSTRAINT qrtz_locks_pkey PRIMARY KEY (sched_name, lock_name);


--
-- Name: qrtz_paused_trigger_grps qrtz_paused_trigger_grps_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_paused_trigger_grps
    ADD CONSTRAINT qrtz_paused_trigger_grps_pkey PRIMARY KEY (sched_name, trigger_group);


--
-- Name: qrtz_scheduler_state qrtz_scheduler_state_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_scheduler_state
    ADD CONSTRAINT qrtz_scheduler_state_pkey PRIMARY KEY (sched_name, instance_name);


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_triggers qrtz_triggers_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: quarantined_component_access quarantined_component_access_pkey; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.quarantined_component_access
    ADD CONSTRAINT quarantined_component_access_pkey PRIMARY KEY (quarantined_component_access_id);


--
-- Name: repository_client_configuration repository_client_configuration_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_client_configuration
    ADD CONSTRAINT repository_client_configuration_pk PRIMARY KEY (repository_client_configuration_id);


--
-- Name: repository_component repository_component_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_component
    ADD CONSTRAINT repository_component_pk PRIMARY KEY (repository_component_id);


--
-- Name: repository_component repository_component_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_component
    ADD CONSTRAINT repository_component_uk UNIQUE (repository_id, pathname);


--
-- Name: repository_connection repository_connection_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_connection
    ADD CONSTRAINT repository_connection_pk PRIMARY KEY (repository_connection_id);


--
-- Name: repository_connection repository_connection_url_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_connection
    ADD CONSTRAINT repository_connection_url_uk UNIQUE (owner_id, format);


--
-- Name: repository_migration repository_id_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_migration
    ADD CONSTRAINT repository_id_uk UNIQUE (repository_id);


--
-- Name: repository_manager repository_manager_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_manager
    ADD CONSTRAINT repository_manager_pk PRIMARY KEY (repository_manager_id);


--
-- Name: repository_manager repository_manager_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_manager
    ADD CONSTRAINT repository_manager_uk UNIQUE (instance_id);


--
-- Name: repository_migration repository_migration_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_migration
    ADD CONSTRAINT repository_migration_pk PRIMARY KEY (repository_migration_id);


--
-- Name: repository repository_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository
    ADD CONSTRAINT repository_pk PRIMARY KEY (repository_id);


--
-- Name: repository_policy_violation repository_policy_violation_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_policy_violation
    ADD CONSTRAINT repository_policy_violation_pk PRIMARY KEY (repository_policy_violation_id);


--
-- Name: repository repository_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository
    ADD CONSTRAINT repository_uk UNIQUE (repository_manager_id, public_id);


--
-- Name: reverse_proxy_authentication_configuration reverse_proxy_authentication_configuration_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.reverse_proxy_authentication_configuration
    ADD CONSTRAINT reverse_proxy_authentication_configuration_pk PRIMARY KEY (reverse_proxy_authentication_configuration_id);


--
-- Name: role role_name_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.role
    ADD CONSTRAINT role_name_uk UNIQUE (name_lowercase_no_whitespace);


--
-- Name: role_permission role_permission_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.role_permission
    ADD CONSTRAINT role_permission_pk PRIMARY KEY (role_permission_id);


--
-- Name: role_permission role_permission_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.role_permission
    ADD CONSTRAINT role_permission_uk UNIQUE (role_id, permission);


--
-- Name: role role_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.role
    ADD CONSTRAINT role_pk PRIMARY KEY (role_id);


--
-- Name: saml_configuration saml_configuration_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.saml_configuration
    ADD CONSTRAINT saml_configuration_pk PRIMARY KEY (saml_configuration_id);


--
-- Name: saml_group saml_group_name_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.saml_group
    ADD CONSTRAINT saml_group_name_uk UNIQUE (name);


--
-- Name: saml_group saml_group_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.saml_group
    ADD CONSTRAINT saml_group_pk PRIMARY KEY (saml_group_id);


--
-- Name: saml_user_group saml_user_group_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.saml_user_group
    ADD CONSTRAINT saml_user_group_pk PRIMARY KEY (saml_user_group_id);


--
-- Name: saml_user_group saml_user_group_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.saml_user_group
    ADD CONSTRAINT saml_user_group_uk UNIQUE (saml_user_id, saml_group_id);


--
-- Name: saml_user saml_user_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.saml_user
    ADD CONSTRAINT saml_user_pk PRIMARY KEY (saml_user_id);


--
-- Name: saml_user saml_user_username_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.saml_user
    ADD CONSTRAINT saml_user_username_uk UNIQUE (username);


--
-- Name: search_index_change search_index_change_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.search_index_change
    ADD CONSTRAINT search_index_change_pk PRIMARY KEY (search_index_change_id);


--
-- Name: source_control_configuration source_control_configuration_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_configuration
    ADD CONSTRAINT source_control_configuration_pk PRIMARY KEY (source_control_configuration_id);


--
-- Name: source_control_default_branch_commit_history source_control_default_branch_commit_history_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_default_branch_commit_history
    ADD CONSTRAINT source_control_default_branch_commit_history_pk PRIMARY KEY (source_control_default_branch_commit_history_id);


--
-- Name: source_control_default_branch_commit_history source_control_default_branch_commit_history_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_default_branch_commit_history
    ADD CONSTRAINT source_control_default_branch_commit_history_uk UNIQUE (application_id, commit_hash);


--
-- Name: source_control_event source_control_event_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_event
    ADD CONSTRAINT source_control_event_pk PRIMARY KEY (source_control_event_id);


--
-- Name: source_control_organization_import_event source_control_organization_import_event_id; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_organization_import_event
    ADD CONSTRAINT source_control_organization_import_event_id PRIMARY KEY (source_control_organization_import_event_id);


--
-- Name: source_control source_control_owner_id_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control
    ADD CONSTRAINT source_control_owner_id_uk UNIQUE (owner_id);


--
-- Name: source_control source_control_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control
    ADD CONSTRAINT source_control_pk PRIMARY KEY (source_control_id);


--
-- Name: source_control_pull_request_comment source_control_pull_request_comment_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_pull_request_comment
    ADD CONSTRAINT source_control_pull_request_comment_pk PRIMARY KEY (source_control_pull_request_comment_id);


--
-- Name: source_control_pull_request_comment source_control_pull_request_comment_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_pull_request_comment
    ADD CONSTRAINT source_control_pull_request_comment_uk UNIQUE (application_id, component_hash, pull_request_id, pathname);


--
-- Name: source_control_pull_request source_control_pull_request_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_pull_request
    ADD CONSTRAINT source_control_pull_request_pk PRIMARY KEY (source_control_pull_request_id);


--
-- Name: source_control_pull_request_result source_control_pull_request_result_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_pull_request_result
    ADD CONSTRAINT source_control_pull_request_result_pk PRIMARY KEY (source_control_pull_request_result_id);


--
-- Name: source_control_pull_request source_control_pull_request_uk1; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_pull_request
    ADD CONSTRAINT source_control_pull_request_uk1 UNIQUE (repository_url, pull_request_id);


--
-- Name: source_link_override source_link_override_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_link_override
    ADD CONSTRAINT source_link_override_pk PRIMARY KEY (source_link_override_id);


--
-- Name: success_metrics_report_data success_metrics_report_data_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.success_metrics_report_data
    ADD CONSTRAINT success_metrics_report_data_pk PRIMARY KEY (success_metrics_report_data_id);


--
-- Name: success_metrics_report success_metrics_report_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.success_metrics_report
    ADD CONSTRAINT success_metrics_report_pk PRIMARY KEY (success_metrics_report_id);


--
-- Name: success_metrics_report success_metrics_report_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.success_metrics_report
    ADD CONSTRAINT success_metrics_report_uk UNIQUE (username, name_lowercase_no_whitespace);


--
-- Name: sv_override sv_override_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.sv_override
    ADD CONSTRAINT sv_override_pk PRIMARY KEY (sv_override_id);


--
-- Name: sv_override sv_override_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.sv_override
    ADD CONSTRAINT sv_override_uk UNIQUE (owner_id, hash, source, reference_id);


--
-- Name: system_configuration_property system_configuration_property_name_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.system_configuration_property
    ADD CONSTRAINT system_configuration_property_name_uk UNIQUE (name);


--
-- Name: system_configuration_property system_configuration_property_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.system_configuration_property
    ADD CONSTRAINT system_configuration_property_pk PRIMARY KEY (system_configuration_property_id);


--
-- Name: system_notice system_notice_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.system_notice
    ADD CONSTRAINT system_notice_pk PRIMARY KEY (system_notice_id);


--
-- Name: tag tag_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.tag
    ADD CONSTRAINT tag_pk PRIMARY KEY (tag_id);


--
-- Name: tag tag_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.tag
    ADD CONSTRAINT tag_uk UNIQUE (organization_id, name_lowercase_no_whitespace);


--
-- Name: attribution_report_template template_name_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.attribution_report_template
    ADD CONSTRAINT template_name_uk UNIQUE (template_name);


--
-- Name: tenant_metadata tenant_metadata_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.tenant_metadata
    ADD CONSTRAINT tenant_metadata_pk PRIMARY KEY (tenant_metadata_id);


--
-- Name: third_party_file third_party_file_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.third_party_file
    ADD CONSTRAINT third_party_file_pk PRIMARY KEY (third_party_file_id);


--
-- Name: third_party_scan third_party_scan_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.third_party_scan
    ADD CONSTRAINT third_party_scan_pk PRIMARY KEY (third_party_scan_id);


--
-- Name: third_party_scan third_party_scan_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.third_party_scan
    ADD CONSTRAINT third_party_scan_uk UNIQUE (third_party_file_id, scan_request_id);


--
-- Name: third_party_vulnerability third_party_vulnerability_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.third_party_vulnerability
    ADD CONSTRAINT third_party_vulnerability_pk PRIMARY KEY (third_party_vulnerability_id);


--
-- Name: third_party_vulnerability third_party_vulnerability_refid_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.third_party_vulnerability
    ADD CONSTRAINT third_party_vulnerability_refid_uk UNIQUE (ref_id);


--
-- Name: user_filter user_filter_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.user_filter
    ADD CONSTRAINT user_filter_pk PRIMARY KEY (user_filter_id);


--
-- Name: user_filter user_filter_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.user_filter
    ADD CONSTRAINT user_filter_uk UNIQUE (username_lowercase, realm_id, name_lowercase_no_whitespace, filter_type);


--
-- Name: user_ide_policy_evaluation user_ide_policy_evaluation_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.user_ide_policy_evaluation
    ADD CONSTRAINT user_ide_policy_evaluation_pk PRIMARY KEY (user_ide_policy_evaluation_id);


--
-- Name: user user_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT."user"
    ADD CONSTRAINT user_pk PRIMARY KEY (user_id);


--
-- Name: user_token user_token_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.user_token
    ADD CONSTRAINT user_token_pk PRIMARY KEY (user_token_id);


--
-- Name: user_token user_token_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.user_token
    ADD CONSTRAINT user_token_uk UNIQUE (username, realm_id);


--
-- Name: user_token user_token_user_code_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.user_token
    ADD CONSTRAINT user_token_user_code_uk UNIQUE (user_code);


--
-- Name: user user_username_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT."user"
    ADD CONSTRAINT user_username_uk UNIQUE (username_lowercase);


--
-- Name: user_ide_policy_evaluation username_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.user_ide_policy_evaluation
    ADD CONSTRAINT username_uk UNIQUE (username);


--
-- Name: vulnerability_custom_cvss_severity vulnerability_custom_cvss_severity_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_severity
    ADD CONSTRAINT vulnerability_custom_cvss_severity_pk PRIMARY KEY (vulnerability_custom_cvss_severity_id);


--
-- Name: vulnerability_custom_cvss_severity_tag vulnerability_custom_cvss_severity_tag_id; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_severity_tag
    ADD CONSTRAINT vulnerability_custom_cvss_severity_tag_id PRIMARY KEY (vulnerability_custom_cvss_severity_tag_id);


--
-- Name: vulnerability_custom_cvss_severity_tag vulnerability_custom_cvss_severity_tag_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_severity_tag
    ADD CONSTRAINT vulnerability_custom_cvss_severity_tag_uk UNIQUE (vulnerability_custom_cvss_severity_id, tag_id);


--
-- Name: vulnerability_custom_cvss_severity vulnerability_custom_cvss_severity_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_severity
    ADD CONSTRAINT vulnerability_custom_cvss_severity_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json);


--
-- Name: vulnerability_custom_cvss_vector vulnerability_custom_cvss_vector_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_vector
    ADD CONSTRAINT vulnerability_custom_cvss_vector_pk PRIMARY KEY (vulnerability_custom_cvss_vector_id);


--
-- Name: vulnerability_custom_cvss_vector_tag vulnerability_custom_cvss_vector_tag_id; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_vector_tag
    ADD CONSTRAINT vulnerability_custom_cvss_vector_tag_id PRIMARY KEY (vulnerability_custom_cvss_vector_tag_id);


--
-- Name: vulnerability_custom_cvss_vector_tag vulnerability_custom_cvss_vector_tag_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_vector_tag
    ADD CONSTRAINT vulnerability_custom_cvss_vector_tag_uk UNIQUE (vulnerability_custom_cvss_vector_id, tag_id);


--
-- Name: vulnerability_custom_cvss_vector vulnerability_custom_cvss_vector_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_vector
    ADD CONSTRAINT vulnerability_custom_cvss_vector_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json);


--
-- Name: vulnerability_custom_cwe vulnerability_custom_cwe_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cwe
    ADD CONSTRAINT vulnerability_custom_cwe_pk PRIMARY KEY (vulnerability_custom_cwe_id);


--
-- Name: vulnerability_custom_cwe_tag vulnerability_custom_cwe_tag_id; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cwe_tag
    ADD CONSTRAINT vulnerability_custom_cwe_tag_id PRIMARY KEY (vulnerability_custom_cwe_tag_id);


--
-- Name: vulnerability_custom_cwe_tag vulnerability_custom_cwe_tag_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cwe_tag
    ADD CONSTRAINT vulnerability_custom_cwe_tag_uk UNIQUE (vulnerability_custom_cwe_id, tag_id);


--
-- Name: vulnerability_custom_cwe vulnerability_custom_cwe_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cwe
    ADD CONSTRAINT vulnerability_custom_cwe_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json);


--
-- Name: vulnerability_custom_remediation vulnerability_custom_remediation_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_remediation
    ADD CONSTRAINT vulnerability_custom_remediation_pk PRIMARY KEY (vulnerability_custom_remediation_id);


--
-- Name: vulnerability_custom_remediation_tag vulnerability_custom_remediation_tag_id; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_remediation_tag
    ADD CONSTRAINT vulnerability_custom_remediation_tag_id PRIMARY KEY (vulnerability_custom_remediation_tag_id);


--
-- Name: vulnerability_custom_remediation_tag vulnerability_custom_remediation_tag_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_remediation_tag
    ADD CONSTRAINT vulnerability_custom_remediation_tag_uk UNIQUE (vulnerability_custom_remediation_id, tag_id);


--
-- Name: vulnerability_custom_remediation vulnerability_custom_remediation_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_remediation
    ADD CONSTRAINT vulnerability_custom_remediation_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json);


--
-- Name: vulnerability_group vulnerability_group_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_group
    ADD CONSTRAINT vulnerability_group_pk PRIMARY KEY (vulnerability_group_id);


--
-- Name: vulnerability_group vulnerability_group_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_group
    ADD CONSTRAINT vulnerability_group_uk UNIQUE (owner_id, name);


--
-- Name: vulnerability_group_vulnerability vulnerability_group_vulnerability_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_group_vulnerability
    ADD CONSTRAINT vulnerability_group_vulnerability_pk PRIMARY KEY (vulnerability_group_vulnerability_id);


--
-- Name: vulnerability_group_vulnerability vulnerability_group_vulnerability_uk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_group_vulnerability
    ADD CONSTRAINT vulnerability_group_vulnerability_uk UNIQUE (vulnerability_group_id, vulnerability_refid);


--
-- Name: webhook_event_type webhook_event_type_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.webhook_event_type
    ADD CONSTRAINT webhook_event_type_pk PRIMARY KEY (webhook_id, event_type);


--
-- Name: webhook webhook_pk; Type: CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.webhook
    ADD CONSTRAINT webhook_pk PRIMARY KEY (webhook_id);


--
-- Name: aggregate_file_application_component_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX aggregate_file_application_component_id_idx ON t_TENANT.aggregate_file USING btree (application_component_id);


--
-- Name: application_component_hash_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX application_component_hash_idx ON t_TENANT.application_component USING btree (hash);


--
-- Name: application_component_license_effective_license_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX application_component_license_effective_license_id_idx ON t_TENANT.application_component_license USING btree (effective_license_id);


--
-- Name: application_component_time_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX application_component_time_idx ON t_TENANT.application_component USING btree ("time");


--
-- Name: component_copyright_owner_component_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX component_copyright_owner_component_idx ON t_TENANT.component_copyright USING btree (owner_id, component_id_format, component_id_coordinates_json);


--
-- Name: component_legal_file_owner_component_type_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX component_legal_file_owner_component_type_idx ON t_TENANT.component_legal_file USING btree (owner_id, component_id_format, component_id_coordinates_json, type);


--
-- Name: component_obligation_attribution_owner_component_obligation_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX component_obligation_attribution_owner_component_obligation_idx ON t_TENANT.component_obligation_attribution USING btree (owner_id, component_id_format, component_id_coordinates_json, obligation_name);


--
-- Name: component_obligation_owner_component_obligation_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX component_obligation_owner_component_obligation_idx ON t_TENANT.component_obligation USING btree (owner_id, component_id_format, component_id_coordinates_json, obligation_name);


--
-- Name: component_source_link_owner_component_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX component_source_link_owner_component_idx ON t_TENANT.component_source_link USING btree (owner_id, component_id_format, component_id_coordinates_json);


--
-- Name: copyright_override_component_copyright_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX copyright_override_component_copyright_id_idx ON t_TENANT.copyright_override USING btree (component_copyright_id);


--
-- Name: idx_qrtz_ft_inst_job_req_rcvry; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_ft_inst_job_req_rcvry ON t_TENANT.qrtz_fired_triggers USING btree (sched_name, instance_name, requests_recovery);


--
-- Name: idx_qrtz_ft_j_g; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_ft_j_g ON t_TENANT.qrtz_fired_triggers USING btree (sched_name, job_name, job_group);


--
-- Name: idx_qrtz_ft_jg; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_ft_jg ON t_TENANT.qrtz_fired_triggers USING btree (sched_name, job_group);


--
-- Name: idx_qrtz_ft_t_g; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_ft_t_g ON t_TENANT.qrtz_fired_triggers USING btree (sched_name, trigger_name, trigger_group);


--
-- Name: idx_qrtz_ft_tg; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_ft_tg ON t_TENANT.qrtz_fired_triggers USING btree (sched_name, trigger_group);


--
-- Name: idx_qrtz_ft_trig_inst_name; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_ft_trig_inst_name ON t_TENANT.qrtz_fired_triggers USING btree (sched_name, instance_name);


--
-- Name: idx_qrtz_j_grp; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_j_grp ON t_TENANT.qrtz_job_details USING btree (sched_name, job_group);


--
-- Name: idx_qrtz_j_req_recovery; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_j_req_recovery ON t_TENANT.qrtz_job_details USING btree (sched_name, requests_recovery);


--
-- Name: idx_qrtz_t_c; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_c ON t_TENANT.qrtz_triggers USING btree (sched_name, calendar_name);


--
-- Name: idx_qrtz_t_g; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_g ON t_TENANT.qrtz_triggers USING btree (sched_name, trigger_group);


--
-- Name: idx_qrtz_t_j; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_j ON t_TENANT.qrtz_triggers USING btree (sched_name, job_name, job_group);


--
-- Name: idx_qrtz_t_jg; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_jg ON t_TENANT.qrtz_triggers USING btree (sched_name, job_group);


--
-- Name: idx_qrtz_t_n_g_state; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_n_g_state ON t_TENANT.qrtz_triggers USING btree (sched_name, trigger_group, trigger_state);


--
-- Name: idx_qrtz_t_n_state; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_n_state ON t_TENANT.qrtz_triggers USING btree (sched_name, trigger_name, trigger_group, trigger_state);


--
-- Name: idx_qrtz_t_next_fire_time; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_next_fire_time ON t_TENANT.qrtz_triggers USING btree (sched_name, next_fire_time);


--
-- Name: idx_qrtz_t_nft_misfire; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_nft_misfire ON t_TENANT.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time);


--
-- Name: idx_qrtz_t_nft_st; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_nft_st ON t_TENANT.qrtz_triggers USING btree (sched_name, trigger_state, next_fire_time);


--
-- Name: idx_qrtz_t_nft_st_misfire; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_nft_st_misfire ON t_TENANT.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time, trigger_state);


--
-- Name: idx_qrtz_t_nft_st_misfire_grp; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_nft_st_misfire_grp ON t_TENANT.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time, trigger_group, trigger_state);


--
-- Name: idx_qrtz_t_state; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX idx_qrtz_t_state ON t_TENANT.qrtz_triggers USING btree (sched_name, trigger_state);


--
-- Name: legal_file_override_component_legal_file_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX legal_file_override_component_legal_file_id_idx ON t_TENANT.legal_file_override USING btree (component_legal_file_id);


--
-- Name: membership_mapping_member_name_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX membership_mapping_member_name_idx ON t_TENANT.membership_mapping USING btree (member_name);


--
-- Name: persisted_policy_evaluation_polling_result_create_time_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX persisted_policy_evaluation_polling_result_create_time_idx ON t_TENANT.persisted_policy_evaluation_polling_result USING btree (create_time);


--
-- Name: persisted_promote_scan_result_create_time_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX persisted_promote_scan_result_create_time_idx ON t_TENANT.persisted_promote_scan_result USING btree (create_time);


--
-- Name: persisted_scan_ticket_create_time_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX persisted_scan_ticket_create_time_idx ON t_TENANT.persisted_scan_ticket USING btree (create_time);


--
-- Name: policy_evaluation_app_monitoring_stage_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX policy_evaluation_app_monitoring_stage_idx ON t_TENANT.policy_evaluation USING btree (application_id, for_monitoring, stage_type_id);


--
-- Name: policy_evaluation_commit_hash_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX policy_evaluation_commit_hash_idx ON t_TENANT.policy_evaluation USING btree (commit_hash);


--
-- Name: policy_evaluation_scan_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX policy_evaluation_scan_id_idx ON t_TENANT.policy_evaluation USING btree (scan_id);


--
-- Name: policy_evaluation_time_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX policy_evaluation_time_idx ON t_TENANT.policy_evaluation USING btree ("time");


--
-- Name: policy_violation_app_fix_time_stage_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX policy_violation_app_fix_time_stage_idx ON t_TENANT.policy_violation USING btree (application_id, fix_time, stage_type_id);


--
-- Name: policy_violation_hash_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX policy_violation_hash_idx ON t_TENANT.policy_violation USING btree (hash);


--
-- Name: policy_violation_policy_app_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX policy_violation_policy_app_idx ON t_TENANT.policy_violation USING btree (policy_id, application_id);


--
-- Name: policy_waiver_owner_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX policy_waiver_owner_id_idx ON t_TENANT.policy_waiver USING btree (owner_id);


--
-- Name: proprietary_component_name_pattern_repo_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX proprietary_component_name_pattern_repo_idx ON t_TENANT.proprietary_component_name_pattern USING btree (repository_id);


--
-- Name: quarantined_component_access_repository_component_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX quarantined_component_access_repository_component_id_idx ON t_TENANT.quarantined_component_access USING btree (repository_component_id);


--
-- Name: quarantined_component_access_repository_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX quarantined_component_access_repository_id_idx ON t_TENANT.quarantined_component_access USING btree (repository_id);


--
-- Name: repository_component_component_coordinates_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX repository_component_component_coordinates_idx ON t_TENANT.repository_component USING btree (component_id_format, component_id_coordinates_json);


--
-- Name: repository_component_hash_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX repository_component_hash_idx ON t_TENANT.repository_component USING btree (hash);


--
-- Name: repository_component_quarantine_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX repository_component_quarantine_idx ON t_TENANT.repository_component USING btree (repository_id, quarantine_time);


--
-- Name: repository_component_release_quarantine_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX repository_component_release_quarantine_idx ON t_TENANT.repository_component USING btree (quarantine_time, unquarantine_time, auto_unquarantined);


--
-- Name: repository_component_repository_unquarantine_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX repository_component_repository_unquarantine_idx ON t_TENANT.repository_component USING btree (repository_id, unquarantine_time);


--
-- Name: repository_policy_violation_pathname_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX repository_policy_violation_pathname_idx ON t_TENANT.repository_policy_violation USING btree (pathname);


--
-- Name: repository_policy_violation_repository_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX repository_policy_violation_repository_id_idx ON t_TENANT.repository_policy_violation USING btree (repository_id);


--
-- Name: source_control_event_create_time_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX source_control_event_create_time_idx ON t_TENANT.source_control_event USING btree (create_time);


--
-- Name: source_control_event_event_status_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX source_control_event_event_status_idx ON t_TENANT.source_control_event USING btree (event_status);


--
-- Name: source_control_event_instance_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX source_control_event_instance_id_idx ON t_TENANT.source_control_event USING btree (instance_id);


--
-- Name: source_control_normalized_repository_url_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX source_control_normalized_repository_url_idx ON t_TENANT.source_control USING btree (normalized_repository_url);


--
-- Name: source_control_pull_request_last_detected_update_time_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX source_control_pull_request_last_detected_update_time_idx ON t_TENANT.source_control_pull_request USING btree (last_detected_update_time);


--
-- Name: source_control_pull_request_result_application_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX source_control_pull_request_result_application_id_idx ON t_TENANT.source_control_pull_request_result USING btree (application_id);


--
-- Name: source_link_override_component_source_link_id_idx; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX source_link_override_component_source_link_id_idx ON t_TENANT.source_link_override USING btree (component_source_link_id);


--
-- Name: third_party_scan_scan_id; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX third_party_scan_scan_id ON t_TENANT.third_party_scan USING btree (scan_id);


--
-- Name: third_party_scan_scan_request_id; Type: INDEX; Schema: t_TENANT; Owner: testuser
--

CREATE INDEX third_party_scan_scan_request_id ON t_TENANT.third_party_scan USING btree (scan_request_id);


--
-- Name: aggregate_file aggregate_file_application_component_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.aggregate_file
    ADD CONSTRAINT aggregate_file_application_component_fk FOREIGN KEY (application_component_id) REFERENCES t_TENANT.application_component(application_component_id);


--
-- Name: application_component application_component_application_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application_component
    ADD CONSTRAINT application_component_application_fk FOREIGN KEY (application_id) REFERENCES t_TENANT.application(application_id);


--
-- Name: application_component_license application_component_license_application_component_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application_component_license
    ADD CONSTRAINT application_component_license_application_component_fk FOREIGN KEY (application_component_id) REFERENCES t_TENANT.application_component(application_component_id);


--
-- Name: application application_organization_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application
    ADD CONSTRAINT application_organization_fk FOREIGN KEY (organization_id) REFERENCES t_TENANT.organization(organization_id);


--
-- Name: application_tag application_tag_app_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application_tag
    ADD CONSTRAINT application_tag_app_fk FOREIGN KEY (application_id) REFERENCES t_TENANT.application(application_id);


--
-- Name: application_tag application_tag_tag_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.application_tag
    ADD CONSTRAINT application_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES t_TENANT.tag(tag_id);


--
-- Name: component_label component_label_label_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.component_label
    ADD CONSTRAINT component_label_label_fk FOREIGN KEY (label_id) REFERENCES t_TENANT.label(label_id);


--
-- Name: coordinate_security coordinate_security_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.coordinate_security
    ADD CONSTRAINT coordinate_security_fk FOREIGN KEY (file_coordinate_id) REFERENCES t_TENANT.file_coordinate(file_coordinate_id);


--
-- Name: copyright_override copyright_override_component_copyright_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.copyright_override
    ADD CONSTRAINT copyright_override_component_copyright_fk FOREIGN KEY (component_copyright_id) REFERENCES t_TENANT.component_copyright(component_copyright_id);


--
-- Name: file_coordinate file_coordinate_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.file_coordinate
    ADD CONSTRAINT file_coordinate_fk FOREIGN KEY (third_party_file_id) REFERENCES t_TENANT.third_party_file(third_party_file_id);


--
-- Name: inner_source_component inner_source_component_application_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.inner_source_component
    ADD CONSTRAINT inner_source_component_application_fk FOREIGN KEY (application_id) REFERENCES t_TENANT.application(application_id);


--
-- Name: last_policy_evaluation last_policy_evaluation_app_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.last_policy_evaluation
    ADD CONSTRAINT last_policy_evaluation_app_fk FOREIGN KEY (application_id) REFERENCES t_TENANT.application(application_id);


--
-- Name: last_policy_evaluation last_policy_evaluation_eval_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.last_policy_evaluation
    ADD CONSTRAINT last_policy_evaluation_eval_fk FOREIGN KEY (policy_evaluation_id) REFERENCES t_TENANT.policy_evaluation(policy_evaluation_id);


--
-- Name: ldap_connection ldap_connection_server_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.ldap_connection
    ADD CONSTRAINT ldap_connection_server_fk FOREIGN KEY (ldap_server_id) REFERENCES t_TENANT.ldap_server(ldap_server_id);


--
-- Name: ldap_usermapping ldap_usermapping_server_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.ldap_usermapping
    ADD CONSTRAINT ldap_usermapping_server_fk FOREIGN KEY (ldap_server_id) REFERENCES t_TENANT.ldap_server(ldap_server_id);


--
-- Name: legal_file_override legal_file_override_component_legal_file_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.legal_file_override
    ADD CONSTRAINT legal_file_override_component_legal_file_fk FOREIGN KEY (component_legal_file_id) REFERENCES t_TENANT.component_legal_file(component_legal_file_id);


--
-- Name: coordinate_license license_coordinate_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.coordinate_license
    ADD CONSTRAINT license_coordinate_fk FOREIGN KEY (file_coordinate_id) REFERENCES t_TENANT.file_coordinate(file_coordinate_id);


--
-- Name: license_override_license license_override_license_override_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.license_override_license
    ADD CONSTRAINT license_override_license_override_fk FOREIGN KEY (license_override_id) REFERENCES t_TENANT.license_override(license_override_id);


--
-- Name: license_threat_group_license license_threat_group_license_group_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.license_threat_group_license
    ADD CONSTRAINT license_threat_group_license_group_fk FOREIGN KEY (license_threat_group_id) REFERENCES t_TENANT.license_threat_group(license_threat_group_id);


--
-- Name: membership_mapping membership_mapping_role_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.membership_mapping
    ADD CONSTRAINT membership_mapping_role_fk FOREIGN KEY (role_id) REFERENCES t_TENANT.role(role_id);


--
-- Name: organization organization_parent_organization_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.organization
    ADD CONSTRAINT organization_parent_organization_fk FOREIGN KEY (parent_organization_id) REFERENCES t_TENANT.organization(organization_id);


--
-- Name: policy_evaluation policy_evaluation_app_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_evaluation
    ADD CONSTRAINT policy_evaluation_app_fk FOREIGN KEY (application_id) REFERENCES t_TENANT.application(application_id);


--
-- Name: policy_tag policy_tag_policy_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_tag
    ADD CONSTRAINT policy_tag_policy_fk FOREIGN KEY (policy_id) REFERENCES t_TENANT.policy(policy_id);


--
-- Name: policy_tag policy_tag_tag_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_tag
    ADD CONSTRAINT policy_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES t_TENANT.tag(tag_id);


--
-- Name: policy_violation policy_violation_app_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_violation
    ADD CONSTRAINT policy_violation_app_fk FOREIGN KEY (application_id) REFERENCES t_TENANT.application(application_id);


--
-- Name: policy_waiver policy_waiver_policy_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.policy_waiver
    ADD CONSTRAINT policy_waiver_policy_fk FOREIGN KEY (policy_id) REFERENCES t_TENANT.policy(policy_id);


--
-- Name: proprietary_component_name_pattern proprietary_component_name_pattern_repository_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.proprietary_component_name_pattern
    ADD CONSTRAINT proprietary_component_name_pattern_repository_fk FOREIGN KEY (repository_id) REFERENCES t_TENANT.repository(repository_id);


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_sched_name_trigger_name_trigger_group_fkey; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_sched_name_trigger_name_trigger_group_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES t_TENANT.qrtz_triggers(sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_sched_name_trigger_name_trigger_group_fkey; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_sched_name_trigger_name_trigger_group_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES t_TENANT.qrtz_triggers(sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_sched_name_trigger_name_trigger_group_fkey; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_sched_name_trigger_name_trigger_group_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES t_TENANT.qrtz_triggers(sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_sched_name_trigger_name_trigger_grou_fkey; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_sched_name_trigger_name_trigger_grou_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES t_TENANT.qrtz_triggers(sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_triggers qrtz_triggers_sched_name_job_name_job_group_fkey; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_sched_name_job_name_job_group_fkey FOREIGN KEY (sched_name, job_name, job_group) REFERENCES t_TENANT.qrtz_job_details(sched_name, job_name, job_group);


--
-- Name: quarantined_component_access quarantined_component_access_repository_component_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.quarantined_component_access
    ADD CONSTRAINT quarantined_component_access_repository_component_fk FOREIGN KEY (repository_component_id) REFERENCES t_TENANT.repository_component(repository_component_id);


--
-- Name: quarantined_component_access quarantined_component_access_repository_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.quarantined_component_access
    ADD CONSTRAINT quarantined_component_access_repository_fk FOREIGN KEY (repository_id) REFERENCES t_TENANT.repository(repository_id);


--
-- Name: repository_component repository_component_repository_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_component
    ADD CONSTRAINT repository_component_repository_fk FOREIGN KEY (repository_id) REFERENCES t_TENANT.repository(repository_id);


--
-- Name: repository_migration repository_migration_repository_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_migration
    ADD CONSTRAINT repository_migration_repository_fk FOREIGN KEY (repository_id) REFERENCES t_TENANT.repository(repository_id);


--
-- Name: repository_policy_violation repository_policy_violation_repository_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository_policy_violation
    ADD CONSTRAINT repository_policy_violation_repository_fk FOREIGN KEY (repository_id) REFERENCES t_TENANT.repository(repository_id);


--
-- Name: repository repository_repository_manager_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.repository
    ADD CONSTRAINT repository_repository_manager_fk FOREIGN KEY (repository_manager_id) REFERENCES t_TENANT.repository_manager(repository_manager_id);


--
-- Name: role_permission role_permission_role_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.role_permission
    ADD CONSTRAINT role_permission_role_fk FOREIGN KEY (role_id) REFERENCES t_TENANT.role(role_id);


--
-- Name: saml_user_group saml_user_group_group_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.saml_user_group
    ADD CONSTRAINT saml_user_group_group_fk FOREIGN KEY (saml_group_id) REFERENCES t_TENANT.saml_group(saml_group_id);


--
-- Name: saml_user_group saml_user_group_user_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.saml_user_group
    ADD CONSTRAINT saml_user_group_user_fk FOREIGN KEY (saml_user_id) REFERENCES t_TENANT.saml_user(saml_user_id);


--
-- Name: source_control_default_branch_commit_history source_control_default_branch_commit_history_application_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_default_branch_commit_history
    ADD CONSTRAINT source_control_default_branch_commit_history_application_fk FOREIGN KEY (application_id) REFERENCES t_TENANT.application(application_id);


--
-- Name: source_control_default_branch_commit_history source_control_default_branch_commit_history_policy_eval_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_default_branch_commit_history
    ADD CONSTRAINT source_control_default_branch_commit_history_policy_eval_fk FOREIGN KEY (policy_evaluation_id) REFERENCES t_TENANT.policy_evaluation(policy_evaluation_id);


--
-- Name: source_control_event source_control_event_application_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_event
    ADD CONSTRAINT source_control_event_application_fk FOREIGN KEY (application_id) REFERENCES t_TENANT.application(application_id);


--
-- Name: source_control_event source_control_event_policy_evaluation_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_event
    ADD CONSTRAINT source_control_event_policy_evaluation_fk FOREIGN KEY (policy_evaluation_id) REFERENCES t_TENANT.policy_evaluation(policy_evaluation_id);


--
-- Name: source_control_organization_import_event source_control_organization_import_event_organization_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_organization_import_event
    ADD CONSTRAINT source_control_organization_import_event_organization_fk FOREIGN KEY (organization_id) REFERENCES t_TENANT.organization(organization_id);


--
-- Name: source_control_pull_request_comment source_control_pull_request_comment_app_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_pull_request_comment
    ADD CONSTRAINT source_control_pull_request_comment_app_fk FOREIGN KEY (application_id) REFERENCES t_TENANT.application(application_id);


--
-- Name: source_control_pull_request_result source_control_pull_request_result_application_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_pull_request_result
    ADD CONSTRAINT source_control_pull_request_result_application_fk FOREIGN KEY (application_id) REFERENCES t_TENANT.application(application_id);


--
-- Name: source_control_pull_request_comment source_control_pull_request_source_policy_eval_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_pull_request_comment
    ADD CONSTRAINT source_control_pull_request_source_policy_eval_fk FOREIGN KEY (source_policy_evaluation_id) REFERENCES t_TENANT.policy_evaluation(policy_evaluation_id);


--
-- Name: source_control_pull_request_comment source_control_pull_request_target_policy_eval_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_control_pull_request_comment
    ADD CONSTRAINT source_control_pull_request_target_policy_eval_fk FOREIGN KEY (target_policy_evaluation_id) REFERENCES t_TENANT.policy_evaluation(policy_evaluation_id);


--
-- Name: source_link_override source_link_override_component_source_link_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.source_link_override
    ADD CONSTRAINT source_link_override_component_source_link_fk FOREIGN KEY (component_source_link_id) REFERENCES t_TENANT.component_source_link(component_source_link_id);


--
-- Name: success_metrics_report_data success_metrics_report_data_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.success_metrics_report_data
    ADD CONSTRAINT success_metrics_report_data_fk FOREIGN KEY (success_metrics_report_data_id) REFERENCES t_TENANT.success_metrics_report(success_metrics_report_id);


--
-- Name: tag tag_organization_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.tag
    ADD CONSTRAINT tag_organization_fk FOREIGN KEY (organization_id) REFERENCES t_TENANT.organization(organization_id);


--
-- Name: third_party_scan third_party_scan_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.third_party_scan
    ADD CONSTRAINT third_party_scan_fk FOREIGN KEY (third_party_file_id) REFERENCES t_TENANT.third_party_file(third_party_file_id);


--
-- Name: vulnerability_custom_cvss_severity_tag vulnerability_custom_cvss_severity_tag_cvss_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_severity_tag
    ADD CONSTRAINT vulnerability_custom_cvss_severity_tag_cvss_fk FOREIGN KEY (vulnerability_custom_cvss_severity_id) REFERENCES t_TENANT.vulnerability_custom_cvss_severity(vulnerability_custom_cvss_severity_id);


--
-- Name: vulnerability_custom_cvss_severity_tag vulnerability_custom_cvss_severity_tag_tag_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_severity_tag
    ADD CONSTRAINT vulnerability_custom_cvss_severity_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES t_TENANT.tag(tag_id);


--
-- Name: vulnerability_custom_cvss_vector_tag vulnerability_custom_cvss_vector_tag_cvss_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_vector_tag
    ADD CONSTRAINT vulnerability_custom_cvss_vector_tag_cvss_fk FOREIGN KEY (vulnerability_custom_cvss_vector_id) REFERENCES t_TENANT.vulnerability_custom_cvss_vector(vulnerability_custom_cvss_vector_id);


--
-- Name: vulnerability_custom_cvss_vector_tag vulnerability_custom_cvss_vector_tag_tag_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cvss_vector_tag
    ADD CONSTRAINT vulnerability_custom_cvss_vector_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES t_TENANT.tag(tag_id);


--
-- Name: vulnerability_custom_cwe_tag vulnerability_custom_cwe_tag_cwe_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cwe_tag
    ADD CONSTRAINT vulnerability_custom_cwe_tag_cwe_fk FOREIGN KEY (vulnerability_custom_cwe_id) REFERENCES t_TENANT.vulnerability_custom_cwe(vulnerability_custom_cwe_id);


--
-- Name: vulnerability_custom_cwe_tag vulnerability_custom_cwe_tag_tag_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_cwe_tag
    ADD CONSTRAINT vulnerability_custom_cwe_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES t_TENANT.tag(tag_id);


--
-- Name: vulnerability_custom_remediation_tag vulnerability_custom_remediation_tag_remediation_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_remediation_tag
    ADD CONSTRAINT vulnerability_custom_remediation_tag_remediation_fk FOREIGN KEY (vulnerability_custom_remediation_id) REFERENCES t_TENANT.vulnerability_custom_remediation(vulnerability_custom_remediation_id);


--
-- Name: vulnerability_custom_remediation_tag vulnerability_custom_remediation_tag_tag_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_custom_remediation_tag
    ADD CONSTRAINT vulnerability_custom_remediation_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES t_TENANT.tag(tag_id);


--
-- Name: vulnerability_group_vulnerability vulnerability_group_vulnerability_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.vulnerability_group_vulnerability
    ADD CONSTRAINT vulnerability_group_vulnerability_fk FOREIGN KEY (vulnerability_group_id) REFERENCES t_TENANT.vulnerability_group(vulnerability_group_id);


--
-- Name: webhook_event_type webhook_event_type_fk; Type: FK CONSTRAINT; Schema: t_TENANT; Owner: testuser
--

ALTER TABLE ONLY t_TENANT.webhook_event_type
    ADD CONSTRAINT webhook_event_type_fk FOREIGN KEY (webhook_id) REFERENCES t_TENANT.webhook(webhook_id);


--
-- PostgreSQL database dump complete
--

--
-- Initial Data Store versions for the canonical schema
--

INSERT INTO t_TENANT.schema_version (data_store_id, schema_version) VALUES ('insight_brain_dm', 12);
INSERT INTO t_TENANT.schema_version (data_store_id, schema_version) VALUES ('insight_brain_third_party_scans', 13);
INSERT INTO t_TENANT.schema_version (data_store_id, schema_version) VALUES ('insight_brain_aggregation', 13);
INSERT INTO t_TENANT.schema_version (data_store_id, schema_version) VALUES ('insight_brain_ods', 303);

INSERT INTO t_TENANT.role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('1b92fae3e55a411793a091fb821c422d', 'System Administrator', 'systemadministrator', 100, 'Manages system configuration and users.', TRUE, TRUE);
INSERT INTO t_TENANT.role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('b9646757e98e486da7d730025f5245f8', 'Policy Administrator', 'policyadministrator', 150, 'Manages all organizations, applications, policies, and policy violations.', TRUE, TRUE);
INSERT INTO t_TENANT.role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('1cddabf7fdaa47d6833454af10e0a3ef', 'Owner', 'owner', 200, 'Manages assigned organizations, applications, policies, and policy violations.', FALSE, TRUE);
INSERT INTO t_TENANT.role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('1da70fae1fd54d6cb7999871ebdb9a36', 'Developer', 'developer', 300, 'Views all information for their assigned organization or application.', FALSE, TRUE);
INSERT INTO t_TENANT.role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('2cb71b3468d649789163ea2e212b541e', 'Application Evaluator', 'applicationevaluator', 400, 'Evaluates applications and views policy violation summary results.', FALSE, TRUE);
INSERT INTO t_TENANT.role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('90c7c98683b4471cb77a916744540bcc', 'Component Evaluator', 'componentevaluator', 500, 'Evaluates individual components and views policy violation results for a specified application.', FALSE, TRUE);
INSERT INTO t_TENANT.role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('0df46317c031440795007f4ce9c7f002', 'Legal Reviewer', 'legalreviewer', 600, 'Reviews legal obligations for component licenses.', FALSE, TRUE);
