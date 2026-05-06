/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubAppCleanupServiceTest
{
  @Mock
  private GitHubAppDAO gitHubAppDAO;

  @Mock
  private GitHubAppDeletionService gitHubAppDeletionService;

  private GitHubAppCleanupService service;

  @Before
  public void setUp() {
    service = new GitHubAppCleanupService(gitHubAppDAO, gitHubAppDeletionService);
  }

  @Test
  public void run_NoCandidates_SkipsDeletion() {
    when(gitHubAppDAO.findInactive()).thenReturn(Collections.emptyList());

    service.run();

    verify(gitHubAppDeletionService, never()).delete(any(GitHubApp.class));
  }

  @Test
  public void run_AllDeletionsSucceed_DeletesAll() {
    GitHubApp app1 = inactiveApp("owner-1");
    GitHubApp app2 = inactiveApp("owner-2");
    when(gitHubAppDAO.findInactive()).thenReturn(List.of(app1, app2));

    service.run();

    verify(gitHubAppDeletionService).delete(app1);
    verify(gitHubAppDeletionService).delete(app2);
  }

  @Test
  public void run_OneDeletionFails_ContinuesBatch() {
    GitHubApp app1 = inactiveApp("owner-1");
    GitHubApp app2 = inactiveApp("owner-2");
    when(gitHubAppDAO.findInactive()).thenReturn(List.of(app1, app2));
    doThrow(new RuntimeException("GitHub API error")).when(gitHubAppDeletionService).delete(app1);

    service.run();

    verify(gitHubAppDeletionService).delete(app1);
    verify(gitHubAppDeletionService).delete(app2);
  }

  private GitHubApp inactiveApp(String ownerId) {
    GitHubApp app = new GitHubApp();
    app.setOwnerId(ownerId);
    app.setActive(false);
    return app;
  }
}
