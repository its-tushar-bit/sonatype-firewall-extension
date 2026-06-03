/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.Comparator;
import java.util.List;

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
      ArrayNode active = MAPPER.createArrayNode();
      ArrayNode waived = MAPPER.createArrayNode();
      ArrayNode all = MAPPER.createArrayNode();
      for (RepositoryPolicyViolation v : violations) {
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
      RepositoryPolicyViolation first = violations.stream()
          .max(Comparator.comparingInt(RepositoryPolicyViolation::getThreatLevel))
          .get();
      ObjectNode group = MAPPER.createObjectNode();
      group.put("hash", component.getHash() != null ? component.getHash() : "");
      if (component.getComponentIdentifier() != null) {
        group.set("componentIdentifier", MAPPER.valueToTree(component.getComponentIdentifier()));
      }
      group.put("policyId", first.getPolicyId());
      group.put("policyName", first.getPolicyName());
      group.put("policyThreatLevel", first.getThreatLevel());
      group.set("activeViolations", active);
      group.set("waivedViolations", waived);
      group.set("allViolations", all);
      aaData.add(group);
    }
    return MAPPER.writeValueAsBytes(root);
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
