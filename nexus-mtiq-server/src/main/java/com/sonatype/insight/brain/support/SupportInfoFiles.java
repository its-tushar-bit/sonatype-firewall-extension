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
import java.util.Map.Entry;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
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

  private final VersionService versionService;

  private final DbData dbData;

  private final SamlUserDAO samlUserDao;

  private final ConfigurationInfo configurationInfo;

  private final SystemInfo systemInfo;

  private final SupportInfoUtil supportInfoUtil;

  private List<SupportFile> supportFiles;

  @Inject
  public SupportInfoFiles(
      VersionService versionService,
      DbData dbData,
      SamlUserDAO samlUserDao,
      ConfigurationInfo configurationInfo,
      SystemInfo systemInfo,
      SupportInfoUtil supportInfoUtil)
  {
    this.versionService = versionService;
    this.dbData = dbData;
    this.samlUserDao = samlUserDao;
    this.configurationInfo = configurationInfo;
    this.systemInfo = systemInfo;
    this.supportInfoUtil = supportInfoUtil;
  }

  public SupportInfoFiles aNewListOfSupportFiles() {
    this.supportFiles = new ArrayList<>();
    return this;
  }

  // CONFIG folder:

  public SupportInfoFiles withConfigPropertiesInfo() {
    String configPropertiesJson = configurationInfo.getConfigurationInfo();

    createAndAddSupportFile(configPropertiesJson, CONFIG_PROPERTIES_FILE, SupportFileType.CONFIG);

    return this;
  }

  // INFO folder:

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

  // DB Folder:

  public SupportInfoFiles withUsersDetails() {
    Entry<String, Object> users = dbData.getUser();
    String usersJson = JsonUtils.format(users);

    createAndAddSupportFile(usersJson, USER_FILE, SupportFileType.DB);

    return this;
  }

  public SupportInfoFiles withSamlUsersDetails() {
    Entry<String, Object> samlUsers = new AbstractMap.SimpleImmutableEntry<>("samlUser", samlUserDao.getAll());
    String samlUsersJson = JsonUtils.format(samlUsers);

    createAndAddSupportFile(samlUsersJson, SAML_USER_FILE, SupportFileType.DB);

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
    String policiesJson = JsonUtils.format(waivers);

    createAndAddSupportFile(policiesJson, WAIVER_FILE, SupportFileType.DB);

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
