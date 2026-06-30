/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.rules.TemporaryFolder;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test utility for fixture-based policy evaluation in Playwright setup.
 * <p>
 * This helper does <b>not</b> emulate the full scanner submission flow
 * ({@code scan.xml} upload -> report generation). Instead, it seeds a pre-generated
 * {@code report.zip} fixture for a known scan id, then triggers policy evaluation for that
 * existing scan so tests get deterministic report data quickly.
 * <p>
 * For end-to-end scan submission behavior, use the application evaluation resource workflow.
 */
public class TestReportEvaluator
{
  final Application app;

  final URL locationOfTestReport;

  final String brainBaseUrl;

  final InsightWork workStorage;

  final String scanId;

  final String stageId;

  private boolean hasEvaluation = false;

  public TestReportEvaluator(
      Application app,
      String scanId,
      URL locationOfTestReport,
      String brainBaseUrl,
      InsightWork workStorage,
      String stageId)
  {
    this.app = app;
    this.scanId = scanId;
    this.locationOfTestReport = locationOfTestReport;
    this.brainBaseUrl = brainBaseUrl;
    this.workStorage = workStorage;
    this.stageId = stageId;
  }

  public TestReportEvaluator(
      Application app,
      String scanId,
      URL locationOfTestReport,
      String brainBaseUrl,
      InsightWork workStorage)
  {
    this(app, scanId, locationOfTestReport, brainBaseUrl, workStorage, Stage.ID_BUILD);
  }

  public void evaluatePolicy() throws IOException {
    ScanHelper.createDummyScanFile(workStorage, app.getId(), scanId);
    addTestReport();
    evaluatePolicyForScanId();
  }

  /** Zip the canned report and submit it for evaluation against {@code app} at {@code stageId}. */
  public static void seedEvaluation(
      Application app,
      String scanId,
      String reportClasspathDir,
      TemporaryFolder tempDir,
      String brainBaseUrl,
      InsightWork workStorage,
      String stageId) throws IOException
  {
    URL zippedReport = ReportHelper.zipReport(reportClasspathDir, tempDir);
    new TestReportEvaluator(app, scanId, zippedReport, brainBaseUrl, workStorage, stageId)
        .evaluatePolicy();
  }

  public void reevaluatePolicy() throws IOException {
    if (hasEvaluation) {
      evaluatePolicyForScanId();
    }
    else {
      throw new IllegalStateException("No previous evaluation to re-evaluate");
    }
  }

  private void addTestReport() throws IOException {
    File reportZipLocation = workStorage.getReportFile(app.getId(), scanId);
    FileUtils.copyURLToFile(locationOfTestReport, reportZipLocation);
  }

  private void evaluatePolicyForScanId() throws IOException {
    HttpPost post = new HttpPost(brainBaseUrl + "rest/policy/" + app.getPublicId() + "/evaluate?scanId=" + scanId);
    post.setEntity(new StringEntity(JsonUtils.format(new Stage(stageId)), ContentType.APPLICATION_JSON));
    post.setHeader("Authorization", TestCredentials.basicAuthHeader());
    try (CloseableHttpClient client = HttpClientBuilder.create().build();
        CloseableHttpResponse response = client.execute(post))
    {
      int status = response.getStatusLine().getStatusCode();
      EntityUtils.consumeQuietly(response.getEntity());
      assertThat(status).isEqualTo(200);
      hasEvaluation = true;
    }
  }

  public void evaluatePolicyForScanIdWithScanTriggerType(ScanTriggerType scanTriggerType) throws IOException {
    ScanHelper.createDummyScanFile(workStorage, app.getId(), scanId);
    addTestReport();

    HttpPost post = new HttpPost(
        brainBaseUrl + "rest/policy/" + app.getPublicId() + "/evaluate?scanId=" + scanId
            + "&scanTriggerType=" + scanTriggerType);
    post.setEntity(new StringEntity(JsonUtils.format(new Stage(stageId)), ContentType.APPLICATION_JSON));
    post.setHeader("Authorization", TestCredentials.basicAuthHeader());
    try (CloseableHttpClient client = HttpClientBuilder.create().build();
        CloseableHttpResponse response = client.execute(post))
    {
      int status = response.getStatusLine().getStatusCode();
      EntityUtils.consumeQuietly(response.getEntity());
      assertThat(status).isEqualTo(200);
      hasEvaluation = true;
    }
  }
}
