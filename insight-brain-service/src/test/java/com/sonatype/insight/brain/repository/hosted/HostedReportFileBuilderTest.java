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
