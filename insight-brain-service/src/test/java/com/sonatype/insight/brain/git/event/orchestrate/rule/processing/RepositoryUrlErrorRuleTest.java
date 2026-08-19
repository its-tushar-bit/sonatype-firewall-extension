/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.UnknownHostException;
import java.util.UUID;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class RepositoryUrlErrorRuleTest
{
  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testCanPushEvent_defaultAllEventTypes() {
    RepositoryUrlErrorRule rule = new RepositoryUrlErrorRule(mockSourceControlUtils);

    // given: an event that has not had a processing error
    SourceControlEvent event = new SourceControlEvent();
    SourceControlEvent.EVENT_TYPES.forEach(type -> {
      event.setEventType(type).setApplicationId(UUID.randomUUID().toString());
      assertThat(rule.canPushEvent(event)).isTrue();
    });
  }

  @Test
  public void testCanPushEvent_exceedErrorThreshold() {
    // given: an event with repository info
    SourceControlEvent event = createEventWithRepositoryInfo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT,
        "http://gitlab.com/project-1/repo-1");

    // when: progressively create errors until limit exceeded
    RepositoryUrlErrorRule rule = new RepositoryUrlErrorRule(mockSourceControlUtils);
    exhaustUrlErrorsAndAssert(rule, event);

    // one more error to exceed the limit
    rule.onEventProcessingError(event, new RuntimeException(new UnknownHostException("host not known")));
    assertThat(rule.canPushEvent(event)).isFalse();
  }

  @Test
  public void testCanPushEvent_errorClearedBySuccessfulEvent() {
    // given: an event with repository info
    SourceControlEvent event = createEventWithRepositoryInfo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT,
        "http://gitlab.com/project-1/repo-1");

    // when: exceed the error limit
    RepositoryUrlErrorRule rule = new RepositoryUrlErrorRule(mockSourceControlUtils);
    exceedUrlErrorLimit(rule, event);
    assertThat(rule.canPushEvent(event)).isFalse();

    // now clear the url error with a successful event
    rule.onEventProcessed(event);
    assertThat(rule.canPushEvent(event)).isTrue();
  }

  @Test
  public void testCanPushEvent_errorClearedByUpdatedUrl() {
    // given: an event with repository info
    SourceControlEvent event = createEventWithRepositoryInfo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT,
        "http://gitlab.com/project-1/repo-1");

    // when: exceed the error limit
    RepositoryUrlErrorRule rule = new RepositoryUrlErrorRule(mockSourceControlUtils);
    exceedUrlErrorLimit(rule, event);
    assertThat(rule.canPushEvent(event)).isFalse();

    // now clear the url error by updating the repo url for the app
    GitRepositoryInfo gitRepositoryInfo = createGitRepositoryInfo("http://sonatype.com/project-1/repo-1");
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(eq(event.getApplicationId())))
        .thenReturn(gitRepositoryInfo);
    assertThat(rule.canPushEvent(event)).isTrue();
  }

  @Test
  public void testCanPushEvent_errorMessageIndicatesUrlProblem() {
    RepositoryUrlErrorRule rule = new RepositoryUrlErrorRule(mockSourceControlUtils);
    RepositoryUrlErrorRule.URL_ERROR_MESSAGES.forEach(message -> {
      // given: an event with repository info
      SourceControlEvent event = createEventWithRepositoryInfo(SourceControlEvent.STATUS_UPDATE_EVENT,
          "http://scm.com/project-1/repo-" + UUID.randomUUID());
      assertThat(rule.canPushEvent(event)).isTrue();
      exceedUrlErrorLimit(rule, event, message);
      assertThat(rule.canPushEvent(event)).isFalse();
    });
  }

  @Test
  public void testCanPushEvent_errorMessageNotAboutUrlProblem() {
    RepositoryUrlErrorRule rule = new RepositoryUrlErrorRule(mockSourceControlUtils);
    SourceControlEvent event = createEventWithRepositoryInfo(SourceControlEvent.STATUS_UPDATE_EVENT,
        "http://scm.com/project-1/repo-X");
    assertThat(rule.canPushEvent(event)).isTrue();

    // when: generate a number of errors not related to the URL
    exceedUrlErrorLimit(rule, event, "something broke");

    // then: we can still push the event (per this rule anyway)
    assertThat(rule.canPushEvent(event)).isTrue();
  }

  private void exhaustUrlErrorsAndAssert(RepositoryUrlErrorRule rule, SourceControlEvent event) {
    for (int i = 1; i < RepositoryUrlErrorRule.REPO_URL_ERROR_THRESHOLD; i++) {
      rule.onEventProcessingError(event, new RuntimeException(new UnknownHostException("host not known")));
      assertThat(rule.canPushEvent(event)).isTrue();
    }
  }

  private void exceedUrlErrorLimit(RepositoryUrlErrorRule rule, SourceControlEvent event) {
    for (int i = 0; i < RepositoryUrlErrorRule.REPO_URL_ERROR_THRESHOLD; i++) {
      rule.onEventProcessingError(event, new RuntimeException(new UnknownHostException("host not known")));
    }
  }

  private void exceedUrlErrorLimit(RepositoryUrlErrorRule rule, SourceControlEvent event, String message) {
    for (int i = 0; i < RepositoryUrlErrorRule.REPO_URL_ERROR_THRESHOLD; i++) {
      rule.onEventProcessingError(event, new Exception(message));
    }
  }

  private SourceControlEvent createEventWithRepositoryInfo(String eventType, String repositoryUrl) {
    SourceControlEvent event = new SourceControlEvent()
        .setEventType(eventType)
        .setApplicationId(UUID.randomUUID().toString());
    GitRepositoryInfo gitRepositoryInfo = createGitRepositoryInfo(repositoryUrl);
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(eq(event.getApplicationId())))
        .thenReturn(gitRepositoryInfo);
    return event;
  }

  private GitRepositoryInfo createGitRepositoryInfo(String repositoryUrl) {
    return new GitRepositoryInfo(repositoryUrl, null, "user", "token", SourceControlProvider.GITLAB, "main", true, true,
        true, true, true, true, false, null);
  }
}
