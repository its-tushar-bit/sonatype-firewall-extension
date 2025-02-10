/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresTest
public class PostgresDashboardViolationRiskServiceTest
    extends AbstractDashboardViolationRiskServiceTest
{
  @Inject
  private PostgresDashboardViolationRiskService dashboardViolationRiskService;

  @Override
  protected DashboardViolationRiskService getDashboardViolationRiskService() {
    return dashboardViolationRiskService;
  }

  // Before constraint facts migration, we expect policy violations with the same details except constraint facts
  // to be grouped together
  @Test
  public void testGet_HandlesPolicyViolations_BeforeConstraintFactsMigration_MultipleSame() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    Condition ageCondition1 = new Condition(AgeInDaysConditionType.ID, "older than", "1");
    Condition ageCondition2 = new Condition(AgeInDaysConditionType.ID, "younger than", "999999");
    Constraint constraint1 = new Constraint(null, "constraintName1", LogicalOperator.AND);
    constraint1.setConditions(Arrays.asList(ageCondition1, ageCondition2));
    Condition relativePopularityCondition1 = new Condition(RelativePopularityConditionType.ID, ">=", "0");
    Condition relativePopularityCondition2 = new Condition(RelativePopularityConditionType.ID, "<=", "100");
    Constraint constraint2 = new Constraint(null, "constraintName2", LogicalOperator.AND);
    constraint2.setConditions(Arrays.asList(relativePopularityCondition1, relativePopularityCondition2));
    Policy policy = new Policy();
    policy.setName("policyName-" + TemporaryEntity.uuid());
    policy.setThreatLevel(10);
    policy.setOwnerId(application.getId());
    policy.setConstraints(List.of(constraint1, constraint2));
    policyDAO.insert(policy);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    String hash = "hash";

    Date date = new Date();
    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId1", DateUtils.addDays(date, -1));
    ConstraintFact constraintFact1 = new ConstraintFact(constraint1.getId(), constraint1.getName(), constraint1
        .getOperator().name());
    String conditionTypeId1 = ageCondition1.getConditionTypeId();
    ConditionFact conditionFact1 = new ConditionFact(conditionTypeId1, 0 /* conditionIndex */, "summary1", "reason1");
    constraintFact1.addConditionFact(conditionFact1);
    PolicyViolation unmigratedPolicyViolation1 =
        new PolicyViolation(policyEvaluation1, policy, hash, componentIdentifier,
            Collections.singletonList(constraintFact1), "filename1");
    policyViolationDAO.insert(unmigratedPolicyViolation1);
    unmigratedPolicyViolation1 = unmigratePolicyViolation(unmigratedPolicyViolation1);

    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId2", date);
    ConstraintFact constraintFact2 = new ConstraintFact(constraint1.getId(), constraint1.getName(), constraint1
        .getOperator().name());
    String conditionTypeId2 = relativePopularityCondition1.getConditionTypeId();
    ConditionFact conditionFact2 = new ConditionFact(conditionTypeId2, 0 /* conditionIndex */, "summary2", "reason2");
    constraintFact2.addConditionFact(conditionFact2);
    PolicyViolation unmigratedPolicyViolation2 =
        new PolicyViolation(policyEvaluation2, policy, hash, componentIdentifier,
            Collections.singletonList(constraintFact2), "filename2");
    policyViolationDAO.insert(unmigratedPolicyViolation2);
    unmigratePolicyViolation(unmigratedPolicyViolation2);

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL",
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);

    assertThat(result.dashboardResults).hasSize(4);
    assertThat(result.hasNextPage).isEqualTo(false);

    DashboardViolationRiskDTO riskDTO0 = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO0, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());

    DashboardViolationRiskDTO riskDTO1 = result.dashboardResults.get(1);
    assertDashboardViolationRiskDTO(riskDTO1, app1, org1, app1PolicyViolation, app1PolicyEvaluation.getTime());

    DashboardViolationRiskDTO riskDTO2 = result.dashboardResults.get(2);
    assertDashboardViolationRiskDTO(riskDTO2, app1, org1, orgPolicyViolation, app1PolicyEvaluation.getTime());

    DashboardViolationRiskDTO riskDTO3 = result.dashboardResults.get(3);
    assertDashboardViolationRiskDTO(riskDTO3, application, organization, unmigratedPolicyViolation1,
        policyEvaluation1.getTime());
  }
}
