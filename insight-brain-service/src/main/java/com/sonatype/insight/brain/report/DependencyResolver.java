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
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.dependency.DependencyNode;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.util.ComponentIdentifierHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

  public static final String MATCH_STATE = "matchState";

  private static final String FIELD_DIRECT_DEPENDENCY = "directDependency";

  private static final String FIELD_INNER_SOURCE = "innerSource";

  private static final String FIELD_INNER_SOURCE_DATA = "innerSourceData";

  private final JsonNode dependenciesJson;

  private final JsonNode bomJson;

  private final JsonNode dataJson;

  private final JsonNode summaryJson;

  private final Application application;

  private final TelemetrySender telemetrySender;

  private final InnerSourceComponentDAO innerSourceComponentDAO;

  private final ApplicationDAO applicationDAO;

  private AtomicInteger knownArtifactCount = new AtomicInteger();

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
      InnerSourceComponent innerSourceComponent = innerSourceComponentDAO.getByPackageUrl(rootArtifactIdentifier);
      if (innerSourceComponent != null) {
        if (!appId.equals(innerSourceComponent.getApplicationId())) {
          innerSourceComponent.setApplicationId(appId);
          innerSourceComponentDAO.update(innerSourceComponent);
          log.info("InnerSource component {} for app {} was updated", innerSourceComponent.getPackageUrl(), appId);
        }
      }
      else {
        innerSourceComponent = new InnerSourceComponent();
        innerSourceComponent.setApplicationId(appId);
        innerSourceComponent.setPackageUrl(rootArtifactIdentifier.getPackageUrl());
        innerSourceComponentDAO.insert(innerSourceComponent);
        log.info("InnerSource component {} for app {} was created", innerSourceComponent.getPackageUrl(), appId);
      }
      return true;
    }
    return false;
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
      ComponentIdentifier simplifiedComponent =
          ComponentIdentifier.createMavenCoordinates(dependencyId.get(ComponentIdentifier.MAVEN_GROUP_ID),
              dependencyId.get(ComponentIdentifier.MAVEN_ARTIFACT_ID), null);

      InnerSourceComponent innerSourceComponent =
          innerSourceComponentDAO.getByPackageUrl(PackageUrlIdentifier.fromComponentIdentifier(simplifiedComponent));

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

    ((ObjectNode) summaryJson).put(KNOWN_ARTIFACT_COUNT, knownArtifactCount.intValue());
  }

  private boolean updateDependencyBomAsInnerSource(
      final ComponentIdentifier innerSourceComponentIdentifier,
      final Application innerSourceApp)
  {
    return findBomComponent(innerSourceComponentIdentifier)
        .map(bomObjectNode -> {
          //If the component is direct and exists as InnerSource, it needs to be updated as such
          boolean isInnerSourceDependency = false;

          // If the associated app for the InnerSource component and the current app in context is the same
          // it does not need to be identified as InnerSource as it belongs to the app of the current report,
          // but it can be marked as a known component
          if (!Objects.equals(application.getId(), innerSourceApp.getId())) {
            InnerSourceData innerSourceData =
                new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(), null);
            updateBomNodeDependencyInformation(true, true, innerSourceComponentIdentifier, null, innerSourceData);
            isInnerSourceDependency = true;
          }

          if (MatchState.UNKNOWN.getId().equals(bomObjectNode.get(MATCH_STATE).asText())) {
            markComponentAsKnown(bomObjectNode, innerSourceComponentIdentifier);
          }

          log.debug(isInnerSourceDependency ? "InnerSource component" : "Component" +
              "'{}' was updated in bom.json as a known component", innerSourceComponentIdentifier);

          return isInnerSourceDependency;
        })
        .orElse(false);
  }

  private Optional<ObjectNode> findBomComponent(ComponentIdentifier identifier) {
    loadBomComponentsIfNotLoaded();
    return Optional.ofNullable(bomComponentNodes.get(identifier));
  }

  private void loadBomComponentsIfNotLoaded() {
    if (bomComponentNodes == null) {
      bomComponentNodes = new HashMap<>();
      for (JsonNode bomChild : bomJson.get("aaData")) {
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

    ComponentDisplayNameUtil.injectDisplayName(bomObjectNode);

    JsonNode analyzerFeatures = bomObjectNode.get("analyzerFeatures");
    bomObjectNode.set("analyzerFeatures", JsonUtils.asTree(new AnalyzerFeatures(AnalysisSource.THIRD_PARTY,
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
          bomObjectNode
              .put(FIELD_DIRECT_DEPENDENCY, bomObjectNode.path(FIELD_DIRECT_DEPENDENCY).asBoolean(false) || isDirect);
          bomObjectNode
              .put(FIELD_INNER_SOURCE, bomObjectNode.path(FIELD_INNER_SOURCE).asBoolean(false) || isInnerSource);
          if (!isDirect && parentComponentId != null) {
            if (bomObjectNode.path(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD).isMissingNode()) {
              bomObjectNode.putArray(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD);
            }
            ((ArrayNode) bomObjectNode.get(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD))
                .add(PackageUrlIdentifier.toPackageUrl(parentComponentId));
          }
          if (innerSourceData != null && bomObjectNode.path(FIELD_INNER_SOURCE_DATA).isMissingNode()) {
            bomObjectNode.set(FIELD_INNER_SOURCE_DATA, JsonUtils.asTree(innerSourceData));
          }
        });
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
      String path = StringUtils.substringAfterLast(bomChild.withArray("pathnames").get(0).asText(), "/");
      bomComponentIdentifier = ComponentIdentifierHelper.parseMavenId(path);
    }
    return bomComponentIdentifier;
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
