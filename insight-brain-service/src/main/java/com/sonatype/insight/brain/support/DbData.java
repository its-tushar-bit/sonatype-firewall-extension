/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemNoticeDAO;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordService;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.hds.TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME;
import static com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderUtils.maskCredentialsFromUrl;

/**
 * @since 1.35
 */
@Named
@Singleton
class DbData
{
  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryDAO repositoryDAO;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final ProprietaryConfigDAO proprietaryConfigDAO;

  private final UserDAO userDAO;

  private final PasswordService passwordService;

  private final RoleDAO roleDAO;

  private final RolePermissionDAO rolePermissionDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final WebhookDAO webhookDAO;

  private final SystemNoticeDAO systemNoticeDAO;

  private final LabelDAO labelDAO;

  private final ComponentLabelDAO componentLabelDAO;

  private final TagDAO tagDAO;

  private final ApplicationTagDAO applicationTagDAO;

  private final PolicyTagDAO policyTagDAO;

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final MultiLicenseDAO multiLicenseDAO;

  private final LicenseDAO licenseDAO;

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private final PolicyDAO policyDAO;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final SourceControlDAO sourceControlDAO;

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final DataRetentionPolicyDAO dataRetentionPolicyDAO;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO;

  private final RepositoryConnectionDAO repositoryConnectionDAO;

  private final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO;

  @Inject
  DbData(final RepositoryManagerDAO repositoryManagerDAO,
         final RepositoryDAO repositoryDAO,
         final OrganizationDAO organizationDAO,
         final ApplicationDAO applicationDAO,
         final ProprietaryConfigDAO proprietaryConfigDAO,
         final UserDAO userDAO,
         final PasswordService passwordService,
         final RoleDAO roleDAO,
         final RolePermissionDAO rolePermissionDAO,
         final MembershipMappingDAO membershipMappingDAO,
         final WebhookDAO webhookDAO,
         final SystemNoticeDAO systemNoticeDAO,
         final LabelDAO labelDAO,
         final ComponentLabelDAO componentLabelDAO,
         final TagDAO tagDAO,
         final ApplicationTagDAO applicationTagDAO,
         final PolicyTagDAO policyTagDAO,
         final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
         final LicenseThreatGroupDAO licenseThreatGroupDAO,
         final MultiLicenseDAO multiLicenseDAO,
         final LicenseDAO licenseDAO,
         final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO,
         final PolicyDAO policyDAO,
         final PolicyMonitoringDAO policyMonitoringDAO,
         final DataRetentionPolicyDAO dataRetentionPolicyDAO,
         final MigrationTrackerDAO migrationTrackerDAO,
         final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
         final SourceControlDAO sourceControlDAO,
         final RepositoryComponentDAO repositoryComponentDAO,
         final PolicyWaiverDAO policyWaiverDAO,
         final ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO,
         final RepositoryConnectionDAO repositoryConnectionDAO,
         final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO)
  {
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.proprietaryConfigDAO = proprietaryConfigDAO;
    this.userDAO = userDAO;
    this.passwordService = passwordService;
    this.roleDAO = roleDAO;
    this.rolePermissionDAO = rolePermissionDAO;
    this.membershipMappingDAO = membershipMappingDAO;
    this.webhookDAO = webhookDAO;
    this.systemNoticeDAO = systemNoticeDAO;
    this.labelDAO = labelDAO;
    this.componentLabelDAO = componentLabelDAO;
    this.tagDAO = tagDAO;
    this.applicationTagDAO = applicationTagDAO;
    this.policyTagDAO = policyTagDAO;
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.licenseDAO = licenseDAO;
    this.licenseThreatGroupLicenseDAO = licenseThreatGroupLicenseDAO;
    this.policyDAO = policyDAO;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.dataRetentionPolicyDAO = dataRetentionPolicyDAO;
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.sourceControlDAO = sourceControlDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.reverseProxyAuthenticationConfigurationDAO = reverseProxyAuthenticationConfigurationDAO;
    this.repositoryConnectionDAO = repositoryConnectionDAO;
    this.cpeMatchingConfigurationDAO = cpeMatchingConfigurationDAO;
  }

  Entry<String, Object> getRepositoryManager() {
    return wrapEntry("repositoryManager", repositoryManagerDAO.getAll());
  }

  Entry<String, Object> getRepository() {
    return wrapEntry("repository", repositoryDAO.getAll());
  }

  Entry<String, Object> getOrganization() {
    return wrapEntry("organization", organizationDAO.getAll());
  }

  Entry<String, Object> getApplication() {
    return wrapEntry("application", applicationDAO.getAll());
  }

  Entry<String, Object> getProprietaryConfig() {
    return wrapEntry("proprietaryConfig", proprietaryConfigDAO.getAll());
  }

  Entry<String, Object> getUser() {
    final List<User> users = userDAO.getAll();
    final String hashedAdminPwd = passwordService.hashPassword("admin123");
    for (final User user : users) {
      // reset all passwords
      user.setPassword(hashedAdminPwd);
    }
    return wrapEntry("user", users);
  }

  Entry<String, Object> getRole() {
    return wrapEntry("role", roleDAO.getAll());
  }

  Entry<String, Object> getRolePermission() {
    return wrapEntry("rolePermission", rolePermissionDAO.getAll());
  }

  Entry<String, Object> getMembershipMapping() {
    return wrapEntry("membershipMapping", membershipMappingDAO.getAll());
  }

  Entry<String, Object> getWebhook() {
    final List<Webhook> webhooks = webhookDAO.getAll();
    for (final Webhook webhook : webhooks) {
      // mask any secretKeys
      if (!StringUtils.isBlank(webhook.getSecretKey())) {
        webhook.setSecretKey(SystemInfo.MASK);
      }
    }
    return wrapEntry("webhook", webhooks);
  }

  Entry<String, Object> getSystemNotice() {
    return wrapEntry("systemNotice", systemNoticeDAO.get());
  }

  Entry<String, Object> getLabel() {
    return wrapEntry("label", labelDAO.getAll());
  }

  Entry<String, Object> getComponentLabel() {
    return wrapEntry("componentLabel", componentLabelDAO.getAll());
  }

  Entry<String, Object> getTag() {
    return wrapEntry("tag", tagDAO.getAll());
  }

  Entry<String, Object> getApplicationTag() {
    return wrapEntry("applicationTag", applicationTagDAO.getAll());
  }

  Entry<String, Object> getPolicyTag() {
    return wrapEntry("policyTag", policyTagDAO.getAll());
  }

  Entry<String, Object> getSecurityVulnerabilityOverride() {
    return wrapEntry("securityVulnerabilityOverride", securityVulnerabilityOverrideDAO.getAll());
  }

  Entry<String, Object> getLicenseThreatGroup() {
    return wrapEntry("licenseThreatGroup", licenseThreatGroupDAO.getAll());
  }

  Entry<String, Object> getMultiLicense() {
    return wrapEntry("multiLicense", multiLicenseDAO.getAll());
  }

  Entry<String, Object> getLicense() {
    return wrapEntry("license", licenseDAO.getAll());
  }

  Entry<String, Object> getLicenseThreatGroupLicense() {
    return wrapEntry("licenseThreatGroupLicense", licenseThreatGroupLicenseDAO.getAll());
  }

  Entry<String, Object> getPolicy() {
    return wrapEntry("policy", policyDAO.getAll());
  }

  Entry<String, Object> getQuarantinedComponent() {
    return wrapEntry("quarantinedComponent", repositoryComponentDAO.getAllQuarantinedComponent());
  }

  Entry<String, Object> getWaiver() {
    return wrapEntry("waiver", policyWaiverDAO.getAll());
  }

  Entry<String, Object> getPolicyMonitoring() {
    return wrapEntry("policyMonitoring", policyMonitoringDAO.getAll());
  }

  Entry<String, Object> getSourceControl() {
    List<SourceControl> sourceControls = sourceControlDAO.getAll();
    sourceControls.forEach(sourceControl -> {
      maskValueIfPresent(sourceControl.getToken(), sourceControl::setToken);
      if (!StringUtils.isBlank(sourceControl.getRepositoryUrl())) {
        sourceControl.setRepositoryUrl(maskCredentialsFromUrl(sourceControl.getRepositoryUrl()));
      }
    });
    return wrapEntry("sourceControl", sourceControls);
  }

  Entry<String, Object> getDataRetentionPolicy() {
    return wrapEntry("dataRetentionPolicy", dataRetentionPolicyDAO.getAll());
  }

  Entry<String, Object> getMigrationTracker() {
    return wrapEntry("migrationTracker", migrationTrackerDAO.getAll());
  }

  Entry<String, Object> getSystemConfiguration() {
    List<SystemConfigurationProperty> systemConfigurationPropertyList = systemConfigurationPropertyDAO.getAll();
    List<SystemConfigurationProperty> scpList = new ArrayList<>();

    HashSet<String> needsMasking = new HashSet<>();
    needsMasking.add(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);

    // Obfuscation (CLM-12603)
    for (SystemConfigurationProperty scp : systemConfigurationPropertyList) {
      if (!needsMasking.contains(scp.getName()) || StringUtils.isBlank(scp.getValue())) {
        scpList.add(scp);
        continue;
      }
      SystemConfigurationProperty telemetryInstanceId = new SystemConfigurationProperty(scp.getName(), SystemInfo.MASK);
      telemetryInstanceId.setId(scp.getId());
      scpList.add(telemetryInstanceId);
    }

    return wrapEntry("systemConfiguration", scpList);
  }

  Entry<String, Object> getReverseProxyAuthenticationConfiguration() {
    return wrapEntry("reverseProxyAuthenticationConfiguration", reverseProxyAuthenticationConfigurationDAO.get());
  }

  Entry<String, Object> getInnerSourceRepositoriesConfiguration() {
    final List<RepositoryConnection> repositoryConfigs = repositoryConnectionDAO.getAll();
    for (RepositoryConnection repositoryConfig : repositoryConfigs) {
      repositoryConfig.setUsername(SystemInfo.MASK);
      repositoryConfig.setPassword(SystemInfo.MASK.toCharArray());
    }

    return wrapEntry("innerSourceRepositoryConnection", repositoryConfigs);
  }

  Entry<String, Object> getCpeMatchingConfiguration() {
    return wrapEntry("cpeMatchingConfiguration", cpeMatchingConfigurationDAO.getAll());
  }

  private static Entry<String, Object> wrapEntry(final String entryName, final Object objectToPut) {
    return new AbstractMap.SimpleImmutableEntry<>(entryName, objectToPut);
  }

  private void maskValueIfPresent(final String value, final Consumer<String> setter) {
    if (!Strings.isNullOrEmpty(value)) {
      setter.accept(SystemInfo.MASK);
    }
  }
}
