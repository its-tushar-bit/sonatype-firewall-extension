/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SloViolationEnricher} surfaces the legacy (grandfathered) flag/timestamp on the SLO feed
 * payload. Legacy status is purely informational — it follows the identical SLO rules as a regular violation and does
 * not alter SLO semantics — so the enricher only copies the timestamp down and derives the boolean flag from it.
 */
@ComponentH2Test
public class SloViolationEnricherLegacyTest
    extends AbstractComponentH2Test
{
  private static final String SCAN_ID = "scan-legacy";

  @Inject
  private SloViolationEnricher enricher;

  @Test
  public void populatesLegacyFieldsForLegacyViolation() {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());
    Date legacyTime = new Date(1_700_000_000_000L);

    PolicyViolation violation = newBaseViolation(application, "pv-legacy-");
    violation.setLegacyViolationTime(legacyTime);

    List<SloViolation> results =
        enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(violation));

    assertThat(results).hasSize(1);
    SloViolation result = results.get(0);
    assertThat(result.legacy).isTrue();
    assertThat(result.legacyViolationTime).isEqualTo(legacyTime);
  }

  @Test
  public void leavesLegacyFalseForRegularViolation() {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());

    PolicyViolation violation = newBaseViolation(application, "pv-regular-");
    assertThat(violation.getLegacyViolationTime()).isNull();

    List<SloViolation> results =
        enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(violation));

    assertThat(results).hasSize(1);
    SloViolation result = results.get(0);
    assertThat(result.legacy).isFalse();
    assertThat(result.legacyViolationTime).isNull();
  }

  private static PolicyViolation newBaseViolation(final Application application, final String idPrefix) {
    PolicyViolation violation = new PolicyViolation();
    violation.setId(idPrefix + System.nanoTime());
    violation.setOwnerId(application.getId());
    violation.setStageTypeId(Stage.ID_RELEASE);
    violation.setPolicyId("policy-1");
    violation.setPolicyName("Legacy test policy");
    violation.setThreatLevel(5);
    violation.setThreatCategory(PolicyThreatCategory.SECURITY);
    return violation;
  }
}
