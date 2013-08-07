/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractLicenseTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

import org.apache.http.client.HttpResponseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ScanClientTest
    extends AbstractLicenseTest
{
  private static final String APP_ID = "ScanClientTest_AppId";

  private static Application application;

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @AfterClass
  public static void afterClass() {
    if (application != null) {
      new ApplicationDAO().delete(application);
    }
    DataSourceFactory.clear_ForTestsOnly();
  }

  @BeforeClass
  public static void createApplication() {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    application = new Application();
    application.setName("test");
    application.setPublicId(APP_ID);
    applicationDAO.insert(application);
  }

  @Test
  public void testUploadCiScan_AllGood() throws Exception {
    Configuration config = brain.getClientConfiguration();
    ScanReceipt receipt = new ScanClient(config, APP_ID).uploadCiScan(tmpDir.newFile("scan.xml.gz"));
    assertEquals("SCAN-ID", receipt.getScanId());
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
    assertEquals("rest/report/ScanClientTest_AppId/SCAN-ID/embedReport/", receipt.getReportUrl());
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
