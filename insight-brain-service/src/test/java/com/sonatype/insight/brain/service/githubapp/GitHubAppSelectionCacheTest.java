/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GitHubAppSelectionCacheTest
{
  private GitHubAppSelectionCache selectionCache;

  @BeforeEach
  public void setUp() {
    selectionCache = new GitHubAppSelectionCache();
  }

  @Test
  public void get_returnsCachedValue() {
    GitHubApp app = new GitHubApp();
    app.setId("app-1");
    selectionCache.put("owner-1", Optional.of(app));

    assertThat(selectionCache.get("owner-1")).hasValue(app);
  }

  @Test
  public void get_returnsNullOnMiss() {
    assertThat(selectionCache.get("unknown-owner")).isNull();
  }

  @Test
  public void get_returnsEmptyOptionalForNegativeCache() {
    selectionCache.put("owner-1", Optional.empty());

    assertThat(selectionCache.get("owner-1")).isEmpty();
  }

  @Test
  public void invalidateAll_clearsAllEntries() {
    GitHubApp app = new GitHubApp();
    app.setId("app-1");
    selectionCache.put("owner-1", Optional.of(app));
    selectionCache.put("owner-2", Optional.of(app));

    selectionCache.invalidateAll();

    assertThat(selectionCache.get("owner-1")).isNull();
    assertThat(selectionCache.get("owner-2")).isNull();
  }

}
