/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
import com.sonatype.insight.brain.innersource.InnerSourceReportUsageTelemetry;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import io.dropwizard.util.Maps;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class DependencyResolverTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private TelemetrySender telemetrySender;

  private InnerSourceComponentDAO innerSourceComponentDAOSpy;

  private ApplicationDAO applicationDAO;

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

    newDependencyResolver().saveInnerSourceComponent(rootComponentIdentifier);

    List<InnerSourceComponent> innerSourceComponents = innerSourceComponentDAOSpy.getByApplicationId(app.getId());
    assertThat(innerSourceComponents).hasSize(1);

    assertThat(innerSourceComponents.get(0).getApplicationId()).isEqualTo(app.getId());

    assertThat(dependenciesJson).isNotNull();
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(dependencyTree);
    PackageUrlIdentifier expectedPurl = new PackageUrlIdentifier(String.format("pkg:maven/%s/%s",
        componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
        componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID)));

    assertThat(innerSourceComponents.get(0).getPackageUrl()).isEqualTo(expectedPurl.getPackageUrl());
    assertThat(innerSourceComponents.get(0).getLatestVersion()).isEqualTo("1.0.0");
  }

  @Test
  public void processInnerSource_checkInnerSourceParent() {
    InnerSourceComponent innerSourceComponent =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    ComponentIdentifier rootComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.innersource.main", "innersource-main", "1.0.0", "", "jar");

    newDependencyResolver().saveInnerSourceComponent(rootComponentIdentifier);

    verify(innerSourceComponentDAOSpy, never()).insert(innerSourceComponent);
    verify(innerSourceComponentDAOSpy, never()).update(innerSourceComponent);
  }

  @Test
  public void processInnerSource_updateInnerSourceParent() {
    Application innerSourceApp = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", innerSourceApp);

    ComponentIdentifier rootComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.nexus", "nexus-platform-api", "1.0.0", "", "jar");

    newDependencyResolver().saveInnerSourceComponent(rootComponentIdentifier);

    ArgumentCaptor<InnerSourceComponent> argument = ArgumentCaptor.forClass(InnerSourceComponent.class);
    verify(innerSourceComponentDAOSpy).update(argument.capture());

    InnerSourceComponent innerSourceComponent = argument.getValue();
    assertThat(innerSourceComponent.getApplicationId()).isEqualTo(app.getId());
    // The original component has null as version, now it should have a value there
    assertThat(innerSourceComponent.getLatestVersion()).isEqualTo("1.0.0");
  }

  @Test
  public void processInnerSource_updateInnerSourceParentWithOlderVersion() {
    Application innerSourceApp = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", innerSourceApp, "1.0.1");

    ComponentIdentifier rootComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.sonatype.nexus", "nexus-platform-api", "1.0.0", "", "jar");

    newDependencyResolver().saveInnerSourceComponent(rootComponentIdentifier);

    ArgumentCaptor<InnerSourceComponent> argument = ArgumentCaptor.forClass(InnerSourceComponent.class);
    verify(innerSourceComponentDAOSpy).update(argument.capture());

    InnerSourceComponent innerSourceComponent = argument.getValue();
    assertThat(innerSourceComponent.getApplicationId()).isEqualTo(app.getId());
    assertThat(innerSourceComponent.getLatestVersion()).isEqualTo("1.0.1");
  }

  @Test
  public void processInnerSource_updateInnerSourceParent_sameApp() {
    InnerSourceComponent innerSourceComponent =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    ComponentIdentifier rootComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.nexus", "nexus-platform-api", "1.0.0", "", "jar");

    newDependencyResolver().saveInnerSourceComponent(rootComponentIdentifier);

    verify(innerSourceComponentDAOSpy, never()).update(innerSourceComponent);
  }

  @Test
  public void processInnerSource_noInnerSourceParent() {
    assertThat(newDependencyResolver().saveInnerSourceComponent(null)).isFalse();

    List<InnerSourceComponent> innerSourceComponents = innerSourceComponentDAO.getByApplicationId(app.getId());
    assertThat(innerSourceComponents).isEmpty();
  }

  @Test
  public void processInnerSource_multiModule_component_not_in_bom() throws Exception {
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

    JsonNode dependenciesJson =
        getJsonNodeInformation("report-innersource-multi-module-component-not-in-bom/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-multi-module-component-not-in-bom/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-multi-module-component-not-in-bom/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-multi-module-component-not-in-bom/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 3, 5, bomInnerSourceParent, bomInnerSourceDependencies);

    assertSummaryCounters(summaryJson, dataJson, 18);

    assertInnerSourceParent(bomInnerSourceParent.get(1), appInnerSource, innerSourceModel);
    assertInnerSourceParent(bomInnerSourceParent.get(2), appInnerSource, innerScannerArchive);
    assertInnerSourceParent(bomInnerSourceParent.get(0), appInnerSource, innerSourceClient);

    assertTransitiveInnerSourceInformation(bomInnerSourceDependencies, appInnerSource);

    Map<ComponentIdentifier, String> dependencyComponentPurls = new HashMap<>();
    dependencyComponentPurls
        .put(ComponentIdentifier.createMavenCoordinates("com.google.code.gson", "gson", "2.8.1", "", "jar"),
            "pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar");
    dependencyComponentPurls
        .put(ComponentIdentifier.createMavenCoordinates("xmlpull", "xmlpull", "1.1.3.1", "", "jar"),
            "pkg:maven/com.sonatype.insight.scan/insight-module-model@1.0.0-SNAPSHOT?type=jar");
    dependencyComponentPurls.put(
        ComponentIdentifier.createMavenCoordinates("org.seleniumhq.selenium", "selenium-leg-rc", "2.48.2", "", "jar"),
        "pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar");
    dependencyComponentPurls
        .put(ComponentIdentifier.createMavenCoordinates("org.slf4j", "slf4j-api", "1.7.30", "", "jar"),
            "pkg:maven/com.sonatype.insight.scan/insight-module-model@1.0.0-SNAPSHOT?type=jar");
    dependencyComponentPurls.put(ComponentIdentifier
            .createMavenCoordinates("org.seleniumhq.selenium", "selenium-remote-driver", "2.48.2", "", "jar"),
        "pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar");
    assertComponentNameForTransitiveDependencies(bomInnerSourceDependencies, dependencyComponentPurls);

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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

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
    ComponentIdentifier modelId = ComponentIdentifier.createMavenCoordinates(
        "com.sonatype.insight.scan", "insight-module-model", "1.0.0-SNAPSHOT", "", "jar");
    tempEntity.newInnerSourceComponent(
        "pkg:maven/com.sonatype.insight.scan/insight-innersource-child", appInnerSource);
    ComponentIdentifier childId = ComponentIdentifier.createMavenCoordinates(
        "com.sonatype.insight.scan", "insight-innersource-child", "2.0.0", "", "jar");
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-unknown-components/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-unknown-components/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-unknown-components/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-unknown-components/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 1, 2, null, bomInnerSourceDependencies);

    assertUpdatedBomAttributeValue(bomJson, modelId, "packageUrl",
        "pkg:maven/com.sonatype.insight.scan/insight-module-model@1.0.0-SNAPSHOT?type=jar");
    assertUpdatedBomAttributeValue(bomJson, childId, "packageUrl",
        "pkg:maven/com.sonatype.insight.scan/insight-innersource-child@2.0.0?type=jar");
    assertTransitiveInnerSourceInformation(bomInnerSourceDependencies, appInnerSource);
    assertSummaryCounters(summaryJson, dataJson, 2);

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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    assertInnerSourceInformation(bomJson, 0, 0, null, null);
    assertSummaryCounters(summaryJson, dataJson, 3);

    verify(telemetrySender, never()).send(Mockito.any(TelemetryData.class));
  }

  @Test
  public void testProcessDependencyTree_not_maven_plugin() throws Exception {
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-depTree-not-maven-plugin/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-depTree-not-maven-plugin/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-depTree-not-maven-plugin/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-depTree-not-maven-plugin/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    ComponentIdentifier knownDirect =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "known-direct", "2.8.1", "", "jar");
    ComponentIdentifier knownTransitive1 =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "known-transitive1", "2.8.1", "", "jar");
    ComponentIdentifier knownTransitive1Level2 =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "known-transitive1-2", "2.8.1", "", "jar");
    ComponentIdentifier unknownTransitive1 =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "unknown-transitive1", "2.8.1", "", "jar");
    ComponentIdentifier unknownDirect =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "unknown-direct", "1.0.0-SNAPSHOT", "", "jar");
    ComponentIdentifier knownTransitive3 =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "known-transitive3", "1.0.0-SNAPSHOT", "", "jar");

    assertBomNodeDependencyInfo(bomJson, knownDirect, true, null);
    assertBomNodeDependencyInfo(bomJson, knownTransitive1, false, Collections.singleton(knownDirect));
    assertBomNodeDependencyInfo(bomJson, knownTransitive1Level2, false,
        Sets.newHashSet(knownTransitive1, unknownDirect));
    assertBomNodeDependencyInfo(bomJson, unknownTransitive1, false, Collections.singleton(knownDirect));
    assertBomNodeDependencyInfo(bomJson, unknownDirect, true, null);
    assertBomNodeDependencyInfo(bomJson, knownTransitive3, false, Collections.singleton(unknownDirect));
  }

  @Test
  public void testProcessDependencyTree_with_maven_plugin() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:maven/com.innersource/InnerSource-Producer", appInnerSource);

    JsonNode dependenciesJson =
        getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    ComponentIdentifier knownDirect =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "known-direct", "2.8.1", "", "jar");
    ComponentIdentifier knownTransitive1 =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "known-transitive1", "2.8.1", "", "jar");
    ComponentIdentifier knownTransitive1Level2 =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "known-transitive1-2", "2.8.1", "", "jar");
    ComponentIdentifier unknownTransitive1 =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "unknown-transitive1", "2.8.1", "", "jar");
    ComponentIdentifier unknownDirect =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "unknown-direct", "1.0.0-SNAPSHOT", "", "jar");
    ComponentIdentifier knownTransitive3 =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "known-transitive3", "1.0.0-SNAPSHOT", "", "jar");
    ComponentIdentifier innerSourceProducer =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "InnerSource-Producer", "3.0.0", "", "jar");
    ComponentIdentifier producerTransitive1 =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "producer-transitive", "3.0.0", "", "jar");

    InnerSourceData isDataForProducer = new InnerSourceData(appInnerSource.getName(), appInnerSource.getId(), null);
    InnerSourceData isDataForProducerTransitive = new InnerSourceData(appInnerSource.getName(), appInnerSource.getId(),
        PackageUrlIdentifier.toPackageUrl(innerSourceProducer));

    assertBomNodeDependencyInfo(bomJson, knownDirect, true, null);
    assertBomNodeDependencyInfo(bomJson, knownTransitive1, false, Collections.singleton(knownDirect));
    assertBomNodeDependencyInfo(bomJson, knownTransitive1Level2, false,
        Sets.newHashSet(knownTransitive1, unknownDirect));
    assertBomNodeDependencyInfo(bomJson, unknownTransitive1, false, Collections.singleton(knownDirect));
    assertBomNodeDependencyInfo(bomJson, unknownDirect, true, false, null, null);
    assertBomNodeDependencyInfo(bomJson, knownTransitive3, false, false, Collections.singleton(unknownDirect), null);
    assertBomNodeDependencyInfo(bomJson, innerSourceProducer, true, true, null,
        Collections.singleton(isDataForProducer));
    assertBomNodeDependencyInfo(bomJson, producerTransitive1, false, false, Collections.singleton(innerSourceProducer),
        Collections.singleton(isDataForProducerTransitive));
  }

  @Test
  public void testResolve_TransitiveAndDirect() throws Exception {
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-transitive-and-direct/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-transitive-and-direct/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-transitive-and-direct/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-transitive-and-direct/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    ComponentIdentifier knownDirect = ComponentIdentifier
        .createMavenCoordinates("com.innersource", "known-direct", "2.8.1", "", "jar");
    ComponentIdentifier knownTransitiveAndDirect = ComponentIdentifier
        .createMavenCoordinates("com.innersource", "known-transitive-and-direct", "2.8.1", "", "jar");
    assertBomNodeDependencyInfo(bomJson, knownTransitiveAndDirect, true, Collections.singleton(knownDirect));
  }

  @Test
  public void testResolve_MultipleParents() throws Exception {
    Application appInnerSource1 = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:maven/com.innersource/known-direct-1", appInnerSource1);
    Application appInnerSource2 = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:maven/com.innersource/known-direct-2", appInnerSource2);
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-multiple-parents/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-multiple-parents/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-multiple-parents/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-multiple-parents/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    ComponentIdentifier knownDirect1 = ComponentIdentifier
        .createMavenCoordinates("com.innersource", "known-direct-1", "2.8.1", "", "jar");
    ComponentIdentifier knownDirect2 = ComponentIdentifier
        .createMavenCoordinates("com.innersource", "known-direct-2", "2.8.1", "", "jar");
    ComponentIdentifier knownTransitive = ComponentIdentifier
        .createMavenCoordinates("com.innersource", "known-transitive", "2.8.1", "", "jar");
    Set<InnerSourceData> expectedInnerSourceData = Sets.newHashSet(
        new InnerSourceData(appInnerSource1.getName(), appInnerSource1.getId(),
            PackageUrlIdentifier.fromComponentIdentifier(knownDirect1).getPackageUrl()),
        new InnerSourceData(appInnerSource2.getName(), appInnerSource2.getId(),
            PackageUrlIdentifier.fromComponentIdentifier(knownDirect2).getPackageUrl())
    );
    assertBomNodeDependencyInfo(bomJson, knownTransitive, false, false, Sets.newHashSet(knownDirect1, knownDirect2),
        expectedInnerSourceData);
    ComponentIdentifier knownTransitiveTransitive = ComponentIdentifier
        .createMavenCoordinates("commons-io", "commons-io", "2.6", "", "jar");
    assertBomNodeDependencyInfo(bomJson, knownTransitiveTransitive, false, false,
        Collections.singleton(knownTransitive), expectedInnerSourceData);
  }

  @Test
  public void testResolve_AddInnerSource_WhenNotIdentifiedByMJA() throws Exception {
    Application appInnerSource1 = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:npm/producer", appInnerSource1);

    JsonNode dependenciesJson = getJsonNodeInformation(
        "report-innersource-npm-add-unrecognized/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation(
        "report-innersource-npm-add-unrecognized/bom.json");
    JsonNode summaryJson = getJsonNodeInformation(
        "report-innersource-npm-add-unrecognized/summary.json");
    JsonNode dataJson = getJsonNodeInformation(
        "report-innersource-npm-add-unrecognized/data.json");

    ComponentIdentifier innerSourceId = ComponentIdentifier
        .createNpmCoordinates("producer", "file:../producer");
    Set<InnerSourceData> expectedInnerSourceData = Sets
        .newHashSet(new InnerSourceData(appInnerSource1.getName(), appInnerSource1.getId(), null));

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    assertBomNodeDependencyInfo(bomJson, innerSourceId, true, true, null, expectedInnerSourceData);
    assertSummaryCounters(summaryJson, dataJson, 3, 3);

    JsonNode newIsNode = findNodeById(bomJson, innerSourceId);
    AnalyzerFeatures analyzerFeatures = JsonUtils
        .asPojo(newIsNode.get("analyzerFeatures"), AnalyzerFeatures.class);

    assertThat(newIsNode.get("hash").asText()).isNotBlank();
    assertThat(newIsNode.get("proprietary").asBoolean()).isFalse();
    assertThat(newIsNode.get("createTime")).isNotNull();
    assertThat(newIsNode.get("relativePopularity").asInt()).isZero();
    assertThat(newIsNode.get("filenames").get(0).asText()).isEqualTo(
        PackageUrlIdentifier.fromComponentIdentifier(innerSourceId).getPackageUrl());
    assertThat(newIsNode.get("pathnames").get(0).asText()).isEqualTo(
        "dependency:/" + PackageUrlIdentifier.fromComponentIdentifier(innerSourceId).getPackageUrl().replace("/", "\\")
    );
    assertThat(newIsNode.get("proprietary").asBoolean()).isFalse();
    assertThat(analyzerFeatures.getAnalysisSource()).isEqualTo(AnalysisSource.THIRD_PARTY);
    assertThat(analyzerFeatures.getAnalysisType()).isEqualTo(AnalysisType.COORDINATE);
    assertThat(analyzerFeatures.getScanClient()).isEqualTo("ci");
    assertThat(analyzerFeatures.isHasIdentity()).isFalse();
    assertThat(analyzerFeatures.isHasLicense()).isFalse();
    assertThat(analyzerFeatures.isHasSecurity()).isFalse();
  }

  @Test
  public void testResolve_NewInnerSourceNode_NotProprietary() throws Exception {
    Application innerSourceApplication = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:npm/producer", innerSourceApplication);
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/bom.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/data.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/summary.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    ComponentIdentifier innerSourceId = ComponentIdentifier.createNpmCoordinates("producer", "file:../producer");
    JsonNode newIsNode = findNodeById(bomJson, innerSourceId);
    assertThat(newIsNode.get("proprietary").asBoolean()).isFalse();
  }

  @Test
  public void testResolve_NewInnerSourceNode_Proprietary() throws Exception {
    Application innerSourceApplication = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:npm/producer", innerSourceApplication);
    tempEntity.newProprietaryConfig(app.getId(), Collections.emptyList(), Collections.singletonList("producer"));
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/bom.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/data.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/summary.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    ComponentIdentifier innerSourceId = ComponentIdentifier.createNpmCoordinates("producer", "file:../producer");
    JsonNode newIsNode = findNodeById(bomJson, innerSourceId);
    assertThat(newIsNode.get("proprietary").asBoolean()).isTrue();
  }

  @Test
  public void testResolve_npm() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    InnerSourceComponent producerOne =
        tempEntity.newInnerSourceComponent("pkg:npm/producer-one", appInnerSource);
    InnerSourceComponent producerTwo = tempEntity
        .newInnerSourceComponent("pkg:npm/producer-two", appInnerSource);
    tempEntity.newInnerSourceComponent("pkg:npm/consumer", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-npm/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-npm/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-npm/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-npm/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertInnerSourceInformation(bomJson, 2, 6, null, bomInnerSourceDependencies);

    assertTransitiveInnerSourceInformation(bomInnerSourceDependencies, appInnerSource);
    assertSummaryCounters(summaryJson, dataJson, 11);

    Set<String> innerSourceIds = new HashSet<>();
    innerSourceIds.add(producerOne.getApplicationId());
    innerSourceIds.add(producerTwo.getApplicationId());
    assertTelemetryInformation(app.getId(), innerSourceIds);
  }

  @Test
  public void testResolve_SimilarMatches() throws Exception {
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-similar-match/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-similar-match/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-similar-match/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-similar-match/data.json");
    JsonNode bomNode = findNodeById(bomJson, ComponentIdentifier.createMavenCoordinates(
        "org.apache.camel", "camel-hl7", "2.23.2", "", "jar"));
    JsonNode bomNodeNoPathnames = findNodeById(bomJson, ComponentIdentifier.createMavenCoordinates(
        "org.apache.camel", "camel-hl7-no-pathnames", "2.23.2", "", "jar"));
    JsonNode bomNodeEmptyPathnames = findNodeById(bomJson, ComponentIdentifier.createMavenCoordinates(
        "org.apache.camel", "camel-hl7-empty-pathnames", "2.23.2", "", "jar"));
    JsonNode bomNodeBadPathnames = findNodeById(bomJson, ComponentIdentifier.createMavenCoordinates(
        "org.apache.camel", "camel-hl7-bad-pathnames", "2.23.2", "", "jar"));
    assertThat(bomNode).isNotNull();
    assertThat(bomNode.get("directDependency")).isNull();
    assertThat(bomNode.get("innerSource")).isNull();
    assertThat(bomNodeNoPathnames).isNotNull();
    assertThat(bomNodeNoPathnames.get("directDependency")).isNull();
    assertThat(bomNodeNoPathnames.get("innerSource")).isNull();
    assertThat(bomNodeEmptyPathnames).isNotNull();
    assertThat(bomNodeEmptyPathnames.get("directDependency")).isNull();
    assertThat(bomNodeEmptyPathnames.get("innerSource")).isNull();
    assertThat(bomNodeBadPathnames).isNotNull();
    assertThat(bomNodeBadPathnames.get("directDependency")).isNull();
    assertThat(bomNodeBadPathnames.get("innerSource")).isNull();

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, app, telemetrySender).resolve();

    assertThat(bomNode.get("directDependency")).isNotNull();
    assertThat(bomNode.get("innerSource")).isNotNull();
    assertThat(bomNodeNoPathnames.get("directDependency")).isNull();
    assertThat(bomNodeNoPathnames.get("innerSource")).isNull();
    assertThat(bomNodeEmptyPathnames.get("directDependency")).isNull();
    assertThat(bomNodeEmptyPathnames.get("innerSource")).isNull();
    assertThat(bomNodeBadPathnames.get("directDependency")).isNull();
    assertThat(bomNodeBadPathnames.get("innerSource")).isNull();
  }

  @Test
  public void testGetPossibleProprietaryCoordinates_Maven() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    assertThat(DependencyResolver.getPossibleProprietaryCoordinates(componentIdentifier)).containsExactly("g", "a");
  }

  @Test
  public void testGetPossibleProprietaryCoordinates_Npm() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    assertThat(DependencyResolver.getPossibleProprietaryCoordinates(componentIdentifier)).containsExactly("p");
  }

  @Test
  public void testGetPossibleProprietaryCoordinates_Default() {
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("f", Maps.of("c1", "v1", "c2", "v2"));
    assertThat(DependencyResolver.getPossibleProprietaryCoordinates(componentIdentifier)).isEmpty();
  }

  @Test
  public void testCreateIsProprietary_NoProprietaryConfig() {
    Application application = tempEntity.newApplicationWithParent();
    assertThat(new ProprietaryConfigDAO().getByOwnerId(application.getId())).isNull();

    Predicate<String> isProprietary = DependencyResolver.createIsProprietary(application.getId());

    assertThat(isProprietary).rejects("any");
  }

  @Test
  public void testCreateIsProprietary_EmptyProprietaryConfig() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), Collections.emptyList(), Collections.emptyList());

    Predicate<String> isProprietary = DependencyResolver.createIsProprietary(application.getId());

    assertThat(isProprietary).rejects("any");
  }

  @Test
  public void testCreateIsProprietary_WithPackages() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), Arrays.asList("a1", "b1.b2"), Collections.emptyList());

    Predicate<String> isProprietary = DependencyResolver.createIsProprietary(application.getId());

    assertThat(isProprietary).accepts("a1");
    assertThat(isProprietary).rejects("a2");
    assertThat(isProprietary).accepts("a1.a2");
    assertThat(isProprietary).rejects("b1");
    assertThat(isProprietary).accepts("b1.b2");
    assertThat(isProprietary).rejects("b1.b3");
    assertThat(isProprietary).accepts("b1.b2.b3");
  }

  @Test
  public void testCreateIsProprietary_WithRegexes() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), Collections.emptyList(), Arrays.asList("a1.*", ".*b1", "c1"));

    Predicate<String> isProprietary = DependencyResolver.createIsProprietary(application.getId());

    assertThat(isProprietary).accepts("a1");
    assertThat(isProprietary).accepts("a1.a2");
    assertThat(isProprietary).accepts("a1.a2.a3");
    assertThat(isProprietary).rejects("a2");
    assertThat(isProprietary).rejects("a2.a1");
    assertThat(isProprietary).rejects("a3.a2.a1");
    assertThat(isProprietary).accepts("b1");
    assertThat(isProprietary).rejects("b1.b2");
    assertThat(isProprietary).rejects("b1.b2.b3");
    assertThat(isProprietary).rejects("b2");
    assertThat(isProprietary).accepts("b2.b1");
    assertThat(isProprietary).accepts("b3.b2.b1");
    assertThat(isProprietary).accepts("c1");
    assertThat(isProprietary).rejects("c2");
    assertThat(isProprietary).rejects("c1.c2");
    assertThat(isProprietary).rejects("c2.c1");
  }

  @Test
  public void testCreateIsProprietary_WithPackagesAndRegexes() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), Collections.singletonList("a1"),
        Collections.singletonList("b1"));

    Predicate<String> isProprietary = DependencyResolver.createIsProprietary(application.getId());

    assertThat(isProprietary).accepts("a1");
    assertThat(isProprietary).accepts("a1.a2");
    assertThat(isProprietary).accepts("a1.a2.a3");
    assertThat(isProprietary).rejects("a2");
    assertThat(isProprietary).rejects("a2.a1");
    assertThat(isProprietary).rejects("a3.a2.a1");
    assertThat(isProprietary).accepts("b1");
    assertThat(isProprietary).rejects("b1.b2");
    assertThat(isProprietary).rejects("b1.b2.b3");
    assertThat(isProprietary).rejects("b2");
    assertThat(isProprietary).rejects("b2.b1");
    assertThat(isProprietary).rejects("b3.b2.b1");
  }

  @Test
  public void testIsProprietary_InitializesIsProprietaryOnceForApplication() {
    Application application = tempEntity.newApplicationWithParent();
    Application other = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), null, Collections.singletonList("p1"));
    tempEntity.newProprietaryConfig(other.getId(), null, Collections.singletonList("p2"));
    DependencyResolver dependencyResolver = DependencyResolver.getInstance(null, null, null, null, application, null);
    assertThat(dependencyResolver.isProprietary).isNull();

    assertThat(dependencyResolver.isProprietary(ComponentIdentifier.createNpmCoordinates("p1", "v"))).isTrue();
    Predicate<String> isProprietary = dependencyResolver.isProprietary;
    assertThat(isProprietary).isNotNull();

    assertThat(dependencyResolver.isProprietary(ComponentIdentifier.createNpmCoordinates("p2", "v"))).isFalse();
    assertThat(dependencyResolver.isProprietary).isEqualTo(isProprietary);
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

  public void assertKnownComponents(
      List<JsonNode> knownDependencies,
      List<ComponentIdentifier> expectedKnownComponents)
  {
    List<ComponentIdentifier> knownComponents =
        knownDependencies.stream().map(ComponentIdentifierAdapter::getComponentIdentifier)
            .collect(Collectors.toList());
    assertThat(knownComponents).containsAll(expectedKnownComponents);
  }

  private void assertBomNodeDependencyInfo(
      final JsonNode bomJson,
      final ComponentIdentifier componentIdentifier,
      final boolean isDirect,
      final Set<ComponentIdentifier> parentIds) throws Exception
  {
    assertBomNodeDependencyInfo(bomJson, componentIdentifier, isDirect, false, parentIds, null);
  }

  private void assertBomNodeDependencyInfo(
      final JsonNode bomJson,
      final ComponentIdentifier componentIdentifier,
      final boolean isDirect,
      final boolean isInnerSource,
      final Set<ComponentIdentifier> parentIds,
      final Set<InnerSourceData> innerSourceData) throws Exception
  {
    JsonNode bomNode = findNodeById(bomJson, componentIdentifier);
    assertThat(objectMapper.treeToValue(bomNode.get("componentIdentifier"), ComponentIdentifier.class)).isEqualTo(
        componentIdentifier);
    assertThat(bomNode.get("directDependency").asBoolean()).isEqualTo(isDirect);
    assertThat(bomNode.get("innerSource").asBoolean()).isEqualTo(isInnerSource);
    Set<ComponentIdentifier> actualParentIds = null;
    JsonNode parentComponentPurls = bomNode.path(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD);
    if (parentComponentPurls.isArray() && !parentComponentPurls.isEmpty()) {
      actualParentIds = new LinkedHashSet<>();
      for (JsonNode parentComponentPurl : parentComponentPurls) {
        ComponentIdentifier compIdentifier =
            ComponentIdentifierAdapter.toComponentIdentifier(parentComponentPurl.asText());
        compIdentifier.ensureComplete();
        if (!actualParentIds.add(compIdentifier)) {
          fail("Duplicate parentComponentPurl " + parentComponentPurl + " found in " + parentComponentPurls);
        }
      }
    }
    assertThat(actualParentIds).isEqualTo(parentIds);
    if (innerSourceData != null) {
      JsonNode innerSourceDataArray = bomNode.get(ComponentDAO.INNER_SOURCE_DATA_FIELD);
      Set<InnerSourceData> actualInnerSourceData = toInnerSourceDataSet(innerSourceDataArray);
      assertThat(actualInnerSourceData).containsExactlyInAnyOrder(innerSourceData.toArray(new InnerSourceData[0]));
    }
  }

  private JsonNode findNodeById(final JsonNode bomJson, final ComponentIdentifier identifier) {
    for (JsonNode node : bomJson.get("aaData")) {
      ComponentIdentifier nodeId = ComponentIdentifierAdapter.getComponentIdentifier(node);
      if (nodeId != null) {
        if (Objects.equals(identifier, nodeId)) {
          return node;
        }
      }
      else {
        String mavenIdString = toMavenIdString(identifier);
        for (final JsonNode pathElement : node.get("pathnames")) {
          if (pathElement.asText().contains(mavenIdString)) {
            return node;
          }
        }
      }
    }
    return null;
  }

  private String toMavenIdString(final ComponentIdentifier identifier) {
    return String.format("%s:%s:jar:%s",
        identifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
        identifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID),
        identifier.get(ComponentIdentifier.VERSION));
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
      JsonNode innerSourceData = bomChild.get(ComponentDAO.INNER_SOURCE_DATA_FIELD);
      if (innerSourceData != null) {
        JsonNode innerSourceNodeParent = bomChild.get("innerSource");
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
    assertSummaryCounters(summaryJson, dataJson, expectedCount, null);
  }

  private void assertSummaryCounters(JsonNode summaryJson, JsonNode dataJson, int expectedCount,
      Integer totalArtifactCount)
  {
    assertThat(summaryJson).isNotNull();
    assertThat(summaryJson.get("knownArtifactCount").asInt()).isEqualTo(expectedCount);

    assertThat(dataJson).isNotNull();
    assertThat(dataJson.get("exactlyMatchedComponentCount").asInt()).isEqualTo(expectedCount);
    assertThat(dataJson.get("knownArtifactCount").asInt()).isEqualTo(expectedCount);

    if (totalArtifactCount != null) {
      assertThat(summaryJson.get("totalArtifactCount").asInt()).isEqualTo(totalArtifactCount);
      assertThat(dataJson.get("totalArtifactCount").asInt()).isEqualTo(totalArtifactCount);
    }
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
    assertInnerSourceTree(bomInnerSource.get(ComponentDAO.INNER_SOURCE_DATA_FIELD), app, null);
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
    assertThat(bomInnerSourceDependencies).isNotEmpty().allSatisfy(transitiveDependency -> {
      assertThat(transitiveDependency).isNotNull();
      assertThat(transitiveDependency.get("componentIdentifier")).isNotNull();
      assertThat(transitiveDependency.get("directDependency").asBoolean()).isFalse();
      assertThat(transitiveDependency.get(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD)).isNotNull();
      assertThat(transitiveDependency.get("innerSource").asBoolean()).isFalse();
      try {
        assertInnerSourceTree(transitiveDependency.get(ComponentDAO.INNER_SOURCE_DATA_FIELD), appInnerSource);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void assertComponentNameForTransitiveDependencies(
      final List<JsonNode> bomInnerSourceDependencies,
      final Map<ComponentIdentifier, String> dependencyComponentPurls)
  {
    for (JsonNode transitiveDependencies : bomInnerSourceDependencies) {
      ComponentIdentifier componentIdentifier =
          ComponentIdentifierAdapter.getComponentIdentifier(transitiveDependencies);
      String expectedComponentName = dependencyComponentPurls.get(componentIdentifier);
      assertThat(transitiveDependencies.get(ComponentDAO.INNER_SOURCE_DATA_FIELD).get(0)
          .get("innerSourceComponentPurl").asText()).isEqualTo(expectedComponentName);
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

  private void assertInnerSourceTree(JsonNode innerSourceDataArray, Application app, String purl) throws Exception {
    Set<InnerSourceData> innerSourceData = toInnerSourceDataSet(innerSourceDataArray);
    InnerSourceData expectedInnerSourceData = new InnerSourceData(app.getName(), app.getId(), purl);
    assertThat(innerSourceData).containsExactly(expectedInnerSourceData);
  }

  private void assertInnerSourceTree(JsonNode innerSourceDataArray, Application app) throws Exception {
    Set<InnerSourceData> innerSourceData = toInnerSourceDataSet(innerSourceDataArray);
    assertThat(innerSourceData).extracting(InnerSourceData::getOwnerApplicationName).containsOnly(app.getName());
    assertThat(innerSourceData).extracting(InnerSourceData::getOwnerApplicationId).containsOnly(app.getId());
    assertThat(innerSourceData).extracting(InnerSourceData::getInnerSourceComponentPurl).doesNotContainNull();
  }

  private Set<InnerSourceData> toInnerSourceDataSet(JsonNode innerSourceDataArray) throws Exception {
    assertThat(innerSourceDataArray).isInstanceOf(ArrayNode.class);
    Set<InnerSourceData> innerSourceData = new HashSet<>();
    for (JsonNode innerSourceNode : innerSourceDataArray) {
      innerSourceData.add(JsonUtils.asPojo(innerSourceNode, InnerSourceData.class));
    }
    return innerSourceData;
  }

  private DependencyResolver newDependencyResolver() {
    return new DependencyResolver(null, null, null, null, app, telemetrySender, innerSourceComponentDAOSpy,
        applicationDAO);
  }

  private JsonNode getJsonNodeInformation(String path) throws IOException {
    return objectMapper.readTree(getClass().getResource("/InnerSourceServiceTest/" + path));
  }

  private void assertUpdatedBomAttributeValue(JsonNode bomJson,
      ComponentIdentifier lookupId, String fieldName,
      String fieldValue) throws IOException
  {
    for (JsonNode dependency : bomJson.get("aaData")) {
      ComponentIdentifier found = JsonUtils
          .asPojo(dependency.get("componentIdentifier"), ComponentIdentifier.class);
      if (lookupId.equals(found)) {
        assertThat(dependency.get(fieldName).asText()).isEqualTo(fieldValue);
        return;
      }
    }
    fail("component identifier " + lookupId + " not found");
  }
}
