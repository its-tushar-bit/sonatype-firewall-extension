/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.api.GitApiClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class SourceControlSshServiceTest
    extends AbstractComponentH2Test
{
  private static final String APPLICATION_ID = "any_app_id";

  private static final String SSH_URL = "git@does:not/matter.git";

  private static final String NO_SSH_URL = null;

  private static final boolean SSH_DISABLED = false;

  private static final boolean SSH_ENABLED = true;

  @Mock
  private SourceControlDAO sourceControlDAO;

  @Mock
  private GitClientFactory gitClientFactory;

  @Mock
  private SourceControlUtils sourceControlUtils;

  @Mock
  private GitApiClient gitApiClient;

  private SourceControlSshService sourceControlSshService;

  @BeforeEach
  public void before() {
    lenient().when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(gitApiClient);
    sourceControlSshService = new SourceControlSshService(sourceControlDAO, gitClientFactory, sourceControlUtils);
  }

  @Test
  public void testSourceControlEntryMissing() {
    // given - no source control entry
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(null);

    // when - the ssh url is checked
    sourceControlSshService.verifySshUrlAndUpdateIfNeeded(APPLICATION_ID);

    // then - there are no API calls to get the SSH URL
    verifyNoInteractions(gitApiClient);
  }

  @Test
  public void testSshDisabled() {
    // given - a source control entry with ssh disabled (and no ssh url)
    GitRepositoryInfo gitRepositoryInfo = createGitRepositoryInfo(NO_SSH_URL, SSH_DISABLED);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);

    // when - the ssh url is checked
    sourceControlSshService.verifySshUrlAndUpdateIfNeeded(APPLICATION_ID);

    // then
    verifyNoInteractions(gitApiClient);
  }

  @Test
  public void testSshEnabledButNoSshUrl() throws IOException {
    // given: a source control object with SSH enabled yet NO SSH url
    GitRepositoryInfo gitRepositoryInfo = createGitRepositoryInfo(NO_SSH_URL, SSH_ENABLED);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);

    // and: an existing db entry (values inside don't matter)
    SourceControl sourceControl = new SourceControl();
    when(sourceControlDAO.getByOwnerId(anyString())).thenReturn(sourceControl);

    // and: the api client returns an ssh url
    when(gitApiClient.getSshUrl()).thenReturn(SSH_URL);

    // when: the ssh url is checked
    sourceControlSshService.verifySshUrlAndUpdateIfNeeded(APPLICATION_ID);

    // then: client was invoked and the ssh url stored
    verify(gitApiClient, times(1)).getSshUrl();
    verify(sourceControlDAO, times(1)).update(argThat(argument -> argument.getRepositorySshUrl().equals(SSH_URL)));
  }

  @Test
  public void testSshEnabledAndAlreadyHasSshUrl() {
    // given: a source control object that has SSH enabled with an SSH url
    GitRepositoryInfo gitRepositoryInfo = createGitRepositoryInfo(SSH_URL, SSH_ENABLED);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);

    // when: the ssh url is checked
    sourceControlSshService.verifySshUrlAndUpdateIfNeeded(APPLICATION_ID);

    // then: no git client interactions and no DAO update
    verifyNoInteractions(gitApiClient);
    verify(sourceControlDAO, never()).update(any(SourceControl.class));
  }

  private GitRepositoryInfo createGitRepositoryInfo(String sshUrl, boolean enabled) {
    return new GitRepositoryInfo(null, sshUrl, null, null, null, null, null, null, null, null, null, null, enabled,
        null);
  }
}
