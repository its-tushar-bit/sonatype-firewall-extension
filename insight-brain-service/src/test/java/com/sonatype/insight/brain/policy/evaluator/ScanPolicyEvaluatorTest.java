/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;

import com.google.inject.Injector;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ScanPolicyEvaluatorTest
    extends AbstractBrainServiceTest
{
  private Application application;

  private ScanPolicyEvaluator scanPolicyEvaluator;

  private AsyncEventBus asyncEventBus;

  private TestEventHandler<ApplicationEvaluationEvent> handler;

  private InsightWork insightWork;

  @After
  public void after() {
    if (handler != null) {
      asyncEventBus.unregister(handler);
    }
  }

  @Before
  public void setup() throws Exception {
    Organization organization = tempEntity.newOrganization();
    application = tempEntity.newApplication("name", "publicId", organization.getId(), "admin");

    Injector injector = getCLMServer().getInjector();
    scanPolicyEvaluator = injector.getInstance(ScanPolicyEvaluator.class);
    asyncEventBus = injector.getInstance(AsyncEventBus.class);
    insightWork = injector.getInstance(InsightWork.class);
  }

  @Test
  public void testEvaluate_EmitsApplicationEvaluationEvent() throws IOException, InterruptedException {
    handler = new TestEventHandler<>(new CountDownLatch(1));

    String scanId = "scanId";
    Stage stage = new Stage(Stage.ID_BUILD);

    // Simulate that the report is available
    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");

    asyncEventBus.register(handler);

    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event, is(notNullValue()));
    assertThat(event.stageTypeId, is(Stage.ID_BUILD));
    assertThat(event.ownerId, is(application.getId()));
    assertThat(event.initiator, is("system"));
  }

  @Test
  public void testEvaluate_DeletesPreviousScanFile() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId1 = "scanId1";
    File scanFile1 = createScanFile(application, scanId1);
    mockReport(scanId1, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId1, stage);
    assertThat(scanFile1.exists(), is(true));

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    String scanId2 = "scanId2";
    File scanFile2 = createScanFile(application, scanId2);
    mockReport(scanId2, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId2, stage);
    assertThat(scanFile1.exists(), is(false));
    assertThat(scanFile2.exists(), is(true));
  }

  @Test
  public void testEvaluate_ReEvaluationDoesNotDeleteScanFile() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = "scanId";
    File scanFile = createScanFile(application, scanId);
    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);
    assertThat(scanFile.exists(), is(true));

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);
    assertThat(scanFile.exists(), is(true));
  }

  @Test
  public void testEvaluate_DoesNotDeleteScanFileForDifferentStage() throws Exception {
    Stage stage1 = new Stage(Stage.ID_BUILD);

    String scanId1 = "scanId1";
    File scanFile1 = createScanFile(application, scanId1);
    mockReport(scanId1, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId1, stage1);
    assertThat(scanFile1.exists(), is(true));

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    Stage stage2 = new Stage(Stage.ID_RELEASE);
    String scanId2 = "scanId2";
    File scanFile2 = createScanFile(application, scanId2);
    mockReport(scanId2, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId2, stage2);
    assertThat(scanFile1.exists(), is(true));
    assertThat(scanFile2.exists(), is(true));
  }

  @Test
  public void testEvaluate_CanReEvaluatePreviousScan() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId1 = "scanId1";
    File scanFile1 = createScanFile(application, scanId1);
    mockReport(scanId1, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId1, stage);

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    String scanId2 = "scanId2";
    createScanFile(application, scanId2);
    mockReport(scanId2, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId2, stage);

    // The first scan file was deleted by the second policy evaluation.
    // A re-evaluation of the first scan doesn't need the scan so it should succeed.
    assertThat(scanFile1.exists(), is(false));
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId1, stage);
  }

  private File createScanFile(Application app, String scanId) {
    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    scanFile.delete();
    URL testScanFileUrl = getClass().getResource("/ScanPolicyEvaluatorTest/scan.xml.gz");
    try {
      FileUtils.copyFile(new File(testScanFileUrl.getFile()), scanFile);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return scanFile;
  }

  private void waitForTimeAdvance() {
    for (long start = System.currentTimeMillis(); System.currentTimeMillis() <= start;) {
    }
  }
}
