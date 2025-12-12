/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.license.model.Feature;

/**
 * This enumeration contains features that can be enabled/disabled by the ApiConfigFeaturesResource. <br/><br/> Each
 * enum value has these properties:
 * <ul>
 *   <li>
 *     {@code name} - this is returned to the frontend via FeaturesResource#getFeatures() after being
 *     transformed according to {@link Feature#getId()}.
 *   </li>
 *   <li>
 *     {@code propertyName} - this is the name stored in the {@link SystemConfigurationProperty} table.
 *   </li>
 *   <li>
 *     {@code propertyValue} - this represents the value that will be stored in the
 *     {@link SystemConfigurationProperty} table. Note that the value has no impact on whether or not the feature is
 *     enabled/disabled (only the presence/absence of the row itself). However the value can be set to help
 *     understanding. This defaults to the String value of the opposite of {@code enabledWhenAbsent}.
 *   </li>
 *   <li>
 *     {@code enabledWhenAbsent} - if this is {@code true}, then the feature will be enabled even if its
 *     {@code propertyName} is absent from the {@link SystemConfigurationProperty} table.
 *   </li>
 * </ul>
 * Note that if you want the feature name passed to ApiConfigFeaturesResource to be different to the
 * {@code name}, result of {@link Feature#getId()}, and {@code propertyName}, then you need to add a mapping to the
 * ApiConfigFeaturesService#getPropertyNameForFeature method.
 * <br/><br/>
 * Typically, a feature would start with {@code enabledWhenAbsent} set to {@code false}, making it experimental.
 * When it's production-ready {@code enabledWhenAbsent} can be changed to {@code true} alongside an incremental script
 * to delete the feature from the {@link SystemConfigurationProperty} table.
 */
public enum SystemConfigurationPropertyFeature
    implements Feature
{
  DASHBOARD_CAN_BE_ENABLED(SystemConfigurationProperty.DASHBOARD_DISABLED, "true", true),
  REPORTS_LIST_CAN_BE_ENABLED(SystemConfigurationProperty.REPORTS_LIST_DISABLED, "true", true),
  VULNERABILITY_SOURCE(SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED, "true",
      true),
  BUILT_FROM_SOURCE(SystemConfigurationProperty.BUILT_FROM_SOURCE, false),
  CROWD_INTEGRATION(SystemConfigurationProperty.CROWD_INTEGRATION, true),
  WEBHOOK_CONFIGURATION(SystemConfigurationProperty.WEBHOOK_CONFIGURATION, true),
  PRODUCT_LICENSE_CONFIGURATION(SystemConfigurationProperty.PRODUCT_LICENSE_CONFIGURATION, true),
  LDAP_CONFIGURATION(SystemConfigurationProperty.LDAP_CONFIGURATION, true),
  EMAIL_CONFIGURATION(SystemConfigurationProperty.EMAIL_CONFIGURATION, true),
  PROXY_CONFIGURATION(SystemConfigurationProperty.PROXY_CONFIGURATION, true),
  SYSTEM_NOTICE_CONFIGURATION(SystemConfigurationProperty.SYSTEM_NOTICE_CONFIGURATION, true),
  SUCCESS_METRICS_CONFIGURATION(SystemConfigurationProperty.SUCCESS_METRICS_CONFIGURATION, true),
  AUTOMATIC_APPLICATION_CONFIGURATION(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CONFIGURATION, true),
  AUTOMATIC_SCM_CONFIGURATION(SystemConfigurationProperty.AUTOMATIC_SCM_CONFIGURATION, true),
  ADVANCED_SEARCH_CONFIGURATION(SystemConfigurationProperty.ADVANCED_SEARCH_CONFIGURATION, true),
  EXPIRE_WAIVER_WHEN_REMEDIATION_AVAILABLE(SystemConfigurationProperty.EXPIRE_WAIVER_WHEN_REMEDIATION_AVAILABLE, false)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      String valueInEnvVar = System.getenv().get(NXIQ_ADVANCED_SEARCH_CONFIGURATION_ENV_VAR);
      return valueInEnvVar == null ? super.isEnabled(tx) : Boolean.parseBoolean(valueInEnvVar);
    }

    @Override
    public void setEnabled(TransactionContext tx, boolean enabled) {
      String valueInEnvVar = System.getenv().get(NXIQ_ADVANCED_SEARCH_CONFIGURATION_ENV_VAR);
      if (valueInEnvVar == null) {
        super.setEnabled(tx, enabled);
      }
    }
  },
  // Special case to let us set ADVANCED_SEARCH_ENABLED to true/false via the configuration API
  // this is an older feature flag and has no concept of being enabled/disabled when absent
  ADVANCED_SEARCH_ENABLED(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, /*ignored*/false)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      return Boolean.parseBoolean(systemConfigurationPropertyDAO.getByName(tx, getPropertyName()).getValue());
    }

    @Override
    public void setEnabled(TransactionContext tx, boolean enabled) {
      systemConfigurationPropertyDAO.set(tx, getPropertyName(), Boolean.toString(enabled));
    }
  },
  TRANSITIVE_SOLVER(SystemConfigurationProperty.TRANSITIVE_SOLVER_ENABLED, true),
  CODE_INSIGHTS(SystemConfigurationProperty.CODE_INSIGHTS, true),
  COMPONENT_SEARCH_API_WITH_INNERSOURCE(SystemConfigurationProperty.COMPONENT_SEARCH_API_WITH_INNERSOURCE, true),
  DEFAULT_BRANCH_MONITORING(SystemConfigurationProperty.DEFAULT_BRANCH_MONITORING, true),
  DEPENDENCY_DATA_IN_API(SystemConfigurationProperty.DEPENDENCY_DATA_IN_API, true),
  INNER_SOURCE_TRANSITIVE_WAIVER(SystemConfigurationProperty.INNER_SOURCE_TRANSITIVE_WAIVER, true),
  INNER_SOURCE_REPOSITORY_INTEGRATION(SystemConfigurationProperty.INNER_SOURCE_REPOSITORY_INTEGRATION, true),
  PR_COMMENTING(SystemConfigurationProperty.PR_COMMENTING, true),
  PR_LINE_COMMENTING(SystemConfigurationProperty.PR_LINE_COMMENTING, true),
  // CLM-35694: When enabled, Bitbucket PR comments are updated even when policy evaluations haven't changed.
  PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE(SystemConfigurationProperty.PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE,
      false),
  ENABLE_UNAUTHENTICATED_PAGES(SystemConfigurationProperty.ENABLE_UNAUTHENTICATED_PAGES, true)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      String valueInEnvVar = System.getenv().get(NXIQ_ENABLE_UNAUTHENTICATED_PAGES_ENV_VAR);
      return valueInEnvVar == null ? super.isEnabled(tx) : Boolean.parseBoolean(valueInEnvVar);
    }

    @Override
    public void setEnabled(TransactionContext tx, boolean enabled) {
      String valueInEnvVar = System.getenv().get(NXIQ_ENABLE_UNAUTHENTICATED_PAGES_ENV_VAR);
      if (valueInEnvVar == null) {
        super.setEnabled(tx, enabled);
      }
    }
  },
  ENABLE_SSO_ONLY(SystemConfigurationProperty.ENABLE_SSO_ONLY, false)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      String valueInEnvVar = System.getenv().get(NXIQ_ENABLE_SSO_ONLY_ENV_VAR);

      if (valueInEnvVar == null) {
        final SystemConfigurationProperty systemConfigurationProperty =
            systemConfigurationPropertyDAO.getByName(tx, getPropertyName());
        // Enabled in MTIQ non-FIPS mode, disabled otherwise
        return systemConfigurationProperty == null ?
            !tenantUtil.isSingleTenant() && !FIPSModeDetector.isEnabled() :
            Boolean.parseBoolean(systemConfigurationProperty.getValue());
      }
      else {
        return Boolean.parseBoolean(valueInEnvVar);
      }
    }

    @Override
    public void setEnabled(TransactionContext tx, boolean enabled) {
      String valueInEnvVar = System.getenv().get(NXIQ_ENABLE_SSO_ONLY_ENV_VAR);
      if (valueInEnvVar == null) {
        super.setEnabled(tx, enabled);
      }
    }
  },
  API_PAGE(SystemConfigurationProperty.API_PAGE, true),
  SCAN_POM_FILES_IN_META_INF_DIRECTORY(SystemConfigurationProperty.SCAN_POM_FILES_IN_META_INF_DIRECTORY, false),
  SCAN_NPM_DEV_AND_OPT_DEPENDENCIES(SystemConfigurationProperty.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES, false),

  /**
   * @deprecated Use {@link SourceControl#getSourceControlEvaluationsEnabled() instead}
   */
  @Deprecated
  INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS(
      SystemConfigurationProperty.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS, true),

  /**
   * Internal feature flag to enable Firewall Onboarding. Disabled by default in IQ < 167.
   * It was enabled by default in IQ >= 167 through IQ <= 180. It is again disabled by default in IQ 182.
   */
  INTERNAL_FIREWALL_ONBOARDING_ENABLED(SystemConfigurationProperty.INTERNAL_FIREWALL_ONBOARDING_ENABLED,
      false/* enabledWhenAbsent */),

  /**
   * If configured a logout request will be sent to Auth0 via a browser redirect when the application is logged out
   */
  LOGOUT_AUTH0_ON_LOGOUT(SystemConfigurationProperty.LOGOUT_AUTH0_ON_LOGOUT, false)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      final SystemConfigurationProperty systemConfigurationProperty =
          systemConfigurationPropertyDAO.getByName(tx, getPropertyName());
      // Enabled in MTIQ non-FIPS mode, disabled otherwise
      return systemConfigurationProperty == null ?
          !tenantUtil.isSingleTenant() && !FIPSModeDetector.isEnabled() :
          Boolean.parseBoolean(systemConfigurationProperty.getValue());
    }
  },

  /**
   * If configured the UI will show the Sonatype managed IDP Auth0 user management pages
   */
  SSO_IDP_MANAGED_BY_SONATYPE(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE, false),
  SCM_UX_IMPROVEMENTS(SystemConfigurationProperty.SCM_UX_IMPROVEMENTS, false),

  /**
   * Self-Hosted: SCM is not feature flagged for self-hosted, so it must always return true for self-hosted. SaaS:
   * SAAS_LIFECYCLE_SCM_ENABLED is enabled by default.
   */
  SAAS_LIFECYCLE_SCM_ENABLED(SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_ENABLED, true)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      if (tenantUtil.isSingleTenant()) {
        return true;
      }
      return super.isEnabled(tx);
    }
  },

  /**
   * Self-Hosted: PR creation is always enabled (at the feature flag level) for self-hosted.
   * SaaS: PR creation can be controlled via feature flag or environment variable.
   */
  SAAS_LIFECYCLE_SCM_PRS_ENABLED(SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_PRS_ENABLED, false)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      if (tenantUtil.isSingleTenant()) {
        return true;
      }
      String valueInEnvVar = System.getenv().get(NXIQ_SAAS_LIFECYCLE_SCM_PRS_ENABLED_ENV_VAR);
      return valueInEnvVar == null ? super.isEnabled(tx) : Boolean.parseBoolean(valueInEnvVar);
    }

    @Override
    public void setEnabled(TransactionContext tx, boolean enabled) {
      String valueInEnvVar = System.getenv().get(NXIQ_SAAS_LIFECYCLE_SCM_PRS_ENABLED_ENV_VAR);
      if (valueInEnvVar == null) {
        super.setEnabled(tx, enabled);
      }
    }
  },

  SBOM_MANAGER(SystemConfigurationProperty.SBOM_MANAGER, false),
  DEVELOPMENT_DASHBOARD_METRIC_COLLECTION(
      SystemConfigurationProperty.DEVELOPMENT_DASHBOARD_METRIC_COLLECTION,
      true
  ),
  PRIORITIZED_FINDINGS_REPORT(SystemConfigurationProperty.PRIORITIZED_FINDINGS_REPORT, true),

  /**
   * If enabled IQ will be able to use OAuth2 to implement authentication and will be able to handle JWT bearer tokens
   * sent on the Authorization HTTP header
   */
  OAUTH2_ENABLED(SystemConfigurationProperty.OAUTH2_ENABLED, false),

  SKIP_SBOM_IMPORT_VALIDATION(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION, false),

  DEVELOPER_BULK_RECOMMENDATIONS(SystemConfigurationProperty.DEVELOPER_BULK_RECOMMENDATIONS, false),

  DEVELOPER_SUMMARY_TABLE(SystemConfigurationProperty.DEVELOPER_SUMMARY_TABLE, true),

  CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT(SystemConfigurationProperty.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT,
      true),

  SBOM_BINARY_SCANNING(SystemConfigurationProperty.SBOM_BINARY_SCANNING, true),

  ALP_FOR_SBOM_MANAGER(SystemConfigurationProperty.ALP_FOR_SBOM_MANAGER, false),

  DEVELOPER_SUGGEST_NON_BREAKING_VERSION(SystemConfigurationProperty.DEVELOPER_SUGGEST_NON_BREAKING_VERSION, true)
  {
    // A feature flag with enabledWhenAbsent = true and an entry in the db with a value of true is not
    // treated as enabled.
    @Override
    public boolean isEnabled(TransactionContext tx) {
      final SystemConfigurationProperty systemConfigurationProperty =
          systemConfigurationPropertyDAO.getByName(tx, getPropertyName());
      return systemConfigurationProperty == null ? super.isEnabled(tx) :
          Boolean.parseBoolean(systemConfigurationProperty.getValue());
    }
  },

  NON_BREAKING_VERSION_SUGGESTION_TELEMETRY(
      SystemConfigurationProperty.NON_BREAKING_VERSION_SUGGESTION_TELEMETRY, true),

  SBOM_CONTINUOUS_MONITORING_UI(SystemConfigurationProperty.SBOM_CONTINUOUS_MONITORING_UI, true),

  SBOM_POLICIES(SystemConfigurationProperty.SBOM_POLICIES, false),

  AUTO_WAIVERS(SystemConfigurationProperty.AUTO_WAIVERS, true)
  {
    // A feature flag with enabledWhenAbsent = true and an entry in the db with a value of true is not
    // treated as enabled.
    @Override
    public boolean isEnabled(TransactionContext tx) {
      final SystemConfigurationProperty systemConfigurationProperty =
          systemConfigurationPropertyDAO.getByName(tx, getPropertyName());
      return systemConfigurationProperty == null ? super.isEnabled(tx) :
          Boolean.parseBoolean(systemConfigurationProperty.getValue());
    }
  },

  WAIVER_REQUEST_WORKFLOW_ENABLED(SystemConfigurationProperty.WAIVER_REQUEST_WORKFLOW_ENABLED, true),

  COMPONENT_CHANGE_DETECTION_API(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_API, false)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      if (tenantUtil.isSingleTenant()) {
        return false;
      }
      return super.isEnabled(tx);
    }
  },

  CONTAINER_IMAGES_EVAL_ENABLED(SystemConfigurationProperty.CONTAINER_IMAGES_EVAL_ENABLED, true)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      final SystemConfigurationProperty systemConfigurationProperty =
          systemConfigurationPropertyDAO.getByName(tx, getPropertyName());
      return systemConfigurationProperty == null
          ? super.isEnabled(tx)
          : Boolean.parseBoolean(systemConfigurationProperty.getValue());
    }
  },

  ZSCALER(SystemConfigurationProperty.ZSCALER, true)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      final SystemConfigurationProperty systemConfigurationProperty =
          systemConfigurationPropertyDAO.getByName(tx, getPropertyName());
      return systemConfigurationProperty == null
          ? super.isEnabled(tx)
          : Boolean.parseBoolean(systemConfigurationProperty.getValue());
    }
  },

  THIRD_PARTY_KEV_LOOKUP(SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP, true)
  {
    // A feature flag with enabledWhenAbsent = true and an entry in the db with a value of true is not
    // treated as enabled.
    @Override
    public boolean isEnabled(TransactionContext tx) {
      final SystemConfigurationProperty systemConfigurationProperty =
          systemConfigurationPropertyDAO.getByName(tx, getPropertyName());
      return systemConfigurationProperty == null ? super.isEnabled(tx) :
          Boolean.parseBoolean(systemConfigurationProperty.getValue());
    }
  },

  SAML_ENABLED(SystemConfigurationProperty.SAML_ENABLED, true)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      final SystemConfigurationProperty systemConfigurationProperty =
          systemConfigurationPropertyDAO.getByName(tx, getPropertyName());

      // CLM-35986 - default this based on the environment we are in.
      // 1) On prem IQ - Default to be enabled.
      // 2) MTIQ with FIPS enabled - Default to be enabled.
      // 3) MITQ with FIPS disabled - Default to be disabled.
      return systemConfigurationProperty == null ?
          tenantUtil.isSingleTenant() || FIPSModeDetector.isEnabled() :
          Boolean.parseBoolean(systemConfigurationProperty.getValue());
    }
  },

  USER_MANAGEMENT_PAGES(SystemConfigurationProperty.USER_MANAGEMENT_PAGES, true)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      final SystemConfigurationProperty systemConfigurationProperty =
          systemConfigurationPropertyDAO.getByName(tx, getPropertyName());
      // Enabled in single tenant OR FIPS mode, disabled otherwise
      return systemConfigurationProperty == null ?
        tenantUtil.isSingleTenant() || FIPSModeDetector.isEnabled() :
        Boolean.parseBoolean(systemConfigurationProperty.getValue());
    }
  },

  EPSS_DATA(SystemConfigurationProperty.EPSS_DATA, false),

  ENABLE_FEDRAMP_AUDIT(SystemConfigurationProperty.ENABLE_FEDRAMP_AUDIT, false)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      String valueInEnvVar = System.getenv().get(NXIQ_ENABLE_FEDRAMP_AUDIT_ENV_VAR);
      return valueInEnvVar == null ? super.isEnabled(tx) : Boolean.parseBoolean(valueInEnvVar);
    }

    @Override
    public void setEnabled(TransactionContext tx, boolean enabled) {
      String valueInEnvVar = System.getenv().get(NXIQ_ENABLE_FEDRAMP_AUDIT_ENV_VAR);
      if (valueInEnvVar == null) {
        super.setEnabled(tx, enabled);
      }
    }
  },

  USER_ACTIVITY_TRACKING(SystemConfigurationProperty.USER_ACTIVITY_TRACKING, false),

  EXIT_ON_FATAL_ERROR(SystemConfigurationProperty.EXIT_ON_FATAL_ERROR, true)
  {
    @Override
    public boolean isEnabled(TransactionContext tx) {
      final SystemConfigurationProperty systemConfigurationProperty =
          systemConfigurationPropertyDAO.getByName(tx, getPropertyName());
      return systemConfigurationProperty == null ? super.isEnabled(tx) :
          Boolean.parseBoolean(systemConfigurationProperty.getValue());
    }

    @Override
    public void setEnabled(TransactionContext tx, boolean enabled) {
      systemConfigurationPropertyDAO.set(tx, getPropertyName(), Boolean.toString(enabled));
    }
  };

  public static final String NXIQ_ENABLE_UNAUTHENTICATED_PAGES_ENV_VAR = "NXIQ_ENABLE_UNAUTHENTICATED_PAGES";

  public static final String NXIQ_ENABLE_SSO_ONLY_ENV_VAR = "NXIQ_ENABLE_SSO_ONLY";

  public static final String NXIQ_ADVANCED_SEARCH_CONFIGURATION_ENV_VAR = "NXIQ_ADVANCED_SEARCH_CONFIGURATION";

  public static final String NXIQ_ENABLE_FEDRAMP_AUDIT_ENV_VAR = "NXIQ_ENABLE_FEDRAMP_AUDIT";

  public static final String NXIQ_SAAS_LIFECYCLE_SCM_PRS_ENABLED_ENV_VAR = "NXIQ_SAAS_LIFECYCLE_SCM_PRS_ENABLED";

  private static SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private static final TenantUtil tenantUtil = new TenantUtil();

  @Inject
  public static void injectDependencies(SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
    SystemConfigurationPropertyFeature.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  private final String propertyName;

  private final String propertyValue;

  private final boolean enabledWhenAbsent;

  SystemConfigurationPropertyFeature(final String propertyName, final boolean enabledWhenAbsent) {
    this(propertyName, String.valueOf(!enabledWhenAbsent), enabledWhenAbsent);
  }

  SystemConfigurationPropertyFeature(
      final String propertyName,
      final String propertyValue,
      final boolean enabledWhenAbsent)
  {
    this.propertyName = propertyName;
    this.propertyValue = propertyValue;
    this.enabledWhenAbsent = enabledWhenAbsent;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public String getPropertyValue() {
    return propertyValue;
  }

  public final boolean isEnabled() {
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      return isEnabled(tx);
    }
  }

  public boolean isEnabled(TransactionContext tx) {
    SystemConfigurationProperty systemConfigurationProperty =
        systemConfigurationPropertyDAO.getByName(tx, propertyName);
    return (systemConfigurationProperty == null) == enabledWhenAbsent;
  }

  public void verifyEnabled() {
    if (!isEnabled()) {
      throw new NotAuthorizedException(getId() + " feature is disabled.");
    }
  }

  public final boolean isStored() {
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      return isStored(tx);
    }
  }

  public boolean isStored(TransactionContext tx) {
    return systemConfigurationPropertyDAO.getByName(tx, propertyName) != null;
  }

  public final void setEnabled(boolean enabled) {
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      tx.begin();
      setEnabled(tx, enabled);
      tx.commit();
    }
  }

  public void setEnabled(TransactionContext tx, boolean enabled) {
    if (isEnabled() == enabled) {
      return;
    }
    if (enabled == enabledWhenAbsent) {
      systemConfigurationPropertyDAO.set(tx, propertyName, null);
    }
    else {
      systemConfigurationPropertyDAO.set(tx, propertyName, propertyValue);
    }
  }
}
