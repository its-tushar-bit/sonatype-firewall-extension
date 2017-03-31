/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;

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
    scanPolicyEvaluator = getCLMServer().getInjector().getInstance(ScanPolicyEvaluator.class);
    asyncEventBus = getCLMServer().getInjector().getInstance(AsyncEventBus.class);
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
}
