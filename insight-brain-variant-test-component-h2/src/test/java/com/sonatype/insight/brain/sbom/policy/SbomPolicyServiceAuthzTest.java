/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.policy;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Authorization coverage for the {@link com.sonatype.insight.brain.model.Owner}-typed overloads on
 * {@link SbomPolicyService} added in CLM-44276. Mirrors {@code ApiReportDataServiceV2AuthzTest}'s
 * three-tier pattern: Anon → Unauthorized → Authorized. The Authorized tier lands in
 * {@link NotFoundException} because no SBOM/report is seeded; that's the point — it proves the
 * {@code @AuthzContext(Key.OWNER)} interceptor resolved the HRC ancestor chain
 * (Repository → RepositoryManager → RepositoryContainer → Organization) correctly and the
 * request reached the service body.
 */
@ComponentH2Test
public class SbomPolicyServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final String SBOM_VERSION = "irrelevant";

  @Inject
  private SbomPolicyService service;

  @Test
  public void getPolicyViolationsReportEntry_Hrc_Anon() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    assertThrows(UnauthenticatedException.class,
        () -> service.getPolicyViolationsReportEntry(hrc, SBOM_VERSION));
  }

  @Test
  public void getPolicyViolationsReportEntry_Hrc_Unauthorized() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    login();
    assertThrows(UnauthorizedException.class,
        () -> service.getPolicyViolationsReportEntry(hrc, SBOM_VERSION));
  }

  @Test
  public void getPolicyViolationsReportEntry_Hrc_Authorized() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    grantReadPermission(hrc.getId());
    assertThrows(NotFoundException.class,
        () -> service.getPolicyViolationsReportEntry(hrc, SBOM_VERSION));
  }

  @Test
  public void getPolicyViolationsJsonNodeByComponentRefOrHash_Hrc_Anon() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    assertThrows(UnauthenticatedException.class,
        () -> service.getPolicyViolationsJsonNodeByComponentRefOrHash(
            hrc, SBOM_VERSION, null, null, null, null, null));
  }

  @Test
  public void getPolicyViolationsJsonNodeByComponentRefOrHash_Hrc_Unauthorized() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    login();
    assertThrows(UnauthorizedException.class,
        () -> service.getPolicyViolationsJsonNodeByComponentRefOrHash(
            hrc, SBOM_VERSION, null, null, null, null, null));
  }

  @Test
  public void getPolicyViolationsJsonNodeByComponentRefOrHash_Hrc_Authorized() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    grantReadPermission(hrc.getId());
    // BadRequestException is thrown by the service body when componentRef/fileCoordinateId/hash are all null.
    // Its presence proves authz passed: the @AuthzContext(Key.OWNER) interceptor resolved the HRC's
    // Repository → RepositoryManager → RepositoryContainer → Organization chain and reached the method body.
    assertThrows(BadRequestException.class,
        () -> service.getPolicyViolationsJsonNodeByComponentRefOrHash(
            hrc, SBOM_VERSION, null, null, null, null, null));
  }
}
