/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RepositorySummaryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RepositorySummaryService service;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    // Need to mock this for telemetry requests, otherwise the real client takes a while to timeout.
    binder.bind(HdsClient.class).toInstance(mock(HdsClient.class));

    // Create another repository only to verify that it's not returned (the user doesn't have the required permission)
    tempEntity.newRepository();
  }

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
