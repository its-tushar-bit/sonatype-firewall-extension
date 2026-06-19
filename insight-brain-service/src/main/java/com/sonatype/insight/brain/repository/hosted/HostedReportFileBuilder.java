/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.*;

/**
 * Builds report overlay files (policythreats.json, data.json, summary.json) for hosted
 * repository components from DB state. Used both by the queue consumer (eager generation)
 * and by reevaluateHostedComponent (post-evaluation refresh).
 */
public class HostedReportFileBuilder
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private HostedReportFileBuilder() {
  }

  public static byte[] build(
      final String name,
      final RepositoryComponent component,
      final List<RepositoryPolicyViolation> violations) throws Exception
  {
    if (POLICY_THREATS.getName().equals(name)) {
      return buildPolicyThreats(component, violations);
    }
    if (BOM_JSON.getName().equals(name)) {
      return buildBom(component);
    }
    if (DATA_JSON.getName().equals(name)) {
      return buildData(component, violations);
    }
    if (SUMMARY_JSON.getName().equals(name)) {
      return "{\"totalComponentCount\":0,\"uniquePopularComponentCount\":0,\"openIssuesCount\":0}".getBytes();
    }
    if (DEPENDENCIES_JSON.getName().equals(name)) {
      return "{\"dependencyTree\":[]}".getBytes();
    }
    if (SECURITY_JSON.getName().equals(name) || LICENSES_JSON.getName().equals(name)) {
      return "{\"aaData\":[]}".getBytes();
    }
    throw new IllegalArgumentException("Unknown report file: " + name);
  }

  private static byte[] buildPolicyThreats(
      final RepositoryComponent component,
      final List<RepositoryPolicyViolation> violations) throws Exception
  {
    ObjectNode root = MAPPER.createObjectNode();
    root.put("version", 5);
    ArrayNode aaData = root.putArray("aaData");
    if (component != null && !violations.isEmpty()) {
      // First pass: group by pathname (the natural unit produced by the evaluator — one batch per
      // (repository, pathname)). LinkedHashMap preserves DAO order: outer first, then inners
      // ordered by pathname.
      Map<String, List<RepositoryPolicyViolation>> byPathname = new LinkedHashMap<>();
      for (RepositoryPolicyViolation v : violations) {
        byPathname.computeIfAbsent(v.getPathname(), k -> new java.util.ArrayList<>()).add(v);
      }
      // Second pass: emit one aaData entry per UNIQUE HASH. The downstream PDF generator
      // (ApiReportDataServiceV2.getPolicyViolationsByHash) builds a Map keyed on hash with NO
      // merge function — two aaData entries with the same hash crash the export with "Duplicate
      // key". This happens whenever an archive-of-archives upload contains two byte-identical
      // copies of the same jar (e.g. `lib.jar` and `lib (1).jar`): the scanner emits two <dir>s
      // with identical sha1, the evaluator persists two identical batches of violations against
      // distinct synthetic pathnames, and we'd otherwise emit two same-hash aaData entries.
      // We keep only the first pathname's batch per effective-hash — identical content has
      // identical findings, so the second batch would be a perfect duplicate and would also
      // explode the PDF generator if both were kept.
      //
      // The dedup key is the SAME effective hash that buildAaDataEntry will write to the JSON
      // (top-violation hash with outer-component fallback) so the dedup choice and the emitted
      // hash never disagree. Null-effective-hash groups fall back to the pathname as the dedup
      // key — that way two distinct null-hash groups don't both emit `hash=""` and re-trigger
      // the same Duplicate-key crash on the empty-string side.
      Set<String> seenHashes = new java.util.HashSet<>();
      for (Map.Entry<String, List<RepositoryPolicyViolation>> entry : byPathname.entrySet()) {
        List<RepositoryPolicyViolation> group = entry.getValue();
        String effectiveHash = effectiveHashForGroup(group, component);
        String dedupKey = effectiveHash != null ? effectiveHash : "__null_hash__::" + entry.getKey();
        if (!seenHashes.add(dedupKey)) {
          continue;
        }
        aaData.add(buildAaDataEntry(entry.getKey(), group, component));
      }
    }
    return MAPPER.writeValueAsBytes(root);
  }

  /**
   * Computes the same hash {@link #buildAaDataEntry} will write to the {@code aaData} entry's
   * {@code hash} field, so the dedup decision in {@link #buildPolicyThreats} keys on the same
   * value the JSON ends up carrying. Mirrors the resolution order used inside
   * {@code buildAaDataEntry}: the highest-threat violation's hash, falling back to the
   * outer component's hash when the violation rows carry no identity (the older single-component
   * shape that pre-dates the multi-{@code
   *
  <dir>
   * } fan-out).
   */
  private static String effectiveHashForGroup(
      final List<RepositoryPolicyViolation> group,
      final RepositoryComponent outerComponent)
  {
    RepositoryPolicyViolation top = selectTopViolation(group);
    if (top == null) {
      return null;
    }
    if (top.getHash() != null) {
      return top.getHash();
    }
    if (outerComponent != null && outerComponent.getHash() != null) {
      return outerComponent.getHash();
    }
    return null;
  }

  /**
   * Selects the "top" violation from a group: the one with the highest {@code threatLevel}, with
   * ties broken on {@code policyId} ascending so two violations sharing the max threat level
   * deterministically pick the lexicographically-earliest policyId. Returns {@code null} for an
   * empty or null group.
   * <p>
   * Single source of truth — both {@link #effectiveHashForGroup} (which is used as the dedup key
   * in {@link #buildPolicyThreats}) and {@link #buildAaDataEntry} (which writes the chosen
   * violation's policyId/policyName/policyThreatLevel into the JSON) call this helper. If they
   * computed the top independently and the comparator drifted, the dedup key and the emitted
   * hash could silently disagree.
   */
  private static RepositoryPolicyViolation selectTopViolation(
      final List<RepositoryPolicyViolation> group)
  {
    if (group == null || group.isEmpty()) {
      return null;
    }
    return group.stream()
        .max(Comparator.comparingInt(RepositoryPolicyViolation::getThreatLevel)
            .thenComparing(RepositoryPolicyViolation::getPolicyId,
                Comparator.nullsFirst(Comparator.<String>naturalOrder()).reversed()))
        .orElse(null);
  }

  /**
   * Builds one {@code aaData} entry for {@code policythreats.json} representing a single
   * component's findings. {@code outerComponent} is consulted only to resolve the hash and
   * componentIdentifier when the violations are attached to its own pathname; inner-pathname
   * groups read their identity from the violation rows themselves.
   */
  private static ObjectNode buildAaDataEntry(
      final String pathname,
      final List<RepositoryPolicyViolation> groupViolations,
      final RepositoryComponent outerComponent)
  {
    ArrayNode active = MAPPER.createArrayNode();
    ArrayNode waived = MAPPER.createArrayNode();
    ArrayNode all = MAPPER.createArrayNode();
    for (RepositoryPolicyViolation v : groupViolations) {
      ObjectNode vNode = MAPPER.createObjectNode();
      vNode.put("policyId", v.getPolicyId());
      vNode.put("policyName", v.getPolicyName());
      vNode.put("policyThreatLevel", v.getThreatLevel());
      vNode.put("policyViolationId", v.getId());
      vNode.put("waived", v.isWaived());
      vNode.put("grandfathered", false);
      vNode.put("legacyViolation", false);
      vNode.putArray("actions");
      vNode.putArray("constraints");
      all.add(vNode);
      if (v.isWaived()) {
        waived.add(vNode);
      }
      else {
        active.add(vNode);
      }
    }
    // Pick the highest-threat violation as the "top" representative for the group's policyId/
    // policyName/policyThreatLevel summary fields. Selection is shared with effectiveHashForGroup
    // via selectTopViolation so the dedup key in buildPolicyThreats and the hash written here
    // can never disagree. orElseThrow makes the invariant (non-empty group) explicit; callers
    // always pass a non-empty list because the grouping loop only adds entries with at least
    // one violation.
    RepositoryPolicyViolation top = selectTopViolation(groupViolations);
    if (top == null) {
      throw new IllegalStateException("Empty violation group passed to buildAaDataEntry");
    }

    ObjectNode group = MAPPER.createObjectNode();

    // Identity: prefer the violation row's own (hash, componentIdentifier) — that's the inner
    // artifact's identity for inner pathnames. When the violation row carries no identity (the
    // older single-component shape that pre-dates the multi-`<dir>` fan-out, where violations
    // were minted without per-row hash metadata), fall back to the outer component's identity.
    // This keeps single-component scans byte-for-byte compatible with the pre-fan-out builder.
    String hash = top.getHash();
    if (hash == null && outerComponent != null && outerComponent.getHash() != null) {
      hash = outerComponent.getHash();
    }
    group.put("hash", hash != null ? hash : "");

    JsonNode componentIdentifier = null;
    if (top.getComponentIdentifier() != null) {
      componentIdentifier = MAPPER.valueToTree(top.getComponentIdentifier());
    }
    else if (outerComponent != null && outerComponent.getComponentIdentifier() != null) {
      componentIdentifier = MAPPER.valueToTree(outerComponent.getComponentIdentifier());
    }
    if (componentIdentifier != null) {
      group.set("componentIdentifier", componentIdentifier);
    }

    group.put("policyId", top.getPolicyId());
    group.put("policyName", top.getPolicyName());
    group.put("policyThreatLevel", top.getThreatLevel());
    group.set("activeViolations", active);
    group.set("waivedViolations", waived);
    group.set("allViolations", all);
    return group;
  }

  private static byte[] injectTopLevelComponentIfAbsent(
      final byte[] originalBom,
      final RepositoryComponent component) throws Exception
  {
    if (component == null || component.getHash() == null) {
      return originalBom;
    }
    JsonNode node = MAPPER.readTree(originalBom);
    if (!(node instanceof ObjectNode)) {
      return originalBom;
    }
    JsonNode aaDataNode = node.get("aaData");
    if (!(aaDataNode instanceof ArrayNode)) {
      return originalBom;
    }
    ArrayNode aaData = (ArrayNode) aaDataNode;
    for (JsonNode entry : aaData) {
      if (component.getHash().equals(entry.path("hash").asText(null))) {
        return originalBom;
      }
    }
    ObjectNode entry = MAPPER.createObjectNode();
    entry.put("hash", component.getHash());
    entry.put("matchState",
        component.getMatchStateId() != null ? component.getMatchStateId() : MatchState.EXACT.getId());
    entry.put("proprietary", false);
    ArrayNode pathnames = entry.putArray("pathnames");
    if (component.getPathname() != null) {
      pathnames.add(component.getPathname());
    }
    if (component.getComponentIdentifier() != null) {
      entry.set("componentIdentifier", MAPPER.valueToTree(component.getComponentIdentifier()));
    }
    entry.putNull("relativePopularity");
    entry.put("createTime", 0L);
    entry.putArray("aaData");
    aaData.add(entry);
    return MAPPER.writeValueAsBytes(node);
  }

  private static byte[] buildBom(final RepositoryComponent component) throws Exception {
    ObjectNode root = MAPPER.createObjectNode();
    ArrayNode aaData = root.putArray("aaData");
    if (component != null && component.getHash() != null) {
      ObjectNode entry = MAPPER.createObjectNode();
      entry.put("hash", component.getHash());
      String displayName = component.getDisplayName() != null ? component.getDisplayName() : component.getPathname();
      ObjectNode displayNameNode = MAPPER.createObjectNode();
      displayNameNode.putArray("parts").addObject().put("value", displayName != null ? displayName : "");
      entry.set("displayName", displayNameNode);
      entry.put("matchState",
          component.getMatchStateId() != null ? component.getMatchStateId() : MatchState.EXACT.getId());
      entry.put("proprietary", false);
      ArrayNode pathnames = entry.putArray("pathnames");
      if (component.getPathname() != null) {
        pathnames.add(component.getPathname());
      }
      if (component.getComponentIdentifier() != null) {
        entry.set("componentIdentifier", MAPPER.valueToTree(component.getComponentIdentifier()));
      }
      entry.putArray("aaData");
      aaData.add(entry);
    }
    return MAPPER.writeValueAsBytes(root);
  }

  /**
   * Patches bom.json: adds displayName to entries that lack it, and injects the top-level
   * repository component if absent. Go/npm modules are not included in the HDS bom.json
   * (which lists sub-packages/dependencies instead), so without this injection the priorities
   * page has nothing to match violations against and shows empty.
   */
  public static byte[] patchBomDisplayName(
      final byte[] originalBom,
      final RepositoryComponent component) throws Exception
  {
    byte[] patched = injectTopLevelComponentIfAbsent(originalBom, component);
    return patchBomDisplayName(patched);
  }

  public static byte[] patchBomDisplayName(final byte[] originalBom) throws Exception {
    JsonNode node = MAPPER.readTree(originalBom);
    if (!(node instanceof ObjectNode)) {
      return originalBom;
    }
    ObjectNode bomJson = (ObjectNode) node;
    JsonNode aaDataNode = bomJson.get("aaData");
    if (!(aaDataNode instanceof ArrayNode)) {
      return originalBom;
    }
    ArrayNode aaData = (ArrayNode) aaDataNode;
    boolean patched = false;
    for (int i = 0; i < aaData.size(); i++) {
      if (!(aaData.get(i) instanceof ObjectNode)) {
        continue;
      }
      ObjectNode entry = (ObjectNode) aaData.get(i);
      JsonNode displayNameNode = entry.get("displayName");
      if (displayNameNode == null || displayNameNode.isNull() || displayNameNode.isMissingNode()) {
        String displayNameStr = null;
        ComponentIdentifier ci = ComponentIdentifierAdapter.getComponentIdentifier(entry);
        if (ci != null && ComponentDisplayNameUtil.fromIdentifier(ci) != null) {
          displayNameStr = ComponentDisplayNameUtil.fromIdentifier(ci).toString();
        }
        // Fallback to filename for unidentified/dependency components — PDF generator
        // NPEs at ApiReportDataServiceV2:289 if displayName is absent from any BOM entry.
        // Use filename only (last path segment) so it shows "test-1.0.war" not "a/b/c/test-1.0.war"
        if (displayNameStr == null) {
          JsonNode pathnames = entry.path("pathnames");
          if (pathnames.isArray() && pathnames.size() > 0) {
            String fullPath = pathnames.get(0).asText();
            int lastSlash = fullPath.lastIndexOf('/');
            displayNameStr = lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;
          }
        }
        if (displayNameStr == null) {
          displayNameStr = entry.path("hash").asText("unknown");
        }
        ObjectNode dnNode = MAPPER.createObjectNode();
        dnNode.putArray("parts").addObject().put("value", displayNameStr);
        entry.set("displayName", dnNode);
        patched = true;
      }
    }
    return patched ? MAPPER.writeValueAsBytes(bomJson) : originalBom;
  }

  private static byte[] buildData(
      final RepositoryComponent component,
      final List<RepositoryPolicyViolation> violations)
  {
    int count = component != null ? 1 : 0;
    boolean isExact = component != null && MatchState.EXACT.getId().equalsIgnoreCase(component.getMatchStateId());
    long activeViolations = violations.stream().filter(v -> !v.isWaived()).count();
    int critical = (int) violations.stream()
        .filter(v -> !v.isWaived() && ThreatLevel.from(v.getThreatLevel()) == ThreatLevel.CRITICAL)
        .count();
    int severe = (int) violations.stream()
        .filter(v -> !v.isWaived() && ThreatLevel.from(v.getThreatLevel()) == ThreatLevel.SEVERE)
        .count();
    int moderate = (int) violations.stream()
        .filter(v -> !v.isWaived() && ThreatLevel.from(v.getThreatLevel()) == ThreatLevel.MODERATE)
        .count();
    int policyComponentCount = activeViolations > 0 ? 1 : 0;
    int nonLowViolationCount = critical + severe + moderate;
    return ("{\"globals\":{\"expandedCoverage\":false},"
        + "\"reportVersion\":4,"
        + "\"totalArtifactCount\":" + count + ","
        + "\"knownArtifactCount\":" + (isExact ? 1 : 0) + ","
        + "\"exactlyMatchedComponentCount\":" + (isExact ? 1 : 0) + ","
        + "\"policyComponentCount\":" + policyComponentCount + ","
        + "\"criticalViolationCount\":" + critical + ","
        + "\"severeViolationCount\":" + severe + ","
        + "\"moderateViolationCount\":" + moderate + ","
        + "\"nonLowViolationCount\":" + nonLowViolationCount + ","
        + "\"components\":[]}").getBytes();
  }
}
