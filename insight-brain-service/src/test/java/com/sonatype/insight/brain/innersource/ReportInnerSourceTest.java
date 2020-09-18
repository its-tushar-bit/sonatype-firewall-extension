/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @since 1.99
 */
public class ReportInnerSourceTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private InnerSourceComponentDAO innerSourceComponentDAOSpy;

  private final InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();

  private Application app;

  @Before
  public void init() {
    innerSourceComponentDAOSpy = spy(innerSourceComponentDAO);
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void processInnerSource_createInnerSourceParent() throws Exception {
    JsonNode dependenciesJson =
        new ObjectMapper()
            .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource/dependencies.json"));
    JsonNode dependencyTree = dependenciesJson.path("dependencyTree");

    ComponentIdentifier rootComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.nexus", "nexus-platform-api", "1.0.0", "", "jar");

    ReportInnerSource.saveInnerSourceComponent(rootComponentIdentifier, app.getId(), innerSourceComponentDAO);

    List<InnerSourceComponent> innerSourceComponents = innerSourceComponentDAOSpy.getByApplicationId(app.getId());
    assertThat(innerSourceComponents).hasSize(1);

    assertThat(innerSourceComponents.get(0).getApplicationId()).isEqualTo(app.getId());

    assertThat(dependenciesJson).isNotNull();
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(dependencyTree);
    PackageUrlIdentifier expectedPurl = new PackageUrlIdentifier(String.format("pkg:maven/%s/%s",
        componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
        componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID)));

    assertThat(innerSourceComponents.get(0).getPackageUrl()).isEqualTo(expectedPurl.getPackageUrl());
  }

  @Test
  public void processInnerSource_checkInnerSourceParent() {
    InnerSourceComponent innerSourceComponent =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    ComponentIdentifier rootComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.innersource.main", "innersource-main", "1.0.0", "", "jar");
    ReportInnerSource.saveInnerSourceComponent(rootComponentIdentifier, app.getId(), innerSourceComponentDAO);

    verify(innerSourceComponentDAOSpy, never()).insert(innerSourceComponent);
    verify(innerSourceComponentDAOSpy, never()).update(innerSourceComponent);
  }

  @Test
  public void processInnerSource_updateInnerSourceParent() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    app = tempEntity.newApplicationWithParent();

    ComponentIdentifier rootComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.nexus", "nexus-platform-api", "1.0.0", "", "jar");
    ReportInnerSource.saveInnerSourceComponent(rootComponentIdentifier, app.getId(), innerSourceComponentDAOSpy);
    ArgumentCaptor<InnerSourceComponent> argument = ArgumentCaptor.forClass(InnerSourceComponent.class);
    verify(innerSourceComponentDAOSpy).update(argument.capture());
    assertThat(argument.getValue().getApplicationId()).isEqualTo(app.getId());
  }

  @Test
  public void processInnerSource_updateInnerSourceParent_sameApp() {
    InnerSourceComponent innerSourceComponent =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    ComponentIdentifier rootComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.nexus", "nexus-platform-api", "1.0.0", "", "jar");
    ReportInnerSource.saveInnerSourceComponent(rootComponentIdentifier, app.getId(), innerSourceComponentDAOSpy);
    verify(innerSourceComponentDAOSpy, never()).update(innerSourceComponent);
  }

  @Test
  public void processInnerSource_noInnerSourceParent() {
    assertThat(ReportInnerSource.saveInnerSourceComponent(null, app.getId(), innerSourceComponentDAO)).isFalse();

    List<InnerSourceComponent> innerSourceComponents = innerSourceComponentDAO.getByApplicationId(app.getId());
    assertThat(innerSourceComponents).isEmpty();
  }

  @Test
  public void processInnerSource_multiModule() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();
    tempEntity
        .newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-scanner-hashing-asm60", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-scanner-hashing", appInnerSource);

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson = objectMapper
        .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-multi-module/dependencies.json"));
    JsonNode bomJson =
        objectMapper
            .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-multi-module/bom.json"));
    JsonNode summaryJson = objectMapper
        .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-multi-module/summary.json"));
    JsonNode dataJson =
        objectMapper
            .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-multi-module/data.json"));
    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app);

    List<InnerSourceComponent> innerSourceComponents = innerSourceComponentDAO.getByApplicationId(app.getId());
    assertThat(innerSourceComponents).hasSize(8);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertThat(innerSourceComponents).extracting(InnerSourceComponent::getApplicationId).containsOnly(app.getId());
    assertInnerSourceInformation(bomJson, 1, 2, bomInnerSourceParent, bomInnerSourceDependencies);
    assertSummaryCounters(summaryJson, dataJson, 15);

    for (JsonNode bom : bomInnerSourceParent) {
      assertInnerSourceParent(bom, appInnerSource, null);
    }

    for (JsonNode transitiveDependencies : bomInnerSourceDependencies) {
      assertThat(transitiveDependencies).isNotNull();
      assertThat(transitiveDependencies.get("ownerApplicationName").asText()).isEqualTo(appInnerSource.getName());
      assertThat(transitiveDependencies.get("componentIdentifier")).isNotNull();
    }
  }

  @Test
  public void processInnerSource_singleModule() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    ComponentIdentifier innerSourceModel = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-module-model", "1.0.0-SNAPSHOT", "", "jar");
    ComponentIdentifier innerScannerArchive = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-archive", "1.0.0-SNAPSHOT", "", "jar");
    ComponentIdentifier innerSourceClient = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-client-utils", "1.0.0-SNAPSHOT", "",
            "jar");

    List<ComponentIdentifier> componentIdentifiers =
        Arrays.asList(innerSourceModel, innerScannerArchive, innerSourceClient);

    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-module-model", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-client-utils", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson =
        objectMapper.readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource/dependencies.json"));
    JsonNode bomJson =
        objectMapper.readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource/bom.json"));
    JsonNode summaryJson =
        objectMapper.readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource/summary.json"));
    JsonNode dataJson =
        objectMapper.readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource/data.json"));
    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 3, 5, bomInnerSourceParent, bomInnerSourceDependencies);

    assertSummaryCounters(summaryJson, dataJson, 18);

    for (JsonNode bom : bomInnerSourceParent) {
      assertInnerSourceParent(bom, appInnerSource, componentIdentifiers);
    }

    for (JsonNode transitiveDependencies : bomInnerSourceDependencies) {
      assertThat(transitiveDependencies).isNotNull();
      assertThat(transitiveDependencies.get("ownerApplicationName").asText()).isEqualTo(appInnerSource.getName());
      assertThat(transitiveDependencies.get("componentIdentifier")).isNotNull();
    }
  }

  private void assertInnerSourceInformation(
      final JsonNode bomJson,
      int expectedISComponents,
      int expectedISDependencies,
      List<JsonNode> bomInnerSourceParent,
      List<JsonNode> bomInnerSourceDependencies)
  {
    if (bomInnerSourceParent == null) {
      bomInnerSourceParent = new ArrayList<>();
    }
    if (bomInnerSourceDependencies == null) {
      bomInnerSourceDependencies = new ArrayList<>();
    }

    for (JsonNode bomChild : bomJson.get("aaData")) {
      JsonNode innerSourceNode = bomChild.get("ownerApplicationName");
      if (innerSourceNode != null) {
        JsonNode innerSourceNodeParent = bomChild.get("innerSource");
        if (innerSourceNodeParent != null && innerSourceNodeParent.asBoolean()) {
          bomInnerSourceParent.add(bomChild);
        }
        else {
          bomInnerSourceDependencies.add(bomChild);
        }
      }
    }
    assertThat(bomInnerSourceParent).hasSize(expectedISComponents);
    assertThat(bomInnerSourceDependencies).hasSize(expectedISDependencies);
  }

  private void assertSummaryCounters(JsonNode summaryJson, JsonNode dataJson, int expectedCount) {
    assertThat(summaryJson).isNotNull();
    assertThat(summaryJson.get("knownArtifactCount").asInt()).isEqualTo(expectedCount);

    assertThat(dataJson).isNotNull();
    assertThat(dataJson.get("exactlyMatchedComponentCount").asInt()).isEqualTo(expectedCount);
    assertThat(dataJson.get("knownArtifactCount").asInt()).isEqualTo(expectedCount);
  }

  @Test
  public void processInnerSource_nested_transitive_dep() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-module-model", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-client-utils", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson = objectMapper
        .readTree(
            getClass().getResource("/InnerSourceServiceTest/report-innersource-nested-transitive/dependencies.json"));
    JsonNode bomJson = objectMapper
        .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-nested-transitive/bom.json"));
    JsonNode summaryJson = objectMapper
        .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-nested-transitive/summary.json"));
    JsonNode dataJson = objectMapper
        .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-nested-transitive/data.json"));
    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app);

    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 3, 15, null, bomInnerSourceDependencies);

    for (JsonNode transitiveDependencies : bomInnerSourceDependencies) {
      assertThat(transitiveDependencies).isNotNull();
      assertThat(transitiveDependencies.get("ownerApplicationName").asText()).isEqualTo(appInnerSource.getName());
      assertThat(transitiveDependencies.get("componentIdentifier")).isNotNull();
    }
    assertSummaryCounters(summaryJson, dataJson, 25);
  }

  @Test
  public void processInnerSource_unknown_components() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-module-model", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson = objectMapper
        .readTree(
            getClass().getResource("/InnerSourceServiceTest/report-innersource-unknown-components/dependencies.json"));
    JsonNode bomJson = objectMapper
        .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-unknown-components/bom.json"));
    JsonNode summaryJson = objectMapper
        .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-unknown-components/summary.json"));
    JsonNode dataJson = objectMapper
        .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-unknown-components/data.json"));
    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app);

    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 1, 1, null, bomInnerSourceDependencies);

    for (JsonNode transitiveDependencies : bomInnerSourceDependencies) {
      assertThat(transitiveDependencies).isNotNull();
      assertThat(transitiveDependencies.get("ownerApplicationName").asText()).isEqualTo(appInnerSource.getName());
      assertThat(transitiveDependencies.get("componentIdentifier")).isNotNull();
    }
    assertSummaryCounters(summaryJson, dataJson, 1);
  }

  @Test
  public void processInnerSource_Without_DependencyTree() throws Exception {

    Application app = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.data/innersource-data", app);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson =
        new ObjectMapper()
            .readTree(
                getClass().getResource(
                    "/InnerSourceServiceTest/report-innersource-not-root/dependencies.json"));
    JsonNode summaryJson =
        objectMapper.readTree(
            getClass().getResource(
                "/InnerSourceServiceTest/report-innersource-not-root/summary.json"));
    JsonNode bomJson =
        objectMapper.readTree(
            getClass().getResource(
                "/InnerSourceServiceTest/report-innersource-not-root/bom.json"));
    JsonNode dataJson =
        objectMapper.readTree(
            getClass().getResource(
                "/InnerSourceServiceTest/report-innersource-not-root/data.json"));
    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app);

    assertInnerSourceInformation(bomJson, 0, 0, null, null);
    assertSummaryCounters(summaryJson, dataJson, 3);
  }

  @Test
  public void testProcessInnerSourceDependencies_without_children() throws Exception {

    Application appInnerSource = tempEntity.newApplicationWithParent();

    ComponentIdentifier innerSourceData = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.innersource.data", "innersource-data", "1.0.0-SNAPSHOT", "", "jar");

    List<ComponentIdentifier> componentIdentifiers = Collections.singletonList(innerSourceData);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.data/innersource-data", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson =
        objectMapper.readTree(getClass()
            .getResource(
                "/InnerSourceServiceTest/report-innersource-not-children/dependencies.json"));
    JsonNode bomJson =
        objectMapper.readTree(
            getClass().getResource(
                "/InnerSourceServiceTest/report-innersource-not-children/bom.json"));
    JsonNode summaryJson =
        objectMapper.readTree(
            getClass().getResource(
                "/InnerSourceServiceTest/report-innersource-not-children/summary.json"));
    JsonNode dataJson =
        objectMapper.readTree(
            getClass().getResource(
                "/InnerSourceServiceTest/report-innersource-not-children/data.json"));
    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 0, 0, bomInnerSourceParent, null);

    assertThat(summaryJson).isNotNull();
    assertThat(summaryJson.get("knownArtifactCount").asInt()).isEqualTo(3);

    assertThat(dataJson).isNotNull();
    assertThat(dataJson.get("exactlyMatchedComponentCount").asInt()).isEqualTo(3);
    assertThat(dataJson.get("knownArtifactCount").asInt()).isEqualTo(3);

    for (JsonNode bom : bomInnerSourceParent) {
      assertInnerSourceParent(bom, appInnerSource, componentIdentifiers);
    }
    assertSummaryCounters(summaryJson, dataJson, 3);
  }

  @Test
  public void testProcessInnerSourceDependencies_producer_not_exists() throws Exception {

    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);
    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson = objectMapper
        .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-not-children/dependencies.json"));
    JsonNode bomJson =
        objectMapper
            .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-not-children/bom.json"));
    JsonNode summaryJson = objectMapper
        .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-not-children/summary.json"));
    JsonNode dataJson =
        objectMapper
            .readTree(getClass().getResource("/InnerSourceServiceTest/report-innersource-not-children/data.json"));
    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app);

    assertInnerSourceInformation(bomJson, 0, 0, null, null);
    assertSummaryCounters(summaryJson, dataJson, 3);
  }

  private void assertInnerSourceParent(
      JsonNode bomInnerSource,
      Application app,
      List<ComponentIdentifier> componentIdentifiers) throws Exception
  {
    assertThat(bomInnerSource).isNotNull();
    assertThat(bomInnerSource.get("componentIdentifier")).isNotNull();
    assertThat(bomInnerSource.get("displayName")).isNotNull();
    assertThat(bomInnerSource.get("innerSource").asBoolean()).isTrue();
    assertThat(bomInnerSource.get("matchState").asText()).isEqualTo(MatchState.EXACT.getId());
    assertThat(bomInnerSource.get("identificationSource").asText())
        .isEqualTo(IdentificationSource.PACKAGE_MANIFEST.getId());

    assertThat(bomInnerSource.get(ComponentIdentifier.MAVEN_GROUP_ID).asText()).isNotNull();
    assertThat(bomInnerSource.get(ComponentIdentifier.MAVEN_ARTIFACT_ID).asText()).isNotNull();
    assertThat(bomInnerSource.get(ComponentIdentifier.VERSION).asText()).isNotNull();

    if (componentIdentifiers != null) {
      assertThat(componentIdentifiers).contains(ComponentIdentifierAdapter.getComponentIdentifier(bomInnerSource));
    }

    AnalyzerFeatures analyzerFeaturesInBom =
        JsonUtils.asPojo(bomInnerSource.get("analyzerFeatures"), AnalyzerFeatures.class);
    AnalyzerFeatures analyzerFeaturesExpected =
        new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.COORDINATE, "mvn");
    assertThat(analyzerFeaturesInBom).usingRecursiveComparison().isEqualTo(analyzerFeaturesExpected);
    assertThat(bomInnerSource.get("ownerApplicationName").asText()).isEqualTo(app.getName());
  }
}
