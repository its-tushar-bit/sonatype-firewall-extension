/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.inject.Binder;
import org.junit.Before;
import org.mockito.Mock;

public abstract class BaseSourceControlDispatcherTest
    extends AbstractComponentTest
{
  @Inject
  protected SourceControlDispatcher dispatcher;
  
  @Inject
  protected PlexusCipher plexusCipher;

  @Inject
  protected InsightConfig config;

  protected Application application;

  protected ApplicationEvaluationEvent event;

  @Mock
  protected TelemetrySender telemetrySenderMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }
  
  @Before
  public void setup() throws PlexusCipherException {
    application = tempEntity.newApplicationWithParent();
    config.setBaseUrl("http://localhost");
    String encryptedToken;
    synchronized (plexusCipher) {
      encryptedToken = plexusCipher.encrypt("token", "CMMDwoV");
    }
    String repositoryUrl = getGitApiClient().getUri() + "owner/repo";
    tempEntity.newSourceControl(application.getId(), repositoryUrl, encryptedToken, null);
    event = createEvent();
  }

  protected abstract GitApiRule getGitApiClient();

  private ApplicationEvaluationEvent createEvent() {
    ApplicationEvaluationEvent event = new ApplicationEvaluationEvent();
    event.reportId = "scanId";
    event.commitHash = "commitHash";
    event.evaluationDate = new Date();
    event.outcome = Action.ID_WARN;
    event.ownerId = application.getId();
    event.policyEvaluationId = "foo";
    return event;
  }
}
