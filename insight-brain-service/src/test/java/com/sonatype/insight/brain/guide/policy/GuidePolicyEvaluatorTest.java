/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsRequestDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GuidePolicyEvaluatorTest
{
  @Mock
  private ApiComponentDetailsServiceV2 detailsService;

  @Mock
  private ComponentDetailsLoaderFactory loaderFactory;

  @Mock
  private ComponentDetailsLoader loader;

  @Mock
  private ComponentPolicyEvaluator policyEvaluator;

  @Mock
  private OwnerDAO ownerDAO;

  @Mock
  private PolicyDAO policyDAO;

  private GuidePolicyEvaluator underTest;

  @Before
  public void setUp() {
    when(loaderFactory.newInstance(any())).thenReturn(loader);
    when(ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID)).thenReturn(EvaluatorFixtures.rootOrg());
    when(policyDAO.getApplicableByOwnerIdWithHierarchy(Organization.ROOT_ORGANIZATION_ID))
        .thenReturn(List.of());
    underTest = new GuidePolicyEvaluator(
        detailsService, loaderFactory, policyEvaluator, ownerDAO, policyDAO);
  }

  @Test
  public void emptyPurls_returnsEmptyMap_andDoesNotCallEvaluator() {
    Map<String, GuidePolicyCompliance> result = underTest.evaluate(List.of());
    assertThat(result).isEmpty();
    verify(policyEvaluator, never()).evaluate(any(), any(), any(), any(), eq(false));
  }

  @Test
  public void multiplePurls_singleHdsCallAndSingleDroolsCall() {
    when(detailsService.getComponentDetailsListFromHds(
        any(ApiComponentDetailsRequestDTOV2.class),
        eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION)))
            .thenReturn(EvaluatorFixtures.evaluationDataFor("pkg:maven/org.example/lib@1.0",
                "pkg:npm/lodash@4.17.21"));
    when(loader.augmentComponentDetails(any())).thenAnswer(EvaluatorFixtures::componentForFirstArg);
    when(policyEvaluator.evaluate(eq(Organization.ROOT_ORGANIZATION_ID), any(Stage.class),
        any(), any(), eq(false)))
            .thenReturn(new PolicyResults());

    Map<String, GuidePolicyCompliance> result = underTest.evaluate(List.of(
        "pkg:maven/org.example/lib@1.0?type=jar",
        "pkg:npm/lodash@4.17.21"));

    assertThat(result).containsOnlyKeys(
        "pkg:maven/org.example/lib@1.0?type=jar",
        "pkg:npm/lodash@4.17.21");
    assertThat(result.get("pkg:maven/org.example/lib@1.0?type=jar").compliant()).isTrue();
    verify(detailsService, times(1)).getComponentDetailsListFromHds(
        any(ApiComponentDetailsRequestDTOV2.class), eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION));
    verify(policyEvaluator, times(1)).evaluate(any(), any(Stage.class), any(), any(), eq(false));
  }

  @Test
  public void scopedNpmPurl_returnsKeyedByCanonicalisedForm() {
    // PackageURL.canonicalize() encodes '@' in namespace as '%40' but keeps '/' as '/'
    // (namespace and name are separate PURL fields joined by '/').
    String canonical = "pkg:npm/%40types/node@25.9.2";
    when(detailsService.getComponentDetailsListFromHds(
        any(ApiComponentDetailsRequestDTOV2.class), eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION)))
            .thenReturn(EvaluatorFixtures.evaluationDataFor(canonical));
    when(loader.augmentComponentDetails(any())).thenAnswer(EvaluatorFixtures::componentForFirstArg);
    when(policyEvaluator.evaluate(any(), any(Stage.class), any(), any(), eq(false)))
        .thenReturn(new PolicyResults());

    Map<String, GuidePolicyCompliance> result = underTest.evaluate(List.of("pkg:npm/@types/node@25.9.2"));

    assertThat(result).containsOnlyKeys(canonical);
  }

  @Test
  public void evaluatorThrows_returnsEmptyMap() {
    when(detailsService.getComponentDetailsListFromHds(any(ApiComponentDetailsRequestDTOV2.class),
        eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION)))
            .thenThrow(new RuntimeException("hds offline"));

    Map<String, GuidePolicyCompliance> result = underTest.evaluate(List.of("pkg:maven/g/n@1.0?type=jar"));

    assertThat(result).isEmpty();
  }

  @Test
  public void unknownOwner_returnsEmptyMap() {
    when(ownerDAO.getById("unknown-owner-id")).thenReturn(null);
    Map<String, GuidePolicyCompliance> result =
        underTest.evaluate(List.of("pkg:maven/g/n@1.0"), "unknown-owner-id", new Stage(Stage.ID_RELEASE));
    assertThat(result).isEmpty();
  }

  @Test
  public void overrideOwnerAndStage_passedThrough() {
    when(ownerDAO.getById("custom-org")).thenReturn(EvaluatorFixtures.org("custom-org"));
    when(policyDAO.getApplicableByOwnerIdWithHierarchy("custom-org")).thenReturn(List.of());
    when(detailsService.getComponentDetailsListFromHds(any(ApiComponentDetailsRequestDTOV2.class),
        eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION)))
            .thenReturn(EvaluatorFixtures.evaluationDataFor("pkg:maven/g/n@1.0"));
    when(loader.augmentComponentDetails(any())).thenAnswer(EvaluatorFixtures::componentForFirstArg);
    ArgumentCaptor<Stage> stageCaptor = ArgumentCaptor.forClass(Stage.class);
    when(policyEvaluator.evaluate(eq("custom-org"), stageCaptor.capture(), any(), any(), eq(false)))
        .thenReturn(new PolicyResults());

    underTest.evaluate(List.of("pkg:maven/g/n@1.0?type=jar"), "custom-org", new Stage("source"));

    assertThat(stageCaptor.getValue().getStageTypeId()).isEqualTo("source");
  }
}
