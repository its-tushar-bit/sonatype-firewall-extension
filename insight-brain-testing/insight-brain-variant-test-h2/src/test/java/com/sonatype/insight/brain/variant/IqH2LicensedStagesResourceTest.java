/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.LicensedStagesResource;
import com.sonatype.insight.brain.policy.StageTypeService;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2LicensedStagesResourceTest
{
  private IqTestContext ctx;

  @Test
  void testGetLicenseStages() throws Exception {
    HttpResponse response = ctx.restRequest().path(LicensedStagesResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    Stage[] stages = response.getBody(Stage[].class);
    assertStages(stages, StageTypes.DEVELOP, StageTypes.SOURCE, StageTypes.BUILD, StageTypes.STAGE_RELEASE,
        StageTypes.RELEASE, StageTypes.OPERATE);
  }

  @Test
  void testGetLicenseStages_All() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(LicensedStagesResource.RESOURCE_PATH)
        .query("context", StageTypeService.ALL_CONTEXT)
        .get();
    ctx.assertResponseStatus(200, response);
    Stage[] stages = response.getBody(Stage[].class);
    assertStages(stages, StageTypes.PROXY, StageTypes.DEVELOP, StageTypes.SOURCE, StageTypes.BUILD,
        StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE);
  }

  private void assertStages(final Stage[] actualStages, final StageType... expectedStages) {
    assertThat(actualStages).hasSameSizeAs(expectedStages);
    for (int i = 0; i < expectedStages.length; i++) {
      assertThat(actualStages[i].getStageTypeId()).isEqualTo(expectedStages[i].getId());
      assertThat(actualStages[i].getStageName()).isEqualTo(expectedStages[i].getName());
    }
  }
}
