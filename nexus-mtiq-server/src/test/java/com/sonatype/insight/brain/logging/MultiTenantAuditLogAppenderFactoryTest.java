/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.logging;

import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.OrganizationResource;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationResourceTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import org.junit.experimental.categories.Category;

/*
 * WARNING:
 * These tests run with an MTIQ test server that is not dedicated to these tests.
 * This means the audit logs may contain stuff logged by previous tests,
 * so don't expect the audit logs to contain only the lines logged from the tests in this class.
 */
@Category(SlowTest.class)
public class MultiTenantAuditLogAppenderFactoryTest
    extends AbstractMultiTenantBaseIntegrationResourceTest
{
  @Before
  public void setUp() throws Exception {
    // Trigger an audit log write to ensure the audit log directory exists for the test tenant.
    // Wait for logback to flush the logs to disk before proceeding.
    organizationRequest().body(new Organization("setupOrg")).post();
    String auditLogFileName = MultiTenantAuditLogAppenderFactory.getAuditLogFileName(getTestTenant().tenantSlug);
    await().atMost(5, TimeUnit.SECONDS).until(() -> new File(auditLogFileName).exists());
  }

  @Test
  public void testAuditLogsAreSeparatedByTenant() throws Exception {
    String expectedSystemStartLogText = "\"username\":\"*SYSTEM\",\"domain\":\"server\",\"type\":\"start\"";
    assertLogContains(Tenant.GLOBAL_TENANT.tenantSlug, expectedSystemStartLogText);
    assertLogDoesNotContain(getTestTenant().tenantSlug, expectedSystemStartLogText);

    // Create an organization for the current tenant and verify that the corresponding audit log record is saved in the
    // tenant specific audit log.
    Organization organization = new Organization("orgName");
    organization = organizationRequest().body(organization).post().getBody(Organization.class);

    String expectedOrgCreateLogText = "\"username\":\"admin\",\"domain\":\"governance.organization\","
        + "\"type\":\"create\",\"data\":{\"organizationId\":\"" + organization.getId()
        + "\",\"organizationName\":\"orgName\"}";
    assertLogDoesNotContain(Tenant.GLOBAL_TENANT.tenantSlug, expectedOrgCreateLogText);
    assertLogContains(getTestTenant().tenantSlug, expectedOrgCreateLogText);
  }

  @Test
  public void testAuditLogLinesAreAllJson() throws Exception {
    // Create an organization for the current tenant to ensure we have some content in the audit log for the tenant.
    organizationRequest().body(new Organization("testOrgName")).post();

    assertLogLinesAreJson(Tenant.GLOBAL_TENANT.tenantSlug);
    assertLogLinesAreJson(getTestTenant().tenantSlug);
  }

  @Test
  public void testGetAuditLogFiles_NoFilesForTheRange() throws Exception {
    List<File> auditLogFiles = MultiTenantAuditLogAppenderFactory.getAuditLogFiles(LocalDate.of(2024, 2, 4),
        LocalDate.of(2024, 2, 4));
    assertThat(auditLogFiles).isEmpty();
  }

  @Test
  public void testGetAuditLogFiles_WhenTheRangeIsToday() throws Exception {
    List<File> auditLogFiles = MultiTenantAuditLogAppenderFactory.getAuditLogFiles(LocalDate.now(), LocalDate.now());

    assertThat(auditLogFiles).hasSize(1);
    // The path for audit logs is configured in src/test/resources/config-test.yml
    assertThat(auditLogFiles.get(0).getAbsolutePath().replace('\\', '/'))
        .endsWith("target/test-audit-logs/" + getTestTenant().tenantSlug + "/log/audit.log");
  }

  private HttpRequest organizationRequest() {
    return restRequest().path(OrganizationResource.RESOURCE_PATH);
  }

  private void assertLogContains(String tenantSlug, String value) {
    String tenantAuditLogFileName = MultiTenantAuditLogAppenderFactory.getAuditLogFileName(tenantSlug);
    assertThat(tenantAuditLogFileName).contains(tenantSlug);
    // Need to await for logback to flush the logs to disk.
    await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> assertThat(FileUtils.readFileToString(new File(tenantAuditLogFileName), StandardCharsets.UTF_8))
                .contains(value));
  }

  private void assertLogDoesNotContain(String tenantSlug, String value) throws IOException {
    String tenantAuditLogFileName = MultiTenantAuditLogAppenderFactory.getAuditLogFileName(tenantSlug);
    assertThat(tenantAuditLogFileName).contains(tenantSlug);
    File tenantAuditLogFile = new File(tenantAuditLogFileName);
    if (!tenantAuditLogFile.exists()) {
      return;
    }
    String tenantAuditLogContents = FileUtils.readFileToString(tenantAuditLogFile, StandardCharsets.UTF_8);
    assertThat(tenantAuditLogContents).doesNotContain(value);
  }

  private void assertLogLinesAreJson(String tenantSlug) throws IOException {
    String tenantAuditLogFileName = MultiTenantAuditLogAppenderFactory.getAuditLogFileName(tenantSlug);
    assertThat(tenantAuditLogFileName).contains(tenantSlug);
    Path tenantAuditLogFile = Paths.get(tenantAuditLogFileName);
    List<String> tenantAuditLogLines = Files.readAllLines(tenantAuditLogFile, StandardCharsets.UTF_8);
    assertThat(tenantAuditLogLines).isNotEmpty();
    for (String line : tenantAuditLogLines) {
      try {
        JsonUtils.parse(line);
      }
      catch (JsonParseException e) {
        throw new RuntimeException("Audit log line is not json: '" + line + "'. Error: " + e.getMessage(), e);
      }
    }
  }
}
