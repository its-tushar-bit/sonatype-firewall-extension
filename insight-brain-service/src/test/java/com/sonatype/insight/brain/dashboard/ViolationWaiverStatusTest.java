/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the indexed-waiver-status to {@link PolicyViolationState} read map, including the
 * shared OPEN-exclusion vocabulary that keeps the state filter and the OPEN facet count symmetric.
 */
public class ViolationWaiverStatusTest
{
  @Test
  public void toState_active_isOpen() {
    assertThat(ViolationWaiverStatus.toState(ViolationWaiverStatus.ACTIVE))
        .isEqualTo(PolicyViolationState.OPEN);
  }

  @Test
  public void toState_waived_isWaived() {
    assertThat(ViolationWaiverStatus.toState(ViolationWaiverStatus.WAIVED))
        .isEqualTo(PolicyViolationState.WAIVED);
  }

  @Test
  public void toState_autoWaived_isWaived() {
    assertThat(ViolationWaiverStatus.toState(ViolationWaiverStatus.AUTO_WAIVED))
        .isEqualTo(PolicyViolationState.WAIVED);
  }

  @Test
  public void toState_legacy_isLegacyViolation() {
    assertThat(ViolationWaiverStatus.toState(ViolationWaiverStatus.LEGACY))
        .isEqualTo(PolicyViolationState.LEGACY_VIOLATION);
  }

  @Test
  public void toState_absentOrUnknown_isOpen() {
    assertThat(ViolationWaiverStatus.toState(null)).isEqualTo(PolicyViolationState.OPEN);
    assertThat(ViolationWaiverStatus.toState("something-else")).isEqualTo(PolicyViolationState.OPEN);
  }

  @Test
  public void isAutoWaived_onlyTrueForAutoWaived() {
    assertThat(ViolationWaiverStatus.isAutoWaived(ViolationWaiverStatus.AUTO_WAIVED)).isTrue();
    assertThat(ViolationWaiverStatus.isAutoWaived(ViolationWaiverStatus.WAIVED)).isFalse();
    assertThat(ViolationWaiverStatus.isAutoWaived(ViolationWaiverStatus.LEGACY)).isFalse();
    assertThat(ViolationWaiverStatus.isAutoWaived(ViolationWaiverStatus.ACTIVE)).isFalse();
  }

  @Test
  public void openExclusionStatuses_containsWaivedAutoWaivedAndLegacy() {
    // OPEN = NOT (this set) on both the state filter and the OPEN facet count; Legacy MUST be present or
    // it leaks into OPEN.
    assertThat(ViolationWaiverStatus.openExclusionStatuses()).isEqualTo("Waived AutoWaived Legacy");
  }
}
