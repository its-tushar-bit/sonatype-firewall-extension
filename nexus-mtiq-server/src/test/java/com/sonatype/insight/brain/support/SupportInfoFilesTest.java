/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;
import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.support.SupportInfoTestHelper.WORK_DIR;
import static com.sonatype.insight.brain.support.SupportInfoTestHelper.cleanWorkDir;
import static com.sonatype.insight.brain.support.SupportInfoTestHelper.writeFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SupportInfoFilesTest
    extends AbstractMultiTenantTest
{
  @Mock
  private VersionService versionService;

  @Mock
  private DbData dbData;

  @Mock
  private SamlUserDAO samlUserDAO;

  @Mock
  private OAuth2UserDAO oAuth2UserDAO;

  @Mock
  private ConfigurationInfo configurationInfo;

  @Mock
  private SystemInfo systemInfo;

  @Mock
  private SourceControlConfigurationInfo sourceControlConfigurationInfo;

  @Mock
  private FeaturePropertiesInfo featurePropertiesInfo;

  @Mock
  private TenantMetadataDAO tenantMetadataDAO;

  @Mock
  private SupportInfoUtil supportInfoUtil;

  private SupportInfoFiles supportInfoFiles;

  @BeforeEach
  public void setup() {
    supportInfoFiles =
        new SupportInfoFiles(versionService, dbData, samlUserDAO, oAuth2UserDAO, configurationInfo, systemInfo,
            sourceControlConfigurationInfo, featurePropertiesInfo, tenantMetadataDAO, supportInfoUtil);
  }

  @AfterAll
  public static void tearDown() throws IOException {
    cleanWorkDir(WORK_DIR);
  }

  // CONFIG folder:

  @Test
  public void shouldProvideConfigPropertiesInfo() throws IOException {
    // Given
    final SortedMap<String, Object> entries = new TreeMap<>();
    entries.put(SystemConfigurationProperty.BASE_URL, "https://localhost/");
    String configProperties = JsonUtils.format(entries);

    // When
    when(configurationInfo.getConfigurationInfo()).thenReturn(configProperties);
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(configProperties), "config.json"));
    SupportFile supportFile = supportInfoFiles.aNewListOfSupportFiles().withConfigPropertiesInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(configProperties));
  }

  // INFO folder:

  @Test
  public void shouldProvideJavaVersion() throws IOException {
    // Given
    SortedMap<String, Object> entries = new TreeMap<>();
    entries.put("java-info", ImmutableMap.of("java.specification.version", "1.8"));
    String javaInfo = new ObjectMapper().writeValueAsString(entries);

    // When
    when(systemInfo.getObfuscatedSystemProperties("java", "java-info")).thenReturn(wrapEntry("java-info", entries));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any()))
        .thenReturn(writeFile(WORK_DIR, javaInfo, "java-info.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withJavaVersion().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo("{\"java-info\":{\"java.specification.version\":\"1.8\"}}");
  }

  @Test
  public void shouldProvideProductVersion() throws IOException {
    // Given
    SortedMap<String, Object> entries = new TreeMap<>();
    entries.put("product-version", ImmutableMap.of("version", "1.156.0"));
    String versionInfo = new ObjectMapper().writeValueAsString(entries);

    // When
    when(systemInfo.getPropertiesJson(any(), eq("product-version"))).thenReturn(versionInfo);
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, versionInfo, "product-version.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withProductVersion().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo("{\"product-version\":{\"version\":\"1.156.0\"}}");
  }

  @Test
  public void shouldProvideLicenseDetails() throws IOException {
    // Given
    SortedMap<String, Object> entries = new TreeMap<>();
    entries.put("licenseInfo", ImmutableMap.of("productEdition", "Lifecycle", "fingerprint", "53274cced19"));
    String licenseInfo = new ObjectMapper().writeValueAsString(entries);

    // When
    when(systemInfo.getProductLicense()).thenReturn(licenseInfo);
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, licenseInfo, "product-license.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withLicenseDetails().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(
        "{\"licenseInfo\":{\"productEdition\":\"Lifecycle\",\"fingerprint\":\"53274cced19\"}}");
  }

  @Test
  public void shouldProvideTenantInfo() throws IOException {
    // Given
    SortedMap<String, Object> entries = new TreeMap<>();
    entries.put("tenant", "tenant-slug");
    String tenantInfo = new ObjectMapper().writeValueAsString(entries);

    // When
    when(systemInfo.getPropertiesJson(any(), eq("tenant-info"))).thenReturn(tenantInfo);
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(writeFile(WORK_DIR, tenantInfo, "tenant-info.json"));
    SupportFile supportFile = supportInfoFiles.aNewListOfSupportFiles().withTenantInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo("{\"tenant\":\"tenant-slug\"}");
  }

  // DB Folder:

  @Test
  public void shouldProvideUsersDetails() throws IOException {
    // Given
    User user1 = new User();
    user1.setId("ADMIN");
    user1.setUsername("admin");
    User user2 = new User();
    user2.setId("0ce490d5");
    user2.setUsername("devuser");
    List<User> users = Arrays.asList(user1, user2);
    Map<String, Object> expectedUsers = new HashMap<>();
    expectedUsers.put("user", users);

    // When
    when(dbData.getUser()).thenReturn(wrapEntry("user", users));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedUsers), "user.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withUsersDetails().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedUsers));
  }

  @Test
  public void shouldProvideSamlUsersDetails() throws IOException {
    // Given
    SamlUser samlUser1 = new SamlUser();
    samlUser1.setId("id1");
    samlUser1.setUsername("name1");
    SamlUser samlUser2 = new SamlUser();
    samlUser2.setId("id2");
    samlUser2.setUsername("name2");
    List<SamlUser> samlUsers = Arrays.asList(samlUser1, samlUser2);
    Map<String, Object> expectedSamlUsers = new HashMap<>();
    expectedSamlUsers.put("user", samlUsers);

    // When
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedSamlUsers), "samlUser.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withSamlUsersDetails().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedSamlUsers));
  }

  @Test
  public void shouldProvideOAuth2UsersDetails() throws IOException {
    // Given
    OAuth2User oAuth2User1 = new OAuth2User();
    oAuth2User1.setId("id1");
    oAuth2User1.setUsername("name1");
    OAuth2User oAuth2User2 = new OAuth2User();
    oAuth2User2.setId("id2");
    oAuth2User2.setUsername("name2");
    List<OAuth2User> oAuth2Users = Arrays.asList(oAuth2User1, oAuth2User2);
    Map<String, Object> expectedOAuth2Users = new HashMap<>();
    expectedOAuth2Users.put("user", oAuth2Users);

    // When
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedOAuth2Users), "oauth2User.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withOauth2UsersDetails().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedOAuth2Users));
  }

  @Test
  public void shouldProvideRolesDetails() throws IOException {
    // Given
    Role role1 = new Role();
    role1.setName("System Administrator");
    role1.setSortOrder(100);
    Role role2 = new Role();
    role2.setName("System Administrator");
    role2.setSortOrder(100);
    List<Role> roles = Arrays.asList(role1, role2);
    Map<String, Object> expectedRoles = new HashMap<>();
    expectedRoles.put("role", roles);

    // When
    when(dbData.getRole()).thenReturn(wrapEntry("role", roles));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedRoles), "role.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withRolesDetails().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedRoles));
  }

  @Test
  public void shouldProvideRolePermissionDetails() throws IOException {
    // Given
    RolePermission rolePermission1 = new RolePermission();
    rolePermission1.setId("id1");
    rolePermission1.setRoleId("rid1");
    rolePermission1.setPermission(Permission.READ);

    RolePermission rolePermission2 = new RolePermission();
    rolePermission2.setId("id1");
    rolePermission2.setRoleId("rid2");
    rolePermission2.setPermission(Permission.WRITE);

    List<RolePermission> rolePermissions = Arrays.asList(rolePermission1, rolePermission2);
    Map<String, Object> expectedRoles = new HashMap<>();
    expectedRoles.put("rolePermission", rolePermissions);

    // When
    when(dbData.getRolePermission()).thenReturn(wrapEntry("rolePermission", rolePermissions));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedRoles), "rolePermission.json"));
    SupportFile supportFile = supportInfoFiles.aNewListOfSupportFiles().withRolePermissionDetails().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedRoles));
  }

  @Test
  public void shouldProvideMembershipMappings() throws IOException {
    // Given
    MembershipMapping mapping1 = new MembershipMapping();
    mapping1.setRoleId("1b92fae");
    mapping1.setMemberName("admin");
    MembershipMapping mapping2 = new MembershipMapping();
    mapping2.setRoleId("1da70fa");
    mapping2.setMemberName("testuser");
    List<MembershipMapping> membershipMappings = Arrays.asList(mapping1, mapping2);
    Map<String, Object> expectedMembershipMappings = new HashMap<>();
    expectedMembershipMappings.put("role", membershipMappings);

    // When
    when(dbData.getMembershipMapping()).thenReturn(wrapEntry("membership_mapping", membershipMappings));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedMembershipMappings), "membership_mapping.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withMembershipMappings().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedMembershipMappings));
  }

  @Test
  public void shouldProvidePolicies() throws IOException {
    // Given
    Policy policy1 = new Policy();
    policy1.setName("Architecture-Cleanup");
    policy1.setThreatLevel(1);
    Policy policy2 = new Policy();
    policy2.setName("Integrity-Rating");
    policy2.setThreatLevel(9);
    List<Policy> policies = Arrays.asList(policy1, policy2);
    Map<String, Object> expectedPolicies = new HashMap<>();
    expectedPolicies.put("policy", policies);

    // When
    when(dbData.getPolicy()).thenReturn(wrapEntry("policy", policies));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedPolicies), "policy.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withPolicies().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedPolicies));
  }

  @Test
  public void shouldProvideComponentsInQuarantine() throws IOException {
    // Given
    ProxyRepositoryComponent repositoryComponent1 = new ProxyRepositoryComponent();
    repositoryComponent1.setQuarantineTime(new Date());
    repositoryComponent1.setUnquarantineTimeForManualRelease(null);
    ProxyRepositoryComponent repositoryComponent2 = new ProxyRepositoryComponent();
    repositoryComponent2.setQuarantineTime(new Date());
    repositoryComponent2.setUnquarantineTimeForManualRelease(null);

    List<ProxyRepositoryComponent> quarantinedComponents = Arrays.asList(repositoryComponent1, repositoryComponent2);
    Map<String, Object> expectedQuarantinedComponents = new HashMap<>();
    expectedQuarantinedComponents.put("quarantinedComponent", quarantinedComponents);

    // When
    when(dbData.getQuarantinedComponent()).thenReturn(wrapEntry("quarantinedComponent", quarantinedComponents));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedQuarantinedComponents),
            "components_in_quarantine.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withComponentsInQuarantine().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedQuarantinedComponents));
  }

  @Test
  public void shouldProvideWaivers() throws IOException {
    // Given
    PolicyWaiver policyWaiver1 = new PolicyWaiver();
    policyWaiver1.setComment("Setting waiver 1");
    policyWaiver1.setExpiryTime(new Date());
    PolicyWaiver policyWaiver2 = new PolicyWaiver();
    policyWaiver2.setComment("Setting waiver 2");
    policyWaiver2.setExpiryTime(new Date());
    List<PolicyWaiver> waivers = Arrays.asList(policyWaiver1, policyWaiver2);
    Map<String, Object> expectedWaivers = new HashMap<>();
    expectedWaivers.put("waiver", waivers);

    // When
    when(dbData.getWaiver()).thenReturn(wrapEntry("waiver", waivers));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedWaivers), "waiver.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withWaivers().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedWaivers));
  }

  @Test
  public void shouldProvideRepositoryManagerInfo() throws IOException {
    // Given
    RepositoryManager repositoryManager = new RepositoryManager();
    repositoryManager.setId("id");
    repositoryManager.setName("name");

    // When
    when(dbData.getRepositoryManager()).thenReturn(wrapEntry("repositoryManager", repositoryManager));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(repositoryManager), "repositoryManager.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withRepositoryManager().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(repositoryManager));
  }

  @Test
  public void shouldProvideRepositoryInfo() throws IOException {
    // Given
    Repository repository = new Repository();
    repository.setId("id");
    repository.setRepositoryManagerId("rmId");

    // When
    when(dbData.getRepository()).thenReturn(wrapEntry("repository", repository));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(repository), "repository.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withRepositories().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(repository));
  }

  @Test
  public void shouldProvideSecurityVulnerabilityOverrides() throws IOException {
    // Given
    SecurityVulnerabilityOverride securityVulnerabilityOverride = new SecurityVulnerabilityOverride();
    securityVulnerabilityOverride.setId("id");
    securityVulnerabilityOverride.setSource("src");

    // When
    when(dbData.getSecurityVulnerabilityOverride()).thenReturn(
        wrapEntry("securityVulnerabilityOverride", securityVulnerabilityOverride));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(securityVulnerabilityOverride),
            "securityVulnerabilityOverride.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withSecurityVulnerabilityOverrides().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(securityVulnerabilityOverride));
  }

  @Test
  public void shouldProvideSystemConfigurationInfo() throws IOException {
    // Given
    SystemConfigurationProperty systemConfigurationProperty = new SystemConfigurationProperty();
    systemConfigurationProperty.setId("id");
    systemConfigurationProperty.setName("name");
    systemConfigurationProperty.setValue("value");

    // When
    when(dbData.getSystemConfiguration()).thenReturn(wrapEntry("systemConfiguration", systemConfigurationProperty));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(systemConfigurationProperty), "systemConfiguration.json"));
    SupportFile supportFile = supportInfoFiles.aNewListOfSupportFiles().withSystemConfigurationInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(systemConfigurationProperty));
  }

  @Test
  public void shouldProvideSystemNoticeInfo() throws IOException {
    // Given
    SystemNotice systemNotice = new SystemNotice();
    systemNotice.setId("id");
    systemNotice.setEnabled(true);
    systemNotice.setMessage("msg");

    // When
    when(dbData.getSystemNotice()).thenReturn(wrapEntry("systemNotice", systemNotice));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(systemNotice), "systemNotice.json"));
    SupportFile supportFile = supportInfoFiles.aNewListOfSupportFiles().withSystemNoticeInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(systemNotice));
  }

  @Test
  public void shouldProvideWebhookInfo() throws IOException {
    // Given
    Webhook webhook = new Webhook();
    webhook.setId("id");
    webhook.setUrl("url");

    // When
    when(dbData.getWebhook()).thenReturn(wrapEntry("webhook", webhook));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(webhook), "webhook.json"));
    SupportFile supportFile = supportInfoFiles.aNewListOfSupportFiles().withWebhookInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(webhook));
  }

  @Test
  public void shouldProvideOrganizationInfo() throws IOException {
    // Given
    Organization organization1 = new Organization();
    organization1.setParentOrganizationId("ROOT_ORGANIZATION_ID");
    organization1.setName("Test Organization 1");
    Organization organization2 = new Organization();
    organization2.setParentOrganizationId("ROOT_ORGANIZATION_ID");
    organization2.setName("Test Organization 2");
    List<Organization> organizations = Arrays.asList(organization1, organization2);
    Map<String, Object> expectedOrganizations = new HashMap<>();
    expectedOrganizations.put("organization", organizations);

    // When
    when(dbData.getOrganization()).thenReturn(wrapEntry("organization", expectedOrganizations));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedOrganizations), "organization.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withOrganizationInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedOrganizations));
  }

  @Test
  public void shouldProvideApplicationInfo() throws IOException {
    // Given
    Application application1 = new Application();
    application1.setName("Test Application 1");
    application1.setPublicId("test-application-1");
    Application application2 = new Application();
    application2.setName("Test Application 2");
    application2.setPublicId("test-application-2");
    List<Application> applications = Arrays.asList(application1, application2);
    Map<String, Object> expectedApplications = new HashMap<>();
    expectedApplications.put("application", applications);

    // When
    when(dbData.getApplication()).thenReturn(wrapEntry("application", expectedApplications));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedApplications), "application.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withApplicationInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedApplications));
  }

  @Test
  public void shouldProvideApplicationTagInfo() throws IOException {
    // Given
    ApplicationTag applicationTag1 = new ApplicationTag();
    applicationTag1.setApplicationId("1234");
    applicationTag1.setTagId("5678");
    List<ApplicationTag> applicationTags = Collections.singletonList(applicationTag1);
    Map<String, Object> expectedApplicationTags = new HashMap<>();
    expectedApplicationTags.put("applicationTag", applicationTags);

    // When
    when(dbData.getApplicationTag()).thenReturn(wrapEntry("applicationTag", expectedApplicationTags));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedApplicationTags), "applicationTag.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withApplicationTagInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedApplicationTags));
  }

  @Test
  public void shouldProvideTagInfo() throws IOException {
    // Given
    Tag tag1 = new Tag();
    tag1.setName("Test tag 1");
    tag1.setOrganizationId("ROOT_ORGANIZATION_ID");
    Tag tag2 = new Tag();
    tag2.setName("Test tag 2");
    tag2.setOrganizationId("ROOT_ORGANIZATION_ID");
    List<Tag> tags = Arrays.asList(tag1, tag2);
    Map<String, Object> expectedTags = new HashMap<>();
    expectedTags.put("tags", tags);

    // When
    when(dbData.getTag()).thenReturn(wrapEntry("tag", expectedTags));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedTags), "tag.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withTagInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedTags));
  }

  @Test
  public void shouldProvidePolicyTagInfo() throws IOException {
    // Given
    PolicyTag policyTag = new PolicyTag();
    policyTag.setPolicyId("1234");
    policyTag.setTagId("5678");
    List<PolicyTag> policyTags = Collections.singletonList(policyTag);
    Map<String, Object> expectedPolicyTags = new HashMap<>();
    expectedPolicyTags.put("policyTags", policyTags);

    // When
    when(dbData.getPolicyTag()).thenReturn(wrapEntry("policyTags", expectedPolicyTags));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedPolicyTags), "policyTag.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withPolicyTagInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedPolicyTags));
  }

  @Test
  public void shouldProvideComponentLabelInfo() throws IOException {
    // Given
    ComponentLabel componentLabel = new ComponentLabel();
    componentLabel.setLabelId("1234");
    componentLabel.setId("5678");
    List<ComponentLabel> componentLabels = Collections.singletonList(componentLabel);
    Map<String, Object> expectedComponentLabels = new HashMap<>();
    expectedComponentLabels.put("componentLabel", componentLabels);

    // When
    when(dbData.getComponentLabel()).thenReturn(wrapEntry("componentLabel", expectedComponentLabels));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedComponentLabels), "componentLabel.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withComponentLabelInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedComponentLabels));
  }

  @Test
  public void shouldProvideLabelInfo() throws IOException {
    // Given
    Label label1 = new Label();
    label1.setLabel("Test Label 1");
    label1.setColor(Color.dark_blue);
    Label label2 = new Label();
    label2.setLabel("Test Label 2");
    label2.setColor(Color.light_purple);
    List<Label> labels = Arrays.asList(label1, label2);
    Map<String, Object> expectedLabels = new HashMap<>();
    expectedLabels.put("label", labels);

    // When
    when(dbData.getLabel()).thenReturn(wrapEntry("label", expectedLabels));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedLabels), "label.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withLabelInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedLabels));
  }

  @Test
  public void shouldProvideDataRetentionPolicyInfo() throws IOException {
    // Given
    DataRetentionPolicy dataRetentionPolicy1 = new DataRetentionPolicy();
    dataRetentionPolicy1.setOwnerId("ROOT_ORGANIZATION_ID");
    dataRetentionPolicy1.setContextId("continuous-monitoring");
    DataRetentionPolicy dataRetentionPolicy2 = new DataRetentionPolicy();
    dataRetentionPolicy2.setOwnerId("ROOT_ORGANIZATION_ID");
    dataRetentionPolicy2.setContextId("build");
    DataRetentionPolicy dataRetentionPolicy3 = new DataRetentionPolicy();
    dataRetentionPolicy3.setOwnerId("ROOT_ORGANIZATION_ID");
    dataRetentionPolicy3.setContextId("develop");
    List<DataRetentionPolicy> dataRetentionPolicies =
        Arrays.asList(dataRetentionPolicy1, dataRetentionPolicy2, dataRetentionPolicy3);
    Map<String, Object> expectedDataRetentionPolicies = new HashMap<>();
    expectedDataRetentionPolicies.put("dataRetentionPolicy", dataRetentionPolicies);

    // When
    when(dbData.getDataRetentionPolicy()).thenReturn(wrapEntry("dataRetentionPolicy", expectedDataRetentionPolicies));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedDataRetentionPolicies), "dataRetentionPolicy.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withDataRetentionPolicyInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedDataRetentionPolicies));
  }

  @Test
  public void shouldProvideLicenseInfo() throws IOException {
    // Given
    License license1 = new License();
    license1.setLongDisplayName("BSD Zero Clause License");
    License license2 = new License();
    license2.setLongDisplayName("10tec Company License Agreement");
    License license3 = new License();
    license3.setLongDisplayName("123 Open-Source MIT Public License v2.0");
    List<License> licenses = Arrays.asList(license1, license2, license3);
    Map<String, Object> expectedLicenses = new HashMap<>();
    expectedLicenses.put("label", licenses);

    // When
    when(dbData.getLicense()).thenReturn(wrapEntry("license", expectedLicenses));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedLicenses), "license.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withLicenseInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedLicenses));
  }

  @Test
  public void shouldProvideMultiLicenseInfo() throws IOException {
    // Given
    MultiLicense multiLicense1 = new MultiLicense();
    multiLicense1.setShortDisplayName("0BSD");
    MultiLicense multiLicense2 = new MultiLicense();
    multiLicense2.setShortDisplayName("ZZZ-Projects-LA");
    MultiLicense multiLicense3 = new MultiLicense();
    multiLicense3.setShortDisplayName("Zuora-Inc-DTLA");

    List<MultiLicense> multiLicenses = Arrays.asList(multiLicense1, multiLicense2, multiLicense3);
    Map<String, Object> expectedMultiLicences = new HashMap<>();
    expectedMultiLicences.put("multiLicense", multiLicenses);

    // When
    when(dbData.getMultiLicense()).thenReturn(wrapEntry("multiLicense", expectedMultiLicences));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedMultiLicences), "multiLicense.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withMultiLicenseInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedMultiLicences));
  }

  @Test
  public void shouldProvideLicenseThreatGroupInfo() throws IOException {
    // Given
    LicenseThreatGroup licenseThreatGroup1 = new LicenseThreatGroup();
    licenseThreatGroup1.setName("Commercial");
    licenseThreatGroup1.setOwnerId("ROOT_ORGANIZATION_ID");
    LicenseThreatGroup licenseThreatGroup2 = new LicenseThreatGroup();
    licenseThreatGroup2.setName("Copyleft");
    licenseThreatGroup2.setOwnerId("ROOT_ORGANIZATION_ID");
    List<LicenseThreatGroup> licenseThreatGroups = Arrays.asList(licenseThreatGroup1, licenseThreatGroup2);
    Map<String, Object> expectedLicenseTreatGroups = new HashMap<>();
    expectedLicenseTreatGroups.put("licenseThreatGroup", licenseThreatGroups);

    // When
    when(dbData.getLicenseThreatGroup()).thenReturn(wrapEntry("licenseThreatGroup", expectedLicenseTreatGroups));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedLicenseTreatGroups), "licenseThreatGroup.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withLicenseThreatGroupInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedLicenseTreatGroups));
  }

  @Test
  public void shouldProvideLicenseThreatGroupLicenseInfo() throws IOException {
    // Given
    LicenseThreatGroupLicense licenseThreatGroupLicense1 = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense1.setLicenseId("SNIA");
    licenseThreatGroupLicense1.setOwnerId("ROOT_ORGANIZATION_ID");
    LicenseThreatGroupLicense licenseThreatGroupLicense2 = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense2.setLicenseId("psutils");
    licenseThreatGroupLicense2.setOwnerId("ROOT_ORGANIZATION_ID");
    LicenseThreatGroupLicense licenseThreatGroupLicense3 = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense3.setLicenseId("AGPL-1.0");
    licenseThreatGroupLicense3.setOwnerId("ROOT_ORGANIZATION_ID");
    List<LicenseThreatGroupLicense> licenseThreatGroupLicenses =
        Arrays.asList(licenseThreatGroupLicense1, licenseThreatGroupLicense2, licenseThreatGroupLicense3);
    Map<String, Object> expectedLicenseTreatGroupLicenses = new HashMap<>();
    expectedLicenseTreatGroupLicenses.put("licenseThreatGroupLicense", licenseThreatGroupLicenses);

    // When
    when(dbData.getLicenseThreatGroupLicense()).thenReturn(
        wrapEntry("licenseThreatGroupLicense", expectedLicenseTreatGroupLicenses));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedLicenseTreatGroupLicenses),
            "licenseThreatGroupLicense.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withLicenseThreatGroupLicenseInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedLicenseTreatGroupLicenses));
  }

  @Test
  public void shouldProvideProprietaryConfigInfo() throws IOException {
    // Given
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setOwnerId("ROOT_ORGANIZATION_ID");
    proprietaryConfig.setPackages(Arrays.asList("hello-foo.jar", "my-component.zip"));
    proprietaryConfig.setRegexes(Arrays.asList("test\\.zip", "hello-foo\\.jar"));

    List<ProprietaryConfig> proprietaryConfigs = Collections.singletonList(proprietaryConfig);
    Map<String, Object> expectedProprietaryConfigs = new HashMap<>();
    expectedProprietaryConfigs.put("proprietaryConfig", proprietaryConfigs);

    // When
    when(dbData.getProprietaryConfig()).thenReturn(wrapEntry("proprietaryConfig", expectedProprietaryConfigs));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedProprietaryConfigs), "proprietaryConfig.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withProprietaryConfigInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedProprietaryConfigs));
  }

  @Test
  public void shouldProvideScmInfo() throws IOException {
    // Given
    SortedMap<String, Object> entries = new TreeMap<>();
    entries.put("cloneDirectory", "source-control");
    entries.put("defaultBranchMonitoringIntervalHours", 24);
    entries.put("pullRequestMonitoringIntervalSeconds", 60);
    entries.put("useUsernameInRepositoryCloneUrl", false);
    String scmInfo = new ObjectMapper().writeValueAsString(entries);

    // When
    when(sourceControlConfigurationInfo.getSourceControlConfigurationInfo()).thenReturn(scmInfo);
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(writeFile(WORK_DIR, scmInfo, "scm.json"));
    SupportFile supportFile = supportInfoFiles.aNewListOfSupportFiles().withScmInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo("{\"cloneDirectory\":\"source-control\"," +
        "\"defaultBranchMonitoringIntervalHours\":24,\"pullRequestMonitoringIntervalSeconds\":60," +
        "\"useUsernameInRepositoryCloneUrl\":false}");
  }

  @Test
  public void shouldProvideSourceControlInfo() throws IOException {
    // Given
    SourceControl sourceControl1 = new SourceControl();
    sourceControl1.setOwnerId("ROOT_ORGANIZATION_ID");
    sourceControl1.setProvider(SourceControlProvider.GITHUB);
    sourceControl1.setPullRequestCommentingEnabled(true);
    SourceControl sourceControl2 = new SourceControl();
    sourceControl2.setOwnerId("123456");
    sourceControl2.setRepositoryUrl("https://github.com/sonatype/project");
    sourceControl2.setPullRequestPollTime(Date.from(Instant.now()));
    List<SourceControl> sourceControls = Arrays.asList(sourceControl1, sourceControl2);
    Map<String, Object> expectedSourceControls = new HashMap<>();
    expectedSourceControls.put("sourceControl", sourceControls);

    // When
    when(dbData.getSourceControl()).thenReturn(wrapEntry("sourceControl", expectedSourceControls));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedSourceControls), "sourceControl.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withSourceControlInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedSourceControls));
  }

  @Test
  public void shouldProvidePolicyMonitoringInfo() throws IOException {
    // Given
    PolicyMonitoring policyMonitoring1 = new PolicyMonitoring();
    policyMonitoring1.setStageTypeId(Stage.ID_BUILD);
    policyMonitoring1.setOwnerId("ROOT_ORGANIZATION_ID");
    PolicyMonitoring policyMonitoring2 = new PolicyMonitoring();
    policyMonitoring2.setStageTypeId(Stage.ID_STAGE_RELEASE);
    policyMonitoring2.setOwnerId("ROOT_ORGANIZATION_ID");
    List<PolicyMonitoring> policyMonitoring = Arrays.asList(policyMonitoring1, policyMonitoring2);
    Map<String, Object> expectedPolicyMonitoring = new HashMap<>();
    expectedPolicyMonitoring.put("policyMonitoring", policyMonitoring);

    // When
    when(dbData.getPolicyMonitoring()).thenReturn(wrapEntry("policyMonitoring", expectedPolicyMonitoring));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedPolicyMonitoring), "policyMonitoring.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withPolicyMonitoringInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedPolicyMonitoring));
  }

  @Test
  public void shouldProvideMigrationTrackerInfo() throws IOException {
    // Given
    MigrationTracker migrationTracker1 = new MigrationTracker();
    migrationTracker1.setId("policy-json");
    migrationTracker1.setVersion(1);
    MigrationTracker migrationTracker2 = new MigrationTracker();
    migrationTracker2.setId("policy-drools-code");
    migrationTracker2.setVersion(5);
    List<MigrationTracker> migrationTrackers = Arrays.asList(migrationTracker1, migrationTracker2);
    Map<String, Object> expectedMigrationTrackers = new HashMap<>();
    expectedMigrationTrackers.put("migrationTracker", migrationTrackers);

    // When
    when(dbData.getMigrationTracker()).thenReturn(wrapEntry("migrationTracker", expectedMigrationTrackers));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedMigrationTrackers), "migrationTracker.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withMigrationTrackerInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedMigrationTrackers));
  }

  @Test
  public void shouldProvideInnerSourceRepositoryConfigurationInfo() throws IOException {
    // Given
    List<RepositoryConnection> expectedRepositoryConfigs = new ArrayList<>();
    expectedRepositoryConfigs.add(new RepositoryConnection("ownerId1", "http://www.example.com",
        RepositoryFormat.MAVEN, "username", "password".toCharArray()));
    expectedRepositoryConfigs.add(new RepositoryConnection("ownerId2", "http://www.example.com",
        RepositoryFormat.GENERIC, "username2", "password2".toCharArray()));

    // When
    when(dbData.getInnerSourceRepositoriesConfiguration()).thenReturn(
        wrapEntry("innerSourceRepositoryConnection", expectedRepositoryConfigs));
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedRepositoryConfigs),
            "innerSourceRepositoryConnection.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withInnerSourceRepositoryInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedRepositoryConfigs));
  }

  @Test
  public void testWithSystemConfigPropertiesInfo() throws IOException {
    // Given
    Map<String, Object> sysConfigProperties = new HashMap<>();
    sysConfigProperties.put("autoWaivers", true);
    sysConfigProperties.put("blockNonAsciiInPath", false);
    sysConfigProperties.put("ADVANCED_REPORTING_INSIGHTS_ENABLED", true);
    sysConfigProperties.put("sbomBinaryScanning", true);
    String sysConfigPropertiesJson = JsonUtils.format(sysConfigProperties);

    // When
    when(featurePropertiesInfo.getSystemConfigPropertiesJson()).thenReturn(sysConfigPropertiesJson);
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, sysConfigPropertiesJson, "systemConfigurationProperties.json"));
    SupportFile supportFile = supportInfoFiles.aNewListOfSupportFiles().withSystemConfigPropertiesInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents.replace("\r\n", "\n")).isEqualTo("""
        {
          "autoWaivers" : true,
          "blockNonAsciiInPath" : false,
          "ADVANCED_REPORTING_INSIGHTS_ENABLED" : true,
          "sbomBinaryScanning" : true
        }""");
  }

  @Test
  public void testWithFeatureConfigPropertiesInfo() throws IOException {
    // Given
    Map<String, Boolean> featureConfigProperties = new LinkedHashMap<>();
    featureConfigProperties.put("ADVANCED_SEARCH_ENABLED", true);
    featureConfigProperties.put("dashboard", true);
    featureConfigProperties.put("enableSsoOnly", true);
    featureConfigProperties.put("saasLifecycleScmEnabled", true);
    featureConfigProperties.put("SSO_IDP_MANAGED_BY_SONATYPE", false);
    List<SystemConfigurationPropertyFeature> filteredFeatures =
        MTIQFeatureService.BANNED_SYSTEM_CONFIGURATION_PROPERTY_FEATURES;

    String featureConfigPropertiesJson = JsonUtils.format(featureConfigProperties);

    // When
    when(featurePropertiesInfo.getFeatureConfigProperties(filteredFeatures)).thenReturn(featureConfigProperties);
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, featureConfigPropertiesJson, "featuresConfigurationProperties.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withFeatureConfigPropertiesInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents.replace("\r\n", "\n")).isEqualTo("""
        {
          "ADVANCED_SEARCH_ENABLED" : true,
          "dashboard" : true,
          "enableSsoOnly" : true,
          "saasLifecycleScmEnabled" : true,
          "SSO_IDP_MANAGED_BY_SONATYPE" : false
        }""");
  }

  @Test
  public void testWithTenantMetadataInfo_ReturnsTenantMetadataFile() throws IOException {
    // Given
    TenantMetadata tenantMetadata =
        new TenantMetadata("appId", "appName", "connId", "connName", "keyName", "orgId", "orgName");
    String tenantMetadataJson =
        JsonUtils.format(new AbstractMap.SimpleImmutableEntry<>("tenantMetadata", tenantMetadata));

    // When
    when(tenantMetadataDAO.get()).thenReturn(tenantMetadata);
    when(supportInfoUtil.writePojoAsJsonToFile(any(), any()))
        .thenReturn(writeFile(WORK_DIR, tenantMetadataJson, "tenantMetadata.json"));
    SupportFile supportFile = supportInfoFiles.aNewListOfSupportFiles().withTenantMetadataInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(tenantMetadataJson);
  }

  private Entry<String, SortedMap<String, Object>> wrapEntry(String entryName, SortedMap<String, Object> objectToPut) {
    return new AbstractMap.SimpleImmutableEntry<>(entryName, objectToPut);
  }

  private Entry<String, Object> wrapEntry(String entryName, Object objectToPut) {
    return new AbstractMap.SimpleImmutableEntry<>(entryName, objectToPut);
  }
}
