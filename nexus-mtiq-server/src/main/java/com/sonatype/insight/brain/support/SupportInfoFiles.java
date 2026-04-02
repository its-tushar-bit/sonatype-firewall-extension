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
import java.util.Map.Entry;
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
import com.sonatype.insight.json.store.JsonUtils;

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
    String javaVersionJson = JsonUtils.format(systemInfo.getObfuscatedSystemProperties("java", JAVA_INFO_ENTRY));

    createAndAddSupportFile(javaVersionJson, JAVA_INFO_FILE, SupportFileType.INFO);

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
    Entry<String, Object> users = dbData.getUser();
    String usersJson = JsonUtils.format(users);

    createAndAddSupportFile(usersJson, USER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withSamlUsersDetails() {
    Entry<String, Object> samlUsers = new AbstractMap.SimpleImmutableEntry<>("samlUser", samlUserDAO.getAll());
    String samlUsersJson = JsonUtils.format(samlUsers);

    createAndAddSupportFile(samlUsersJson, SAML_USER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withOauth2UsersDetails() {
    Entry<String, Object> oauth2Users = new AbstractMap.SimpleImmutableEntry<>("oauth2User", oAuth2UserDAO.getAll());
    String oauthUsersJson = JsonUtils.format(oauth2Users);

    createAndAddSupportFile(oauthUsersJson, OAUTH2_USER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withRolesDetails() {
    Entry<String, Object> roles = dbData.getRole();
    String rolesJson = JsonUtils.format(roles);

    createAndAddSupportFile(rolesJson, ROLE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withRolePermissionDetails() {
    Entry<String, Object> rolePermissions = dbData.getRolePermission();
    String rolePermissionsJson = JsonUtils.format(rolePermissions);

    createAndAddSupportFile(rolePermissionsJson, ROLE_PERMISSION_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withMembershipMappings() {
    Entry<String, Object> membershipMappings = dbData.getMembershipMapping();
    String membershipMappingsJson = JsonUtils.format(membershipMappings);

    createAndAddSupportFile(membershipMappingsJson, MEMBERSHIP_MAPPING_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withPolicies() {
    Entry<String, Object> policies = dbData.getPolicy();
    String policiesJson = JsonUtils.format(policies);

    createAndAddSupportFile(policiesJson, POLICY_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withComponentsInQuarantine() {
    Entry<String, Object> quarantinedComponents = dbData.getQuarantinedComponent();
    String quarantinedComponentsJson = JsonUtils.format(quarantinedComponents);

    createAndAddSupportFile(quarantinedComponentsJson, COMPONENTS_IN_QUARANTINE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withWaivers() {
    Entry<String, Object> waivers = dbData.getWaiver();
    String waiversJson = JsonUtils.format(waivers);

    createAndAddSupportFile(waiversJson, WAIVER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withRepositoryManager() {
    Entry<String, Object> repositoryManager = dbData.getRepositoryManager();
    String repositoryManagerJson = JsonUtils.format(repositoryManager);

    createAndAddSupportFile(repositoryManagerJson, REPOSITORY_MANAGER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withRepositories() {
    Entry<String, Object> repository = dbData.getRepository();
    String repositoriesJson = JsonUtils.format(repository);

    createAndAddSupportFile(repositoriesJson, REPOSITORY_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withSecurityVulnerabilityOverrides() {
    Entry<String, Object> securityVulnerabilityOverride = dbData.getSecurityVulnerabilityOverride();
    String securityVulnerabilityOverridesJson = JsonUtils.format(securityVulnerabilityOverride);

    createAndAddSupportFile(securityVulnerabilityOverridesJson, SECURITY_VULNERABILITY_OVERRIDE_FILE,
        SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withSystemConfigurationInfo() {
    Entry<String, Object> systemConfiguration = dbData.getSystemConfiguration();
    String systemConfigurationJson = JsonUtils.format(systemConfiguration);

    createAndAddSupportFile(systemConfigurationJson, SYSTEM_CONFIGURATION_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withSystemNoticeInfo() {
    Entry<String, Object> systemNotice = dbData.getSystemNotice();
    String systemNoticeJson = JsonUtils.format(systemNotice);

    createAndAddSupportFile(systemNoticeJson, SYSTEM_NOTICE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withWebhookInfo() {
    Entry<String, Object> webhook = dbData.getWebhook();
    String webhookJson = JsonUtils.format(webhook);

    createAndAddSupportFile(webhookJson, WEBHOOK_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withOrganizationInfo() {
    Entry<String, Object> organizations = dbData.getOrganization();
    String organizationsJson = JsonUtils.format(organizations);

    createAndAddSupportFile(organizationsJson, ORGANIZATION_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withApplicationInfo() {
    Entry<String, Object> applications = dbData.getApplication();
    String applicationsJson = JsonUtils.format(applications);

    createAndAddSupportFile(applicationsJson, APPLICATION_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withApplicationTagInfo() {
    Entry<String, Object> applicationTags = dbData.getApplicationTag();
    String applicationTagsJson = JsonUtils.format(applicationTags);

    createAndAddSupportFile(applicationTagsJson, APPLICATION_TAG_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withTagInfo() {
    Entry<String, Object> tags = dbData.getTag();
    String tagsJson = JsonUtils.format(tags);

    createAndAddSupportFile(tagsJson, TAG_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withPolicyTagInfo() {
    Entry<String, Object> policyTags = dbData.getPolicyTag();
    String policyTagsJson = JsonUtils.format(policyTags);

    createAndAddSupportFile(policyTagsJson, POLICY_TAG_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withComponentLabelInfo() {
    Entry<String, Object> componentLabels = dbData.getComponentLabel();
    String componentLabelsJson = JsonUtils.format(componentLabels);

    createAndAddSupportFile(componentLabelsJson, COMPONENT_LABEL_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withLabelInfo() {
    Entry<String, Object> labels = dbData.getLabel();
    String labelsJson = JsonUtils.format(labels);

    createAndAddSupportFile(labelsJson, LABEL_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withDataRetentionPolicyInfo() {
    Entry<String, Object> dataRetentionPolicy = dbData.getDataRetentionPolicy();
    String dataRetentionPolicyJson = JsonUtils.format(dataRetentionPolicy);

    createAndAddSupportFile(dataRetentionPolicyJson, DATA_RETENTION_POLICY_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withLicenseInfo() {
    Entry<String, Object> license = dbData.getLicense();
    String licenseJson = JsonUtils.format(license);

    createAndAddSupportFile(licenseJson, LICENSE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withMultiLicenseInfo() {
    Entry<String, Object> multiLicense = dbData.getMultiLicense();
    String multiLicenseJson = JsonUtils.format(multiLicense);

    createAndAddSupportFile(multiLicenseJson, MULTI_LICENSE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withLicenseThreatGroupInfo() {
    Entry<String, Object> licenseThreatGroup = dbData.getLicenseThreatGroup();
    String licenseThreatGroupJson = JsonUtils.format(licenseThreatGroup);

    createAndAddSupportFile(licenseThreatGroupJson, LICENSE_THREAT_GROUP_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withLicenseThreatGroupLicenseInfo() {
    Entry<String, Object> licenseThreatGroupLicense = dbData.getLicenseThreatGroupLicense();
    String licenseThreatLicenseGroupJson = JsonUtils.format(licenseThreatGroupLicense);

    createAndAddSupportFile(licenseThreatLicenseGroupJson, LICENSE_THREAT_GROUP_LICENSE_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withProprietaryConfigInfo() {
    Entry<String, Object> proprietaryConfig = dbData.getProprietaryConfig();
    String proprietaryConfigJson = JsonUtils.format(proprietaryConfig);

    createAndAddSupportFile(proprietaryConfigJson, PROPRIETARY_CONFIG_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withScmInfo() {
    String scmJson = sourceControlConfigurationInfo.getSourceControlConfigurationInfo();

    createAndAddSupportFile(scmJson, SCM_FILE, SupportFileType.CONFIG);

    return this;
  }

  public SupportInfoFiles withSourceControlInfo() {
    Entry<String, Object> sourceControl = dbData.getSourceControl();
    String sourceControlJson = JsonUtils.format(sourceControl);

    createAndAddSupportFile(sourceControlJson, SOURCE_CONTROL_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withPolicyMonitoringInfo() {
    Entry<String, Object> policyMonitoring = dbData.getPolicyMonitoring();
    String policyMonitoringJson = JsonUtils.format(policyMonitoring);

    createAndAddSupportFile(policyMonitoringJson, POLICY_MONITORING_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withMigrationTrackerInfo() {
    Entry<String, Object> migrationTracker = dbData.getMigrationTracker();
    String migrationTrackerJson = JsonUtils.format(migrationTracker);

    createAndAddSupportFile(migrationTrackerJson, MIGRATION_TRACKER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withInnerSourceRepositoryInfo() {
    Entry<String, Object> innerSourceRepositoriesConfiguration = dbData.getInnerSourceRepositoriesConfiguration();
    String innerSourceRepositoriesConfigurationJson = JsonUtils.format(innerSourceRepositoriesConfiguration);

    createAndAddSupportFile(innerSourceRepositoriesConfigurationJson, INNER_SOURCE_REPOSITORY_CONFIGURATION_FILE,
        SupportFileType.DB);

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
    String featureConfigPropertiesJson = JsonUtils.format(featureConfigProperties);

    createAndAddSupportFile(featureConfigPropertiesJson, FEATURE_CONFIG_PROPERTIES_FILE, SupportFileType.CONFIG);

    return this;
  }

  public SupportInfoFiles withTenantMetadataInfo() {
    TenantMetadata tenantMetadata = tenantMetadataDAO.get();
    String tenantMetadataJson =
        JsonUtils.format(new AbstractMap.SimpleImmutableEntry<>("tenantMetadata", tenantMetadata));

    createAndAddSupportFile(tenantMetadataJson, TENANT_METADATA_FILE, SupportFileType.DB);

    return this;
  }

  public List<SupportFile> build() {
    return this.supportFiles;
  }

  private void createAndAddSupportFile(String json, String fileName, SupportFileType supportFileType) {
    try {
      File jsonFile = supportInfoUtil.writeTextToFile(json, fileName);

      if (jsonFile.exists()) {
        log.info("Adding support file: {}", jsonFile.getName());
        this.supportFiles.add(new SupportFile(supportFileType, jsonFile, true));
      }
      else {
        log.info("Skipped support file: {}", fileName);
      }
    }
    catch (IOException e) {
      log.warn("Could not create support file: {}, error: {}", fileName, e.getMessage(), e);
    }
  }
}
