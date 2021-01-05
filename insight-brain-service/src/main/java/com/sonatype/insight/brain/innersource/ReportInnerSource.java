/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
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
        if (tree != null && tree.getComponentIdentifier() != null) {
          InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();
          boolean isValidRootArtifact =
              saveInnerSourceComponent(tree.getComponentIdentifier(), application.getId(), innerSourceComponentDAO);
          if (isValidRootArtifact) {
            processInnerSourceDependencies(tree.getChildren(), bomJson, dataJson, summaryJson, application,
                innerSourceComponentDAO, telemetrySender);
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
      for (DependencyNode dependencyChild : children) {
        if (dependencyChild.isModule()) {
          Set<String> moduleInnerSourceAppIds = associateModuleToApp(dependencyChild, applicationDAO,
              innerSourceComponentDAO, bomJson, application, knownArtifactCount, exactlyMatchedComponentCount);
          innerSourceAppIds.addAll(moduleInnerSourceAppIds);
        }
        else if (dependencyChild.isDirect()) {
          processDirectDependency(dependencyChild, applicationDAO, innerSourceComponentDAO, bomJson,
              application, knownArtifactCount, exactlyMatchedComponentCount, innerSourceAppIds);
        }
      }
      updateReportSummaryWithInnerSourceResults(dataJson, summaryJson, knownArtifactCount,
          exactlyMatchedComponentCount);

      sendTelemetryData(application.getId(), innerSourceAppIds, telemetrySender);
    }
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
      final AtomicInteger exactlyMatchedComponentCount)
  {
    ComponentIdentifier moduleComponent = moduleDependency.getComponentIdentifier();
    log.debug("InnerSource module '{}' found", moduleComponent);

    saveInnerSourceComponent(moduleComponent, currentApplication.getId(), innerSourceComponentDAO);

    Set<String> innerSourceAppIds = new HashSet<>();
    for (DependencyNode directDependencyChild : moduleDependency.getChildren()) {
      processDirectDependency(directDependencyChild, applicationDAO, innerSourceComponentDAO, bom, currentApplication,
          knownArtifactCount, exactlyMatchedComponentCount, innerSourceAppIds);
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
      final Set<String> innerSourceAppIds)
  {
    ComponentIdentifier parentComponent = directDependency.getComponentIdentifier();

    ComponentIdentifier simplifiedComponent =
        ComponentIdentifier.createMavenCoordinates(parentComponent.get(ComponentIdentifier.MAVEN_GROUP_ID),
            parentComponent.get(ComponentIdentifier.MAVEN_ARTIFACT_ID), null);

    InnerSourceComponent innerSourceComponent =
        innerSourceComponentDAO.getByPackageUrl(PackageUrlIdentifier.fromComponentIdentifier(simplifiedComponent));

    if (innerSourceComponent != null) {
      Application innerSourceApp = applicationDAO.getByIdNotNull(innerSourceComponent.getApplicationId());

      boolean isInnerSourceDependency =
          updateDependencyBomAsInnerSource(bom, parentComponent, innerSourceApp, currentApplication, knownArtifactCount,
              exactlyMatchedComponentCount);

      if (isInnerSourceDependency) {
        innerSourceAppIds.add(innerSourceApp.getId());

        List<DependencyNode> childrenComponents = getAllTransitiveDependencies(directDependency.getChildren());

        log.info("InnerSource component found '{}' with {} transitive dependencies", parentComponent,
            childrenComponents.size());
        processTransitiveDependencies(bom, childrenComponents, innerSourceApp, innerSourceComponentDAO,
            knownArtifactCount, exactlyMatchedComponentCount, directDependency);
      }
    }
  }

  private static List<DependencyNode> getAllTransitiveDependencies(List<DependencyNode> children) {
    List<DependencyNode> result = new LinkedList<>();
    for (DependencyNode child : children) {
      result.add(child);
      if (!child.getChildren().isEmpty()) {
        result.addAll(getAllTransitiveDependencies(child.getChildren()));
      }
    }
    return result;
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
          String innerSourceComponentName = getComponentName(innerSourceComponentIdentifier);
          InnerSourceData innerSourceData =
              new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(), innerSourceComponentName, true);
          bomObjectNode.set("innerSourceData", JsonUtils.asTree(innerSourceData));
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

  private static void processTransitiveDependencies(
      final JsonNode bom,
      final List<DependencyNode> transitiveDependencies,
      final Application innerSourceApp,
      final InnerSourceComponentDAO innerSourceComponentDAO,
      final AtomicInteger knownArtifactCount,
      final AtomicInteger exactlyMatchedComponentCount,
      final DependencyNode parentDependency)
  {
    for (DependencyNode dependency : transitiveDependencies) {
      for (JsonNode bomChild : bom.get("aaData")) {
        ComponentIdentifier bomComponentIdentifier = getBomComponentIdentifier(bomChild);

        if (Objects.equals(bomComponentIdentifier, dependency.getComponentIdentifier())) {
          ObjectNode bomObjectNode = (ObjectNode) bomChild;

          String innerSourceComponentName = getComponentName(parentDependency.getComponentIdentifier());
          InnerSourceData innerSourceData =
              new InnerSourceData(innerSourceApp.getName(), innerSourceApp.getId(), innerSourceComponentName);
          bomObjectNode.set("innerSourceData", JsonUtils.asTree(innerSourceData));
          log.debug("Component {} associated with InnerSource app {}", bomComponentIdentifier,
              innerSourceApp.getName());

          if (MatchState.UNKNOWN.getId().equals(bomChild.get(MATCH_STATE).asText())) {
            updateUnknownTransitiveDependencyAsKnown(innerSourceComponentDAO, knownArtifactCount,
                exactlyMatchedComponentCount, bomComponentIdentifier, bomObjectNode);
          }
          break;
        }
      }
    }
  }

  private static String getComponentName(ComponentIdentifier componentIdentifier) {
    PackageUrlIdentifier packageUrlIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    return packageUrlIdentifier.getName();
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
