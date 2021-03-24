/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.99
 */
public final class ReportInnerSource
{
  private static final Logger log = LoggerFactory.getLogger(ReportInnerSource.class);

  private static final String EXACTLY_MATCHED_COMPONENT_COUNT = "exactlyMatchedComponentCount";

  private static final String KNOWN_ARTIFACT_COUNT = "knownArtifactCount";

  public static final String MATCH_STATE = "matchState";

  private ReportInnerSource() {}

  public static void processDependencyTree(
      final JsonNode dependenciesJson,
      final JsonNode bomJson,
      final JsonNode dataJson,
      final JsonNode summaryJson,
      final Application application,
      final TelemetrySender telemetrySender) throws IOException
  {
    if (dependenciesJson != null) {
      JsonNode dependencyTreeNode = dependenciesJson.path("dependencyTree");

      if (!dependencyTreeNode.isMissingNode()) {
        DependencyNode tree = JsonUtils.asPojo(dependencyTreeNode, DependencyNode.class);
        if (tree != null) {
          if (tree.getComponentIdentifier() != null) {
            InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();
            boolean isValidRootArtifact =
                saveInnerSourceComponent(tree.getComponentIdentifier(), application.getId(), innerSourceComponentDAO);
            if (isValidRootArtifact) {
              processInnerSourceDependencies(tree.getChildren(), bomJson, dataJson, summaryJson, application,
                  innerSourceComponentDAO, telemetrySender);
            }
          }
          // no root ComponentIdentifier refers to a tree derived based on HDS data
          else if (CollectionUtils.isNotEmpty(tree.getChildren())) {
            updateDependencyInfoForComponentChildren(tree.getChildren(), true, false, null, bomJson);
          }
        }
      }
    }
  }

  // Visible for testing
  static boolean saveInnerSourceComponent(
      final ComponentIdentifier componentIdentifier,
      final String appId,
      final InnerSourceComponentDAO innerSourceComponentDAO)
  {
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

  private static PackageUrlIdentifier getVersionlessPackageUrl(final ComponentIdentifier componentIdentifier) {
    if (componentIdentifier != null) {
      return new PackageUrlIdentifier(String.format("pkg:maven/%s/%s",
          componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
          componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID)));
    }
    return null;
  }

  // visible for testing
  static void processInnerSourceDependencies(
      final List<DependencyNode> children,
      final JsonNode bomJson,
      final JsonNode dataJson,
      final JsonNode summaryJson,
      final Application application,
      final InnerSourceComponentDAO innerSourceComponentDAO,
      final TelemetrySender telemetrySender)
  {
    if (!children.isEmpty()) {
      AtomicInteger knownArtifactCount = new AtomicInteger(summaryJson.path(KNOWN_ARTIFACT_COUNT).asInt());
      AtomicInteger exactlyMatchedComponentCount =
          new AtomicInteger(dataJson.path(EXACTLY_MATCHED_COMPONENT_COUNT).asInt());
      ApplicationDAO applicationDAO = new ApplicationDAO();

      Set<String> innerSourceAppIds = new HashSet<>();
      Set<ComponentIdentifier> directDependencies = getDirectDependencies(children);

      for (DependencyNode dependencyChild : children) {
        if (dependencyChild.isModule()) {
          Set<String> moduleInnerSourceAppIds = associateModuleToApp(dependencyChild, applicationDAO,
              innerSourceComponentDAO, bomJson, application, knownArtifactCount, exactlyMatchedComponentCount,
              directDependencies);
          innerSourceAppIds.addAll(moduleInnerSourceAppIds);
        }
        else if (dependencyChild.isDirect()) {
          processDirectDependency(dependencyChild, applicationDAO, innerSourceComponentDAO, bomJson,
              application, knownArtifactCount, exactlyMatchedComponentCount, innerSourceAppIds, directDependencies);
        }
      }
      updateReportSummaryWithInnerSourceResults(dataJson, summaryJson, knownArtifactCount,
          exactlyMatchedComponentCount);

      sendTelemetryData(application.getId(), innerSourceAppIds, telemetrySender);
    }
  }

  private static Set<ComponentIdentifier> getDirectDependencies(List<DependencyNode> children) {
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

  private static void sendTelemetryData(
      final String consumerId,
      final Set<String> innerSourceAppIds,
      final TelemetrySender telemetrySender)
  {
    if (!innerSourceAppIds.isEmpty()) {
      telemetrySender.send(TelemetryUtils.buildInnerSourceTelemetryData(consumerId, innerSourceAppIds));
    }
  }

  private static Set<String> associateModuleToApp(
      final DependencyNode moduleDependency,
      final ApplicationDAO applicationDAO,
      final InnerSourceComponentDAO innerSourceComponentDAO,
      final JsonNode bom,
      final Application currentApplication,
      final AtomicInteger knownArtifactCount,
      final AtomicInteger exactlyMatchedComponentCount,
      final Set<ComponentIdentifier> directDependencies)
  {
    ComponentIdentifier moduleComponent = moduleDependency.getComponentIdentifier();
    log.debug("InnerSource module '{}' found", moduleComponent);

    saveInnerSourceComponent(moduleComponent, currentApplication.getId(), innerSourceComponentDAO);

    Set<String> innerSourceAppIds = new HashSet<>();
    for (DependencyNode directDependencyChild : moduleDependency.getChildren()) {
      processDirectDependency(directDependencyChild, applicationDAO, innerSourceComponentDAO, bom, currentApplication,
          knownArtifactCount, exactlyMatchedComponentCount, innerSourceAppIds, directDependencies);
    }
    return innerSourceAppIds;
  }

  private static void processDirectDependency(
      final DependencyNode directDependency,
      final ApplicationDAO applicationDAO,
      final InnerSourceComponentDAO innerSourceComponentDAO,
      final JsonNode bom,
      final Application currentApplication,
      final AtomicInteger knownArtifactCount,
      final AtomicInteger exactlyMatchedComponentCount,
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
            updateDependencyBomAsInnerSource(bom, dependencyId, innerSourceApp, currentApplication, knownArtifactCount,
                exactlyMatchedComponentCount);

        if (isInnerSourceDependency) {
          innerSourceAppIds.add(innerSourceApp.getId());
          processTransitiveDependencies(bom, directDependency.getChildren(), innerSourceApp, innerSourceComponentDAO,
              knownArtifactCount, exactlyMatchedComponentCount, directDependency,
              directDependency.getComponentIdentifier(), directDependencies);
        }
      }
      else {
        // a regular (non InnerSource) dependency
        updateBomNodeDependencyInformation(bom, true, false, dependencyId, null, null);
        updateDependencyInfoForComponentChildren(directDependency.getChildren(), false, false,
            directDependency.getComponentIdentifier(), bom);
      }
    }
  }

  private static void updateDependencyInfoForComponentChildren(
      final List<DependencyNode> children,
      final boolean isDirect,
      final boolean isInnerSource,
      final ComponentIdentifier parentIdentifier,
      final JsonNode bomJson)
  {
    for (DependencyNode node : children) {
      if (node != null && node.getComponentIdentifier() != null) {
        updateBomNodeDependencyInformation(bomJson, isDirect, isInnerSource, node.getComponentIdentifier(),
            parentIdentifier, null);
        if (CollectionUtils.isNotEmpty(node.getChildren())) {
          updateDependencyInfoForComponentChildren(node.getChildren(), false, isInnerSource,
              node.getComponentIdentifier(), bomJson);
        }
      }
    }
  }

  private static void updateReportSummaryWithInnerSourceResults(
      final JsonNode dataJson,
      final JsonNode summaryJson,
      final AtomicInteger knownArtifactCount,
      final AtomicInteger exactlyMatchedComponentCount)
  {
    ObjectNode dataObjectNode = (ObjectNode) dataJson;
    dataObjectNode.put(EXACTLY_MATCHED_COMPONENT_COUNT, exactlyMatchedComponentCount.intValue());
    dataObjectNode.put(KNOWN_ARTIFACT_COUNT, knownArtifactCount.intValue());

    ((ObjectNode) summaryJson).put(KNOWN_ARTIFACT_COUNT, knownArtifactCount.intValue());
  }

  private static boolean updateDependencyBomAsInnerSource(
      final JsonNode bom,
      final ComponentIdentifier innerSourceComponentIdentifier,
      final Application innerSourceApp,
      final Application currentApplication,
      final AtomicInteger knownArtifactCount,
      final AtomicInteger exactlyMatchedComponentCount)
  {
    boolean isInnerSourceDependency = false;

    for (JsonNode bomChild : bom.get("aaData")) {
      ComponentIdentifier bomComponentIdentifier = getBomComponentIdentifier(bomChild);

      if (Objects.equals(bomComponentIdentifier, innerSourceComponentIdentifier)) {
        //If the component is direct and exists as InnerSource, it needs to be updated as such
        ObjectNode bomObjectNode = (ObjectNode) bomChild;

        // If the associated app for the InnerSource component and the current app in context is the same
        // it does not need to be identified as InnerSource as it belongs to the app of the current report,
        // but it can be marked as a known component
        if (!Objects.equals(currentApplication.getId(), innerSourceApp.getId())) {
          InnerSourceData innerSourceData = new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(), null);
          updateBomNodeDependencyInformation(bom, true, true, bomComponentIdentifier, null, innerSourceData);
          isInnerSourceDependency = true;
        }

        if (MatchState.UNKNOWN.getId().equals(bomChild.get(MATCH_STATE).asText())) {
          markComponentAsKnown(bomObjectNode, bomComponentIdentifier, knownArtifactCount, exactlyMatchedComponentCount);
        }

        log.debug(isInnerSourceDependency ? "InnerSource component" : "Component" +
            "'{}' was updated in bom.json as a known component", bomComponentIdentifier);

        return isInnerSourceDependency;
      }
    }
    return false;
  }

  private static void markComponentAsKnown(
      final ObjectNode bomObjectNode,
      final ComponentIdentifier componentIdentifier,
      final AtomicInteger knownArtifactCount,
      final AtomicInteger exactlyMatchedComponentCount)
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

  private static void updateBomNodeDependencyInformation(
      final JsonNode bomJson,
      final boolean isDirect,
      final boolean isInnerSource,
      final ComponentIdentifier componentId,
      final ComponentIdentifier parentComponentId,
      final InnerSourceData innerSourceData)
  {
    for (JsonNode bomChild : bomJson.get("aaData")) {
      ComponentIdentifier bomComponentIdentifier = getBomComponentIdentifier(bomChild);
      if (Objects.equals(bomComponentIdentifier, componentId)) {
        ObjectNode bomObjectNode = (ObjectNode) bomChild;
        //At the moment we don't support multiple parents. So if a certain dependency found in multiple positions
        //of a tree we keep the first resolved relationship
        if (bomObjectNode.get("directDependency") == null) {
          bomObjectNode.put("directDependency", isDirect);
          bomObjectNode.put("innerSource", isInnerSource);
          if (!isDirect && parentComponentId != null) {
            bomObjectNode.put("parentComponentPurl", PackageUrlIdentifier.toPackageUrl(parentComponentId));
          }
          if (innerSourceData != null) {
            bomObjectNode.set("innerSourceData", JsonUtils.asTree(innerSourceData));
          }
        }
      }
    }
  }

  private static void processTransitiveDependencies(
      final JsonNode bom,
      final List<DependencyNode> transitiveDependencies,
      final Application innerSourceApp,
      final InnerSourceComponentDAO innerSourceComponentDAO,
      final AtomicInteger knownArtifactCount,
      final AtomicInteger exactlyMatchedComponentCount,
      final DependencyNode innerSourceParent,
      final ComponentIdentifier parentComponentIdentifier,
      final Set<ComponentIdentifier> directDependencies)
  {
    for (DependencyNode dependency : transitiveDependencies) {
      for (JsonNode bomChild : bom.get("aaData")) {
        ComponentIdentifier bomComponentIdentifier = getBomComponentIdentifier(bomChild);

        if (!directDependencies.contains(bomComponentIdentifier) &&
            Objects.equals(bomComponentIdentifier, dependency.getComponentIdentifier())) {
          ObjectNode bomObjectNode = (ObjectNode) bomChild;

          InnerSourceData innerSourceData = new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(),
              PackageUrlIdentifier.toPackageUrl(innerSourceParent.getComponentIdentifier()));
          updateBomNodeDependencyInformation(bom, false, false, bomComponentIdentifier,
              parentComponentIdentifier, innerSourceData);
          log.debug("Component {} associated with InnerSource app {}", bomComponentIdentifier,
              innerSourceApp.getName());

          if (MatchState.UNKNOWN.getId().equals(bomChild.get(MATCH_STATE).asText())) {
            updateUnknownTransitiveDependencyAsKnown(innerSourceComponentDAO, knownArtifactCount,
                exactlyMatchedComponentCount, bomComponentIdentifier, bomObjectNode);
          }
          break;
        }
      }
      if (CollectionUtils.isNotEmpty(dependency.getChildren())) {
        processTransitiveDependencies(bom, dependency.getChildren(), innerSourceApp, innerSourceComponentDAO,
            knownArtifactCount, exactlyMatchedComponentCount, innerSourceParent,
            dependency.getComponentIdentifier(), directDependencies);
      }
    }
  }

  private static ComponentIdentifier getBomComponentIdentifier(JsonNode bomChild) {
    ComponentIdentifier bomComponentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(bomChild);

    if (bomComponentIdentifier == null) {
      String path = StringUtils.substringAfterLast(bomChild.withArray("pathnames").get(0).asText(), "/");
      bomComponentIdentifier = ComponentIdentifierHelper.parseMavenId(path);
    }
    return bomComponentIdentifier;
  }

  private static void updateUnknownTransitiveDependencyAsKnown(
      final InnerSourceComponentDAO innerSourceComponentDAO,
      final AtomicInteger knownArtifactCount,
      final AtomicInteger exactlyMatchedComponentCount,
      final ComponentIdentifier bomComponentIdentifier,
      final ObjectNode bomObjectNode)
  {
    PackageUrlIdentifier purl = getVersionlessPackageUrl(bomComponentIdentifier);
    InnerSourceComponent is = innerSourceComponentDAO.getByPackageUrl(purl);
    if (is != null) {
      //If the component is transitive and exists as InnerSource, it needs to be updated so it can be marked as
      //Transitive dependency but not as InnerSource
      markComponentAsKnown(bomObjectNode, bomComponentIdentifier, knownArtifactCount,
          exactlyMatchedComponentCount);
      log.debug("InnerSource module {} was updated in bom.json as Transitive InnerSource",
          bomComponentIdentifier);
    }
  }
}
