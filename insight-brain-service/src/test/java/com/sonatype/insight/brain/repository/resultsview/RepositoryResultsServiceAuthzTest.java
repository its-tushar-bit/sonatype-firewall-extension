/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.resultsview;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

public class RepositoryResultsServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RepositoryResultsService repositoryResultsService;

  private Repository repository;

  private RepositoryResultsDetailsRequestDto detailsRequest;

  @Before
  public void setup() {
    repository = tempEntity.newRepository();
    detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
  }

  @Test
  public void testGetDetails_Authorized() {
    grantReadPermission(repository.getId());
    repositoryResultsService.getDetails(repository.getId(), detailsRequest);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetDetails_Unauthorized() {
    login();
    repositoryResultsService.getDetails(repository.getId(), detailsRequest);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetDetails_Unauthenticated() {
    repositoryResultsService.getDetails(repository.getId(), detailsRequest);
  }
}
