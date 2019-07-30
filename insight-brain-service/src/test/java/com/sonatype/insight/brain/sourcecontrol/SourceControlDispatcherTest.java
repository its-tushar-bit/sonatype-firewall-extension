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
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlProvider;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.nexus.github.JsonUtils;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlDispatcherTest
    extends AbstractComponentTest
{
  @Rule
  public final GitHubApiRule github = new GitHubApiRule();

  @Inject
  private SourceControlDispatcher dispatcher;
  
  @Inject
  private PlexusCipher plexusCipher;

  @Inject
  private InsightConfig config;
  
  private Application application;
  
  private ApplicationEvaluationEvent event;

  @Mock
  private TelemetrySender telemetrySenderMock;
  
  private static final ImmutableMap<Object, Object> SUCCESS = ImmutableMap.builder()
      .put("url", "http://example/com")
      .put("creator",
          ImmutableMap.builder().put("login", "foo").build()
      ).build();
        
  private static final String API_URL = "/api/v3/repos/owner/repo/statuses/commitHash";

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
    String repositoryUrl = github.getUri() + "owner/repo";
    tempEntity.newSourceControl(application.getId(), repositoryUrl, encryptedToken, SourceControlProvider.GITHUB);
    event = createEvent();
  }
  
  @Test
  public void testOnEvent() throws Exception {
    github.setResponseForUri(API_URL, JsonUtils.toJson(SUCCESS), 201);
    
    dispatcher.on(event);
    
    assertThat(github.verify(API_URL, 201)).isTrue();
  }

  /**
   * remote api will respond with 404 for misconfigured urls or if authentication is not adequate
   * https://developer.github.com/v3/troubleshooting/#why-am-i-getting-a-404-error-on-a-repository-that-exists
   */
  @Test
  public void testOnEvent_404() throws Exception {
    dispatcher.on(event);

    assertThat(github.verify(API_URL, 404)).isTrue(); 
  }

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
