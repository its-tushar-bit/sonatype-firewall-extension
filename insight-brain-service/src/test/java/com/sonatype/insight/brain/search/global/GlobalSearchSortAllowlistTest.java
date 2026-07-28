/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class GlobalSearchSortAllowlistTest
{
  // ---- Components ---------------------------------------------------------------------------

  @Test
  public void components_acceptsIqLocalKeys() {
    for (String key : new String[]{"relevance", "name"}) {
      assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.COMPONENT, key))
          .as("Components must allow %s", key)
          .isTrue();
    }
  }

  @Test
  public void components_rejectsGuideOnlyKeys() {
    for (String guideOnly : new String[]{"maxCvss", "publishedDate", "versionScore", "dts.overall"}) {
      assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.COMPONENT, guideOnly))
          .as("Components must NOT allow Guide-only key %s in the IQ-local allowlist", guideOnly)
          .isFalse();
    }
  }

  @Test
  public void components_rejectsUnknownKey() {
    assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.COMPONENT, "unknownSortField")).isFalse();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> GlobalSearchSortAllowlist.requireAllowed(Tab.COMPONENT, "unknownSortField"));
  }

  // ---- Vulnerabilities ----------------------------------------------------------------------

  @Test
  public void vulnerabilities_acceptsIqLocalKeys() {
    for (String key : new String[]{"relevance", "name"}) {
      assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.VULNERABILITY, key))
          .as("Vulnerabilities must allow %s", key)
          .isTrue();
    }
  }

  @Test
  public void vulnerabilities_rejectsSeverityUntilNumericSortMachineryLands() {
    // severity is held out of the allowlist: vulnerabilitySeverity is a numeric FloatPoint with no
    // sorted-numeric twin and sortFor builds only a STRING SortField, so a severity sort would sort
    // lexicographically or fail on the missing doc-values once field sort is enabled.
    assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.VULNERABILITY, "severity")).isFalse();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> GlobalSearchSortAllowlist.requireAllowed(Tab.VULNERABILITY, "severity"));
  }

  @Test
  public void vulnerabilities_rejectsGuideOnlyKeys() {
    for (String guideOnly : new String[]{"cvssSeverity", "publishedAt", "epss"}) {
      assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.VULNERABILITY, guideOnly))
          .as("Vulnerabilities must NOT allow Guide-only key %s in the IQ-local allowlist", guideOnly)
          .isFalse();
    }
  }

  @Test
  public void vulnerabilities_rejectsUnknownKey() {
    assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.VULNERABILITY, "maxCvss")).isFalse();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> GlobalSearchSortAllowlist.requireAllowed(Tab.VULNERABILITY, "maxCvss"));
  }

  // ---- Applications -------------------------------------------------------------------------

  @Test
  public void applications_acceptsAllSupportedSortKeys() {
    for (String key : new String[]{"relevance", "name", "policyEvaluationStage"}) {
      assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.APPLICATION, key)).isTrue();
    }
  }

  @Test
  public void applications_rejectsUnknownKey() {
    assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.APPLICATION, "maxCvss")).isFalse();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> GlobalSearchSortAllowlist.requireAllowed(Tab.APPLICATION, "maxCvss"));
  }

  // ---- Merged violation tab: relevance + name + threat --------------------------------------

  @Test
  public void violationTab_acceptsRelevanceNameAndThreat_rejectsOthers() {
    Tab tab = Tab.VIOLATION;
    assertThat(GlobalSearchSortAllowlist.isAllowed(tab, "relevance")).isTrue();
    assertThat(GlobalSearchSortAllowlist.isAllowed(tab, "name")).isTrue();
    assertThat(GlobalSearchSortAllowlist.isAllowed(tab, "threat")).isTrue();
    assertThat(GlobalSearchSortAllowlist.isAllowed(tab, "maxCvss")).isFalse();
    assertThat(GlobalSearchSortAllowlist.isAllowed(tab, "epss")).isFalse();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> GlobalSearchSortAllowlist.requireAllowed(tab, "unknown"));
  }

  @Test
  public void applicationTab_acceptsLatestEvaluationSort() {
    assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.APPLICATION, "lastEvaluationTime")).isTrue();
  }

  // ---- Waiver tab: relevance + created (newest first) ---------------------------------------

  @Test
  public void waiverTab_acceptsRelevanceAndCreated_rejectsOthers() {
    Tab tab = Tab.WAIVER;
    assertThat(GlobalSearchSortAllowlist.isAllowed(tab, "relevance")).isTrue();
    assertThat(GlobalSearchSortAllowlist.isAllowed(tab, GlobalSearchSortAllowlist.WAIVER_CREATED)).isTrue();
    assertThat(GlobalSearchSortAllowlist.isAllowed(tab, "name"))
        .as("WAIVER must not allow name (no waiver-name sortable field)")
        .isFalse();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> GlobalSearchSortAllowlist.requireAllowed(tab, "name"));
  }

  // ---- Per-entity default sort --------------------------------------------------------------

  @Test
  public void defaultSort_perTab_matchesPrototypeDefaults() {
    assertThat(GlobalSearchSortAllowlist.defaultSortFor(Tab.APPLICATION)).isEqualTo("lastEvaluationTime");
    assertThat(GlobalSearchSortAllowlist.defaultSortFor(Tab.VIOLATION)).isEqualTo("threat");
    assertThat(GlobalSearchSortAllowlist.defaultSortFor(Tab.WAIVER))
        .isEqualTo(GlobalSearchSortAllowlist.WAIVER_CREATED);
    assertThat(GlobalSearchSortAllowlist.defaultSortFor(Tab.COMPONENT))
        .isEqualTo(GlobalSearchSortAllowlist.RELEVANCE);
    assertThat(GlobalSearchSortAllowlist.defaultSortFor(Tab.VULNERABILITY))
        .isEqualTo(GlobalSearchSortAllowlist.RELEVANCE);
    assertThat(GlobalSearchSortAllowlist.defaultSortFor(null))
        .isEqualTo(GlobalSearchSortAllowlist.RELEVANCE);
  }

  @Test
  public void defaultSort_perTab_isAlwaysAllowlistedForThatTab() {
    for (Tab tab : Tab.values()) {
      String def = GlobalSearchSortAllowlist.defaultSortFor(tab);
      assertThat(GlobalSearchSortAllowlist.isAllowed(tab, def))
          .as("default sort %s for tab %s must be allowlisted", def, tab)
          .isTrue();
    }
  }

  // ---- requireAllowed default behaviour -----------------------------------------------------

  @Test
  public void requireAllowed_nullKey_defaultsToRelevance() {
    assertThat(GlobalSearchSortAllowlist.requireAllowed(Tab.COMPONENT, null))
        .isEqualTo(GlobalSearchSortAllowlist.RELEVANCE);
  }

  @Test
  public void requireAllowed_blankKey_defaultsToRelevance() {
    assertThat(GlobalSearchSortAllowlist.requireAllowed(Tab.COMPONENT, "  "))
        .isEqualTo(GlobalSearchSortAllowlist.RELEVANCE);
  }

  @Test
  public void requireAllowed_allowedKey_returnsKeyUnchanged() {
    assertThat(GlobalSearchSortAllowlist.requireAllowed(Tab.COMPONENT, "name")).isEqualTo("name");
  }

  // ---- isAllowed null safety ----------------------------------------------------------------

  @Test
  public void isAllowed_nullTab_returnsFalse() {
    assertThat(GlobalSearchSortAllowlist.isAllowed(null, "relevance")).isFalse();
  }

  @Test
  public void isAllowed_nullKey_returnsFalse() {
    assertThat(GlobalSearchSortAllowlist.isAllowed(Tab.COMPONENT, null)).isFalse();
  }

  // ---- Sort allowlist coverage --------------------------------------------------------------

  @Test
  public void allTabsHaveAtLeastRelevance() {
    for (Tab tab : Tab.values()) {
      assertThat(GlobalSearchSortAllowlist.allowedFor(tab))
          .as("Tab %s must allow relevance", tab)
          .contains(GlobalSearchSortAllowlist.RELEVANCE);
    }
  }

  // ---- Fingerprint contract -----------------------------------------------------------------

  @Test
  public void fingerprint_isStableAcrossCalls() {
    String first = GlobalSearchSortAllowlist.fingerprint();
    String second = GlobalSearchSortAllowlist.fingerprint();
    assertThat(first).isEqualTo(second);
  }

  @Test
  public void fingerprint_keysAreSortedSoOrderDoesNotAffectStability() {
    // Keys are emitted in sorted order per tab, so the fingerprint depends on contents, not on the
    // set's iteration order (ImmutableSet.of does not contract insertion order).
    String fp = GlobalSearchSortAllowlist.fingerprint();
    for (Tab tab : Tab.values()) {
      List<String> keys = new ArrayList<>(GlobalSearchSortAllowlist.allowedFor(tab));
      if (keys.size() < 2) {
        continue;
      }
      List<String> sorted = new ArrayList<>(keys);
      java.util.Collections.sort(sorted);
      String segment = tab.name() + "=" + String.join(",", sorted);
      assertThat(fp).as("fingerprint must list %s keys in sorted order", tab).contains(segment);
    }
  }

  @Test
  public void fingerprint_containsEveryTabAndKey() {
    String fp = GlobalSearchSortAllowlist.fingerprint();
    for (Tab tab : Tab.values()) {
      assertThat(fp).as("fingerprint must contain tab %s", tab).contains(tab.name());
      for (String key : GlobalSearchSortAllowlist.allowedFor(tab)) {
        assertThat(fp).as("fingerprint must contain key %s for tab %s", key, tab).contains(key);
      }
    }
  }
}
