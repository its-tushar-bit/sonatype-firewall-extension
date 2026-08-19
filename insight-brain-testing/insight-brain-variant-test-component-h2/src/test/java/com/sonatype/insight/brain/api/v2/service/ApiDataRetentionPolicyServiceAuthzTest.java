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
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiDataRetentionPolicyServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiDataRetentionPolicyService dataRetentionPolicyService;

  @Test
  public void testGetDataRetentionPolicies_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> dataRetentionPolicyService.getDataRetentionPolicies(org.getId()));
  }

  @Test
  public void testGetParentDataRetentionPolicies_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> dataRetentionPolicyService.getParentDataRetentionPolicies(org.getId()));
  }

  @Test
  public void testGetDataRetentionPolicies_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> dataRetentionPolicyService.getDataRetentionPolicies(org.getId()));
  }

  @Test
  public void testGetParentDataRetentionPolicies_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> dataRetentionPolicyService.getParentDataRetentionPolicies(org.getId()));
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

  @Test
  public void testSetDataRetentionPolicies_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), new ApiDataRetentionPoliciesDTO()));
  }

  @Test
  public void testSetDataRetentionPolicies_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), new ApiDataRetentionPoliciesDTO()));
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
