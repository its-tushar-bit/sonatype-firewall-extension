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
    return build(name, component, violations, null);
  }

  /**
   * CLM-40943: overload that accepts an {@code outerHashOverride} — the hash HDS's
   * {@code bom.json} uses for the outer component. When the bom hash differs from
   * {@code repository_component.hash} (npm and most nuget formats — file SHA1 vs HDS-derived
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
      final RepositoryComponent component,
      final List<RepositoryPolicyViolation> violations,
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
      final RepositoryComponent component,
      final List<RepositoryPolicyViolation> violations) throws Exception
  {
    return buildPolicyThreats(component, violations, null);
  }

  private static byte[] buildPolicyThreats(
      final RepositoryComponent component,
      final List<RepositoryPolicyViolation> violations,
      final String outerHashOverride) throws Exception
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
      String outerPathname = component != null ? component.getPathname() : null;
      Set<String> seenHashes = new java.util.HashSet<>();
      for (Map.Entry<String, List<RepositoryPolicyViolation>> entry : byPathname.entrySet()) {
        List<RepositoryPolicyViolation> group = entry.getValue();
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
      final List<RepositoryPolicyViolation> group,
      final RepositoryComponent outerComponent)
  {
    return effectiveHashForGroup(group, outerComponent, null);
  }

  private static String effectiveHashForGroup(
      final List<RepositoryPolicyViolation> group,
      final RepositoryComponent outerComponent,
      final String outerHashOverride)
  {
    RepositoryPolicyViolation top = selectTopViolation(group);
    if (top == null) {
      return null;
    }
    // CLM-40943: when caller supplied a bom-hash override for this outer-pathname group, use it
    // unconditionally. This wins over the persisted RepositoryPolicyViolation.hash, which for
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
    return buildAaDataEntry(pathname, groupViolations, outerComponent, null);
  }

  private static ObjectNode buildAaDataEntry(
      final String pathname,
      final List<RepositoryPolicyViolation> groupViolations,
      final RepositoryComponent outerComponent,
      final String outerHashOverride)
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
    // CLM-40943: when the caller supplied an outerHashOverride (bom.json's hash for the outer
    // entry — different from RepositoryPolicyViolation.hash for npm/nuget/pub formats), use it
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

  /**
   * Overwrites {@code policyComponentCount} and {@code policyCounts[]} in the HDS-supplied
   * {@code data.json} based on rolled-up {@code RepositoryPolicyViolation} rows from the IQ
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
   * {@code data.json} so the Build Report header ("X COMPONENTS, 100% of all components
   * identified") matches the Hosted Repos list COMPONENTS column for the same outer artifact.
   * <p>
   * HDS's view of {@code totalArtifactCount} reflects its full bom expansion, which for
   * formats whose payload includes a dependency manifest (Gemfile.lock inside a .gem,
   * requirements.txt inside a sdist, package-lock.json inside an npm tarball) inflates beyond
   * what is physically present in the artifact. Per Ross's guidance the hosted-repo scan
   * should report only physically-present components, so this method writes the directly-
   * identified count both as {@code totalArtifactCount} (the "X COMPONENTS" total) and as
   * {@code knownArtifactCount} (the numerator of the "% identified" percentage) so the header
   * stays internally consistent.
   * <p>
   * Returns the original bytes unchanged if {@code directCount} is negative or
   * {@code originalDataJson} cannot be parsed as an object node — fail-soft.
   */
  public static byte[] patchDataJsonTotalArtifactCount(
      final byte[] originalDataJson,
      final int directCount)
  {
    if (originalDataJson == null || originalDataJson.length == 0 || directCount < 0) {
      return originalDataJson;
    }
    try {
      JsonNode parsed = MAPPER.readTree(originalDataJson);
      if (!(parsed instanceof ObjectNode)) {
        return originalDataJson;
      }
      ObjectNode data = (ObjectNode) parsed;
      data.put("totalArtifactCount", directCount);
      data.put("knownArtifactCount", directCount);
      return MAPPER.writeValueAsBytes(data);
    }
    catch (Exception e) {
      return originalDataJson;
    }
  }

  public static byte[] patchDataJsonPolicyCounts(
      final byte[] originalDataJson,
      final RepositoryComponent outerComponent,
      final List<RepositoryPolicyViolation> violations)
  {
    return patchDataJsonPolicyCounts(originalDataJson, outerComponent, violations, null);
  }

  /**
   * Overload accepting {@code outerHashOverride} — the hash bom.json carries for the outer
   * entry, used as the dedup key for the outer-pathname group so the count of buckets in
   * data.json's {@code policyComponentCount} matches what {@code policythreats.json} (built with
   * the same override) emits. See {@link #build(String, RepositoryComponent, List, String)}
   * for the rationale on per-format hash divergence.
   */
  public static byte[] patchDataJsonPolicyCounts(
      final byte[] originalDataJson,
      final RepositoryComponent outerComponent,
      final List<RepositoryPolicyViolation> violations,
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
      Map<String, List<RepositoryPolicyViolation>> byPathname = new LinkedHashMap<>();
      for (RepositoryPolicyViolation v : violations) {
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
      for (Map.Entry<String, List<RepositoryPolicyViolation>> entry : byPathname.entrySet()) {
        List<RepositoryPolicyViolation> group = entry.getValue();
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
            .mapToInt(RepositoryPolicyViolation::getThreatLevel)
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
