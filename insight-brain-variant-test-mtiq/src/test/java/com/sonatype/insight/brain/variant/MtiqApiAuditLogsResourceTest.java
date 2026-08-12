/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.logging.MultiTenantAuditLogAppenderFactory;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.organization.OrganizationResource;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * MTIQ variant conversion of {@code MtiqApiAuditLogsResourceTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationResourceTest}). No base class; an injected
 * {@link MtiqTestContext} supplies the reused multi-tenant server, a fresh per-test tenant, and REST/lookup
 * access.
 */
@MtiqTest
class MtiqApiAuditLogsResourceTest
{
  private static final String AUDIT_2024_02_07_CONTENT =
      "{\"timestamp\":\"2024-02-07T17:56:48.007-03:00\",\"username\":\"*SYSTEM\",\"domain\":\"server\","
          + "\"type\":\"start\",\"data\":{\"serverInstanceId\":\"e3a2d628-48fb-4be1-8b7e-861bf64b9224\","
          + "\"serverConfigurationFile\":\"/home/config.yml\",\"serverRelease\":\"173.0-SNAPSHOT\","
          + "\"serverBuild\":\"build-number\",\"processOwner\":\"obarra\"}}\n";

  private static final String AUDIT_2024_02_08_CONTENT =
      "{\"timestamp\":\"2024-02-08T17:56:48.007-03:00\",\"username\":\"*SYSTEM\",\"domain\":\"server\","
          + "\"type\":\"start\",\"data\":{\"serverInstanceId\":\"e3a2d628-48fb-4be1-8b7e-861bf64b9224\","
          + "\"serverConfigurationFile\":\"/home/config.yml\",\"serverRelease\":\"173.0-SNAPSHOT\","
          + "\"serverBuild\":\"build-number\",\"processOwner\":\"obarra\"}}\n";

  // Injected by MtiqServerExtension: the reused multi-tenant server + a fresh per-test tenant context.
  private MtiqTestContext ctx;

  @AfterEach
  void after() throws IOException {
    deleteAuditLogs(
        Paths.get(MultiTenantAuditLogAppenderFactory.getAuditLogFileName(Tenant.GLOBAL_TENANT.tenantSlug)).getParent());
    deleteAuditLogs(
        Paths.get(MultiTenantAuditLogAppenderFactory.getAuditLogFileName(ctx.getTestTenant().tenantSlug)).getParent());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.AUDIT_LOGS_RESOURCE_PATH);
  }

  @Test
  void testGetAuditLogs_FilteredByTimeWindow() throws Exception {
    // Create some audit logs for the global tenant - to verify that they are not included when we ask for the test
    // tenant audit logs.
    copyTestResource("globalTenant", Tenant.GLOBAL_TENANT.tenantSlug, "audit-2024-02-07.log.gz");
    copyTestResource("globalTenant", Tenant.GLOBAL_TENANT.tenantSlug, "audit-2024-02-08.log.gz");
    // Create some audit logs for the test tenant.
    String testTenantSlug = ctx.getTestTenant().tenantSlug;
    copyTestResource("testTenant", testTenantSlug, "audit-2024-02-07.log.gz");
    copyTestResource("testTenant", testTenantSlug, "audit-2024-02-08.log.gz");

    HttpResponse response = restRequest().auth(getUser())
        .query("startUtcDate", "2024-02-04")
        .query("endUtcDate", "2024-02-07")
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(response.getBodyText()).isEqualTo(AUDIT_2024_02_07_CONTENT);
  }

  @Test
  void testGetAuditLogs_IncludesTodayLog() throws Exception {
    // Create some audit logs for the global tenant - to verify that they are not included when we ask for the test
    // tenant audit logs.
    copyTestResource("globalTenant", Tenant.GLOBAL_TENANT.tenantSlug, "audit-2024-02-07.log.gz");
    copyTestResource("globalTenant", Tenant.GLOBAL_TENANT.tenantSlug, "audit-2024-02-08.log.gz");
    // Create some audit logs for the test tenant.
    String testTenantSlug = ctx.getTestTenant().tenantSlug;
    copyTestResource("testTenant", testTenantSlug, "audit-2024-02-07.log.gz");
    copyTestResource("testTenant", testTenantSlug, "audit-2024-02-08.log.gz");

    User user = getUser();
    // Trigger an audit log write to create the current audit log file.
    organizationRequest().auth(user).body(new Organization("testOrg")).post();

    String auditLogFileName = MultiTenantAuditLogAppenderFactory.getAuditLogFileName(testTenantSlug);
    File auditLogFile = new File(auditLogFileName);

    // Wait for logback to flush the audit log to disk.
    await().atMost(5, TimeUnit.SECONDS).until(auditLogFile::exists);
    String todayAuditLogContent = FileUtils.readFileToString(auditLogFile, StandardCharsets.UTF_8);

    HttpResponse response = restRequest().auth(user)
        .query("startUtcDate", "2024-02-04")
        .query("endUtcDate", LocalDate.now().toString())
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);

    String expectedContent = AUDIT_2024_02_07_CONTENT + AUDIT_2024_02_08_CONTENT + todayAuditLogContent;
    assertThat(response.getBodyText()).isEqualTo(expectedContent);
  }

  private void copyTestResource(String sourceDir, String tenantSlug, String filename) throws IOException {
    String filepath = getClass().getClassLoader()
        .getResource(getClass().getSimpleName() + "/" + sourceDir + "/" + filename)
        .getFile();
    Path tenantAuditLogDir = Paths.get(MultiTenantAuditLogAppenderFactory.getAuditLogFileName(tenantSlug)).getParent();
    Files.createDirectories(tenantAuditLogDir);
    Files.copy(new File(filepath).toPath(), Paths.get(tenantAuditLogDir.toString(), filename));
  }

  private void deleteAuditLogs(Path logDir) throws IOException {
    Files.list(logDir)
        .filter(
            file -> file.getFileName().toString().startsWith("audit") && file.getFileName().toString().endsWith(".gz"))
        .forEach(file -> {
          try {
            Files.delete(file);
          }
          catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
  }

  private User getUser() {
    User[] holder = new User[1];
    ctx.testAsTestTenant(test -> {
      User user = ctx.tempEntity().newUser();
      Role role = ctx.tempEntity().newRole(false /* global */, Permission.ACCESS_AUDIT_LOG);
      ctx.tempEntity().newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
      holder[0] = user;
    });
    return holder[0];
  }

  private HttpRequest organizationRequest() {
    return ctx.restRequest().path(OrganizationResource.RESOURCE_PATH);
  }
}
