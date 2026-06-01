/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StageTypeServiceTest
    extends AbstractComponentTest
{
  @Inject
  private StageTypeService stageTypeService;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testGetLicensedStageTypes_OrderedByComponentLifecycle() {
    List<StageType> all = new ArrayList<>(StageTypes.getAll());
    Collections.reverse(all);
    testProductLicense.setStageTypes(all);

    assertThat(stageTypeService.getLicensedStageTypes()).containsExactly( //
        StageTypes.PROXY, //
        StageTypes.DEVELOP, //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE, //
        StageTypes.COMPLIANCE);
  }

  @Test
  public void testGetLicensedStageTypes_ContextAll() {
    testProductLicense.setStageTypes(StageTypes.getAll());

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.ALL_CONTEXT)).containsExactly( //
        StageTypes.PROXY, //
        StageTypes.DEVELOP, //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE, //
        StageTypes.COMPLIANCE);
  }

  @Test
  public void testGetLicensedStageTypes_ContextDashboard() {
    testProductLicense.setStageTypes(StageTypes.getAll());

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)).containsExactly( //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE);
  }

  @Test
  public void testGetLicensedStageTypes_ContextCI() {
    testProductLicense.setStageTypes(StageTypes.getAll());

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.CI_CONTEXT)).containsExactly( //
        StageTypes.DEVELOP, //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE);
  }

  @Test
  public void testGetLicensedStageTypes_ContextCli() {
    testProductLicense.setStageTypes(StageTypes.getAll());

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.CLI_CONTEXT)).containsExactly( //
        StageTypes.DEVELOP, //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE);
  }

  @Test
  public void testGetLicensedStageTypes_ContextQa() {
    testProductLicense.setStageTypes(StageTypes.getAll());

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.QA_CONTEXT)).containsExactly( //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE);
  }

  @Test
  public void testGetLicensedStageTypes_ContextRm() {
    testProductLicense.setStageTypes(StageTypes.getAll());

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.RM_CONTEXT)).containsExactly( //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE);
  }

  @Test
  public void testGetLicensedStageTypes_ContextMaven() {
    testProductLicense.setStageTypes(StageTypes.getAll());

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.MAVEN_CONTEXT)).containsExactly( //
        StageTypes.DEVELOP, //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE);
  }

  @Test
  public void testGetLicensedStageTypes_ContextSbom() {
    testProductLicense.setStageTypes(StageTypes.getAll());

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.SBOM_CONTEXT)).containsExactly(
        StageTypes.COMPLIANCE);
  }

  @Test
  public void testGetLicensedStageTypes_ContextLifecycle() {
    testProductLicense.setStageTypes(StageTypes.getAll());

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT)).containsExactly( //
        StageTypes.PROXY, //
        StageTypes.DEVELOP, //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE);
  }

  /**
   * With HOSTED_REPOSITORY_EVALUATION OFF (the default), HOSTED must be absent from every
   * classic context even when the license includes it. The other testGetLicensedStageTypes_*
   * tests assert hardcoded stage lists that don't include HOSTED, which incidentally proves
   * the filter today — but if a future change accidentally drops the gate from one of the
   * filters in StageTypeService, this test makes the regression obvious.
   */
  @Test
  public void testGetLicensedStageTypes_HostedExcludedFromAllClassicContexts_WhenFeatureFlagOff() {
    // Flag default is false (enabledWhenAbsent=false); @After resets to false between tests,
    // so no explicit setEnabled(false) is needed here.
    testProductLicense.setStageTypes(StageTypes.getAll());

    List<String> classicContexts = List.of(
        StageTypeService.ALL_CONTEXT,
        StageTypeService.CI_CONTEXT,
        StageTypeService.CLI_CONTEXT,
        StageTypeService.QA_CONTEXT,
        StageTypeService.RM_CONTEXT,
        StageTypeService.MAVEN_CONTEXT,
        StageTypeService.DASHBOARD_CONTEXT,
        StageTypeService.LIFECYCLE_CONTEXT);

    for (String context : classicContexts) {
      assertThat(stageTypeService.getLicensedStageTypes(context))
          .as("HOSTED must be absent from context: " + context)
          .doesNotContain(StageTypes.HOSTED);
    }
  }

  /**
   * With HOSTED_REPOSITORY_EVALUATION ON, ALL_CONTEXT and LIFECYCLE_CONTEXT must include
   * HOSTED so the policy editor's Actions matrix surfaces a Hosted column for customers
   * who have opted into synchronous hosted-repository enforcement (CLM-39870). Other classic
   * contexts (CI, CLI, QA, RM, Maven, Dashboard) intentionally remain unchanged because
   * HOSTED enforcement is a server-side action, not a developer-tooling stage.
   */
  @Test
  public void testGetLicensedStageTypes_HostedIncludedInAllAndLifecycle_WhenFeatureFlagOn() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    testProductLicense.setStageTypes(StageTypes.getAll());

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.ALL_CONTEXT))
        .as("ALL_CONTEXT must include HOSTED when feature flag on")
        .contains(StageTypes.HOSTED);
    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT))
        .as("LIFECYCLE_CONTEXT must include HOSTED when feature flag on")
        .contains(StageTypes.HOSTED);

    List<String> unaffectedContexts = List.of(
        StageTypeService.CI_CONTEXT,
        StageTypeService.CLI_CONTEXT,
        StageTypeService.QA_CONTEXT,
        StageTypeService.RM_CONTEXT,
        StageTypeService.MAVEN_CONTEXT,
        StageTypeService.DASHBOARD_CONTEXT);
    for (String context : unaffectedContexts) {
      assertThat(stageTypeService.getLicensedStageTypes(context))
          .as("HOSTED must still be absent from context: " + context)
          .doesNotContain(StageTypes.HOSTED);
    }
  }

  @After
  public void resetHostedFeatureFlag() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }
}
