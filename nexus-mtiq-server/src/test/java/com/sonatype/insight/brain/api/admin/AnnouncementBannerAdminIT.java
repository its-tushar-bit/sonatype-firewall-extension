/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.configuration.AnnouncementBannerResource;
import com.sonatype.insight.brain.logging.MultiTenantAuditLogAppenderFactory;
import com.sonatype.insight.brain.model.configuration.AnnouncementBanner;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_ANNOUNCEMENT_BANNER_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Full-stack integration test for the announcement banner admin API.
 */
public class AnnouncementBannerAdminIT
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void shouldUpdateBannerViaAdminEndpoint() throws Exception {
    AnnouncementBanner banner = enabledBanner("2026-05-26-us");

    HttpResponse put = callBannerAdminEndpoint("global").body(banner).put();
    assertResponseStatus(200, put);

    AnnouncementBanner persisted = put.getBody(AnnouncementBanner.class);
    assertThat(persisted.isEnabled()).isTrue();
    assertThat(persisted.getWindowId()).isEqualTo("2026-05-26-us");
    assertThat(persisted.getMessage()).isEqualTo(banner.getMessage());
    assertThat(persisted.getUpdatedAt()).isNotNull();
  }

  @Test
  public void shouldReadBannerViaAdminEndpoint() throws Exception {
    callBannerAdminEndpoint("global").body(enabledBanner("read-test")).put();

    HttpResponse get = callBannerAdminEndpoint("global").get();

    assertResponseStatus(200, get);
    AnnouncementBanner banner = get.getBody(AnnouncementBanner.class);
    assertThat(banner.getWindowId()).isEqualTo("read-test");
  }

  @Test
  public void shouldReject401_whenAdminEndpointCalledWithoutJwt() throws Exception {
    HttpResponse response = adminRequest()
        .path("api/")
        .path(ADMIN_ANNOUNCEMENT_BANNER_PATH)
        .parameter("global")
        .body(enabledBanner("no-auth"))
        .put();

    assertResponseStatus(401, response);
  }

  @Test
  public void shouldReject400_whenEnabledWithoutMessage() throws Exception {
    AnnouncementBanner bad = enabledBanner("bad");
    bad.setMessage(null);

    HttpResponse response = callBannerAdminEndpoint("global").body(bad).put();

    assertResponseStatus(400, response);
  }

  @Test
  public void shouldReject400_whenSeverityIsInvalid() throws Exception {
    AnnouncementBanner bad = enabledBanner("bad-severity");
    bad.setSeverity("danger");

    HttpResponse response = callBannerAdminEndpoint("global").body(bad).put();

    assertResponseStatus(400, response);
  }

  @Test
  public void shouldReject400_whenDisplayFromIsAfterDisplayUntil() throws Exception {
    AnnouncementBanner bad = enabledBanner("bad-window");
    bad.setDisplayFrom(OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    bad.setDisplayUntil(OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC));

    HttpResponse response = callBannerAdminEndpoint("global").body(bad).put();

    assertResponseStatus(400, response);
  }

  @Test
  public void shouldReject400_whenEnabledWithoutWindowId() throws Exception {
    AnnouncementBanner bad = enabledBanner(null);
    bad.setWindowId(null);

    HttpResponse response = callBannerAdminEndpoint("global").body(bad).put();

    assertResponseStatus(400, response);
  }

  @Test
  public void shouldReject400_whenTenantSlugIsNotGlobal() throws Exception {
    // Any slug other than 'global' must be rejected.
    String tenantSlug = getTestTenant().tenantSlug;

    HttpResponse putResponse = callBannerAdminEndpoint(tenantSlug).body(enabledBanner("should-not-be-accepted")).put();
    assertResponseStatus(400, putResponse);

    HttpResponse getResponse = callBannerAdminEndpoint(tenantSlug).get();
    assertResponseStatus(400, getResponse);
  }

  @Test
  public void shouldReject400_whenPutBodyIsMissing() throws Exception {
    HttpResponse response = callBannerAdminEndpoint("global").put();

    assertResponseStatus(400, response);
  }

  @Test
  public void appPortShouldNotExposeWritePath() throws Exception {
    HttpResponse response = restRequest()
        .path(ADMIN_ANNOUNCEMENT_BANNER_PATH)
        .parameter("global")
        .body(enabledBanner("sneak"))
        .put();

    // Admin resource is registered only on the admin connector; on the app port Jersey returns 404.
    assertResponseStatus(404, response);
  }

  @Test
  public void appPortShouldExposeReadEndpoint() throws Exception {
    callBannerAdminEndpoint("global").body(enabledBanner("cross-tenant-read")).put();

    HttpResponse response = restRequest()
        .path(AnnouncementBannerResource.RESOURCE_PATH)
        .path("fetch")
        .get();

    assertResponseStatus(200, response);
    Map<?, ?> body = response.getBody(Map.class);
    assertThat(body.get("windowId")).isEqualTo("cross-tenant-read");
    assertThat(body.get("enabled")).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void shouldWriteAuditLogEntry_whenPutSucceeds() throws Exception {
    // @Audited(CONFIGURE_ANNOUNCEMENT_BANNER) must land in the global-tenant audit log on a successful PUT.
    AnnouncementBanner banner = enabledBanner("audit-smoke");

    HttpResponse put = callBannerAdminEndpoint("global").body(banner).put();
    assertResponseStatus(200, put);

    String auditLogFileName = MultiTenantAuditLogAppenderFactory.getAuditLogFileName(Tenant.GLOBAL_TENANT.tenantSlug);
    File auditLogFile = new File(auditLogFileName);
    await().atMost(5, TimeUnit.SECONDS).until(auditLogFile::exists);

    String auditLog = FileUtils.readFileToString(auditLogFile, StandardCharsets.UTF_8);
    assertThat(auditLog).contains("CONFIGURE_ANNOUNCEMENT_BANNER");
  }

  @Test
  public void appPortReadEndpointRejectsAnonymousRequest() throws Exception {
    // @UnlicensedPath bypasses the license check but not authentication; Shiro must still 401 anonymous callers.
    HttpResponse response = restRequest()
        .path(AnnouncementBannerResource.RESOURCE_PATH)
        .path("fetch")
        .anon()
        .get();

    assertResponseStatus(401, response);
  }

  private AnnouncementBanner enabledBanner(String windowId) {
    AnnouncementBanner banner = new AnnouncementBanner();
    banner.setEnabled(true);
    banner.setWindowId(windowId);
    banner.setSeverity("info");
    banner.setMessage("Scheduled maintenance: May 26, 6-10 PM EDT.");
    banner.setDisplayFrom(OffsetDateTime.of(2026, 5, 20, 0, 0, 0, 0, ZoneOffset.UTC));
    banner.setDisplayUntil(OffsetDateTime.of(2026, 5, 26, 23, 0, 0, 0, ZoneOffset.UTC));
    return banner;
  }

  private HttpRequest callBannerAdminEndpoint(String tenantSlug) {
    return adminRestRequest(ADMIN_ANNOUNCEMENT_BANNER_PATH).parameter(tenantSlug);
  }
}
