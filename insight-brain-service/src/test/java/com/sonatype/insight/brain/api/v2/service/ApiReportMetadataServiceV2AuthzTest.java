/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiReportMetadataServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiReportMetadataServiceV2 metadataService;

  @Test(expected = UnauthenticatedException.class)
  public void getMetadata_Anon() throws Exception {
    // Expect: getMetadata to throw UnauthenticatedException when not logged in
    metadataService.getMetadata(app.getPublicId(), "irrelevant-scan-id");
  }

  @Test(expected = UnauthorizedException.class)
  public void getMetadata_Unauthorized() throws Exception {
    // Given: logged in but no READ permission
    login();

    // Expect: getMetadata to throw UnauthorizedException
    metadataService.getMetadata(app.getPublicId(), "irrelevant-scan-id");
  }

  @Test(expected = RuntimeException.class)
  public void getMetadata_Authorized() throws Exception {
    // Given: logged in with READ permission
    grantReadPermission(app.getId());

    // Create a policy evaluation
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test-scan");

    // Expect: getMetadata to throw RuntimeException (scan not found - proves authz passed)
    metadataService.getMetadata(app.getPublicId(), "non-existent-scan");
  }

  @Test
  public void getMetadata_AuthorizedScanExists() throws Exception {
    // Given: logged in with READ permission
    grantReadPermission(app.getId());

    // And: policy evaluation exists
    String scanId = "authorized-scan";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    // Expect: getMetadata returns successfully (no exception)
    metadataService.getMetadata(app.getPublicId(), scanId);
  }
}
