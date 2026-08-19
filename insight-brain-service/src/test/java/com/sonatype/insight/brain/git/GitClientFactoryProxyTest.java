/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.utils.AbstractHttpClientTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.gitlab.GitLabApiClientUtils;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Ensure that all SCM clients and providers properly support proxy calls.
 *
 * Implementation note: we need to run the proxy test against all SCM clients and (currently) three API method calls.
 * This impl uses a junit parameterized test with a custom interface ({@link TestConsumer} (which simply contains the
 * five necessary parameters (one is the 'url' coming from the parent test) and handles the checked Exception).
 *
 * So the actual test methods are in {@link AbstractHttpClientTest} and this class overrides {@link #pingUrl(String)}.
 * The junit parameterized test will ultimately result in 12 test runs (4 scms times 3 methods to test).
 */
@RunWith(Parameterized.class)
public class GitClientFactoryProxyTest
    extends AbstractHttpClientTest
{
  private static final TestConsumer createApiClient = (client, provider, url, urlSuffix, username) -> {
    client.clearUrlCaches();
    if (provider == SourceControlProvider.GITLAB) {
      client.addApiUrlMapping(url + urlSuffix, url + "api/v4");
    }
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo(url + urlSuffix, null, username, "token", provider, "master", false, false, false, false,
            false, false, false, null);
    client.createApiClient(gitRepositoryInfo).isRepositoryPrivate();
  };

  private static final TestConsumer createPullRequestInfoClient = (client, provider, url, urlSuffix, username) -> {
    client.clearUrlCaches();
    if (provider == SourceControlProvider.GITLAB) {
      client.addPullRequestInfoClientUrlMapping(url + urlSuffix, url + "api/v4");
    }
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo(url + urlSuffix, null, username, "token", provider, "master", false, false, false, false,
            false, false, false, null);
    client.createPullRequestInfoClient(gitRepositoryInfo).getPullRequestsSince("namespace", OffsetDateTime.now(), 0);
  };

  private static final TestConsumer createGeneralApiClient = (client, provider, url, urlSuffix, username) -> {
    if (provider == SourceControlProvider.GITLAB) {
      // GitLabApiClientUtils.getBaseApiUrl makes calls to GitLab REST API to determine the URL context
      // The below setup is required to make it work
      GitLabApiClientUtils gitLabApiClientUtilsSpy = spy(new GitLabApiClientUtils(new Configuration()));
      doReturn(url + "api/v4").when(gitLabApiClientUtilsSpy).getBaseApiUrl(anyString(), anyString());
      GitClientFactory clientSpy = spy(client);
      doReturn(gitLabApiClientUtilsSpy).when(clientSpy).getClientUtils(eq(SourceControlProvider.GITLAB), any());
      clientSpy.createGeneralApiClient(provider, url, username, "token").listAllRepositories();
    }
    else {
      client.createGeneralApiClient(provider, url, username, "token").listAllRepositories();
    }
  };

  @Parameter(0)
  public TestConsumer func;

  @Parameter(1)
  public SourceControlProvider provider;

  @Parameter(2)
  public String urlSuffix;

  @Parameter(3)
  public String username;

  @Inject
  private GitClientFactory gitClientFactory;

  @Parameters(name = "{index} - {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {createApiClient, SourceControlProvider.GITHUB, "org/repo", null},
      {createApiClient, SourceControlProvider.GITLAB, "org/repo", null},
      {createApiClient, SourceControlProvider.BITBUCKET, "scm/org/repo", "username"},
      {createApiClient, SourceControlProvider.AZURE, "org/project/_git/repo", "username"},
      {createPullRequestInfoClient, SourceControlProvider.GITHUB, "org/repo", null},
      {createPullRequestInfoClient, SourceControlProvider.GITLAB, "org/repo", null},
      {createPullRequestInfoClient, SourceControlProvider.BITBUCKET, "scm/org/repo", "username"},
      {createPullRequestInfoClient, SourceControlProvider.AZURE, "org/project/_git/repo", "username"},
      {createGeneralApiClient, SourceControlProvider.GITHUB, "org/repo", null},
      {createGeneralApiClient, SourceControlProvider.GITLAB, "org/repo", null},
      {createGeneralApiClient, SourceControlProvider.BITBUCKET, "scm/org/repo", "username"},
      {createGeneralApiClient, SourceControlProvider.AZURE, "org/project/_git/repo", "username"}
    });
  }

  @Override
  protected void pingUrl(String url) throws Exception {
    // Invoke the test method and pass in test parameters
    // Note 'url' comes the parent test and the rest from the test matrix.
    func.accept(gitClientFactory, provider, url, urlSuffix, username);
  }

  /**
   * Interface which supports the throwing of a checked exception from the client calls used in test above
   */
  private interface TestConsumer
  {
    /**
     * @param gitClientFactory The subject class under test. Injected above and passed in.
     * @param sourceControlProvider The SCM provider under test.
     * @param url host url for SCM (passed in from {@link #pingUrl(String)}).
     * @param urlSuffix url suffix to add to host URL that is provider specific.
     * @param username username from credentials.
     * @throws Exception The client methods (e.g. getPullRequestsSince) throw an IOException.
     */
    void accept(
        final GitClientFactory gitClientFactory,
        final SourceControlProvider sourceControlProvider,
        final String url,
        final String urlSuffix,
        final String username) throws Exception;
  }
}
