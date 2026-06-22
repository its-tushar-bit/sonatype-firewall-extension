/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceVersionDAO;
import com.sonatype.insight.brain.innersource.InnerSourceConsumerTelemetry;
import com.sonatype.insight.brain.innersource.InnerSourceProducerComponentTelemetry;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.innersource.InnerSourceVersion;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.utils.ComponentDependencyUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.dependency.DependencyNode;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.util.HashUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.InnerSourceUtils.getPackageUrl;

/**
 * @since 1.112
 */
public class DependencyResolver
{
  private static final Logger log = LoggerFactory.getLogger(DependencyResolver.class);

  private static final String EXACTLY_MATCHED_COMPONENT_COUNT = "exactlyMatchedComponentCount";

  private static final String KNOWN_ARTIFACT_COUNT = "knownArtifactCount";

  private static final String TOTAL_ARTIFACT_COUNT = "totalArtifactCount";

  public static final String MATCH_STATE = "matchState";

  private static final String FIELD_DIRECT_DEPENDENCY = "directDependency";

  private static final String FIELD_INNER_SOURCE = "innerSource";

  private static final String FIELD_COMPONENT_IDENTIFIER = "componentIdentifier";

  private static final String FIELD_PACKAGE_URL = "packageUrl";

  public static final String AA_DATA_NODE = "aaData";

  public static final String FIELD_ANALYZER_FEATURES = "analyzerFeatures";

  public static final String FIELD_DEPENDENCY_INDICATOR = "dependencyDataIncluded";

  private final String stageTypeId;

  private final JsonNode dependenciesJson;

  private final JsonNode bomJson;

  private final JsonNode dataJson;

  private final JsonNode summaryJson;

  private final Application application;

  // Visible for testing
  Predicate<String> isProprietary;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  private final InnerSourceApplicationDAO innerSourceApplicationDAO;

  private final InnerSourceVersionDAO innerSourceVersionDAO;

  private final ApplicationDAO applicationDAO;

  private final ProprietaryConfigService proprietaryConfigService;

  private AtomicInteger knownArtifactCount = new AtomicInteger();

  private AtomicInteger totalArtifactCount = new AtomicInteger();

  private AtomicInteger exactlyMatchedComponentCount = new AtomicInteger();

  private Map<String, ObjectNode> bomNodesByPackageUrl;

  private Map<String, InnerSourceApplication> innerSourceApplicationsByPackageUrl = Collections.emptyMap();

  private final Set<InnerSourceProducerComponentTelemetry> innerSourceProducerTelemetries = new HashSet<>();

  public static DependencyResolver getInstance(
      JsonNode dependenciesJson,
      JsonNode bomJson,
      JsonNode dataJson,
      JsonNode summaryJson,
      String stageTypeId,
      Application application,
      TelemetrySender telemetrySender,
      TelemetryUtils telemetryUtils,
      InnerSourceApplicationDAO innerSourceApplicationDAO,
      InnerSourceVersionDAO innerSourceVersionDAO,
      ApplicationDAO applicationDAO,
      ProprietaryConfigService proprietaryConfigService)
  {
    return new DependencyResolver(dependenciesJson, bomJson, dataJson, summaryJson, stageTypeId, application,
        telemetrySender, telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO,
        proprietaryConfigService);
  }

  // visible for testing
  DependencyResolver(
      final JsonNode dependenciesJson,
      final JsonNode bomJson,
      final JsonNode dataJson,
      final JsonNode summaryJson,
      final String stageTypeId,
      final Application application,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      final InnerSourceApplicationDAO innerSourceApplicationDAO,
      final InnerSourceVersionDAO innerSourceVersionDAO,
      final ApplicationDAO applicationDAO,
      final ProprietaryConfigService proprietaryConfigService)
  {
    this.stageTypeId = stageTypeId;
    this.dependenciesJson = dependenciesJson;
    this.bomJson = bomJson;
    this.dataJson = dataJson;
    this.summaryJson = summaryJson;
    this.application = application;
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
    this.innerSourceApplicationDAO = innerSourceApplicationDAO;
    this.innerSourceVersionDAO = innerSourceVersionDAO;
    this.applicationDAO = applicationDAO;
    this.proprietaryConfigService = proprietaryConfigService;
    if (summaryJson != null) {
      knownArtifactCount = new AtomicInteger(summaryJson.path(KNOWN_ARTIFACT_COUNT).asInt());
      totalArtifactCount = new AtomicInteger(summaryJson.path(TOTAL_ARTIFACT_COUNT).asInt());
      exactlyMatchedComponentCount = new AtomicInteger(dataJson.path(EXACTLY_MATCHED_COMPONENT_COUNT).asInt());
    }
  }

  void resolve() throws IOException {
    if (dependenciesJson != null) {
      JsonNode dependencyTreeNode = dependenciesJson.path("dependencyTree");

      if (!dependencyTreeNode.isMissingNode()) {
        boolean dependencyDataExists = false;
        DependencyNode tree = JsonUtils.asPojo(dependencyTreeNode, DependencyNode.class);
        if (tree != null) {
          PackageUrlIdentifier rootPurl = getPackageUrl(tree);
          if (rootPurl != null) {
            boolean isValidRootArtifact = saveInnerSourceComponent(rootPurl);
            if (isValidRootArtifact) {
              dependencyDataExists = true;
              processInnerSourceDependencies(tree.getChildren());
            }
          }
          // no root ComponentIdentifier refers to a tree derived based on HDS data
          // or SBOM File where the parent component does not have a purl
          else if (CollectionUtils.isNotEmpty(tree.getChildren())) {
            dependencyDataExists = true;
            updateDependencyInfoForComponentChildren(tree.getChildren(), true, false, null, false);
          }
          addBomDependencyDataIndicator(bomJson, dependencyDataExists);
        }
      }
    }
  }

  private void addBomDependencyDataIndicator(JsonNode bomJson, boolean dependencyDataExists) {
    ObjectNode bomNode = (ObjectNode) bomJson;
    bomNode.put(FIELD_DEPENDENCY_INDICATOR, dependencyDataExists);
  }

  // visible for testing
  boolean saveInnerSourceComponent(final PackageUrlIdentifier packageUrl) {
    if (packageUrl == null) {
      return false;
    }

    String appId = application.getId();
    String version = packageUrl.getVersion();
    PackageUrlIdentifier versionlessPurl = packageUrl.createAlternativeVersion(null);

    if (versionlessPurl == null) {
      return false;
    }

    // Single lookup is intentional here: this is the write path, invoked once per module/root artifact, so the
    // number of lookups is bounded by module count. The read/association path resolves from the batched map instead.
    InnerSourceApplication innerSourceApplication = innerSourceApplicationDAO.getByPackageUrl(versionlessPurl);
    try (TransactionContext tx = innerSourceApplicationDAO.createTransactionContext()) {
      tx.begin();
      if (innerSourceApplication != null) {
        processExistingInnerSourceComponent(tx, innerSourceApplication, version, appId, packageUrl.getFormat());
        tx.commit();
        return true;
      }
      innerSourceApplication = new InnerSourceApplication();
      innerSourceApplication.setApplicationId(appId);
      innerSourceApplication.setPackageUrl(versionlessPurl.getPackageUrl());
      innerSourceApplicationDAO.insert(tx, innerSourceApplication);

      if (version != null) {
        InnerSourceVersion newInnerSourceVersion = new InnerSourceVersion();
        newInnerSourceVersion.setInnerSourceApplicationId(innerSourceApplication.getId());
        newInnerSourceVersion.setLatestVersion(version);
        newInnerSourceVersion.setStageTypeId(stageTypeId);
        innerSourceVersionDAO.insert(tx, newInnerSourceVersion);
      }
      tx.commit();
    }

    log.info("InnerSource application {} with latest version {} for app {} and stage {} was created",
        innerSourceApplication.getPackageUrl(), version, appId, stageTypeId);
    return true;
  }

  private void processExistingInnerSourceComponent(
      final TransactionContext tx,
      final InnerSourceApplication innerSourceApplication,
      final String version,
      final String appId,
      final String format)
  {
    boolean isNewerVersion = true;

    if (!appId.equals(innerSourceApplication.getApplicationId())) {
      innerSourceApplication.setApplicationId(appId);
      innerSourceApplicationDAO.update(tx, innerSourceApplication);
    }

    InnerSourceVersion innerSourceVersion =
        innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplication.getId(), stageTypeId);

    if (innerSourceVersion == null) {
      innerSourceVersion =
          innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplication.getId(), null);
    }

    if (innerSourceVersion != null) {
      isNewerVersion = isNewerVersion(innerSourceVersion.getLatestVersion(), version, format);
      if (isNewerVersion) {
        innerSourceVersion.setLatestVersion(version);
      }
      innerSourceVersion.setStageTypeId(stageTypeId);
      innerSourceVersionDAO.update(tx, innerSourceVersion);
    }
    else if (version != null) {
      InnerSourceVersion newInnerSourceVersion = new InnerSourceVersion();
      newInnerSourceVersion.setInnerSourceApplicationId(innerSourceApplication.getId());
      newInnerSourceVersion.setLatestVersion(version);
      newInnerSourceVersion.setStageTypeId(stageTypeId);
      innerSourceVersionDAO.insert(tx, newInnerSourceVersion);
    }

    String updatedVersion = isNewerVersion ? version : innerSourceVersion.getLatestVersion();
    log.info("InnerSource application {} with latest version {} for app {} and stage {} was updated",
        innerSourceApplication.getPackageUrl(), updatedVersion, appId, stageTypeId);
  }

  private boolean isNewerVersion(String oldVersion, String newVersion, String format) {
    CompositeComparableVersion oldComparableVersion =
        InnerSourceUtils.createCompositeComparableVersion(oldVersion, format);
    CompositeComparableVersion newComparableVersion =
        InnerSourceUtils.createCompositeComparableVersion(newVersion, format);
    return newComparableVersion.compareTo(oldComparableVersion) > 0;
  }

  void processInnerSourceDependencies(final List<DependencyNode> children) {
    if (!children.isEmpty()) {

      Set<PackageUrlIdentifier> modules = getModuleDependencies(children);
      Set<PackageUrlIdentifier> directDependencies = getDirectDependencies(children);
      Set<String> processedDirectDependencies = new HashSet<>();
      // Loaded once here, before module processing. associateModuleToApp may insert a new InnerSourceApplication via
      // saveInnerSourceComponent, but the map is intentionally not refreshed afterwards: it reflects associations as
      // of scan start, so a transitive is only marked known if its association already existed then. A module
      // association first written during this scan is resolved on the next scan rather than retroactively mid-scan.
      innerSourceApplicationsByPackageUrl = loadInnerSourceApplications(children);

      for (DependencyNode dependencyChild : children) {
        if (dependencyChild.isModule()) {
          associateModuleToApp(dependencyChild, directDependencies, processedDirectDependencies, modules);
        }
        else if (dependencyChild.isDirect()) {
          processDirectDependency(dependencyChild, directDependencies, processedDirectDependencies, modules);
        }
      }
      updateReportSummaryWithInnerSourceResults(dataJson, summaryJson);
      sendTelemetryData();
    }
  }

  private Map<String, InnerSourceApplication> loadInnerSourceApplications(final List<DependencyNode> children) {
    // CLM-39951 / CLM-40956: batch-load every InnerSource association in the dependency tree (direct, module
    // and transitive components) in a single query, so the association lookups stay at one query per scan
    // regardless of component count. package_url is unique in inner_source_application, so each purl resolves
    // to exactly one association.
    Set<PackageUrlIdentifier> packageUrls = collectAllPackageUrls(children);
    if (packageUrls.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, InnerSourceApplication> result = new HashMap<>();
    for (InnerSourceApplication innerSourceApplication : innerSourceApplicationDAO.getByPackageUrls(packageUrls)) {
      result.put(innerSourceApplication.getPackageUrl(), innerSourceApplication);
    }
    return result;
  }

  private Set<PackageUrlIdentifier> collectAllPackageUrls(final List<DependencyNode> children) {
    Set<PackageUrlIdentifier> packageUrls = new HashSet<>();
    collectPackageUrls(children, packageUrls);
    return packageUrls;
  }

  private void collectPackageUrls(final List<DependencyNode> nodes, final Set<PackageUrlIdentifier> packageUrls) {
    for (DependencyNode node : nodes) {
      addSimplifiedPackageUrl(packageUrls, node);
      if (CollectionUtils.isNotEmpty(node.getChildren())) {
        collectPackageUrls(node.getChildren(), packageUrls);
      }
    }
  }

  private void addSimplifiedPackageUrl(final Set<PackageUrlIdentifier> packageUrls, final DependencyNode node) {
    PackageUrlIdentifier packageUrl = getPackageUrl(node);
    if (packageUrl != null) {
      PackageUrlIdentifier simplifiedPurl = packageUrl.createAlternativeVersion(null);
      if (simplifiedPurl != null) {
        packageUrls.add(simplifiedPurl);
      }
    }
  }

  private InnerSourceApplication getInnerSourceApplicationExcludingApplication(
      final PackageUrlIdentifier simplifiedPurl)
  {
    InnerSourceApplication innerSourceApplication =
        innerSourceApplicationsByPackageUrl.get(simplifiedPurl.getPackageUrl());
    if (innerSourceApplication == null || application.getId().equals(innerSourceApplication.getApplicationId())) {
      return null;
    }
    return innerSourceApplication;
  }

  private void associateModuleToApp(
      final DependencyNode moduleDependency,
      final Set<PackageUrlIdentifier> directDependencies,
      final Set<String> processedDirectDependencies,
      final Set<PackageUrlIdentifier> modules)
  {
    PackageUrlIdentifier modulePurl = getPackageUrl(moduleDependency);
    if (modulePurl != null) {
      log.debug("InnerSource module '{}' found", modulePurl);
      saveInnerSourceComponent(modulePurl);

      updateBomNodeDependencyInformation(true, false, modulePurl, null, true);

      for (DependencyNode directDependencyChild : moduleDependency.getChildren()) {
        processDirectDependency(directDependencyChild, directDependencies, processedDirectDependencies, modules);
      }
    }
  }

  private void processDirectDependency(
      final DependencyNode directDependency,
      final Set<PackageUrlIdentifier> directDependencies,
      final Set<String> processedDirectDependencies,
      final Set<PackageUrlIdentifier> modules)
  {
    PackageUrlIdentifier packageUrlIdentifier = getPackageUrl(directDependency);

    if (packageUrlIdentifier != null) {
      PackageUrlIdentifier simplifiedPurl = packageUrlIdentifier.createAlternativeVersion(null);
      // Only InnerSource components that are different from the current app,
      // if they are the same app it means they are likely modules
      InnerSourceApplication innerSourceApplication = simplifiedPurl == null
          ? null
          : getInnerSourceApplicationExcludingApplication(simplifiedPurl);

      if (innerSourceApplication != null) {
        Application innerSourceApp = applicationDAO.getByIdNotNull(innerSourceApplication.getApplicationId());

        if (!processedDirectDependencies.contains(packageUrlIdentifier.getPackageUrl())) {
          updateDependencyBomAsInnerSource(packageUrlIdentifier, innerSourceApp);
          processedDirectDependencies.add(packageUrlIdentifier.getPackageUrl());
        }
        processInnerSourceTransitiveDependencies(directDependency.getChildren(), directDependency, innerSourceApp,
            getPackageUrl(directDependency), directDependencies);
      }
      else {
        Optional<Collection<ObjectNode>> bomNode = findBomComponent(packageUrlIdentifier);
        // Only modules need to be created
        if (bomNode.isEmpty()) {
          if (directDependency.isModule()) {
            ObjectNode node = newNodeComponent(packageUrlIdentifier);
            bomNodesByPackageUrl.put(packageUrlIdentifier.getPackageUrl(), node);
          }
          else {
            log.debug("Dependency with purl '{}' exists in the tree but no the bom.json, it was not created",
                packageUrlIdentifier.getPackageUrl());
          }
        }

        boolean markAsKnown = directDependency.isModule() || modules.contains(packageUrlIdentifier);

        // a regular (non InnerSource) dependency/module
        updateBomNodeDependencyInformation(true, false, packageUrlIdentifier, null, markAsKnown);
        updateDependencyInfoForComponentChildren(directDependency.getChildren(), false, false, packageUrlIdentifier,
            markAsKnown);
      }
    }
  }

  private void processInnerSourceTransitiveDependencies(
      final List<DependencyNode> transitiveDependencies,
      final DependencyNode innerSourceParent,
      final Application innerSourceApp,
      final PackageUrlIdentifier parentPurl,
      final Set<PackageUrlIdentifier> directDependencies)
  {
    for (DependencyNode dependency : transitiveDependencies) {
      PackageUrlIdentifier dependencyPurl = getPackageUrl(dependency);
      Optional<Collection<ObjectNode>> bomObjectNodeOptional = findBomComponent(dependencyPurl);

      if (bomObjectNodeOptional.isPresent() && dependencyPurl != null && !directDependencies.contains(dependencyPurl)) {
        for (ObjectNode bomObjectNode : bomObjectNodeOptional.get()) {
          InnerSourceData innerSourceData = new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(),
              PackageUrlIdentifier.toPackageUrl(innerSourceParent.getComponentIdentifier()));
          updateBomNodeDependencyInformation(false, false, dependencyPurl, parentPurl, innerSourceData, null, true);
          log.debug("Component {} associated with InnerSource app {}", dependencyPurl, innerSourceApp.getName());

          if (MatchState.UNKNOWN.getId().equals(bomObjectNode.get(MATCH_STATE).asText())) {
            updateUnknownTransitiveDependencyAsKnown(dependencyPurl, bomObjectNode);
          }
        }
      }

      if (CollectionUtils.isNotEmpty(dependency.getChildren())) {
        processInnerSourceTransitiveDependencies(dependency.getChildren(), innerSourceParent, innerSourceApp,
            dependencyPurl, directDependencies);
      }
    }
  }

  private void updateDependencyInfoForComponentChildren(
      final List<DependencyNode> children,
      final boolean isDirect,
      final boolean isInnerSource,
      final PackageUrlIdentifier parentPurl,
      final boolean markAsKnown)
  {
    for (DependencyNode node : children) {
      if (node != null && node.getComponentIdentifier() != null) {
        PackageUrlIdentifier purl = getPackageUrl(node);
        updateBomNodeDependencyInformation(isDirect, isInnerSource, purl, parentPurl, markAsKnown);
        if (CollectionUtils.isNotEmpty(node.getChildren())) {
          updateDependencyInfoForComponentChildren(node.getChildren(), false, isInnerSource, purl, markAsKnown);
        }
      }
    }
  }

  private void updateReportSummaryWithInnerSourceResults(
      final JsonNode dataJson,
      final JsonNode summaryJson)
  {
    ObjectNode dataObjectNode = (ObjectNode) dataJson;
    dataObjectNode.put(EXACTLY_MATCHED_COMPONENT_COUNT, exactlyMatchedComponentCount.intValue());
    dataObjectNode.put(KNOWN_ARTIFACT_COUNT, knownArtifactCount.intValue());
    dataObjectNode.put(TOTAL_ARTIFACT_COUNT, totalArtifactCount.intValue());

    ((ObjectNode) summaryJson).put(KNOWN_ARTIFACT_COUNT, knownArtifactCount.intValue());
    ((ObjectNode) summaryJson).put(TOTAL_ARTIFACT_COUNT, totalArtifactCount.intValue());
  }

  private void updateDependencyBomAsInnerSource(
      final PackageUrlIdentifier innerSourcePackageUrlIdentifier,
      final Application innerSourceApp)
  {
    InnerSourceProducerComponentTelemetry producerInfo = new InnerSourceProducerComponentTelemetry();
    producerInfo.setFormat(innerSourcePackageUrlIdentifier.getFormat());
    producerInfo.setProducerAppId(innerSourceApp.getId());
    InnerSourceData innerSourceData = new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(), null);

    // In some scenarios (during MJA matching) the component isn't identified and not present in the bom.json.
    // However, we are certain at this point this is an IS component so add a new identified component here
    if (findBomComponent(innerSourcePackageUrlIdentifier).isEmpty()) {
      ObjectNode isNode = newNodeComponent(innerSourcePackageUrlIdentifier);

      bomNodesByPackageUrl.put(innerSourcePackageUrlIdentifier.getPackageUrl(), isNode);
      totalArtifactCount.getAndIncrement();

      log.debug("InnerSource Component '{}' was created in bom.json", innerSourcePackageUrlIdentifier.getPackageUrl());
    }
    updateBomNodeDependencyInformation(true, true, innerSourcePackageUrlIdentifier, null, innerSourceData, producerInfo,
        true);
    innerSourceProducerTelemetries.add(producerInfo);
  }

  private ObjectNode newNodeComponent(PackageUrlIdentifier componentPurl) {
    ArrayNode aaNode = (ArrayNode) bomJson.get(AA_DATA_NODE);
    ObjectNode isNode = aaNode.addObject();
    isNode.put("hash", getHash(componentPurl));
    isNode.put("proprietary", isProprietaryComponent(componentPurl));
    isNode.set(FIELD_ANALYZER_FEATURES, JsonUtils.asTree(getAnalyzerFeaturesForNewNode(aaNode)));
    isNode.set("createTime", NullNode.getInstance());
    isNode.put("relativePopularity", 0);
    String packageUrlString = componentPurl.getPackageUrl();
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode filenames = objectMapper.createArrayNode();
    filenames.add(packageUrlString);
    isNode.set("filenames", filenames);
    ArrayNode pathnames = objectMapper.createArrayNode();
    pathnames.add(ComponentDependencyUtils.getDependencyStringFromPackageUrlIdentifier(packageUrlString));
    isNode.set("pathnames", pathnames);

    totalArtifactCount.getAndIncrement();

    return isNode;
  }

  // Visible for testing
  boolean isProprietaryComponent(PackageUrlIdentifier componentPurl) {
    if (isProprietary == null) {
      isProprietary = proprietaryConfigService.createIsProprietary(application.getId());
    }
    return componentPurl.toComponentIdentifier().getProprietaryCoordinates().stream().anyMatch(isProprietary);
  }

  private String getHash(PackageUrlIdentifier componentPurl) {
    return HashHelper.truncateHash(HashUtils.hash(componentPurl.getPackageUrl(), HashUtils.SHA1));
  }

  private AnalyzerFeatures getAnalyzerFeaturesForNewNode(JsonNode aaNode) {
    // it's extremely unlikely to have bom.json with 0 components (identified+unknown combined)
    // and having an InnerSource coordinate in dependencies.json. Setting to "cli"
    // in such a rare case where in all other cases it will adopt a sibling's scanClient.
    String scanClient = "cli";
    if (!aaNode.isEmpty() && aaNode.get(0).hasNonNull(FIELD_ANALYZER_FEATURES)) {
      scanClient = aaNode.get(0).get(FIELD_ANALYZER_FEATURES).get("scanClient").asText();
    }
    return new AnalyzerFeatures(AnalysisSource.THIRD_PARTY,
        AnalysisType.COORDINATE, scanClient, false, false, false);
  }

  private Optional<Collection<ObjectNode>> findBomComponent(PackageUrlIdentifier identifier) {
    loadBomComponentsIfNotLoaded();
    Collection<ObjectNode> result = new ArrayList<>();
    for (Entry<String, ObjectNode> bomPurl : bomNodesByPackageUrl.entrySet()) {
      if (bomPurl.getKey().startsWith(identifier.getPackageUrl())) {
        result.add(bomPurl.getValue());
      }
    }
    if (result.isEmpty()) {
      return Optional.empty();
    }
    else {
      return Optional.of(result);
    }
  }

  private void loadBomComponentsIfNotLoaded() {
    if (bomNodesByPackageUrl == null) {
      bomNodesByPackageUrl = new HashMap<>();
      for (JsonNode bomChild : bomJson.get(AA_DATA_NODE)) {
        PackageUrlIdentifier purl = ComponentIdentifierAdapter.getPackageUrlIdentifier(bomChild);
        if (purl != null) {
          bomNodesByPackageUrl.put(purl.getPackageUrl(), (ObjectNode) bomChild);
        }
        String matchStateString = bomChild.path(MATCH_STATE).asText();
        MatchState matchState = MatchState.getById(matchStateString);
        if (matchState == MatchState.SIMILAR) {
          JsonNode pathnames = bomChild.get("pathnames");
          if (pathnames == null || pathnames.isEmpty()) {
            continue;
          }
          purl = ComponentIdentifierAdapter.parsePathToId(pathnames.get(0).asText());
          if (purl != null) {
            bomNodesByPackageUrl.put(purl.getPackageUrl(), (ObjectNode) bomChild);
          }
        }
      }
    }
  }

  private void markComponentAsKnown(
      final ObjectNode bomObjectNode,
      final PackageUrlIdentifier componentPurl,
      final boolean isDirect)
  {
    knownArtifactCount.getAndIncrement();
    exactlyMatchedComponentCount.getAndIncrement();

    bomObjectNode.put(MATCH_STATE, MatchState.EXACT.getId());
    bomObjectNode.put("identificationSource", IdentificationSource.PACKAGE_MANIFEST.getId());

    bomObjectNode.set(FIELD_COMPONENT_IDENTIFIER, JsonUtils.asTree(getComponentIdentifier(componentPurl)));
    bomObjectNode.put(FIELD_PACKAGE_URL, componentPurl.getPackageUrl());

    ComponentDisplayNameUtil.injectDisplayName(bomObjectNode);

    if (!bomObjectNode.hasNonNull(FIELD_DIRECT_DEPENDENCY)) {
      bomObjectNode.put(FIELD_DIRECT_DEPENDENCY, isDirect);
    }

    JsonNode analyzerFeatures = bomObjectNode.get(FIELD_ANALYZER_FEATURES);
    JsonNode manifestContentType = analyzerFeatures.get("manifestContentType");
    JsonNode scanType = analyzerFeatures.get("analysisType");

    bomObjectNode.set(FIELD_ANALYZER_FEATURES, JsonUtils.asTree(new AnalyzerFeatures(AnalysisSource.THIRD_PARTY,
        scanType != null ? AnalysisType.valueOf(scanType.asText()) : AnalysisType.COORDINATE,
        analyzerFeatures.get("scanClient").asText(),
        manifestContentType != null ? manifestContentType.asText() : null)));
  }

  private ComponentIdentifier getComponentIdentifier(PackageUrlIdentifier purl) {
    ComponentIdentifier ci = purl.toComponentIdentifier();
    ci.ensureComplete();
    return ci;
  }

  private void updateBomNodeDependencyInformation(
      final boolean isDirect,
      final boolean isInnerSource,
      final PackageUrlIdentifier componentPurl,
      final PackageUrlIdentifier parentComponentPurl,
      final boolean markAsKnown)
  {
    updateBomNodeDependencyInformation(isDirect, isInnerSource, componentPurl, parentComponentPurl, null,
        null, markAsKnown);
  }

  private void updateBomNodeDependencyInformation(
      final boolean isDirect,
      final boolean isInnerSource,
      final PackageUrlIdentifier componentPurl,
      final PackageUrlIdentifier parentComponentPurl,
      final InnerSourceData innerSourceData,
      final InnerSourceProducerComponentTelemetry producerInfo,
      final boolean markAsKnown)
  {
    findBomComponent(componentPurl)
        .ifPresent(bomObjectNode -> processBomNode(isDirect, isInnerSource, componentPurl, parentComponentPurl,
            innerSourceData, producerInfo, bomObjectNode, markAsKnown));
  }

  private void processBomNode(
      final boolean isDirect,
      final boolean isInnerSource,
      final PackageUrlIdentifier componentPurl,
      final PackageUrlIdentifier parentComponentPurl,
      final InnerSourceData innerSourceData,
      final InnerSourceProducerComponentTelemetry producerInfo,
      final Collection<ObjectNode> bomObjectNodes,
      final boolean markAsKnown)
  {
    for (ObjectNode bomObjectNode : bomObjectNodes) {
      if (!bomObjectNode.hasNonNull(FIELD_COMPONENT_IDENTIFIER)) {
        bomObjectNode.set(FIELD_COMPONENT_IDENTIFIER,
            new ObjectMapper().valueToTree(getComponentIdentifier(componentPurl)));
      }
      if (!bomObjectNode.hasNonNull(FIELD_PACKAGE_URL)) {
        bomObjectNode.put(FIELD_PACKAGE_URL, componentPurl.getPackageUrl());
      }
      bomObjectNode
          .put(FIELD_DIRECT_DEPENDENCY, bomObjectNode.path(FIELD_DIRECT_DEPENDENCY).asBoolean(false) || isDirect);
      bomObjectNode
          .put(FIELD_INNER_SOURCE, bomObjectNode.path(FIELD_INNER_SOURCE).asBoolean(false) || isInnerSource);
      if (!isDirect && parentComponentPurl != null) {
        if (bomObjectNode.path(ComponentLoader.PARENT_COMPONENT_PURLS_FIELD).isMissingNode()) {
          bomObjectNode.putArray(ComponentLoader.PARENT_COMPONENT_PURLS_FIELD);
        }
        ArrayNode parentComponentPurls = (ArrayNode) bomObjectNode.get(ComponentLoader.PARENT_COMPONENT_PURLS_FIELD);
        if (!contains(parentComponentPurls, parentComponentPurl)) {
          parentComponentPurls.add(parentComponentPurl.getPackageUrl());
        }
      }
      if (innerSourceData != null) {
        if (bomObjectNode.path(ComponentLoader.INNER_SOURCE_DATA_FIELD).isMissingNode()) {
          bomObjectNode.putArray(ComponentLoader.INNER_SOURCE_DATA_FIELD);
        }
        ArrayNode innerSourceDataArray = (ArrayNode) bomObjectNode.get(ComponentLoader.INNER_SOURCE_DATA_FIELD);
        if (!contains(innerSourceDataArray, innerSourceData)) {
          innerSourceDataArray.add(JsonUtils.asTree(innerSourceData));
        }
      }

      // Unknown components resulted from a binary scan (maven or other tools) that are not InnerSource
      // or modules should not be updated as "known"
      if (markAsKnown && (!bomObjectNode.hasNonNull(MATCH_STATE) ||
          MatchState.UNKNOWN.getId().equals(bomObjectNode.get(MATCH_STATE).asText())))
      {
        markComponentAsKnown(bomObjectNode, componentPurl, isDirect);
        log.debug((innerSourceData != null ? "InnerSource component" : "Component")
            + "'{}' was updated in bom.json as a known component", componentPurl);
      }

      setTelemetryInfo(producerInfo, bomObjectNode);
    }
  }

  private void setTelemetryInfo(
      final InnerSourceProducerComponentTelemetry producerInfo,
      final ObjectNode bomObjectNode)
  {
    try {
      if (producerInfo != null && bomObjectNode.hasNonNull(FIELD_ANALYZER_FEATURES)) {
        AnalyzerFeatures analyzerFeatures =
            JsonUtils.asPojo(bomObjectNode.get(FIELD_ANALYZER_FEATURES), AnalyzerFeatures.class);
        producerInfo.setScanClient(analyzerFeatures.getScanClient());
        producerInfo.setScanType(analyzerFeatures.getAnalysisType().name());
        producerInfo.setManifestContentType(analyzerFeatures.getManifestContentType());
      }
    }
    catch (Exception e) {
      log.warn("Error getting information for bom node", e);
    }
  }

  private boolean contains(ArrayNode parentComponentPurls, PackageUrlIdentifier packageUrlIdentifier) {
    for (JsonNode parentComponentPurl : parentComponentPurls) {
      if (new PackageUrlIdentifier(parentComponentPurl.asText()).equals(packageUrlIdentifier)) {
        return true;
      }
    }
    return false;
  }

  private boolean contains(ArrayNode innerSourceDataArray, InnerSourceData innerSourceData) {
    for (JsonNode innerSourceNode : innerSourceDataArray) {
      try {
        if (innerSourceData.equals(JsonUtils.asPojo(innerSourceNode, InnerSourceData.class))) {
          return true;
        }
      }
      catch (IOException e) {
        log.debug("Failed to parse InnerSource data " + innerSourceNode, e);
      }
    }
    return false;
  }

  private void updateUnknownTransitiveDependencyAsKnown(
      final PackageUrlIdentifier bomPurl,
      final ObjectNode bomObjectNode)
  {
    PackageUrlIdentifier versionlessPurl = bomPurl.createAlternativeVersion(null);
    if (versionlessPurl == null) {
      return;
    }
    // CLM-40956: resolve from the in-memory batch map populated in processInnerSourceDependencies, so each transitive
    // is a map lookup rather than a per-component database query.
    // No same-app exclusion is applied here (unlike the direct path's getInnerSourceApplicationExcludingApplication):
    // a same-app transitive InnerSource is still marked known. The map reflects associations that existed at scan
    // start; one first written during this scan via saveInnerSourceComponent is resolved on the next scan rather than
    // retroactively mid-scan.
    InnerSourceApplication is = innerSourceApplicationsByPackageUrl.get(versionlessPurl.getPackageUrl());
    if (is != null) {
      // If the component is transitive and exists as InnerSource, it needs to be updated, so it can be marked as
      // Transitive dependency but not as InnerSource
      markComponentAsKnown(bomObjectNode, bomPurl, false);
      log.debug("InnerSource module {} was updated in bom.json as Transitive InnerSource", bomPurl);
    }
  }

  private Set<PackageUrlIdentifier> getDirectDependencies(List<DependencyNode> children) {
    Set<PackageUrlIdentifier> directDependencies = new HashSet<>();
    for (DependencyNode child : children) {
      if (!child.isModule() && child.isDirect()) {
        directDependencies.add(getPackageUrl(child));
      }

      for (DependencyNode firstLevel : child.getChildren()) {
        if (firstLevel.isDirect()) {
          directDependencies.add(getPackageUrl(firstLevel));
        }
      }
    }
    return directDependencies;
  }

  private Set<PackageUrlIdentifier> getModuleDependencies(List<DependencyNode> children) {
    Set<PackageUrlIdentifier> modules = new HashSet<>();
    for (DependencyNode child : children) {
      if (child.isModule()) {
        modules.add(getPackageUrl(child));
      }
    }
    return modules;
  }

  private TelemetryData buildInnerSourceTelemetryData(
      final String applicationId,
      final Set<InnerSourceProducerComponentTelemetry> producers)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.INNER_SOURCE_REPORT_USAGE);
    telemetryData.put(InnerSourceConsumerTelemetry.ATTRIBUTE_NAME,
        new InnerSourceConsumerTelemetry(applicationId,
            telemetryUtils.obfuscateIfAdvancedReportingDisabled(applicationId), producers));
    return telemetryData;
  }

  private void sendTelemetryData() {
    if (!innerSourceProducerTelemetries.isEmpty()) {
      telemetrySender.send(
          buildInnerSourceTelemetryData(application.getId(), innerSourceProducerTelemetries));
    }
  }
}
