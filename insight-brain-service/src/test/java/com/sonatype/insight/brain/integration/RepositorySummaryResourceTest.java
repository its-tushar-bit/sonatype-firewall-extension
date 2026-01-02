/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class RepositorySummaryResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RepositorySummaryResource.RESOURCE_PATH);
  }

  @Test
  public void testGetRepositories() throws Exception {
    Repository repository = tempEntity.newRepository();

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    RepositorySummary[] repositorySummaries = response.getBody(RepositorySummary[].class);
    assertThat(repositorySummaries).hasSize(1);
    assertThat(repositorySummaries[0].id).isEqualTo(repository.getId());
    assertThat(repositorySummaries[0].name).isEqualTo(repository.getName());
  }
}
