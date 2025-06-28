/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

public class RepositoryContainerImageServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RepositoryContainerImageService service;

  @Test(expected = UnauthenticatedException.class)
  public void testIsContainerImageQuarantined_unauthenticated() {
    service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "test-image");
  }

  @Test(expected = UnauthorizedException.class)
  public void testIsContainerImageQuarantined_unauthorized() {
    login();
    service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "test-image");
  }

  @Test(expected = NotFoundException.class)
  public void testIsContainerImageQuarantined_authorized() {
    repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    grantEvaluateComponentPermission(repository.getId());

    service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "test-image");
  }
}
