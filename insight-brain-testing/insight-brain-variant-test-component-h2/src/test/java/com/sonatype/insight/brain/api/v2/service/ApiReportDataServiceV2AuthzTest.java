/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiReportDataServiceV2AuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiReportDataServiceV2 reportDataService;

  @Test
  public void getRawData_Anon() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> reportDataService.getRawData(app.getPublicId(), "irrelevant"));
  }

  @Test
  public void getRawData_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> reportDataService.getRawData(app.getPublicId(), "irrelevant"));
  }

  @Test
  public void getRawData_Authorized() throws Exception {
    grantReadPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> reportDataService.getRawData(app.getPublicId(), "irrelevant"));
  }

  @Test
  public void getDependencyTree_Anon() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> reportDataService.getDependencyTree(app.getPublicId(), "irrelevant"));
  }

  @Test
  public void getDependencyTree_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> reportDataService.getDependencyTree(app.getPublicId(), "irrelevant"));
  }

  @Test
  public void getDependencyTree_Authorized() throws Exception {
    grantReadPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> reportDataService.getDependencyTree(app.getPublicId(), "irrelevant"));
  }

  @Test
  public void testGetPolicyViolations_Anon() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> reportDataService.getPolicyViolationsData(app.getPublicId(), "irrelevant", false));
  }

  @Test
  public void testGetPolicyViolations_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> reportDataService.getPolicyViolationsData(app.getPublicId(), "irrelevant", false));
  }

  @Test
  public void testGetPolicyViolations_Authorized() throws Exception {
    grantReadPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> reportDataService.getPolicyViolationsData(app.getPublicId(), "irrelevant", false));
  }

  @Test
  public void getRawData_Hrc_Unauthenticated() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    assertThrows(UnauthenticatedException.class,
        () -> reportDataService.getRawData(hrc, "irrelevant", false));
  }

  @Test
  public void getRawData_Hrc_Unauthorized() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    login();
    assertThrows(UnauthorizedException.class,
        () -> reportDataService.getRawData(hrc, "irrelevant", false));
  }

  @Test
  public void getDependencyTree_Hrc_Unauthenticated() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    assertThrows(UnauthenticatedException.class,
        () -> reportDataService.getDependencyTree(hrc, "irrelevant"));
  }

  @Test
  public void getDependencyTree_Hrc_Unauthorized() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    login();
    assertThrows(UnauthorizedException.class,
        () -> reportDataService.getDependencyTree(hrc, "irrelevant"));
  }

  @Test
  public void testGetPolicyViolations_Hrc_Unauthenticated() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    assertThrows(UnauthenticatedException.class,
        () -> reportDataService.getPolicyViolationsData(hrc, "irrelevant", false, null, null));
  }

  @Test
  public void testGetPolicyViolations_Hrc_Unauthorized() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    login();
    assertThrows(UnauthorizedException.class,
        () -> reportDataService.getPolicyViolationsData(hrc, "irrelevant", false, null, null));
  }

  @Test
  public void getRawData_Hrc_Authorized() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    grantReadPermission(hrc.getId());
    assertThrows(NotFoundException.class,
        () -> reportDataService.getRawData(hrc, "irrelevant", false));
  }

  @Test
  public void getDependencyTree_Hrc_Authorized() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    grantReadPermission(hrc.getId());
    assertThrows(NotFoundException.class,
        () -> reportDataService.getDependencyTree(hrc, "irrelevant"));
  }

  @Test
  public void testGetPolicyViolations_Hrc_Authorized() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    grantReadPermission(hrc.getId());
    assertThrows(NotFoundException.class,
        () -> reportDataService.getPolicyViolationsData(hrc, "irrelevant", false, null, null));
  }
}
