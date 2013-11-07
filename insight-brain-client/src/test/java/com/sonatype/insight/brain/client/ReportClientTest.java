/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.service.AbstractLicenseTest;

import org.junit.Assert;
import org.junit.Test;

public class ReportClientTest
    extends AbstractLicenseTest
{
  private static String applicationPublicId = "ReportClientTest_AppId";

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
}
