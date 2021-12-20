/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.Collection;

import com.sonatype.insight.brain.model.policy.ConditionType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConditionTypesTest
{
  @Test
  public void testGetAll() {
    Collection<ConditionType> allConditionTypes = ConditionTypes.getAll();

    assertThat(allConditionTypes).hasSize(23);
    assertThat(allConditionTypes).extracting(ConditionType::getId)
        .doesNotContain(DeprecatedSecurityVulnerabilityConditionType.ID)
        .contains(HygieneRatingConditionType.ID)
        .contains(IntegrityRatingConditionType.ID)
        .contains(SecurityVulnerabilitySourceConditionType.ID);
    assertThat(allConditionTypes).filteredOn(ConditionType::isEnabled).extracting(ConditionType::getId)
        .doesNotContain(HygieneRatingConditionType.ID)
        .doesNotContain(SecurityVulnerabilitySourceConditionType.ID)
        .doesNotContain(IacControlConditionType.ID);
  }

  @Test
  public void testGetById_DeprecatedSecurityVulnerabilityConditionType() {
    assertThat(ConditionTypes.getById(DeprecatedSecurityVulnerabilityConditionType.ID))
        .isEqualTo(ConditionTypes.DeprecatedSecurityVulnerabilityConditionType);
  }

  @Test
  public void testGetById_LicensedConditionTypes() {
    assertThat(ConditionTypes.getById(HygieneRatingConditionType.ID))
        .isEqualTo(ConditionTypes.HygieneRatingConditionType);

    assertThat(ConditionTypes.getById(IntegrityRatingConditionType.ID))
        .isEqualTo(ConditionTypes.IntegrityRatingConditionType);
  }
}
