/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestPollingTrackerTest
    extends AbstractComponentTest
{
  private static final long MS_PER_MINUTE = 60_000;

  @Inject
  private SourceControlDAO sourceControlDAO;

  // test subject
  private PullRequestPollingTracker pollingTracker;

  @Before
  public void before() {
    pollingTracker = new PullRequestPollingTracker(sourceControlDAO);
  }

  @Test
  public void testGetNextRepositoryToPoll() {
    // given: two application source control entries
    long now = System.currentTimeMillis();
    SourceControl sourceControl1 = createSourceControl();
    sourceControl1.setPullRequestPollTime(new Date(now - 2000));
    sourceControlDAO.update(sourceControl1);
    SourceControl sourceControl2 = createSourceControl();
    sourceControl2.setPullRequestPollTime(new Date(now - 1000));
    sourceControlDAO.update(sourceControl2);

    // when: get next repo to poll
    SourceControl sourceControl = pollingTracker.getNextRepositoryToPoll();

    // then: expecting source control 1
    assertThat(sourceControl.getId()).isEqualTo(sourceControl1.getId());

    // Simulate that sourceControl1 was processed
    sourceControl1.setPullRequestPollTime(new Date(now));
    sourceControlDAO.update(sourceControl1);

    // when: get next repo to poll
    sourceControl = pollingTracker.getNextRepositoryToPoll();

    // then: expecting source control 2
    assertThat(sourceControl.getId()).isEqualTo(sourceControl2.getId());

    // when: get next repo to poll
    sourceControl = pollingTracker.getNextRepositoryToPoll();

    // then: we've already seen both source controls in this tracker so expecting null now
    assertThat(sourceControl).isNull();
  }

  @Test
  public void testOnPullRequestProcessed() {
    // given: source control entry with initial values
    Date date = new Date();
    SourceControl sourceControl = createSourceControl();
    sourceControl.setPullRequestErrorCount(3);
    sourceControlDAO.update(sourceControl);

    // when: update poll times called
    pollingTracker.onPullRequestProcessed(sourceControl, "org", "repo", "token", date);

    // then: verify dates and error count
    sourceControl = sourceControlDAO.getById(sourceControl.getId());
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(date);
    assertThat(sourceControl.getPullRequestErrorCount()).isEqualTo(0);

    // and: cutoff time is correct
    Date cutoff = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24));
    assertThat(pollingTracker.getCachedCutoffTime("org", "repo", "token", cutoff)).isAfter(cutoff);
    assertThat(pollingTracker.getCachedCutoffTime("org2", "repo", "token2", cutoff)).isEqualTo(cutoff);
  }

  @Test
  public void testOnPullRequestProcessed_2IQAppsSameRepoUrl() {
    // given: source control entries with initial values
    Date date = new Date();
    SourceControl sourceControl1 = createSourceControl("http://localhost/test/repo");
    SourceControl sourceControl2 = createSourceControl("http://localhost/test/repo");

    // when: update poll times called
    pollingTracker.onPullRequestProcessed(sourceControl1, "org", "repo", "token", date);

    // then: verify pullRequestPollTime is updated for both records
    assertThat(sourceControlDAO.getById(sourceControl1.getId()).getPullRequestPollTime()).isEqualTo(date);
    assertThat(sourceControlDAO.getById(sourceControl2.getId()).getPullRequestPollTime()).isEqualTo(date);
  }

  @Test
  public void testOnPullRequestProcessed_withNullRepo() {
    // given: source control entry with initial values
    Date date = new Date();
    SourceControl sourceControl = createSourceControl();
    sourceControl.setPullRequestErrorCount(3);
    sourceControlDAO.update(sourceControl);

    // when: update poll times called
    pollingTracker.onPullRequestProcessed(sourceControl, "org", null, "token", date);

    // then: verify dates and error count
    sourceControl = sourceControlDAO.getById(sourceControl.getId());
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(date);
    assertThat(sourceControl.getPullRequestErrorCount()).isEqualTo(0);

    // and: cutoff time is correct
    Date cutoff = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24));
    assertThat(pollingTracker.getCachedCutoffTime("org", null, "token", cutoff)).isAfter(cutoff);
    assertThat(pollingTracker.getCachedCutoffTime("org2", null, "token2", cutoff)).isEqualTo(cutoff);
  }

  @Test
  public void testOnPullRequestProcessed_forApplication() {
    // given: source control entry for app with initial values
    Date date = new Date();
    SourceControl sourceControl = createSourceControl();
    sourceControl.setPullRequestErrorCount(3);
    sourceControlDAO.update(sourceControl);

    // when: update poll times called
    pollingTracker.onPullRequestProcessedForApplication(sourceControl.getOwnerId(), date);

    // then: verify poll dates set correctly as well as error count
    sourceControl = sourceControlDAO.getById(sourceControl.getId());
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(date);
  }

  @Test
  public void testOnErrorProcessingPullRequests() {
    // given: initialized source control entry
    final List<Integer> expectedErrorOffsetsInMinutes = ImmutableList.of(5, 10, 15, 30, 60, 360, 720, 60 * 24, 60 * 24);
    final List<String> expectedErrorOffsetText = ImmutableList.of("5 minutes", "10 minutes", "15 minutes", "30 minutes",
        "1 hour", "6 hours", "12 hours", "24 hours", "24 hours");
    Date cutoffTime = new Date();
    SourceControl sourceControl = createSourceControl();
    sourceControl.setPullRequestErrorCount(0);
    sourceControl.setPullRequestPollTime(cutoffTime);
    sourceControlDAO.update(sourceControl);

    for (int i = 0; i < expectedErrorOffsetsInMinutes.size(); i++) {
      // when: report error
      Date minPollTime = new Date(currentTimeMillis() + (MS_PER_MINUTE * expectedErrorOffsetsInMinutes.get(i)));
      String offsetMessage = pollingTracker.onErrorProcessingPullRequests(sourceControl);
      Date maxPollTime = new Date(currentTimeMillis() + (MS_PER_MINUTE * expectedErrorOffsetsInMinutes.get(i)));

      // then: error count incremented, cutoff unchanged, poll time updated per sequence
      assertThat(offsetMessage).isEqualTo(expectedErrorOffsetText.get(i));
      sourceControl = sourceControlDAO.getById(sourceControl.getId());
      assertThat(sourceControl.getPullRequestPollTime()).isBetween(minPollTime, maxPollTime, true, true);
      assertThat(sourceControl.getPullRequestErrorCount()).isEqualTo(i + 1);
    }
  }

  @Test
  public void testOnErrorProcessingPullRequests_2IQAppsSameRepoUrl() {
    // given: initialized source control entries
    SourceControl sourceControl1 = createSourceControl("http://localhost/test/repo");
    sourceControl1.setPullRequestErrorCount(2);
    sourceControlDAO.update(sourceControl1);
    SourceControl sourceControl2 = createSourceControl("http://localhost/test/repo");
    sourceControl2.setPullRequestErrorCount(2);
    sourceControlDAO.update(sourceControl2);

    // when: report error
    Date minPollTime = new Date(currentTimeMillis() + (MS_PER_MINUTE * 15));
    String offsetMessage = pollingTracker.onErrorProcessingPullRequests(sourceControl1);
    Date maxPollTime = new Date(currentTimeMillis() + (MS_PER_MINUTE * 15));

    // then: error count incremented, cutoff unchanged, poll time updated per sequence
    assertThat(offsetMessage).isEqualTo("15 minutes");

    // both source control records were updated
    SourceControl sourceControl = sourceControlDAO.getById(sourceControl1.getId());
    assertThat(sourceControl.getPullRequestPollTime()).isBetween(minPollTime, maxPollTime, true, true);
    assertThat(sourceControl.getPullRequestErrorCount()).isEqualTo(3);

    sourceControl = sourceControlDAO.getById(sourceControl2.getId());
    assertThat(sourceControl.getPullRequestPollTime()).isBetween(minPollTime, maxPollTime, true, true);
    assertThat(sourceControl.getPullRequestErrorCount()).isEqualTo(3);
  }

  @Test
  public void testVisitAndCheckKeyAlreadyUsed() {
    // when: visit and key combo used for first time
    boolean visited = pollingTracker.visitAndCheckKeyAlreadyUsed("org", "repo", "token");

    // then: shouldn't have been visited yet
    assertThat(visited).isFalse();

    // when: visit again
    visited = pollingTracker.visitAndCheckKeyAlreadyUsed("org", "repo", "token");

    // then: should indicate was visited already
    assertThat(visited).isTrue();
  }

  @Test
  public void testVisitAndCheckKeyAlreadyUsed_withNullRepo() {
    // when: visit and key combo used for first time
    boolean visited = pollingTracker.visitAndCheckKeyAlreadyUsed("org", null, "token");

    // then: shouldn't have been visited yet
    assertThat(visited).isFalse();

    // when: visit again
    visited = pollingTracker.visitAndCheckKeyAlreadyUsed("org", null, "token");

    // then: should indicate was visited already
    assertThat(visited).isTrue();
  }

  @Test
  public void cutOffTimesAreTenantAware() {
    Date date = new Date();
    Date oldDate = Date.from(date.toInstant().minusSeconds(1L));

    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      // The initial fetch of a key will add it to the cache for this tenant
      Date cachedCutoffTime = pollingTracker.getCachedCutoffTime("org", "repo", "token", date);

      assertThat(cachedCutoffTime).isEqualTo(date);
    });

    testAsNewTenant(testName, t2 -> {
      Date cachedCutoffTime = pollingTracker.getCachedCutoffTime("org", "repo", "token", oldDate);

      // cutOffTime should be unique for each tenant therefore the date passed in here should be used
      assertThat(cachedCutoffTime).isEqualTo(oldDate);
    });

    testAsTenant(tenant1, t -> {
      // Ensure the original tenant cache is not overridden
      Date cachedCutoffTime = pollingTracker.getCachedCutoffTime("org", "repo", "token", date);
      assertThat(cachedCutoffTime).isEqualTo(date);
    });
  }

  private SourceControl createSourceControl() {
    Application app = tempEntity.newApplicationWithParent();
    return tempEntity.newSourceControl(app.getId(), "http://localhost/test/" + app.getId(), "testToken",
        SourceControlProvider.GITHUB);
  }

  private SourceControl createSourceControl(String repoUrl) {
    Application app = tempEntity.newApplicationWithParent();
    return tempEntity.newSourceControl(app.getId(), repoUrl, "testToken", SourceControlProvider.GITLAB);
  }
}
