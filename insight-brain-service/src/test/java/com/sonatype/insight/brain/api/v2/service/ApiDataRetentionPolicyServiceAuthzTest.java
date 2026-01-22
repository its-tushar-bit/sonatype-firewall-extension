/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPolicyDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiDataRetentionPolicyServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiDataRetentionPolicyService dataRetentionPolicyService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetDataRetentionPolicies_Unauthenticated() {
    dataRetentionPolicyService.getDataRetentionPolicies(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetParentDataRetentionPolicies_Unauthenticated() {
    dataRetentionPolicyService.getParentDataRetentionPolicies(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetDataRetentionPolicies_Unauthorized() {
    login();
    dataRetentionPolicyService.getDataRetentionPolicies(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetParentDataRetentionPolicies_Unauthorized() {
    login();
    dataRetentionPolicyService.getParentDataRetentionPolicies(org.getId());
  }

  @Test
  public void testGetDataRetentionPolicies_Authorized() {
    grantReadPermission(org.getId());
    dataRetentionPolicyService.getDataRetentionPolicies(org.getId());
  }

  @Test
  public void testGetParentDataRetentionPolicies_Authorized() {
    grantReadPermission(org.getId());
    dataRetentionPolicyService.getParentDataRetentionPolicies(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetDataRetentionPolicies_Unauthenticated() {
    dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), new ApiDataRetentionPoliciesDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetDataRetentionPolicies_Unauthorized() {
    login();
    dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), new ApiDataRetentionPoliciesDTO());
  }

  @Test
  public void testSetDataRetentionPolicies_Authorized() {
    grantWritePermission(org.getId());
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.applicationReports.stages.put(Stage.ID_BUILD, new ApiReportRetentionPolicyDTO());
    dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), dto);
  }
}
