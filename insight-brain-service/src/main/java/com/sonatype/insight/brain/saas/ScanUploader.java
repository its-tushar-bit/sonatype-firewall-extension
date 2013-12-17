/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
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

  private final SaasClient client;

  private final InsightWork work;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Inject
  public ScanUploader(final SaasClient client, final InsightWork work) {
    this.client = client;
    this.work = work;
  }

  protected ScanReceipt upload(HttpServletRequest request, String applicationPublicId, String path, String... params)
      throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    final File scanDir = work.getScanDir(appId);
    scanDir.mkdirs();

    final File scanFile = FileUtils.createTempFile("temp-", ".xml.gz", scanDir);

    final FileOutputStream os = new FileOutputStream(scanFile);
    try {
      IOUtil.copy(request.getInputStream(), os);
    }
    finally {
      IOUtil.close(os);
    }

    request.setAttribute(SaasClient.UPLOAD_FILE_ATTRIBUTE, scanFile);

    final ScanReceipt receipt = client.get(request, ScanReceipt.class, path, params);

    if (StringUtils.isNotBlank(receipt.getScanId())) {
      FileUtils.rename(scanFile, work.getScanFile(appId, receipt.getScanId()));
    }

    log.debug("Successfully uploaded scan id {}", receipt.getScanId());

    // SaaS knows nothing about where CLM Server stores reports, add this info to the receipt.
    receipt.setReportUrl(UserInterfaceLinksResource.getReportUrl(applicationPublicId, receipt.getScanId()));

    return receipt;
  }

  /**
   * Uploads an existing scan file to the SaaS server.
   * 
   * @since 1.7.1
   */
  public ScanReceipt upload(File scanFile, String applicationPublicId, String path)
      throws IOException
  {
    ScanReceipt receipt = client.put(ScanReceipt.class, path, scanFile);

    log.debug("Successfully uploaded scan id {}", receipt.getScanId());

    // SaaS knows nothing about where CLM Server stores reports, add this info to the receipt.
    receipt.setReportUrl(UserInterfaceLinksResource.getReportUrl(applicationPublicId, receipt.getScanId()));

    return receipt;
  }
}
