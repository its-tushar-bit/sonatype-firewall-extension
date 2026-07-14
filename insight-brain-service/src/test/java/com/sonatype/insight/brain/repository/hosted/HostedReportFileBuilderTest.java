/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.List;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HostedReportFileBuilderTest
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static RepositoryComponent component(String hash) {
    RepositoryComponent c = new RepositoryComponent();
    c.setHash(hash);
    c.setPathname("com/example/lib-1.0.jar");
    return c;
  }

  private static RepositoryPolicyViolation violation(String policyId, String policyName, int threatLevel) {
    RepositoryPolicyViolation v = new RepositoryPolicyViolation();
    v.setId("vid-" + policyId);
    v.setPolicyId(policyId);
    v.setPolicyName(policyName);
    v.setThreatLevel(threatLevel);
    v.setWaived(false);
    return v;
  }

  @Test
  public void buildPolicyThreats_multipleViolations_groupUsesHighestThreatViolation() throws Exception {
    RepositoryComponent comp = component("abc123");
    RepositoryPolicyViolation low = violation("pol-low", "Low Policy", 3);
    RepositoryPolicyViolation high = violation("pol-high", "High Policy", 10);
    RepositoryPolicyViolation mid = violation("pol-mid", "Mid Policy", 7);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of(low, high, mid));

    JsonNode root = MAPPER.readTree(result);
    JsonNode group = root.path("aaData").get(0);

    assertThat(group.path("policyId").asText()).isEqualTo("pol-high");
    assertThat(group.path("policyName").asText()).isEqualTo("High Policy");
    assertThat(group.path("policyThreatLevel").asInt()).isEqualTo(10);
  }

  @Test
  public void buildPolicyThreats_singleViolation_groupUseThatViolation() throws Exception {
    RepositoryComponent comp = component("abc123");
    RepositoryPolicyViolation only = violation("pol-only", "Only Policy", 5);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of(only));

    JsonNode root = MAPPER.readTree(result);
    JsonNode group = root.path("aaData").get(0);

    assertThat(group.path("policyId").asText()).isEqualTo("pol-only");
    assertThat(group.path("policyThreatLevel").asInt()).isEqualTo(5);
  }

  @Test
  public void buildPolicyThreats_allViolationsContainedInGroup() throws Exception {
    RepositoryComponent comp = component("abc123");
    RepositoryPolicyViolation v1 = violation("pol-a", "Policy A", 3);
    RepositoryPolicyViolation v2 = violation("pol-b", "Policy B", 10);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of(v1, v2));

    JsonNode root = MAPPER.readTree(result);
    JsonNode group = root.path("aaData").get(0);

    assertThat(group.path("allViolations").size()).isEqualTo(2);
    assertThat(group.path("activeViolations").size()).isEqualTo(2);
    assertThat(group.path("waivedViolations").size()).isEqualTo(0);
  }

  @Test
  public void buildPolicyThreats_noViolations_emptyAaData() throws Exception {
    RepositoryComponent comp = component("abc123");

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of());

    JsonNode root = MAPPER.readTree(result);
    assertThat(root.path("aaData").size()).isEqualTo(0);
  }

  /**
   * Archive-of-archives upload: violations attached to the outer pathname plus violations attached
   * to two inner pathnames must produce three distinct {@code aaData} groups, each carrying the
   * violation row's own {@code (hash, componentIdentifier)} so the report shows per-inner
   * findings instead of lumping them under the outer's identity.
   */
  @Test
  public void buildPolicyThreats_violationsOnMultiplePathnames_oneGroupPerPathname() throws Exception {
    String outerPath = "com/example/bundle-1.0.zip";
    RepositoryComponent outerComp = new RepositoryComponent();
    outerComp.setHash("outer_hash_001");
    outerComp.setPathname(outerPath);
    ComponentIdentifier outerCi = ci("com.example", "bundle", "1.0", "zip");
    outerComp.setComponentIdentifier(outerCi);

    RepositoryPolicyViolation outerV =
        violationOn(outerPath, "outer_hash_001", outerCi, "pol-outer", "Outer Policy", 4);
    RepositoryPolicyViolation log4jV = violationOn(outerPath + "!/log4j-core-2.14.1.jar",
        "inner_log4j_hash", ci("org.apache.logging.log4j", "log4j-core", "2.14.1", "jar"),
        "pol-cve", "Log4j CVE", 10);
    RepositoryPolicyViolation cliV = violationOn(outerPath + "!/commons-cli-1.9.0.jar",
        "inner_cli_hash_x", ci("commons-cli", "commons-cli", "1.9.0", "jar"),
        "pol-arch", "Architecture", 1);

    byte[] result = HostedReportFileBuilder.build(
        "policythreats.json", outerComp, List.of(outerV, log4jV, cliV));

    JsonNode root = MAPPER.readTree(result);
    JsonNode aaData = root.path("aaData");
    assertThat(aaData.size()).as("one group per pathname").isEqualTo(3);

    JsonNode outerGroup = aaData.get(0);
    assertThat(outerGroup.path("hash").asText()).isEqualTo("outer_hash_001");
    assertThat(outerGroup.path("policyId").asText()).isEqualTo("pol-outer");

    JsonNode log4jGroup = aaData.get(1);
    assertThat(log4jGroup.path("hash").asText()).isEqualTo("inner_log4j_hash");
    assertThat(log4jGroup.path("policyId").asText()).isEqualTo("pol-cve");
    assertThat(log4jGroup.path("policyThreatLevel").asInt()).isEqualTo(10);

    JsonNode cliGroup = aaData.get(2);
    assertThat(cliGroup.path("hash").asText()).isEqualTo("inner_cli_hash_x");
    assertThat(cliGroup.path("policyId").asText()).isEqualTo("pol-arch");
  }

  /**
   * Two byte-identical jars in an archive (e.g. {@code lib.jar} and {@code lib (1).jar}) produce
   * two scanner {@code
   *
  <dir>
   * } entries with the same {@code sha1}. The evaluator persists two
   * batches of violations against distinct synthetic pathnames. Without dedup, the synthesised
   * aaData would carry two entries with the same hash — and the downstream PDF generator
   * ({@code ApiReportDataServiceV2.getPolicyViolationsByHash}) does {@code Collectors.toMap}
   * keyed on hash without a merge function, which throws "Duplicate key" and 500s the report
   * export. The {@code seenHashes} guard in {@code buildPolicyThreats} keeps only the first
   * pathname's batch per unique hash; identical content has identical findings so the second
   * batch is safe to drop.
   */
  @Test
  public void buildPolicyThreats_duplicateHashes_dedupedToOneAaDataEntry() throws Exception {
    String outerPath = "com/example/bundle-1.0.zip";
    RepositoryComponent outerComp = new RepositoryComponent();
    outerComp.setHash("outer_hash_dup_01");
    outerComp.setPathname(outerPath);

    String sameHash = "spring_beans_hash";
    RepositoryPolicyViolation outerV =
        violationOn(outerPath, "outer_hash_dup_01", null, "pol-outer", "Outer Policy", 2);
    RepositoryPolicyViolation copyA = violationOn(outerPath + "!/spring-beans-5.3.17.jar",
        sameHash, ci("org.springframework", "spring-beans", "5.3.17", "jar"),
        "pol-spring-cve", "Security-Critical", 10);
    RepositoryPolicyViolation copyB = violationOn(outerPath + "!/spring-beans-5.3.17 (1).jar",
        sameHash, ci("org.springframework", "spring-beans", "5.3.17", "jar"),
        "pol-spring-cve", "Security-Critical", 10);

    byte[] result = HostedReportFileBuilder.build(
        "policythreats.json", outerComp, List.of(outerV, copyA, copyB));

    JsonNode aaData = MAPPER.readTree(result).path("aaData");
    // Outer + ONE entry for the duplicate-hash pair (the second copy is dropped). Without
    // dedup this would be 3 entries with two sharing the same hash, crashing the PDF export.
    assertThat(aaData.size()).as("dedup keeps one entry per unique hash").isEqualTo(2);
    // Confirm only one aaData has the duplicated hash, not two.
    long hashOccurrences = java.util.stream.StreamSupport
        .stream(aaData.spliterator(), false)
        .filter(n -> sameHash.equals(n.path("hash").asText()))
        .count();
    assertThat(hashOccurrences).as("same-hash collapses to a single aaData entry").isEqualTo(1L);
  }

  /**
   * Two distinct pathnames whose violation rows both have null hash must NOT collapse into a
   * single aaData entry — the dedup logic falls back to the pathname as the dedup key when the
   * effective hash is null, so each null-hash group survives. Without the per-pathname fallback,
   * both groups would dedup against the empty-string key (because the dedup hash and emitted
   * hash both resolve to {@code ""} when null) and only one would emit, silently dropping
   * violations from the report.
   */
  @Test
  public void buildPolicyThreats_twoNullHashGroupsOnDifferentPathnames_bothEmit() throws Exception {
    String outerPath = "com/example/bundle-1.0.zip";
    RepositoryComponent outerComp = new RepositoryComponent();
    outerComp.setPathname(outerPath);
    // No hash on outerComp either — the fallback to outerComp.getHash() is also null, so the
    // effective hash is null for every group.

    RepositoryPolicyViolation v1 = violationOn(outerPath + "!/a.jar", null, null, "pol-a", "Policy A", 5);
    RepositoryPolicyViolation v2 = violationOn(outerPath + "!/b.jar", null, null, "pol-b", "Policy B", 5);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", outerComp, List.of(v1, v2));

    JsonNode aaData = MAPPER.readTree(result).path("aaData");
    assertThat(aaData.size())
        .as("two distinct null-hash pathnames must both emit; pathname is the dedup-key fallback")
        .isEqualTo(2);
  }

  /**
   * The dedup key and the emitted hash must be the same value. Earlier the dedup keyed on
   * {@code group.get(0).getHash()} while the emitted hash came from {@code top.getHash()}
   * (max-threat). For a group whose top-threat violation differed from group[0] in some way
   * those could disagree and silently drop a still-unique aaData entry. This test pins the
   * invariant: when the top-threat violation's hash is unique relative to other groups, the
   * group emits — even if group[0]'s hash happens to collide with another group.
   */
  @Test
  public void buildPolicyThreats_dedupKeysOnTopViolationHash_notFirstViolationHash() throws Exception {
    String outerPath = "com/example/bundle-1.0.zip";
    RepositoryComponent outerComp = new RepositoryComponent();
    outerComp.setPathname(outerPath);

    // group A: first-encountered violation has hash "shared", but top-threat has hash "uniqueA"
    RepositoryPolicyViolation aLow = violationOn(outerPath + "!/a.jar", "shared", null, "pol-a-low", "A-Low", 1);
    RepositoryPolicyViolation aTop = violationOn(outerPath + "!/a.jar", "uniqueA", null, "pol-a-hi", "A-High", 9);

    // group B: only one violation, hash "shared" (collides with aLow but not aTop)
    RepositoryPolicyViolation bOnly = violationOn(outerPath + "!/b.jar", "shared", null, "pol-b", "B", 5);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", outerComp, List.of(aLow, aTop, bOnly));

    JsonNode aaData = MAPPER.readTree(result).path("aaData");
    // group A emits with hash "uniqueA" (the top-threat's hash). group B emits with hash
    // "shared". Without keying dedup on the same effective hash buildAaDataEntry writes, group A
    // would have been deduped against B's "shared" and silently dropped, even though their
    // emitted hashes are different.
    assertThat(aaData.size()).as("both groups emit because their EFFECTIVE hashes differ").isEqualTo(2);
    java.util.Set<String> emittedHashes = new java.util.HashSet<>();
    aaData.forEach(n -> emittedHashes.add(n.path("hash").asText()));
    assertThat(emittedHashes).containsExactlyInAnyOrder("uniqueA", "shared");
  }

  /**
   * Tie-break by {@code policyId} when two violations share the maximum threat level. Without
   * the secondary comparator, the chosen "top" policy summary fields flip non-deterministically
   * across runs, breaking reproducible builds and snapshot-style assertions.
   */
  @Test
  public void buildPolicyThreats_sameThreatLevel_tieBreaksOnPolicyIdForDeterministicOrdering() throws Exception {
    RepositoryComponent comp = component("abc123");
    // Two violations at the same threat=8 level. Insertion order is high-then-low policyId so
    // a tie-break that defaults to "first encountered" would pick policyId="zzz". The DAO
    // contract is policyId asc → "aaa" wins.
    RepositoryPolicyViolation high1 = violation("zzz-policy", "Policy Z", 8);
    RepositoryPolicyViolation high2 = violation("aaa-policy", "Policy A", 8);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of(high1, high2));

    JsonNode group = MAPPER.readTree(result).path("aaData").get(0);
    assertThat(group.path("policyId").asText()).isEqualTo("aaa-policy");
    assertThat(group.path("policyName").asText()).isEqualTo("Policy A");
    assertThat(group.path("policyThreatLevel").asInt()).isEqualTo(8);
  }

  // ---- patchDataJsonPolicyCounts (CLM-40943 Defect 5) ----

  @Test
  public void patchDataJsonPolicyCounts_outerPlusTwoInners_setsCorrectCounts() throws Exception {
    // 3 unique pathnames: outer + 2 inners. Threats: outer=2, log4j inner=10, cli inner=1.
    // Expected: policyCounts[10]=1 (log4j), policyCounts[2]=1 (outer), policyCounts[1]=1 (cli).
    // policyComponentCount counts buckets with maxThreat>=2 = 2 (outer + log4j).
    String outer = "archive/archive/1/archive-1.zip";
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn(outer, "h0", null, "p0", "Component-Unknown", 2),
        violationOn(outer + "!/log4j-core.jar", "h1", null, "p1", "Security-Critical", 10),
        violationOn(outer + "!/commons-cli.jar", "h2", null, "p2", "Architecture-Quality", 1));
    String originalData = "{\"reportVersion\":4,\"totalArtifactCount\":3,"
        + "\"policyComponentCount\":0,\"policyCounts\":[0,0,0,0,0,0,0,0,0,0,0]}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(
        originalData.getBytes(), null, violations);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").asInt()).isEqualTo(2);
    assertThat(result.path("policyCounts").isArray()).isTrue();
    assertThat(result.path("policyCounts").size()).isEqualTo(11);
    assertThat(result.path("policyCounts").get(1).asInt()).isEqualTo(1);
    assertThat(result.path("policyCounts").get(2).asInt()).isEqualTo(1);
    assertThat(result.path("policyCounts").get(10).asInt()).isEqualTo(1);
    // Preserves pre-existing fields.
    assertThat(result.path("totalArtifactCount").asInt()).isEqualTo(3);
    assertThat(result.path("reportVersion").asInt()).isEqualTo(4);
  }

  /**
   * CLM-40943 — byte-identical inner jars (e.g. {@code spring-beans-5.3.17.jar} and
   * {@code spring-beans-5.3.17 (1).jar} from a duplicated drag-and-drop) must count as ONE
   * component, not two. The scanner emits two {@code
   *
  <dir>
   * } entries with identical sha1, the
   * evaluator persists two batches of violations on distinct synthetic pathnames, but they are
   * the same logical artifact. This is what the application-evaluation path produces — and
   * what the QA report's "Affecting N components" pill compares against.
   */
  @Test
  public void patchDataJsonPolicyCounts_byteIdenticalInnersDedupedByHash() throws Exception {
    String outer = "archive-7-7-7-7.7.7.zip";
    String springBeans = outer + "!/spring-beans-5.3.17.jar";
    String springBeansDup = outer + "!/spring-beans-5.3.17 (1).jar";
    // Outer + 5 distinct inner artifacts, but spring-beans appears twice with the SAME hash.
    // Expected: 6 unique components (1 outer + 5 unique inners), not 7.
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn(outer, "h_outer", null, "p0", "Component-Unknown", 2),
        violationOn(outer + "!/commons-text.jar", "h_commons", null, "p1", "Security-Severe", 5),
        violationOn(outer + "!/jackson.jar", "h_jackson", null, "p2", "Security-Critical", 9),
        violationOn(springBeans, "h_spring", null, "p3", "Security-Severe", 6),
        violationOn(springBeansDup, "h_spring", null, "p3", "Security-Severe", 6),
        violationOn(outer + "!/struts2.jar", "h_struts", null, "p4", "Security-Critical", 10),
        violationOn(outer + "!/tika.jar", "h_tika", null, "p5", "Security-Severe", 7));
    String originalData = "{\"policyComponentCount\":0,\"policyCounts\":[0,0,0,0,0,0,0,0,0,0,0]}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(
        originalData.getBytes(), null, violations);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").asInt())
        .as("byte-identical inner jars must count as one component (matches app-eval semantics)")
        .isEqualTo(6);
    // Sanity: 6 buckets total across all threat levels.
    int totalBuckets = 0;
    for (int i = 0; i < 11; i++) {
      totalBuckets += result.path("policyCounts").get(i).asInt();
    }
    assertThat(totalBuckets).isEqualTo(6);
  }

  @Test
  public void patchDataJsonPolicyCounts_distinctHashesNotDedup() throws Exception {
    // Two inners with different hashes — must count as two components.
    String outer = "archive.zip";
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn(outer + "!/a.jar", "h_a", null, "p1", "Severe", 5),
        violationOn(outer + "!/b.jar", "h_b", null, "p2", "Severe", 5));
    String originalData = "{\"policyComponentCount\":0,\"policyCounts\":[0,0,0,0,0,0,0,0,0,0,0]}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(
        originalData.getBytes(), null, violations);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").asInt()).isEqualTo(2);
    assertThat(result.path("policyCounts").get(5).asInt()).isEqualTo(2);
  }

  @Test
  public void patchDataJsonPolicyCounts_nullHashesFallBackToPathnameDedup() throws Exception {
    // Two distinct pathnames, both with null hash and no outer-fallback. They must still count
    // as two separate components (no false collapse to one).
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn("a.jar", null, null, "p1", "Severe", 5),
        violationOn("b.jar", null, null, "p2", "Severe", 5));
    String originalData = "{\"policyComponentCount\":0,\"policyCounts\":[0,0,0,0,0,0,0,0,0,0,0]}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(
        originalData.getBytes(), null, violations);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").asInt()).isEqualTo(2);
    assertThat(result.path("policyCounts").get(5).asInt()).isEqualTo(2);
  }

  @Test
  public void patchDataJsonPolicyCounts_multipleViolationsSamePathname_takesMaxThreat() throws Exception {
    // Same outer pathname has both moderate and critical violations — max wins.
    String outer = "g/a-1.jar";
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn(outer, "h", null, "p1", "Moderate", 3),
        violationOn(outer, "h", null, "p2", "Critical", 9));
    String originalData = "{\"policyComponentCount\":0,\"policyCounts\":[0,0,0,0,0,0,0,0,0,0,0]}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(
        originalData.getBytes(), null, violations);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").asInt()).isEqualTo(1);
    assertThat(result.path("policyCounts").get(9).asInt()).isEqualTo(1);
    assertThat(result.path("policyCounts").get(3).asInt()).isEqualTo(0);
  }

  @Test
  public void patchDataJsonPolicyCounts_waivedViolationsIgnored() throws Exception {
    String outer = "g/a-1.jar";
    RepositoryPolicyViolation waived = violationOn(outer, "h", null, "p1", "Critical", 9);
    waived.setWaived(true);
    RepositoryPolicyViolation active = violationOn(outer, "h", null, "p2", "Moderate", 3);
    String originalData = "{\"policyComponentCount\":0,\"policyCounts\":[0,0,0,0,0,0,0,0,0,0,0]}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(
        originalData.getBytes(), null, List.of(waived, active));

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").asInt()).isEqualTo(1);
    assertThat(result.path("policyCounts").get(9).asInt())
        .as("waived critical violation must not contribute to critical count")
        .isEqualTo(0);
    assertThat(result.path("policyCounts").get(3).asInt()).isEqualTo(1);
  }

  @Test
  public void patchDataJsonPolicyCounts_emptyViolations_returnsOriginalUnchanged() {
    byte[] original = "{\"policyComponentCount\":99,\"policyCounts\":[0,0,5,0,0,0,0,0,0,0,0]}".getBytes();

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(original, null, List.of());

    assertThat(patched).isSameAs(original);
  }

  @Test
  public void patchDataJsonPolicyCounts_nullViolations_returnsOriginalUnchanged() {
    byte[] original = "{\"policyComponentCount\":1}".getBytes();

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(original, null, null);

    assertThat(patched).isSameAs(original);
  }

  @Test
  public void patchDataJsonPolicyCounts_malformedJson_returnsOriginalUnchanged() {
    byte[] original = "not-json-at-all".getBytes();
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn("a.jar", "h", null, "p", "Critical", 9));

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(original, null, violations);

    assertThat(patched).isSameAs(original);
  }

  @Test
  public void patchDataJsonPolicyCounts_nullDataJson_returnsNullUnchanged() {
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn("a.jar", "h", null, "p", "Critical", 9));

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(null, null, violations);

    assertThat(patched).isNull();
  }

  // ---- patchDataJsonPolicyComponentCountIfAbsent (CLM-41737 follow-up) ----

  @Test
  public void patchDataJsonPolicyComponentCountIfAbsent_fieldAbsent_stampsCount() throws Exception {
    // Non-nested single artifact (Maven/PyPI/RubyGems single/R): HDS omits policyComponentCount.
    // Frontend defaults absent field to 0 -> "Affecting 0 components" even when violations exist.
    // The stamp must write the correct count so the header pill renders "Affecting 1".
    String pathname = "org/apache/logging/log4j/log4j-core/2.14.1/log4j-core-2.14.1.jar";
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn(pathname, "h_log4j", null, "p1", "Security-Critical", 10),
        violationOn(pathname, "h_log4j", null, "p2", "Security-Severe", 6));
    // HDS-style data.json: no policyComponentCount key present.
    String originalData = "{\"reportVersion\":4,\"totalArtifactCount\":1,\"knownArtifactCount\":1,"
        + "\"policyCounts\":[0,0,0,0,0,0,1,0,0,0,1]}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyComponentCountIfAbsent(
        originalData.getBytes(), null, violations, null);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").isMissingNode()).isFalse();
    assertThat(result.path("policyComponentCount").asInt()).isEqualTo(1);
    // Existing fields (including HDS's policyCounts[]) untouched.
    assertThat(result.path("totalArtifactCount").asInt()).isEqualTo(1);
    assertThat(result.path("knownArtifactCount").asInt()).isEqualTo(1);
    assertThat(result.path("policyCounts").get(6).asInt()).isEqualTo(1);
    assertThat(result.path("policyCounts").get(10).asInt()).isEqualTo(1);
  }

  @Test
  public void patchDataJsonPolicyComponentCountIfAbsent_fieldPresent_leftUntouched() {
    // Nested/bundled case: HDS already supplied policyComponentCount. We must NOT overwrite,
    // otherwise the intentional CLM-42119 removal (which preserved HDS's view for bundled
    // archives) regresses.
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn("a.jar", "h", null, "p", "Critical", 9));
    byte[] original = "{\"policyComponentCount\":4,\"policyCounts\":[0,0,0,0,0,0,0,0,0,1,0]}".getBytes();

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyComponentCountIfAbsent(
        original, null, violations, null);

    assertThat(patched).isSameAs(original);
  }

  @Test
  public void patchDataJsonPolicyComponentCountIfAbsent_absentFieldNoViolations_stampsZero() throws Exception {
    // Absent field + no violations: stamp 0 so the field is present and self-consistent
    // (frontend already renders 0, but writing the key makes data.json Lifecycle-parallel).
    String originalData = "{\"totalArtifactCount\":1}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyComponentCountIfAbsent(
        originalData.getBytes(), null, List.of(), null);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").asInt()).isEqualTo(0);
  }

  @Test
  public void patchDataJsonPolicyComponentCountIfAbsent_waivedViolationsExcluded() throws Exception {
    // Waived violations do not contribute — mirrors patchDataJsonPolicyCounts semantics and
    // the aaData "activeViolations" that policythreats.json emits.
    RepositoryPolicyViolation waived = violationOn("a.jar", "h", null, "p1", "Critical", 9);
    waived.setWaived(true);
    String originalData = "{\"totalArtifactCount\":1}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyComponentCountIfAbsent(
        originalData.getBytes(), null, List.of(waived), null);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").asInt()).isEqualTo(0);
  }

  @Test
  public void patchDataJsonPolicyComponentCountIfAbsent_maxThreatBelowTwoNotCounted() throws Exception {
    // maxThreat >= 2 threshold matches ScanPolicyEvaluator.updateDataJson so hosted-repo and
    // application-evaluation paths compute the same value for the same violation set.
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn("a.jar", "h", null, "p", "Low", 1));
    String originalData = "{\"totalArtifactCount\":1}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyComponentCountIfAbsent(
        originalData.getBytes(), null, violations, null);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").asInt()).isEqualTo(0);
  }

  @Test
  public void patchDataJsonPolicyComponentCountIfAbsent_malformedJson_returnsOriginalUnchanged() {
    byte[] original = "not-json".getBytes();
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn("a.jar", "h", null, "p", "Critical", 9));

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyComponentCountIfAbsent(
        original, null, violations, null);

    assertThat(patched).isSameAs(original);
  }

  @Test
  public void patchDataJsonPolicyComponentCountIfAbsent_nullDataJson_returnsNullUnchanged() {
    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyComponentCountIfAbsent(
        null, null, List.of(), null);

    assertThat(patched).isNull();
  }

  @Test
  public void patchDataJsonPolicyComponentCountIfAbsent_dedupMatchesPolicyThreats() throws Exception {
    // Two byte-identical inner jars — same dedup as policythreats.json → one component, not two.
    // Keeps the "Affecting N" pill in agreement with the aaData row count downstream.
    String outer = "archive.zip";
    List<RepositoryPolicyViolation> violations = List.of(
        violationOn(outer + "!/spring.jar", "h_spring", null, "p1", "Critical", 9),
        violationOn(outer + "!/spring (1).jar", "h_spring", null, "p1", "Critical", 9));
    String originalData = "{\"totalArtifactCount\":2}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyComponentCountIfAbsent(
        originalData.getBytes(), null, violations, null);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("policyComponentCount").asInt()).isEqualTo(1);
  }

  // ---- totalRisk: dedup by (hash, constraintFactsId) matches a same-file LC scan ----

  @Test
  public void totalRisk_singleArtifact_multipleDistinctCvesSamePolicy_allCount() {
    // log4j-core reproducer: one component, one policy (Security-Critical), THREE distinct CVEs
    // (distinct constraintFactsId), each threat 10. The old (component, policy)-max key collapsed
    // these to a single 10; LC counts all three -> 30. Distinct constraintFactsId must each count.
    ComponentIdentifier log4j = ci("org.apache.logging.log4j", "log4j-core", "2.14.1", "jar");
    String p = "org/apache/logging/log4j/log4j-core-2.14.1.jar";
    List<RepositoryPolicyViolation> violations = List.of(
        withCfid(violationOn(p, "h_log4j", log4j, "pol-crit", "Security-Critical", 10), "cve-A"),
        withCfid(violationOn(p, "h_log4j", log4j, "pol-crit", "Security-Critical", 10), "cve-B"),
        withCfid(violationOn(p, "h_log4j", log4j, "pol-crit", "Security-Critical", 10), "cve-C"));

    assertThat(HostedReportFileBuilder.totalRisk(component("h_log4j"), violations))
        .as("distinct constraints on one component each contribute")
        .isEqualTo(30);
  }

  @Test
  public void totalRisk_frameworkFanout_distinctInnerHashes_allCount() {
    ComponentIdentifier dll = ci("nuget", "Newtonsoft.Json.dll", "12.0.1", "dll");
    String outer = "newtonsoft.json.12.0.1.nupkg";
    List<RepositoryPolicyViolation> violations = List.of(
        withCfid(violationOn(outer + "!/lib/net20/Newtonsoft.Json.dll", "h_net20", dll, "pol-sec", "Security-High", 9),
            "cve-1"),
        withCfid(violationOn(outer + "!/lib/net40/Newtonsoft.Json.dll", "h_net40", dll, "pol-sec", "Security-High", 9),
            "cve-1"),
        withCfid(violationOn(outer + "!/lib/net45/Newtonsoft.Json.dll", "h_net45", dll, "pol-sec", "Security-High", 9),
            "cve-1"));

    assertThat(HostedReportFileBuilder.totalRisk(component("h_outer"), violations))
        .as("same constraint across distinct binary hashes each contributes")
        .isEqualTo(27);
  }

  @Test
  public void totalRisk_distinctInners_allCount() {
    List<RepositoryPolicyViolation> violations = List.of(
        withCfid(violationOn("outer.gem!/rack", "rack_h", ci("rubygems", "rack", "2.0.6", "gem"), "p1", "P1", 6), "c1"),
        withCfid(violationOn("outer.gem!/nokogiri", "nok_h", ci("rubygems", "nokogiri", "1.8.2", "gem"), "p1", "P1", 8),
            "c2"),
        withCfid(
            violationOn("outer.gem!/actionpack", "act_h", ci("rubygems", "actionpack", "5.2.0", "gem"), "p1", "P1", 10),
            "c3"));

    assertThat(HostedReportFileBuilder.totalRisk(component("outer_h"), violations)).isEqualTo(6 + 8 + 10);
  }

  @Test
  public void totalRisk_waivedViolations_excluded() {
    ComponentIdentifier axios = ci("npm", "axios", "0.18.0", "tgz");
    RepositoryPolicyViolation active = withCfid(violationOn("axios.tgz", "ha", axios, "p1", "P1", 7), "c1");
    RepositoryPolicyViolation waived = withCfid(violationOn("axios.tgz", "hb", axios, "p2", "P2", 9), "c2");
    waived.setWaived(true);

    assertThat(HostedReportFileBuilder.totalRisk(component("h"), List.of(active, waived))).isEqualTo(7);
  }

  @Test
  public void totalRisk_nullHashAndConstraint_fallsBackToId_bothCount() {
    ComponentIdentifier axios = ci("npm", "axios", "0.18.0", "tgz");
    RepositoryPolicyViolation v1 = violationOn("axios.tgz", null, axios, "p1", "P1", 5);
    v1.setId("id1");
    RepositoryPolicyViolation v2 = violationOn("axios.tgz", null, axios, "p1", "P1", 3);
    v2.setId("id2");

    assertThat(HostedReportFileBuilder.totalRisk(component("h"), List.of(v1, v2))).isEqualTo(5 + 3);
  }

  @Test
  public void totalRisk_emptyList_zero() {
    assertThat(HostedReportFileBuilder.totalRisk(component("h"), List.of())).isZero();
  }

  @Test
  public void excludeOuter_npmSelfMirror_dropsRedundantOuter() {
    ComponentIdentifier axios = ci("npm", "axios", "0.18.0", "tgz");
    String outerPath = "axios-0.18.0.tgz";
    RepositoryComponent outerComp = new RepositoryComponent();
    outerComp.setPathname(outerPath);
    outerComp.setHash("file_sha1");
    RepositoryPolicyViolation outer =
        withCfid(violationOn(outerPath, "file_sha1", axios, "pol-sec", "Security-High", 14), "cve-1");
    RepositoryPolicyViolation innerMirror =
        withCfid(violationOn(outerPath + "!/axios@0.18.0", "hds_hash", axios, "pol-sec", "Security-High", 14), "cve-1");

    List<RepositoryPolicyViolation> result =
        HostedReportFileBuilder.excludeOuterViolationsForFormat(outerComp, List.of(outer, innerMirror), "npm");

    assertThat(result).as("outer whose identity matches an inner is dropped").containsExactly(innerMirror);
    assertThat(HostedReportFileBuilder.totalRisk(outerComp, result)).isEqualTo(14);
  }

  @Test
  public void excludeOuter_nugetFanout_keepsOuterAndInners() {
    ComponentIdentifier pkg = ci("nuget", "System.Text.Encodings.Web", "4.5.0", "nupkg");
    ComponentIdentifier dll = ci("nuget", "System.Text.Encodings.Web.dll", "4.5.0", "dll");
    String outerPath = "system.text.encodings.web.4.5.0.nupkg";
    RepositoryComponent outerComp = new RepositoryComponent();
    outerComp.setPathname(outerPath);
    RepositoryPolicyViolation outer =
        withCfid(violationOn(outerPath, "h_outer", pkg, "pol-sec", "Security-High", 9), "cve-1");
    RepositoryPolicyViolation dllA =
        withCfid(violationOn(outerPath + "!/lib/net20/x.dll", "h_dll_a", dll, "pol-sec", "Security-High", 9), "cve-1");
    RepositoryPolicyViolation dllB =
        withCfid(violationOn(outerPath + "!/lib/net40/x.dll", "h_dll_b", dll, "pol-sec", "Security-High", 9), "cve-1");

    List<RepositoryPolicyViolation> result =
        HostedReportFileBuilder.excludeOuterViolationsForFormat(outerComp, List.of(outer, dllA, dllB), "nuget");

    assertThat(result).as("nuget outer identity differs from inner DLLs, nothing dropped").hasSize(3);
    assertThat(HostedReportFileBuilder.totalRisk(outerComp, result)).isEqualTo(27);
  }

  @Test
  public void excludeOuter_go_containerWithTransitives_outerBecomesComponentUnknown2() {
    RepositoryComponent goOuter = new RepositoryComponent();
    goOuter.setPathname("github.com/gin-gonic/gin/@v/v1.6.0.zip");
    goOuter.setHash("h_outer");
    RepositoryPolicyViolation outer = withCfid(violationOn(
        "github.com/gin-gonic/gin/@v/v1.6.0.zip", "h_outer",
        ci("go", "github.com/gin-gonic/gin", "v1.6.0", "zip"), "pol-sec", "Security-High", 9), "cve-outer");
    RepositoryPolicyViolation inner = withCfid(violationOn(
        "github.com/gin-gonic/gin/@v/v1.6.0.zip!/dependency:/gopkg.in/yaml.v2", "h_yaml",
        ci("go", "gopkg.in/yaml.v2", "v2.2.2", "zip"), "pol-sec", "Security-High", 8), "cve-yaml");

    List<RepositoryPolicyViolation> result =
        HostedReportFileBuilder.excludeOuterViolationsForFormat(goOuter, List.of(outer, inner), "go");

    assertThat(result).hasSize(2);
    assertThat(result).contains(inner);
    RepositoryPolicyViolation unknown = result.stream()
        .filter(v -> "Component-Unknown".equals(v.getPolicyName()))
        .findFirst()
        .orElseThrow();
    assertThat(unknown.getThreatLevel()).isEqualTo(2);
    assertThat(unknown.getPathname()).isEqualTo("github.com/gin-gonic/gin/@v/v1.6.0.zip");
    assertThat(HostedReportFileBuilder.totalRisk(goOuter, result)).isEqualTo(10);
  }

  @Test
  public void excludeOuter_go_identifiedBareModule_collapsesToUnknown2() {
    RepositoryComponent goOuter = new RepositoryComponent();
    goOuter.setPathname("github.com/valyala/fasthttp/@v/v1.2.0.zip");
    RepositoryPolicyViolation sec = withCfid(violationOn(
        "github.com/valyala/fasthttp/@v/v1.2.0.zip", "h",
        ci("go", "github.com/valyala/fasthttp", "v1.2.0", "zip"), "pol-sec", "Security-High", 9), "c1");
    RepositoryPolicyViolation arch = withCfid(violationOn(
        "github.com/valyala/fasthttp/@v/v1.2.0.zip", "h",
        ci("go", "github.com/valyala/fasthttp", "v1.2.0", "zip"), "pol-arch", "Architecture-Quality", 1), "c2");

    List<RepositoryPolicyViolation> result =
        HostedReportFileBuilder.excludeOuterViolationsForFormat(goOuter, List.of(sec, arch), "go");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPolicyName()).isEqualTo("Component-Unknown");
    assertThat(HostedReportFileBuilder.totalRisk(goOuter, result)).isEqualTo(2);
  }

  @Test
  public void excludeOuter_go_stampsResolvedPolicyFields_evenWhenRenamedAndRethreated() {
    RepositoryComponent goOuter = new RepositoryComponent();
    goOuter.setPathname("m/@v/v1.zip");
    RepositoryPolicyViolation outer = withCfid(violationOn(
        "m/@v/v1.zip", "h", ci("go", "m", "v1", "zip"), "pol-sec", "Security-High", 9), "c1");

    // Resolved policy: tenant renamed Component-Unknown and set threat 8. The synthetic outer row
    // must carry the resolved id/name/threat (not the hardcoded default) so it matches LC.
    Policy resolved = new Policy("resolved-cu-id", "Renamed-Unknown");
    resolved.setThreatLevel(8);

    List<RepositoryPolicyViolation> result = HostedReportFileBuilder
        .excludeOuterViolationsForFormat(goOuter, List.of(outer), "go", resolved);

    RepositoryPolicyViolation outerRow = result.stream()
        .filter(v -> "m/@v/v1.zip".equals(v.getPathname()))
        .findFirst()
        .orElseThrow();
    assertThat(outerRow.getPolicyId()).isEqualTo("resolved-cu-id");
    assertThat(outerRow.getPolicyName()).isEqualTo("Renamed-Unknown");
    assertThat(outerRow.getThreatLevel()).isEqualTo(8);
    assertThat(HostedReportFileBuilder.totalRisk(goOuter, result)).isEqualTo(8);
  }

  @Test
  public void excludeOuter_go_nullResolvedPolicy_fallsBackToDefault() {
    RepositoryComponent goOuter = new RepositoryComponent();
    goOuter.setPathname("m/@v/v1.zip");
    RepositoryPolicyViolation outer = withCfid(violationOn(
        "m/@v/v1.zip", "h", ci("go", "m", "v1", "zip"), "pol-sec", "Security-High", 9), "c1");

    List<RepositoryPolicyViolation> result = HostedReportFileBuilder
        .excludeOuterViolationsForFormat(goOuter, List.of(outer), "go", null);

    RepositoryPolicyViolation outerRow = result.get(0);
    assertThat(outerRow.getPolicyName()).isEqualTo("Component-Unknown");
    assertThat(outerRow.getThreatLevel()).isEqualTo(2);
  }

  @Test
  public void excludeOuter_maven_retainsOuter() {
    RepositoryComponent mavenOuter = new RepositoryComponent();
    mavenOuter.setPathname("com/example/lib-1.0.jar");
    mavenOuter.setHash("h");
    RepositoryPolicyViolation outer = withCfid(violationOn(
        "com/example/lib-1.0.jar", "h", ci("maven", "example", "lib", "jar"),
        "pol-sec", "Security-High", 9), "cve-1");

    List<RepositoryPolicyViolation> result =
        HostedReportFileBuilder.excludeOuterViolationsForFormat(mavenOuter, List.of(outer), "maven");

    assertThat(result).containsExactly(outer);
  }

  @Test
  public void excludeOuter_go_standalone_collapsesToUnknown2() {
    RepositoryComponent goStandalone = new RepositoryComponent();
    goStandalone.setPathname("gopkg.in/yaml.v2/@v/v2.2.2.zip");
    RepositoryPolicyViolation outerOnly = withCfid(violationOn(
        "gopkg.in/yaml.v2/@v/v2.2.2.zip", "h",
        ci("go", "gopkg.in/yaml.v2", "v2.2.2", "zip"), "p", "Security-Low", 3), "c1");

    List<RepositoryPolicyViolation> result =
        HostedReportFileBuilder.excludeOuterViolationsForFormat(goStandalone, List.of(outerOnly), "go");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPolicyName()).isEqualTo("Component-Unknown");
    assertThat(HostedReportFileBuilder.totalRisk(goStandalone, result)).isEqualTo(2);
  }

  @Test
  public void excludeOuter_nullFormat_noMirror_noop() {
    RepositoryComponent outerComp = new RepositoryComponent();
    outerComp.setPathname("x.zip");
    RepositoryPolicyViolation v = withCfid(violationOn("x.zip", "h", ci("go", "x", "1", "zip"), "p", "P", 5), "c");
    assertThat(HostedReportFileBuilder.excludeOuterViolationsForFormat(outerComp, List.of(v), null))
        .containsExactly(v);
  }

  @Test
  public void excludeOuter_emptyViolations_returnsUnchanged() {
    RepositoryComponent outerComp = new RepositoryComponent();
    outerComp.setPathname("x.tgz");
    List<RepositoryPolicyViolation> empty = List.of();
    assertThat(HostedReportFileBuilder.excludeOuterViolationsForFormat(outerComp, empty, "npm"))
        .isSameAs(empty);
  }

  @Test
  public void excludeOuter_nullOuterPathname_returnsUnchanged() {
    RepositoryComponent outerComp = new RepositoryComponent();
    RepositoryPolicyViolation v = withCfid(violationOn("x.tgz", "h", ci("npm", "x", "1", "tgz"), "p", "P", 5), "c");
    List<RepositoryPolicyViolation> in = List.of(v);
    assertThat(HostedReportFileBuilder.excludeOuterViolationsForFormat(outerComp, in, "npm"))
        .isSameAs(in);
  }

  /**
   * npm-style self-mirror drop keys on component identity: an outer row with no componentIdentifier
   * (unidentified outer) has a null identity, so it can never match an inner and must be retained.
   */
  @Test
  public void excludeOuter_npm_outerWithNoIdentity_retained() {
    String outerPath = "pkg-1.0.0.tgz";
    RepositoryComponent outerComp = new RepositoryComponent();
    outerComp.setPathname(outerPath);
    RepositoryPolicyViolation outer =
        withCfid(violationOn(outerPath, "file_sha1", null, "pol-sec", "Security-High", 7), "cve-1");
    RepositoryPolicyViolation inner =
        withCfid(violationOn(outerPath + "!/dep@1.0.0", "hds_hash", ci("npm", "dep", "1.0.0", "tgz"),
            "pol-sec", "Security-High", 9), "cve-2");

    List<RepositoryPolicyViolation> result =
        HostedReportFileBuilder.excludeOuterViolationsForFormat(outerComp, List.of(outer, inner), "npm");

    assertThat(result).as("outer with null identity is not dropped").containsExactly(outer, inner);
  }

  @Test
  public void totalRisk_sameHashAndConstraint_collapsesToMaxThreat() {
    ComponentIdentifier axios = ci("npm", "axios", "0.18.0", "tgz");
    RepositoryPolicyViolation outer =
        withCfid(violationOn("axios.tgz", "shared_hash", axios, "pol-sec", "Security-High", 14), "cve-1");
    RepositoryPolicyViolation innerMirror =
        withCfid(violationOn("axios.tgz!/axios@0.18.0", "shared_hash", axios, "pol-sec", "Security-High", 14),
            "cve-1");

    assertThat(HostedReportFileBuilder.totalRisk(component("shared_hash"), List.of(outer, innerMirror)))
        .as("identical (hash, constraint) contributes once at the max threat")
        .isEqualTo(14);
  }

  @Test
  public void totalRisk_nullViolations_zero() {
    assertThat(HostedReportFileBuilder.totalRisk(component("h"), null)).isZero();
  }

  private static RepositoryPolicyViolation withCfid(RepositoryPolicyViolation v, String constraintFactsId) {
    v.setConstraintFactsId(constraintFactsId);
    return v;
  }

  // --- patchDataJsonTotalArtifactCount (2-arg + 3-arg overloads) ------------

  @Test
  public void patchDataJsonTotalArtifactCount_oneArg_writesBothFieldsFromDirectCount() throws Exception {
    // Back-compat: single-arg callers (identified-outer collapse gate) treat every counted
    // component as identified. total=known=directCount. Preserves CLM-42117 behavior.
    String original = "{\"totalArtifactCount\":42,\"knownArtifactCount\":42}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonTotalArtifactCount(original.getBytes(), 1);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("totalArtifactCount").asInt()).isEqualTo(1);
    assertThat(result.path("knownArtifactCount").asInt()).isEqualTo(1);
  }

  @Test
  public void patchDataJsonTotalArtifactCount_threeArg_unknownOuter_writesZeroKnown() throws Exception {
    // The bug we're fixing: for a Component-Unknown outer (helm chart of a custom operator,
    // proprietary archive), the sole physical component has matchState=unknown, so
    // knownArtifactCount MUST stay 0 — the header should read "0% identified" like iq-cli.
    String original = "{\"totalArtifactCount\":1,\"knownArtifactCount\":0}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonTotalArtifactCount(original.getBytes(), 1, 0);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("totalArtifactCount").asInt()).isEqualTo(1);
    assertThat(result.path("knownArtifactCount").asInt()).isZero();
  }

  @Test
  public void patchDataJsonTotalArtifactCount_threeArg_knownCountClampedToDirect() throws Exception {
    // Defensive clamp: a caller that over-counts (knownCount > directCount) still produces a
    // well-formed ≤100% percentage. Prevents any UI reading >100% identified.
    String original = "{\"totalArtifactCount\":10}";

    byte[] patched = HostedReportFileBuilder.patchDataJsonTotalArtifactCount(original.getBytes(), 3, 99);

    JsonNode result = MAPPER.readTree(patched);
    assertThat(result.path("totalArtifactCount").asInt()).isEqualTo(3);
    assertThat(result.path("knownArtifactCount").asInt()).isEqualTo(3);
  }

  @Test
  public void patchDataJsonTotalArtifactCount_negativeCounts_returnsOriginalUnchanged() {
    String original = "{\"totalArtifactCount\":5}";

    byte[] withNegativeDirect = HostedReportFileBuilder.patchDataJsonTotalArtifactCount(original.getBytes(), -1, 0);
    byte[] withNegativeKnown = HostedReportFileBuilder.patchDataJsonTotalArtifactCount(original.getBytes(), 5, -1);

    assertThat(withNegativeDirect).isEqualTo(original.getBytes());
    assertThat(withNegativeKnown).isEqualTo(original.getBytes());
  }

  // --- countKnownMatchesInBom -----------------------------------------------

  @Test
  public void countKnownMatchesInBom_countsExactSimilarEmbedded_ignoresUnknownAndPartial() {
    // Mixed bom: 2 known (exact, similar), 1 embedded (also known), 1 unknown, 1 partial → 3.
    String bom = "{\"aaData\":["
        + "{\"matchState\":\"exact\"},"
        + "{\"matchState\":\"similar\"},"
        + "{\"matchState\":\"embedded\"},"
        + "{\"matchState\":\"unknown\"},"
        + "{\"matchState\":\"partial\"}"
        + "]}";

    int known = HostedReportFileBuilder.countKnownMatchesInBom(bom.getBytes());

    assertThat(known).isEqualTo(3);
  }

  @Test
  public void countKnownMatchesInBom_allUnknown_returnsZero() {
    // The contour-operator-4.2.1.tgz case: 1 aaData entry, matchState=unknown → 0 known.
    String bom = "{\"aaData\":[{\"matchState\":\"unknown\",\"pathnames\":[\"contour-operator-4.2.1.tgz\"]}]}";

    int known = HostedReportFileBuilder.countKnownMatchesInBom(bom.getBytes());

    assertThat(known).isZero();
  }

  @Test
  public void countKnownMatchesInBom_matchStateCaseInsensitive() {
    // HDS occasionally emits capitalized matchState values; treat case-insensitively.
    String bom = "{\"aaData\":[{\"matchState\":\"EXACT\"},{\"matchState\":\"Similar\"}]}";

    int known = HostedReportFileBuilder.countKnownMatchesInBom(bom.getBytes());

    assertThat(known).isEqualTo(2);
  }

  @Test
  public void countKnownMatchesInBom_missingAaData_returnsZero() {
    String bom = "{\"foo\":\"bar\"}";

    int known = HostedReportFileBuilder.countKnownMatchesInBom(bom.getBytes());

    assertThat(known).isZero();
  }

  @Test
  public void countKnownMatchesInBom_nullOrEmpty_returnsZero() {
    assertThat(HostedReportFileBuilder.countKnownMatchesInBom(null)).isZero();
    assertThat(HostedReportFileBuilder.countKnownMatchesInBom(new byte[0])).isZero();
  }

  @Test
  public void countKnownMatchesInBom_unparseableJson_failsSoftToZero() {
    // Fail-soft path: garbage bytes → 0, not a thrown exception. Under-reporting 0% identified
    // is safer than a thrown exception aborting the whole data.json patch.
    byte[] garbage = "not-json-at-all".getBytes();

    assertThat(HostedReportFileBuilder.countKnownMatchesInBom(garbage)).isZero();
  }

  // --- dedupeBomIdentifiedRows -----------------------------------------------

  /**
   * Nuget framework fanout: {@code /lib/net40/CefSharp.dll} and {@code /lib/net45/CefSharp.dll}
   * share the same {@code (format, coordinates)} because HDS does not put the target framework
   * in the coord map, but both are rich HDS-identified entries (each carries {@code packageUrl})
   * and represent distinct physical DLLs with distinct hashes. Neither is a sparse shadow of
   * the other, so both survive.
   */
  @Test
  public void dedupeBomIdentifiedRows_nugetFrameworkFanout_bothRichVariantsKept() throws Exception {
    String bom = "{\"aaData\":["
        + "{\"hash\":\"h_net40\",\"matchState\":\"exact\","
        + "\"pathnames\":[\"lib/net40/CefSharp.dll\"],"
        + "\"packageUrl\":\"pkg:nuget/CefSharp.dll@79.1.310.0\","
        + "\"identificationSource\":\"Sonatype\","
        + "\"componentIdentifier\":{\"format\":\"nuget\","
        + "\"coordinates\":{\"packageId\":\"CefSharp.dll\",\"version\":\"79.1.310.0\"}}},"
        + "{\"hash\":\"h_net45\",\"matchState\":\"exact\","
        + "\"pathnames\":[\"lib/net45/CefSharp.dll\"],"
        + "\"packageUrl\":\"pkg:nuget/CefSharp.dll@79.1.310.0\","
        + "\"identificationSource\":\"Sonatype\","
        + "\"componentIdentifier\":{\"format\":\"nuget\","
        + "\"coordinates\":{\"packageId\":\"CefSharp.dll\",\"version\":\"79.1.310.0\"}}}"
        + "]}";

    byte[] deduped = HostedReportFileBuilder.dedupeBomIdentifiedRows(bom.getBytes());

    JsonNode result = MAPPER.readTree(deduped);
    assertThat(result.path("aaData").size())
        .as("both rich framework-fanout DLLs must survive — neither is a sparse shadow")
        .isEqualTo(2);
  }

  /**
   * NPM dot-prop 4.2.0 shape (verified against real HDS output): a rich entry with
   * {@code packageUrl} + {@code identificationSource} + full inner pathname list, plus a sparse
   * file-SHA1 self-mirror shadow with just the outer pathname and none of the identification
   * metadata ({@code createTime=0}, empty inner {@code aaData}). Without dedupe the sparse
   * shadow inflates {@code knownArtifactCount} beyond {@code totalArtifactCount} → 200%
   * identified. Sparse-shadow drop reduces the group to the rich entry only.
   */
  @Test
  public void dedupeBomIdentifiedRows_npmSelfMirror_sparseShadowDropped() throws Exception {
    String bom = "{\"aaData\":["
        + "{\"hash\":\"8dacfb545e80\",\"matchState\":\"exact\","
        + "\"pathnames\":["
        + "\"dot-prop/-/dot-prop-4.2.0.tgz\","
        + "\"dot-prop/-/dot-prop-4.2.0.tgz/package/index.js\","
        + "\"dot-prop/-/dot-prop-4.2.0.tgz/package/license\","
        + "\"dot-prop/-/dot-prop-4.2.0.tgz/package/package.json\","
        + "\"dot-prop/-/dot-prop-4.2.0.tgz/package/readme.md\"],"
        + "\"packageUrl\":\"pkg:npm/dot-prop@4.2.0\","
        + "\"identificationSource\":\"Sonatype\","
        + "\"aggregateFiles\":[],"
        + "\"componentIdentifier\":{\"format\":\"npm\","
        + "\"coordinates\":{\"packageId\":\"dot-prop\",\"version\":\"4.2.0\"}}},"
        + "{\"hash\":\"1f19e0c2e1aa\",\"matchState\":\"exact\","
        + "\"pathnames\":[\"dot-prop/-/dot-prop-4.2.0.tgz\"],"
        + "\"createTime\":0,\"aaData\":[],"
        + "\"componentIdentifier\":{\"format\":\"npm\","
        + "\"coordinates\":{\"packageId\":\"dot-prop\",\"version\":\"4.2.0\"}}}"
        + "]}";

    byte[] deduped = HostedReportFileBuilder.dedupeBomIdentifiedRows(bom.getBytes());

    JsonNode result = MAPPER.readTree(deduped);
    assertThat(result.path("aaData").size())
        .as("sparse file-SHA1 shadow of dot-prop must be dropped when the rich entry exists")
        .isEqualTo(1);
    assertThat(result.path("aaData").get(0).path("hash").asText())
        .as("rich entry survives — its hash is the join key downstream callers read")
        .isEqualTo("8dacfb545e80");
  }

  /**
   * When a sparse entry has no rich counterpart at the same {@code (format, coords)}, it is
   * kept — it is the only representation of that identity and dropping it would silently lose
   * a component from the report.
   */
  @Test
  public void dedupeBomIdentifiedRows_sparseAlone_kept() throws Exception {
    String bom = "{\"aaData\":["
        + "{\"hash\":\"h1\",\"matchState\":\"exact\",\"pathnames\":[\"a.tgz\"],"
        + "\"componentIdentifier\":{\"format\":\"npm\","
        + "\"coordinates\":{\"packageId\":\"a\",\"version\":\"1.0\"}}},"
        + "{\"hash\":\"h2\",\"matchState\":\"exact\",\"pathnames\":[\"b.tgz\"],"
        + "\"componentIdentifier\":{\"format\":\"npm\","
        + "\"coordinates\":{\"packageId\":\"b\",\"version\":\"1.0\"}}}"
        + "]}";

    byte[] deduped = HostedReportFileBuilder.dedupeBomIdentifiedRows(bom.getBytes());

    JsonNode result = MAPPER.readTree(deduped);
    assertThat(result.path("aaData").size())
        .as("sparse entries with no rich counterpart at the same coord are preserved")
        .isEqualTo(2);
  }

  /**
   * Unknown-matchState rows are never candidates for dedupe: HDS uses them as placeholders and
   * their meaning may differ even when coords collide. Preserve them as-is.
   */
  @Test
  public void dedupeBomIdentifiedRows_unknownMatchState_alwaysPreserved() throws Exception {
    String bom = "{\"aaData\":["
        + "{\"hash\":\"h1\",\"matchState\":\"unknown\","
        + "\"pathnames\":[\"a.dll\"],"
        + "\"componentIdentifier\":{\"format\":\"nuget\","
        + "\"coordinates\":{\"packageId\":\"x\",\"version\":\"1.0\"}}},"
        + "{\"hash\":\"h2\",\"matchState\":\"unknown\","
        + "\"pathnames\":[\"a.dll\"],"
        + "\"componentIdentifier\":{\"format\":\"nuget\","
        + "\"coordinates\":{\"packageId\":\"x\",\"version\":\"1.0\"}}}"
        + "]}";

    byte[] deduped = HostedReportFileBuilder.dedupeBomIdentifiedRows(bom.getBytes());

    JsonNode result = MAPPER.readTree(deduped);
    assertThat(result.path("aaData").size())
        .as("unknown-matchState rows are preserved regardless of coord collision")
        .isEqualTo(2);
  }

  @Test
  public void dedupeBomIdentifiedRows_lessThanTwoEntries_returnedUnchanged() {
    byte[] empty = "{\"aaData\":[]}".getBytes();
    byte[] single = "{\"aaData\":[{\"hash\":\"h\",\"matchState\":\"exact\"}]}".getBytes();

    assertThat(HostedReportFileBuilder.dedupeBomIdentifiedRows(empty)).isSameAs(empty);
    assertThat(HostedReportFileBuilder.dedupeBomIdentifiedRows(single)).isSameAs(single);
  }

  @Test
  public void dedupeBomIdentifiedRows_noDuplicates_returnedUnchanged() {
    byte[] bom = ("{\"aaData\":["
        + "{\"hash\":\"h1\",\"matchState\":\"exact\",\"pathnames\":[\"a.dll\"],"
        + "\"componentIdentifier\":{\"format\":\"nuget\","
        + "\"coordinates\":{\"packageId\":\"a\",\"version\":\"1.0\"}}},"
        + "{\"hash\":\"h2\",\"matchState\":\"exact\",\"pathnames\":[\"b.dll\"],"
        + "\"componentIdentifier\":{\"format\":\"nuget\","
        + "\"coordinates\":{\"packageId\":\"b\",\"version\":\"1.0\"}}}"
        + "]}").getBytes();

    assertThat(HostedReportFileBuilder.dedupeBomIdentifiedRows(bom)).isSameAs(bom);
  }

  @Test
  public void dedupeBomIdentifiedRows_nullOrEmpty_returnedUnchanged() {
    assertThat(HostedReportFileBuilder.dedupeBomIdentifiedRows(null)).isNull();
    byte[] empty = new byte[0];
    assertThat(HostedReportFileBuilder.dedupeBomIdentifiedRows(empty)).isSameAs(empty);
  }

  @Test
  public void dedupeBomIdentifiedRows_unparseableJson_returnedUnchanged() {
    byte[] garbage = "not-json".getBytes();
    assertThat(HostedReportFileBuilder.dedupeBomIdentifiedRows(garbage)).isSameAs(garbage);
  }

  @Test
  public void zeroDataJsonPolicyCounts_nonZeroCountsAndComponentCount_replacedWithZeros() throws Exception {
    // HDS's pre-collapse view: 5 components identified, various threat levels present. When the
    // collapse gate deletes all inner rows and the surviving outer set is empty, the file must
    // be reset to a "1 component / 0 threats" shape so the drill-in table matches the pill.
    byte[] input = ("{\"totalArtifactCount\":5,\"knownArtifactCount\":5,"
        + "\"policyCounts\":[0,0,3,1,0,2,0,0,4,1,0],"
        + "\"policyComponentCount\":5}").getBytes();

    byte[] result = HostedReportFileBuilder.zeroDataJsonPolicyCounts(input);
    JsonNode parsed = MAPPER.readTree(result);

    assertThat(parsed.path("policyComponentCount").asInt())
        .as("policyComponentCount must be zero when caller signals empty outer-violations")
        .isEqualTo(0);
    assertThat(parsed.path("policyCounts").isArray()).isTrue();
    assertThat(parsed.path("policyCounts").size()).isEqualTo(11);
    for (JsonNode n : parsed.path("policyCounts")) {
      assertThat(n.asInt()).isEqualTo(0);
    }
    // Non-target fields must survive unchanged — the caller expects totalArtifactCount and
    // knownArtifactCount to already have been set by patchDataJsonTotalArtifactCount.
    assertThat(parsed.path("totalArtifactCount").asInt()).isEqualTo(5);
    assertThat(parsed.path("knownArtifactCount").asInt()).isEqualTo(5);
  }

  @Test
  public void zeroDataJsonPolicyCounts_missingPolicyCountsField_writesFreshZeroArray() throws Exception {
    // Some HDS variants omit policyCounts entirely for artifact types with no policy hits.
    // The helper still needs to emit a zero-length-11 array so ReportStatusBar.jsx (which
    // reads by index) doesn't NPE.
    byte[] input = "{\"totalArtifactCount\":1,\"knownArtifactCount\":0}".getBytes();

    byte[] result = HostedReportFileBuilder.zeroDataJsonPolicyCounts(input);
    JsonNode parsed = MAPPER.readTree(result);

    assertThat(parsed.path("policyCounts").isArray()).isTrue();
    assertThat(parsed.path("policyCounts").size()).isEqualTo(11);
    assertThat(parsed.path("policyComponentCount").asInt()).isEqualTo(0);
  }

  @Test
  public void zeroDataJsonPolicyCounts_unparseableJson_returnedUnchanged() {
    // Fail-soft: an overlay-store corruption or partial write of data.json must not throw
    // and abort the containing collapse-gate flow; the caller's outer if-guard (patchedData
    // != dataEntry.buf) then skips the redundant re-save.
    byte[] garbage = "not-json".getBytes();
    assertThat(HostedReportFileBuilder.zeroDataJsonPolicyCounts(garbage)).isSameAs(garbage);
  }

  @Test
  public void zeroDataJsonPolicyCounts_nonObjectRootJson_returnedUnchanged() throws Exception {
    // A JSON array or scalar at the root doesn't have fields to set. Return original bytes
    // instead of coercing shape — the caller is expected to handle empty-file / malformed
    // cases higher up.
    byte[] input = "[1,2,3]".getBytes();
    assertThat(HostedReportFileBuilder.zeroDataJsonPolicyCounts(input)).isSameAs(input);
  }

  @Test
  public void zeroDataJsonPolicyCounts_nullInput_returnedUnchanged() {
    assertThat(HostedReportFileBuilder.zeroDataJsonPolicyCounts(null)).isNull();
  }

  @Test
  public void zeroDataJsonPolicyCounts_emptyInput_returnedUnchanged() {
    byte[] empty = new byte[0];
    assertThat(HostedReportFileBuilder.zeroDataJsonPolicyCounts(empty)).isSameAs(empty);
  }

  private static RepositoryPolicyViolation violationOn(
      String pathname,
      String hash,
      ComponentIdentifier ci,
      String policyId,
      String policyName,
      int threatLevel)
  {
    RepositoryPolicyViolation v = violation(policyId, policyName, threatLevel);
    v.setPathname(pathname);
    v.setHash(hash);
    v.setComponentIdentifier(ci);
    return v;
  }

  private static ComponentIdentifier ci(String groupId, String artifactId, String version, String extension) {
    TreeMap<String, String> coords = new TreeMap<>();
    coords.put("groupId", groupId);
    coords.put("artifactId", artifactId);
    coords.put("version", version);
    coords.put("extension", extension);
    return new ComponentIdentifier("maven", coords);
  }
}
