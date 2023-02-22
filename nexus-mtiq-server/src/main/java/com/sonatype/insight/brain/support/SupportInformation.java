/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SupportInformation
{
  private static final Logger log = LoggerFactory.getLogger(SupportInformation.class);

  private static final String JAVA_INFO_ENTRY = "java-info";

  private static final String JAVA_INFO_FILE = JAVA_INFO_ENTRY + ".json";

  private static final String PRODUCT_VERSION_ENTRY = "product-version";

  private static final String PRODUCT_VERSION_FILE = PRODUCT_VERSION_ENTRY + ".json";

  private static final String PRODUCT_LICENSE_FILE = "product-license.json";

  private static final String USERS_FILE = "users.json";

  private static final String ROLES_FILE = "roles.json";

  private static final String MEMBERSHIP_MAPPINGS_FILE = "membership_mappings.json";

  private static final String POLICIES_FILE = "policies.json";

  private static final String COMPONENTS_IN_QUARANTINE_FILE = "components_in_quarantine.json";

  private static final String WAIVERS_FILE = "waivers.json";

  private final VersionService versionService;

  private final DbData dbData;

  private final SystemInfo systemInfo;

  private final SupportInfoUtil supportInfoUtil;

  private List<SupportFile> supportFiles;

  @Inject
  public SupportInformation(
      VersionService versionService,
      DbData dbData,
      SystemInfo systemInfo,
      SupportInfoUtil supportInfoUtil)
  {
    this.versionService = versionService;
    this.dbData = dbData;
    this.systemInfo = systemInfo;
    this.supportInfoUtil = supportInfoUtil;
  }

  public SupportInformation aNewListOfSupportFiles() {
    this.supportFiles = new ArrayList<>();
    return this;
  }

  public SupportInformation withJavaVersion() {
    String javaVersionJson = JsonUtils.format(systemInfo.getObfuscatedSystemProperties("java", JAVA_INFO_ENTRY));

    createAndAddSupportFile(javaVersionJson, JAVA_INFO_FILE, SupportFileType.INFO);

    return this;
  }

  public SupportInformation withProductVersion() {
    String productVersionJson =
        systemInfo.getPropertiesJson(versionService.getProperties(), PRODUCT_VERSION_ENTRY);

    createAndAddSupportFile(productVersionJson, PRODUCT_VERSION_FILE, SupportFileType.INFO);

    return this;
  }

  public SupportInformation withLicenseDetails() {
    String productLicenseJson = systemInfo.getProductLicense();

    createAndAddSupportFile(productLicenseJson, PRODUCT_LICENSE_FILE, SupportFileType.INFO);

    return this;
  }

  public SupportInformation withUsersDetails() {
    Entry<String, Object> users = dbData.getUser();
    String usersJson = JsonUtils.format(users);

    createAndAddSupportFile(usersJson, USERS_FILE, SupportFileType.TENANT);

    return this;
  }

  public SupportInformation withRolesDetails() {
    Entry<String, Object> roles = dbData.getRole();
    String rolesJson = JsonUtils.format(roles);

    createAndAddSupportFile(rolesJson, ROLES_FILE, SupportFileType.TENANT);

    return this;
  }

  public SupportInformation withMembershipMappings() {
    Entry<String, Object> membershipMappings = dbData.getMembershipMapping();
    String membershipMappingsJson = JsonUtils.format(membershipMappings);

    createAndAddSupportFile(membershipMappingsJson, MEMBERSHIP_MAPPINGS_FILE, SupportFileType.TENANT);

    return this;
  }

  public SupportInformation withPolicies() {
    Entry<String, Object> policies = dbData.getPolicy();
    String policiesJson = JsonUtils.format(policies);

    createAndAddSupportFile(policiesJson, POLICIES_FILE, SupportFileType.TENANT);

    return this;
  }

  public SupportInformation withComponentsInQuarantine() {
    Entry<String, Object> quarantinedComponents = dbData.getQuarantinedComponent();
    String quarantinedComponentsJson = JsonUtils.format(quarantinedComponents);

    createAndAddSupportFile(quarantinedComponentsJson, COMPONENTS_IN_QUARANTINE_FILE, SupportFileType.TENANT);

    return this;
  }

  public SupportInformation withWaivers() {
    Entry<String, Object> waivers = dbData.getWaiver();
    String policiesJson = JsonUtils.format(waivers);

    createAndAddSupportFile(policiesJson, WAIVERS_FILE, SupportFileType.TENANT);

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
