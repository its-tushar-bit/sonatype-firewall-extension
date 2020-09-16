/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.plexus.util.FileUtils;

/**
 * Evaluate policy by using a pregenerated report.zip
 */
class TestReportEvaluator
{
  final Application app;

  final URL locationOfTestReport;

  final String brainBaseUrl;

  final InsightWork workStorage;

  final String scanId;

  private boolean hasEvaluation = false;

  public TestReportEvaluator(Application app,
                             String scanId,
                             URL locationOfTestReport,
                             String brainBaseUrl,
                             InsightWork workStorage)
  {
    this.app = app;
    this.scanId = scanId;
    this.locationOfTestReport = locationOfTestReport;
    this.brainBaseUrl = brainBaseUrl;
    this.workStorage = workStorage;
  }

  public void evaluatePolicy() throws IOException {
    ScanHelper.createDummyScanFile(workStorage, app.getId(), scanId);
    addTestReport();
    evaluatePolicyForScanId();
  }

  public void reevaluatePolicy() throws IOException {
    // guard against programming error when writing tests
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
    HttpClient client = HttpClientBuilder.create().build();
    HttpPost post = new HttpPost(brainBaseUrl + "rest/policy/" + app.getPublicId() + "/evaluate?scanId=" + scanId);
    post.setEntity(new StringEntity(JsonUtils.format(new Stage(Stage.ID_BUILD)), ContentType.APPLICATION_JSON));
    // please don't change the admin password on me!
    post.setHeader("Authorization",
        "Basic " + Base64.getEncoder().encodeToString("admin:admin123".getBytes(StandardCharsets.UTF_8)));
    HttpResponse response = client.execute(post);
    // evaluation is done synchronously within the request, if the request is successful the eval is complete
    hasEvaluation = true;
    assert response.getStatusLine().getStatusCode() == 200;
  }
}
