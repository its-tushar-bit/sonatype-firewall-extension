/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.api.model.ProjectUrl;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

/**
 * This class exists simply because PullRequestPollingSchedulerTest is a subclass of AbstractMultiTenantDatabaseTest
 * and this does not have clean access to TemporaryEntity. At this time, the test hierarchy is such that there is
 * no easy way to convert it to something that has access to TemporaryEntity while also being able to run
 * tests that use multiple tenants in the same test. Therefore, single-tenant tests which do need access to
 * TemporaryEntity are in this separate test class
 */
@RunWith(MockitoJUnitRunner.class)
@Category(SlowTest.class)
public class PullRequestPollingSchedulerSingleTenantTest
    extends AbstractBrainServiceIntegrationTest
{
  private static final String TOKEN = new String(
      new PasswordHandler(new TestEncryptionKeyStore())
          .encryptPassword("password".toCharArray())
  );

  // There seems to be a quirk in AbstractBaseIntegrationTest (this class' grandparent) where it won't reliable
  // start a distinct IQ server for a given test class, using that class' `getBrainModules` method, unless that
  // class also specifies a custom configurator or one of a number of other things (see the logic in
  // AbstractBaseIntegrationTest.maybeStopTestIqServer).  We don't need a custom configurator generally, but in
  // order to get our `getBrainModules` method to be used reliably, we need to specify a configurator that is
  // specific to this class. This no-op function below will do.
  private static final Configurator CONFIGURATOR = config -> {
  };

  @Rule
  public LogOutput logOutput =
      new LogOutput(PullRequestPollingScheduler.class, PullRequestPollingService.class, SourceControlDAO.class);

  @Mock
  private IqForScmLicenseChecker licenseCheckerMock;

  @Mock
  private PullRequestInfoProvider pullRequestInfoProviderMock;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Mock
  private ScmNodeProcessor scmNodeProcessor;

  private GitClientFactory gitClientFactorySpy;

  private PullRequestPollingScheduler scheduler;

  private final int delaySeconds = 1;

  private final int intervalSeconds = 1;

  @Singleton
  private static class GitClientFactorySpyProvider
      implements Provider<GitClientFactory>
  {
    private GitClientFactory gitClientFactorySpy;

    @Inject
    public GitClientFactorySpyProvider(InsightProxy insightProxy) {
      gitClientFactorySpy = spy(new GitClientFactory(insightProxy));
    }

    @Override
    public GitClientFactory get() {
      return gitClientFactorySpy;
    }
  }

  @Override
  protected List<Module> getBrainModules() {
    List<Module> modules = new ArrayList<>();
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(GitClientFactory.class).toProvider(GitClientFactorySpyProvider.class);
        bind(IqForScmLicenseChecker.class).toInstance(licenseCheckerMock);
      }
    });

    modules.addAll(super.getBrainModules());
    return modules;
  }

  @Before
  public void before() throws Exception {
    startIqTestServer(CONFIGURATOR);

    PullRequestPollingService service = lookup(PullRequestPollingService.class);
    ApiConfigFeaturesService apiConfigFeaturesService = lookup(ApiConfigFeaturesService.class);
    gitClientFactorySpy = lookup(GitClientFactory.class);

    doReturn(pullRequestInfoProviderMock).when(gitClientFactorySpy).createPullRequestInfoClient(any());
    doAnswer(invocation -> {
      GitRepositoryInfo repoInfo = (GitRepositoryInfo)invocation.getArgument(0);

      GitApiClient gitApiClient = mock(GitApiClient.class);
      ProjectUrl projectUrl = mock(ProjectUrl.class);
      when(gitApiClient.getProjectUrl()).thenReturn(projectUrl);
      when(projectUrl.getNamespace()).thenReturn(repoInfo.getRepositoryUrl());

      return gitApiClient;
    }).when(gitClientFactorySpy).createApiClient(any());

    when(licenseCheckerMock.isPullRequestCommentingSupported()).thenReturn(true);
    scheduler = spy(new PullRequestPollingScheduler(service, licenseCheckerMock, apiConfigFeaturesService,
        delaySeconds, intervalSeconds, mockShutdownHandler, scmNodeProcessor));
  }

  @After
  public void after() {
    scheduler.deregister();
  }

  @Test
  @ManualIqServerInit
  public void testPullRequestPollingScheduler_skipsAppsWithMissingParentConfiguration() throws Exception {
    Organization orgWithoutScmConf = tempEntity.newOrganization("orgWithoutScmConf");
    String orgWithoutScmConfId = orgWithoutScmConf.getId();
    Application appWithoutParentScmConf = tempEntity.newApplication(orgWithoutScmConfId);
    Application app2WithoutParentScmConf = tempEntity.newApplication(orgWithoutScmConfId);
    Organization orgWithScmConf = tempEntity.newOrganization("orgWithScmConf");
    String orgWithScmConfId = orgWithScmConf.getId();
    Application appWithParentScmConf = tempEntity.newApplication(orgWithScmConfId);

    // only created so that the SCM config for appWithoutParentScmConf can be created. After that, but before
    // the scheduler is registered, this is deleted.
    SourceControl tempOrgSourceControl =
        tempEntity.newSourceControl(orgWithoutScmConfId, null, TOKEN, SourceControlProvider.GITLAB);

    // the org SCM config that will still exist as the test runs
    tempEntity.newSourceControl(orgWithScmConfId, null, TOKEN, SourceControlProvider.GITLAB);

    // All apps have only a partial SCM configuration of their own. However one of the apps is in an org
    // that has a full SCM configuration from which it inherits.  This first one has PR commenting explicitly turned
    // off - ensure that that is also processed correctly and doesn't cause exceptions that prevent the processing
    // of the remaining ones
    tempEntity.newSourceControl(app2WithoutParentScmConf.getId(), (SourceControlProvider)null, null,
        "https://localhost:12345/foo/asdf", null, false, null, null, null);
    tempEntity.newSourceControl(appWithoutParentScmConf.getId(), (SourceControlProvider)null, null,
        "https://localhost:12346/foo/asdf", null, true, null, null, null);

    tempEntity.newSourceControl(appWithParentScmConf.getId(), (SourceControlProvider)null, null,
        "https://localhost:12347/bar/qwerty", null, true, null, null, null);

    lookup(SourceControlDAO.class).delete(tempOrgSourceControl);

    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    // We need to wait for the _completion_ of the discoverPullRequestsForCommenting
    // method. Simply waiting for mockito to record a call to the spy is not enough as it only records when the
    // method is entered, not when it completes. The suggested workaround in
    // https://github.com/mockito/mockito/issues/1089 is to use a CountDownLatch as seen here.
    CountDownLatch prPollingCountdownLatch = new CountDownLatch(1);
    doAnswer(invocation -> {
      invocation.callRealMethod();
      prPollingCountdownLatch.countDown();
      return null;
    }).when(scheduler).discoverPullRequestsForCommenting();

    // start the scheduler and wait until it has run once (but not so long that it can run twice!)
    scheduler.register();
    boolean prPollingCompleted = prPollingCountdownLatch.await(2, TimeUnit.SECONDS);
    assertThat(prPollingCompleted).isTrue();

    // Note: the whole URL gets passed through due to the mocked implementation of projectUri.getNamespace().
    // The important part here is that it's the URL of appWithParentScmConf and not the other ones
    verify(pullRequestInfoProviderMock)
        .getPullRequestsSince(eq("https://localhost:12347/bar/qwerty"), any(), anyInt());

    // check that the call checked above is the only one
    verify(pullRequestInfoProviderMock, times(1))
        .getPullRequestsSince(any(), any(), anyInt());

    logOutput.assertThat().atErrorLevel().isEmpty();
  }
}
