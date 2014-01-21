/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.utils;

import com.sonatype.clm.dto.model.policy.Stage
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.service.InsightWork

import com.sun.jersey.core.util.Base64
import com.yammer.dropwizard.testing.JsonHelpers
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.codehaus.plexus.util.FileUtils

/**
 * Evaluate policy by using a pregenerated report.zip
 */
class TestReportEvaluator
{
  final Application app
  final URL locationOfTestReport
  final String brainBaseUrl
  final InsightWork workStorage
  String scanId

  public TestReportEvaluator(Application app, URL locationOfTestReport, String brainBaseUrl, InsightWork workStorage) {
    this.app = app
    this.locationOfTestReport = locationOfTestReport
    this.brainBaseUrl = brainBaseUrl
    this.workStorage = workStorage;
  }

  public String evaluatePolicy() {
    initScanId()
    addTestReport()
    evaluatePolicyForScanId()

    return scanId
  }

  public String reevaluatePolicy() {
    // guard against programming error when writing tests
    if (hasEvaluation()) {
      evaluatePolicyForScanId()
      return scanId
    }
    else {
      throw new IllegalStateException("No previous evaluation to re-evaluate")
    }
  }

  private initScanId() {
    scanId = 'scan-' + UUID.randomUUID().toString()
  }

  private addTestReport() {
    File reportZipLocation = workStorage.getReportFile(app.id, scanId)
    FileUtils.copyURLToFile(locationOfTestReport, reportZipLocation)
  }

  private evaluatePolicyForScanId() {
    HttpClient client = HttpClientBuilder.create().build()
    HttpPost post = new HttpPost(brainBaseUrl + 'rest/policy/' + app.publicId +'/evaluate?scanId=' + scanId)
    post.setEntity(new StringEntity(JsonHelpers.asJson(new Stage(Stage.ID_BUILD)), ContentType.APPLICATION_JSON))
    // please don't change the admin password on me!
    post.setHeader("Authorization", "Basic " + Base64.encode("admin:admin123"))
    HttpResponse response = client.execute(post)
    // evaluation is done synchronously within the request, if the request is successful the eval is complete
    assert response.statusLine.statusCode == 200
  }

  private boolean hasEvaluation() {
    return scanId
  }
}
