/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationLog;
import com.sonatype.insight.brain.service.TestInsightBrainService;

import org.codehaus.plexus.util.FileUtils;

class TestHelper
{
  private final TemporaryEntity tempEntity;

  private final TestInsightBrainService brain;

  public TestHelper(TemporaryEntity tempEntity, TestInsightBrainService brain) {
    this.tempEntity = tempEntity;
    this.brain = brain;
  }

  public Application createAppWithScan(String appPublicId, String stageId) throws Exception {
    Application app = tempEntity.newApplication(appPublicId.toUpperCase(Locale.ENGLISH), appPublicId, null);
    createScanForApp(app.getId(), stageId, app.getPublicId());
    return app;
  }

  public void createScanForApp(String appId, String stageId, String resPath) throws Exception {
    String scanId = UUID.randomUUID().toString().replace("-", "");
    FileUtils.copyURLToFile(getClass().getResource("/SearchResourceTest/" + resPath + "/bom.json"),
        getReportCacheEntry(appId, scanId, "bom.json"));
    FileUtils.copyURLToFile(getClass().getResource("/SearchResourceTest/" + resPath + "/policyalerts.json"),
        getReportCacheEntry(appId, scanId, "policyalerts.json"));
    createReport(appId, scanId);
    PolicyEvaluationLog log = new PolicyEvaluationLog(brain.getAuditDir(appId));
    log.add(new PolicyEvaluation(new Stage(stageId), scanId), "nobody", null);
  }

  private File getReportCacheEntry(String appId, String scanId, String name) {
    return new File(new File(brain.getReportDir(appId, scanId), "report.cache"), name);
  }

  private void createReport(String appId, String scanId) throws Exception {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(brain.getReportDir(appId, scanId),
        "report.zip")));
    try {
      zos.putNextEntry(new ZipEntry("index.html"));
    }
    finally {
      zos.close();
    }
  }
}
