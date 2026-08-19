/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.integration.RepositorySummary;
import com.sonatype.insight.brain.integration.RepositorySummaryResource;
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2RepositorySummaryResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(RepositorySummaryResource.RESOURCE_PATH);
  }

  @Test
  void testGetRepositories() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    RepositorySummary[] repositorySummaries = response.getBody(RepositorySummary[].class);
    assertThat(repositorySummaries).hasSize(1);
    assertThat(repositorySummaries[0].id).isEqualTo(repository.getId());
    assertThat(repositorySummaries[0].name).isEqualTo(repository.getName());
  }
}
