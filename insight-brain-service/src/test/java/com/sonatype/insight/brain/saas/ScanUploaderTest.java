/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ScanUploaderTest
    extends AbstractComponentTest
{
  @Inject
  private ScanUploader scanUploader;

  @Test
  public void testAugmentScanReceipt() {
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan id");
    scanUploader.augmentScanReceipt("app id", receipt);
    assertThat(receipt.getReportUrl(), is("ui/links/application/app%20id/report/scan%20id"));
    assertThat(receipt.getPdfUrl(), is("ui/links/application/app%20id/report/scan%20id/pdf"));
    assertThat(receipt.getDataUrl(), is("api/v1/applications/app%20id/reports/scan%20id"));
  }
}
