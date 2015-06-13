/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class LicensedStagesResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetLicenseStages() throws Exception {
    HttpResponse response = restRequest().path(LicensedStagesResource.SERVICE_PATH).get();
    assertResponseStatus(200, response);
    Stage[] stages = fromJson(response, Stage[].class);
    assertStages(stages);
  }

  private void assertStages(final Stage[] stages) {
    // This rest call will return the stages in the following predefined order
    assertThat(stages.length, is(5));
    assertThat(stages[0].getStageTypeId(), is(StageTypes.DEVELOP.getId()));
    assertThat(stages[0].getStageName(), is(StageTypes.DEVELOP.getName()));
    assertThat(stages[1].getStageTypeId(), is(StageTypes.BUILD.getId()));
    assertThat(stages[1].getStageName(), is(StageTypes.BUILD.getName()));
    assertThat(stages[2].getStageTypeId(), is(StageTypes.STAGE_RELEASE.getId()));
    assertThat(stages[2].getStageName(), is(StageTypes.STAGE_RELEASE.getName()));
    assertThat(stages[3].getStageTypeId(), is(StageTypes.RELEASE.getId()));
    assertThat(stages[3].getStageName(), is(StageTypes.RELEASE.getName()));
    assertThat(stages[4].getStageTypeId(), is(StageTypes.OPERATE.getId()));
    assertThat(stages[4].getStageName(), is(StageTypes.OPERATE.getName()));
  }
}
