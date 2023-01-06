/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardPolicyWaiverDTOAdapterTest
    extends AbstractComponentTest
{
  private DashboardPolicyWaiverDTOAdapter dtoAdapter;

  private Organization org;

  private Application app;

  private Policy testPolicy;

  private PolicyWaiver testPolicyWaiver;

  private final Map<String, Owner> ownersById = new HashMap<>();

  private final Map<String, Policy> policiesById = new HashMap<>();

  @Before
  public void beforeEach() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication("testApplication", "testApplication", org.getId());
    ownersById.put(app.getId(), app);
    testPolicyWaiver = createPolicyWaiverWithFullDetails();
    policiesById.put(testPolicy.getId(), testPolicy);
  }

  @Test
  public void testPolicyWaiverToDTO_ExcludeDetails() {
    dtoAdapter = new DashboardPolicyWaiverDTOAdapter(policiesById, ownersById, false);
    DashboardPolicyWaiverDTO dto = dtoAdapter.toDto(testPolicyWaiver);

    assertThat(dto.id).isEqualTo(testPolicyWaiver.getId());
    assertThat(dto.threatLevel).isEqualTo(policiesById.get(testPolicyWaiver.getPolicyId()).getThreatLevel());
    assertThat(dto.createTime).isEqualTo(testPolicyWaiver.getCreateTime());
    assertThat(dto.expiryTime).isEqualTo(testPolicyWaiver.getExpiryTime());
    assertThat(dto.policyId).isEqualTo(testPolicyWaiver.getPolicyId());
    assertThat(dto.policyName).isEqualTo(policiesById.get(testPolicyWaiver.getPolicyId()).getName());
    assertThat(dto.ownerId).isEqualTo(testPolicyWaiver.getOwnerId());
    assertThat(dto.ownerName).isEqualTo(ownersById.get(testPolicyWaiver.getOwnerId()).getName());
    assertThat(dto.ownerType).isEqualTo(ownersById.get(testPolicyWaiver.getOwnerId()).getType().toString());
    assertThat(dto.componentMatchStrategy).isEqualTo(testPolicyWaiver.getComponentMatchStrategy());
    assertThat(dto.hash).isEqualTo(testPolicyWaiver.getHash());
    assertThat(dto.componentIdentifier.toComponentIdentifier()).isEqualTo(testPolicyWaiver.getComponentIdentifier());
    assertThat(dto.getDisplayName().toString())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(testPolicyWaiver.getComponentIdentifier()).toString());

    assertThat(dto.comment).isNull();
    assertThat(dto.constraintFacts).isNull();
    assertThat(dto.creatorId).isNull();
    assertThat(dto.creatorName).isNull();
  }

  @Test
  public void testPolicyWaiverToDTO_IncludeDetails() {
    dtoAdapter = new DashboardPolicyWaiverDTOAdapter(policiesById, ownersById, true);
    DashboardPolicyWaiverDTO dto = dtoAdapter.toDto(testPolicyWaiver);

    assertThat(dto.id).isEqualTo(testPolicyWaiver.getId());
    assertThat(dto.threatLevel).isEqualTo(policiesById.get(testPolicyWaiver.getPolicyId()).getThreatLevel());
    assertThat(dto.createTime).isEqualTo(testPolicyWaiver.getCreateTime());
    assertThat(dto.expiryTime).isEqualTo(testPolicyWaiver.getExpiryTime());
    assertThat(dto.policyId).isEqualTo(testPolicyWaiver.getPolicyId());
    assertThat(dto.policyName).isEqualTo(policiesById.get(testPolicyWaiver.getPolicyId()).getName());
    assertThat(dto.ownerId).isEqualTo(testPolicyWaiver.getOwnerId());
    assertThat(dto.ownerName).isEqualTo(ownersById.get(testPolicyWaiver.getOwnerId()).getName());
    assertThat(dto.ownerType).isEqualTo(ownersById.get(testPolicyWaiver.getOwnerId()).getType().toString());
    assertThat(dto.componentMatchStrategy).isEqualTo(testPolicyWaiver.getComponentMatchStrategy());
    assertThat(dto.hash).isEqualTo(testPolicyWaiver.getHash());
    assertThat(dto.componentIdentifier.toComponentIdentifier()).isEqualTo(testPolicyWaiver.getComponentIdentifier());
    assertThat(dto.getDisplayName().toString())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(testPolicyWaiver.getComponentIdentifier()).toString());

    assertThat(dto.comment).isEqualTo(testPolicyWaiver.getComment());
    assertThat(dto.constraintFacts).isEqualTo(testPolicyWaiver.getConstraintFacts());
    assertThat(dto.creatorId).isEqualTo(testPolicyWaiver.getCreatorId());
    assertThat(dto.creatorName).isEqualTo(testPolicyWaiver.getCreatorName());
  }

  private PolicyWaiver createPolicyWaiverWithFullDetails() {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    testPolicy = tempEntity.newPolicy(app.getId(), "Very bad security threat", 9);

    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vulnerability-1");
    ConditionFact conditionFact =
        new ConditionFact(ConditionTypes.SecurityVulnerabilityStatusConditionType.getId(), 0, "summary", "reason",
            triggerReference);
    TreeMap<String, String> coordinates = new TreeMap<String, String>() {{
        this.put("artifactId", "a1");
        this.put("groupId", "g1");
        this.put("version", "v1");
        this.put("classifier", "c1");
        this.put("extension", "jar");
      }};
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    ConstraintFact constraintFact = new ConstraintFact("constraint id", "constraint name", "operator", conditionFact);
    return tempEntity.newWaiver("hash", testPolicy.getId(), app.getId(),
        singletonList(constraintFact), purl, EXACT_COMPONENT, "a comment", today, aWeekFromNow);
  }
}
