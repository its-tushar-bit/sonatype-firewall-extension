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
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApplicationForContainerImageFirewallServiceAuthzTest
    extends AbstractComponentH2AuthzTest
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

  @Test
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

    assertThrows(UnauthorizedException.class,
        () -> service.verifyOrCreateApplicationForContainerImage(repository, dto));
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_unauthenticated() {
    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            repositoryManager.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    assertThrows(UnauthenticatedException.class,
        () -> service.verifyOrCreateApplicationForContainerImage(repository, dto));
  }
}
