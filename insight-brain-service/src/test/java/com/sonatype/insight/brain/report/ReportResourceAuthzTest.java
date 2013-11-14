/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationLog;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

public class ReportResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testAugmentData() throws Exception {
    grantWritePermission(app.getId());
    String json = "{}";

    String url = getRestUrl(ReportResource.SERVICE_PATH + "/augmentData/{path}", app.getPublicId(), "scanId",
        "test.json");
    testAuthzPost(url, json);
  }

  @Test
  public void testAuditLog() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(ReportResource.SERVICE_PATH + "/auditLog/{path}", app.getPublicId(), "scanId",
        "security.json");
    testAuthzGet(url);
  }

  @Test
  public void testBrowseReport() throws Exception {
    String scanId = "scanId";
    File saasReportFile = getReportResponseFile(getLicenseFingerprint(), scanId);
    FileUtils.copyURLToFile(getClass().getResource("/ReportResourceTest/report.zip"), saasReportFile);

    grantReadPermission(app.getId());

    String url = getRestUrl(ReportResource.SERVICE_PATH + "/browseReport/{path}", app.getPublicId(), scanId,
        "index.html");
    testAuthzGet(url);
  }

  @Test
  public void testPrintReport() throws Exception {
    String scanId = "scanId";
    File saasReportFile = getReportResponseFile(getLicenseFingerprint(), scanId);
    FileUtils.copyURLToFile(getClass().getResource("/ReportResourceTest/report.zip"), saasReportFile);

    grantReadPermission(app.getId());

    String url = getRestUrl(ReportResource.SERVICE_PATH + "/printReport", app.getPublicId(), scanId);
    testAuthzGet(url);
  }

  @Test
  public void testReevaluatePolicy() throws Exception {
    String scanId = "scanId";
    File saasReportFile = getReportResponseFile(getLicenseFingerprint(), scanId);
    FileUtils.copyURLToFile(getClass().getResource("/ReportResourceTest/report.zip"), saasReportFile);
    PolicyEvaluationLog evalLog = new PolicyEvaluationLog(brain.getAuditDir(app.getId()));
    evalLog.add(new Stage(Stage.ID_BUILD), scanId, "nobody", "127.0.0.1");

    grantReadPermission(app.getId());

    String url = getRestUrl(ReportResource.SERVICE_PATH + "/reevaluatePolicy", app.getPublicId(), scanId);
    testAuthzGet(url);
  }
}
