/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiEnhancedPolicyViolationDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.Sets;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiPolicyViolationServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String ORG_POLICY_NAME1 = "org-policy1";

  private static final String APP_POLICY_NAME1 = "app-policy1";

  private static final String ORG_POLICY_NAME2 = "org-policy2";

  private static final String APP_POLICY_NAME2 = "app-policy2";

  private static final String PACKAGE_URL = "pkg:maven/g1/a1@v1";

  @Inject
  private ApiPolicyViolationServiceV2 apiPolicyViolationService;

  private Policy orgPolicy;

  private PolicyEvaluation pe1App1;

  private PolicyViolation pv1App1;

  @Before
  public void setUpPolicyViolations() {
    orgPolicy = tempEntity.newPolicy(org.getId(), ORG_POLICY_NAME1);
    tempEntity.newPolicy(app.getId(), APP_POLICY_NAME1);

    // One policy violation for app1
    pe1App1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    pv1App1 = tempEntity.newPolicyViolation(pe1App1, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    tempEntity.newPolicy(org2.getId(), ORG_POLICY_NAME2);
    Policy app2Policy = tempEntity.newPolicy(app2.getId(), APP_POLICY_NAME2);

    // One policy violation for app2
    PolicyEvaluation pe1App2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1App2");
    tempEntity.newPolicyViolation(pe1App2, app2Policy, "g2", "a2", "v2", "h2", "r2");
  }

  @Test
  public void testGetPolicyViolations_AuthorizedOneApp() {
    grantReadPermission(app.getId());

    Set<String> policyIds = Sets.newHashSet(orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertThat(apiApplicationViolationDTO.application).isNotNull();
    assertThat(apiApplicationViolationDTO.application.id).isEqualTo(app.getId());
    assertThat(apiApplicationViolationDTO.application.name).isEqualTo(app.getName());
    assertThat(apiApplicationViolationDTO.application.publicId).isEqualTo(app.getPublicId());
    assertThat(apiApplicationViolationDTO.application.contactUserName).isEqualTo(app.getContactInternalName());
    assertThat(apiApplicationViolationDTO.application.organizationId).isEqualTo(app.getOrganizationId());

    assertThat(apiApplicationViolationDTO.policyViolations).hasSize(1);
    ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO = apiApplicationViolationDTO.policyViolations.get(0);
    assertThat(apiPolicyViolationDTO.policyId).isEqualTo(pv1App1.getPolicyId());
    assertThat(apiPolicyViolationDTO.policyName).isEqualTo(pv1App1.getPolicyName());
    assertThat(apiPolicyViolationDTO.policyViolationId).isEqualTo(pv1App1.getId());
    assertThat(apiPolicyViolationDTO.threatLevel).isEqualTo(pv1App1.getThreatLevel());
    assertThat(apiPolicyViolationDTO.reportUrl)
        .isEqualTo("ui/links/application/" + app.getPublicId() + "/report/" + pe1App1.getScanId());
    assertThat(apiPolicyViolationDTO.stageId).isEqualTo(pe1App1.getStageTypeId());
    assertThat(apiPolicyViolationDTO.component.hash).isEqualTo(pv1App1.getHash());
    assertThat(apiPolicyViolationDTO.component.proprietary).isFalse();
    assertThat(apiPolicyViolationDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(pv1App1.getComponentIdentifier());
    assertThat(apiPolicyViolationDTO.component.packageUrl).isEqualTo(PACKAGE_URL);
    assertThat(apiPolicyViolationDTO.component.displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(pv1App1.getComponentIdentifier()).toString());

    assertThat(apiPolicyViolationDTO.constraintViolations).hasSize(1);
    ApiConstraintViolationDTO apiConstraintViolationDTO = apiPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId).isEqualTo(pv1App1.getConstraintFacts().get(0).getConstraintId());
    assertThat(apiConstraintViolationDTO.constraintName)
        .isEqualTo(pv1App1.getConstraintFacts().get(0).getConstraintName());

    assertThat(apiConstraintViolationDTO.reasons).hasSize(1);
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason)
        .isEqualTo(pv1App1.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());
  }

  @Test
  public void testGetPolicyViolations_Unauthenticated() {
    assertEmptyWhenUnauthorizedOrAuthenticated();
  }

  @Test
  public void testGetPolicyViolations_UnauthorizedButAuthenticated() {
    login();
    assertEmptyWhenUnauthorizedOrAuthenticated();
  }

  private void assertEmptyWhenUnauthorizedOrAuthenticated() {
    Set<String> policyIds = Sets.newHashSet(orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);
    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).isEmpty();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetTransitivePolicyViolations_Unauthenticated_Application() {
    apiPolicyViolationService.getTransitivePolicyViolations(app.getType(), app.getPublicId(), null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetTransitivePolicyViolations_Unauthorized_Application() {
    login();
    apiPolicyViolationService.getTransitivePolicyViolations(app.getType(), app.getPublicId(), null, null, null, null);
  }

  @Test
  public void testGetTransitivePolicyViolations_Authorized_Application() {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolations(app.getType(), app.getPublicId(), BuildStageType.ID, null,
            "pkg:maven/g/a@v?type=e", null))
        .withMessageContaining("Component not found.");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetTransitivePolicyViolations_Unauthenticated_Organization() {
    apiPolicyViolationService.getTransitivePolicyViolations(org.getType(), org.getPublicId(), null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetTransitivePolicyViolations_Unauthorized_Organization() {
    login();
    apiPolicyViolationService.getTransitivePolicyViolations(org.getType(), org.getPublicId(), null, null, null, null);
  }

  @Test
  public void testGetTransitivePolicyViolations_Authorized_Organization() {
    grantReadPermission(org.getId());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolations(org.getType(), org.getPublicId(), BuildStageType.ID, null,
            "pkg:maven/g/a@v?type=e", null))
        .withMessageContaining("Component not found.");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetTransitivePolicyViolations_Unauthenticated_RootOrganization() {
    apiPolicyViolationService.getTransitivePolicyViolations(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetTransitivePolicyViolations_Unauthorized_RootOrganization() {
    login();
    apiPolicyViolationService.getTransitivePolicyViolations(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        null, null, null, null);
  }

  @Test
  public void testGetTransitivePolicyViolations_Authorized_RootOrganization() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolations(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID,
            null, "pkg:maven/g/a@v?type=e", null))
        .withMessageContaining("Component not found.");
  }
}
