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

import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
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
public class SupportInformationTest
    extends MultiTenantTestSupport
{
  @Mock
  private VersionService versionService;

  @Mock
  private DbData dbData;

  @Mock
  private SystemInfo systemInfo;

  @Mock
  private SupportInfoUtil supportInfoUtil;

  private SupportInformation supportInformation;

  @Before
  @Override
  public void setup() {
    super.setup();
    supportInformation = new SupportInformation(versionService, dbData, systemInfo, supportInfoUtil);
  }

  @AfterClass
  public static void tearDown() throws IOException {
    cleanWorkDir(WORK_DIR);
  }

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
        supportInformation.aNewListOfSupportFiles().withJavaVersion().build().get(0);

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
        supportInformation.aNewListOfSupportFiles().withProductVersion().build().get(0);

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
        supportInformation.aNewListOfSupportFiles().withLicenseDetails().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(
        "{\"licenseInfo\":{\"productEdition\":\"Lifecycle\",\"fingerprint\":\"53274cced19\"}}");
  }

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
    when(dbData.getUser()).thenReturn(wrapEntry("users", users));
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedUsers), "users.json"));
    SupportFile supportFile =
        supportInformation.aNewListOfSupportFiles().withUsersDetails().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedUsers));
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
    when(dbData.getRole()).thenReturn(wrapEntry("roles", roles));
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedRoles), "roles.json"));
    SupportFile supportFile =
        supportInformation.aNewListOfSupportFiles().withRolesDetails().build().get(0);

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
    when(dbData.getMembershipMapping()).thenReturn(wrapEntry("membership_mappings", membershipMappings));
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedMembershipMappings), "membership_mappings.json"));
    SupportFile supportFile =
        supportInformation.aNewListOfSupportFiles().withMembershipMappings().build().get(0);

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
    when(dbData.getPolicy()).thenReturn(wrapEntry("policies", policies));
    when(supportInfoUtil.writeTextToFile(any(), any())).thenReturn(
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedPolicies), "policies.json"));
    SupportFile supportFile =
        supportInformation.aNewListOfSupportFiles().withPolicies().build().get(0);

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
        supportInformation.aNewListOfSupportFiles().withComponentsInQuarantine().build().get(0);

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
        writeFile(WORK_DIR, JsonUtils.writeUnformatted(expectedWaivers), "waivers.json"));
    SupportFile supportFile =
        supportInformation.aNewListOfSupportFiles().withWaivers().build().get(0);

    // Then
    assertThat(supportFile.file).exists();
    String fileContents = new String(Files.readAllBytes(supportFile.file.toPath()));
    assertThat(fileContents).isEqualTo(JsonUtils.writeUnformatted(expectedWaivers));
  }

  private Entry<String, SortedMap<String, Object>> wrapEntry(String entryName, SortedMap<String, Object> objectToPut) {
    return new AbstractMap.SimpleImmutableEntry<>(entryName, objectToPut);
  }

  private Entry<String, Object> wrapEntry(String entryName, Object objectToPut) {
    return new AbstractMap.SimpleImmutableEntry<>(entryName, objectToPut);
  }
}
