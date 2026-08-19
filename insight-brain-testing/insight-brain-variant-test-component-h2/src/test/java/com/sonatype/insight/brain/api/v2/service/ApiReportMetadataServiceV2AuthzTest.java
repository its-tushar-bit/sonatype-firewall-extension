/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class ApiReportMetadataServiceV2AuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiReportMetadataServiceV2 metadataService;

  @Test
  public void getMetadata_Anon() throws Exception {
    // Expect: getMetadata to throw UnauthenticatedException when not logged in
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> metadataService.getMetadata(app, "irrelevant-scan-id"));
  }

  @Test
  public void getMetadata_Unauthorized() throws Exception {
    // Given: logged in but no READ permission
    login();

    // Expect: getMetadata to throw UnauthorizedException
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> metadataService.getMetadata(app, "irrelevant-scan-id"));
  }

  @Test
  public void getMetadata_Authorized() throws Exception {
    // Given: logged in with READ permission
    grantReadPermission(app.getId());

    // Create a policy evaluation
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test-scan");

    // Expect: getMetadata to throw RuntimeException (scan not found - proves authz passed)
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> metadataService.getMetadata(app, "non-existent-scan"));
  }

  @Test
  public void getMetadata_AuthorizedScanExists() throws Exception {
    // Given: logged in with READ permission
    grantReadPermission(app.getId());

    // And: policy evaluation exists
    String scanId = "authorized-scan";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    // Expect: getMetadata returns successfully (no exception)
    metadataService.getMetadata(app, scanId);
  }

  @Test
  public void getMetadata_Hrc_Anon() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> metadataService.getMetadata(hrc, "irrelevant-scan-id"));
  }

  @Test
  public void getMetadata_Hrc_Unauthorized() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> metadataService.getMetadata(hrc, "irrelevant-scan-id"));
  }

  @Test
  public void getMetadata_Hrc_AuthorizedScanExists() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    grantReadPermission(hrc.getId());

    String scanId = "hrc-authorized-scan";
    tempEntity.newPolicyEvaluation(hrc.getId(), BuildStageType.ID, scanId);

    metadataService.getMetadata(hrc, scanId);
  }
}
