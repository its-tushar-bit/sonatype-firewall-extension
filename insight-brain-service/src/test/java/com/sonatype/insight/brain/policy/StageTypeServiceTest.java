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

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;

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
   * Explicit guard: HOSTED must be absent from every classic context. The other
   * testGetLicensedStageTypes_* tests assert hardcoded stage lists that don't include
   * HOSTED, which incidentally proves the filter today — but if a future change
   * accidentally drops the {@code !HostedStageType.ID.equals(...)} guard from one of the
   * filters in StageTypeService, this is the test that makes the regression obvious.
   */
  @Test
  public void testGetLicensedStageTypes_HostedExcludedFromAllClassicContexts() {
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
}
