/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SloViolationEnricher} hydrates CVE/CVSS fields from a policy violation's constraint facts,
 * exercising the real {@code PolicyViolationDAO.loadConstraintFacts} DB round trip.
 */
public class SloViolationEnricherCveTest
    extends AbstractComponentTest
{
  private static final String SCAN_ID = "scan-1";

  private static final String CVE = "CVE-2021-1234";

  private static final double SEVERITY = 7.5;

  private static final String VECTOR_STRING = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H";

  @Inject
  private SloViolationEnricher enricher;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Test
  public void populatesCveFromConstraintFacts() {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, SCAN_ID);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0.0", null, null);
    List<ConstraintFact> constraintFacts = securityConstraintFacts(CVE, SEVERITY, VECTOR_STRING);

    PolicyViolation violation = new PolicyViolation(evaluation, policy, "hash-1", componentIdentifier,
        constraintFacts, "component.jar");
    violation.setId("pv-" + System.nanoTime());
    policyViolationDAO.insert(violation);

    // Fetch fresh so constraint facts are NOT loaded, proving the enricher's loadConstraintFacts call hydrates them.
    PolicyViolation reloaded = policyViolationDAO.getById(violation.getId());
    assertThat(reloaded.getConstraintFactsId()).isNotNull();
    assertThat(reloaded.constraintFactsAreLoaded()).isFalse();

    List<SloViolation> results =
        enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(reloaded));

    assertThat(results).hasSize(1);
    SloViolation result = results.get(0);
    assertThat(result.vulnerabilityRefId).isEqualTo(CVE);
    assertThat(result.cvssScore).isEqualTo(SEVERITY);
    assertThat(result.cvssVector).isEqualTo("Network");
  }

  @Test
  public void leavesCveNullWhenNoConstraintFacts() {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());

    PolicyViolation violation = new PolicyViolation();
    violation.setId("pv-nofacts-" + System.nanoTime());
    violation.setOwnerId(application.getId());
    violation.setStageTypeId(Stage.ID_RELEASE);
    violation.setPolicyId("policy-1");
    violation.setPolicyName("No facts policy");
    violation.setThreatLevel(5);
    violation.setThreatCategory(PolicyThreatCategory.SECURITY);
    assertThat(violation.getConstraintFactsId()).isNull();

    List<SloViolation> results =
        enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(violation));

    assertThat(results).hasSize(1);
    SloViolation result = results.get(0);
    assertThat(result.vulnerabilityRefId).isNull();
    assertThat(result.cvssScore).isNull();
    assertThat(result.cvssVector).isNull();
  }

  private static List<ConstraintFact> securityConstraintFacts(
      final String refId,
      final Object severity,
      final String vectorString)
  {
    Map<String, Object> triggerData = new LinkedHashMap<>();
    triggerData.put("refId", refId);
    triggerData.put("severity", severity);
    triggerData.put("vectorString", vectorString);

    ConditionTrigger trigger = new ConditionTrigger(0, triggerData);
    String triggerJson = JsonUtils.writeUnformatted(trigger);

    ConditionFact conditionFact = new ConditionFact("SecurityVulnerabilitySeverity", 0, "SECURITY",
        "Security violation");
    conditionFact.setTriggerJson(triggerJson);

    ConstraintFact constraintFact = new ConstraintFact("constraint-1", "Security constraint", "AND");
    constraintFact.addConditionFact(conditionFact);

    return Collections.singletonList(constraintFact);
  }
}
