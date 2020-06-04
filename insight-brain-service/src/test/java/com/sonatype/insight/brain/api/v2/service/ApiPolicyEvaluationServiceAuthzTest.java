/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationPolicyEvaluationsDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiPolicyEvaluationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiPolicyEvaluationService apiPolicyEvaluationService;

  @Test
  public void testGetApplicationEvaluations_Authorized() throws IOException, URISyntaxException {
    grantReadPermission(app.getId());

    ApiApplicationPolicyEvaluationsDTO evaluations = apiPolicyEvaluationService.getAllPolicyEvaluations(app.getId());

    assertThat(evaluations.applicationId).isEqualTo(app.getId());
    assertThat(evaluations.policyEvaluations).hasSize(0);
  }

  @Test
  public void testGetApplicationEvaluations_AuthorizedOrg() throws IOException, URISyntaxException {
    grantReadPermission(org.getId());

    ApiApplicationPolicyEvaluationsDTO evaluations = apiPolicyEvaluationService.getAllPolicyEvaluations(app.getId());

    assertThat(evaluations.applicationId).isEqualTo(app.getId());
    assertThat(evaluations.policyEvaluations).hasSize(0);
  }

  @Test
  public void testGetPolicies_Unauthenticated() {
    assertThatThrownBy(() -> apiPolicyEvaluationService.getAllPolicyEvaluations(app.getId())).isInstanceOf(
        UnauthenticatedException.class);
  }

  @Test
  public void testGetPolicies_UnauthorizedButAuthenticated() {
    login();
    assertThatThrownBy(() -> apiPolicyEvaluationService.getAllPolicyEvaluations(app.getId())).isInstanceOf(
        UnauthorizedException.class);
  }
}
