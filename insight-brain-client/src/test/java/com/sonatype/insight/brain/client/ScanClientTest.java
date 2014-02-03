/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.service.AbstractLicenseTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ScanClientTest
    extends AbstractLicenseTest
{
  private static final String APP_ID = "ScanClientTest_AppId";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Before
  public void createApplication() {
    tempEntity.newApplicationWithParent(APP_ID, "test");
  }

  @Test
  public void testUploadCiScan_AllGood() throws Exception {
    Configuration config = brain.getClientConfiguration();
    ScanReceipt receipt = new ScanClient(config, APP_ID).uploadCiScan(tmpDir.newFile("scan.xml.gz"));
    assertEquals("SCAN-ID", receipt.getScanId());
    assertEquals("ui/links/application/ScanClientTest_AppId/report/SCAN-ID", receipt.getReportUrl());
    assertEquals("ui/links/application/ScanClientTest_AppId/report/SCAN-ID/pdf", receipt.getPdfUrl());
  }

  @Test
  public void testUploaCiScan_InvalidAppId() throws Exception {
    Configuration config = brain.getClientConfiguration();
    try {
      new ScanClient(config, "invalid-id").uploadCiScan(tmpDir.newFile("scan.xml.gz"));
      fail("Upload should have failed due to invalid app ID");
    }
    catch (HttpResponseException e) {
      assertEquals(404, e.getStatusCode());
      assertEquals("Could not find an application with public id invalid-id.", e.getMessage());
    }
  }

  @Test
  public void testUploadRepoManScan_AllGood() throws Exception {
    Configuration config = brain.getClientConfiguration();
    ScanReceipt receipt = new ScanClient(config, APP_ID).uploadRepoManScan(tmpDir.newFile("scan.xml.gz"));
    assertEquals("SCAN-ID", receipt.getScanId());
    assertEquals("ui/links/application/ScanClientTest_AppId/report/SCAN-ID", receipt.getReportUrl());
    assertEquals("ui/links/application/ScanClientTest_AppId/report/SCAN-ID/pdf", receipt.getPdfUrl());
  }

  @Test
  public void testUploadRepoManScan_InvalidAppId() throws Exception {
    Configuration config = brain.getClientConfiguration();
    try {
      new ScanClient(config, "invalid-id").uploadRepoManScan(tmpDir.newFile("scan.xml.gz"));
      fail("Upload should have failed due to invalid app ID");
    }
    catch (HttpResponseException e) {
      assertEquals(404, e.getStatusCode());
      assertEquals("Could not find an application with public id invalid-id.", e.getMessage());
    }
  }

}
