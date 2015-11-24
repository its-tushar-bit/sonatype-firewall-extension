/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RepositoryResourceTest
    extends AbstractResourceTest
{
  private Repository repo;

  @Before
  public void setup() {
    repo = tempEntity.newRepository();
  }

  @Test
  public void testGetRepository() throws Exception {
    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH).parameter(repo.getId()).get();
    assertResponseStatus(200, response);
    Repository actual = response.getBody(Repository.class);

    assertThat(actual.getId(), is(repo.getId()));
    assertThat(actual.getPublicId(), is(repo.getPublicId()));
  }
}
