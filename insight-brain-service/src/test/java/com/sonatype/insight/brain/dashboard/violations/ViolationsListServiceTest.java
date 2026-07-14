/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ViolationsListService} sort semantics. Focused on the null-threat placement
 * regression: rows with an absent threat level must always sort last, for both ascending and the
 * default descending order (CLM-42254).
 */
public class ViolationsListServiceTest
{
  @Test
  public void descendingSort_ordersHighestThreatFirst_withNullsLast() {
    List<ViolationRowDTO> rows = rowsWithThreat(3, null, 10, 8);

    rows.sort(ViolationsListService.comparator("-policyThreatLevel"));

    assertThat(rows).extracting(row -> row.threatLevel).containsExactly(10, 8, 3, null);
  }

  @Test
  public void ascendingSort_ordersLowestThreatFirst_withNullsLast() {
    List<ViolationRowDTO> rows = rowsWithThreat(3, null, 10, 8);

    rows.sort(ViolationsListService.comparator("policyThreatLevel"));

    assertThat(rows).extracting(row -> row.threatLevel).containsExactly(3, 8, 10, null);
  }

  private static List<ViolationRowDTO> rowsWithThreat(final Integer... threatLevels) {
    List<ViolationRowDTO> rows = new ArrayList<>();
    Arrays.stream(threatLevels).forEach(threat -> {
      ViolationRowDTO row = new ViolationRowDTO();
      row.threatLevel = threat;
      rows.add(row);
    });
    return rows;
  }
}
