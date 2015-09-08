/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ScanUploader
{
  private static final Logger log = LoggerFactory.getLogger(ScanUploader.class);

  private static final String HDS_PATH = "rest/application/analysis";

  private final HdsClient client;

  private final InsightWork work;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Inject
  public ScanUploader(final HdsClient client, final InsightWork work) {
    this.client = client;
    this.work = work;
  }

  protected ScanReceipt upload(HttpServletRequest request, String applicationPublicId, String... params)
      throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    final File scanDir = work.getScanDir(appId);
    scanDir.mkdirs();

    final File scanFile = FileUtils.createTempFile("temp-", ".xml.gz", scanDir);

    try (ServletInputStream is = request.getInputStream(); FileOutputStream os = new FileOutputStream(scanFile)) {
      IOUtil.copy(is, os);
    }

    request.setAttribute(HdsClient.UPLOAD_FILE_ATTRIBUTE, scanFile);

    final ScanReceipt receipt = client.get(request, ScanReceipt.class, HDS_PATH, params);

    if (StringUtils.isNotBlank(receipt.getScanId())) {
      FileUtils.rename(scanFile, work.getScanFile(appId, receipt.getScanId()));
    }

    augmentScanReceipt(applicationPublicId, receipt);

    return receipt;
  }

  /**
   * Uploads an existing scan file to the HDS server.
   *
   * @since 1.8
   */
  public ScanReceipt upload(File scanFile, String applicationPublicId)
      throws IOException
  {
    ScanReceipt receipt = client.put(ScanReceipt.class, HDS_PATH, scanFile);

    augmentScanReceipt(applicationPublicId, receipt);

    return receipt;
  }

  void augmentScanReceipt(String applicationPublicId, ScanReceipt receipt) {
    log.debug("Successfully uploaded scan id {}", receipt.getScanId());

    // HDS knows nothing about where CLM Server stores reports, add this info to the receipt.
    receipt.setReportUrl(UserInterfaceLinksResource.getReportUrl(applicationPublicId, receipt.getScanId()));
    receipt.setPdfUrl(UserInterfaceLinksResource.getPdfUrl(applicationPublicId, receipt.getScanId()));
    receipt.setDataUrl(ApiReportDataResourceV2.getDataUrl(applicationPublicId, receipt.getScanId()));
  }
}
