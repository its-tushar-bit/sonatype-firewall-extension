/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.net.URL;
import java.util.zip.ZipFile;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.service.AbstractLicenseTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.client.HttpResponseException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ReportClientTest
    extends AbstractLicenseTest
{
  private static final String applicationPublicId = "ReportClientTest_AppId";

  private static final String scanId = "ReportResourceClient_ScanId";

  private static final String licenseFingerprint = "ReportResourceClient_LicenseFingerprint";

  private static final String reportFileName = "report.zip";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testScanIdNull() {
    try {
      new ReportClient(brain.getClientConfiguration(), applicationPublicId, null /* scanId */);
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testScanIdEmpty() {
    try {
      new ReportClient(brain.getClientConfiguration(), applicationPublicId, " " /* scanId */);
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testLinkToReport() throws Exception {
    String appId = "app id";
    String scanId = "scan id";
    ReportClient reportClient = new ReportClient(brain.getClientConfiguration(), appId, scanId);
    UriBuilder uriBuilder = UriBuilder.fromPath(brain.getClientConfiguration().getServerUrl());
    uriBuilder.path(UserInterfaceLinksResource.SERVICE_PATH).path(UserInterfaceLinksResource.REPORT_PATH);
    Assert.assertEquals(reportClient.linkToReport(), uriBuilder.build(appId, scanId).toString());
  }

  @Test
  public void testDownloadBundle() throws Exception {
    tempEntity.newApplicationWithParent(applicationPublicId).getId();
    setLicenseFingerprint(licenseFingerprint);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    final URL testReportResultUrl = getClass().getResource(reportFileName);
    FileUtils.copyURLToFile(testReportResultUrl, saasReportFile);

    File retrievedFile = temporaryFolder.newFile();

    Configuration config = brain.getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    ReportClient client = new ReportClient(config, applicationPublicId, scanId);
    client.downloadBundle(retrievedFile);

    assertThat(retrievedFile.exists(), is(true));
    assertThat(retrievedFile.length(), is(greaterThan(0L)));
    // Verify that the file is in ZIP format
    new ZipFile(retrievedFile).close();
  }

  @Test
  public void testUnauthorizedError() throws Exception {
    tempEntity.newApplicationWithParent(applicationPublicId).getId();
    setLicenseFingerprint(licenseFingerprint);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    final URL testReportResultUrl = getClass().getResource(reportFileName);
    FileUtils.copyURLToFile(testReportResultUrl, saasReportFile);

    File retrievedFile = temporaryFolder.newFile();

    Configuration config = brain.getClientConfiguration();
    ReportClient client = new ReportClient(config, applicationPublicId, scanId);
    try {
      client.downloadBundle(retrievedFile);
      fail("Expected an HttpResponseException for Unauthorized");
    }
    catch (HttpResponseException e) {
      assertThat(e.getMessage(), is("Unauthorized"));
    }
  }
}
