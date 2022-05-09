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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusDTO;
import com.sonatype.insight.brain.api.v2.service.AbstractApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiArtifactoryConnectionService;
import com.sonatype.insight.brain.api.v2.service.DefaultApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.artifactory.ArtifactoryClientFactory;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResult;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.lqa.LqaFormat;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RepositoryMatcher
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryMatcher.class);

  private static final Set<String> MATCHABLE_STATUSES =
      ImmutableSet.of(MatchState.UNKNOWN.getId(), MatchState.SIMILAR.getId());

  private static final Set<String> MATCHABLE_EXTENSIONS = ImmutableSet.of("jar");

  private static final String ARTIFACTORY_API_STORAGE_PREFIX = "/artifactory/api/storage/";

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

  public static final String FIELD_DEPENDENCY_DATA_INCLUDED = "dependencyDataIncluded";

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

  public static final String FIELD_AA_DATA = "aaData";

  private static final String UNSPECIFIED_LICENSE_NAME = "Not Provided";

  private final ArtifactoryConnectionDAO artifactoryConnectionDao;

  private final ArtifactoryClientFactory artifactoryClientFactory;

  private final PasswordHandler passwordHandler;

  private final DefaultApiComponentDetailsServiceV2 defaultApiComponentDetailsServiceV2;

  private final ApiArtifactoryConnectionService artifactoryConnectionService;

  @Inject
  public RepositoryMatcher(
      final ArtifactoryConnectionDAO artifactoryConnectionDao,
      final ArtifactoryClientFactory artifactoryClientFactory,
      final ApiArtifactoryConnectionService artifactoryConnectionService,
      final PasswordHandler passwordHandler,
      final DefaultApiComponentDetailsServiceV2 defaultApiComponentDetailsServiceV2)
  {
    this.artifactoryConnectionDao = artifactoryConnectionDao;
    this.artifactoryClientFactory = artifactoryClientFactory;
    this.artifactoryConnectionService = artifactoryConnectionService;
    this.passwordHandler = passwordHandler;
    this.defaultApiComponentDetailsServiceV2 = defaultApiComponentDetailsServiceV2;
  }

  public Set<ComponentIdentifier> match(
      Application application,
      JsonNode bomJson,
      JsonNode dataJson,
      JsonNode summaryJson,
      JsonNode licensesJson,
      JsonNode securityJson)
  {
    if (!SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()) {
      return Collections.emptySet();
    }

    Set<ComponentIdentifier> result = new HashSet<>();
    try {
      long start = System.currentTimeMillis();
      Map<ComponentIdentifier, ObjectNode> sha256Matched = identify(application.getId(), bomJson);
      log.debug("performed repository matching in {} seconds with {} identified results",
          (System.currentTimeMillis() - start) / 1000, sha256Matched.size());
      start = System.currentTimeMillis();
      Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier =
          getEvaluationByIdentifier(new ArrayList<>(sha256Matched.keySet()));
      log.debug("performed component evaluation in {} seconds with {} evaluation results",
          (System.currentTimeMillis() - start) / 1000, evaluationByIdentifier.size());
      start = System.currentTimeMillis();
      result.addAll(updateJsonFiles(application, (ObjectNode) bomJson, (ObjectNode) dataJson, (ObjectNode) summaryJson,
          (ObjectNode) licensesJson, (ObjectNode) securityJson, sha256Matched, evaluationByIdentifier));
      log.debug("updated json files in {} seconds", (System.currentTimeMillis() - start) / 1000);
    }
    catch (Exception e) {
      log.error("Failed to perform Artifactory repository matching.", e);
    }
    return result;
  }

  // Visible for testing
  static Set<ComponentIdentifier> updateJsonFiles(
      Application application,
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
    Predicate<String> isProprietary = ProprietaryConfigService.createIsProprietary(application.getId());
    int unknown = 0;
    int similar = 0;
    for (Entry<ComponentIdentifier, ComponentEvaluationData> entry : evaluationByIdentifier.entrySet()) {
      try {
        ComponentIdentifier componentIdentifier = entry.getKey();
        ComponentEvaluationData evaluation = entry.getValue();
        ObjectNode bomNode = bomNodeByIdentifier.get(componentIdentifier);
        String hash = bomNode.get(FIELD_HASH).asText();
        boolean proprietary = componentIdentifier.getProprietaryCoordinates().stream().anyMatch(isProprietary);
        result.add(componentIdentifier);
        MatchState matchState = MatchState.getById(bomNode.get(FIELD_MATCH_STATE).asText());
        if (MatchState.UNKNOWN.equals(matchState)) {
          unknown++;
        }
        else if (MatchState.SIMILAR.equals(matchState)) {
          similar++;
        }
        updateBomJson(bomJson, componentIdentifier, bomNode, proprietary, evaluation);
        updateLicensesJson(licensesJson, componentIdentifier, hash, proprietary, evaluation);
        updateSecurityJson(securityJson, componentIdentifier, hash, proprietary, evaluation);
      }
      catch (Exception e) {
        log.error("Failed to update json files for component {}.", entry.getKey(), e);
      }
    }
    updateDataJson(dataJson, unknown, similar);
    updateSummaryJson(summaryJson, unknown, similar);
    return result;
  }

  // Visible for testing
  static void updateBomJson(
      ObjectNode bomJson,
      ComponentIdentifier componentIdentifier,
      ObjectNode bomNode,
      boolean proprietary,
      ComponentEvaluationData evaluation)
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
    newBomNode.set(FIELD_HASH, bomNode.get(FIELD_HASH));
    newBomNode.set(FIELD_SHA256, bomNode.get(FIELD_SHA256));
    newBomNode.put(FIELD_RELATIVE_POPULARITY, evaluation.relativePopularity);
    newBomNode.put(FIELD_CREATE_TIME, evaluation.catalogDate);
    newBomNode.set(FIELD_LAST_MODIFIED_TIME, bomNode.get(FIELD_LAST_MODIFIED_TIME));
    newBomNode.set(FIELD_LAST_MODIFIED_ENTRY_TIME, bomNode.get(FIELD_LAST_MODIFIED_ENTRY_TIME));
    newBomNode.set(FIELD_WEBSITE, bomNode.get(FIELD_WEBSITE));
    newBomNode.put(FIELD_IDENTIFICATION_SOURCE, IdentificationSource.SONATYPE.getId());
    newBomNode.set(FIELD_COMPONENT_CATEGORIES, convert(evaluation.componentCategories));
    newBomNode.set(FIELD_HYGIENE_RATING, convert(evaluation.hygieneRating));
    newBomNode.set(FIELD_ANALYZER_FEATURES, convert(createAnalyzerFeatures(componentIdentifier.getFormat(),
        bomNode.path(FIELD_ANALYZER_FEATURES).path(FIELD_SCAN_CLIENT).asText(CLI_SCAN_CLIENT))));
    newBomNode.set(FIELD_INTEGRITY_RATING, convert(evaluation.integrityRating));
    setIfNotNull(newBomNode, FIELD_DEPENDENCY_DATA_INCLUDED, bomNode.get(FIELD_DEPENDENCY_DATA_INCLUDED));
  }

  // Visible for testing
  static AnalyzerFeatures createAnalyzerFeatures(String format, String scanClient) {
    if (ComponentIdentifier.getSupportedFormats().contains(format)) {
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
      ObjectNode licensesJson,
      ComponentIdentifier componentIdentifier,
      String hash,
      boolean proprietary,
      ComponentEvaluationData evaluation)
  {
    ArrayNode licenseNodes = (ArrayNode) licensesJson.get(FIELD_AA_DATA);
    removeNodesByHash(licenseNodes, hash);
    ObjectNode licenseNode = licenseNodes.addObject();
    licenseNode.put(FIELD_HASH, hash);
    updateComponentIdentifier(licenseNode, componentIdentifier);
    Set<String> declaredLicenseIds = convert(evaluation.declaredLicenses);
    if (declaredLicenseIds.isEmpty()) {
      declaredLicenseIds.add(UNSPECIFIED_LICENSE_NAME);
    }
    licenseNode.set(FIELD_DECLARED_LICENSES, convert(declaredLicenseIds));
    Set<String> observedLicenseIds = convert(evaluation.observedLicenses);
    if (observedLicenseIds.isEmpty()) {
      observedLicenseIds.add(UNSPECIFIED_LICENSE_NAME);
    }
    licenseNode.set(FIELD_OBSERVED_LICENSES, convert(observedLicenseIds));
    Set<String> effectiveLicenseIds =
        ComponentDetailsLoader.calculateEffectiveLicenses(declaredLicenseIds, observedLicenseIds);
    licenseNode.set(FIELD_EFFECTIVE_LICENSES, convert(effectiveLicenseIds));
    licenseNode.put(FIELD_MATCH_STATE, MatchState.EXACT.getId());
    licenseNode.put(FIELD_PROPRIETARY, proprietary);
    licenseNode.put(FIELD_MATCHED_BY_COORDINATES, true);
    licenseNode.put(FIELD_CATALOG_DATE, evaluation.catalogDate);
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
      ComponentEvaluationData evaluation)
  {
    ArrayNode securityNodes = (ArrayNode) securityJson.get(FIELD_AA_DATA);
    removeNodesByHash(securityNodes, hash);
    if (evaluation.securityVulnerabilities == null) {
      return;
    }
    for (SecurityVulnerability securityVulnerability : evaluation.securityVulnerabilities) {
      ObjectNode securityVulnerabilityNode = securityNodes.addObject();
      securityVulnerabilityNode.put(FIELD_HASH, hash);
      updateComponentIdentifier(securityVulnerabilityNode, componentIdentifier);
      securityVulnerabilityNode.put(FIELD_URL, securityVulnerability.getUrl());
      securityVulnerabilityNode.put(FIELD_REFERENCE, securityVulnerability.getRefId());
      securityVulnerabilityNode.put(FIELD_SOURCE, securityVulnerability.getSource());
      securityVulnerabilityNode.put(FIELD_SCORE, securityVulnerability.getSeverity());
      securityVulnerabilityNode.put(FIELD_MATCH_STATE, MatchState.EXACT.getId());
      securityVulnerabilityNode.put(FIELD_PROPRIETARY, proprietary);
      securityVulnerabilityNode.set(FIELD_VULNERABILITY_CATEGORIES,
          convert(securityVulnerability.getVulnerabilityCategories()));
    }
  }

  // Visible for testing
  static void updateComponentIdentifier(ObjectNode objectNode, ComponentIdentifier componentIdentifier) {
    objectNode.set(ComponentIdentifierAdapter.COMPONENT_IDENTIFIER, convert(componentIdentifier));
    objectNode.put(ComponentIdentifierAdapter.PURL_IDENTIFIER,
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl());
    if (ComponentIdentifier.FORMAT_MAVEN.equals(componentIdentifier.getFormat())) {
      Report.setMavenCoordinatesWithExtension(objectNode, componentIdentifier);
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

  //visible for testing
  Map<ComponentIdentifier, ObjectNode> identify(final String applicationId, final JsonNode bomJson) {
    Map<ComponentIdentifier, ObjectNode> identifiedComponents = new HashMap<>();
    Set<ObjectNode> filteredNodes = filterMatchableNodes(bomJson);
    if (CollectionUtils.isNotEmpty(filteredNodes)) {
      ArtifactoryConnection connection = getArtifactoryConnection(applicationId);
      if (connection != null) {
        ArtifactoryClient artifactoryClient = artifactoryClientFactory.create()
            .forArtifactory(connection.getBaseUrl(), connection.getUsername(),
                passwordHandler.decryptPassword(connection.getPassword()));
        for (ObjectNode node : filteredNodes) {
          if (!matchWithRepository(identifiedComponents, connection, artifactoryClient, node)) {
            break; // avoid checksum search in case of any connection errors to repository
          }
        }
      }
    }
    return identifiedComponents;
  }

  private ArtifactoryConnection getArtifactoryConnection(String applicationId) {
    ApiArtifactoryConnectionStatusDTO statusDTO =
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
          defaultApiComponentDetailsServiceV2.getComponentDetailsListFromHds(componentIdentifiers,
              AbstractApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
      for (ComponentEvaluationData componentEvaluationData : componentDetailsListFromHds) {
        result.put(componentIdentifiers.get(componentEvaluationData.requestIndex), componentEvaluationData);
      }
    }
    catch (Exception e) {
      log.error("Unable to evaluate repository components.", e);
    }
    return result;
  }

  private static boolean matchWithRepository(
      final Map<ComponentIdentifier, ObjectNode> identifiedComponents,
      final ArtifactoryConnection rootConnection,
      final ArtifactoryClient artifactoryClient,
      final ObjectNode node)
  {
    String sha256 = node.get(FIELD_SHA256).asText();
    try {
      ArtifactoryChecksumSearchResults artifactoryChecksumSearchResults =
          artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);
      if (CollectionUtils.isNotEmpty(artifactoryChecksumSearchResults.results)) {
        ComponentIdentifier resolvedId = resolveComponentIdentifier(artifactoryChecksumSearchResults);
        if (resolvedId != null) {
          identifiedComponents.put(resolvedId, node);
        }
        else {
          log.debug("no recognizable artifact found in repository for sha256={}", sha256);
        }
      }
      return true;
    }
    catch (IOException e) {
      log.error("Checksum search error for repository connection uri {}", rootConnection.getBaseUrl(), e);
      return false;
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
    }
    return null;
  }

  //visible for testing
  static ComponentIdentifier resolveComponentIdentifierFromUri(final String uriString) {
    if (StringUtils.isBlank(uriString)) {
      return null;
    }

    try {
      URI uri = new URI(uriString);
      String[] pathParts = StringUtils.split(
          StringUtils.removeStart(uri.getPath(), ARTIFACTORY_API_STORAGE_PREFIX), "/");
      // We expect at least ARTIFACTORY_API_STORAGE_PREFIX + [repo]/[group]/[artifact]/[version]/[filename.extension]
      if (pathParts.length >= 5) {
        String extension = resolveExtension(pathParts[pathParts.length - 1]);
        pathParts = ArrayUtils.removeAll(pathParts, 0, pathParts.length - 1); // remove repository and filename
        String version = resolvePathPart(pathParts[pathParts.length - 1]);
        String name = resolvePathPart(pathParts[pathParts.length - 2]);
        String namespace = resolvePathPart(StringUtils.join(
            ArrayUtils.removeAll(pathParts, pathParts.length - 1, pathParts.length - 2), "."));
        return ComponentIdentifier.createMavenCoordinates(namespace, name, version, null, extension);
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
          hasMatchableStatus(bomObjectNode) &&
          isOfMatchableFileType(bomObjectNode) &&
          notProprietary(bomObjectNode)) {
        filteredNodes.add(bomObjectNode);
      }
    }
    return filteredNodes;
  }

  private static boolean hasSha256(final ObjectNode bomObjectNode) {
    return bomObjectNode.hasNonNull(FIELD_SHA256);
  }

  private static boolean notProprietary(final ObjectNode bomObjectNode) {
    JsonNode proprietaryNode = bomObjectNode.get(FIELD_PROPRIETARY);
    return proprietaryNode != null && !proprietaryNode.asBoolean(false);
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

  private static void setIfNotNull(ObjectNode objectNode, String name, JsonNode value) {
    if (value != null) {
      objectNode.set(name, value);
    }
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
