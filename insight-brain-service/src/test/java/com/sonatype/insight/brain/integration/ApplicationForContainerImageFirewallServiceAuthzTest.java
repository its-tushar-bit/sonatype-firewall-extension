/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiVerifyOrCreateApplicationForContainerImageFirewallDTO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationForContainerImageFirewallServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationForContainerImageFirewallService service;

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_authorized() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository proxyRepository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    grantPermission(proxyRepository.getId(), Permission.EVALUATE_COMPONENT);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            proxyRepository.getPublicId(),
            repositoryManager.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    String result = service.verifyOrCreateApplicationForContainerImage(proxyRepository, dto);
    assertThat(result).isNotNull();
  }

  @Test(expected = UnauthorizedException.class)
  public void testVerifyOrCreateApplicationForContainerImage_unauthorized() {
    login();

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            repositoryManager.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    service.verifyOrCreateApplicationForContainerImage(repository, dto);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testVerifyOrCreateApplicationForContainerImage_unauthenticated() {
    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            repositoryManager.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    service.verifyOrCreateApplicationForContainerImage(repository, dto);
  }
}
