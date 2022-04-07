/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
import com.sonatype.insight.brain.innersource.InnerSourceProducerComponentTelemetry;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.dependency.DependencyNode;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.archive.CompoundSelector;
import com.sonatype.insight.scan.archive.PathSelector;
import com.sonatype.insight.scan.archive.RegexSelector;
import com.sonatype.insight.scan.archive.Selector;
import com.sonatype.insight.scan.archive.Selector.Selection;
import com.sonatype.insight.scan.util.HashUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final JsonNode dependenciesJson;

  private final JsonNode bomJson;

  private final JsonNode dataJson;

  private final JsonNode summaryJson;

  private final Application application;

  // Visible for testing
  Predicate<String> isProprietary;

  private final TelemetrySender telemetrySender;

  private final InnerSourceComponentDAO innerSourceComponentDAO;

  private final ApplicationDAO applicationDAO;

  private AtomicInteger knownArtifactCount = new AtomicInteger();

  private AtomicInteger totalArtifactCount = new AtomicInteger();

  private AtomicInteger exactlyMatchedComponentCount = new AtomicInteger();

  private Map<PackageUrlIdentifier, ObjectNode> bomComponentNodes;

  private final Set<InnerSourceProducerComponentTelemetry> innerSourceProducerTelemetries = new HashSet<>();

  public static DependencyResolver getInstance(
      JsonNode dependenciesJson,
      JsonNode bomJson,
      JsonNode dataJson,
      JsonNode summaryJson,
      Application application,
      TelemetrySender telemetrySender)
  {
    return new DependencyResolver(dependenciesJson, bomJson, dataJson, summaryJson, application, telemetrySender);
  }

  private DependencyResolver(
      final JsonNode dependenciesJson,
      final JsonNode bomJson,
      final JsonNode dataJson,
      final JsonNode summaryJson,
      final Application application,
      final TelemetrySender telemetrySender)
  {
    this(dependenciesJson, bomJson, dataJson, summaryJson, application, telemetrySender, new InnerSourceComponentDAO(),
        new ApplicationDAO());
  }

  //visible for testing
  DependencyResolver(
      final JsonNode dependenciesJson,
      final JsonNode bomJson,
      final JsonNode dataJson,
      final JsonNode summaryJson,
      final Application application,
      final TelemetrySender telemetrySender,
      final InnerSourceComponentDAO innerSourceComponentDAO,
      final ApplicationDAO applicationDAO)
  {

    this.dependenciesJson = dependenciesJson;
    this.bomJson = bomJson;
    this.dataJson = dataJson;
    this.summaryJson = summaryJson;
    this.application = application;
    this.telemetrySender = telemetrySender;
    this.innerSourceComponentDAO = innerSourceComponentDAO;
    this.applicationDAO = applicationDAO;
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
        DependencyNode tree = JsonUtils.asPojo(dependencyTreeNode, DependencyNode.class);
        if (tree != null) {
          PackageUrlIdentifier rootPurl = InnerSourceUtils.getPackageUrl(tree);
          if (rootPurl != null) {
            boolean isValidRootArtifact = saveInnerSourceComponent(rootPurl);
            if (isValidRootArtifact) {
              processInnerSourceDependencies(tree.getChildren());
            }
          }
          // no root ComponentIdentifier refers to a tree derived based on HDS data
          // or SBOM File where the parent component does not have a purl
          else if (CollectionUtils.isNotEmpty(tree.getChildren())) {
            updateDependencyInfoForComponentChildren(tree.getChildren(), true, false, null);
          }
          addBomDependencyDataIndicator(bomJson);
        }
      }
    }
  }

  private void addBomDependencyDataIndicator(JsonNode bomJson) {
    ObjectNode bomNode = (ObjectNode) bomJson;
    bomNode.put(FIELD_DEPENDENCY_INDICATOR, true);
  }

  //visible for testing
  boolean saveInnerSourceComponent(final PackageUrlIdentifier packageUrl) {
    if (packageUrl != null) {
      String appId = application.getId();
      PackageUrlIdentifier versionlessPurl = packageUrl.createAlternativeVersion(null);
      if (versionlessPurl != null) {
        String version = packageUrl.getVersion();
        InnerSourceComponent innerSourceComponent = innerSourceComponentDAO.getByPackageUrl(versionlessPurl);
        if (innerSourceComponent != null) {
          processExistingInnerSourceComponent(innerSourceComponent, version, appId);
        }
        else {
          innerSourceComponent = new InnerSourceComponent();
          innerSourceComponent.setApplicationId(appId);
          innerSourceComponent.setPackageUrl(versionlessPurl.getPackageUrl());
          innerSourceComponent.setLatestVersion(version);
          innerSourceComponentDAO.insert(innerSourceComponent);
          log.info("InnerSource component {} with version {} for app {} was created",
              innerSourceComponent.getPackageUrl(), version, appId);
        }
        return true;
      }
    }
    return false;
  }

  private void processExistingInnerSourceComponent(
      final InnerSourceComponent innerSourceComponent,
      final String version,
      final String appId)
  {
    boolean isNewerVersion = isNewerVersion(innerSourceComponent.getLatestVersion(), version);
    if (!appId.equals(innerSourceComponent.getApplicationId()) || isNewerVersion) {
      innerSourceComponent.setApplicationId(appId);
      if (isNewerVersion) {
        innerSourceComponent.setLatestVersion(version);
      }
      innerSourceComponentDAO.update(innerSourceComponent);
      log.info("InnerSource component {} with version {} for app {} was updated",
          innerSourceComponent.getPackageUrl(), version, appId);
    }
  }

  private boolean isNewerVersion(String oldVersion, String newVersion) {
    return new ComparableVersion(newVersion == null ? "" : newVersion)
        .compareTo(new ComparableVersion(oldVersion == null ? "" : oldVersion)) > 0;
  }

  private void associateModuleToApp(
      final DependencyNode moduleDependency,
      final Set<PackageUrlIdentifier> directDependencies,
      final Map<String, Boolean> processedDirectDependencies)
  {
    PackageUrlIdentifier modulePurl = InnerSourceUtils.getPackageUrl(moduleDependency);
    if (modulePurl != null) {
      log.debug("InnerSource module '{}' found", modulePurl);
      saveInnerSourceComponent(modulePurl);
      for (DependencyNode directDependencyChild : moduleDependency.getChildren()) {
        processDirectDependency(directDependencyChild, directDependencies, processedDirectDependencies);
      }
    }
  }

  void processInnerSourceDependencies(final List<DependencyNode> children) {
    if (!children.isEmpty()) {

      Set<PackageUrlIdentifier> directDependencies = getDirectDependencies(children);
      Map<String, Boolean> processedDirectDependencies = new HashMap<>();

      for (DependencyNode dependencyChild : children) {
        if (dependencyChild.isModule()) {
          associateModuleToApp(dependencyChild, directDependencies, processedDirectDependencies);
        }
        else if (dependencyChild.isDirect()) {
          processDirectDependency(dependencyChild, directDependencies, processedDirectDependencies);
        }
      }
      updateReportSummaryWithInnerSourceResults(dataJson, summaryJson);

      sendTelemetryData();
    }
  }

  private void processDirectDependency(
      final DependencyNode directDependency,
      final Set<PackageUrlIdentifier> directDependencies,
      final Map<String, Boolean> processedDirectDependencies)
  {
    ComponentIdentifier componentIdentifier = directDependency.getComponentIdentifier();
    PackageUrlIdentifier packageUrlIdentifier = InnerSourceUtils.getPackageUrl(directDependency);

    if (packageUrlIdentifier != null) {
      PackageUrlIdentifier simplifiedPurl = packageUrlIdentifier.createAlternativeVersion(null);
      InnerSourceComponent innerSourceComponent =
          simplifiedPurl == null ? null : innerSourceComponentDAO.getByPackageUrl(simplifiedPurl);

      if (innerSourceComponent != null) {
        Application innerSourceApp = applicationDAO.getByIdNotNull(innerSourceComponent.getApplicationId());

        if (!processedDirectDependencies.containsKey(packageUrlIdentifier.getPackageUrl())) {
          boolean isInnerSourceDependency =
              updateDependencyBomAsInnerSource(componentIdentifier, packageUrlIdentifier, innerSourceApp);

          if (isInnerSourceDependency) {
            processTransitiveDependencies(directDependency.getChildren(), directDependency, innerSourceApp,
                InnerSourceUtils.getPackageUrl(directDependency), directDependencies);
          }
          processedDirectDependencies.put(packageUrlIdentifier.getPackageUrl(), isInnerSourceDependency);
        }
        else if (Boolean.TRUE.equals(processedDirectDependencies.get(packageUrlIdentifier.getPackageUrl()))) {
          processTransitiveDependencies(directDependency.getChildren(), directDependency, innerSourceApp,
              InnerSourceUtils.getPackageUrl(directDependency), directDependencies);
        }
      }
      else {
        // a regular (non InnerSource) dependency
        updateBomNodeDependencyInformation(true, false, componentIdentifier, packageUrlIdentifier, null, null);
        updateDependencyInfoForComponentChildren(directDependency.getChildren(), false, false,
            InnerSourceUtils.getPackageUrl(directDependency));
      }
    }
  }

  private void processTransitiveDependencies(
      final List<DependencyNode> transitiveDependencies,
      final DependencyNode innerSourceParent,
      final Application innerSourceApp,
      final PackageUrlIdentifier parentPurl,
      final Set<PackageUrlIdentifier> directDependencies)
  {
    for (DependencyNode dependency : transitiveDependencies) {
      ComponentIdentifier dependencyComponentIdentifier = dependency.getComponentIdentifier();
      PackageUrlIdentifier dependencyPurl = InnerSourceUtils.getPackageUrl(dependency);
      Optional<ObjectNode> bomObjectNodeOptional = findBomComponent(dependencyPurl);

      if (bomObjectNodeOptional.isPresent() && dependencyPurl != null && !directDependencies.contains(dependencyPurl)) {
        ObjectNode bomObjectNode = bomObjectNodeOptional.get();

        InnerSourceData innerSourceData = new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(),
            PackageUrlIdentifier.toPackageUrl(innerSourceParent.getComponentIdentifier()));
        updateBomNodeDependencyInformation(false, false, dependencyComponentIdentifier, dependencyPurl, parentPurl,
            innerSourceData);
        log.debug("Component {} associated with InnerSource app {}", dependencyPurl, innerSourceApp.getName());

        if (MatchState.UNKNOWN.getId().equals(bomObjectNode.get(MATCH_STATE).asText())) {
          updateUnknownTransitiveDependencyAsKnown(dependencyComponentIdentifier, dependencyPurl, bomObjectNode);
        }
      }

      if (CollectionUtils.isNotEmpty(dependency.getChildren())) {
        processTransitiveDependencies(dependency.getChildren(), innerSourceParent, innerSourceApp, dependencyPurl,
            directDependencies);
      }
    }
  }

  private void updateDependencyInfoForComponentChildren(
      final List<DependencyNode> children,
      final boolean isDirect,
      final boolean isInnerSource,
      final PackageUrlIdentifier parentPurl)
  {
    for (DependencyNode node : children) {
      if (node != null && node.getComponentIdentifier() != null) {
        updateBomNodeDependencyInformation(isDirect, isInnerSource, node.getComponentIdentifier(),
            InnerSourceUtils.getPackageUrl(node), parentPurl, null);
        if (CollectionUtils.isNotEmpty(node.getChildren())) {
          updateDependencyInfoForComponentChildren(node.getChildren(), false, isInnerSource,
              InnerSourceUtils.getPackageUrl(node));
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

  private boolean updateDependencyBomAsInnerSource(
      final ComponentIdentifier innerSourceComponentIdentifier,
      final PackageUrlIdentifier innerSourcePackageUrlIdentifier,
      final Application innerSourceApp)
  {
    //If the component is direct and exists as InnerSource, it needs to be updated as such
    // If the associated app for the InnerSource component and the current app in context is the same
    // it does not need to be identified as InnerSource as it belongs to the app of the current report
    boolean isInnerSourceDependency = !Objects.equals(application.getId(), innerSourceApp.getId());

    InnerSourceProducerComponentTelemetry producerInfo = null;

    if (isInnerSourceDependency) {
      producerInfo = new InnerSourceProducerComponentTelemetry();
      producerInfo.setFormat(innerSourcePackageUrlIdentifier.getFormat());
      producerInfo.setProducerAppId(innerSourceApp.getId());
    }

    Optional<ObjectNode> bomLookup = findBomComponent(innerSourcePackageUrlIdentifier);
    if (bomLookup.isPresent()) {
      ObjectNode bomObjectNode = bomLookup.get();
      if (isInnerSourceDependency) {
        InnerSourceData innerSourceData = new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(), null);
        updateBomNodeDependencyInformation(true, true, innerSourceComponentIdentifier, innerSourcePackageUrlIdentifier,
            null, innerSourceData, producerInfo);
      }

      if (MatchState.UNKNOWN.getId().equals(bomObjectNode.get(MATCH_STATE).asText())) {
        markComponentAsKnown(bomObjectNode, innerSourceComponentIdentifier, innerSourcePackageUrlIdentifier, true);
        log.debug(isInnerSourceDependency ? "InnerSource component" : "Component" +
            "'{}' was updated in bom.json as a known component", innerSourcePackageUrlIdentifier);
      }
    }
    else {
      //In some scenarios (for example during MJA matching) the component is not identified
      // and not present in the bom.json. However, we are certain at this point this is
      // an IS component so add a new identified component here and only marked as IS if the application is different,
      // otherwise is only marked as direct.
      ObjectNode isNode = newNodeForISComponent(innerSourcePackageUrlIdentifier);

      bomComponentNodes.put(innerSourcePackageUrlIdentifier, isNode);
      InnerSourceData innerSourceData = null;
      if (isInnerSourceDependency) {
        innerSourceData = new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(), null);
      }
      totalArtifactCount.getAndIncrement();
      markComponentAsKnown(isNode, innerSourceComponentIdentifier, innerSourcePackageUrlIdentifier, true);
      updateBomNodeDependencyInformation(true, isInnerSourceDependency, innerSourceComponentIdentifier,
          innerSourcePackageUrlIdentifier, null, innerSourceData, producerInfo);
      log.debug(isInnerSourceDependency ? "InnerSource component" : "Component" + "'{}' was created in bom.json",
          innerSourceComponentIdentifier);
    }

    if (producerInfo != null) {
      innerSourceProducerTelemetries.add(producerInfo);
    }

    return isInnerSourceDependency;
  }

  private ObjectNode newNodeForISComponent(PackageUrlIdentifier componentPurl) {
    ArrayNode aaNode = (ArrayNode) bomJson.get(AA_DATA_NODE);
    ObjectNode isNode = aaNode.addObject();
    isNode.put("hash", getHash(componentPurl));
    isNode.put("proprietary", isProprietaryComponent(componentPurl));
    isNode.set(FIELD_ANALYZER_FEATURES, JsonUtils.asTree(getAnalyzerFeaturesForNewNode(aaNode)));
    isNode.put("createTime", new Date().getTime());
    isNode.put("relativePopularity", 0);
    String packageUrlString = componentPurl.getPackageUrl();
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode filenames = objectMapper.createArrayNode();
    filenames.add(packageUrlString);
    isNode.set("filenames", filenames);
    ArrayNode pathnames = objectMapper.createArrayNode();
    pathnames.add("dependency:/" + packageUrlString.replace('/', '\\'));
    isNode.set("pathnames", pathnames);
    return isNode;
  }

  // Visible for testing
  boolean isProprietaryComponent(PackageUrlIdentifier componentPurl) {
    if (isProprietary == null) {
      isProprietary = createIsProprietary(application.getId());
    }
    return componentPurl.toComponentIdentifier().getProprietaryCoordinates().stream().anyMatch(isProprietary);
  }

  // Visible for testing
  static Predicate<String> createIsProprietary(String internalOwnerId) {
    ProprietaryConfig proprietaryConfig =
        ProprietaryConfigService.getProprietaryConfig(internalOwnerId, new OwnerDAO(), new ProprietaryConfigDAO());
    List<Selector> selectors = new ArrayList<>();
    if (!proprietaryConfig.getPackages().isEmpty()) {
      selectors.add(PathSelector.forProprietaryPackages(
          StringUtils.join(proprietaryConfig.getPackages().iterator(), ProprietaryConfig.PACKAGE_DELIM)));
    }
    if (!proprietaryConfig.getRegexes().isEmpty()) {
      selectors.add(RegexSelector.forProprietaryRegexes(
          StringUtils.join(proprietaryConfig.getRegexes().iterator(), ProprietaryConfig.REGEX_DELIM)));
    }
    if (selectors.isEmpty()) {
      return s -> false;
    }
    Selector compoundSelector = new CompoundSelector(PathSelector.PROPERTY_NAME, selectors.toArray(new Selector[0]));
    return s -> compoundSelector.isSelected(s) == Selection.EXCLUDED;
  }

  private String getHash(PackageUrlIdentifier componentPurl) {
    return HashHelper.truncateHash(HashUtils.hash(componentPurl.getPackageUrl(), HashUtils.SHA1));
  }

  private AnalyzerFeatures getAnalyzerFeaturesForNewNode(JsonNode aaNode) {
    // it's extremely unlikely to have bom.json with 0 components (identified+unknown combined)
    // and having an InnerSource coordinate in dependencies.json. Setting to "cli"
    // in such a rare case where in all other cases it will adopt a sibling's scanClient.
    String scanClient = "cli";
    if (aaNode.size() > 0 && aaNode.get(0).hasNonNull(FIELD_ANALYZER_FEATURES)) {
      scanClient = aaNode.get(0).get(FIELD_ANALYZER_FEATURES).get("scanClient").asText();
    }
    return new AnalyzerFeatures(AnalysisSource.THIRD_PARTY,
        AnalysisType.COORDINATE, scanClient, false, false, false);
  }

  private Optional<ObjectNode> findBomComponent(PackageUrlIdentifier identifier) {
    loadBomComponentsIfNotLoaded();
    return Optional.ofNullable(bomComponentNodes.get(identifier));
  }

  private void loadBomComponentsIfNotLoaded() {
    if (bomComponentNodes == null) {
      bomComponentNodes = new HashMap<>();
      for (JsonNode bomChild : bomJson.get(AA_DATA_NODE)) {
        PackageUrlIdentifier purl = ComponentIdentifierAdapter.getPackageUrlIdentifier(bomChild);
        if (purl != null) {
          bomComponentNodes.put(purl, (ObjectNode) bomChild);
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
            bomComponentNodes.put(purl, (ObjectNode) bomChild);
          }
        }
      }
    }
  }

  private void markComponentAsKnown(
      final ObjectNode bomObjectNode,
      final ComponentIdentifier componentIdentifier,
      final PackageUrlIdentifier componentPurl,
      final boolean isDirect)
  {
    knownArtifactCount.getAndIncrement();
    exactlyMatchedComponentCount.getAndIncrement();

    bomObjectNode.put(MATCH_STATE, MatchState.EXACT.getId());
    bomObjectNode.put("identificationSource", IdentificationSource.PACKAGE_MANIFEST.getId());

    bomObjectNode.set(FIELD_COMPONENT_IDENTIFIER, JsonUtils.asTree(componentIdentifier));
    bomObjectNode.put(FIELD_PACKAGE_URL, componentPurl.getPackageUrl());

    ComponentDisplayNameUtil.injectDisplayName(bomObjectNode);

    if (!bomObjectNode.hasNonNull(FIELD_DIRECT_DEPENDENCY)) {
      bomObjectNode.put(FIELD_DIRECT_DEPENDENCY, isDirect);
    }

    JsonNode analyzerFeatures = bomObjectNode.get(FIELD_ANALYZER_FEATURES);
    JsonNode manifestContentType = analyzerFeatures.get("manifestContentType");
    bomObjectNode.set(FIELD_ANALYZER_FEATURES, JsonUtils.asTree(new AnalyzerFeatures(AnalysisSource.THIRD_PARTY,
        AnalysisType.COORDINATE, analyzerFeatures.get("scanClient").asText(),
        manifestContentType != null ? manifestContentType.asText() : null)));
  }

  private void updateBomNodeDependencyInformation(
      final boolean isDirect,
      final boolean isInnerSource,
      final ComponentIdentifier componentIdentifier,
      final PackageUrlIdentifier componentPurl,
      final PackageUrlIdentifier parentComponentPurl,
      final InnerSourceData innerSourceData)
  {
    updateBomNodeDependencyInformation(isDirect, isInnerSource, componentIdentifier, componentPurl, parentComponentPurl,
        innerSourceData, null);
  }

  private void updateBomNodeDependencyInformation(
      final boolean isDirect,
      final boolean isInnerSource,
      final ComponentIdentifier componentIdentifier,
      final PackageUrlIdentifier componentPurl,
      final PackageUrlIdentifier parentComponentPurl,
      final InnerSourceData innerSourceData,
      final InnerSourceProducerComponentTelemetry producerInfo)
  {
    findBomComponent(componentPurl).ifPresent(
        bomObjectNode -> processBomNode(isDirect, isInnerSource, componentIdentifier, componentPurl,
            parentComponentPurl, innerSourceData, producerInfo, bomObjectNode));
  }

  private void processBomNode(
      final boolean isDirect,
      final boolean isInnerSource,
      final ComponentIdentifier componentIdentifier,
      final PackageUrlIdentifier componentPurl,
      final PackageUrlIdentifier parentComponentPurl,
      final InnerSourceData innerSourceData,
      final InnerSourceProducerComponentTelemetry producerInfo,
      final ObjectNode bomObjectNode)
  {
    if (!bomObjectNode.hasNonNull(FIELD_COMPONENT_IDENTIFIER)) {
      bomObjectNode.set(FIELD_COMPONENT_IDENTIFIER, new ObjectMapper().valueToTree(componentIdentifier));
    }
    if (!bomObjectNode.hasNonNull(FIELD_PACKAGE_URL)) {
      bomObjectNode.put(FIELD_PACKAGE_URL, componentPurl.getPackageUrl());
    }
    bomObjectNode
        .put(FIELD_DIRECT_DEPENDENCY, bomObjectNode.path(FIELD_DIRECT_DEPENDENCY).asBoolean(false) || isDirect);
    bomObjectNode
        .put(FIELD_INNER_SOURCE, bomObjectNode.path(FIELD_INNER_SOURCE).asBoolean(false) || isInnerSource);
    if (!isDirect && parentComponentPurl != null) {
      if (bomObjectNode.path(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD).isMissingNode()) {
        bomObjectNode.putArray(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD);
      }
      ArrayNode parentComponentPurls = (ArrayNode) bomObjectNode.get(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD);
      if (!contains(parentComponentPurls, parentComponentPurl)) {
        parentComponentPurls.add(parentComponentPurl.getPackageUrl());
      }
    }
    if (innerSourceData != null) {
      if (bomObjectNode.path(ComponentDAO.INNER_SOURCE_DATA_FIELD).isMissingNode()) {
        bomObjectNode.putArray(ComponentDAO.INNER_SOURCE_DATA_FIELD);
      }
      ArrayNode innerSourceDataArray = (ArrayNode) bomObjectNode.get(ComponentDAO.INNER_SOURCE_DATA_FIELD);
      if (!contains(innerSourceDataArray, innerSourceData)) {
        innerSourceDataArray.add(JsonUtils.asTree(innerSourceData));
      }
    }
    setFieldAnalyzerFeatures(producerInfo, bomObjectNode);
  }

  private void setFieldAnalyzerFeatures(
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
        log.debug("Failed to parse inner source data " + innerSourceNode, e);
      }
    }
    return false;
  }

  private void updateUnknownTransitiveDependencyAsKnown(
      final ComponentIdentifier bomComponentIdentifier,
      final PackageUrlIdentifier bomPurl,
      final ObjectNode bomObjectNode)
  {
    PackageUrlIdentifier versionlessPurl = bomPurl.createAlternativeVersion(null);
    InnerSourceComponent is = innerSourceComponentDAO.getByPackageUrl(versionlessPurl);
    if (is != null) {
      //If the component is transitive and exists as InnerSource, it needs to be updated, so it can be marked as
      //Transitive dependency but not as InnerSource
      markComponentAsKnown(bomObjectNode, bomComponentIdentifier, bomPurl, false);
      log.debug("InnerSource module {} was updated in bom.json as Transitive InnerSource", bomPurl);
    }
  }

  private Set<PackageUrlIdentifier> getDirectDependencies(List<DependencyNode> children) {
    Set<PackageUrlIdentifier> directDependencies = new HashSet<>();
    for (DependencyNode child : children) {
      if (!child.isModule() && child.isDirect()) {
        directDependencies.add(InnerSourceUtils.getPackageUrl(child));
      }

      for (DependencyNode firstLevel : child.getChildren()) {
        if (firstLevel.isDirect()) {
          directDependencies.add(InnerSourceUtils.getPackageUrl(firstLevel));
        }
      }
    }
    return directDependencies;
  }

  private void sendTelemetryData() {
    if (!innerSourceProducerTelemetries.isEmpty()) {
      telemetrySender.send(
          TelemetryUtils.buildInnerSourceTelemetryData(application.getId(), innerSourceProducerTelemetries));
    }
  }
}
