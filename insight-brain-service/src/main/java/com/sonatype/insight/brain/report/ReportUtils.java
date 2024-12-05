/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ReportUtils
{
  Logger log = LoggerFactory.getLogger(ReportUtils.class);

  String BOM_JSON_FILENAME = "bom.json";

  String DATA_JSON_FILENAME = "data.json";

  String SECURITY_JSON_FILENAME = "security.json";

  String SUMMARY_JSON_FILENAME = "summary.json";

  String LICENSES_JSON_FILENAME = "licenses.json";

  String DEPENDENCIES_JSON_FILENAME = "dependencies.json";

  String CACHE_DIRECTORY_NAME = "report.cache";

  String POLICY_THREATS = "policythreats.json";

  String CHILDREN_NODE = "children";

  String DIRECT_DEPENDENCY_NODE = "directDependency";

  ReportEntry getEntry(Report reportFile, String name) throws IOException;

  void putEntry(Report reportFile, String name, byte[] buf) throws IOException;

  void putEntry(Report reportFile, String name, String text) throws IOException;

  String toEntryName(String path);

  ReportEntry appendCacheBustingParams(ReportEntry reportEntry, String clmVersion);

  void applyChanges(
      Application application,
      Report reportFile,
      RepositoryMatcher repositoryMatcher,
      TelemetrySender telemetrySender,
      TelemetryUtils telemetryUtils,
      Configuration configuration)
      throws IOException;

  void updateSecurityCounts(double severity, int[] securityCounts);

  Properties getTemplateProperties(Report reportFile) throws IOException;

  void fill(ArrayNode node, int[] data);

  Report tempReport(Report reportFile);

  void rename(Report tempFile, Report reportFile) throws IOException;

  boolean downloadReport(String scanId, Report tempFile, int reportTimeoutInSeconds, int i);

  void appendToReport(Report reportFile, ThirdPartyApplicationReportDTO dto) throws IOException;

  Report getFileReport(String appId, String scanId);

  Report getVulnerabilitySignatureJson(String applicationId, String reportId) throws IOException;

  static void setMavenCoordinatesWithExtension(
      final ObjectNode objectNode,
      final ComponentIdentifier componentIdentifier)
  {
    setMavenCoordinates(objectNode, componentIdentifier);
    objectNode.put(ComponentIdentifier.MAVEN_EXTENSION, componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION));
  }

  static void setMavenCoordinates(
      final ObjectNode objectNode,
      final ComponentIdentifier componentIdentifier)
  {
    objectNode.put(ComponentIdentifier.MAVEN_GROUP_ID, componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID));
    objectNode
        .put(ComponentIdentifier.MAVEN_ARTIFACT_ID, componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
    objectNode.put(ComponentIdentifier.VERSION, componentIdentifier.get(ComponentIdentifier.VERSION));
    objectNode.put(ComponentIdentifier.MAVEN_CLASSIFIER, componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER));
  }

  @VisibleForTesting
  static Map<ComponentIdentifier, Set<Integer>> parseDependencyDepths(JsonNode dependenciesJson) {
    long start = System.currentTimeMillis();

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier = new LinkedHashMap<>();
    JsonNode componentDepths = dependenciesJson.path("componentDepths");
    JsonNode gavDepths = dependenciesJson.path("gavDepths");
    if (componentDepths.isArray()) {
      // new structure: [ { "componentIdentifier" : {...}, "depths" : [1, 2, 3] }, ... ]
      for (JsonNode element : componentDepths) {
        ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(element);
        Set<Integer> depths = new LinkedHashSet<>();
        for (final JsonNode level : element.path("depths")) {
          depths.add(level.asInt());
        }
        depthsByIdentifier.put(componentIdentifier, depths);
      }
    }
    else if (gavDepths.isObject()) {
      // legacy structure: { "g:a:v" : [1, 2, 3], ... }
      for (Iterator<Entry<String, JsonNode>> it = gavDepths.fields(); it.hasNext();) {
        Entry<String, JsonNode> entry = it.next();
        String[] gav = entry.getKey().split(":");
        if (gav.length != 3) {
          continue;
        }
        ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(gav[0], gav[1], gav[2]);
        Set<Integer> depths = new LinkedHashSet<>();
        for (final JsonNode level : entry.getValue()) {
          depths.add(level.asInt());
        }
        depthsByIdentifier.put(componentIdentifier, depths);
      }
    }

    log.debug("parseDependencyDepths: {} depthsByIdentifier, {} ms.", depthsByIdentifier.size(),
        System.currentTimeMillis() - start);

    return depthsByIdentifier;
  }

  @VisibleForTesting
  static boolean hasAnyLicenseOverrides(LicenseOverrideDAO licenseOverrideDAO, String applicationId) {
    return licenseOverrideDAO.getCountByOwnerId(applicationId) > 0;
  }

  @VisibleForTesting
  static void augmentDependenciesGraph(final JsonNode dependenciesJsonData) {
    JsonNode dependencyGraphNode = dependenciesJsonData.get("dependencyGraph");
    if (dependencyGraphNode == null) {
      return;
    }

    // root node with component identifier 'null' contains all direct dependencies
    List<ComponentIdentifier> directComponentIdentifiers = new ArrayList<>();
    for (JsonNode child : dependencyGraphNode) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(child);
      if (componentIdentifier == null && child.has(CHILDREN_NODE)) {
        for (JsonNode rootChild : child.get(CHILDREN_NODE)) {
          ((ObjectNode) rootChild).put(DIRECT_DEPENDENCY_NODE, true);
          directComponentIdentifiers.add(ComponentIdentifierAdapter.getComponentIdentifier(rootChild));
        }
        break;
      }
    }

    // setting relevant component identifiers in the full component list
    for (JsonNode child : dependencyGraphNode) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(child);
      if (componentIdentifier != null) {
        ((ObjectNode) child).put(DIRECT_DEPENDENCY_NODE, directComponentIdentifiers.contains(componentIdentifier));
      }
    }
  }

  @VisibleForTesting
  static void hideObservedLicenses(ComponentIdentifier matchedComponent,
                                   ObjectNode matchedComponentNode,
                                   boolean isALPObservedLicenseEnabled,
                                   License notSupportedLicense)
  {
    // we do no replacement for empty or only "Not-Supported" entry
    Set<String> currentObservedLicenses = JsonUtils.getStringSetFromArray(matchedComponentNode.get("observedLicenses"));
    if (CollectionUtils.isNotEmpty(currentObservedLicenses) &&
        !currentObservedLicenses.equals(Collections.singleton(notSupportedLicense.getShortDisplayName()))) {
      if (!isALPObservedLicenseEnabled && License.isAlpObservedLicenseFormatHidden(matchedComponent.getFormat())) {
        matchedComponentNode.putArray("observedLicenses")
            .add(notSupportedLicense.getShortDisplayName());
        matchedComponentNode.put("hiddenObservedLicenses", true);

        ArrayNode effectiveLicensesNode = matchedComponentNode.putArray("effectiveLicenses");
        JsonNode declaredLicenses = matchedComponentNode.get("declaredLicenses");
        if (declaredLicenses != null) {
          for (String declaredLicense : JsonUtils.getStringSetFromArray(declaredLicenses)) {
            effectiveLicensesNode.add(declaredLicense);
          }
        }
      }
      else {
        matchedComponentNode.put("hiddenObservedLicenses", false);
      }
    }
    else {
      matchedComponentNode.put("hiddenObservedLicenses", false);
    }
  }
}
