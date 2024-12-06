/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.report.pdf.PdfGenerator;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.experimental.ApiVulnerabilitySignatureService.VULNERABILITY_SIGNATURE_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ReportDataStore.augmentDependenciesGraph;
import static com.sonatype.insight.brain.report.ReportDataStore.hideObservedLicenses;
import static com.sonatype.insight.brain.report.ReportDataStore.setMavenCoordinates;
import static com.sonatype.insight.brain.report.ReportDataStore.setMavenCoordinatesWithExtension;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME;

@Singleton
public class FileReportDataStore
    implements ReportDataStore
{
  private static final Logger log = LoggerFactory.getLogger(FileReportDataStore.class);

  public static final List<String> THIRD_PARTY_CACHED_FILES = Arrays.asList(THIRD_PARTY_BOM_JSON_FILENAME,
      THIRD_PARTY_SECURITY_JSON_FILENAME, THIRD_PARTY_LICENSE_JSON_FILENAME);

  private static final String EXACTLY_MATCHED_COMPONENT_COUNT = "exactlyMatchedComponentCount";

  private static final String KNOWN_ARTIFACT_COUNT = "knownArtifactCount";

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ThirdPartyComponentDAO thirdPartyComponentDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LicenseDAO licenseDAO;

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private final MultiLicenseDAO multiLicenseDAO;

  private final ApplicationDAO applicationDAO;

  private final InnerSourceComponentDAO innerSourceComponentDAO;

  private final ProprietaryConfigService proprietaryConfigService;

  private final InsightWork insightWork;

  private final ReportDownloader reportDownloader;

  public enum ReportType
  {
    FULL, ERROR
  }

  @Inject
  public FileReportDataStore(
      final ComponentLoaderFactory componentLoaderFactory,
      final Provider<ThirdPartyComponentDAO> thirdPartyComponentDaoProvider,
      final LicenseDAO licenseDAO,
      final HashComponentIdentifierDAO hashComponentIdentifierDAO,
      final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
      final MultiLicenseDAO multiLicenseDAO,
      final LicenseOverrideDAO licenseOverrideDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final ApplicationDAO applicationDAO,
      final InnerSourceComponentDAO innerSourceComponentDAO,
      final ProprietaryConfigService proprietaryConfigService,
      final InsightWork insightWork,
      final ReportDownloader reportDownloader
  )
  {
    this.componentLoaderFactory = componentLoaderFactory;
    this.thirdPartyComponentDAO = thirdPartyComponentDaoProvider.get();
    this.licenseDAO = licenseDAO;
    this.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.applicationDAO = applicationDAO;
    this.innerSourceComponentDAO = innerSourceComponentDAO;
    this.proprietaryConfigService = proprietaryConfigService;
    this.insightWork = insightWork;
    this.reportDownloader = reportDownloader;
  }

  @Override
  public ReportEntry getEntry(final ApplicationReport reportFile, final String name) throws IOException {
    if (name.contains("../") || name.contains("..\\")) {
      // legit callers use normalized paths, no directory traversal into restricted areas
      return null;
    }
    final File cacheFile = getCacheFile(((FileReportEntity) reportFile).getFile(), name);
    if (cacheFile.canRead()) {
      return new ReportEntry(name, cacheFile.lastModified(), fetch(cacheFile));
    }
    return extractEntry(((FileReportEntity) reportFile).getFile(), name);
  }

  @Override
  public void putEntry(final ApplicationReport reportFile, final String name, final byte[] buf) throws IOException {
    cache(getCacheFile(((FileReportEntity) reportFile).getFile(), name), buf);
  }

  /**
   * Major hack for testing only
   */
  @VisibleForTesting
  public static void putEntryStatic(final ApplicationReport applicationReport, final String name, final byte[] buf)
      throws IOException
  {
    cache(getCacheFile(((FileReportEntity) applicationReport).getFile(), name), buf);
  }

  @Override
  public void putEntry(final ApplicationReport applicationReport, final String name, final String text)
      throws IOException
  {
    putEntry(applicationReport, name, text.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String toEntryName(final String path) {
    if (null == path || path.isEmpty()) {
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

  private void embedApplicationPublicId(Application application, File reportFile) throws IOException {
    String filename = "index.html";
    ReportEntry reportEntry = extractEntry(reportFile, filename);
    String originalIndexHtmlContent = new String(reportEntry.buf, StandardCharsets.UTF_8);
    String augmentedIndexHtmlContent = originalIndexHtmlContent.replace("applicationId = ''", "applicationId = '"
        + application.getPublicId() + "'");
    if (!augmentedIndexHtmlContent.equals(originalIndexHtmlContent)) {
      cache(getCacheFile(reportFile, filename), augmentedIndexHtmlContent.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Override
  public ReportEntry appendCacheBustingParams(ReportEntry reportEntry, String clmVersion) {
    String originalIndexHtmlContent = new String(reportEntry.buf, StandardCharsets.UTF_8);
    String augmentedIndexHtmlContent = originalIndexHtmlContent.replace("/brain.client.js",
        "/brain.client.js?" + clmVersion).replace("/cip-loader.js", "/cip-loader.js?" + clmVersion);
    return new ReportEntry(reportEntry.name, reportEntry.time,
        augmentedIndexHtmlContent.getBytes(StandardCharsets.UTF_8));
  }

  private int[] getSecurityCounts(ObjectNode dataJson) {
    int[] securityCounts = new int[10];
    JsonNode securityCountsNode = dataJson.get("securityCounts");
    if (securityCountsNode != null && !securityCountsNode.isEmpty()) {
      try {
        securityCounts = JsonUtils.asPojo(securityCountsNode, int[].class);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return securityCounts;
  }

  @Override
  public void applyChanges(
      final Application application,
      final ApplicationReport applicationReport,
      final RepositoryMatcher repositoryMatcher,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      final Configuration configuration)
      throws IOException
  {
    long start = System.currentTimeMillis();

    final ReportType reportType = getType(((FileReportEntity) applicationReport).getFile());

    if (ReportType.ERROR.equals(reportType)) {
      return;
    }

    // If this is called from a policy re-evaluation, some files may be cached.
    // Start fresh by deleting any cached files.
    new FileCleaner().delete(getCacheDir(((FileReportEntity) applicationReport).getFile()));
    deletePdfReport(((FileReportEntity) applicationReport).getFile());

    embedApplicationPublicId(application, ((FileReportEntity) applicationReport).getFile());

    applyComponentRelatedChanges(application, applicationReport, repositoryMatcher, telemetrySender, telemetryUtils);
    cacheThirdPartyData(applicationReport);

    // these data items have already had changes applied as part of applyComponentRelatedChanges above
    final ContainerNode<?> security = JsonUtils.parse(getEntry(applicationReport, SECURITY_JSON_FILENAME).buf);
    final ContainerNode<?> licenses = JsonUtils.parse(getEntry(applicationReport, LICENSES_JSON_FILENAME).buf);
    final ContainerNode<?> partialMatched = JsonUtils.parse(getEntry(applicationReport, "partialmatched.json").buf);

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier =
        ReportDataStore.parseDependencyDepths(JsonUtils.parse(extractEntry(
            ((FileReportEntity) applicationReport).getFile(), DEPENDENCIES_JSON_FILENAME).buf));

    final ObjectNode data = JsonUtils.parse(getEntry(applicationReport, DATA_JSON_FILENAME).buf);
    final int[] securityCounts = getSecurityCounts(data);
    final int[] licenseCounts = new int[11];

    int insecureArtifactCount = 0;
    boolean isALPObservedLicenseEnabled = configuration.isALPObservedLicenseDetectionEnabled();

    final ArrayList<int[]> securityPunchCard = new ArrayList<>();
    final ArrayList<int[]> licensePunchCard = new ArrayList<>();

    Set<ComponentIdentifier> components = new HashSet<>();
    for (final JsonNode row : security.get("aaData")) {
      final String status = row.path("status").asText();
      if (!SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE.getName().equals(status)) {
        double severity = row.path("score").asDouble();
        updateSecurityCounts(severity, securityCounts);

        ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(row);
        if (components.add(componentIdentifier)) {
          insecureArtifactCount++;
        }

        final int counter = severity < 4 ? 2 : severity < 7 ? 1 : 0;
        updatePunchCard(securityPunchCard, componentIdentifier, depthsByIdentifier, counter);
      }
    }

    License notSupportedLicense = licenseDAO.getById(License.NOT_SUPPORTED_ID);

    ComponentLoader componentLoader = componentLoaderFactory.createComponentLoader(application);
    for (JsonNode licenseJsonNode : licenses.get("aaData")) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(licenseJsonNode);

      hideObservedLicenses(componentIdentifier,
          (ObjectNode) licenseJsonNode,
          isALPObservedLicenseEnabled,
          notSupportedLicense);

      final Component component = componentLoader.getComponent(licenseJsonNode);
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
        ObjectNode matchedComponentNode = (ObjectNode) matchedComponentJsonNode;

        final Component matchedComponent = componentLoader.getComponent(matchedComponentJsonNode);
        matchedComponentNode.put("effectiveLicenseThreat", matchedComponent.getLicenseThreatLevel());
        if (matchedComponent.isLicenseOverridden()) {
          matchedComponentNode.put("overriddenLicenseThreat", matchedComponent.getLicenseThreatLevel());
        }
      }
    }

    saveReportEntry(((FileReportEntity) applicationReport).getFile(), LICENSES_JSON_FILENAME, licenses);
    saveReportEntry(((FileReportEntity) applicationReport).getFile(), "partialmatched.json", partialMatched);
    writeLicenseThreatsToReportFile(application, ((FileReportEntity) applicationReport).getFile());

    fill(data.putArray("securityCounts"), securityCounts);
    data.put("insecureArtifactCount", insecureArtifactCount);
    fill(data.putArray("effectiveLicenseCounts"), licenseCounts);
    fill(data.putArray("securityPunchCard"), securityPunchCard);
    fill(data.putArray("licensePunchCard"), licensePunchCard);

    saveReportEntry(((FileReportEntity) applicationReport).getFile(), DATA_JSON_FILENAME, data);

    log.debug("Applied changes to report in {} ms", System.currentTimeMillis() - start);
  }

  @Override
  public void updateSecurityCounts(final double severity, int[] securityCounts) {
    final int threatIndex = 10 - (int) Math.floor(severity);
    securityCounts[threatIndex < 0 ? 0 : threatIndex < 10 ? threatIndex : 9]++;
  }

  private void cacheThirdPartyData(final ApplicationReport applicationReport) {
    THIRD_PARTY_CACHED_FILES.forEach(filename -> {
      try {
        final ReportEntry entry = getEntry(applicationReport, filename);
        if (entry != null) {
          cache(getCacheFile(((FileReportEntity) applicationReport).getFile(), filename), entry.buf);
        }
      }
      catch (IOException e) {
        log.error("Error reading third party data from report file: {}", applicationReport.getLocation(), e);
      }
    });
  }

  private void deletePdfReport(File reportFile) {
    File pdfReportFile = new File(reportFile.getParentFile(), PdfGenerator.REPORT_FILE_NAME);
    try {
      if (Files.deleteIfExists(pdfReportFile.toPath())) {
        log.debug("Deleted obsolete PDF report file: {}.", pdfReportFile.getAbsolutePath());
      }
    }
    catch (Exception e) {
      log.error("Cannot delete obsolete PDF report file: {}. Cause: {}", pdfReportFile.getAbsolutePath(),
          e.getMessage(), e);
    }
  }

  private void updatePunchCard(List<int[]> punchCard,
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

  private Map<String, HashComponentIdentifier> applyClaimedComponents(ContainerNode<?> bomJsonData,
                                                                             ContainerNode<?> dataJson,
                                                                             ContainerNode<?> summaryJsonData)
  {
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
          setMavenCoordinatesWithExtension(bomObjectNode, componentIdentifier);
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

      if (!matchState.equals(MatchState.UNKNOWN)) {
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
    data.put(EXACTLY_MATCHED_COMPONENT_COUNT, exactlyMatchedComponentCount);
    data.put(KNOWN_ARTIFACT_COUNT, knownArtifactCount);

    // the pdf report uses summary.json not data.json
    summary.put(KNOWN_ARTIFACT_COUNT, knownArtifactCount);

    log.debug("applyClaimedComponents: {} components, {} claimed.", aaData.size(), claimedComponentsByHash.size());

    return claimedComponentsByHash;
  }

  private Set<ComponentIdentifier> fixBomComponentIdentifiers(ContainerNode<?> bomJsonData) {
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

  private void fixComponentIdentifiers(ContainerNode<?> jsonData,
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

  private Set<ComponentIdentifier> applyLicenseOverrides(ContainerNode<?> licensesJsonData,
                                                                Application application)
  {
    Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides = new HashSet<>();

    if (!ReportDataStore.hasAnyLicenseOverrides(licenseOverrideDAO, application.getId())) {
      return componentIdentifiersWithLicenseOverrides;
    }

    ArrayNode licensesAaData = (ArrayNode) licensesJsonData.get("aaData");
    Iterator<JsonNode> iterLicenseData = licensesAaData.iterator();
    int licenseOverrideCount = 0;
    while (iterLicenseData.hasNext()) {
      ObjectNode licenseJsonNode = (ObjectNode) iterLicenseData.next();
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(licenseJsonNode);
      LicenseOverride licenseOverride = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(
          application, componentIdentifier);
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

  private void applySecurityVulnerabilityOverrides(ContainerNode<?> securityJsonData, Application application) {
    ArrayNode securityAaData = (ArrayNode) securityJsonData.get("aaData");
    Iterator<JsonNode> iterSecurityData = securityAaData.iterator();
    int overrideCount = 0;
    while (iterSecurityData.hasNext()) {
      ObjectNode securityJsonNode = (ObjectNode) iterSecurityData.next();
      String hash = securityJsonNode.get("hash").asText();
      String source = securityJsonNode.get("source").asText();
      String referenceId = securityJsonNode.get("reference").asText();
      SecurityVulnerabilityOverride override =
          securityVulnerabilityOverrideDAO.getByOwnerIdHashSourceAndReferenceId(application.getId(),
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

  private Set<ComponentIdentifier> addLicenseOverridesForClaimedComponents(
      ArrayNode licensesAaData,
      Collection<HashComponentIdentifier> hashComponentIdentifiers,
      Application application)
  {
    Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides = new HashSet<>();

    int licenseOverrideCount = 0;
    for (HashComponentIdentifier hashComponentIdentifier : hashComponentIdentifiers) {
      LicenseOverride licenseOverride = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(
          application, hashComponentIdentifier.getComponentIdentifier());
      if (licenseOverride != null) {
        licenseOverrideCount++;
        ObjectNode licenseJsonNode = licensesAaData.addObject();
        licenseJsonNode.put("hash", hashComponentIdentifier.getHash());
        ComponentIdentifier componentIdentifier = hashComponentIdentifier.getComponentIdentifier();
        componentIdentifiersWithLicenseOverrides.add(componentIdentifier);
        licenseJsonNode.set("componentIdentifier", JsonUtils.asTree(componentIdentifier));
        if (componentIdentifier.isMaven()) {
          // reports generated before 1.13.0 still require separate GAV fields
          setMavenCoordinates(licenseJsonNode, componentIdentifier);
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
   * Applies changes to component data (bom/license/security/partialmatched/dependencies) including claiming components
   */
  private void applyComponentRelatedChanges(
      final Application application,
      final ApplicationReport applicationReport,
      final RepositoryMatcher repositoryMatcher,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils) throws IOException
  {
    long start = System.currentTimeMillis();

    ContainerNode<?> bomJsonData = loadReportEntry(((FileReportEntity) applicationReport).getFile(), BOM_JSON_FILENAME);
    ContainerNode<?> dataJson = loadReportEntry(((FileReportEntity) applicationReport).getFile(), DATA_JSON_FILENAME);
    ContainerNode<?> summaryJsonData =
        loadReportEntry(((FileReportEntity) applicationReport).getFile(), SUMMARY_JSON_FILENAME);

    Map<String, HashComponentIdentifier> claimedComponentsByHash =
        applyClaimedComponents(bomJsonData, dataJson, summaryJsonData);

    // must start from un-edited data
    ContainerNode<?> licensesJsonData =
        loadReportEntry(((FileReportEntity) applicationReport).getFile(), LICENSES_JSON_FILENAME);
    ContainerNode<?> securityJsonData =
        loadReportEntry(((FileReportEntity) applicationReport).getFile(), SECURITY_JSON_FILENAME);
    ContainerNode<?> dependenciesJsonData =
        loadReportEntry(((FileReportEntity) applicationReport).getFile(), DEPENDENCIES_JSON_FILENAME);
    thirdPartyComponentDAO.updateReport(bomJsonData, licensesJsonData, securityJsonData, dataJson, summaryJsonData,
        applicationReport);

    Set<ComponentIdentifier> componentIdentifiers = fixBomComponentIdentifiers(bomJsonData);

    // now apply any data edits (e.g. modified flag)
    augmentDependenciesGraph(dependenciesJsonData);
    saveReportEntry(((FileReportEntity) applicationReport).getFile(), DEPENDENCIES_JSON_FILENAME, dependenciesJsonData);

    DependencyResolver
        .getInstance(dependenciesJsonData, bomJsonData, dataJson, summaryJsonData, application, telemetrySender,
            telemetryUtils, innerSourceComponentDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    componentIdentifiers.addAll(
        repositoryMatcher.match(application, bomJsonData, dataJson, summaryJsonData, licensesJsonData,
            securityJsonData));

    fixComponentIdentifiers(licensesJsonData, componentIdentifiers);
    Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides = applyLicenseOverrides(licensesJsonData,
        application);
    ArrayNode licensesAaData = (ArrayNode) licensesJsonData.get("aaData");
    componentIdentifiersWithLicenseOverrides
        .addAll(addLicenseOverridesForClaimedComponents(licensesAaData, claimedComponentsByHash.values(), application));
    saveReportEntry(((FileReportEntity) applicationReport).getFile(), LICENSES_JSON_FILENAME, licensesJsonData);

    saveReportEntry(((FileReportEntity) applicationReport).getFile(), DATA_JSON_FILENAME, dataJson);
    saveReportEntry(((FileReportEntity) applicationReport).getFile(), SUMMARY_JSON_FILENAME, summaryJsonData);

    augmentModified(componentIdentifiersWithLicenseOverrides, bomJsonData);
    saveReportEntry(((FileReportEntity) applicationReport).getFile(), BOM_JSON_FILENAME, bomJsonData);

    fixComponentIdentifiers(securityJsonData, componentIdentifiers);
    applySecurityVulnerabilityOverrides(securityJsonData, application);
    saveReportEntry(((FileReportEntity) applicationReport).getFile(), SECURITY_JSON_FILENAME, securityJsonData);

    // must start from un-edited data
    ContainerNode<?> partialmatchedJsonData =
        loadReportEntry(((FileReportEntity) applicationReport).getFile(), "partialmatched.json");
    removeClaimedComponentsFromPartialMatched(partialmatchedJsonData, claimedComponentsByHash);
    saveReportEntry(((FileReportEntity) applicationReport).getFile(), "partialmatched.json", partialmatchedJsonData);

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

  private ContainerNode<?> loadReportEntry(File reportFile, String entryFileName) throws IOException {
    long start = System.currentTimeMillis();

    ReportEntry reportEntry = extractEntry(reportFile, entryFileName);
    ContainerNode<?> result = JsonUtils.parse(reportEntry.buf);

    log.debug("loadReportEntry: {} in {} ms.", entryFileName, System.currentTimeMillis() - start);

    return result;
  }

  private void saveReportEntry(File reportFile, String entryFileName, ContainerNode<?> jsonData)
      throws IOException
  {
    long start = System.currentTimeMillis();

    cache(getCacheFile(reportFile, entryFileName), JsonUtils.generate(jsonData));

    log.debug("saveReportEntry: {} in {} ms.", entryFileName, System.currentTimeMillis() - start);
  }

  void writeLicenseThreatsToReportFile(
      final Application application,
      final File reportFile)
      throws IOException
  {
    Map<String, Integer> threatLevelsBySimpleLicenseId =
        licenseThreatGroupDAO.getLicenseThreatLevelsByApplication(application);

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode licenseTable = mapper.createObjectNode();
    for (MultiLicense multiLicense : multiLicenseDAO.getAll()) {
      Integer threatLevel = null;
      for (License license : multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(multiLicense.getId())) {
        Integer simpleLicenseThreatLevel = threatLevelsBySimpleLicenseId.get(license.getId());

        if (simpleLicenseThreatLevel != null) {
          if (threatLevel == null) {
            threatLevel = simpleLicenseThreatLevel;
          }
          else {
            threatLevel = Math.max(threatLevel, simpleLicenseThreatLevel);
          }
        }
      }
      licenseTable.put(multiLicense.getShortDisplayName(), threatLevel);
    }

    ObjectNode licenseThreatsJson = mapper.createObjectNode();
    licenseThreatsJson.set("aaData", licenseTable);
    saveReportEntry(reportFile, "licensethreats.json", licenseThreatsJson);
  }

  private ReportEntry extractEntry(final File reportFile, final String name) throws IOException {
    // When the archive is closed, all InputStreams retrieved from this archive are also closed.
    try (final ZipFile archive = new ZipFile(reportFile)) {
      final ZipEntry entry = archive.getEntry(name);
      if (entry != null) {
        final byte[] buf = IOUtils.toByteArray(archive.getInputStream(entry));
        return new ReportEntry(entry.getName(), entry.getTime(), buf);
      }
    }

    // Starting with release 1.168, we serve shared resources for legacy report from the jar
    // HDS does not include these files in the report.zip when IQ client is v1.168 or higher
    String resource = "/com/sonatype/insight/brain/legacy.report/" + name;
    try (InputStream stream = FileReportDataStore.class.getResourceAsStream(resource)) {
      if (stream != null) {
        return new ReportEntry(name, new Date().getTime(), IOUtils.toByteArray(stream));
      }
    }

    return null;
  }

  private ReportType getType(final File reportFile) throws IOException {
    try (final ZipFile archive = new ZipFile(reportFile)) {
      if (archive.getEntry(SECURITY_JSON_FILENAME) == null && archive.getEntry(LICENSES_JSON_FILENAME) == null) {
        return ReportType.ERROR;
      }
      return ReportType.FULL;
    }
  }

  /**
   * Gets the contents of the {@code template.properties} embedded in the report from the HDS or an empty map if none.
   */
  @Override
  public Properties getTemplateProperties(ApplicationReport reportFile) throws IOException {
    try (ZipFile archive = new ZipFile(((FileReportEntity) reportFile).getFile())) {
      Properties props = new Properties();
      ZipEntry entry = archive.getEntry("template.properties");
      if (entry != null) {
        props.load(archive.getInputStream(entry));
      }
      return props;
    }
  }

  public static File getCacheDir(final File reportFile) {
    File file = new File(reportFile.getParentFile(), CACHE_DIRECTORY_NAME);
    log.trace("Cache dir: {}", file.getAbsolutePath());
    return file;
  }

  // public access for tests only
  public static File getCacheFile(final File reportFile, final String name) {
    File file = new File(getCacheDir(reportFile), name);
    log.trace("Cache file: {}", file.getAbsolutePath());
    return file;
  }

  private static void cache(final File cacheFile, final byte[] buf) throws IOException {
    Files.createDirectories(cacheFile.getAbsoluteFile().getParentFile().toPath());
    Files.write(cacheFile.toPath(), buf);
  }

  private byte[] fetch(final File cacheFile) throws IOException {
    return Files.readAllBytes(cacheFile.toPath());
  }

  @Override
  public void fill(final ArrayNode node, final int[] data) {
    for (final int d : data) {
      node.add(d);
    }
  }

  @Override
  public FileReportEntity tempReport(final ApplicationReport reportFile) {
    final File tempFile =
        FileUtils.createTempFile("temp-", ".zip", ((FileReportEntity) reportFile).getFile().getParentFile());
    return new FileReportEntity(tempFile);
  }

  @Override
  public void rename(final ApplicationReport tempFile, final ApplicationReport reportFile) throws IOException {
    FileUtils.rename(((FileReportEntity) tempFile).getFile(), ((FileReportEntity) reportFile).getFile());
  }

  /**
   * Downloads a report for a scan.
   *
   * @param scanId                 of the report
   * @param tempApplicationReport             to save report to
   * @param reportTimeoutInSeconds time to wait before the report times out - 0 will not make retry attempts
   * @return true if the report was downloaded, false otherwise.
   */
  @Override
  public boolean downloadReport(
      final String scanId,
      final ApplicationReport tempApplicationReport,
      final int reportTimeoutInSeconds,
      final int retryIntervalInSeconds)
  {
    return reportDownloader.downloadReport(scanId, tempApplicationReport, reportTimeoutInSeconds,
        retryIntervalInSeconds);
  }

  private void fill(final ArrayNode node, final List<int[]> datas) {
    for (final int[] data : datas) {
      fill(node.addArray(), data);
    }
  }

  @Override
  public void appendToReport(final ApplicationReport reportFile, final ThirdPartyApplicationReportDTO dto)
      throws IOException
  {
    Map<String, Object> env = new HashMap<>();
    env.put("create", "false");
    env.put("useTempFile", Boolean.TRUE); //to avoid large byte streams created in memory
    Path archivePath = ((FileReportEntity) reportFile).getFile().toPath();
    URI archiveUri = URI.create("jar:" + archivePath.toUri());
    try (FileSystem fs = FileSystems.newFileSystem(archiveUri, env)) {
      appendFileToReportZip(fs, THIRD_PARTY_BOM_JSON_FILENAME, dto.billOfMaterials);
      appendFileToReportZip(fs, THIRD_PARTY_SECURITY_JSON_FILENAME, dto.securityRows);
      appendFileToReportZip(fs, THIRD_PARTY_LICENSE_JSON_FILENAME, dto.licenseRows);
    }
  }

  @Override
  public ApplicationReport getFileReport(final String appId, final String scanId) {
    return new FileReportEntity(insightWork.getReportFile(appId, scanId));
  }

  @Override
  public FileReportEntity getVulnerabilitySignatureJson(final String applicationId, final String reportId) {
    return new FileReportEntity(
        new File(insightWork.getReportFile(applicationId, reportId).getParentFile(),
            VULNERABILITY_SIGNATURE_JSON_FILENAME));
  }

  private void appendFileToReportZip(final FileSystem fs, final String filename, final List<?> data)
      throws IOException
  {
    Path newFile = fs.getPath(filename);
    try (Writer writer = Files.newBufferedWriter(newFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
      writer.write(new String(JsonUtils.generate(JsonUtils.aaData(data)), StandardCharsets.UTF_8));
    }
  }
}
