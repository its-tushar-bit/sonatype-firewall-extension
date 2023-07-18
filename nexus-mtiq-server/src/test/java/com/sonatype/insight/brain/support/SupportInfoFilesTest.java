/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.IOException;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.support.SupportInfoTestHelper.WORK_DIR;
import static com.sonatype.insight.brain.support.SupportInfoTestHelper.cleanWorkDir;
import static com.sonatype.insight.brain.support.SupportInfoTestHelper.writeFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SupportInfoFilesTest
    extends MultiTenantTestSupport
{
  @Mock
  private VersionService versionService;

  @Mock
  private DbData dbData;

  @Mock
  private SamlUserDAO samlUserDao;

  @Mock
  private ConfigurationInfo configurationInfo;

  @Mock
  private SystemInfo systemInfo;

  @Mock
  private SupportInfoUtil supportInfoUtil;

  private SupportInfoFiles supportInfoFiles;

  @Before
  @Override
  public void setup() {
    super.setup();
    supportInfoFiles =
        new SupportInfoFiles(versionService, dbData, samlUserDao, configurationInfo, systemInfo, supportInfoUtil);
  }

  @AfterClass
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(writeFile(WORK_DIR, javaInfo, "java-info.json"));
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedSamlUsers), "samlUser.json"));
    SupportFile supportFile =
        supportInfoFiles.aNewListOfSupportFiles().withSamlUsersDetails().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedSamlUsers));
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    RepositoryComponent repositoryComponent1 = new RepositoryComponent();
    repositoryComponent1.setQuarantineTime(new Date());
    repositoryComponent1.setUnquarantineTimeForManualRelease(null);
    RepositoryComponent repositoryComponent2 = new RepositoryComponent();
    repositoryComponent2.setQuarantineTime(new Date());
    repositoryComponent2.setUnquarantineTimeForManualRelease(null);

    List<RepositoryComponent> quarantinedComponents = Arrays.asList(repositoryComponent1, repositoryComponent2);
    Map<String, Object> expectedQuarantinedComponents = new HashMap<>();
    expectedQuarantinedComponents.put("quarantinedComponent", quarantinedComponents);

    // When
    when(dbData.getQuarantinedComponent()).thenReturn(wrapEntry("quarantinedComponent", quarantinedComponents));
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
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
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(webhook), "webhook.json"));
    SupportFile supportFile = supportInfoFiles.aNewListOfSupportFiles().withWebhookInfo().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(webhook));
  }

  private Entry<String, SortedMap<String, Object>> wrapEntry(String entryName, SortedMap<String, Object> objectToPut) {
    return new AbstractMap.SimpleImmutableEntry<>(entryName, objectToPut);
  }

  private Entry<String, Object> wrapEntry(String entryName, Object objectToPut) {
    return new AbstractMap.SimpleImmutableEntry<>(entryName, objectToPut);
  }
}
