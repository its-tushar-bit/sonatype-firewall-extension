/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.model.policy.ConditionType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConditionTypesTest
    extends AbstractDataTest
{
  @Test
  public void testGetAll() {
    Collection<ConditionType> allConditionTypes = ConditionTypes.getAll();

    assertThat(allConditionTypes).hasSize(35);
    assertThat(allConditionTypes).extracting(ConditionType::getId)
        .doesNotContain(DeprecatedSecurityVulnerabilityConditionType.ID)
        .contains(HygieneRatingConditionType.ID)
        .contains(IntegrityRatingConditionType.ID)
        .contains(SecurityVulnerabilitySourceConditionType.ID);
    assertThat(allConditionTypes).filteredOn(ConditionType::isEnabled)
        .extracting(ConditionType::getId)
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

  @Test
  public void testGetAll_SortedAlphabetically() {
    // Get all condition types
    List<ConditionType> conditionTypes = new ArrayList<>(ConditionTypes.getAll());

    // Verify there are at least a few condition types to test sorting
    assertThat(conditionTypes).hasSizeGreaterThan(2);

    // Verify the list is sorted alphabetically by display name
    for (int i = 0; i < conditionTypes.size() - 1; i++) {
      String currentName = conditionTypes.get(i).getName();
      String nextName = conditionTypes.get(i + 1).getName();
      assertThat(currentName.compareToIgnoreCase(nextName)).isLessThanOrEqualTo(0);
    }
  }

  @Test
  public void testGetAllWithAutoUnquarantineSupported_SortedAlphabetically() {
    // Get all auto-unquarantine supported condition types
    List<ConditionType> conditionTypes = new ArrayList<>(ConditionTypes.getAllWithAutoUnquarantineSupported());

    // Skip test if there are no auto-unquarantine supported condition types
    if (conditionTypes.isEmpty()) {
      return;
    }

    // Verify the list is sorted alphabetically by display name
    for (int i = 0; i < conditionTypes.size() - 1; i++) {
      String currentName = conditionTypes.get(i).getName();
      String nextName = conditionTypes.get(i + 1).getName();
      assertThat(currentName.compareToIgnoreCase(nextName)).isLessThanOrEqualTo(0);
    }
  }
}
