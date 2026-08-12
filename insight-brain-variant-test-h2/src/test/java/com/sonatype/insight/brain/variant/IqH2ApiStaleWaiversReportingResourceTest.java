/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiStaleWaiversReportingResource;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiversResponseDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiStaleWaiversReportingResourceTest
{
  private IqTestContext ctx;

  @Test
  void testGetStaleWaivers() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy orgPolicy = ctx.tempEntity().newPolicy(app.getParentOwnerId());
    ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    ctx.tempEntity().newWaiver(orgPolicy.getId(), app.getParentOwnerId());

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiStaleWaiversReportingResource.PATH)
            .get();

    ctx.assertResponseStatus(200, response);
    ApiStaleWaiversResponseDTO responseDTO = response.getBody(ApiStaleWaiversResponseDTO.class);
    assertThat(responseDTO).isNotNull();
    assertThat(responseDTO.staleWaivers).hasSize(1);
  }

  @Test
  void testGetStaleWaivers_LegacyRepositoryWaiver() throws Exception {
    Date date = new Date();
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(app.getParentOwnerId());
    ConstraintFact constraintFact1 = new ConstraintFact("constraintFact1", "aa c", "OR");
    constraintFact1.addConditionFact(new ConditionFact("MatchState", 0,
        "Match State is exact", "Match State was exact"));
    List<ConstraintFact> constraintFacts1 = Collections.singletonList(constraintFact1);
    Repository repo = ctx.tempEntity().newRepository("repo");
    // legacy waived repo violation does not have these pieces of information
    String legacyWaiverId = null;
    String legacyWaiverComment = null;
    Date legacyWaiverDate = null;

    ctx.tempEntity().newWaiver("h2", policy.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, "repo waiver");

    ctx.tempEntity()
        .newRepositoryPolicyViolation(
            repo.getId(), 6, "pathName1", "hash1", constraintFacts1, true,
            "actionId1", policy.getId(), policy.getName(), null, date,
            legacyWaiverId, legacyWaiverComment, legacyWaiverDate);

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiStaleWaiversReportingResource.PATH)
            .get();

    ctx.assertResponseStatus(409, response);
  }
}
