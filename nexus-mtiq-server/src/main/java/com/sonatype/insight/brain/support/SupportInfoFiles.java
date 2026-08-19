/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;
import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.version.VersionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SupportInfoFiles
{
  private static final Logger log = LoggerFactory.getLogger(SupportInfoFiles.class);

  private static final String JAVA_INFO_ENTRY = "java-info";

  private static final String JAVA_INFO_FILE = JAVA_INFO_ENTRY + ".json";

  private static final String PRODUCT_VERSION_ENTRY = "product-version";

  private static final String PRODUCT_VERSION_FILE = PRODUCT_VERSION_ENTRY + ".json";

  private static final String PRODUCT_LICENSE_FILE = "product-license.json";

  private static final String TENANT_INFO_ENTRY = "tenant-info";

  private static final String TENANT_INFO_FILE = TENANT_INFO_ENTRY + ".json";

  private static final String USER_FILE = "user.json";

  private static final String SAML_USER_FILE = "samlUser.json";

  private static final String OAUTH2_USER_FILE = "oauth2User.json";

  private static final String ROLE_FILE = "role.json";

  private static final String ROLE_PERMISSION_FILE = "rolePermission.json";

  private static final String MEMBERSHIP_MAPPING_FILE = "membershipMapping.json";

  private static final String POLICY_FILE = "policy.json";

  private static final String COMPONENTS_IN_QUARANTINE_FILE = "componentsInQuarantine.json";

  private static final String WAIVER_FILE = "waiver.json";

  private static final String REPOSITORY_MANAGER_FILE = "repositoryManager.json";

  private static final String REPOSITORY_FILE = "repository.json";

  private static final String SECURITY_VULNERABILITY_OVERRIDE_FILE = "securityVulnerabilityOverride.json";

  private static final String SYSTEM_CONFIGURATION_FILE = "systemConfiguration.json";

  private static final String CONFIG_PROPERTIES_FILE = "config.json";

  private static final String SYSTEM_NOTICE_FILE = "systemNotice.json";

  private static final String WEBHOOK_FILE = "webhook.json";

  private static final String ORGANIZATION_FILE = "organization.json";

  private static final String APPLICATION_FILE = "application.json";

  private static final String APPLICATION_TAG_FILE = "applicationTag.json";

  private static final String TAG_FILE = "tag.json";

  private static final String POLICY_TAG_FILE = "policyTag.json";

  private static final String COMPONENT_LABEL_FILE = "componentLabel.json";

  private static final String LABEL_FILE = "label.json";

  private static final String DATA_RETENTION_POLICY_FILE = "dataRetentionPolicy.json";

  private static final String LICENSE_FILE = "license.json";

  private static final String MULTI_LICENSE_FILE = "multiLicense.json";

  private static final String LICENSE_THREAT_GROUP_FILE = "licenseThreatGroup.json";

  private static final String LICENSE_THREAT_GROUP_LICENSE_FILE = "licenseThreatGroupLicense.json";

  private static final String PROPRIETARY_CONFIG_FILE = "proprietaryConfig.json";

  private static final String SCM_FILE = "scm.json";

  private static final String SOURCE_CONTROL_FILE = "sourceControl.json";

  private static final String POLICY_MONITORING_FILE = "policyMonitoring.json";

  private static final String MIGRATION_TRACKER_FILE = "migrationTracker.json";

  private static final String INNER_SOURCE_REPOSITORY_CONFIGURATION_FILE = "innerSourceRepositoryConnection.json";

  private static final String SYSTEM_CONFIG_PROPERTIES_FILE = "systemConfigurationProperties.json";

  private static final String FEATURE_CONFIG_PROPERTIES_FILE = "featuresConfigurationProperties.json";

  private static final String TENANT_METADATA_FILE = "tenantMetadata.json";

  private final VersionService versionService;

  private final DbData dbData;

  private final SamlUserDAO samlUserDAO;

  private final OAuth2UserDAO oAuth2UserDAO;

  private final ConfigurationInfo configurationInfo;

  private final SystemInfo systemInfo;

  private final SourceControlConfigurationInfo sourceControlConfigurationInfo;

  private final FeaturePropertiesInfo featurePropertiesInfo;

  private final TenantMetadataDAO tenantMetadataDAO;

  private final SupportInfoUtil supportInfoUtil;

  private List<SupportFile> supportFiles;

  @Inject
  public SupportInfoFiles(
      VersionService versionService,
      DbData dbData,
      SamlUserDAO samlUserDAO,
      OAuth2UserDAO oAuth2UserDAO,
      ConfigurationInfo configurationInfo,
      SystemInfo systemInfo,
      SourceControlConfigurationInfo sourceControlConfigurationInfo,
      FeaturePropertiesInfo featurePropertiesInfo,
      TenantMetadataDAO tenantMetadataDAO,
      SupportInfoUtil supportInfoUtil)
  {
    this.versionService = versionService;
    this.dbData = dbData;
    this.samlUserDAO = samlUserDAO;
    this.oAuth2UserDAO = oAuth2UserDAO;
    this.configurationInfo = configurationInfo;
    this.systemInfo = systemInfo;
    this.sourceControlConfigurationInfo = sourceControlConfigurationInfo;
    this.featurePropertiesInfo = featurePropertiesInfo;
    this.tenantMetadataDAO = tenantMetadataDAO;
    this.supportInfoUtil = supportInfoUtil;
  }

  public SupportInfoFiles aNewListOfSupportFiles() {
    this.supportFiles = new ArrayList<>();
    return this;
  }

  public SupportInfoFiles withConfigPropertiesInfo() {
    String configPropertiesJson = configurationInfo.getConfigurationInfo();

    createAndAddSupportFile(configPropertiesJson, CONFIG_PROPERTIES_FILE, SupportFileType.CONFIG);

    return this;
  }

  public SupportInfoFiles withJavaVersion() {
    createAndAddSupportFile(systemInfo.getObfuscatedSystemProperties("java", JAVA_INFO_ENTRY),
        JAVA_INFO_FILE, SupportFileType.INFO);

    return this;
  }

  public SupportInfoFiles withProductVersion() {
    String productVersionJson =
        systemInfo.getPropertiesJson(versionService.getProperties(), PRODUCT_VERSION_ENTRY);

    createAndAddSupportFile(productVersionJson, PRODUCT_VERSION_FILE, SupportFileType.INFO);

    return this;
  }

  public SupportInfoFiles withLicenseDetails() {
    String productLicenseJson = systemInfo.getProductLicense();

    createAndAddSupportFile(productLicenseJson, PRODUCT_LICENSE_FILE, SupportFileType.INFO);

    return this;
  }

  public SupportInfoFiles withTenantInfo() {
    Properties properties = new Properties();
    properties.put("tenant", TenantThreadLocal.getTenant().tenantSlug);

    String tenantInfoJson = systemInfo.getPropertiesJson(properties, TENANT_INFO_ENTRY);

    createAndAddSupportFile(tenantInfoJson, TENANT_INFO_FILE, SupportFileType.INFO);

    return this;
  }

  public SupportInfoFiles withUsersDetails() {
    createAndAddSupportFile(dbData.getUser(), USER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withSamlUsersDetails() {
    createAndAddSupportFile(new AbstractMap.SimpleImmutableEntry<>("samlUser", samlUserDAO.getAll()),
        SAML_USER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withOauth2UsersDetails() {
    createAndAddSupportFile(new AbstractMap.SimpleImmutableEntry<>("oauth2User", oAuth2UserDAO.getAll()),
        OAUTH2_USER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withRolesDetails() {
    createAndAddSupportFile(dbData.getRole(), ROLE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withRolePermissionDetails() {
    createAndAddSupportFile(dbData.getRolePermission(), ROLE_PERMISSION_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withMembershipMappings() {
    createAndAddSupportFile(dbData.getMembershipMapping(), MEMBERSHIP_MAPPING_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withPolicies() {
    createAndAddSupportFile(dbData.getPolicy(), POLICY_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withComponentsInQuarantine() {
    createAndAddSupportFile(dbData.getQuarantinedComponent(), COMPONENTS_IN_QUARANTINE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withWaivers() {
    createAndAddSupportFile(dbData.getWaiver(), WAIVER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withRepositoryManager() {
    createAndAddSupportFile(dbData.getRepositoryManager(), REPOSITORY_MANAGER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withRepositories() {
    createAndAddSupportFile(dbData.getRepository(), REPOSITORY_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withSecurityVulnerabilityOverrides() {
    createAndAddSupportFile(dbData.getSecurityVulnerabilityOverride(), SECURITY_VULNERABILITY_OVERRIDE_FILE,
        SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withSystemConfigurationInfo() {
    createAndAddSupportFile(dbData.getSystemConfiguration(), SYSTEM_CONFIGURATION_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withSystemNoticeInfo() {
    createAndAddSupportFile(dbData.getSystemNotice(), SYSTEM_NOTICE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withWebhookInfo() {
    createAndAddSupportFile(dbData.getWebhook(), WEBHOOK_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withOrganizationInfo() {
    createAndAddSupportFile(dbData.getOrganization(), ORGANIZATION_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withApplicationInfo() {
    createAndAddSupportFile(dbData.getApplication(), APPLICATION_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withApplicationTagInfo() {
    createAndAddSupportFile(dbData.getApplicationTag(), APPLICATION_TAG_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withTagInfo() {
    createAndAddSupportFile(dbData.getTag(), TAG_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withPolicyTagInfo() {
    createAndAddSupportFile(dbData.getPolicyTag(), POLICY_TAG_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withComponentLabelInfo() {
    createAndAddSupportFile(dbData.getComponentLabel(), COMPONENT_LABEL_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withLabelInfo() {
    createAndAddSupportFile(dbData.getLabel(), LABEL_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withDataRetentionPolicyInfo() {
    createAndAddSupportFile(dbData.getDataRetentionPolicy(), DATA_RETENTION_POLICY_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withLicenseInfo() {
    createAndAddSupportFile(dbData.getLicense(), LICENSE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withMultiLicenseInfo() {
    createAndAddSupportFile(dbData.getMultiLicense(), MULTI_LICENSE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withLicenseThreatGroupInfo() {
    createAndAddSupportFile(dbData.getLicenseThreatGroup(), LICENSE_THREAT_GROUP_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withLicenseThreatGroupLicenseInfo() {
    createAndAddSupportFile(dbData.getLicenseThreatGroupLicense(), LICENSE_THREAT_GROUP_LICENSE_FILE,
        SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withProprietaryConfigInfo() {
    createAndAddSupportFile(dbData.getProprietaryConfig(), PROPRIETARY_CONFIG_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withScmInfo() {
    String scmJson = sourceControlConfigurationInfo.getSourceControlConfigurationInfo();

    createAndAddSupportFile(scmJson, SCM_FILE, SupportFileType.CONFIG);

    return this;
  }

  public SupportInfoFiles withSourceControlInfo() {
    createAndAddSupportFile(dbData.getSourceControl(), SOURCE_CONTROL_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withPolicyMonitoringInfo() {
    createAndAddSupportFile(dbData.getPolicyMonitoring(), POLICY_MONITORING_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withMigrationTrackerInfo() {
    createAndAddSupportFile(dbData.getMigrationTracker(), MIGRATION_TRACKER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withInnerSourceRepositoryInfo() {
    createAndAddSupportFile(dbData.getInnerSourceRepositoriesConfiguration(),
        INNER_SOURCE_REPOSITORY_CONFIGURATION_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withSystemConfigPropertiesInfo() {
    String flagConfigPropertiesJson = featurePropertiesInfo.getSystemConfigPropertiesJson();

    createAndAddSupportFile(flagConfigPropertiesJson, SYSTEM_CONFIG_PROPERTIES_FILE, SupportFileType.CONFIG);

    return this;
  }

  public SupportInfoFiles withFeatureConfigPropertiesInfo() {
    Map<String, Boolean> featureConfigProperties = featurePropertiesInfo.getFeatureConfigProperties(
        MTIQFeatureService.BANNED_SYSTEM_CONFIGURATION_PROPERTY_FEATURES);

    createAndAddSupportFile(featureConfigProperties, FEATURE_CONFIG_PROPERTIES_FILE, SupportFileType.CONFIG);

    return this;
  }

  public SupportInfoFiles withTenantMetadataInfo() {
    TenantMetadata tenantMetadata = tenantMetadataDAO.get();

    createAndAddSupportFile(new AbstractMap.SimpleImmutableEntry<>("tenantMetadata", tenantMetadata),
        TENANT_METADATA_FILE, SupportFileType.DB);

    return this;
  }

  public List<SupportFile> build() {
    return this.supportFiles;
  }

  private void createAndAddSupportFile(String json, String fileName, SupportFileType supportFileType) {
    try {
      File jsonFile = supportInfoUtil.writeTextToFile(json, fileName);

      recordSupportFile(jsonFile, fileName, supportFileType);
    }
    catch (IOException e) {
      log.warn("Could not create support file: {}, error: {}", fileName, e.getMessage(), e);
    }
  }

  /**
   * Streams the given POJO as JSON straight to a support file on disk. Prefer this over the
   * String-based overload whenever the payload originates from the database or another source
   * that can be arbitrarily large — a String/byte[] representation would OOM past ~2 GiB and
   * restart the shared MTIQ task (see CLM-42243).
   */
  private void createAndAddSupportFile(Object pojo, String fileName, SupportFileType supportFileType) {
    try {
      File jsonFile = supportInfoUtil.writePojoAsJsonToFile(pojo, fileName);

      recordSupportFile(jsonFile, fileName, supportFileType);
    }
    catch (IOException e) {
      log.warn("Could not create support file: {}, error: {}", fileName, e.getMessage(), e);
    }
  }

  private void recordSupportFile(File jsonFile, String fileName, SupportFileType supportFileType) {
    if (jsonFile.exists()) {
      log.info("Adding support file: {}", jsonFile.getName());
      this.supportFiles.add(new SupportFile(supportFileType, jsonFile, true));
    }
    else {
      log.info("Skipped support file: {}", fileName);
    }
  }
}
