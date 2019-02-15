/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.ws.rs.core.Response.ResponseBuilder;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Report
{
  private static final Logger log = LoggerFactory.getLogger(Report.class);

  private static enum ReportType
  {
    FULL, ERROR
  }

  public static ReportEntry getEntry(final File reportFile, final String name) throws IOException {
    if (name.contains("../") || name.contains("..\\")) {
      // legit callers use normalized paths, no directory traversal into restricted areas
      return null;
    }
    final File cacheFile = getCacheFile(reportFile, name);
    if (cacheFile.canRead()) {
      return new ReportEntry(name, cacheFile.lastModified(), fetch(cacheFile));
    }
    return extractEntry(reportFile, name);
  }

  public static void putEntry(final File reportFile, final String name, final byte[] buf) throws IOException {
    cache(getCacheFile(reportFile, name), buf);
  }

  public static void putEntry(final File reportFile, final String name, final String text) throws IOException {
    putEntry(reportFile, name, text.getBytes("UTF-8"));
  }

  public static String toEntryName(final String path) {
    if (null == path || path.length() == 0) {
      return "index.html";
    }
    boolean seenSlash = true;
    StringBuilder buf = null;
    for (int i = 0, len = path.length(); i < len; i++) {
      final char c = path.charAt(i);
      final boolean isSlash = '/' == c;
      if (seenSlash && isSlash) {
        if (buf == null) {
          buf = new StringBuilder(path.subSequence(0, i));
        }
      }
      else if (buf != null) {
        buf.append(c);
      }
      seenSlash = isSlash;
    }
    if (seenSlash && buf != null) {
      buf.append("index.html");
    }
    return buf != null ? buf.toString() : path;
  }

  private static void embedApplicationPublicId(Application application, File reportFile) throws IOException {
    String filename = "index.html";
    ReportEntry reportEntry = extractEntry(reportFile, filename);
    String originalIndexHtmlContent = new String(reportEntry.buf, Charset.forName("UTF-8"));
    String augmentedIndexHtmlContent = originalIndexHtmlContent.replace("applicationId = ''", "applicationId = '"
        + application.getPublicId() + "'");
    if (!augmentedIndexHtmlContent.equals(originalIndexHtmlContent)) {
      cache(getCacheFile(reportFile, filename), augmentedIndexHtmlContent.getBytes("UTF-8"));
    }
  }

  public static ReportEntry appendCacheBustingParams(ReportEntry reportEntry, String clmVersion) throws IOException {
    String originalIndexHtmlContent = new String(reportEntry.buf, Charset.forName("UTF-8"));
    String augmentedIndexHtmlContent = originalIndexHtmlContent.replace("/brain.client.js",
        "/brain.client.js?" + clmVersion).replace("/cip-loader.js", "/cip-loader.js?" + clmVersion);
    return new ReportEntry(reportEntry.name, reportEntry.time, augmentedIndexHtmlContent.getBytes("UTF-8"));
  }

  static void applyChanges(final Application application, final File reportFile)
      throws IOException
  {
    long start = System.currentTimeMillis();

    final ReportType reportType = getType(reportFile);

    if (ReportType.ERROR.equals(reportType)) {
      return;
    }

    embedApplicationPublicId(application, reportFile);

    applyComponentRelatedChanges(application, reportFile);

    // this data item is not in the original report, but is placed in the cache by the policy evaluator
    final ReportEntry policyReportEntry = getEntry(reportFile, ScanPolicyEvaluator.POLICY_THREATS_FILENAME);

    // these data items have already had changes applied as part of applyComponentRelatedChanges above
    final ContainerNode<?> security = JsonUtils.parse(getEntry(reportFile, "security.json").buf);
    final ContainerNode<?> licenses = JsonUtils.parse(getEntry(reportFile, "licenses.json").buf);
    final ContainerNode<?> partialMatched = JsonUtils.parse(getEntry(reportFile, "partialmatched.json").buf);

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier = parseDependencyDepths(JsonUtils.parse(extractEntry(
        reportFile, "dependencies.json").buf));

    /*
     * TODO: extract basic calculation method so it can be shared with the insight-scan-processor
     */

    final int[] policyCounts = new int[11];
    final int[] securityCounts = new int[10];
    final int[] licenseCounts = new int[11];

    int policyComponentCount = 0;
    int insecureArtifactCount = 0;
    int grandfatheredPolicyViolationCount = 0;

    final ArrayList<int[]> securityPunchCard = new ArrayList<>();
    final ArrayList<int[]> licensePunchCard = new ArrayList<>();

    if (policyReportEntry != null) {
      for (final JsonNode row : JsonUtils.parse(policyReportEntry.buf).get("aaData")) {
        PolicyThreats.Component component = JsonUtils.asPojo(row, PolicyThreats.Component.class);
        final int level = component.policyThreatLevel;
        policyCounts[level < 0 ? 0 : level < 11 ? level : 10]++;
        if (level >= 2) {
          policyComponentCount++;
        }
        for (PolicyThreats.PolicyViolation policyViolation : component.allViolations) {
          if (policyViolation.grandfathered) {
            grandfatheredPolicyViolationCount++;
          }
        }
      }
    }

    Set<ComponentIdentifier> components = new HashSet<>();
    for (final JsonNode row : security.get("aaData")) {
      final String status = row.path("status").asText();
      if (!SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE.getName().equals(status)) {
        final double severity = row.path("score").asDouble();
        final int threatIndex = 10 - (int) Math.floor(severity);

        securityCounts[threatIndex < 0 ? 0 : threatIndex < 10 ? threatIndex : 9]++;

        ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(row);
        if (components.add(componentIdentifier)) {
          insecureArtifactCount++;
        }

        final int counter = severity < 4 ? 2 : severity < 7 ? 1 : 0;
        updatePunchCard(securityPunchCard, componentIdentifier, depthsByIdentifier, counter);
      }
    }

    ComponentDAO componentDAO = new ComponentDAO();
    for (JsonNode licenseJsonNode : licenses.get("aaData")) {
      final Component component = componentDAO.getComponent(application, licenseJsonNode);
      ObjectNode licenseNode = (ObjectNode) licenseJsonNode;
      Integer threatLevel = component.getLicenseThreatLevel();
      licenseNode.put("effectiveLicenseThreat", threatLevel);
      if (component.isLicenseOverridden()) {
        licenseNode.put("overriddenLicenseThreat", threatLevel);
      }

      if (threatLevel != null) {
        threatLevel = Math.min(10, Math.max(0, threatLevel));
        licenseCounts[threatLevel]++;
        if (threatLevel > 0) {
          // Punch card expects 0 to be the highest threat with 2 being the lowest
          final int threatDepth = threatLevel < 4 ? 2 : threatLevel < 8 ? 1 : 0;
          updatePunchCard(licensePunchCard, component.getComponentIdentifier(), depthsByIdentifier, threatDepth);
        }
      }
    }

    for (JsonNode licenseJsonNode : partialMatched.get("aaData")) {
      final ArrayNode matchedComponentNodes = (ArrayNode) licenseJsonNode.get("matchDetails");
      for (JsonNode matchedComponentJsonNode : matchedComponentNodes) {
        final Component matchedComponent = new ComponentDAO().getComponent(application, matchedComponentJsonNode);
        ObjectNode matchedComponentNode = (ObjectNode) matchedComponentJsonNode;
        matchedComponentNode.put("effectiveLicenseThreat", matchedComponent.getLicenseThreatLevel());
        if (matchedComponent.isLicenseOverridden()) {
          matchedComponentNode.put("overriddenLicenseThreat", matchedComponent.getLicenseThreatLevel());
        }
      }
    }

    saveReportEntry(reportFile, "licenses.json", licenses);
    saveReportEntry(reportFile, "partialmatched.json", partialMatched);
    writeLicenseThreatsToReportFile(application, reportFile);

    final ObjectNode data = JsonUtils.parse(getEntry(reportFile, "data.json").buf);
    fill(data.putArray("policyCounts"), policyCounts);
    data.put("policyComponentCount", policyComponentCount);
    fill(data.putArray("securityCounts"), securityCounts);
    data.put("grandfatheredPolicyViolationCount", grandfatheredPolicyViolationCount);
    data.put("insecureArtifactCount", insecureArtifactCount);
    fill(data.putArray("effectiveLicenseCounts"), licenseCounts);
    fill(data.putArray("securityPunchCard"), securityPunchCard);
    fill(data.putArray("licensePunchCard"), licensePunchCard);

    saveReportEntry(reportFile, "data.json", data);

    log.debug("Applied changes to report in {} ms", System.currentTimeMillis() - start);
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
      for (Iterator<Map.Entry<String, JsonNode>> it = gavDepths.fields(); it.hasNext();) {
        Map.Entry<String, JsonNode> entry = it.next();
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

  private static void updatePunchCard(List<int[]> punchCard,
                                      ComponentIdentifier componentIdentifier,
                                      Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier,
                                      int level)
  {
    Set<Integer> depths = depthsByIdentifier.get(componentIdentifier);
    if (depths == null) {
      return;
    }
    for (Integer depth : depths) {
      int index = depth - 1;
      while (index >= punchCard.size()) {
        punchCard.add(new int[3]);
      }
      punchCard.get(index)[level]++;
    }
  }

  private static Map<String, HashComponentIdentifier> applyClaimedComponents(ContainerNode<?> bomJsonData,
                                                                             ContainerNode<?> dataJson,
                                                                             ContainerNode<?> summaryJsonData)
  {
    HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();
    int exactlyMatchedComponentCount = 0;
    int partiallyMatchedComponentCount = 0;
    int knownArtifactCount = 0;

    Map<String, HashComponentIdentifier> claimedComponentsByHash = new LinkedHashMap<>();
    JsonNode aaData = bomJsonData.get("aaData");
    for (JsonNode bomJsonNode : aaData) {
      String hash = bomJsonNode.get("hash").asText();
      HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(hash);
      ObjectNode bomObjectNode = (ObjectNode) bomJsonNode;

      if (hashComponentIdentifier != null) {
        ComponentIdentifier componentIdentifier = hashComponentIdentifier.getComponentIdentifier();
        if (componentIdentifier.isMaven()) {
          // reports generated before 1.13.0 still require separate GAV fields
          bomObjectNode.put("groupId", componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID));
          bomObjectNode.put("artifactId", componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
          bomObjectNode.put("version", componentIdentifier.get(ComponentIdentifier.VERSION));
          bomObjectNode.put("extension", componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION));
          bomObjectNode.put("classifier", componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER));
        }
        // injectComponentIdentifier below is for legacy reports and does not help claimed components
        bomObjectNode.set("componentIdentifier", JsonUtils.asTree(componentIdentifier));
        bomObjectNode.put("matchState", MatchState.EXACT.getId());
        bomObjectNode.put("createTime", hashComponentIdentifier.getCreateTimeLong());
        bomObjectNode.set("relativePopularity", NullNode.getInstance());
        bomObjectNode.put("identificationSource", IdentificationSource.MANUAL.getId());
        bomObjectNode.put("comment", hashComponentIdentifier.getComment());
        claimedComponentsByHash.put(hash, hashComponentIdentifier);
      }

      String matchStateString = bomObjectNode.get("matchState").asText();
      MatchState matchState = MatchState.getById(matchStateString);

      // matchState is null for Expanded Coverage reports.
      if (matchState != null && !matchState.equals(MatchState.UNKNOWN)) {
        knownArtifactCount++;
        if (matchState.equals(MatchState.SIMILAR)) {
          partiallyMatchedComponentCount++;
        }
        else {
          exactlyMatchedComponentCount++;
        }
      }
    }

    ObjectNode data = (ObjectNode) dataJson;
    ObjectNode summary = (ObjectNode) summaryJsonData;

    data.put("partiallyMatchedComponentCount", partiallyMatchedComponentCount);
    data.put("exactlyMatchedComponentCount", exactlyMatchedComponentCount);
    data.put("knownArtifactCount", knownArtifactCount);
    
    // the pdf report uses summary.json not data.json
    summary.put("knownArtifactCount", knownArtifactCount);

    log.debug("applyClaimedComponents: {} components, {} claimed.", aaData.size(), claimedComponentsByHash.size());

    return claimedComponentsByHash;
  }

  private static Set<ComponentIdentifier> fixBomComponentIdentifiers(ContainerNode<?> bomJsonData) {
    Set<ComponentIdentifier> componentIdentifiers = new LinkedHashSet<>();
    JsonNode aaData = bomJsonData.get("aaData");
    for (JsonNode bomJsonNode : aaData) {
      ObjectNode bomObjectNode = (ObjectNode) bomJsonNode;

      ComponentIdentifierAdapter.injectComponentIdentifier(bomObjectNode);
      ComponentDisplayNameUtil.injectDisplayName(bomObjectNode);
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(bomObjectNode);
      componentIdentifiers.add(componentIdentifier);
    }

    log.debug("fixBomComponentIdentifiers: {} components.", aaData.size());

    return componentIdentifiers;
  }

  private static void fixComponentIdentifiers(ContainerNode<?> jsonData,
                                              Set<ComponentIdentifier> componentIdentifiers)
  {
    ArrayNode aaData = (ArrayNode) jsonData.get("aaData");
    Iterator<JsonNode> iterJsonData = aaData.iterator();
    int removedCount = 0;
    while (iterJsonData.hasNext()) {
      ObjectNode jsonNode = (ObjectNode) iterJsonData.next();
      ComponentIdentifierAdapter.injectComponentIdentifier(jsonNode);
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonNode);

      if (!componentIdentifiers.contains(componentIdentifier)) {
        // License/security data for a component that is not in this report. Remove it.
        iterJsonData.remove();
        removedCount++;
      }
      else {
        ComponentDisplayNameUtil.injectDisplayName(jsonNode);
      }
    }

    log.debug("fixComponentIdentifiers: {} components, {} removed.", aaData.size(), removedCount);
  }

  private static Set<ComponentIdentifier> applyLicenseOverrides(ContainerNode<?> licensesJsonData,
                                                                Application application)
  {
    LicenseDAO licenseDAO = new LicenseDAO();
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides = new HashSet<>();

    ArrayNode licensesAaData = (ArrayNode) licensesJsonData.get("aaData");
    Iterator<JsonNode> iterLicenseData = licensesAaData.iterator();
    int licenseOverrideCount = 0;
    while (iterLicenseData.hasNext()) {
      ObjectNode licenseJsonNode = (ObjectNode) iterLicenseData.next();
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(licenseJsonNode);
      LicenseOverride licenseOverride = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifier(
          application.getId(), componentIdentifier);
      if (licenseOverride != null) {
        licenseOverrideCount++;
        componentIdentifiersWithLicenseOverrides.add(componentIdentifier);
        licenseJsonNode.put("status", licenseOverride.getStatus().getName());
        if (!licenseOverride.getLicenseIds().isEmpty()) {
          ArrayNode licenseOverrideNode = licenseJsonNode.putArray("overriddenLicenses");

          for (String licenseId : licenseOverride.getLicenseIds()) {
            licenseOverrideNode.add(licenseDAO.getByIdNotNull(licenseId).getShortDisplayName());
          }
        }
        if (licenseOverride.getComment() != null) {
          licenseJsonNode.put("comment", licenseOverride.getComment());
        }
      }
    }

    log.debug("applyLicenseOverrides: {} components, {} overrides.", licensesAaData.size(), licenseOverrideCount);
    return componentIdentifiersWithLicenseOverrides;
  }

  private static void applySecurityVulnerabilityOverrides(ContainerNode<?> securityJsonData, Application application) {
    SecurityVulnerabilityOverrideDAO overrideDAO = new SecurityVulnerabilityOverrideDAO();

    ArrayNode securityAaData = (ArrayNode) securityJsonData.get("aaData");
    Iterator<JsonNode> iterSecurityData = securityAaData.iterator();
    int overrideCount = 0;
    while (iterSecurityData.hasNext()) {
      ObjectNode securityJsonNode = (ObjectNode) iterSecurityData.next();
      String hash = securityJsonNode.get("hash").asText();
      String source = securityJsonNode.get("source").asText();
      String referenceId = securityJsonNode.get("reference").asText();
      SecurityVulnerabilityOverride override = overrideDAO.getByOwnerIdHashSourceAndReferenceId(application.getId(),
          hash, source, referenceId);
      if (override != null) {
        overrideCount++;
        securityJsonNode.put("status", override.getStatus().getName());
        if (override.getComment() != null) {
          securityJsonNode.put("comment", override.getComment());
        }
      }
    }

    log.debug("applySecurityVulnerabilityOverrides: {} components, {} overrides.", securityJsonData.size(),
        overrideCount);
  }

  private static Set<ComponentIdentifier> addLicenseOverridesForClaimedComponents(
      ArrayNode licensesAaData,
      Collection<HashComponentIdentifier> hashComponentIdentifiers,
      Application application)
  {
    LicenseDAO licenseDAO = new LicenseDAO();
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides = new HashSet<>();

    int licenseOverrideCount = 0;
    for (HashComponentIdentifier hashComponentIdentifier : hashComponentIdentifiers) {
      LicenseOverride licenseOverride = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifier(
          application.getId(), hashComponentIdentifier.getComponentIdentifier());
      if (licenseOverride != null) {
        licenseOverrideCount++;
        ObjectNode licenseJsonNode = licensesAaData.addObject();
        licenseJsonNode.put("hash", hashComponentIdentifier.getHash());
        ComponentIdentifier componentIdentifier = hashComponentIdentifier.getComponentIdentifier();
        componentIdentifiersWithLicenseOverrides.add(componentIdentifier);
        licenseJsonNode.set("componentIdentifier", JsonUtils.asTree(componentIdentifier));
        if (componentIdentifier.isMaven()) {
          // reports generated before 1.13.0 still require separate GAV fields
          licenseJsonNode.put("groupId", componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID));
          licenseJsonNode.put("artifactId", componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
          licenseJsonNode.put("version", componentIdentifier.get(ComponentIdentifier.VERSION));
          licenseJsonNode.put("classifier", componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER));
        }
        licenseJsonNode.put("matchState", MatchState.EXACT.getId());
        licenseJsonNode.put("catalogDate", hashComponentIdentifier.getCreateTimeLong());
        licenseJsonNode.put("status", licenseOverride.getStatus().getName());
        if (!licenseOverride.getLicenseIds().isEmpty()) {
          ArrayNode licenseOverrideNode = licenseJsonNode.putArray("overriddenLicenses");

          for (String licenseId : licenseOverride.getLicenseIds()) {
            licenseOverrideNode.add(licenseDAO.getByIdNotNull(licenseId).getShortDisplayName());
          }

        }
        if (licenseOverride.getComment() != null) {
          licenseJsonNode.put("comment", licenseOverride.getComment());
        }
      }
    }
    log.debug("addLicenseOverridesForClaimedComponents: {} overrides.", licenseOverrideCount);
    return componentIdentifiersWithLicenseOverrides;
  }

  private static void removeClaimedComponentsFromPartialMatched(
      ContainerNode<?> partialmatchedJsonData,
      Map<String, HashComponentIdentifier> claimedComponentsByHash)
  {
    JsonNode aaData = partialmatchedJsonData.get("aaData");
    Iterator<JsonNode> iterPartialMatchData = aaData.iterator();
    int removedCount = 0;
    while (iterPartialMatchData.hasNext()) {
      JsonNode jsonNode = iterPartialMatchData.next();
      String hash = jsonNode.path("hash").asText();
      if (claimedComponentsByHash.containsKey(hash)) {
        removedCount++;
        iterPartialMatchData.remove();
      }
      else {
        JsonNode matchDetails = jsonNode.get("matchDetails");
        for (JsonNode matchDetail : matchDetails) {
          ObjectNode detailsNode = (ObjectNode) matchDetail;
          ComponentIdentifierAdapter.injectComponentIdentifier(detailsNode);
          ComponentDisplayNameUtil.injectDisplayName(detailsNode);
        }
      }
    }

    log.debug("removeClaimedComponentsFromPartialMatched: {} partial matches, {} removed.", aaData.size(),
        removedCount);
  }

  /**
   * Applies changes to component data (bom/license/security/partialmatched) including claiming components
   */
  private static void applyComponentRelatedChanges(final Application application,
                                                   final File reportFile) throws IOException
  {
    long start = System.currentTimeMillis();

    ContainerNode<?> bomJsonData = loadReportEntry(reportFile, "bom.json");
    ContainerNode<?> dataJson = loadReportEntry(reportFile, "data.json");
    ContainerNode<?> summaryJsonData = loadReportEntry(reportFile, "summary.json");

    Map<String, HashComponentIdentifier> claimedComponentsByHash =
        applyClaimedComponents(bomJsonData, dataJson, summaryJsonData);
    Set<ComponentIdentifier> componentIdentifiers = fixBomComponentIdentifiers(bomJsonData);
    saveReportEntry(reportFile, "data.json", dataJson);
    saveReportEntry(reportFile, "summary.json", summaryJsonData);

    // Must start from un-edited license data.
    ContainerNode<?> licensesJsonData = loadReportEntry(reportFile, "licenses.json");
    fixComponentIdentifiers(licensesJsonData, componentIdentifiers);
    Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides = applyLicenseOverrides(licensesJsonData,
        application);
    ArrayNode licensesAaData = (ArrayNode) licensesJsonData.get("aaData");
    componentIdentifiersWithLicenseOverrides
        .addAll(addLicenseOverridesForClaimedComponents(licensesAaData, claimedComponentsByHash.values(), application));
    saveReportEntry(reportFile, "licenses.json", licensesJsonData);

    // now apply any data edits (e.g. modified flag)
    augmentModified(componentIdentifiersWithLicenseOverrides, bomJsonData);
    saveReportEntry(reportFile, "bom.json", bomJsonData);

    // must start from un-edited data
    ContainerNode<?> securityJsonData = loadReportEntry(reportFile, "security.json");
    fixComponentIdentifiers(securityJsonData, componentIdentifiers);
    applySecurityVulnerabilityOverrides(securityJsonData, application);
    saveReportEntry(reportFile, "security.json", securityJsonData);

    // must start from un-edited data
    ContainerNode<?> partialmatchedJsonData = loadReportEntry(reportFile, "partialmatched.json");
    removeClaimedComponentsFromPartialMatched(partialmatchedJsonData, claimedComponentsByHash);
    saveReportEntry(reportFile, "partialmatched.json", partialmatchedJsonData);

    log.debug("applyComponentRelatedChanges finished  in {} ms", System.currentTimeMillis() - start);
  }

  @VisibleForTesting
  static void augmentModified(Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides, JsonNode bomJsonData) {
    ArrayNode components = (ArrayNode) bomJsonData.get("aaData");
    for (int componentIndex = 0; componentIndex < components.size(); componentIndex++) {
      ObjectNode component = (ObjectNode) components.get(componentIndex);
      if (componentIdentifiersWithLicenseOverrides
          .contains(ComponentIdentifierAdapter.getComponentIdentifier(component))) {
        component.put("modified", true);
      }
    }
  }

  private static ContainerNode<?> loadReportEntry(File reportFile, String entryFileName) throws IOException {
    long start = System.currentTimeMillis();

    ReportEntry reportEntry = extractEntry(reportFile, entryFileName);
    ContainerNode<?> result = JsonUtils.parse(reportEntry.buf);

    log.debug("loadReportEntry: {} in {} ms.", entryFileName, System.currentTimeMillis() - start);

    return result;
  }

  private static void saveReportEntry(File reportFile, String entryFileName, ContainerNode<?> jsonData)
      throws IOException
  {
    long start = System.currentTimeMillis();

    cache(getCacheFile(reportFile, entryFileName), JsonUtils.generate(jsonData));

    log.debug("saveReportEntry: {} in {} ms.", entryFileName, System.currentTimeMillis() - start);
  }

  private static void writeLicenseThreatsToReportFile(final Application application, final File reportFile)
      throws IOException
  {
    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode licenseThreatsJson = mapper.createObjectNode();
    ObjectNode licenseTable = mapper.createObjectNode();
    for (MultiLicense multiLicense : multiLicenseDAO.getAll()) {
      Integer threatLevel = multiLicenseDAO.getLicenseThreatLevelByApplicationAndMultiLicenseId(application,
          multiLicense.getId());
      licenseTable.put(multiLicense.getShortDisplayName(), threatLevel);
    }
    licenseThreatsJson.set("aaData", licenseTable);
    saveReportEntry(reportFile, "licensethreats.json", licenseThreatsJson);
  }

  public static void printPdf(final File reportFile,
                              final String projectName,
                              final String stageName,
                              final ContactDTO contact,
                              final ResponseBuilder response) throws IOException
  {
    Pdf.generate(reportFile, getCacheDir(reportFile), projectName, stageName, contact, response);
  }

  public static File printPdf(final File reportFile,
                              final String projectName,
                              final String stageName,
                              final ContactDTO contact) throws IOException
  {
    return Pdf.generate(reportFile, getCacheDir(reportFile), projectName, stageName, contact);
  }

  public static void deletePdf(final File reportFile) {
    Pdf.delete(reportFile);
  }

  private static ReportEntry extractEntry(final File reportFile, final String name) throws IOException {
    // When the archive is closed, all InputStreams retrieved from this archive are also closed.
    try (final ZipFile archive = new ZipFile(reportFile)) {
      final ZipEntry entry = archive.getEntry(name);
      if (entry != null) {
        final byte[] buf = IOUtil.toByteArray(archive.getInputStream(entry));
        return new ReportEntry(entry.getName(), entry.getTime(), buf);
      }
    }
    return null;
  }

  private static ReportType getType(final File reportFile) throws IOException {
    try (final ZipFile archive = new ZipFile(reportFile)) {
      if (archive.getEntry("security.json") == null && archive.getEntry("licenses.json") == null) {
        return ReportType.ERROR;
      }
      return ReportType.FULL;
    }
  }

  /**
   * Gets the contents of the {@code template.properties} embedded in the report from the HDS or an empty map if none.
   */
  public static Properties getTemplateProperties(File reportFile) throws IOException {
    try (ZipFile archive = new ZipFile(reportFile)) {
      Properties props = new Properties();
      ZipEntry entry = archive.getEntry("template.properties");
      if (entry != null) {
        props.load(archive.getInputStream(entry));
      }
      return props;
    }
  }

  public static File getCacheDir(final File reportFile) {
    return new File(reportFile.getParentFile(), "report.cache");
  }

  // public access for tests only
  public static File getCacheFile(final File reportFile, final String name) {
    return new File(getCacheDir(reportFile), name);
  }

  private static void cache(final File cacheFile, final byte[] buf) throws IOException {
    cacheFile.getAbsoluteFile().getParentFile().mkdirs();
    final OutputStream os = new FileOutputStream(cacheFile);
    try {
      IOUtil.copy(buf, os);
    }
    finally {
      IOUtil.close(os);
    }
  }

  private static byte[] fetch(final File cacheFile) throws IOException {
    final InputStream is = new FileInputStream(cacheFile);
    try {
      return IOUtil.toByteArray(is);
    }
    finally {
      IOUtil.close(is);
    }
  }

  private static void fill(final ArrayNode node, final int[] data) {
    for (final int d : data) {
      node.add(d);
    }
  }

  private static void fill(final ArrayNode node, final List<int[]> datas) {
    for (final int[] data : datas) {
      fill(node.addArray(), data);
    }
  }
}
