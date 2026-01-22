/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.telemetry.PendoCache;
import com.sonatype.insight.brain.telemetry.PendoService;
import com.sonatype.insight.brain.telemetry.PendoService.PendoConfig;
import com.sonatype.insight.brain.telemetry.UserTelemetryResource;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class RepoManResourceTest
    extends AbstractResourceTest
{
  private HttpRequest scanRequest(String appId) {
    return super.restRequest().path(RepoManResource.RESOURCE_PATH, RepoManResource.SCAN_PATH).parameter(appId);
  }

  @Test
  public void testUploadScan() throws Exception {
    final String applicationPublicId = "RepoManResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("f75365d9d93b4f1ea2dd8457a25dc44d");
    scanReceipt.setTimeToReport(30L);
    mockScanReceipt(scanReceipt);

    String testClientUserAgent = "testClientUserAgent";
    HttpRequest request = scanRequest(applicationPublicId);
    request.header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent);
    final HttpResponse response = request.put();

    assertResponseStatus(200, response);

    ScanReceipt receipt = response.getBody(ScanReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getScanId()).isEqualTo(scanReceipt.getScanId());
    assertThat(receipt.getTimeToReport()).isEqualTo(scanReceipt.getTimeToReport());
    assertThat(receipt.getReportUrl())
        .isEqualTo("ui/links/application/RepoManResourceTest_AppId/report/f75365d9d93b4f1ea2dd8457a25dc44d");
    assertThat(receipt.getPdfUrl())
        .isEqualTo("ui/links/application/RepoManResourceTest_AppId/report/f75365d9d93b4f1ea2dd8457a25dc44d/pdf");

    assertThat(getHdsServer().getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH)
        .get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testUploadScan_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = scanRequest("unlicensedappid").put();
    assertResponseStatus(402, response);
  }

  @Test
  public void testUploadScan_FeatureUnlicensed() throws Exception {
    setMissingFeature(LicensedFeature.RM_STAGING_INTEGRATION);

    HttpResponse response = scanRequest("unlicensedappid").put();
    assertResponseStatus(402, response);
  }

  @Test
  public void testProxyTelemetry_Config() throws Exception {
    HttpResponse response = restRequest().path(RepoManResource.RESOURCE_PATH)
        .path(UserTelemetryResource.RESOURCE_SUBPATH).path(UserTelemetryResource.CONFIG_PATH).get();

    assertResponseStatus(200, response);

    PendoConfig config = response.getBody(PendoConfig.class);
    assertThat(config.visitor).isNotNull();
    assertThat(config.account).isNotNull();
  }

  @Test
  public void testProxyTelemetry_JavaScript() throws Exception {
    getCLMServer().getInstance(PendoCache.class).invalidateAll();
    getHdsServer().respondWith("some javascript").atUri("user-telemetry.js");

    HttpResponse response = restRequest().path(RepoManResource.RESOURCE_PATH)
        .path(UserTelemetryResource.RESOURCE_SUBPATH).path(UserTelemetryResource.JAVASCRIPT_PATH).get();

    assertResponseStatus(200, response);

    String js = response.getBodyText();
    assertThat(js).isEqualTo("some javascript");
  }

  @Test
  public void testProxyTelemetry_EventsGet() throws Exception {
    getHdsServer().respondWith("some response").atUri(PendoService.HDS_TELEMETRY_PATH + "/foo/bar");

    String url = UriBuilder.fromPath(RepoManResource.RESOURCE_PATH)
        .path(UserTelemetryResource.RESOURCE_SUBPATH).path(UserTelemetryResource.EVENTS_PATH)
        .build(new String[]{"foo/bar"}, false /* encodeSlashInPath */)
        .toString();

    HttpResponse response = restRequest().path(url).get();
    assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("some response");
  }

  @Test
  public void testProxyTelemetry_EventsPost() throws Exception {
    getHdsServer().respondWith("some response").atUri(PendoService.HDS_TELEMETRY_PATH + "/foo/bar");

    String url = UriBuilder.fromPath(RepoManResource.RESOURCE_PATH)
        .path(UserTelemetryResource.RESOURCE_SUBPATH).path(UserTelemetryResource.EVENTS_PATH)
        .build(new String[]{"foo/bar"}, false /* encodeSlashInPath */)
        .toString();

    HttpResponse response = restRequest().path(url).post();
    assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("some response");
  }
}
