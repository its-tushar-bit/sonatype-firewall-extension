/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitHubAppSelectionServiceTest
{
  @Mock
  private GitHubAppDAO gitHubAppDAO;

  @Mock
  private GitHubAppSelectionCache selectionCache;

  private GitHubAppSelectionService service;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new GitHubAppSelectionService(gitHubAppDAO, selectionCache);
  }

  @Test
  public void select_cacheHit_returnsWithoutDaoCall() {
    GitHubApp cached = createApp("app-1");
    when(selectionCache.get("owner-X")).thenReturn(Optional.of(cached));

    GitHubApp result = service.select("owner-X");

    assertThat(result).isSameAs(cached);
    verifyNoInteractions(gitHubAppDAO);
  }

  @Test
  public void select_cacheHitEmpty_returnsNullWithoutDaoCall() {
    when(selectionCache.get("owner-X")).thenReturn(Optional.empty());

    GitHubApp result = service.select("owner-X");

    assertThat(result).isNull();
    verifyNoInteractions(gitHubAppDAO);
  }

  @Test
  public void select_noApps_returnsNullAndCachesNegativeResult() {
    when(selectionCache.get("owner-X")).thenReturn(null);
    when(gitHubAppDAO.getNearestGitHubApps("owner-X")).thenReturn(Collections.emptyList());

    GitHubApp result = service.select("owner-X");

    assertThat(result).isNull();
    verify(selectionCache).put("owner-X", Optional.empty());
  }

  @Test
  public void select_singleApp_returnsThatApp() {
    GitHubApp app1 = createApp("app-1");
    when(selectionCache.get("owner-X")).thenReturn(null);
    when(gitHubAppDAO.getNearestGitHubApps("owner-X")).thenReturn(List.of(app1));

    GitHubApp result = service.select("owner-X");

    assertThat(result).isSameAs(app1);
    verify(selectionCache).put("owner-X", Optional.of(app1));
  }

  @Test
  public void select_multipleApps_deterministicSelection() {
    GitHubApp app1 = createApp("aaa");
    GitHubApp app2 = createApp("bbb");
    GitHubApp app3 = createApp("ccc");
    when(selectionCache.get("owner-X")).thenReturn(null);
    when(gitHubAppDAO.getNearestGitHubApps("owner-X")).thenReturn(List.of(app1, app2, app3));

    GitHubApp result1 = service.select("owner-X");
    // Reset cache mock for second call
    when(selectionCache.get("owner-X")).thenReturn(null);
    GitHubApp result2 = service.select("owner-X");

    assertThat(result1.getId()).isEqualTo(result2.getId());
  }

  @Test
  public void select_handlesIntegerMinValueHashCode() {
    // "polygenelubricants" has hashCode() == Integer.MIN_VALUE in Java
    String edgeCaseId = "polygenelubricants";
    GitHubApp app1 = createApp("app-1");
    GitHubApp app2 = createApp("app-2");
    GitHubApp app3 = createApp("app-3");
    when(selectionCache.get(edgeCaseId)).thenReturn(null);
    when(gitHubAppDAO.getNearestGitHubApps(edgeCaseId)).thenReturn(List.of(app1, app2, app3));

    GitHubApp result = service.select(edgeCaseId);
    assertThat(result).isNotNull();
  }

  @Test
  public void select_differentOwners_canSelectDifferentApps() {
    GitHubApp app1 = createApp("app-1");
    GitHubApp app2 = createApp("app-2");
    List<GitHubApp> apps = List.of(app1, app2);
    when(selectionCache.get(anyString())).thenReturn(null);
    when(gitHubAppDAO.getNearestGitHubApps(anyString())).thenReturn(apps);

    boolean foundDifferent = false;
    GitHubApp firstResult = service.select("owner-0");
    for (int i = 1; i < 100; i++) {
      GitHubApp result = service.select("owner-" + i);
      if (!result.getId().equals(firstResult.getId())) {
        foundDifferent = true;
        break;
      }
    }
    assertThat(foundDifferent).isTrue();
  }

  private GitHubApp createApp(String id) {
    GitHubApp app = new GitHubApp();
    app.setId(id);
    return app;
  }
}
