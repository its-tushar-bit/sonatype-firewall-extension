/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.Test;

public class RepositorySummaryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RepositorySummaryService service;

  @Test
  public void testGetApplications_Authorized() {
    grantEvaluateComponentPermission(repository.getId());
    List<RepositorySummary> result = service.getRepositories();
    assertThat(result).extracting(repo -> repo.id).containsExactly(repository.getId());
  }

  @Test
  public void testGetApplications_Unauthorized() {
    login();
    List<RepositorySummary> result = service.getRepositories();
    assertThat(result).isEmpty();
  }
}
