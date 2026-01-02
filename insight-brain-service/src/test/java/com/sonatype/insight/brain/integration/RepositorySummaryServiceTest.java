/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class RepositorySummaryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RepositorySummaryService service;

  @Test
  public void testGetRepository() {
    Repository repo1 = tempEntity.newRepository("C");
    Repository repo2 = tempEntity.newRepository("B");
    Repository repo3 = tempEntity.newRepository("A");

    List<RepositorySummary> repositorySummaryList = service.getRepositories();

    assertThat(repositorySummaryList).hasSize(3);

    // Repositories are sorted by name
    RepositorySummary repositorySummary = repositorySummaryList.get(0);
    assertThat(repositorySummary.id).isEqualTo(repo3.getId());
    assertThat(repositorySummary.name).isEqualTo(repo3.getName());

    repositorySummary = repositorySummaryList.get(1);
    assertThat(repositorySummary.id).isEqualTo(repo2.getId());
    assertThat(repositorySummary.name).isEqualTo(repo2.getName());

    repositorySummary = repositorySummaryList.get(2);
    assertThat(repositorySummary.id).isEqualTo(repo1.getId());
    assertThat(repositorySummary.name).isEqualTo(repo1.getName());
  }
}
