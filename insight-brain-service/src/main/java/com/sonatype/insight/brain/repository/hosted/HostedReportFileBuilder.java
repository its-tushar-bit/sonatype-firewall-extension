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
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.*;

/**
 * Builds report overlay files (policythreats.json, data.json, summary.json) for hosted
 * repository components from DB state. Used by the queue consumer (eager generation)
 * and by the Continuous Monitoring flow's post-evaluation refresh
 * ({@link com.sonatype.insight.brain.report.ReportService#refreshHostedComponentAfterEvaluation}).
 */
public class HostedReportFileBuilder
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Formats whose outer artifact is not a real Lifecycle component: LC resolves only the inner
   * (transitive) dependencies and represents the outer as a single {@code Component-Unknown}(2).
   * For these, the outer's own violations are replaced with that row so the report matches LC.
   */
  private static final Set<String> OUTER_AS_UNKNOWN_FORMATS = Set.of("go");

  /**
   * Fallback name/threat for the synthetic outer row, used only when the caller could not resolve
   * the tenant's Component-Unknown policy (e.g. it was deleted). Normal path stamps the resolved
   * policy's own id/name/threat so the row matches whatever LC emits for the same owner.
   */
  private static final String DEFAULT_COMPONENT_UNKNOWN_POLICY_NAME = "Component-Unknown";

  private static final int DEFAULT_COMPONENT_UNKNOWN_THREAT_LEVEL = 2;

  private HostedReportFileBuilder() {
  }

  /**
   * Reshapes the hosted violation rows so the report matches a same-file Lifecycle scan, keyed on
   * component identity (not hash, which collides for nuget framework DLLs):
   * <ul>
   * <li>go ({@link #OUTER_AS_UNKNOWN_FORMATS}): replace the outer's violations with a single
   * Component-Unknown row (from the resolved policy), keep the inners.</li>
   * <li>other formats: drop the outer row when its identity also appears on an inner row (the
   * npm self-mirror duplicate); nuget fan-out is kept since the outer identity differs.</li>
   * </ul>
   * Read-time only — no stored rows are modified; enforcement paths read the outer row directly.
   */
  public static List<ProxyRepositoryPolicyViolation> excludeOuterViolationsForFormat(
      final ProxyRepositoryComponent outerComponent,
      final List<ProxyRepositoryPolicyViolation> violations,
      final String repositoryFormat)
  {
    return excludeOuterViolationsForFormat(outerComponent, violations, repositoryFormat, null);
  }

  /**
   * Overload that takes the tenant's resolved Component-Unknown policy so the synthetic outer row
   * (go format) carries the same policy id, name and threat level LC emits when it evaluates a bare
   * {@code .zip} whose matchState is unknown. The caller resolves the policy for the scan's owning
   * org (inheritance honored) by its {@code MatchState is unknown} condition, not by name — the name
   * and threat are tenant-editable, so hardcoding either diverges from LC once a tenant customizes
   * the policy. When the policy cannot be resolved the synthetic row falls back to the defaults.
   */
  public static List<ProxyRepositoryPolicyViolation> excludeOuterViolationsForFormat(
      final ProxyRepositoryComponent outerComponent,
      final List<ProxyRepositoryPolicyViolation> violations,
      final String repositoryFormat,
      final Policy componentUnknownPolicy)
  {
    if (violations == null || violations.isEmpty()) {
      return violations;
    }
    final String outerPathname = outerComponent != null ? outerComponent.getPathname() : null;
    if (outerPathname == null) {
      return violations;
    }

    if (repositoryFormat != null && OUTER_AS_UNKNOWN_FORMATS.contains(repositoryFormat.toLowerCase())) {
      List<ProxyRepositoryPolicyViolation> inners = violations.stream()
          .filter(v -> !outerPathname.equals(v.getPathname()))
          .collect(java.util.stream.Collectors.toList());
      List<ProxyRepositoryPolicyViolation> result = new java.util.ArrayList<>(inners.size() + 1);
      result.add(buildOuterUnknownViolation(outerComponent, componentUnknownPolicy));
      result.addAll(inners);
      return result;
    }

    Set<String> innerIdentities = new java.util.HashSet<>();
    for (ProxyRepositoryPolicyViolation v : violations) {
      if (v.getPathname() != null && v.getPathname().startsWith(outerPathname + "!/")) {
        String id = componentIdentity(v);
        if (id != null) {
          innerIdentities.add(id);
        }
      }
    }
    if (innerIdentities.isEmpty()) {
      return violations;
    }
    return violations.stream()
        .filter(v -> {
          boolean isOuter = outerPathname.equals(v.getPathname());
          return !(isOuter && innerIdentities.contains(componentIdentity(v)));
        })
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * True when {@code policy} is the Component-Unknown policy — identified by its {@code MatchState is
   * unknown} condition, the same condition LC's evaluator fires on for an unidentified component.
   * Keyed on the condition (stable code enums) rather than the policy name or id, both of which are
   * tenant-editable. Callers use this to resolve the effective policy for a scan's owner.
   */
  public static boolean isComponentUnknownPolicy(final Policy policy) {
    if (policy == null || policy.getConstraints() == null) {
      return false;
    }
    return policy.getConstraints()
        .stream()
        .filter(c -> c != null && c.getConditions() != null)
        .flatMap(c -> c.getConditions().stream())
        .anyMatch(cond -> cond != null
            && MatchStateConditionType.ID.equals(cond.getConditionTypeId())
            && MatchState.UNKNOWN.getId().equals(cond.getValue()));
  }

  /**
   * Resolves the effective Component-Unknown policy for a scan whose leaf owner is {@code ownerId}
   * (typically {@code Application.getId()}), using
   * {@link PolicyDAO#getApplicableByOwnerIdWithHierarchy} so the result reflects the
   * inherited-and-overridden policy the LC evaluator would fire for the same owner. The policy is
   * identified by its {@code MatchState is unknown} condition ({@link #isComponentUnknownPolicy}),
   * not by name — name and threat are tenant-editable, so keying on either diverges from LC once a
   * tenant customizes the policy. Returns {@code null} when no such policy is applicable (the
   * caller falls back to defaults).
   */
  public static Policy resolveComponentUnknownPolicy(final PolicyDAO policyDAO, final String ownerId) {
    if (ownerId == null) {
      return null;
    }
    try (TransactionContext tx = policyDAO.createTransactionContext()) {
      return policyDAO.getApplicableByOwnerIdWithHierarchy(tx, ownerId)
          .stream()
          .filter(HostedReportFileBuilder::isComponentUnknownPolicy)
          .findFirst()
          .orElse(null);
    }
  }

  /** Component identity used to detect the outer self-mirror: format + coordinates (never the hash). */
  private static String componentIdentity(final ProxyRepositoryPolicyViolation v) {
    ComponentIdentifier ci = v.getComponentIdentifier();
    if (ci == null || ci.getCoordinates() == null || ci.getCoordinates().isEmpty()) {
      return null;
    }
    return ci.getFormat() + "::" + ci.getCoordinates();
  }

  /**
   * Builds the single Component-Unknown row that represents a Go outer module in the hosted report,
   * mirroring LC. Stamps the resolved policy's id, name and threat level so the row matches whatever
   * LC emits for the scan's owner; when no policy was resolved, falls back to the defaults.
   */
  private static ProxyRepositoryPolicyViolation buildOuterUnknownViolation(
      final ProxyRepositoryComponent outerComponent,
      final Policy componentUnknownPolicy)
  {
    ProxyRepositoryPolicyViolation unknown = new ProxyRepositoryPolicyViolation();
    unknown.setPathname(outerComponent != null ? outerComponent.getPathname() : null);
    unknown.setHash(outerComponent != null ? outerComponent.getHash() : null);
    if (componentUnknownPolicy != null) {
      unknown.setPolicyId(componentUnknownPolicy.getId());
      unknown.setPolicyName(componentUnknownPolicy.getName());
      unknown.setThreatLevel(componentUnknownPolicy.getThreatLevel());
    }
    else {
      unknown.setPolicyName(DEFAULT_COMPONENT_UNKNOWN_POLICY_NAME);
      unknown.setThreatLevel(DEFAULT_COMPONENT_UNKNOWN_THREAT_LEVEL);
    }
    return unknown;
  }

  public static byte[] build(
      final String name,
      final ProxyRepositoryComponent component,
      final List<ProxyRepositoryPolicyViolation> violations) throws Exception
  {
    return build(name, component, violations, null);
  }

  /**
   * CLM-40943: overload that accepts an {@code outerHashOverride} — the hash HDS's
   * {@code bom.json} uses for the outer component. When the bom hash differs from
   * {@code proxy_repository_component.hash} (npm and most nuget formats — file SHA1 vs HDS-derived
   * package hash), the synthesised {@code policythreats.json} must use bom's hash so the
   * downstream LC Application Report body can join violations to bom entries by hash. Without
   * this the body table shows the outer component with zero violations attached even when the
   * pill header reports many. Formats whose file SHA1 already equals HDS's hash (maven, pypi,
   * rubygems, conda, helm, r) pass {@code null} and behave as before.
   * <p>
   * The override applies ONLY to the outer-pathname row (the row whose pathname equals
   * {@code component.getPathname()}). Inner-pathname rows (from the archive-of-archives mirror)
   * keep their own per-violation hash, which is already the HDS identification hash for the
   * inner component.
   */
  public static byte[] build(
      final String name,
      final ProxyRepositoryComponent component,
      final List<ProxyRepositoryPolicyViolation> violations,
      final String outerHashOverride) throws Exception
  {
    if (POLICY_THREATS.getName().equals(name)) {
      return buildPolicyThreats(component, violations, outerHashOverride);
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
      final ProxyRepositoryComponent component,
      final List<ProxyRepositoryPolicyViolation> violations) throws Exception
  {
    return buildPolicyThreats(component, violations, null);
  }

  private static byte[] buildPolicyThreats(
      final ProxyRepositoryComponent component,
      final List<ProxyRepositoryPolicyViolation> violations,
      final String outerHashOverride) throws Exception
  {
    ObjectNode root = MAPPER.createObjectNode();
    root.put("version", 5);
    ArrayNode aaData = root.putArray("aaData");
    if (component != null && !violations.isEmpty()) {
      // First pass: group by pathname (the natural unit produced by the evaluator — one batch per
      // (repository, pathname)). LinkedHashMap preserves DAO order: outer first, then inners
      // ordered by pathname.
      Map<String, List<ProxyRepositoryPolicyViolation>> byPathname = new LinkedHashMap<>();
      for (ProxyRepositoryPolicyViolation v : violations) {
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
      String outerPathname = component != null ? component.getPathname() : null;
      Set<String> seenHashes = new java.util.HashSet<>();
      for (Map.Entry<String, List<ProxyRepositoryPolicyViolation>> entry : byPathname.entrySet()) {
        List<ProxyRepositoryPolicyViolation> group = entry.getValue();
        // CLM-40943: only the outer-pathname row gets the bom-hash override. Inner-pathname rows
        // (`outer.zip!/inner.jar`) keep their own per-violation hash, which is already the inner
        // component's HDS identification hash.
        boolean isOuter = outerHashOverride != null && outerPathname != null
            && outerPathname.equals(entry.getKey());
        String hashOverrideForThisGroup = isOuter ? outerHashOverride : null;
        String effectiveHash = effectiveHashForGroup(group, component, hashOverrideForThisGroup);
        String dedupKey = effectiveHash != null ? effectiveHash : "__null_hash__::" + entry.getKey();
        if (!seenHashes.add(dedupKey)) {
          continue;
        }
        aaData.add(buildAaDataEntry(entry.getKey(), group, component, hashOverrideForThisGroup));
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
      final List<ProxyRepositoryPolicyViolation> group,
      final ProxyRepositoryComponent outerComponent)
  {
    return effectiveHashForGroup(group, outerComponent, null);
  }

  private static String effectiveHashForGroup(
      final List<ProxyRepositoryPolicyViolation> group,
      final ProxyRepositoryComponent outerComponent,
      final String outerHashOverride)
  {
    ProxyRepositoryPolicyViolation top = selectTopViolation(group);
    if (top == null) {
      return null;
    }
    // CLM-40943: when caller supplied a bom-hash override for this outer-pathname group, use it
    // unconditionally. This wins over the persisted ProxyRepositoryPolicyViolation.hash, which for
    // npm/nuget is the file SHA1 (`.tgz`/`.nupkg`) — HDS's bom.json carries a different
    // identification hash and the LC Application Report body joins on bom's hash.
    if (outerHashOverride != null) {
      return outerHashOverride;
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
  private static ProxyRepositoryPolicyViolation selectTopViolation(
      final List<ProxyRepositoryPolicyViolation> group)
  {
    if (group == null || group.isEmpty()) {
      return null;
    }
    return group.stream()
        .max(Comparator.comparingInt(ProxyRepositoryPolicyViolation::getThreatLevel)
            .thenComparing(ProxyRepositoryPolicyViolation::getPolicyId,
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
      final List<ProxyRepositoryPolicyViolation> groupViolations,
      final ProxyRepositoryComponent outerComponent)
  {
    return buildAaDataEntry(pathname, groupViolations, outerComponent, null);
  }

  private static ObjectNode buildAaDataEntry(
      final String pathname,
      final List<ProxyRepositoryPolicyViolation> groupViolations,
      final ProxyRepositoryComponent outerComponent,
      final String outerHashOverride)
  {
    ArrayNode active = MAPPER.createArrayNode();
    ArrayNode waived = MAPPER.createArrayNode();
    ArrayNode all = MAPPER.createArrayNode();
    for (ProxyRepositoryPolicyViolation v : groupViolations) {
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
    ProxyRepositoryPolicyViolation top = selectTopViolation(groupViolations);
    if (top == null) {
      throw new IllegalStateException("Empty violation group passed to buildAaDataEntry");
    }

    ObjectNode group = MAPPER.createObjectNode();

    // Identity: prefer the violation row's own (hash, componentIdentifier) — that's the inner
    // artifact's identity for inner pathnames. When the violation row carries no identity (the
    // older single-component shape that pre-dates the multi-`<dir>` fan-out, where violations
    // were minted without per-row hash metadata), fall back to the outer component's identity.
    // This keeps single-component scans byte-for-byte compatible with the pre-fan-out builder.
    // CLM-40943: when the caller supplied an outerHashOverride (bom.json's hash for the outer
    // entry — different from ProxyRepositoryPolicyViolation.hash for npm/nuget/pub formats), use it
    // so the LC Application Report body table can join violations to bom entries by hash.
    String hash;
    if (outerHashOverride != null) {
      hash = outerHashOverride;
    }
    else {
      hash = top.getHash();
      if (hash == null && outerComponent != null && outerComponent.getHash() != null) {
        hash = outerComponent.getHash();
      }
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
      final ProxyRepositoryComponent component) throws Exception
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

  private static byte[] buildBom(final ProxyRepositoryComponent component) throws Exception {
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
      final ProxyRepositoryComponent component) throws Exception
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

  /**
   * Overwrites {@code policyComponentCount} and {@code policyCounts[]} in the HDS-supplied
   * {@code data.json} based on rolled-up {@code ProxyRepositoryPolicyViolation} rows from the IQ
   * evaluator. Mirrors the algorithm in {@code ScanPolicyEvaluator.updateDataJson} — for each
   * UNIQUE component (deduped by effective hash, the same dedup logic
   * {@link #buildPolicyThreats} uses to emit one aaData entry per byte-identical inner jar),
   * take the max threat-level across that component's active (non-waived) violations; the
   * number of buckets with maxThreat≥2 becomes {@code policyComponentCount}, and
   * {@code policyCounts[i]} is the count of buckets whose maxThreat equals {@code i}.
   * <p>
   * Hash-based dedup matters when an archive contains byte-identical copies of the same jar
   * (e.g. {@code lib.jar} and {@code lib (1).jar}): the scanner emits two {@code
   *
  <dir>
   * } entries
   * with identical sha1, the evaluator persists two batches of violations on distinct synthetic
   * pathnames, but they are still ONE logical component. The application-evaluation path dedups
   * the same way; without this, hosted-repo would report one more affected component than the
   * application path for the same upload.
   * <p>
   * Returns the original bytes unchanged if {@code violations} is null/empty or
   * {@code originalDataJson} cannot be parsed as an object node — fail-soft.
   */
  /**
   * Trims the HDS-supplied {@code bom.json}'s {@code aaData[]} array to keep only the entry
   * whose hash matches the outer artifact's hash — used by the identified-outer gate in
   * {@link HostedComponentScanQueueConsumer} so the drill-in Build Report's body component
   * list matches {@code iq-cli <single-file>}'s output for the same binary (per Dariush's
   * Slack confirmation 2026-06-26 "match the CLI scan as though you just passed it the
   * binary").
   * <p>
   * HDS's {@code bom.json.aaData[]} for an identified rubygems / pypi / npm / maven / r
   * artifact contains both the outer itself AND a dependency-manifest expansion (Gemfile.lock
   * entries, requirements.txt entries, package-lock.json entries) that iq-cli does NOT
   * surface for the same binary. Trimming aaData to the outer is the IQ-side equivalent of
   * what iq-cli's bom contains — a one-entry view.
   * <p>
   * Nuget is excluded from the collapse by the caller because nuget's bom entries are real
   * binary DLLs that ship inside the .nupkg (framework-fanout, localization resource DLLs);
   * iq-cli reports all 21, so hosted must too. Caller checks the format set before invoking.
   * <p>
   * Returns the original bytes unchanged if {@code outerHash} is null/empty, if
   * {@code originalBom} cannot be parsed as an object with an array {@code aaData}, or if no
   * aaData entry matches the outer hash — fail-soft so a parse failure can never zero out
   * the body table.
   */
  public static byte[] patchBomKeepOuterOnly(final byte[] originalBom, final String outerHash) {
    if (originalBom == null || originalBom.length == 0 || outerHash == null || outerHash.isEmpty()) {
      return originalBom;
    }
    try {
      JsonNode parsed = MAPPER.readTree(originalBom);
      if (!(parsed instanceof ObjectNode)) {
        return originalBom;
      }
      ObjectNode root = (ObjectNode) parsed;
      JsonNode aaDataNode = root.get("aaData");
      if (!(aaDataNode instanceof ArrayNode)) {
        return originalBom;
      }
      ArrayNode aaData = (ArrayNode) aaDataNode;
      // CLM-40943: rubygems specifically — HDS's bom for a .gem like devise-4.4.0.gem contains
      // many aaData entries that ALL share the same hash (the outer file SHA1 or HDS-derived
      // package hash). Most are `dependency:`-prefixed Gemfile.lock-derived entries. The
      // legitimate outer entry (componentIdentifier coords == devise 4.4.0) is in that same
      // hash-group too. A naive "first hash-matching entry" trim would keep whichever entry
      // HDS happened to list first — frequently a manifest-derived neighbor like
      // activemodel-serializers-xml — and the body row would label the outer's violations
      // with the wrong component name.
      //
      // Two-pass: first try to find an entry that BOTH matches the outer hash AND has no
      // `dependency:`-prefixed pathname (i.e. is direct-identification). Fall back to the
      // first hash-match of any kind if no direct entry exists.
      ArrayNode kept = MAPPER.createArrayNode();
      JsonNode firstHashMatch = null;
      for (JsonNode entry : aaData) {
        if (!entryHashMatches(entry, outerHash)) {
          continue;
        }
        if (firstHashMatch == null) {
          firstHashMatch = entry;
        }
        if (isDirectIdentificationBomEntry(entry)) {
          kept.add(entry);
          break;
        }
      }
      if (kept.size() == 0 && firstHashMatch != null) {
        // No direct-identification entry shared the outer hash; keep the first hash-match
        // entry as a fallback (preserves old behaviour for npm/maven/pypi/single-entry boms).
        kept.add(firstHashMatch);
      }
      if (kept.size() == 0) {
        // No entry matched the outer hash — refuse to write an empty body table. Leaves the
        // overlay as HDS wrote it. Logged at the caller; no exception here so the broader
        // gate path doesn't fail.
        return originalBom;
      }
      root.set("aaData", kept);
      return MAPPER.writeValueAsBytes(root);
    }
    catch (Exception e) {
      return originalBom;
    }
  }

  /**
   * CLM-40943: a bom {@code aaData[]} entry is "direct-identification" iff at least one of its
   * {@code pathnames} does NOT start with {@code "dependency:"}. The {@code "dependency:"}
   * prefix is insight-scanner's marker for components discovered via manifest extraction
   * (Gemfile.lock entry, requirements.txt entry, package-lock.json entry). When HDS returns
   * multiple aaData entries with the same hash — common for rubygems where the .gem file SHA1
   * is shared between the outer entry and all its Gemfile.lock-derived siblings — we want to
   * prefer the direct-identification one so the Build Report body labels violations with the
   * actual uploaded component's coordinates, not a manifest neighbour's.
   * <p>
   * Entries with no {@code pathnames} array are treated as direct (preserves single-entry-bom
   * behaviour for npm/maven/pypi/etc. where HDS doesn't emit pathnames at all).
   */
  private static boolean isDirectIdentificationBomEntry(final JsonNode entry) {
    JsonNode paths = entry.path("pathnames");
    if (!paths.isArray() || paths.isEmpty()) {
      return true;
    }
    for (JsonNode p : paths) {
      String s = p.asText("");
      if (!s.startsWith("dependency:")) {
        return true;
      }
    }
    return false;
  }

  private static boolean entryHashMatches(final JsonNode entry, final String outerHash) {
    if (entry == null || outerHash == null) {
      return false;
    }
    if (outerHash.equalsIgnoreCase(entry.path("hash").asText(""))) {
      return true;
    }
    JsonNode coords = entry.path("componentIdentifier").path("coordinates");
    if (outerHash.equalsIgnoreCase(coords.path("sha1").asText(""))
        || outerHash.equalsIgnoreCase(coords.path("sha256").asText(""))
        || outerHash.equalsIgnoreCase(coords.path("hash").asText("")))
    {
      return true;
    }
    return false;
  }

  /**
   * Overwrites {@code totalArtifactCount} and {@code knownArtifactCount} in the HDS-supplied
   * {@code data.json} so the Build Report header ("X COMPONENTS, N% of all components
   * identified") matches the Hosted Repos list COMPONENTS column for the same outer artifact.
   * <p>
   * HDS's view of {@code totalArtifactCount} reflects its full bom expansion, which for
   * formats whose payload includes a dependency manifest (Gemfile.lock inside a .gem,
   * requirements.txt inside a sdist, package-lock.json inside an npm tarball) inflates beyond
   * what is physically present in the artifact. Per Ross's guidance the hosted-repo scan
   * should report only physically-present components, so this method writes the directly-
   * identified count as {@code totalArtifactCount} (the "X COMPONENTS" total).
   * <p>
   * <b>Single-arg overload behavior</b>: also writes {@code knownArtifactCount = directCount},
   * which is only correct when the caller has already established that every counted component
   * is a known match (e.g. the CLM-42117 identified-outer collapse gate where the outer's
   * matchState was verified before invoking this patcher). For paths where the counted
   * components may include {@code matchState=unknown} entries (e.g. helm charts of custom
   * operators, proprietary archives), use {@link #patchDataJsonTotalArtifactCount(byte[], int, int)}
   * to pass an explicit {@code knownCount} so the header "% identified" percentage reflects the
   * real matchState distribution rather than falsely reading 100%.
   * <p>
   * Returns the original bytes unchanged if {@code directCount} is negative or
   * {@code originalDataJson} cannot be parsed as an object node — fail-soft.
   */
  public static byte[] patchDataJsonTotalArtifactCount(
      final byte[] originalDataJson,
      final int directCount)
  {
    return patchDataJsonTotalArtifactCount(originalDataJson, directCount, directCount);
  }

  /**
   * Overload that accepts an explicit {@code knownCount} — the number of components whose
   * {@code matchState} is one of {@code exact}, {@code similar}, or {@code embedded}. Use this
   * from paths where {@code directCount} may include unknown-matchState components so the
   * "% identified" percentage reports the true fraction rather than falsely reading 100%.
   * <p>
   * {@code knownCount} is clamped to {@code [0, directCount]} defensively; a caller that
   * over-counts will still produce a well-formed &lt;=100% percentage in the UI.
   * <p>
   * Returns the original bytes unchanged if either count is negative or the JSON cannot be
   * parsed as an object node — fail-soft.
   */
  public static byte[] patchDataJsonTotalArtifactCount(
      final byte[] originalDataJson,
      final int directCount,
      final int knownCount)
  {
    if (originalDataJson == null || originalDataJson.length == 0 || directCount < 0 || knownCount < 0) {
      return originalDataJson;
    }
    try {
      JsonNode parsed = MAPPER.readTree(originalDataJson);
      if (!(parsed instanceof ObjectNode)) {
        return originalDataJson;
      }
      ObjectNode data = (ObjectNode) parsed;
      data.put("totalArtifactCount", directCount);
      data.put("knownArtifactCount", Math.min(knownCount, directCount));
      return MAPPER.writeValueAsBytes(data);
    }
    catch (Exception e) {
      return originalDataJson;
    }
  }

  /**
   * Dedupe HDS's {@code bom.json.aaData} by dropping "sparse" file-SHA1 self-mirror shadows
   * whose {@code (format, coordinates)} is already covered by a "rich" HDS-identified entry.
   * Rich and sparse are distinguished by identification-metadata presence, not by hash or
   * pathnames (both of which differ across the two shapes in real HDS output).
   * <p>
   * <b>Rich:</b> carries at least one of {@code packageUrl}, {@code identificationSource}, or
   * {@code aggregateFiles} — HDS's real identification with full metadata.
   * <br>
   * <b>Sparse:</b> file-SHA1 self-mirror shadow with none of those — {@code createTime=0},
   * empty inner {@code aaData}. HDS emits these alongside the rich entry for npm/pypi/rubygems
   * singles; without dedupe, {@code knownArtifactCount} exceeds {@code totalArtifactCount}
   * (dot-prop 4.2.0 reads 200% identified).
   * <p>
   * <b>Nuget framework fanout preserved:</b> distinct DLLs at {@code /lib/net40/} vs
   * {@code /lib/net45/} inside a {@code .nupkg} are both rich (both are first-class HDS
   * identifications) with the same {@code (format, coordinates)} but different hashes — the
   * shape check keeps both because neither is a sparse shadow of the other.
   * <p>
   * <b>Unknown matchState:</b> preserved as-is regardless of coord collision — HDS uses those
   * for placeholders whose meaning may differ.
   * <p>
   * <b>Keep-first invariant:</b> callers downstream ({@link com.sonatype.insight.brain.repository
   * .hosted.HostedComponentScanQueueConsumer#extractBomOuterHash}, {@link #patchBomKeepOuterOnly})
   * read {@code aaData[0].hash} as the outer's join key. Only sparse duplicates are dropped, so
   * the original first entry survives when it's the rich one; when the sparse one happens to
   * come first (uncommon), we still keep the rich entry — the caller reads whichever entry now
   * sits at index 0, and either hash is a valid join key since both derive from the same coord.
   * <p>
   * Returns the original bytes unchanged when {@code aaData} has less than 2 entries, when no
   * sparse duplicates are found, or when parsing fails — fail-soft.
   */
  public static byte[] dedupeBomIdentifiedRows(final byte[] bomBytes) {
    if (bomBytes == null || bomBytes.length == 0) {
      return bomBytes;
    }
    try {
      JsonNode root = MAPPER.readTree(bomBytes);
      if (!(root instanceof ObjectNode)) {
        return bomBytes;
      }
      JsonNode aa = root.path("aaData");
      if (!aa.isArray() || aa.size() < 2) {
        return bomBytes;
      }
      // Pass 1: collect (format, coords) keys that have at least one rich entry. Any sparse
      // entry sharing that key is a self-mirror shadow and gets dropped in pass 2.
      java.util.Set<String> keysWithRichEntry = new java.util.HashSet<>();
      for (JsonNode e : aa) {
        if (!isKnownMatchState(e) || !isRichIdentifiedBomEntry(e)) {
          continue;
        }
        keysWithRichEntry.add(bomCoordKey(e));
      }
      // Pass 2: keep every rich, every unknown-matchState, every sparse whose coord has no
      // rich counterpart. Drop only sparse entries that are shadowed by a rich sibling.
      ArrayNode kept = MAPPER.createArrayNode();
      int dropped = 0;
      for (JsonNode e : aa) {
        if (!isKnownMatchState(e)) {
          kept.add(e);
          continue;
        }
        if (!isRichIdentifiedBomEntry(e) && keysWithRichEntry.contains(bomCoordKey(e))) {
          dropped++;
          continue;
        }
        kept.add(e);
      }
      if (dropped == 0) {
        return bomBytes;
      }
      ((ObjectNode) root).set("aaData", kept);
      return MAPPER.writeValueAsBytes(root);
    }
    catch (Exception ex) {
      return bomBytes;
    }
  }

  private static boolean isKnownMatchState(final JsonNode entry) {
    String ms = entry.path("matchState").asText("").toLowerCase();
    return "exact".equals(ms) || "similar".equals(ms) || "embedded".equals(ms);
  }

  /**
   * A rich HDS-identified bom entry carries at least one of {@code packageUrl},
   * {@code identificationSource}, or {@code aggregateFiles}. The complementary sparse shape
   * (HDS's file-SHA1 self-mirror shadow) has none of these — {@code createTime=0}, empty inner
   * {@code aaData}. The three fields are checked with {@link JsonNode#hasNonNull(String)} so an
   * explicitly-null field is treated the same as an absent one.
   */
  private static boolean isRichIdentifiedBomEntry(final JsonNode entry) {
    return entry.hasNonNull("packageUrl")
        || entry.hasNonNull("identificationSource")
        || entry.hasNonNull("aggregateFiles");
  }

  private static String bomCoordKey(final JsonNode entry) {
    JsonNode ci = entry.path("componentIdentifier");
    String format = ci.path("format").asText("");
    JsonNode coords = ci.path("coordinates");
    return format + "::" + (coords.isObject() ? coords.toString() : "");
  }

  /**
   * Overwrites just {@code knownArtifactCount} in {@code data.json} to the given value,
   * clamped to {@code [0, totalArtifactCount]}. Leaves {@code totalArtifactCount} and every
   * other field untouched. Used after {@link #dedupeBomIdentifiedRows} in the non-collapse
   * path where {@code totalArtifactCount} is whatever HDS wrote but {@code knownArtifactCount}
   * needs to be reduced to the deduped known-match count so the header shows a
   * &lt;=100% percentage instead of HDS's over-count.
   * <p>
   * Returns the original bytes unchanged when {@code newKnownCount} is negative, when
   * {@code data.json} cannot be parsed, or when the existing {@code knownArtifactCount}
   * already equals the clamped value — fail-soft.
   */
  public static byte[] patchDataJsonKnownArtifactCountOnly(
      final byte[] originalDataJson,
      final int newKnownCount)
  {
    if (originalDataJson == null || originalDataJson.length == 0 || newKnownCount < 0) {
      return originalDataJson;
    }
    try {
      JsonNode parsed = MAPPER.readTree(originalDataJson);
      if (!(parsed instanceof ObjectNode)) {
        return originalDataJson;
      }
      ObjectNode data = (ObjectNode) parsed;
      int total = data.path("totalArtifactCount").asInt(-1);
      int clamped = total >= 0 ? Math.min(newKnownCount, total) : newKnownCount;
      int existing = data.path("knownArtifactCount").asInt(-1);
      if (existing == clamped) {
        return originalDataJson;
      }
      data.put("knownArtifactCount", clamped);
      return MAPPER.writeValueAsBytes(data);
    }
    catch (Exception e) {
      return originalDataJson;
    }
  }

  /**
   * Counts entries in a {@code bom.json} byte payload whose {@code matchState} is one of
   * {@code exact}, {@code similar}, or {@code embedded} (case-insensitive). Returns 0 when the
   * payload is null/empty, {@code aaData} is missing/not-array, or parsing fails — fail-soft.
   * <p>
   * Callers use this to compute {@code knownArtifactCount} for {@link
   * #patchDataJsonTotalArtifactCount(byte[], int, int)}. The three known match states match
   * what LC's iq-cli counts toward "identified"; anything else ({@code unknown}, {@code partial},
   * absent) is treated as not identified.
   */
  public static int countKnownMatchesInBom(final byte[] bomBytes) {
    if (bomBytes == null || bomBytes.length == 0) {
      return 0;
    }
    try {
      JsonNode root = MAPPER.readTree(bomBytes);
      JsonNode aa = root.path("aaData");
      if (!aa.isArray()) {
        return 0;
      }
      int n = 0;
      for (JsonNode e : aa) {
        String ms = e.path("matchState").asText("").toLowerCase();
        if ("exact".equals(ms) || "similar".equals(ms) || "embedded".equals(ms)) {
          n++;
        }
      }
      return n;
    }
    catch (Exception ex) {
      return 0;
    }
  }

  public static byte[] patchDataJsonPolicyCounts(
      final byte[] originalDataJson,
      final ProxyRepositoryComponent outerComponent,
      final List<ProxyRepositoryPolicyViolation> violations)
  {
    return patchDataJsonPolicyCounts(originalDataJson, outerComponent, violations, null);
  }

  /**
   * Overload accepting {@code outerHashOverride} — the hash bom.json carries for the outer
   * entry, used as the dedup key for the outer-pathname group so the count of buckets in
   * data.json's {@code policyComponentCount} matches what {@code policythreats.json} (built with
   * the same override) emits. See {@link #build(String, ProxyRepositoryComponent, List, String)}
   * for the rationale on per-format hash divergence.
   */
  public static byte[] patchDataJsonPolicyCounts(
      final byte[] originalDataJson,
      final ProxyRepositoryComponent outerComponent,
      final List<ProxyRepositoryPolicyViolation> violations,
      final String outerHashOverride)
  {
    if (originalDataJson == null || originalDataJson.length == 0
        || violations == null || violations.isEmpty())
    {
      return originalDataJson;
    }
    try {
      JsonNode parsed = MAPPER.readTree(originalDataJson);
      if (!(parsed instanceof ObjectNode)) {
        return originalDataJson;
      }
      ObjectNode data = (ObjectNode) parsed;

      // Pass 1: group active (non-waived) violations by pathname — the natural unit produced
      // by the evaluator (one batch per (repository, pathname)).
      Map<String, List<ProxyRepositoryPolicyViolation>> byPathname = new LinkedHashMap<>();
      for (ProxyRepositoryPolicyViolation v : violations) {
        if (v.isWaived() || v.getPathname() == null) {
          continue;
        }
        byPathname.computeIfAbsent(v.getPathname(), k -> new java.util.ArrayList<>()).add(v);
      }

      // Pass 2: dedup by effective hash — same dedup key buildPolicyThreats uses so the count
      // of buckets here matches the count of aaData entries in policythreats.json (which is
      // what the application-evaluation path also produces). Null-effective-hash groups fall
      // back to the pathname as the dedup key so two distinct null-hash groups still count as
      // two components (no false collapse).
      String outerPathname = outerComponent != null ? outerComponent.getPathname() : null;
      Set<String> seenHashes = new java.util.HashSet<>();
      int[] policyCounts = new int[11];
      int policyComponentCount = 0;
      for (Map.Entry<String, List<ProxyRepositoryPolicyViolation>> entry : byPathname.entrySet()) {
        List<ProxyRepositoryPolicyViolation> group = entry.getValue();
        // CLM-40943: apply the outerHashOverride only to the outer-pathname group so the dedup
        // key matches buildPolicyThreats's emitted hash for that aaData entry.
        boolean isOuter = outerHashOverride != null && outerPathname != null
            && outerPathname.equals(entry.getKey());
        String effectiveHash = effectiveHashForGroup(group, outerComponent, isOuter ? outerHashOverride : null);
        String dedupKey = effectiveHash != null ? effectiveHash : "__null_hash__::" + entry.getKey();
        if (!seenHashes.add(dedupKey)) {
          continue;
        }
        int maxThreat = group.stream()
            .mapToInt(ProxyRepositoryPolicyViolation::getThreatLevel)
            .max()
            .orElse(0);
        int bucketed = maxThreat < 0 ? 0 : Math.min(maxThreat, 10);
        policyCounts[bucketed]++;
        if (maxThreat >= 2) {
          policyComponentCount++;
        }
      }

      ArrayNode countsNode = data.putArray("policyCounts");
      for (int c : policyCounts) {
        countsNode.add(c);
      }
      data.put("policyComponentCount", policyComponentCount);
      return MAPPER.writeValueAsBytes(data);
    }
    catch (Exception e) {
      return originalDataJson;
    }
  }

  /**
   * Zero {@code policyCounts[]} and {@code policyComponentCount} in {@code data.json}. Used by
   * the collapse gate's rebuild path when the surviving outer-violation set is empty —
   * {@link #patchDataJsonPolicyCounts} short-circuits on empty input and returns the original
   * bytes unchanged, which would leave HDS's pre-collapse expanded counts in the file even
   * though the collapse decision reports one component with no active violations.
   * <p>
   * Returns the original bytes unchanged when the JSON can't be parsed as an object — fail-soft.
   */
  public static byte[] zeroDataJsonPolicyCounts(final byte[] originalDataJson) {
    if (originalDataJson == null || originalDataJson.length == 0) {
      return originalDataJson;
    }
    try {
      JsonNode parsed = MAPPER.readTree(originalDataJson);
      if (!(parsed instanceof ObjectNode)) {
        return originalDataJson;
      }
      ObjectNode data = (ObjectNode) parsed;
      ArrayNode counts = data.putArray("policyCounts");
      for (int i = 0; i < 11; i++) {
        counts.add(0);
      }
      data.put("policyComponentCount", 0);
      return MAPPER.writeValueAsBytes(data);
    }
    catch (Exception e) {
      return originalDataJson;
    }
  }

  /**
   * Writes {@code policyComponentCount} into the HDS-supplied {@code data.json} ONLY when the
   * field is absent. HDS emits the field for nested/bundled formats (nuget, helm, pub, conda,
   * npm/pypi/rubygems bundled) but omits it for non-nested single artifacts (Maven, PyPI single,
   * RubyGems single, R CRAN); when absent, the report header's "Affecting N components" pill
   * falls back to 0 in the UI even when violations exist ({@code ReportStatusBar.jsx:21,90}).
   * <p>
   * Companion to {@link #patchDataJsonPolicyCounts} that ONLY touches this single field —
   * {@code policyCounts[]} is left untouched so HDS's threat-level breakdowns (Critical/Severe/
   * Moderate pills) flow through unchanged. That was the reason the full-recompute
   * {@code patchDataJsonPolicyCounts} was removed in CLM-41737: recomputing {@code policyCounts[]}
   * from IQ-side rolled-up violations diverged from HDS's view for archives that bundle other
   * components. Restoring only the missing field closes the "Affecting 0" gap without
   * re-introducing that divergence.
   * <p>
   * The count is computed from {@code violations} using the same pathname-group + effective-hash
   * dedup as {@link #buildPolicyThreats}, so the number here matches
   * {@code policythreats.json.aaData.length} by construction. Waived violations are excluded.
   * <p>
   * Returns the original bytes unchanged when {@code policyComponentCount} is already present in
   * {@code originalDataJson}, or when the JSON cannot be parsed as an object — fail-soft.
   */
  public static byte[] patchDataJsonPolicyComponentCountIfAbsent(
      final byte[] originalDataJson,
      final ProxyRepositoryComponent outerComponent,
      final List<ProxyRepositoryPolicyViolation> violations,
      final String outerHashOverride)
  {
    if (originalDataJson == null || originalDataJson.length == 0) {
      return originalDataJson;
    }
    try {
      JsonNode parsed = MAPPER.readTree(originalDataJson);
      if (!(parsed instanceof ObjectNode)) {
        return originalDataJson;
      }
      ObjectNode data = (ObjectNode) parsed;
      if (!data.path("policyComponentCount").isMissingNode()) {
        return originalDataJson;
      }
      int policyComponentCount = countPolicyAffectedComponents(outerComponent, violations, outerHashOverride);
      data.put("policyComponentCount", policyComponentCount);
      return MAPPER.writeValueAsBytes(data);
    }
    catch (Exception e) {
      return originalDataJson;
    }
  }

  /**
   * Counts unique components (deduped by effective hash, same key as {@link #buildPolicyThreats})
   * whose max active-violation threat level is at least 2. Mirrors the {@code maxThreat >= 2}
   * threshold used by {@link #patchDataJsonPolicyCounts} and {@code ScanPolicyEvaluator.updateDataJson}
   * so hosted-repo and application-evaluation paths compute the same value for the same violation set.
   */
  private static int countPolicyAffectedComponents(
      final ProxyRepositoryComponent outerComponent,
      final List<ProxyRepositoryPolicyViolation> violations,
      final String outerHashOverride)
  {
    if (violations == null || violations.isEmpty()) {
      return 0;
    }
    Map<String, List<ProxyRepositoryPolicyViolation>> byPathname = new LinkedHashMap<>();
    for (ProxyRepositoryPolicyViolation v : violations) {
      if (v.isWaived() || v.getPathname() == null) {
        continue;
      }
      byPathname.computeIfAbsent(v.getPathname(), k -> new java.util.ArrayList<>()).add(v);
    }
    String outerPathname = outerComponent != null ? outerComponent.getPathname() : null;
    Set<String> seenHashes = new java.util.HashSet<>();
    int count = 0;
    for (Map.Entry<String, List<ProxyRepositoryPolicyViolation>> entry : byPathname.entrySet()) {
      List<ProxyRepositoryPolicyViolation> group = entry.getValue();
      boolean isOuter = outerHashOverride != null && outerPathname != null
          && outerPathname.equals(entry.getKey());
      String effectiveHash = effectiveHashForGroup(group, outerComponent, isOuter ? outerHashOverride : null);
      String dedupKey = effectiveHash != null ? effectiveHash : "__null_hash__::" + entry.getKey();
      if (!seenHashes.add(dedupKey)) {
        continue;
      }
      int maxThreat = group.stream()
          .mapToInt(ProxyRepositoryPolicyViolation::getThreatLevel)
          .max()
          .orElse(0);
      if (maxThreat >= 2) {
        count++;
      }
    }
    return count;
  }

  /**
   * Sums {@code max(threatLevel)} over distinct {@code (hash, constraintFactsId)}, mirroring LC's
   * {@code PolicyViolationComparator}: distinct CVEs and distinct binary instances each count.
   * Waived rows are excluded; a null hash or constraintFactsId falls back to the row id so the row
   * contributes once (never over-collapses). Since constraintFactsId is backfilled asynchronously,
   * not-yet-backfilled rows use that fallback until the backfill completes.
   * <p>
   * Callers pass the {@link #excludeOuterViolationsForFormat} output so the row set already matches
   * LC's component set. {@code outerComponent} is unused, kept for call-site symmetry.
   */
  public static int totalRisk(
      final ProxyRepositoryComponent outerComponent,
      final List<ProxyRepositoryPolicyViolation> violations)
  {
    if (violations == null || violations.isEmpty()) {
      return 0;
    }
    Map<String, Integer> maxThreatByKey = new LinkedHashMap<>();
    for (ProxyRepositoryPolicyViolation v : violations) {
      if (v.isWaived()) {
        continue;
      }
      String hashKey = v.getHash() != null ? v.getHash() : "__id__::" + v.getId();
      String constraintKey = v.getConstraintFactsId() != null ? v.getConstraintFactsId() : "__id__::" + v.getId();
      maxThreatByKey.merge(hashKey + "::" + constraintKey, v.getThreatLevel(), Math::max);
    }
    return maxThreatByKey.values()
        .stream()
        .mapToInt(Integer::intValue)
        .sum();
  }

  private static byte[] buildData(
      final ProxyRepositoryComponent component,
      final List<ProxyRepositoryPolicyViolation> violations)
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
