/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Date;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.dependency.DependencyNode;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.util.HashUtils;
import com.sonatype.insight.util.ComponentIdentifierHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.InnerSourceUtils.getVersionlessPackageUrl;

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

  private static final String PURL_PREFIX = "pkg:";

  public static final String AA_DATA_NODE = "aaData";

  public static final String FIELD_ANALYZER_FEATURES = "analyzerFeatures";

  private final JsonNode dependenciesJson;

  private final JsonNode bomJson;

  private final JsonNode dataJson;

  private final JsonNode summaryJson;

  private final Application application;

  private final TelemetrySender telemetrySender;

  private final InnerSourceComponentDAO innerSourceComponentDAO;

  private final ApplicationDAO applicationDAO;

  private AtomicInteger knownArtifactCount = new AtomicInteger();

  private AtomicInteger totalArtifactCount = new AtomicInteger();

  private AtomicInteger exactlyMatchedComponentCount = new AtomicInteger();

  private Map<ComponentIdentifier, ObjectNode> bomComponentNodes;

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
          if (tree.getComponentIdentifier() != null) {
            boolean isValidRootArtifact =
                saveInnerSourceComponent(tree.getComponentIdentifier());
            if (isValidRootArtifact) {
              processInnerSourceDependencies(tree.getChildren());
            }
          }
          // no root ComponentIdentifier refers to a tree derived based on HDS data
          else if (CollectionUtils.isNotEmpty(tree.getChildren())) {
            updateDependencyInfoForComponentChildren(tree.getChildren(), true, false, null);
          }
        }
      }
    }
  }

  //visible for testing
  boolean saveInnerSourceComponent(final ComponentIdentifier componentIdentifier) {
    String appId = application.getId();
    PackageUrlIdentifier rootArtifactIdentifier = getVersionlessPackageUrl(componentIdentifier);
    if (rootArtifactIdentifier != null) {
      String version = componentIdentifier.get(ComponentIdentifier.VERSION);
      InnerSourceComponent innerSourceComponent = innerSourceComponentDAO.getByPackageUrl(rootArtifactIdentifier);
      if (innerSourceComponent != null) {
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
      else {
        innerSourceComponent = new InnerSourceComponent();
        innerSourceComponent.setApplicationId(appId);
        innerSourceComponent.setPackageUrl(rootArtifactIdentifier.getPackageUrl());
        innerSourceComponent.setLatestVersion(version);
        innerSourceComponentDAO.insert(innerSourceComponent);
        log.info("InnerSource component {} with version {} for app {} was created",
            innerSourceComponent.getPackageUrl(), version, appId);
      }
      return true;
    }
    return false;
  }

  private boolean isNewerVersion(String oldVersion, String newVersion) {
    return new ComparableVersion(newVersion == null ? "" : newVersion)
        .compareTo(new ComparableVersion(oldVersion == null ? "" : oldVersion)) > 0;
  }

  private Set<String> associateModuleToApp(
      final DependencyNode moduleDependency,
      final Set<ComponentIdentifier> directDependencies)
  {
    ComponentIdentifier moduleComponent = moduleDependency.getComponentIdentifier();
    log.debug("InnerSource module '{}' found", moduleComponent);

    saveInnerSourceComponent(moduleComponent);

    Set<String> innerSourceAppIds = new HashSet<>();
    for (DependencyNode directDependencyChild : moduleDependency.getChildren()) {
      processDirectDependency(directDependencyChild, innerSourceAppIds, directDependencies);
    }
    return innerSourceAppIds;
  }

  void processInnerSourceDependencies(final List<DependencyNode> children) {
    if (!children.isEmpty()) {

      Set<String> innerSourceAppIds = new HashSet<>();
      Set<ComponentIdentifier> directDependencies = getDirectDependencies(children);

      for (DependencyNode dependencyChild : children) {
        if (dependencyChild.isModule()) {
          Set<String> moduleInnerSourceAppIds = associateModuleToApp(dependencyChild, directDependencies);
          innerSourceAppIds.addAll(moduleInnerSourceAppIds);
        }
        else if (dependencyChild.isDirect()) {
          processDirectDependency(dependencyChild, innerSourceAppIds, directDependencies);
        }
      }
      updateReportSummaryWithInnerSourceResults(dataJson, summaryJson);

      sendTelemetryData(innerSourceAppIds);
    }
  }

  private void processDirectDependency(
      final DependencyNode directDependency,
      final Set<String> innerSourceAppIds,
      final Set<ComponentIdentifier> directDependencies)
  {
    ComponentIdentifier dependencyId = directDependency.getComponentIdentifier();

    if (dependencyId != null) {
      PackageUrlIdentifier simplifiedPurl = InnerSourceUtils.getVersionlessPackageUrl(dependencyId);
      InnerSourceComponent innerSourceComponent =
          simplifiedPurl == null ? null : innerSourceComponentDAO.getByPackageUrl(simplifiedPurl);

      if (innerSourceComponent != null) {
        Application innerSourceApp = applicationDAO.getByIdNotNull(innerSourceComponent.getApplicationId());

        boolean isInnerSourceDependency =
            updateDependencyBomAsInnerSource(dependencyId, innerSourceApp);

        if (isInnerSourceDependency) {
          innerSourceAppIds.add(innerSourceApp.getId());
          processTransitiveDependencies(directDependency.getChildren(), directDependency, innerSourceApp,
              directDependency.getComponentIdentifier(), directDependencies);
        }
      }
      else {
        // a regular (non InnerSource) dependency
        updateBomNodeDependencyInformation(true, false, dependencyId, null, null);
        updateDependencyInfoForComponentChildren(directDependency.getChildren(), false, false,
            directDependency.getComponentIdentifier());
      }
    }
  }

  private void processTransitiveDependencies(
      final List<DependencyNode> transitiveDependencies,
      final DependencyNode innerSourceParent,
      final Application innerSourceApp,
      final ComponentIdentifier parentComponentIdentifier,
      final Set<ComponentIdentifier> directDependencies)
  {
    for (DependencyNode dependency : transitiveDependencies) {
      ComponentIdentifier dependencyIdentifier = dependency.getComponentIdentifier();
      Optional<ObjectNode> bomObjectNodeOptional = findBomComponent(dependencyIdentifier);

      if (bomObjectNodeOptional.isPresent() && !directDependencies.contains(dependencyIdentifier)) {
        ObjectNode bomObjectNode = bomObjectNodeOptional.get();

        InnerSourceData innerSourceData = new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(),
            PackageUrlIdentifier.toPackageUrl(innerSourceParent.getComponentIdentifier()));
        updateBomNodeDependencyInformation(false, false, dependencyIdentifier, parentComponentIdentifier,
            innerSourceData);
        log.debug("Component {} associated with InnerSource app {}", dependencyIdentifier,
            innerSourceApp.getName());

        if (MatchState.UNKNOWN.getId().equals(bomObjectNode.get(MATCH_STATE).asText())) {
          updateUnknownTransitiveDependencyAsKnown(dependencyIdentifier, bomObjectNode);
        }
      }

      if (CollectionUtils.isNotEmpty(dependency.getChildren())) {
        processTransitiveDependencies(dependency.getChildren(), innerSourceParent, innerSourceApp,
            dependencyIdentifier, directDependencies);
      }
    }
  }

  private void updateDependencyInfoForComponentChildren(
      final List<DependencyNode> children,
      final boolean isDirect,
      final boolean isInnerSource,
      final ComponentIdentifier parentIdentifier)
  {
    for (DependencyNode node : children) {
      if (node != null && node.getComponentIdentifier() != null) {
        updateBomNodeDependencyInformation(isDirect, isInnerSource, node.getComponentIdentifier(),
            parentIdentifier, null);
        if (CollectionUtils.isNotEmpty(node.getChildren())) {
          updateDependencyInfoForComponentChildren(node.getChildren(), false, isInnerSource,
              node.getComponentIdentifier());
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
      final Application innerSourceApp)
  {
    //If the component is direct and exists as InnerSource, it needs to be updated as such
    boolean isInnerSourceDependency = false;
    // If the associated app for the InnerSource component and the current app in context is the same
    // it does not need to be identified as InnerSource as it belongs to the app of the current report,
    if (!Objects.equals(application.getId(), innerSourceApp.getId())) {
      isInnerSourceDependency = true;
    }

    Optional<ObjectNode> bomLookup = findBomComponent(innerSourceComponentIdentifier);
    if (bomLookup.isPresent()) {
      ObjectNode bomObjectNode = bomLookup.get();
      if (isInnerSourceDependency) {
        InnerSourceData innerSourceData =
            new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(), null);
        updateBomNodeDependencyInformation(true, true, innerSourceComponentIdentifier, null,
            innerSourceData);
      }

      if (MatchState.UNKNOWN.getId().equals(bomObjectNode.get(MATCH_STATE).asText())) {
        markComponentAsKnown(bomObjectNode, innerSourceComponentIdentifier);
      }
      log.debug(isInnerSourceDependency ? "InnerSource component" : "Component" +
          "'{}' was updated in bom.json as a known component", innerSourceComponentIdentifier);
    }
    else {
      //In some scenarios (for example during MJA matching) the IS component is not identified
      // and not present in the bom.json. However we are certain at this point this is
      // an IS component so add a new identified component here.
      ObjectNode isNode = newNodeForISComponent(innerSourceComponentIdentifier);
      bomComponentNodes.put(innerSourceComponentIdentifier, isNode);
      InnerSourceData innerSourceData =
            new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(), null);
      totalArtifactCount.getAndIncrement();
      markComponentAsKnown(isNode, innerSourceComponentIdentifier);
      updateBomNodeDependencyInformation(true, true, innerSourceComponentIdentifier, null, innerSourceData);
    }
    return isInnerSourceDependency;
  }

  private ObjectNode newNodeForISComponent(ComponentIdentifier componentIdentifier) {
    ArrayNode aaNode = (ArrayNode) bomJson.get(AA_DATA_NODE);
    ObjectNode isNode = aaNode.addObject();
    isNode.put("hash", getHash(componentIdentifier));
    isNode.put("proprietary", false);
    isNode.set(FIELD_ANALYZER_FEATURES, JsonUtils.asTree(getAnalyzerFeaturesForNewNode(aaNode)));
    isNode.put("createTime", new Date().getTime());
    isNode.put("relativePopularity", 0);
    return isNode;
  }

  private String getHash(ComponentIdentifier componentIdentifier) {
    String hashString = componentIdentifier.getFormat() + ":" + StringUtils
        .join(componentIdentifier.getCoordinates().values(), ":");
    return HashHelper.truncateHash(HashUtils.hash(hashString, HashUtils.SHA1));
  }

  private AnalyzerFeatures getAnalyzerFeaturesForNewNode(JsonNode aaNode) {
    // its extremely unlikely to have bom.json with 0 components (identified+unknown combined)
    // and having a innersource coordinate in dependencies.json. Setting to "cli"
    // in such a rare case where in all other cases it will adopt a sibling's scanClient.
    String scanClient = "cli";
    if (aaNode.size() > 0 && aaNode.get(0).hasNonNull(FIELD_ANALYZER_FEATURES)) {
      scanClient = aaNode.get(0).get(FIELD_ANALYZER_FEATURES).get("scanClient").asText();
    }
    return new AnalyzerFeatures(AnalysisSource.THIRD_PARTY,
        AnalysisType.COORDINATE, scanClient, false, false, false);
  }

  private Optional<ObjectNode> findBomComponent(ComponentIdentifier identifier) {
    loadBomComponentsIfNotLoaded();
    return Optional.ofNullable(bomComponentNodes.get(identifier));
  }

  private void loadBomComponentsIfNotLoaded() {
    if (bomComponentNodes == null) {
      bomComponentNodes = new HashMap<>();
      for (JsonNode bomChild : bomJson.get(AA_DATA_NODE)) {
        ComponentIdentifier bomComponentIdentifier = getBomComponentIdentifier(bomChild);
        if (bomComponentIdentifier != null) {
          bomComponentNodes.put(bomComponentIdentifier, (ObjectNode) bomChild);
        }
      }
    }
  }

  private void markComponentAsKnown(
      final ObjectNode bomObjectNode,
      final ComponentIdentifier componentIdentifier)
  {
    knownArtifactCount.getAndIncrement();
    exactlyMatchedComponentCount.getAndIncrement();

    bomObjectNode.put(MATCH_STATE, MatchState.EXACT.getId());
    bomObjectNode.put("identificationSource", IdentificationSource.PACKAGE_MANIFEST.getId());

    bomObjectNode.set("componentIdentifier", JsonUtils.asTree(componentIdentifier));
    bomObjectNode.put("packageUrl", PackageUrlIdentifier.toPackageUrl(componentIdentifier));

    ComponentDisplayNameUtil.injectDisplayName(bomObjectNode);

    JsonNode analyzerFeatures = bomObjectNode.get(FIELD_ANALYZER_FEATURES);
    bomObjectNode.set(FIELD_ANALYZER_FEATURES, JsonUtils.asTree(new AnalyzerFeatures(AnalysisSource.THIRD_PARTY,
        AnalysisType.COORDINATE, analyzerFeatures.get("scanClient").asText())));
  }

  private void updateBomNodeDependencyInformation(
      final boolean isDirect,
      final boolean isInnerSource,
      final ComponentIdentifier componentId,
      final ComponentIdentifier parentComponentId,
      final InnerSourceData innerSourceData)
  {
    findBomComponent(componentId)
        .ifPresent(bomObjectNode -> {
          if (!bomObjectNode.hasNonNull(FIELD_COMPONENT_IDENTIFIER)) {
            bomObjectNode.set(FIELD_COMPONENT_IDENTIFIER, new ObjectMapper().valueToTree(componentId));
          }
          bomObjectNode
              .put(FIELD_DIRECT_DEPENDENCY, bomObjectNode.path(FIELD_DIRECT_DEPENDENCY).asBoolean(false) || isDirect);
          bomObjectNode
              .put(FIELD_INNER_SOURCE, bomObjectNode.path(FIELD_INNER_SOURCE).asBoolean(false) || isInnerSource);
          if (!isDirect && parentComponentId != null) {
            if (bomObjectNode.path(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD).isMissingNode()) {
              bomObjectNode.putArray(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD);
            }
            ArrayNode parentComponentPurls = (ArrayNode) bomObjectNode.get(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD);
            PackageUrlIdentifier parentComponentPurl = PackageUrlIdentifier.fromComponentIdentifier(parentComponentId);
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
        });
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
      final ObjectNode bomObjectNode)
  {
    PackageUrlIdentifier purl = getVersionlessPackageUrl(bomComponentIdentifier);
    InnerSourceComponent is = innerSourceComponentDAO.getByPackageUrl(purl);
    if (is != null) {
      //If the component is transitive and exists as InnerSource, it needs to be updated so it can be marked as
      //Transitive dependency but not as InnerSource
      markComponentAsKnown(bomObjectNode, bomComponentIdentifier);
      log.debug("InnerSource module {} was updated in bom.json as Transitive InnerSource", bomComponentIdentifier);
    }
  }

  private ComponentIdentifier getBomComponentIdentifier(JsonNode bomChild) {
    ComponentIdentifier bomComponentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(bomChild);

    if (bomComponentIdentifier == null) {
      bomComponentIdentifier = parsePathToId(bomChild.withArray("pathnames").get(0).asText());
    }
    return bomComponentIdentifier;
  }

  private ComponentIdentifier parsePathToId(final String pathnames) {
    String path;
    if (StringUtils.contains(pathnames, PURL_PREFIX)) {
      path = StringUtils.substring(pathnames, pathnames.indexOf(PURL_PREFIX), pathnames.length());
    }
    else {
      path = StringUtils.substringAfterLast(pathnames, "/");
    }
    return ComponentIdentifierHelper.parseId(path);
  }

  private Set<ComponentIdentifier> getDirectDependencies(List<DependencyNode> children) {
    Set<ComponentIdentifier> directDependencies = new HashSet<>();
    for (DependencyNode child : children) {
      if (!child.isModule() && child.isDirect()) {
        directDependencies.add(child.getComponentIdentifier());
      }

      for (DependencyNode firstLevel : child.getChildren()) {
        if (firstLevel.isDirect()) {
          directDependencies.add(firstLevel.getComponentIdentifier());
        }
      }
    }
    return directDependencies;
  }

  private void sendTelemetryData(final Set<String> innerSourceAppIds) {
    if (!innerSourceAppIds.isEmpty()) {
      telemetrySender.send(TelemetryUtils.buildInnerSourceTelemetryData(application.getId(), innerSourceAppIds));
    }
  }
}
