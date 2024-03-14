/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.logging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.OrganizationResource;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationResourceTest;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantAuditLogAppenderFactoryTest
    extends AbstractMultiTenantBaseIntegrationResourceTest
{
  @Test
  public void testAuditLogsAreSeparatedByTenant() throws Exception {
    String expectedSystemStartLogText = "\"username\":\"*SYSTEM\",\"domain\":\"server\",\"type\":\"start\"";
    assertLogContains(Tenant.GLOBAL_TENANT.tenantSlug, expectedSystemStartLogText);
    assertLogDoesNotContain(getTestTenant().tenantSlug, expectedSystemStartLogText);

    String expectedTenantCreateLogText =
        "\"username\":\"test@test.com\",\"domain\":\"mtiq.tenant\",\"type\":\"create\"";
    // The audit log for the global tenant may contain this text or not from tests that were run before this test,
    // so we cannot assert that it does not contain it.
    assertLogContains(getTestTenant().tenantSlug, expectedTenantCreateLogText);

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
  public void testGetAuditLogFiles_NoFilesForTheRange() throws Exception {
    List<File> auditLogFiles = MultiTenantAuditLogAppenderFactory.getAuditLogFiles(LocalDate.of(2024, 2, 4),
        LocalDate.of(2024, 2, 4));
    assertThat(auditLogFiles).isEmpty();
  }

  @Test
  public void testGetAuditLogFiles_WhenTheRangeIsToday() throws Exception {
    List<File> auditLogFiles = MultiTenantAuditLogAppenderFactory.getAuditLogFiles(LocalDate.now(), LocalDate.now());

    assertThat(auditLogFiles).hasSize(1);
    assertThat(auditLogFiles.get(0).getAbsolutePath().replace('\\', '/'))
        .endsWith("target/test-audit-logs/" + getTestTenant().tenantSlug + "/log/audit.log");
  }

  private HttpRequest organizationRequest() {
    return restRequest().path(OrganizationResource.RESOURCE_PATH);
  }

  private void assertLogContains(String tenantSlug, String value) throws IOException {
    String tenantAuditLogFileName = MultiTenantAuditLogAppenderFactory.getAuditLogFileName(tenantSlug);
    assertThat(tenantAuditLogFileName).contains(tenantSlug);
    String tenantAuditLogContents =
        FileUtils.readFileToString(new File(tenantAuditLogFileName), StandardCharsets.UTF_8);
    assertThat(tenantAuditLogContents).contains(value);
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
}
