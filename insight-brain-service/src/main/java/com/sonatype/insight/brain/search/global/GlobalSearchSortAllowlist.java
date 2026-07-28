/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;

import static com.sonatype.insight.brain.search.global.Tab.ALL;
import static com.sonatype.insight.brain.search.global.Tab.APPLICATION;
import static com.sonatype.insight.brain.search.global.Tab.COMPONENT;
import static com.sonatype.insight.brain.search.global.Tab.VIOLATION;
import static com.sonatype.insight.brain.search.global.Tab.VULNERABILITY;
import static com.sonatype.insight.brain.search.global.Tab.WAIVER;

/**
 * Per-tab sort-key allowlist for Global Search. Validated twice as defense-in-depth: at the
 * controller (HTTP 400 on unknown key) and again in the query builder, so a missed upstream check
 * cannot smuggle a non-allowlisted key into the index search. Only keys mapping to a physical
 * IQ-local index field are listed; Guide-only catalog sort keys are handled by the orchestrator.
 * Comparisons are case-sensitive (camelCase per IQ API style).
 *
 * <p>
 * The allowlist layout feeds the {@link GlobalSearchCursor} generation token via
 * {@link #fingerprint()}, so any change here invalidates cursors issued against the old layout.
 */
public final class GlobalSearchSortAllowlist
{
  /** Sort key every tab supports; maps to Lucene/OpenSearch native _score. */
  public static final String RELEVANCE = "relevance";

  /** WAIVER default sort key: newest waiver first, backed by the created-at epoch-millis twin. */
  public static final String WAIVER_CREATED = "created";

  /**
   * WAIVER sort key: oldest waiver first, backed by the SAME created-at epoch-millis twin as
   * {@link #WAIVER_CREATED} but ASCENDING (missing create time sorts last).
   */
  public static final String WAIVER_OLDEST = "oldest";

  /**
   * WAIVER sort key: highest threat level first, backed by the waiver threat-level numeric twin.
   * Mirrors the VIOLATION {@code threat} key (numeric, descending, missing-last).
   */
  public static final String WAIVER_THREAT = "threat";

  /**
   * WAIVER sort key: soonest expiry first, backed by the expires-at epoch-millis field. Numeric but
   * ASCENDING (unlike created/threat), and never-expiring waivers (no expiry value) sort LAST,
   * matching the prototype's {@code Infinity} fallback for a missing expiration date.
   */
  public static final String WAIVER_EXPIRATION = "expiration";

  public static final String DEFAULT_SORT = RELEVANCE;

  private static final Map<Tab, Set<String>> ALLOWLIST = buildAllowlist();

  /**
   * Default sort per tab, applied when the request supplies no sort key. Prototype defaults:
   * Applications = latest evaluation (newest first), Violations = threat (highest first), Waivers =
   * created (newest first). Every other tab defaults to relevance. Each non-relevance default MUST
   * also be in {@link #ALLOWLIST} for its tab and carry a sortable index field in
   * {@code IqLocalSearchService.SORTABLE_FIELD_BY_KEY}.
   */
  private static final Map<Tab, String> DEFAULT_SORT_BY_TAB = buildDefaultSortByTab();

  private GlobalSearchSortAllowlist() {
  }

  private static Map<Tab, Set<String>> buildAllowlist() {
    Map<Tab, Set<String>> m = new EnumMap<>(Tab.class);

    m.put(ALL, ImmutableSet.of(RELEVANCE));

    // Components My-tab: relevance, name, and policyThreatLevel (backed by the denormalized
    // componentMaxPolicyThreatLevel int twin on component docs). The prototype's All-tab component
    // sorts (trending/downloads/latest_release/dts) are Catalog/federation attributes with no local
    // index field and are NOT listed here — they are a separate cross-service follow-up.
    m.put(COMPONENT, ImmutableSet.of(RELEVANCE, "name", "policyThreatLevel"));
    // Vulnerabilities My-tab: relevance, name, and cvss (backed by the vulnerabilitySeverity float
    // sort twin). A published sort is NOT listed: local vuln docs carry no published date (V2 gap).
    m.put(VULNERABILITY, ImmutableSet.of(RELEVANCE, "name", "cvss"));
    m.put(APPLICATION, ImmutableSet.of(
        RELEVANCE, "name", "policyEvaluationStage", "lastEvaluationTime", "policyThreatLevel", "violationState"));

    m.put(VIOLATION, ImmutableSet.of(RELEVANCE, "name", "threat"));
    // WAIVER supports relevance + created (newest first, default), threat (highest first), and
    // expiration (soonest first, never-expires last). All three non-relevance keys are backed by
    // numeric epoch/level twins (see IqLocalSearchService.SORTABLE_FIELD_BY_KEY).
    m.put(WAIVER, ImmutableSet.of(RELEVANCE, WAIVER_CREATED, WAIVER_OLDEST, WAIVER_THREAT, WAIVER_EXPIRATION));

    return Collections.unmodifiableMap(m);
  }

  private static Map<Tab, String> buildDefaultSortByTab() {
    Map<Tab, String> m = new EnumMap<>(Tab.class);
    m.put(APPLICATION, "lastEvaluationTime");
    m.put(VIOLATION, "threat");
    m.put(WAIVER, WAIVER_CREATED);
    return Collections.unmodifiableMap(m);
  }

  /**
   * Default sort key for a tab (relevance unless the tab has a prototype-specified default). Returned
   * key is always allowlisted for the tab. Used when a request supplies no sort key.
   */
  public static String defaultSortFor(final Tab tab) {
    if (tab == null) {
      return RELEVANCE;
    }
    return DEFAULT_SORT_BY_TAB.getOrDefault(tab, RELEVANCE);
  }

  public static boolean isAllowed(final Tab tab, final String sortKey) {
    if (tab == null || sortKey == null) {
      return false;
    }
    Set<String> allowed = ALLOWLIST.get(tab);
    return allowed != null && allowed.contains(sortKey);
  }

  /**
   * Returns the key if allowlisted, {@link #RELEVANCE} if {@code null}/blank, else throws
   * {@link IllegalArgumentException}.
   */
  public static String requireAllowed(final Tab tab, final String sortKey) {
    if (StringUtils.isBlank(sortKey)) {
      return RELEVANCE;
    }
    if (!isAllowed(tab, sortKey)) {
      // Do not echo the raw (attacker-controllable) sortKey in the message; list the allowed keys.
      throw new IllegalArgumentException(
          "Sort key is not allowed for tab " + tab + ". Allowed: " + allowedFor(tab));
    }
    return sortKey;
  }

  public static Set<String> allowedFor(final Tab tab) {
    return Optional.ofNullable(ALLOWLIST.get(tab)).orElse(Collections.emptySet());
  }

  public static Map<Tab, Set<String>> allTabs() {
    return ALLOWLIST;
  }

  /**
   * Stable serialisation of the allowlist layout, fed as one preimage component to the cursor
   * generation token. A code change here yields a new fingerprint, hence a new token, which
   * invalidates cursors issued against the old layout.
   */
  public static String fingerprint() {
    return FINGERPRINT_VALUE;
  }

  private static final String FINGERPRINT_VALUE = computeFingerprint();

  private static String computeFingerprint() {
    // Tabs iterate in enum order (EnumMap) and keys are sorted, so the fingerprint depends on the
    // allowlist contents, not on set iteration order (which ImmutableSet.of does not contract).
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Tab, Set<String>> e : ALLOWLIST.entrySet()) {
      sb.append(e.getKey().name()).append('=');
      List<String> keys = new ArrayList<>(e.getValue());
      Collections.sort(keys);
      boolean first = true;
      for (String key : keys) {
        if (!first) {
          sb.append(',');
        }
        sb.append(key);
        first = false;
      }
      sb.append('\n');
    }
    return sb.toString();
  }
}
