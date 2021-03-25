/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @since 1.99
 */
public class ReportInnerSourceTest extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private TelemetrySender telemetrySender;

  private InnerSourceComponentDAO innerSourceComponentDAOSpy;

  private final InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();

  private Application app;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void init() {
    innerSourceComponentDAOSpy = spy(innerSourceComponentDAO);
    app = tempEntity.newApplicationWithParent();
  }

  @Override
  public void configure(Binder binder) {
    telemetrySender = mock(TelemetrySender.class);
    binder.bind(TelemetrySender.class).toInstance(telemetrySender);
    super.configure(binder);
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
    InnerSourceComponent innerSourceComponent = tempEntity
        .newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-scanner-hashing", appInnerSource);

    ComponentIdentifier knownModule1 = ComponentIdentifier
            .createMavenCoordinates("com.sonatype.insight.scan", "insight-test-reverse-proxy", "2.23.5-SNAPSHOT", "",
                "jar");
    ComponentIdentifier knownModule2 = ComponentIdentifier
            .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-model", "2.23.5-SNAPSHOT", "", "jar");
    ComponentIdentifier knownModule3 = ComponentIdentifier
            .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-model-io", "2.23.5-SNAPSHOT", "",
                "jar");
    ComponentIdentifier knownModule4 = ComponentIdentifier
            .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-core", "2.23.5-SNAPSHOT", "", "jar");

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-multi-module/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-multi-module/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-multi-module/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-multi-module/data.json");

    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender);

    List<InnerSourceComponent> innerSourceComponents = innerSourceComponentDAO.getByApplicationId(app.getId());
    assertThat(innerSourceComponents).hasSize(8);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    List<JsonNode> knownDependencies = new ArrayList<>();
    assertThat(innerSourceComponents).extracting(InnerSourceComponent::getApplicationId).containsOnly(app.getId());
    assertInnerSourceInformation(bomJson, 1, 2, bomInnerSourceParent, bomInnerSourceDependencies, knownDependencies);
    assertSummaryCounters(summaryJson, dataJson, 19);

    assertKnownComponents(knownDependencies, Arrays.asList(knownModule1, knownModule2, knownModule3, knownModule4));

    ComponentIdentifier innerSourceParent = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-hashing", "1.12.0-01", "", "jar");

    assertInnerSourceParent(bomInnerSourceParent.get(0), appInnerSource, innerSourceParent);

    assertTransitiveInnerSourceInformation(bomInnerSourceDependencies, appInnerSource);
    assertTelemetryInformation(app.getId(), Sets.newHashSet(innerSourceComponent.getApplicationId()));
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

    InnerSourceComponent model =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-module-model", appInnerSource);
    InnerSourceComponent archive = tempEntity
        .newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive", appInnerSource);
    InnerSourceComponent utils =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-client-utils", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource/data.json");

    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 3, 5, bomInnerSourceParent, bomInnerSourceDependencies);

    assertSummaryCounters(summaryJson, dataJson, 18);

    assertInnerSourceParent(bomInnerSourceParent.get(1), appInnerSource, innerSourceModel);
    assertInnerSourceParent(bomInnerSourceParent.get(2), appInnerSource, innerScannerArchive);
    assertInnerSourceParent(bomInnerSourceParent.get(0), appInnerSource, innerSourceClient);

    assertTransitiveInnerSourceInformation(bomInnerSourceDependencies, appInnerSource);

    Map<ComponentIdentifier, String> dependencyComponentNameMap = new HashMap<>();
    dependencyComponentNameMap
        .put(ComponentIdentifier.createMavenCoordinates("com.google.code.gson", "gson", "2.8.1", "", "jar"),
            "insight-scanner-archive");
    dependencyComponentNameMap
        .put(ComponentIdentifier.createMavenCoordinates("xmlpull", "xmlpull", "1.1.3.1", "", "jar"),
            "insight-module-model");
    dependencyComponentNameMap.put(
        ComponentIdentifier.createMavenCoordinates("org.seleniumhq.selenium", "selenium-leg-rc", "2.48.2", "", "jar"),
        "insight-scanner-archive");
    dependencyComponentNameMap
        .put(ComponentIdentifier.createMavenCoordinates("org.slf4j", "slf4j-api", "1.7.30", "", "jar"),
            "insight-module-model");
    dependencyComponentNameMap.put(ComponentIdentifier
            .createMavenCoordinates("org.seleniumhq.selenium", "selenium-remote-driver", "2.48.2", "", "jar"),
        "insight-scanner-archive");
    assertComponentNameForTransitiveDependencies(bomInnerSourceDependencies, dependencyComponentNameMap);

    Set<String> innerSourceIds = new HashSet<>();
    innerSourceIds.add(model.getApplicationId());
    innerSourceIds.add(archive.getApplicationId());
    innerSourceIds.add(utils.getApplicationId());
    assertTelemetryInformation(app.getId(), innerSourceIds);
  }

  @Test
  public void processInnerSource_InvalidDep() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();
    tempEntity
        .newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-scanner-hashing-asm60", appInnerSource);
    InnerSourceComponent innerSourceComponent = tempEntity
        .newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-scanner-hashing", appInnerSource);

    ComponentIdentifier knownModule1 = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-test-reverse-proxy", "2.23.5-SNAPSHOT", "",
            "jar");
    ComponentIdentifier knownModule2 = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-model", "2.23.5-SNAPSHOT", "", "jar");
    ComponentIdentifier knownModule3 = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-model-io", "2.23.5-SNAPSHOT", "",
            "jar");
    ComponentIdentifier knownModule4 = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-core", "2.23.5-SNAPSHOT", "", "jar");

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-invalid-dep/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-invalid-dep/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-invalid-dep/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-invalid-dep/data.json");

    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender);

    List<InnerSourceComponent> innerSourceComponents = innerSourceComponentDAO.getByApplicationId(app.getId());
    assertThat(innerSourceComponents).hasSize(8);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    List<JsonNode> knownDependencies = new ArrayList<>();
    assertThat(innerSourceComponents).extracting(InnerSourceComponent::getApplicationId).containsOnly(app.getId());
    assertInnerSourceInformation(bomJson, 1, 2, bomInnerSourceParent, bomInnerSourceDependencies, knownDependencies);
    assertSummaryCounters(summaryJson, dataJson, 19);

    assertKnownComponents(knownDependencies, Arrays.asList(knownModule1, knownModule2, knownModule3, knownModule4));

    ComponentIdentifier innerSourceParent = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-hashing", "1.12.0-01", "", "jar");

    assertInnerSourceParent(bomInnerSourceParent.get(0), appInnerSource, innerSourceParent);

    assertTransitiveInnerSourceInformation(bomInnerSourceDependencies, appInnerSource);
    assertTelemetryInformation(app.getId(), Sets.newHashSet(innerSourceComponent.getApplicationId()));
  }

  @Test
  public void processInnerSource_knownInnerSourceParent() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    InnerSourceComponent model =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-module-model", appInnerSource);
    InnerSourceComponent archive = tempEntity
        .newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive", appInnerSource);
    InnerSourceComponent utils =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-client-utils", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-known/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-known/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-known/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-known/data.json");

    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 3, 5, bomInnerSourceParent, bomInnerSourceDependencies);

    assertSummaryCounters(summaryJson, dataJson, 18);

    AnalyzerFeatures analyzerFeaturesSonatype =
        new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.HASH, "mvn", true, true, true);
    assertIdentificationSourceAndAnalyzerFeatures(bomInnerSourceParent.get(1), IdentificationSource.SONATYPE.getId(),
        analyzerFeaturesSonatype);
    AnalyzerFeatures analyzerFeaturesPackageManifest =
        new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.COORDINATE, "mvn");
    assertIdentificationSourceAndAnalyzerFeatures(bomInnerSourceParent.get(0),
        IdentificationSource.PACKAGE_MANIFEST.getId(),
        analyzerFeaturesPackageManifest);

    assertTransitiveInnerSourceInformation(bomInnerSourceDependencies, appInnerSource);

    Set<String> innerSourceIds = new HashSet<>();
    innerSourceIds.add(model.getApplicationId());
    innerSourceIds.add(archive.getApplicationId());
    innerSourceIds.add(utils.getApplicationId());
    assertTelemetryInformation(app.getId(), innerSourceIds);
  }

  public void assertKnownComponents(
      List<JsonNode> knownDependencies,
      List<ComponentIdentifier> expectedKnownComponents)
  {
    List<ComponentIdentifier> knownComponents =
        knownDependencies.stream().map(ComponentIdentifierAdapter::getComponentIdentifier)
            .collect(Collectors.toList());
    assertThat(knownComponents).containsAll(expectedKnownComponents);
  }

  private void assertInnerSourceInformation(
      final JsonNode bomJson,
      int expectedISComponents,
      int expectedISDependencies,
      List<JsonNode> bomInnerSourceParent,
      List<JsonNode> bomInnerSourceDependencies)
  {
    assertInnerSourceInformation(bomJson, expectedISComponents, expectedISDependencies, bomInnerSourceParent,
        bomInnerSourceDependencies, null);
  }

  private void assertInnerSourceInformation(
      final JsonNode bomJson,
      int expectedISComponents,
      int expectedISDependencies,
      List<JsonNode> bomInnerSourceParent,
      List<JsonNode> bomInnerSourceDependencies,
      List<JsonNode> knownDependencies)
  {
    if (bomInnerSourceParent == null) {
      bomInnerSourceParent = new ArrayList<>();
    }
    if (bomInnerSourceDependencies == null) {
      bomInnerSourceDependencies = new ArrayList<>();
    }

    for (JsonNode bomChild : bomJson.get("aaData")) {
      JsonNode innerSourceData = bomChild.get("innerSourceData");
      if (innerSourceData != null) {
        JsonNode innerSourceNodeParent = innerSourceData.get("innerSource");
        if (innerSourceNodeParent != null && innerSourceNodeParent.asBoolean()) {
          bomInnerSourceParent.add(bomChild);
        }
        else {
          bomInnerSourceDependencies.add(bomChild);
        }
      }
      else {
        JsonNode matchState = bomChild.get("matchState");
        if (knownDependencies != null && MatchState.getById(matchState.asText()) == MatchState.EXACT) {
          knownDependencies.add(bomChild);
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

    InnerSourceComponent model =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-module-model", appInnerSource);
    InnerSourceComponent archive = tempEntity
        .newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive", appInnerSource);
    InnerSourceComponent utils =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-client-utils", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-nested-transitive/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-nested-transitive/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-nested-transitive/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-nested-transitive/data.json");

    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender);

    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 3, 15, null, bomInnerSourceDependencies);

    assertTransitiveInnerSourceInformation(bomInnerSourceDependencies, appInnerSource);
    assertSummaryCounters(summaryJson, dataJson, 25);

    Set<String> innerSourceIds = new HashSet<>();
    innerSourceIds.add(model.getApplicationId());
    innerSourceIds.add(archive.getApplicationId());
    innerSourceIds.add(utils.getApplicationId());
    assertTelemetryInformation(app.getId(), innerSourceIds);
  }

  @Test
  public void processInnerSource_unknown_components() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    InnerSourceComponent model =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.insight.scan/insight-module-model", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-unknown-components/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-unknown-components/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-unknown-components/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-unknown-components/data.json");

    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender);

    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 1, 1, null, bomInnerSourceDependencies);

    assertTransitiveInnerSourceInformation(bomInnerSourceDependencies, appInnerSource);
    assertSummaryCounters(summaryJson, dataJson, 1);

    assertTelemetryInformation(app.getId(), Sets.newHashSet(model.getApplicationId()));
  }

  @Test
  public void processInnerSource_Without_DependencyTree() throws Exception {

    Application app = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.data/innersource-data", app);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-not-root/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-not-root/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-not-root/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-not-root/data.json");

    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender);

    assertInnerSourceInformation(bomJson, 0, 0, null, null);
    assertSummaryCounters(summaryJson, dataJson, 3);

    verify(telemetrySender, never()).send(Mockito.any(TelemetryData.class));
  }

  @Test
  public void testProcessInnerSourceDependencies_without_children() throws Exception {

    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.data/innersource-data", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-not-children/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-not-children/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-not-children/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-not-children/data.json");

    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 0, 0, bomInnerSourceParent, null);

    assertThat(summaryJson).isNotNull();
    assertThat(summaryJson.get("knownArtifactCount").asInt()).isEqualTo(3);

    assertThat(dataJson).isNotNull();
    assertThat(dataJson.get("exactlyMatchedComponentCount").asInt()).isEqualTo(3);
    assertThat(dataJson.get("knownArtifactCount").asInt()).isEqualTo(3);

    assertSummaryCounters(summaryJson, dataJson, 3);
    verify(telemetrySender, never()).send(Mockito.any(TelemetryData.class));
  }

  @Test
  public void testProcessInnerSourceDependencies_dependencyIsTransitiveAndDirect() throws Exception {

    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceComponent("pkg:maven/org.example/ACME-data", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:maven/org.example/ACME-business", appInnerSource);
    InnerSourceComponent innerSourceComponent =
        tempEntity.newInnerSourceComponent("pkg:maven/org.example/ACME-Producer", appInnerSource);

    JsonNode dependenciesJson =
        getJsonNodeInformation("report-innersource-direct-transitive-dependency/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-direct-transitive-dependency/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-direct-transitive-dependency/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-direct-transitive-dependency/data.json");

    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender);

    List<InnerSourceComponent> innerSourceComponents = innerSourceComponentDAO.getByApplicationId(app.getId());
    assertThat(innerSourceComponents).hasSize(2);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    List<JsonNode> knownDependencies = new ArrayList<>();
    assertThat(innerSourceComponents).extracting(InnerSourceComponent::getApplicationId).containsOnly(app.getId());
    assertInnerSourceInformation(bomJson, 1, 2, bomInnerSourceParent, bomInnerSourceDependencies, knownDependencies);
    assertSummaryCounters(summaryJson, dataJson, 5);

    ComponentIdentifier directDep = ComponentIdentifier
        .createMavenCoordinates("javax.inject", "javax.inject", "1", "", "jar");
    assertKnownComponents(knownDependencies, Collections.singletonList(directDep));

    ComponentIdentifier innerSourceParent = ComponentIdentifier
        .createMavenCoordinates("org.example", "ACME-business", "1.0-SNAPSHOT", "", "jar");

    assertInnerSourceParent(bomInnerSourceParent.get(0), appInnerSource, innerSourceParent);

    assertTransitiveInnerSourceInformation(bomInnerSourceDependencies, appInnerSource);
    assertTelemetryInformation(app.getId(), Sets.newHashSet(innerSourceComponent.getApplicationId()));
  }

  @Test
  public void testProcessInnerSourceDependencies_producer_not_exists() throws Exception {

    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-not-children/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-not-children/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-not-children/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-not-children/data.json");

    ReportInnerSource.processDependencyTree(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender);

    assertInnerSourceInformation(bomJson, 0, 0, null, null);
    assertSummaryCounters(summaryJson, dataJson, 3);

    verify(telemetrySender, never()).send(Mockito.any(TelemetryData.class));
  }

  private void assertInnerSourceParent(
      JsonNode bomInnerSource,
      Application app,
      ComponentIdentifier componentIdentifier) throws Exception
  {
    assertThat(bomInnerSource).isNotNull();
    assertThat(bomInnerSource.get("componentIdentifier")).isNotNull();
    assertThat(bomInnerSource.get("displayName")).isNotNull();
    assertThat(bomInnerSource.get("matchState").asText()).isEqualTo(MatchState.EXACT.getId());

    assertThat(bomInnerSource.get(ComponentIdentifier.MAVEN_GROUP_ID).asText()).isNotNull();
    assertThat(bomInnerSource.get(ComponentIdentifier.MAVEN_ARTIFACT_ID).asText()).isNotNull();
    assertThat(bomInnerSource.get(ComponentIdentifier.VERSION).asText()).isNotNull();

    assertThat(componentIdentifier).isEqualTo(ComponentIdentifierAdapter.getComponentIdentifier(bomInnerSource));

    AnalyzerFeatures analyzerFeaturesExpected =
        new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.COORDINATE, "mvn");
    assertIdentificationSourceAndAnalyzerFeatures(bomInnerSource, IdentificationSource.PACKAGE_MANIFEST.getId(),
        analyzerFeaturesExpected);
    assertInnerSourceTree(bomInnerSource, app, componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
  }

  private void assertIdentificationSourceAndAnalyzerFeatures(
      JsonNode bomInnerSource,
      String identificationSource,
      AnalyzerFeatures analyzerFeaturesExpected) throws Exception
  {
    assertThat(bomInnerSource.get("identificationSource").asText()).isEqualTo(identificationSource);
    AnalyzerFeatures analyzerFeaturesInBom =
        JsonUtils.asPojo(bomInnerSource.get("analyzerFeatures"), AnalyzerFeatures.class);
    assertThat(analyzerFeaturesInBom).usingRecursiveComparison().isEqualTo(analyzerFeaturesExpected);
  }

  private void assertTransitiveInnerSourceInformation(
      final List<JsonNode> bomInnerSourceDependencies,
      final Application appInnerSource)
  {
    for (JsonNode transitiveDependencies : bomInnerSourceDependencies) {
      assertThat(transitiveDependencies).isNotNull();
      assertThat(transitiveDependencies.get("componentIdentifier")).isNotNull();
      assertThat(transitiveDependencies.get("innerSourceData").get("ownerApplicationName").asText())
          .isEqualTo(appInnerSource.getName());
      assertThat(transitiveDependencies.get("innerSourceData").get("ownerApplicationId").asText())
          .isEqualTo(appInnerSource.getId());
    }
  }

  private void assertComponentNameForTransitiveDependencies(
      final List<JsonNode> bomInnerSourceDependencies,
      final Map<ComponentIdentifier, String> dependencyComponentNameMap)
  {
    for (JsonNode transitiveDependencies : bomInnerSourceDependencies) {
      ComponentIdentifier componentIdentifier =
          ComponentIdentifierAdapter.getComponentIdentifier(transitiveDependencies);
      String expectedComponentName = dependencyComponentNameMap.get(componentIdentifier);
      assertThat(transitiveDependencies.get("innerSourceData").get("ownerComponentName").asText())
          .isEqualTo(expectedComponentName);
    }
  }

  private void assertTelemetryInformation(String consumerId, Set<String> innerSourceComponents) {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.INNER_SOURCE_REPORT_USAGE);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).hasSize(1);

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put(InnerSourceReportUsageTelemetry.ATTRIBUTE_NAME,
        new InnerSourceReportUsageTelemetry(consumerId, innerSourceComponents));

    assertThat(telemetryData.getAttributes().keySet().iterator().next())
        .isEqualTo(expectedAttributes.keySet().iterator().next());
    assertThat((InnerSourceReportUsageTelemetry) telemetryData.getAttributes().values().iterator().next())
        .usingRecursiveComparison().isEqualTo(expectedAttributes.values().iterator().next());
  }

  private void assertInnerSourceTree(
      JsonNode innerSourceNode,
      Application app,
      String innerSourceComponentName)
      throws IOException
  {
    InnerSourceData expectedInnerSourceData =
        new InnerSourceData(app.getName(), app.getId(), innerSourceComponentName, true);
    InnerSourceData innerSourceDataInBom =
        JsonUtils.asPojo(innerSourceNode.get("innerSourceData"), InnerSourceData.class);
    assertThat(innerSourceDataInBom).usingRecursiveComparison().isEqualTo(expectedInnerSourceData);
  }

  private JsonNode getJsonNodeInformation(String path) throws IOException {
    return objectMapper.readTree(getClass().getResource("/InnerSourceServiceTest/" + path));
  }
}
