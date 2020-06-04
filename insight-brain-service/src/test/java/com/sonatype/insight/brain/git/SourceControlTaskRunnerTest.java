/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Provider;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.iq.location.discovery.LocationDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlTaskRunnerTest
    extends AbstractComponentTest
{
  @Mock
  private Provider<PullRequestTask> pullRequestTaskProvider;

  @Mock
  private PullRequestTask pullRequestTask;

  @Mock
  private PullRequestExecutor pullRequestExecutor;

  @Mock
  private PullRequestRemediationDetails pullRequestRemediationDetails;

  @Mock
  private Provider<PullRequestLocationDiscoveryTask> locationDiscoveryTaskProvider;

  @Mock
  private PullRequestLocationDiscoveryTask locationDiscoveryTask;

  @Mock
  private LocationDiscoveryExecutor locationDiscoveryExecutor;

  @Mock
  private GitRepositoryInfo gitRepositoryInfo;

  //Subject
  private SourceControlTaskRunner sourceControlTaskRunner;

  private Application application;

  @Rule
  public LogOutput logOutput = new LogOutput(SourceControlTaskRunner.class);

  @Before
  public void setup() {
    sourceControlTaskRunner =
        new SourceControlTaskRunner(
            pullRequestTaskProvider, pullRequestExecutor, locationDiscoveryTaskProvider, locationDiscoveryExecutor);
    Organization organization = tempEntity.newOrganization();
    application = tempEntity.newApplication("appname", "abc123", organization.getId());
  }

  @Test
  public void testDoPullRequestRemediation() {
    // given
    when(pullRequestTaskProvider.get()).thenReturn(pullRequestTask);

    // and
    when(pullRequestRemediationDetails.getApp()).thenReturn(application);
    when(pullRequestRemediationDetails.getToBeRemediated())
        .thenReturn(ComponentIdentifier.createMavenCoordinates("grpid", "artid", "1.2.3"));

    // when
    sourceControlTaskRunner.doPullRequestRemediation(pullRequestRemediationDetails);

    // then we see the PR task executed
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(logOutput).atInfoLevel().contains(
          "Sent for execution: pull request task for [maven: {artifactId=artid," +
          " groupId=grpid, version=1.2.3}] on application with id [" + application.getId() + "]");
    });
    verify(pullRequestTask).run();
  }

  @Test
  public void testIsFormatSupportedForPullRequestRemediation_notSupported() {
    // given that no format is supported

    // when we check format support
    boolean supported = sourceControlTaskRunner
        .isFormatSupportedForPullRequestRemediation(ComponentIdentifier.createNugetCoordinates("foo", "1.2.3"));

    // then we see that the format is not supported
    assertThat(supported).isFalse();
  }

  @Test
  public void testIsFormatSupportedForPullRequestRemediation_mavenFormatSupported() {
    // maven format is supported
    when(pullRequestExecutor.isSupportedFormat(ComponentIdentifier.FORMAT_MAVEN)).thenReturn(true);

    // when we check format support
    boolean supported = sourceControlTaskRunner
        .isFormatSupportedForPullRequestRemediation(ComponentIdentifier.createMavenCoordinates("bar", "foo", "1.2"));

    // then we see that the format is not supported
    assertThat(supported).isTrue();
  }

  @Test
  public void testDoPullRequestLocationDiscovery() throws Exception {
    // given
    when(locationDiscoveryTaskProvider.get()).thenReturn(locationDiscoveryTask);
    when(locationDiscoveryTask.call()).thenReturn(new LocationDiscoveryResult());

    List<ComponentIdentifier> list = new LinkedList<>();
    list.add(ComponentIdentifier.createMavenCoordinates("bar", "foo", "1.2"));

    // when
    LocationDiscoveryResult result =
        sourceControlTaskRunner.doPullRequestLocationDiscovery(list, gitRepositoryInfo, "branch", application.getId());

    // then we see the PR task executed
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(logOutput).atInfoLevel()
          .contains("Sent for execution: location discovery task for 1 component(s) on application with id [" +
              application.getId() + "]");
    });
    verify(locationDiscoveryTask).call();
    assertThat(result).isNotNull();
  }
}
