/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiArtifactoryConnectionService;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.artifactory.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.ArtifactoryClientFactory;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResult;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCache;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.lqa.LqaFormat;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RepositoryMatcher
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryMatcher.class);

  private static final Set<String> MATCHABLE_STATUSES =
      ImmutableSet.of(MatchState.UNKNOWN.getId(), MatchState.SIMILAR.getId());

  private static final Set<String> MATCHABLE_EXTENSIONS = ImmutableSet.of("jar");

  public static final String API_STORAGE_PREFIX = "/api/storage/";

  public static final String CLI_SCAN_CLIENT = "cli";

  public static final String FIELD_SHA256 = "sha256";

  public static final String FIELD_PROPRIETARY = "proprietary";

  public static final String FIELD_MATCH_STATE = "matchState";

  public static final String FIELD_FILENAMES = "filenames";

  public static final String FIELD_PATHNAMES = "pathnames";

  public static final String FIELD_AGGREGATE_FILES = "aggregateFiles";

  public static final String FIELD_SCAN_ERROR = "scanError";

  public static final String FIELD_RELATIVE_POPULARITY = "relativePopularity";

  public static final String FIELD_CREATE_TIME = "createTime";

  public static final String FIELD_LAST_MODIFIED_TIME = "lastModifiedTime";

  public static final String FIELD_LAST_MODIFIED_ENTRY_TIME = "lastModifiedEntryTime";

  public static final String FIELD_WEBSITE = "website";

  public static final String FIELD_IDENTIFICATION_SOURCE = "identificationSource";

  public static final String FIELD_COMPONENT_CATEGORIES = "componentCategories";

  public static final String FIELD_HYGIENE_RATING = "hygieneRating";

  public static final String FIELD_ANALYZER_FEATURES = "analyzerFeatures";

  public static final String FIELD_SCAN_CLIENT = "scanClient";

  public static final String FIELD_INTEGRITY_RATING = "integrityRating";

  public static final String FIELD_PARTIALLY_MATCHED_COMPONENT_COUNT = "partiallyMatchedComponentCount";

  public static final String FIELD_EXACTLY_MATCHED_COMPONENT_COUNT = "exactlyMatchedComponentCount";

  public static final String FIELD_KNOWN_ARTIFACT_COUNT = "knownArtifactCount";

  public static final String FIELD_HASH = "hash";

  public static final String FIELD_DECLARED_LICENSES = "declaredLicenses";

  public static final String FIELD_OBSERVED_LICENSES = "observedLicenses";

  public static final String FIELD_EFFECTIVE_LICENSES = "effectiveLicenses";

  public static final String FIELD_CATALOG_DATE = "catalogDate";

  public static final String FIELD_URL = "url";

  public static final String FIELD_REFERENCE = "reference";

  public static final String FIELD_SOURCE = "source";

  public static final String FIELD_SCORE = "score";

  public static final String FIELD_VULNERABILITY_CATEGORIES = "vulnerabilityCategories";

  public static final String FIELD_MATCHED_BY_COORDINATES = "matchedByCoordinates";

  public static final String FIELD_CWE = "cwe";

  public static final String FIELD_VECTOR_SOURCE = "cvssVectorSource";

  public static final String FIELD_VECTOR_STRING = "cvssVectorString";

  public static final String BFS_ARTIFACTORY_EXPIRED_TOKEN_SUBJECT = "Nexus IQ Server Artifactory Token has Expired";

  public static final String BFS_ARTIFACTORY_EXPIRED_TOKEN_BODY = "The Artifactory token, which the Nexus IQ Server " +
      "at %s is using for the owner with ID \"%s\", has expired.<br/><br/> This prevents built-from-source from " +
      "attempting identification of some components and may result in more similar or unknown matches in your policy " +
      "evaluations.";

  public static final String FIELD_AA_DATA = "aaData";

  public static final String NOT_SUPPORTED_LICENSE_NAME = "Not Supported";

  private final ArtifactoryConnectionDAO artifactoryConnectionDao;

  private final ArtifactoryClientFactory artifactoryClientFactory;

  private final PasswordHandler passwordHandler;

  private final ApiComponentDetailsServiceV2 apiComponentDetailsServiceV2;

  private final ApiArtifactoryConnectionService artifactoryConnectionService;

  private final RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache;

  private final Configuration configuration;

  private final InsightMail insightMail;

  private final ProprietaryConfigService proprietaryConfigService;

  @Inject
  public RepositoryMatcher(
      final ArtifactoryConnectionDAO artifactoryConnectionDao,
      final ArtifactoryClientFactory artifactoryClientFactory,
      final ApiArtifactoryConnectionService artifactoryConnectionService,
      final PasswordHandler passwordHandler,
      final ApiComponentDetailsServiceV2 apiComponentDetailsServiceV2,
      final RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache,
      final Configuration configuration,
      final InsightMail insightMail,
      final ProprietaryConfigService proprietaryConfigService)
  {
    this.artifactoryConnectionDao = artifactoryConnectionDao;
    this.artifactoryClientFactory = artifactoryClientFactory;
    this.artifactoryConnectionService = artifactoryConnectionService;
    this.passwordHandler = passwordHandler;
    this.apiComponentDetailsServiceV2 = apiComponentDetailsServiceV2;
    this.repositoryIdentifiedComponentCache = repositoryIdentifiedComponentCache;
    this.configuration = configuration;
    this.insightMail = insightMail;
    this.proprietaryConfigService = proprietaryConfigService;
  }

  public Set<ComponentIdentifier> match(
      Owner owner,
      JsonNode bomJson,
      JsonNode dataJson,
      JsonNode summaryJson,
      JsonNode licensesJson,
      JsonNode securityJson)
  {
    if (!SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()) {
      return Collections.emptySet();
    }

    // TODO CLM-44136: Artifactory BFS matching is Application-only for now.
    if (!(owner instanceof Application)) {
      return Collections.emptySet();
    }

    Set<ComponentIdentifier> result = new HashSet<>();
    ArtifactoryConnection connection = null;
    try {
      long start = System.currentTimeMillis();
      connection = getArtifactoryConnection(owner.getId());
      Map<ComponentIdentifier, ObjectNode> sha256Matched = identify(connection, bomJson);
      log.debug("performed repository matching in {} seconds with {} identified results",
          (System.currentTimeMillis() - start) / 1000, sha256Matched.size());
      start = System.currentTimeMillis();
      Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier =
          getEvaluationByIdentifier(new ArrayList<>(sha256Matched.keySet()));
      log.debug("performed component evaluation in {} seconds with {} evaluation results",
          (System.currentTimeMillis() - start) / 1000, evaluationByIdentifier.size());
      start = System.currentTimeMillis();
      result.addAll(updateJsonFiles(owner, (ObjectNode) bomJson, (ObjectNode) dataJson, (ObjectNode) summaryJson,
          (ObjectNode) licensesJson, (ObjectNode) securityJson, sha256Matched, evaluationByIdentifier));
      log.debug("updated json files in {} seconds", (System.currentTimeMillis() - start) / 1000);
    }
    catch (Exception e) {
      log.error("Failed to perform Artifactory repository matching.", e);
      sendBfsArtifactoryExpiredTokenEmailIfNeeded(connection, e);
    }
    return result;
  }

  private void sendBfsArtifactoryExpiredTokenEmailIfNeeded(ArtifactoryConnection connection, Exception exception) {
    if (connection == null) {
      return;
    }
    if (!ExceptionUtils.getFullStackTrace(exception).matches(configuration.getBfsArtifactoryExpiredTokenRegex())) {
      return;
    }
    String email = configuration.getBfsArtifactoryExpiredTokenEmail();
    if (email == null) {
      return;
    }
    try {
      insightMail.sendHtml(email, BFS_ARTIFACTORY_EXPIRED_TOKEN_SUBJECT,
          String.format(BFS_ARTIFACTORY_EXPIRED_TOKEN_BODY, configuration.getBaseUrlConfiguration().getBaseUrl(),
              connection.getOwnerId()));
    }
    catch (Exception e) {
      log.error("Failed to send artifactory expired token email.", e);
    }
  }

  // Visible for testing
  Set<ComponentIdentifier> updateJsonFiles(
      Owner owner,
      ObjectNode bomJson,
      ObjectNode dataJson,
      ObjectNode summaryJson,
      ObjectNode licensesJson,
      ObjectNode securityJson,
      Map<ComponentIdentifier, ObjectNode> bomNodeByIdentifier,
      Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier)
  {
    if (evaluationByIdentifier.isEmpty()) {
      return Collections.emptySet();
    }
    Set<ComponentIdentifier> result = new HashSet<>();
    Predicate<String> isProprietary = proprietaryConfigService.createIsProprietary(owner.getId());
    AtomicInteger unknown = new AtomicInteger();
    AtomicInteger similar = new AtomicInteger();
    for (Entry<ComponentIdentifier, ComponentEvaluationData> entry : evaluationByIdentifier.entrySet()) {
      updateComponentInformation(entry, bomNodeByIdentifier, isProprietary, result, bomJson, licensesJson, securityJson,
          unknown, similar);
    }
    updateDataJson(dataJson, unknown.intValue(), similar.intValue());
    updateSummaryJson(summaryJson, unknown.intValue(), similar.intValue());
    return result;
  }

  private static void updateComponentInformation(
      final Entry<ComponentIdentifier, ComponentEvaluationData> entry,
      final Map<ComponentIdentifier, ObjectNode> bomNodeByIdentifier,
      final Predicate<String> isProprietary,
      final Set<ComponentIdentifier> result,
      final ObjectNode bomJson,
      final ObjectNode licensesJson,
      final ObjectNode securityJson,
      final AtomicInteger unknown,
      final AtomicInteger similar)
  {
    try {
      ComponentIdentifier componentIdentifier = entry.getKey();
      ComponentEvaluationData evaluation = entry.getValue();
      ObjectNode bomNode = bomNodeByIdentifier.get(componentIdentifier);
      String hash = bomNode.get(FIELD_HASH).asText();
      boolean proprietary = componentIdentifier.getProprietaryCoordinates().stream().anyMatch(isProprietary);
      result.add(componentIdentifier);
      MatchState matchState = MatchState.getById(bomNode.get(FIELD_MATCH_STATE).asText());
      if (MatchState.UNKNOWN.equals(matchState)) {
        unknown.getAndIncrement();
      }
      else if (MatchState.SIMILAR.equals(matchState)) {
        similar.getAndIncrement();
      }

      if (StringUtils.isNotBlank(evaluation.matchState)) {
        boolean shouldIdentifyAsExternalRepository =
            MatchState.UNKNOWN.equals(MatchState.getById(evaluation.matchState));
        updateBomJson(bomJson, componentIdentifier, bomNode, proprietary, evaluation,
            shouldIdentifyAsExternalRepository);
        updateLicensesJson(licensesJson, componentIdentifier, hash, proprietary, evaluation,
            shouldIdentifyAsExternalRepository);
        updateSecurityJson(securityJson, componentIdentifier, hash, proprietary, evaluation,
            shouldIdentifyAsExternalRepository);
      }
    }
    catch (Exception e) {
      log.error("Failed to update json files for component {}.", entry.getKey(), e);
    }
  }

  // Visible for testing
  static void updateBomJson(
      final ObjectNode bomJson,
      final ComponentIdentifier componentIdentifier,
      final ObjectNode bomNode,
      final boolean proprietary,
      final ComponentEvaluationData evaluation,
      final boolean isExternalRepositoryIdentified)
  {
    ArrayNode bomNodes = (ArrayNode) bomJson.get(FIELD_AA_DATA);
    removeNodesByHash(bomNodes, bomNode.get(FIELD_HASH).asText());
    ObjectNode newBomNode = bomNodes.addObject();
    updateComponentIdentifier(newBomNode, componentIdentifier);
    newBomNode.set(FIELD_FILENAMES, bomNode.get(FIELD_FILENAMES));
    newBomNode.set(FIELD_PATHNAMES, bomNode.get(FIELD_PATHNAMES));
    newBomNode.set(FIELD_AGGREGATE_FILES, bomNode.get(FIELD_AGGREGATE_FILES));
    newBomNode.put(FIELD_MATCH_STATE, MatchState.EXACT.getId());
    newBomNode.set(FIELD_SCAN_ERROR, bomNode.get(FIELD_SCAN_ERROR));
    newBomNode.put(FIELD_PROPRIETARY, proprietary);
    newBomNode.set(FIELD_SHA256, bomNode.get(FIELD_SHA256));

    if (isExternalRepositoryIdentified) {
      newBomNode.put(FIELD_IDENTIFICATION_SOURCE, IdentificationSource.EXTERNAL_REPO.getId());
      newBomNode.set(FIELD_RELATIVE_POPULARITY, NullNode.getInstance());
      newBomNode.set(FIELD_CREATE_TIME, NullNode.getInstance());
      newBomNode.set(FIELD_LAST_MODIFIED_TIME, NullNode.getInstance());
      newBomNode.set(FIELD_LAST_MODIFIED_ENTRY_TIME, NullNode.getInstance());
      newBomNode.set(FIELD_WEBSITE, NullNode.getInstance());
      newBomNode.putArray(FIELD_COMPONENT_CATEGORIES);
      newBomNode.set(FIELD_HYGIENE_RATING, NullNode.getInstance());
      newBomNode.set(FIELD_INTEGRITY_RATING, NullNode.getInstance());
      newBomNode.set(FIELD_HASH, bomNode.get(FIELD_HASH));

      newBomNode.set(FIELD_ANALYZER_FEATURES, JsonUtils.asTree(new AnalyzerFeatures(AnalysisSource.THIRD_PARTY,
          AnalysisType.COORDINATE,
          bomNode.path(FIELD_ANALYZER_FEATURES).path(FIELD_SCAN_CLIENT).asText(CLI_SCAN_CLIENT), null)));
    }
    else {
      newBomNode.put(FIELD_RELATIVE_POPULARITY, evaluation.relativePopularity);
      newBomNode.put(FIELD_CREATE_TIME, evaluation.catalogDate);
      newBomNode.set(FIELD_LAST_MODIFIED_TIME, bomNode.get(FIELD_LAST_MODIFIED_TIME));
      newBomNode.set(FIELD_LAST_MODIFIED_ENTRY_TIME, bomNode.get(FIELD_LAST_MODIFIED_ENTRY_TIME));
      newBomNode.set(FIELD_WEBSITE, bomNode.get(FIELD_WEBSITE));
      newBomNode.put(FIELD_IDENTIFICATION_SOURCE, IdentificationSource.SONATYPE_EXTERNAL_REPO.getId());
      if (CollectionUtils.isNotEmpty(evaluation.componentCategories)) {
        newBomNode.set(FIELD_COMPONENT_CATEGORIES, convert(evaluation.componentCategories));
      }
      else {
        newBomNode.putArray(FIELD_COMPONENT_CATEGORIES);
      }
      newBomNode.set(FIELD_HYGIENE_RATING, convert(evaluation.hygieneRating));
      newBomNode.set(FIELD_INTEGRITY_RATING, convert(evaluation.integrityRating));
      newBomNode.put(FIELD_HASH, evaluation.hash);
      newBomNode.set(FIELD_ANALYZER_FEATURES, convert(createAnalyzerFeatures(componentIdentifier.getFormat(),
          bomNode.path(FIELD_ANALYZER_FEATURES).path(FIELD_SCAN_CLIENT).asText(CLI_SCAN_CLIENT))));
    }
  }

  // Visible for testing
  static AnalyzerFeatures createAnalyzerFeatures(String format, String scanClient) {
    if (ComponentIdentifier.getFormatsSupportedByHds().contains(format)) {
      if (ComponentIdentifier.NO_LICENSE_FORMATS.contains(format)) {
        return new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, scanClient, false, true, true);
      }
      else {
        return new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, scanClient, true, true, true);
      }
    }
    else if (LqaFormat.isLqaFormat(format)) {
      return new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, scanClient, false, false, true);
    }
    return null;
  }

  // Visible for testing
  static void updateLicensesJson(
      final ObjectNode licensesJson,
      final ComponentIdentifier componentIdentifier,
      final String hash,
      final boolean proprietary,
      final ComponentEvaluationData evaluation,
      final boolean isExternalRepositoryIdentified)
  {
    ArrayNode licenseNodes = (ArrayNode) licensesJson.get(FIELD_AA_DATA);
    removeNodesByHash(licenseNodes, hash);
    ObjectNode licenseNode = licenseNodes.addObject();
    updateComponentIdentifier(licenseNode, componentIdentifier);
    Set<String> declaredLicenseIds = new HashSet<>();
    Set<String> observedLicenseIds = new HashSet<>();

    if (!isExternalRepositoryIdentified) {
      licenseNode.put(FIELD_HASH, evaluation.hash);
      licenseNode.put(FIELD_CATALOG_DATE, evaluation.catalogDate);
      declaredLicenseIds = convert(evaluation.declaredLicenses);
      observedLicenseIds = convert(evaluation.observedLicenses);
    }
    else {
      licenseNode.put(FIELD_HASH, hash);
      licenseNode.set(FIELD_CATALOG_DATE, NullNode.getInstance());
    }

    if (declaredLicenseIds.isEmpty()) {
      declaredLicenseIds.add(NOT_SUPPORTED_LICENSE_NAME);
    }

    if (observedLicenseIds.isEmpty()) {
      observedLicenseIds.add(NOT_SUPPORTED_LICENSE_NAME);
    }

    Set<String> effectiveLicenseIds =
        ComponentDetailsLoader.calculateEffectiveLicenses(declaredLicenseIds, observedLicenseIds);
    licenseNode.set(FIELD_EFFECTIVE_LICENSES, convert(effectiveLicenseIds));

    licenseNode.set(FIELD_DECLARED_LICENSES, convert(declaredLicenseIds));
    licenseNode.set(FIELD_OBSERVED_LICENSES, convert(observedLicenseIds));

    licenseNode.put(FIELD_MATCH_STATE, MatchState.EXACT.getId());
    licenseNode.put(FIELD_PROPRIETARY, proprietary);
    licenseNode.put(FIELD_MATCHED_BY_COORDINATES, true);
  }

  private static Set<String> convert(Set<License> licenses) {
    if (licenses == null) {
      return new HashSet<>();
    }
    return licenses.stream().map(License::getLicenseName).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  // Visible for testing
  static void updateSecurityJson(
      ObjectNode securityJson,
      ComponentIdentifier componentIdentifier,
      String hash,
      boolean proprietary,
      ComponentEvaluationData evaluation,
      boolean isExternalRepositoryIdentified)
  {
    ArrayNode securityNodes = (ArrayNode) securityJson.get(FIELD_AA_DATA);
    removeNodesByHash(securityNodes, hash);
    if (evaluation.securityVulnerabilities == null || isExternalRepositoryIdentified) {
      return;
    }
    for (SecurityVulnerability securityVulnerability : evaluation.securityVulnerabilities) {
      ObjectNode securityVulnerabilityNode = securityNodes.addObject();
      securityVulnerabilityNode.put(FIELD_HASH, evaluation.hash);
      updateComponentIdentifier(securityVulnerabilityNode, componentIdentifier);
      securityVulnerabilityNode.put(FIELD_URL, securityVulnerability.getUrl());
      securityVulnerabilityNode.put(FIELD_REFERENCE, securityVulnerability.getRefId());
      securityVulnerabilityNode.put(FIELD_SOURCE, securityVulnerability.getSource());
      securityVulnerabilityNode.put(FIELD_SCORE, securityVulnerability.getSeverity());
      securityVulnerabilityNode.put(FIELD_MATCH_STATE, MatchState.EXACT.getId());
      securityVulnerabilityNode.put(FIELD_PROPRIETARY, proprietary);
      securityVulnerabilityNode.set(FIELD_VULNERABILITY_CATEGORIES,
          convert(securityVulnerability.getVulnerabilityCategories()));
      securityVulnerabilityNode.put(FIELD_CWE, securityVulnerability.getCwe());
      securityVulnerabilityNode.put(FIELD_VECTOR_SOURCE, securityVulnerability.getCvssVectorSource());
      securityVulnerabilityNode.put(FIELD_VECTOR_STRING, securityVulnerability.getCvssVector());
    }
  }

  // Visible for testing
  static void updateComponentIdentifier(ObjectNode objectNode, ComponentIdentifier componentIdentifier) {
    objectNode.set(ComponentIdentifierAdapter.COMPONENT_IDENTIFIER, convert(componentIdentifier));
    objectNode.put(ComponentIdentifierAdapter.PURL_IDENTIFIER,
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl());
    if (ComponentIdentifier.FORMAT_MAVEN.equals(componentIdentifier.getFormat())) {
      ReportService.setMavenCoordinatesWithExtension(objectNode, componentIdentifier);
    }
    ComponentDisplayNameUtil.injectDisplayName(objectNode);
  }

  // Visible for testing
  static void updateDataJson(ObjectNode dataJson, int unknown, int similar) {
    int newKnown = unknown + similar;
    int partiallyMatchedComponentCount =
        Math.max(0, dataJson.path(FIELD_PARTIALLY_MATCHED_COMPONENT_COUNT).asInt() - similar);
    dataJson.put(FIELD_PARTIALLY_MATCHED_COMPONENT_COUNT, partiallyMatchedComponentCount);
    int exactlyMatchedComponentCount = dataJson.path(FIELD_EXACTLY_MATCHED_COMPONENT_COUNT).asInt() + newKnown;
    dataJson.put(FIELD_EXACTLY_MATCHED_COMPONENT_COUNT, exactlyMatchedComponentCount);
    int knownArtifactCount = Math.max(newKnown, dataJson.path(FIELD_KNOWN_ARTIFACT_COUNT).asInt() + unknown);
    dataJson.put(FIELD_KNOWN_ARTIFACT_COUNT, knownArtifactCount);
  }

  // Visible for testing
  static void updateSummaryJson(ObjectNode summaryJson, int unknown, int similar) {
    int knownArtifactCount =
        Math.max(unknown + similar, summaryJson.path(FIELD_KNOWN_ARTIFACT_COUNT).asInt() + unknown);
    summaryJson.put(FIELD_KNOWN_ARTIFACT_COUNT, knownArtifactCount);
  }

  // visible for testing
  Map<ComponentIdentifier, ObjectNode> identify(final ArtifactoryConnection connection, final JsonNode bomJson) {
    Map<ComponentIdentifier, ObjectNode> identifiedComponents = new HashMap<>();
    if (connection != null) {
      Set<ObjectNode> filteredNodes = filterMatchableNodes(bomJson);
      if (CollectionUtils.isNotEmpty(filteredNodes)) {

        Set<String> nodesToRemove = matchWithRepository(identifiedComponents, connection, filteredNodes);

        // Remove nodes that only have sha256 (not coordinates) and were not matched by BFS
        removeUnknownComponentsWithSha256(bomJson, nodesToRemove);
      }
    }
    return identifiedComponents;
  }

  private void removeUnknownComponentsWithSha256(final JsonNode bomJson, Set<String> nodesToRemove) {
    Iterator<JsonNode> iterator = bomJson.get(FIELD_AA_DATA).elements();
    while (iterator.hasNext()) {
      if (nodesToRemove.contains(iterator.next().path("sha256").asText())) {
        iterator.remove();
      }
    }
  }

  private ArtifactoryConnection getArtifactoryConnection(String applicationId) {
    ApiArtifactoryConnectionStatusResponseDTO statusDTO =
        artifactoryConnectionService.getOwnerArtifactoryConnectionStatus(OwnerType.APPLICATION, applicationId);
    String effectiveOwnerId = null;

    if (Boolean.TRUE.equals(statusDTO.inheritedFromOrgEnabled)) {
      effectiveOwnerId = statusDTO.inheritedFromOrganizationId;
    }
    else if (statusDTO.allowChange && Boolean.TRUE.equals(statusDTO.enabled)) {
      effectiveOwnerId = applicationId;
    }

    return effectiveOwnerId != null ? artifactoryConnectionDao.getByOwnerId(effectiveOwnerId) : null;
  }

  // Visible for testing
  Map<ComponentIdentifier, ComponentEvaluationData> getEvaluationByIdentifier(
      List<ComponentIdentifier> componentIdentifiers)
  {
    Map<ComponentIdentifier, ComponentEvaluationData> result = new HashMap<>();
    try {
      List<ComponentEvaluationData> componentDetailsListFromHds =
          apiComponentDetailsServiceV2.getComponentDetailsListFromHds(componentIdentifiers,
              ApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
      for (ComponentEvaluationData componentEvaluationData : componentDetailsListFromHds) {
        result.put(componentIdentifiers.get(componentEvaluationData.requestIndex), componentEvaluationData);
      }
    }
    catch (Exception e) {
      log.error("Unable to evaluate repository components.", e);
    }
    return result;
  }

  private Set<String> matchWithRepository(
      final Map<ComponentIdentifier, ObjectNode> identifiedComponents,
      final ArtifactoryConnection artifactoryConnection,
      final Set<ObjectNode> nodes)
  {
    Map<String, ObjectNode> unresolvedNodesBySha256 = new HashMap<>();
    for (ObjectNode node : nodes) {
      String sha256 = node.get(FIELD_SHA256).asText();
      ComponentIdentifier resolvedId = repositoryIdentifiedComponentCache.get(sha256);
      if (resolvedId == null) {
        unresolvedNodesBySha256.put(sha256, node);
      }
      else {
        identifiedComponents.put(resolvedId, node);
      }
    }

    Map<String, ComponentIdentifier> resolved =
        resolveComponentIdentifierFromArtifactory(artifactoryConnection, unresolvedNodesBySha256.keySet());

    resolved.forEach((key, value) -> {
      repositoryIdentifiedComponentCache.put(key, value);
      identifiedComponents.put(value, unresolvedNodesBySha256.get(key));
      unresolvedNodesBySha256.remove(key);
    });

    Set<String> sha256ToRemove = new HashSet<>();
    for (Entry<String, ObjectNode> unmatchedComponent : unresolvedNodesBySha256.entrySet()) {
      ObjectNode node = unmatchedComponent.getValue();
      if (isSbomComponentWithoutCoordinates(node)) {
        sha256ToRemove.add(unmatchedComponent.getKey());
      }
    }
    return sha256ToRemove;
  }

  private Map<String, ComponentIdentifier> resolveComponentIdentifierFromArtifactory(
      ArtifactoryConnection artifactoryConnection,
      Set<String> sha256s)
  {
    Map<String, ComponentIdentifier> result = new HashMap<>();
    try {
      if (!sha256s.isEmpty()) {

        ArtifactoryClient artifactoryClient = artifactoryClientFactory.create()
            .forArtifactory(artifactoryConnection.getBaseUrl(), artifactoryConnection.getUsername(),
                passwordHandler.decryptPassword(artifactoryConnection.getPassword()));

        Integer componentQueryLimit = configuration.getBfsComponentLimit();
        Set<String> repositories =
            new LinkedHashSet<>(Arrays.asList(
                Objects.toString(configuration.getBfsQueryRepositoriesList(), "").split(",")));
        if (componentQueryLimit == null || componentQueryLimit > 0) {
          if (StringUtils.isNotBlank(artifactoryConnection.getUsername())) {
            queryUsingAQL(artifactoryClient, sha256s, componentQueryLimit, result, repositories);
          }
          else {
            queryUsingChecksum(artifactoryClient, sha256s, componentQueryLimit, result, repositories);
          }
        }

        if (componentQueryLimit == null) {
          if (result.size() > 0) {
            log.debug("Artifactory search for {} checksum(s) resulted in {} match(es).", sha256s.size(), result.size());
          }
          else {
            log.debug("Artifactory search for {} checksum(s) resulted in no matches.", sha256s.size());
          }
        }
        else {
          log.debug("Artifactory search, limited to {} queries, for {} checksum(s), resulted in {} match(es).",
              componentQueryLimit, sha256s.size(), result.size());
        }
      }
    }
    catch (IOException e) {
      log.error("Checksum search error for repository connection uri {}", artifactoryConnection.getBaseUrl(), e);
    }
    return result;
  }

  private void queryUsingAQL(
      ArtifactoryClient artifactoryClient,
      Set<String> sha256s,
      Integer componentQueryLimit,
      Map<String, ComponentIdentifier> result,
      Set<String> repositories) throws IOException
  {
    if (componentQueryLimit != null && sha256s.size() > componentQueryLimit) {
      sha256s = new HashSet<>(Iterables.partition(sha256s, componentQueryLimit).iterator().next());
    }

    Integer aqlBatchSize = configuration.getBfsArtifactoryAqlBatchSize();

    for (Entry<String, ArtifactoryChecksumSearchResults> entry : artifactoryClient.searchByChecksumsUsingAQL(
        ChecksumType.SHA256, sha256s, repositories, aqlBatchSize).entrySet())
    {
      ComponentIdentifier componentIdentifier = resolveComponentIdentifier(entry.getValue());
      if (componentIdentifier != null) {
        result.put(entry.getKey(), componentIdentifier);
      }
    }
  }

  private void queryUsingChecksum(
      ArtifactoryClient artifactoryClient,
      Set<String> sha256s,
      Integer componentQueryLimit,
      Map<String, ComponentIdentifier> result,
      Set<String> repositories) throws IOException
  {
    int requestCounter = 0;
    for (String sha256 : sha256s) {
      ArtifactoryChecksumSearchResults artifactoryChecksumSearchResults =
          artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256, repositories);

      ComponentIdentifier componentIdentifier = resolveComponentIdentifier(artifactoryChecksumSearchResults);
      if (componentIdentifier != null) {
        result.put(sha256, componentIdentifier);
      }
      requestCounter++;

      if (componentQueryLimit != null && requestCounter == componentQueryLimit) {
        break;
      }
    }
  }

  private static ComponentIdentifier resolveComponentIdentifier(
      final ArtifactoryChecksumSearchResults artifactoryChecksumSearchResults)
  {
    for (ArtifactoryChecksumSearchResult result : artifactoryChecksumSearchResults.results) {
      ComponentIdentifier resolvedId = resolveComponentIdentifierFromUri(result.uri);
      if (resolvedId != null && hasRequiredCoordinates(resolvedId)) {
        return resolvedId;
      }
      else {
        log.debug("Unable to parse the uri {}.", result.uri);
      }
    }
    return null;
  }

  // visible for testing
  static ComponentIdentifier resolveComponentIdentifierFromUri(final String uriString) {
    if (StringUtils.isBlank(uriString)) {
      return null;
    }

    try {
      String path = new URI(uriString).getPath();
      int start = path.indexOf(API_STORAGE_PREFIX);
      if (start == -1) {
        return null;
      }
      path = path.substring(start + API_STORAGE_PREFIX.length());
      String[] pathParts = StringUtils.split(path, "/");
      // We expect at least [repo]/[group]/[artifact]/[version]/[filename.extension]
      log.debug("Parsing the path {} from Artifactory.", path);
      if (pathParts.length >= 5) {
        String extension = resolveExtension(pathParts[pathParts.length - 1]);
        pathParts = ArrayUtils.removeAll(pathParts, 0, pathParts.length - 1); // remove repository and filename
        String version = resolvePathPart(pathParts[pathParts.length - 1]);
        String name = resolvePathPart(pathParts[pathParts.length - 2]);
        String namespace = resolvePathPart(StringUtils.join(
            ArrayUtils.removeAll(pathParts, pathParts.length - 1, pathParts.length - 2), "."));

        if (isValidExtension(extension)) {
          return ComponentIdentifier.createMavenCoordinates(namespace, name, version, null, extension);
        }
        else {
          log.debug("The path {} from Artifactory has an unsupported extension", path);
          return null;
        }
      }
    }
    catch (URISyntaxException e) {
      log.debug("bad result uri from artifactory {}", uriString, e);
    }
    return null;
  }

  private static String resolveExtension(final String pathPart) {
    if (StringUtils.isBlank(pathPart)) {
      return null;
    }

    return FilenameUtils.getExtension(pathPart);
  }

  private static String resolvePathPart(final String pathPart) {
    if (StringUtils.isBlank(pathPart)) {
      return null;
    }

    return pathPart;
  }

  private static Set<ObjectNode> filterMatchableNodes(final JsonNode bomJson) {
    Set<ObjectNode> filteredNodes = new HashSet<>();
    JsonNode aaData = bomJson.get(FIELD_AA_DATA);
    for (JsonNode bomJsonNode : aaData) {
      ObjectNode bomObjectNode = (ObjectNode) bomJsonNode;
      if (hasSha256(bomObjectNode) &&
          notProprietary(bomObjectNode) &&
          (hasMatchableStatus(bomObjectNode) &&
              isOfMatchableFileType(bomObjectNode) || isSbomComponentWithoutSupportedCoordinates(bomObjectNode)))
      {
        filteredNodes.add(bomObjectNode);
      }
    }
    return filteredNodes;
  }

  private static boolean hasSha256(final ObjectNode bomObjectNode) {
    return bomObjectNode.hasNonNull(FIELD_SHA256);
  }

  private static boolean isSbomComponentWithoutCoordinates(final ObjectNode node) {
    if (!isSbomComponent(node)) {
      return false;
    }
    return getCoordinates(node) == null;
  }

  private static boolean isSbomComponentWithoutSupportedCoordinates(final ObjectNode node) {
    if (!isSbomComponent(node)) {
      return false;
    }
    ComponentIdentifier componentIdentifier = getCoordinates(node);
    return componentIdentifier == null ||
        !ComponentIdentifier.getFormatsSupportedByHds().contains(componentIdentifier.getFormat());
  }

  private static boolean isSbomComponent(final ObjectNode bomObjectNode) {
    try {
      AnalyzerFeatures analyzerFeatures =
          JsonUtils.asPojo(bomObjectNode.get(FIELD_ANALYZER_FEATURES), AnalyzerFeatures.class);
      if (analyzerFeatures != null) {
        return analyzerFeatures.getAnalysisSource() == AnalysisSource.THIRD_PARTY &&
            "SBOM".equals(analyzerFeatures.getManifestContentType());
      }
    }
    catch (Exception e) {
      log.warn("Error getting metadata for component");
    }
    return false;
  }

  private static ComponentIdentifier getCoordinates(final ObjectNode bomObjectNode) {
    JsonNode purl = bomObjectNode.get("packageUrl");
    if (purl != null && !purl.isNull()) {
      return ComponentIdentifierAdapter.toComponentIdentifier(purl.asText());
    }
    JsonNode ci = bomObjectNode.get("componentIdentifier");
    if (ci != null && !ci.isNull()) {
      return ComponentIdentifierAdapter.getComponentIdentifier(ci);
    }
    return null;
  }

  private static boolean notProprietary(final ObjectNode bomObjectNode) {
    return !bomObjectNode.path(FIELD_PROPRIETARY).asBoolean(false);
  }

  private static boolean isOfMatchableFileType(final ObjectNode bomObjectNode) {
    return containsMatchableExtension(bomObjectNode, FIELD_FILENAMES) ||
        containsMatchableExtension(bomObjectNode, FIELD_PATHNAMES);
  }

  private static boolean containsMatchableExtension(ObjectNode bomObjectNode, String arrayFieldName) {
    JsonNode arrayFieldNode = bomObjectNode.get(arrayFieldName);
    if (arrayFieldNode != null) {
      for (JsonNode pathElement : arrayFieldNode) {
        String extension = StringUtils.lowerCase(FilenameUtils.getExtension(pathElement.asText()), Locale.ROOT);
        if (extension != null && MATCHABLE_EXTENSIONS.contains(extension)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isValidExtension(String componentExtension) {
    String extension = StringUtils.lowerCase(componentExtension, Locale.ROOT);
    return extension != null && MATCHABLE_EXTENSIONS.contains(extension);
  }

  private static boolean hasMatchableStatus(final ObjectNode bomObjectNode) {
    JsonNode matchStateNode = bomObjectNode.get(FIELD_MATCH_STATE);
    return matchStateNode != null && MATCHABLE_STATUSES.contains(matchStateNode.asText());
  }

  private static boolean hasRequiredCoordinates(ComponentIdentifier componentIdentifier) {
    try {
      componentIdentifier.ensureRequired();
      return true;
    }
    catch (InvalidComponentIdentifierException e) {
      return false;
    }
  }

  // Visible for testing
  static JsonNode convert(Object object) {
    if (object == null) {
      return NullNode.getInstance();
    }
    return JsonUtils.asTree(object);
  }

  private static void removeNodesByHash(ArrayNode arrayNode, String hash) {
    Iterator<JsonNode> iterator = arrayNode.elements();
    while (iterator.hasNext()) {
      if (hash.equals(iterator.next().path(FIELD_HASH).asText())) {
        iterator.remove();
      }
    }
  }
}
