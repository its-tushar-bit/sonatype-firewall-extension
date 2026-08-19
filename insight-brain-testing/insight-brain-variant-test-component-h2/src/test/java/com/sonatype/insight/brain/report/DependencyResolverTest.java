/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.insight.brain.variant.AbstractBrainInjectedH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.Sets;
import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceVersionDAO;
import com.sonatype.insight.brain.innersource.InnerSourceConsumerTelemetry;
import com.sonatype.insight.brain.innersource.InnerSourceProducerComponentTelemetry;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.innersource.InnerSourceVersion;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
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
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@ComponentH2Test
public class DependencyResolverTest
    extends AbstractBrainInjectedH2Test
{
  private final TelemetrySender telemetrySender = Mockito.mock(TelemetrySender.class);

  @Inject
  private TelemetryUtils telemetryUtils;

  private InnerSourceApplicationDAO observedInnerSourceApplicationDAO;

  @Inject
  private InnerSourceApplicationDAO innerSourceApplicationDAO;

  @Inject
  private InnerSourceVersionDAO innerSourceVersionDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private ProprietaryConfigService proprietaryConfigService;

  private Application app;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  public void init() {
    observedInnerSourceApplicationDAO = spy(innerSourceApplicationDAO);
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void processInnerSource_createInnerSourceParent() throws Exception {
    JsonNode dependenciesJson =
        new ObjectMapper()
            .readTree(getClass().getResource("/DependencyResolverTest/report-innersource/dependencies.json"));
    JsonNode dependencyTree = dependenciesJson.path("dependencyTree");

    PackageUrlIdentifier rootPurl =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.nexus/nexus-platform-api@1.0.0?type=jar");

    newDependencyResolver(StageTypes.RELEASE.getId()).saveInnerSourceComponent(rootPurl);

    List<InnerSourceApplication> innerSourceApplications =
        observedInnerSourceApplicationDAO.getByApplicationId(app.getId());
    assertThat(innerSourceApplications).hasSize(1);

    assertThat(innerSourceApplications.get(0).getApplicationId()).isEqualTo(app.getId());

    assertThat(dependenciesJson).isNotNull();
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(dependencyTree);
    assertThat(componentIdentifier).isNotNull();
    PackageUrlIdentifier expectedPurl =
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier.createAlternativeVersion(null));
    assertThat(innerSourceApplications.get(0).getPackageUrl()).isEqualTo(expectedPurl.getPackageUrl());

    InnerSourceVersion innerSourceVersion =
        innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplications.get(0).getId(),
            StageTypes.RELEASE.getId());
    assertThat(innerSourceVersion).isNotNull();

    assertThat(innerSourceVersion.getInnerSourceApplicationId()).isEqualTo(innerSourceApplications.get(0).getId());
    assertThat(innerSourceVersion.getLatestVersion()).isEqualTo("1.0.0");
    assertThat(innerSourceVersion.getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());
  }

  @Test
  public void processInnerSource_createInnerSourceParentWithoutInnerSourceVersion() throws Exception {
    JsonNode dependenciesJson =
        new ObjectMapper()
            .readTree(getClass().getResource("/DependencyResolverTest/report-innersource/dependencies.json"));
    JsonNode dependencyTree = dependenciesJson.path("dependencyTree");

    PackageUrlIdentifier rootPurl =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar");

    newDependencyResolver().saveInnerSourceComponent(rootPurl);

    List<InnerSourceApplication> innerSourceApplications =
        observedInnerSourceApplicationDAO.getByApplicationId(app.getId());
    assertThat(innerSourceApplications).hasSize(1);

    assertThat(innerSourceApplications.get(0).getApplicationId()).isEqualTo(app.getId());

    assertThat(dependenciesJson).isNotNull();
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(dependencyTree);
    assertThat(componentIdentifier).isNotNull();
    PackageUrlIdentifier expectedPurl =
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier.createAlternativeVersion(null));
    assertThat(innerSourceApplications.get(0).getPackageUrl()).isEqualTo(expectedPurl.getPackageUrl());

    assertThat(innerSourceVersionDAO.getByInnerSourceApplicationId(innerSourceApplications.get(0).getId()))
        .hasSize(0);
  }

  @Test
  public void processInnerSource_checkInnerSourceParent() {
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.innersource.main/innersource-main?type=jar", app);

    PackageUrlIdentifier rootPurl =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.innersource.main/innersource-main@1.0.0?type=jar");

    newDependencyResolver().saveInnerSourceComponent(rootPurl);

    verify(observedInnerSourceApplicationDAO, never()).insert(innerSourceApplication);
    verify(observedInnerSourceApplicationDAO, never()).update(innerSourceApplication);
  }

  @Test
  public void processInnerSource_updateInnerSourceParentWithNonNullInnerSourceVersion() {
    Application innerSourceApp = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar", innerSourceApp);

    PackageUrlIdentifier rootPurl =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.nexus/nexus-platform-api@1.0.0?type=jar");

    newDependencyResolver(StageTypes.RELEASE.getId()).saveInnerSourceComponent(rootPurl);

    ArgumentCaptor<TransactionContext> argument = ArgumentCaptor.forClass(TransactionContext.class);
    ArgumentCaptor<InnerSourceApplication> argument2 = ArgumentCaptor.forClass(InnerSourceApplication.class);
    verify(observedInnerSourceApplicationDAO).update(argument.capture(), argument2.capture());

    InnerSourceApplication innerSourceApplication = argument2.getValue();
    assertThat(innerSourceApplication.getApplicationId()).isEqualTo(app.getId());

    assertThat(innerSourceVersionDAO.getByInnerSourceApplicationId(innerSourceApplication.getId())).hasSize(1);
    InnerSourceVersion innerSourceVersion =
        innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplication.getId(),
            StageTypes.RELEASE.getId());
    assertThat(innerSourceVersion).isNotNull();

    assertThat(innerSourceVersion.getInnerSourceApplicationId()).isEqualTo(innerSourceApplication.getId());
    assertThat(innerSourceVersion.getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());

    // The original component has null as version, now it should have a value there
    assertThat(innerSourceVersion.getLatestVersion()).isEqualTo("1.0.0");
  }

  @Test
  public void processInnerSource_updateInnerSourceParentWithOlderVersion() {
    Application innerSourceApp = tempEntity.newApplicationWithParent();
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar",
            innerSourceApp);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.1", StageTypes.RELEASE.getId());

    PackageUrlIdentifier rootPurl =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.nexus/nexus-platform-api@1.0.0?type=jar");
    newDependencyResolver(StageTypes.RELEASE.getId()).saveInnerSourceComponent(rootPurl);

    ArgumentCaptor<TransactionContext> argument = ArgumentCaptor.forClass(TransactionContext.class);
    ArgumentCaptor<InnerSourceApplication> argument2 = ArgumentCaptor.forClass(InnerSourceApplication.class);
    verify(observedInnerSourceApplicationDAO).update(argument.capture(), argument2.capture());

    innerSourceApplication = argument2.getValue();
    assertThat(innerSourceApplication.getApplicationId()).isEqualTo(app.getId());

    assertThat(innerSourceVersionDAO.getByInnerSourceApplicationId(innerSourceApplication.getId())).hasSize(1);
    InnerSourceVersion innerSourceVersion =
        innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplication.getId(),
            StageTypes.RELEASE.getId());
    assertThat(innerSourceVersion).isNotNull();

    assertThat(innerSourceVersion.getInnerSourceApplicationId()).isEqualTo(innerSourceApplication.getId());
    assertThat(innerSourceVersion.getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());
    assertThat(innerSourceVersion.getLatestVersion()).isEqualTo("1.0.1");
  }

  @Test
  public void processInnerSource_updateInnerSourceParentWithNewerVersion() {
    Application innerSourceApp = tempEntity.newApplicationWithParent();
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar",
            innerSourceApp);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.0", StageTypes.RELEASE.getId());

    PackageUrlIdentifier rootPurl =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.nexus/nexus-platform-api@1.0.1?type=jar");
    newDependencyResolver(StageTypes.RELEASE.getId()).saveInnerSourceComponent(rootPurl);

    ArgumentCaptor<TransactionContext> argument = ArgumentCaptor.forClass(TransactionContext.class);
    ArgumentCaptor<InnerSourceApplication> argument2 = ArgumentCaptor.forClass(InnerSourceApplication.class);
    verify(observedInnerSourceApplicationDAO).update(argument.capture(), argument2.capture());

    innerSourceApplication = argument2.getValue();
    assertThat(innerSourceApplication.getApplicationId()).isEqualTo(app.getId());

    assertThat(innerSourceVersionDAO.getByInnerSourceApplicationId(innerSourceApplication.getId())).hasSize(1);
    InnerSourceVersion innerSourceVersion =
        innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplication.getId(),
            StageTypes.RELEASE.getId());
    assertThat(innerSourceVersion).isNotNull();

    assertThat(innerSourceVersion.getInnerSourceApplicationId()).isEqualTo(innerSourceApplication.getId());
    assertThat(innerSourceVersion.getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());
    assertThat(innerSourceVersion.getLatestVersion()).isEqualTo("1.0.1");
  }

  @Test
  public void processInnerSource_updateInnerSourceParentWithNullVersion() {
    Application innerSourceApp = tempEntity.newApplicationWithParent();
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar",
            innerSourceApp);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.1", StageTypes.RELEASE.getId());

    // Simulate a purl without a version
    PackageUrlIdentifier rootVersionlessPurl =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar");
    newDependencyResolver(StageTypes.RELEASE.getId()).saveInnerSourceComponent(rootVersionlessPurl);

    ArgumentCaptor<TransactionContext> argument = ArgumentCaptor.forClass(TransactionContext.class);
    ArgumentCaptor<InnerSourceApplication> argument2 = ArgumentCaptor.forClass(InnerSourceApplication.class);
    verify(observedInnerSourceApplicationDAO).update(argument.capture(), argument2.capture());

    innerSourceApplication = argument2.getValue();
    assertThat(innerSourceApplication.getApplicationId()).isEqualTo(app.getId());

    assertThat(innerSourceVersionDAO.getByInnerSourceApplicationId(innerSourceApplication.getId())).hasSize(1);
    InnerSourceVersion innerSourceVersion =
        innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplication.getId(),
            StageTypes.RELEASE.getId());
    assertThat(innerSourceVersion).isNotNull();

    assertThat(innerSourceVersion.getInnerSourceApplicationId()).isEqualTo(innerSourceApplication.getId());
    assertThat(innerSourceVersion.getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());

    // Since the new version is null, it kept the latest version
    assertThat(innerSourceVersion.getLatestVersion()).isEqualTo("1.0.1");
  }

  @Test
  public void processInnerSource_updateInnerSourceParentWithNonNullStage() {
    Application innerSourceApp = tempEntity.newApplicationWithParent();
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar",
            innerSourceApp);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.0", null);

    PackageUrlIdentifier rootPurl =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.nexus/nexus-platform-api@1.0.0?type=jar");

    newDependencyResolver(StageTypes.RELEASE.getId()).saveInnerSourceComponent(rootPurl);

    ArgumentCaptor<TransactionContext> argument = ArgumentCaptor.forClass(TransactionContext.class);
    ArgumentCaptor<InnerSourceApplication> argument2 = ArgumentCaptor.forClass(InnerSourceApplication.class);
    verify(observedInnerSourceApplicationDAO).update(argument.capture(), argument2.capture());

    innerSourceApplication = argument2.getValue();
    assertThat(innerSourceApplication.getApplicationId()).isEqualTo(app.getId());

    assertThat(innerSourceVersionDAO.getByInnerSourceApplicationId(innerSourceApplication.getId())).hasSize(1);
    InnerSourceVersion innerSourceVersion =
        innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplication.getId(),
            StageTypes.RELEASE.getId());
    assertThat(innerSourceVersion).isNotNull();

    assertThat(innerSourceVersion.getInnerSourceApplicationId()).isEqualTo(innerSourceApplication.getId());
    assertThat(innerSourceVersion.getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());
  }

  @Test
  public void processInnerSource_updateInnerSourceParentWithMoreThanOneInnerSourceVersion() {
    Application innerSourceApp = tempEntity.newApplicationWithParent();
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar",
            innerSourceApp);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.0", StageTypes.RELEASE.getId());

    PackageUrlIdentifier rootPurl =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.nexus/nexus-platform-api@1.0.0?type=jar");

    newDependencyResolver(StageTypes.DEVELOP.getId()).saveInnerSourceComponent(rootPurl);

    ArgumentCaptor<TransactionContext> argument = ArgumentCaptor.forClass(TransactionContext.class);
    ArgumentCaptor<InnerSourceApplication> argument2 = ArgumentCaptor.forClass(InnerSourceApplication.class);
    verify(observedInnerSourceApplicationDAO).update(argument.capture(), argument2.capture());

    innerSourceApplication = argument2.getValue();
    assertThat(innerSourceApplication.getApplicationId()).isEqualTo(app.getId());

    assertThat(innerSourceVersionDAO.getByInnerSourceApplicationId(innerSourceApplication.getId())).hasSize(2);
    InnerSourceVersion innerSourceVersion =
        innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplication.getId(),
            StageTypes.RELEASE.getId());
    assertThat(innerSourceVersion).isNotNull();
    assertThat(innerSourceVersion.getInnerSourceApplicationId()).isEqualTo(innerSourceApplication.getId());
    assertThat(innerSourceVersion.getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());

    innerSourceVersion =
        innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplication.getId(),
            StageTypes.DEVELOP.getId());
    assertThat(innerSourceVersion).isNotNull();
    assertThat(innerSourceVersion.getInnerSourceApplicationId()).isEqualTo(innerSourceApplication.getId());
    assertThat(innerSourceVersion.getStageTypeId()).isEqualTo(StageTypes.DEVELOP.getId());
  }

  @Test
  public void processInnerSource_updateInnerSourceParent_sameApp() {
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api", app);

    PackageUrlIdentifier rootPurl =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.nexus/nexus-platform-api@1.0.0?type=jar");

    newDependencyResolver().saveInnerSourceComponent(rootPurl);

    verify(observedInnerSourceApplicationDAO, never()).update(innerSourceApplication);
  }

  @Test
  public void processInnerSource_noInnerSourceParent() {
    assertThat(newDependencyResolver().saveInnerSourceComponent(null)).isFalse();

    List<InnerSourceApplication> innerSourceApplications = innerSourceApplicationDAO.getByApplicationId(app.getId());
    assertThat(innerSourceApplications).isEmpty();
  }

  @Test
  public void processInnerSource_multiModule_component_not_in_bom() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication(
        "pkg:maven/com.sonatype.insight.scan/insight-scanner-hashing-asm60?type=jar", appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-scanner-hashing?type=jar",
        appInnerSource);

    PackageUrlIdentifier knownModule1 =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.insight.scan/insight-scanner-model@2.23.5-SNAPSHOT?type=jar");
    PackageUrlIdentifier knownModule2 = new PackageUrlIdentifier(
        "pkg:maven/com.sonatype.insight.scan/insight-scanner-model-io@2.23.5-SNAPSHOT?type=jar");
    PackageUrlIdentifier knownModule3 =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.insight.scan/insight-scanner-core@2.23.5-SNAPSHOT?type=jar");

    List<String> knownComponents =
        Arrays.asList(knownModule1.getPackageUrl(), knownModule2.getPackageUrl(), knownModule3.getPackageUrl());

    JsonNode dependenciesJson =
        getJsonNodeInformation("report-innersource-multi-module-component-not-in-bom/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-multi-module-component-not-in-bom/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-multi-module-component-not-in-bom/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-multi-module-component-not-in-bom/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    List<InnerSourceApplication> innerSourceApplications = innerSourceApplicationDAO.getByApplicationId(app.getId());
    assertThat(innerSourceApplications).hasSize(9);

    assertThat(innerSourceApplications).extracting(InnerSourceApplication::getApplicationId).containsOnly(app.getId());
    assertDependencyInfo(bomJson, 8, 2, 1, 2, 3, 13, 0, summaryJson, dataJson, appInnerSource, knownComponents);

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    producerTelemetries.add(
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_MAVEN,
            AnalysisType.COORDINATE.name(), "mvn", null));

    assertTelemetryInformation(app.getId(), producerTelemetries);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testDependencyResolve_NoExactPurls_MatchUsingPurlPrefix() throws Exception {
    JsonNode dependenciesJson =
        getJsonNodeInformation("report-dependency-match-purl-prefix/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-dependency-match-purl-prefix/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-dependency-match-purl-prefix/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-dependency-match-purl-prefix/data.json");

    List<String> expectedKnownPurls = List.of("pkg:pypi/multidict@6.1.0?extension=whl&qualifier=cp311-cp311-win_amd64",
        "pkg:pypi/tomlkit@0.13.2?extension=tar.gz",
        "pkg:pypi/protobuf@4.25.5?extension=whl&qualifier=cp37-abi3-manylinux2014_x86_64",
        "pkg:pypi/gitpython@3.0.5?extension=tar.gz");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();
    assertDependencyInfo(bomJson, 2, 2, 0, 0, 0, 4, 0, summaryJson, dataJson, app, expectedKnownPurls);
  }

  @Test
  public void processInnerSource_multiModule() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication(
        "pkg:maven/com.sonatype.insight.scan/insight-scanner-hashing-asm60?type=jar", appInnerSource);
    tempEntity.newInnerSourceApplication(
        "pkg:maven/com.sonatype.insight.scan/insight-scanner-hashing?type=jar", appInnerSource);

    PackageUrlIdentifier knownModule1 = new PackageUrlIdentifier(
        "pkg:maven/com.sonatype.insight.scan/insight-test-reverse-proxy@2.23.5-SNAPSHOT?type=jar");
    PackageUrlIdentifier knownModule2 =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.insight.scan/insight-scanner-model@2.23.5-SNAPSHOT?type=jar");
    PackageUrlIdentifier knownModule3 = new PackageUrlIdentifier(
        "pkg:maven/com.sonatype.insight.scan/insight-scanner-model-io@2.23.5-SNAPSHOT?type=jar");
    PackageUrlIdentifier knownModule4 =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.insight.scan/insight-scanner-core@2.23.5-SNAPSHOT?type=jar");

    List<String> knownComponents =
        Arrays.asList(knownModule1.getPackageUrl(), knownModule2.getPackageUrl(), knownModule3.getPackageUrl(),
            knownModule4.getPackageUrl());

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-multi-module/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-multi-module/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-multi-module/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-multi-module/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender, telemetryUtils, observedInnerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO,
        proprietaryConfigService)
        .resolve();

    List<InnerSourceApplication> innerSourceApplications = innerSourceApplicationDAO.getByApplicationId(app.getId());
    assertThat(innerSourceApplications).hasSize(9);

    assertThat(innerSourceApplications).extracting(InnerSourceApplication::getApplicationId).containsOnly(app.getId());
    assertDependencyInfo(bomJson, 10, 2, 1, 2, 2, 14, 0, summaryJson, dataJson, appInnerSource, knownComponents);

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    producerTelemetries.add(
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_MAVEN,
            AnalysisType.COORDINATE.name(), "mvn", null));

    assertTelemetryInformation(app.getId(), producerTelemetries);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();

    // CLM-39951: every direct-dependency InnerSource association is resolved with a single batch
    // query rather than one query per component.
    verify(observedInnerSourceApplicationDAO, times(1)).getByPackageUrls(Mockito.any());
  }

  @Test
  public void processInnerSource_singleModule() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    PackageUrlIdentifier innerSourceModel =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.insight.scan/insight-module-model@1.0.0-SNAPSHOT?type=jar");
    PackageUrlIdentifier innerScannerArchive =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar");
    PackageUrlIdentifier innerSourceClient =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.insight.scan/insight-client-utils@1.0.0-SNAPSHOT?type=jar");

    List<String> knownComponents = Arrays.asList(innerSourceModel.getPackageUrl(), innerScannerArchive.getPackageUrl(),
        innerSourceClient.getPackageUrl());

    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-module-model?type=jar",
        appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive?type=jar",
        appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-client-utils?type=jar",
        appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();
    assertDependencyInfo(bomJson, 4, 6, 3, 5, 4, 14, 0, summaryJson, dataJson, appInnerSource, knownComponents);

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

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    producerTelemetries.add(
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_MAVEN,
            AnalysisType.HASH.name(), "mvn", null));

    assertTelemetryInformation(app.getId(), producerTelemetries);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void processInnerSource_knownInnerSourceParent() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-module-model?type=jar",
        appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive?type=jar",
        appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-client-utils?type=jar",
        appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-known/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-known/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-known/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-known/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertDependencyInfo(bomJson, 4, 7, 3, 5, 6, 17, 0, summaryJson, dataJson, appInnerSource);

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    producerTelemetries.add(
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_MAVEN,
            AnalysisType.HASH.name(), "mvn", null));

    assertTelemetryInformation(app.getId(), producerTelemetries);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void processInnerSource_nested_transitive_dep() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-module-model?type=jar",
        appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive?type=jar",
        appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-client-utils?type=jar",
        appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-nested-transitive/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-nested-transitive/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-nested-transitive/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-nested-transitive/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();
    assertDependencyInfo(bomJson, 4, 17, 3, 15, 5, 25, 1, summaryJson, dataJson, appInnerSource);

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    producerTelemetries.add(
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_MAVEN,
            AnalysisType.HASH.name(), "mvn", null));

    assertTelemetryInformation(app.getId(), producerTelemetries);

    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void processInnerSource_unknown_components() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.insight.scan/insight-module-model?type=jar",
        appInnerSource);
    PackageUrlIdentifier modelId =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.insight.scan/insight-module-model@1.0.0-SNAPSHOT?type=jar");
    tempEntity.newInnerSourceApplication(
        "pkg:maven/com.sonatype.insight.scan/insight-innersource-child?type=jar", appInnerSource);
    PackageUrlIdentifier childId =
        new PackageUrlIdentifier("pkg:maven/com.sonatype.insight.scan/insight-innersource-child@2.0.0?type=jar");
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.nexus/nexus-platform-api?type=jar", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-unknown-components/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-unknown-components/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-unknown-components/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-unknown-components/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertDependencyInfo(bomJson, 1, 2, 1, 2, 0, 3, 0, summaryJson, dataJson, appInnerSource);

    assertUpdatedBomAttributeValue(bomJson, modelId, "packageUrl",
        "pkg:maven/com.sonatype.insight.scan/insight-module-model@1.0.0-SNAPSHOT?type=jar");
    assertUpdatedBomAttributeValue(bomJson, childId, "packageUrl",
        "pkg:maven/com.sonatype.insight.scan/insight-innersource-child@2.0.0?type=jar");

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    producerTelemetries.add(
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_MAVEN,
            AnalysisType.HASH.name(), "mvn", null));

    assertTelemetryInformation(app.getId(), producerTelemetries);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void processInnerSource_Without_DependencyTree() throws Exception {

    Application app = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.innersource.data/innersource-data", app);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-not-root/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-not-root/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-not-root/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-not-root/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertDependencyInfo(bomJson, 0, 0, 0, 0, 4, 3, 1, summaryJson, dataJson, app);

    verify(telemetrySender, never()).send(Mockito.any(TelemetryData.class));
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR)).isNull();
  }

  @Test
  public void testProcessInnerSourceDependencies_without_children() throws Exception {

    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.innersource.data/innersource-data", appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-not-children/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-not-children/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-not-children/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-not-children/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertDependencyInfo(bomJson, 0, 0, 0, 0, 4, 3, 1, summaryJson, dataJson, appInnerSource);
    verify(telemetrySender, never()).send(Mockito.any(TelemetryData.class));
  }

  @Test
  public void testProcessInnerSourceDependencies_dependencyIsTransitiveAndDirect() throws Exception {

    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:maven/org.example/ACME-data?type=jar", appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/org.example/ACME-business?type=jar", appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/org.example/ACME-Producer?type=jar", appInnerSource);

    JsonNode dependenciesJson =
        getJsonNodeInformation("report-innersource-direct-transitive-dependency/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-direct-transitive-dependency/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-direct-transitive-dependency/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-direct-transitive-dependency/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    List<InnerSourceApplication> innerSourceApplications = innerSourceApplicationDAO.getByApplicationId(app.getId());
    assertThat(innerSourceApplications).hasSize(2);

    assertThat(innerSourceApplications).extracting(InnerSourceApplication::getApplicationId).containsOnly(app.getId());
    assertDependencyInfo(bomJson, 3, 2, 1, 2, 0, 5, 0, summaryJson, dataJson, appInnerSource);

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    producerTelemetries.add(
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_MAVEN,
            AnalysisType.HASH.name(), "mvn", null));

    assertTelemetryInformation(app.getId(), producerTelemetries);
  }

  @Test
  public void testProcessInnerSourceDependencies_producer_not_exists() throws Exception {

    Application appInnerSource = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-not-children/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-not-children/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-not-children/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-not-children/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertDependencyInfo(bomJson, 0, 0, 0, 0, 4, 3, 1, summaryJson, dataJson, appInnerSource);

    verify(telemetrySender, never()).send(Mockito.any(TelemetryData.class));
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testProcessDependencyTree_not_maven_plugin() throws Exception {
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-depTree-not-maven-plugin/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-depTree-not-maven-plugin/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-depTree-not-maven-plugin/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-depTree-not-maven-plugin/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

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
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testProcessDependencyTree_with_maven_plugin() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication("pkg:maven/com.innersource/InnerSource-Producer?type=jar", appInnerSource);

    JsonNode dependenciesJson =
        getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

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
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void processInnerSource_directDependencyRegisteredToSameApp_notMarkedAsInnerSource() throws Exception {
    // CLM-39951: a direct-dependency purl registered as InnerSource to the *current* application must
    // be excluded by getInnerSourceApplicationExcludingApplication and therefore must NOT be tagged as
    // an InnerSource dependency of itself. This guards the in-memory same-app filter that replaced the
    // removed getByPackageUrlExcludingApplication DAO query.
    tempEntity.newInnerSourceApplication("pkg:maven/com.innersource/known-direct?type=jar", app);

    JsonNode dependenciesJson =
        getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-depTree-with-maven-plugin/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender, telemetryUtils, observedInnerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO,
        proprietaryConfigService)
        .resolve();

    ComponentIdentifier knownDirect =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "known-direct", "2.8.1", "", "jar");
    // Excluded because it belongs to the current app: it stays a regular direct dependency.
    assertBomNodeDependencyInfo(bomJson, knownDirect, true, false, null, null);
    assertThat(findNodeById(bomJson, knownDirect).get(ComponentLoader.INNER_SOURCE_DATA_FIELD)).isNull();
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();

    // CLM-39951: the same-app-exclusion path must also resolve associations from the single batch query.
    verify(observedInnerSourceApplicationDAO, times(1)).getByPackageUrls(Mockito.any());
  }

  @Test
  public void processInnerSource_unknownTransitiveUnderInnerSourceParent_resolvedFromBatchNotPerComponentQuery() throws Exception {
    // CLM-40956: an UNKNOWN transitive dependency under an InnerSource parent must be resolved from the
    // up-front batch map (getByPackageUrls) rather than a per-component getByPackageUrl call. This guards
    // against reintroducing the N+1 query pattern for transitive InnerSource lookups (follow-up to CLM-39951).
    Application appInnerSource = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication("pkg:maven/com.innersource/known-direct?type=jar", appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:maven/com.innersource/unknown-transitive?type=jar", appInnerSource);

    JsonNode dependenciesJson =
        getJsonNodeInformation("report-innersource-transitive-unknown-with-purl/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-transitive-unknown-with-purl/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-transitive-unknown-with-purl/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-transitive-unknown-with-purl/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender, telemetryUtils, observedInnerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO,
        proprietaryConfigService)
        .resolve();

    String unknownTransitivePurl = "pkg:maven/com.innersource/unknown-transitive?type=jar";

    // The transitive InnerSource association must NOT be looked up with a per-component query.
    verify(observedInnerSourceApplicationDAO, never())
        .getByPackageUrl(Mockito.argThat(purl -> unknownTransitivePurl.equals(purl.getPackageUrl())));

    // It must instead be covered by the single up-front batch query.
    verify(observedInnerSourceApplicationDAO, times(1))
        .getByPackageUrls(Mockito.argThat(purls -> purls.stream()
            .map(PackageUrlIdentifier::getPackageUrl)
            .anyMatch(unknownTransitivePurl::equals)));

    // Behavior preserved: the unknown transitive that exists as InnerSource is marked as known (exact).
    ComponentIdentifier unknownTransitive =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "unknown-transitive", "2.8.1", "", "jar");
    JsonNode bomNode = findNodeById(bomJson, unknownTransitive);
    assertThat(bomNode).isNotNull();
    assertThat(bomNode.get("matchState").asText()).isEqualTo(MatchState.EXACT.getId());
  }

  @Test
  public void processInnerSource_unknownTransitiveRegisteredToSameApp_stillMarkedKnown() throws Exception {
    // CLM-40956: the transitive path intentionally applies NO same-app exclusion (unlike the direct path,
    // which uses getInnerSourceApplicationExcludingApplication). This preserves the pre-batch getByPackageUrl
    // behavior, so an UNKNOWN transitive that exists as InnerSource registered to the *current* app must still
    // be marked as known. This locks in the asymmetry between the direct and transitive resolution paths.
    Application appInnerSource = tempEntity.newApplicationWithParent();
    // Parent belongs to a different app so the direct path recurses into its transitive children.
    tempEntity.newInnerSourceApplication("pkg:maven/com.innersource/known-direct?type=jar", appInnerSource);
    // Transitive is registered to the current app (same app); the transitive path must not exclude it.
    tempEntity.newInnerSourceApplication("pkg:maven/com.innersource/unknown-transitive?type=jar", app);

    JsonNode dependenciesJson =
        getJsonNodeInformation("report-innersource-transitive-unknown-with-purl/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-transitive-unknown-with-purl/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-transitive-unknown-with-purl/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-transitive-unknown-with-purl/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, observedInnerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO,
        proprietaryConfigService)
        .resolve();

    String unknownTransitivePurl = "pkg:maven/com.innersource/unknown-transitive?type=jar";

    // Defence-in-depth: even when the InnerSource association is registered to the *current* app, the transitive
    // path must resolve it from the up-front batch map, never via a per-component query. This guards the stated
    // asymmetry (no same-app exclusion on the transitive path) against a future regression that reaches the same
    // matchState through a per-component DB call.
    verify(observedInnerSourceApplicationDAO, never())
        .getByPackageUrl(Mockito.argThat(purl -> unknownTransitivePurl.equals(purl.getPackageUrl())));

    ComponentIdentifier unknownTransitive =
        ComponentIdentifier.createMavenCoordinates("com.innersource", "unknown-transitive", "2.8.1", "", "jar");
    JsonNode bomNode = findNodeById(bomJson, unknownTransitive);
    assertThat(bomNode).isNotNull();
    assertThat(bomNode.get("matchState").asText()).isEqualTo(MatchState.EXACT.getId());
  }

  @Test
  public void testResolve_TransitiveAndDirect() throws Exception {
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-transitive-and-direct/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-transitive-and-direct/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-transitive-and-direct/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-transitive-and-direct/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    ComponentIdentifier knownDirect = ComponentIdentifier
        .createMavenCoordinates("com.innersource", "known-direct", "2.8.1", "", "jar");
    ComponentIdentifier knownTransitiveAndDirect = ComponentIdentifier
        .createMavenCoordinates("com.innersource", "known-transitive-and-direct", "2.8.1", "", "jar");
    assertBomNodeDependencyInfo(bomJson, knownTransitiveAndDirect, true, Collections.singleton(knownDirect));
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_MultipleParents() throws Exception {
    Application appInnerSource1 = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication("pkg:maven/com.innersource/known-direct-1?type=jar", appInnerSource1);
    Application appInnerSource2 = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication("pkg:maven/com.innersource/known-direct-2?type=jar", appInnerSource2);
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-multiple-parents/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-multiple-parents/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-multiple-parents/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-multiple-parents/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

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
            PackageUrlIdentifier.fromComponentIdentifier(knownDirect2).getPackageUrl()));
    assertBomNodeDependencyInfo(bomJson, knownTransitive, false, false, Sets.newHashSet(knownDirect1, knownDirect2),
        expectedInnerSourceData);
    ComponentIdentifier knownTransitiveTransitive = ComponentIdentifier
        .createMavenCoordinates("commons-io", "commons-io", "2.6", "", "jar");
    assertBomNodeDependencyInfo(bomJson, knownTransitiveTransitive, false, false,
        Collections.singleton(knownTransitive), expectedInnerSourceData);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_AddInnerSource_WhenNotIdentifiedByMJA() throws Exception {
    Application appInnerSource1 = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication("pkg:npm/producer", appInnerSource1);

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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertBomNodeDependencyInfo(bomJson, innerSourceId, true, true, null, expectedInnerSourceData);
    assertSummaryCounters(summaryJson, dataJson, 3, 4);

    JsonNode newIsNode = findNodeById(bomJson, innerSourceId);
    assertThat(newIsNode).isNotNull();
    AnalyzerFeatures analyzerFeatures = JsonUtils
        .asPojo(newIsNode.get("analyzerFeatures"), AnalyzerFeatures.class);

    assertThat(newIsNode.get("hash").asText()).isNotBlank();
    assertThat(newIsNode.get("proprietary").asBoolean()).isFalse();
    // CLM-39739: HDS-miss components emit JSON null, not a wall-clock timestamp.
    assertThat(newIsNode.get("createTime")).isEqualTo(NullNode.getInstance());
    assertThat(newIsNode.get("relativePopularity").asInt()).isZero();
    assertThat(newIsNode.get("filenames").get(0).asText()).isEqualTo(
        PackageUrlIdentifier.fromComponentIdentifier(innerSourceId).getPackageUrl());
    assertThat(newIsNode.get("pathnames").get(0).asText()).isEqualTo(
        "dependency:/"
            + PackageUrlIdentifier.fromComponentIdentifier(innerSourceId).getPackageUrl().replace("/", "\\"));
    assertThat(newIsNode.get("proprietary").asBoolean()).isFalse();
    assertThat(analyzerFeatures.getAnalysisSource()).isEqualTo(AnalysisSource.THIRD_PARTY);
    assertThat(analyzerFeatures.getAnalysisType()).isEqualTo(AnalysisType.COORDINATE);
    assertThat(analyzerFeatures.getScanClient()).isEqualTo("ci");
    assertThat(analyzerFeatures.isHasIdentity()).isFalse();
    assertThat(analyzerFeatures.isHasLicense()).isFalse();
    assertThat(analyzerFeatures.isHasSecurity()).isFalse();
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_NewInnerSourceNode_NotProprietary() throws Exception {
    Application innerSourceApplication = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication("pkg:npm/producer", innerSourceApplication);
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/bom.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/data.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/summary.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    ComponentIdentifier innerSourceId = ComponentIdentifier.createNpmCoordinates("producer", "file:../producer");
    JsonNode newIsNode = findNodeById(bomJson, innerSourceId);
    assertThat(newIsNode).isNotNull();
    assertThat(newIsNode.get("proprietary").asBoolean()).isFalse();
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_NewInnerSourceNode_Proprietary() throws Exception {
    Application innerSourceApplication = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication("pkg:npm/producer", innerSourceApplication);
    tempEntity.newProprietaryConfig(app.getId(), Collections.emptyList(), Collections.singletonList("producer"));
    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/bom.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/data.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-npm-add-unrecognized/summary.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    ComponentIdentifier innerSourceId = ComponentIdentifier.createNpmCoordinates("producer", "file:../producer");
    JsonNode newIsNode = findNodeById(bomJson, innerSourceId);
    assertThat(newIsNode).isNotNull();
    assertThat(newIsNode.get("proprietary").asBoolean()).isTrue();
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_npm() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:npm/producer-one", appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:npm/producer-two", appInnerSource);
    tempEntity.newInnerSourceApplication("pkg:npm/consumer", app);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-npm/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-npm/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-npm/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-npm/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();
    assertDependencyInfo(bomJson, 3, 7, 2, 6, 0, 10, 0, summaryJson, dataJson, appInnerSource);

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    producerTelemetries.add(
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_NPM,
            AnalysisType.COORDINATE.name(), "cli", null));

    assertTelemetryInformation(app.getId(), producerTelemetries);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_otherFormat() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:pypi/pypi-app?extension=zip", appInnerSource);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-otherformat/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-otherformat/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-otherformat/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-otherformat/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertDependencyInfo(bomJson, 2, 1, 1, 1, 0, 3, 0, summaryJson, dataJson, appInnerSource);

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    InnerSourceProducerComponentTelemetry producerInfo =
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_PYPI,
            AnalysisType.COORDINATE.name(), "cli", "SBOM");
    producerTelemetries.add(producerInfo);

    assertTelemetryInformation(app.getId(), producerTelemetries);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_sbom() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:golang/acme-app", appInnerSource);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-sbom/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-sbom/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-sbom/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-sbom/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();
    assertDependencyInfo(bomJson, 1, 1, 1, 1, 0, 2, 0, summaryJson, dataJson, appInnerSource);

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    InnerSourceProducerComponentTelemetry producerInfo =
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_GOLANG,
            AnalysisType.COORDINATE.name(), "cli", ItemContentType.SBOM.name());
    producerTelemetries.add(producerInfo);

    assertTelemetryInformation(app.getId(), producerTelemetries);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_sbom_thirdParty() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:maven/org.example/ACME-Producer@1.0-SNAPSHOT?type=pom", appInnerSource);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-sbom-third-party/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-sbom-third-party/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-sbom-third-party/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-sbom-third-party/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertDependencyInfo(bomJson, 3, 3, 0, 0, 0, 6, 0, summaryJson, dataJson, appInnerSource);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_maven_unknownComponent() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:maven/org.example/ACME-Producer@1.0-SNAPSHOT?type=pom", appInnerSource);

    String folderName = "report-maven-plugin-unknown-component";
    JsonNode dependenciesJson = getJsonNodeInformation(folderName + "/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation(folderName + "/bom.json");
    JsonNode summaryJson = getJsonNodeInformation(folderName + "/summary.json");
    JsonNode dataJson = getJsonNodeInformation(folderName + "/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertDependencyInfo(bomJson, 1, 0, 0, 0, 0, 0, 1, summaryJson, dataJson, appInnerSource);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_maven_unknownComponent_modules() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:maven/org.example/ACME-Producer@1.0-SNAPSHOT?type=pom", appInnerSource);

    String folderName = "report-maven-plugin-unknown-component-modules";
    JsonNode dependenciesJson = getJsonNodeInformation(folderName + "/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation(folderName + "/bom.json");
    JsonNode summaryJson = getJsonNodeInformation(folderName + "/summary.json");
    JsonNode dataJson = getJsonNodeInformation(folderName + "/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertDependencyInfo(bomJson, 2, 4, 0, 0, 0, 5, 1, summaryJson, dataJson, appInnerSource);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testResolve_duplicatedElementTree() throws Exception {
    Application appInnerSource = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceApplication("pkg:maven/test/hashing?type=jar", appInnerSource);

    JsonNode dependenciesJson = getJsonNodeInformation("report-innersource-dup-dependency-tree/dependencies.json");
    JsonNode bomJson = getJsonNodeInformation("report-innersource-dup-dependency-tree/bom.json");
    JsonNode summaryJson = getJsonNodeInformation("report-innersource-dup-dependency-tree/summary.json");
    JsonNode dataJson = getJsonNodeInformation("report-innersource-dup-dependency-tree/data.json");

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertDependencyInfo(bomJson, 3, 2, 1, 2, 1, 6, 0, summaryJson, dataJson, appInnerSource);

    Set<InnerSourceProducerComponentTelemetry> producerTelemetries = new HashSet<>();
    InnerSourceProducerComponentTelemetry producerInfo =
        new InnerSourceProducerComponentTelemetry(appInnerSource.getId(), ComponentIdentifier.FORMAT_MAVEN,
            AnalysisType.COORDINATE.name(), "thirdPartyApi", ItemContentType.SBOM.name());
    producerTelemetries.add(producerInfo);

    assertTelemetryInformation(app.getId(), producerTelemetries);
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
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

    DependencyResolver.getInstance(dependenciesJson, bomJson, dataJson, summaryJson, StageTypes.RELEASE.getId(), app,
        telemetrySender,
        telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    assertThat(bomNode.get("directDependency")).isNotNull();
    assertThat(bomNode.get("innerSource")).isNotNull();
    assertThat(bomNodeNoPathnames.get("directDependency")).isNull();
    assertThat(bomNodeNoPathnames.get("innerSource")).isNull();
    assertThat(bomNodeEmptyPathnames.get("directDependency")).isNull();
    assertThat(bomNodeEmptyPathnames.get("innerSource")).isNull();
    assertThat(bomNodeBadPathnames.get("directDependency")).isNull();
    assertThat(bomNodeBadPathnames.get("innerSource")).isNull();
    assertThat(bomJson.get(DependencyResolver.FIELD_DEPENDENCY_INDICATOR).asBoolean()).isTrue();
  }

  @Test
  public void testIsProprietary_InitializesIsProprietaryOnceForApplication() {
    Application application = tempEntity.newApplicationWithParent();
    Application other = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), null, Collections.singletonList("p1"));
    tempEntity.newProprietaryConfig(other.getId(), null, Collections.singletonList("p2"));
    DependencyResolver dependencyResolver =
        DependencyResolver.getInstance(null, null, null, null, null, application, null, null, null, null, null,
            proprietaryConfigService);
    assertThat(dependencyResolver.isProprietary).isNull();

    assertThat(dependencyResolver.isProprietaryComponent(new PackageUrlIdentifier("pkg:npm/p1@v"))).isTrue();
    Predicate<String> isProprietary = dependencyResolver.isProprietary;
    assertThat(isProprietary).isNotNull();

    assertThat(dependencyResolver.isProprietaryComponent(new PackageUrlIdentifier("pkg:npm/p2@v"))).isFalse();
    assertThat(dependencyResolver.isProprietary).isEqualTo(isProprietary);
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
    assertThat(bomNode).isNotNull();
    PackageUrlIdentifier purlId = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    assertThat(ComponentIdentifierAdapter.getPackageUrlIdentifier(bomNode)).isEqualTo(purlId);
    assertThat(ComponentIdentifierAdapter.getComponentIdentifier(bomNode)).isEqualTo(componentIdentifier);
    assertThat(bomNode.get("directDependency").asBoolean()).isEqualTo(isDirect);
    assertThat(bomNode.get("innerSource").asBoolean()).isEqualTo(isInnerSource);
    Set<ComponentIdentifier> actualParentIds = null;
    JsonNode parentComponentPurls = bomNode.path(ComponentLoader.PARENT_COMPONENT_PURLS_FIELD);
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
      JsonNode innerSourceDataArray = bomNode.get(ComponentLoader.INNER_SOURCE_DATA_FIELD);
      Set<InnerSourceData> actualInnerSourceData = toInnerSourceDataSet(innerSourceDataArray);
      assertThat(actualInnerSourceData).containsExactlyInAnyOrder(innerSourceData.toArray(new InnerSourceData[0]));
    }
  }

  private JsonNode findNodeById(final JsonNode bomJson, final ComponentIdentifier identifier) {
    PackageUrlIdentifier purlId = PackageUrlIdentifier.fromComponentIdentifier(identifier);
    for (JsonNode node : bomJson.get("aaData")) {
      PackageUrlIdentifier nodePurl = ComponentIdentifierAdapter.getPackageUrlIdentifier(node);
      if (nodePurl != null) {
        if (Objects.equals(purlId, nodePurl)) {
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

  private void assertDependencyInfo(
      final JsonNode bomJson,
      final int expectedDirectDependencies,
      final int expectedTransitiveDependencies,
      final int expectedDirectISDependencies,
      final int expectedTransitiveISDependencies,
      final int expectedUnknownDependencyType,
      final int expectedKnownComponentsCount,
      final int expectedUnknownComponentsCount,
      final JsonNode summaryJson,
      final JsonNode dataJson,
      final Application innerSourceApp)
  {
    assertDependencyInfo(bomJson, expectedDirectDependencies, expectedTransitiveDependencies,
        expectedDirectISDependencies, expectedTransitiveISDependencies, expectedUnknownDependencyType,
        expectedKnownComponentsCount, expectedUnknownComponentsCount, summaryJson, dataJson, innerSourceApp, null);
  }

  private void assertDependencyInfo(
      final JsonNode bomJson,
      final int expectedDirectDependencies,
      final int expectedTransitiveDependencies,
      final int expectedDirectISDependencies,
      final int expectedTransitiveISDependencies,
      final int expectedUnknownDependencyType,
      final int expectedKnownComponentsCount,
      final int expectedUnknownComponentsCount,
      final JsonNode summaryJson,
      final JsonNode dataJson,
      final Application innerSourceApp,
      final List<String> expectedKnownComponents)
  {
    // List of components that are direct
    List<JsonNode> direct = new ArrayList<>();
    // List of components that are transitive
    List<JsonNode> transitive = new ArrayList<>();
    // List of components that don't have dep information
    List<JsonNode> unknownDepType = new ArrayList<>();
    // Purl list of the known components
    Set<String> componentList = new HashSet<>();
    Pair<Integer, Integer> componentsCount =
        getDependencyInfo(bomJson, direct, transitive, unknownDepType, componentList);

    int actualUnknownCount = componentsCount.getRight();
    int actualKnownCount = componentsCount.getLeft();

    assertThat(actualKnownCount).isEqualTo(expectedKnownComponentsCount);
    assertThat(actualUnknownCount).isEqualTo(expectedUnknownComponentsCount);

    assertThat(unknownDepType).hasSize(expectedUnknownDependencyType);
    assertThat(direct).hasSize(expectedDirectDependencies);
    assertThat(transitive).hasSize(expectedTransitiveDependencies);

    int expectedCount = direct.size() + transitive.size() + unknownDepType.size();

    if (expectedDirectISDependencies > 0) {
      assertDirectDependenciesInformation(direct, expectedDirectISDependencies, innerSourceApp);
    }
    if (expectedTransitiveISDependencies > 0) {
      assertTransitiveInformation(transitive, expectedTransitiveISDependencies, innerSourceApp);
    }
    if (expectedKnownComponents != null) {
      assertThat(componentList).containsAll(expectedKnownComponents);
    }
    assertSummaryCounters(summaryJson, dataJson, expectedCount - actualUnknownCount, expectedCount);
  }

  private Pair<Integer, Integer> getDependencyInfo(
      final JsonNode bomJson,
      List<JsonNode> direct,
      List<JsonNode> transitive,
      List<JsonNode> unknownDepType,
      Set<String> componentList)
  {
    int unknownComponentsCount = 0;
    int knownComponentsCount = 0;
    List<JsonNode> bomNodes = loadBomInfo(bomJson);

    for (JsonNode bomChild : bomNodes) {
      JsonNode directDependency = bomChild.get("directDependency");
      if (directDependency != null) {
        if (directDependency.asBoolean()) {
          direct.add(bomChild);
        }
        else {
          transitive.add(bomChild);
        }
      }
      else {
        // Component has no dependency type
        unknownDepType.add(bomChild);
      }

      if (bomChild.hasNonNull("packageUrl")) {
        componentList.add(bomChild.get("packageUrl").asText());
      }

      String matchState = bomChild.get("matchState").asText();
      if (MatchState.UNKNOWN.getId().equals(matchState)) {
        unknownComponentsCount++;
      }
      else {
        knownComponentsCount++;
      }
    }
    return Pair.of(knownComponentsCount, unknownComponentsCount);
  }

  private List<JsonNode> loadBomInfo(final JsonNode bomJson) {
    List<JsonNode> bomNodes = new ArrayList<>();
    for (JsonNode bomChild : bomJson.get("aaData")) {
      bomNodes.add(bomChild);
    }
    return bomNodes;
  }

  private void assertDirectDependenciesInformation(
      final List<JsonNode> directs,
      final int expectedInnerSourceComponents,
      final Application innerSourceApp)
  {
    List<JsonNode> directInnerSourceComponents = new ArrayList<>();
    assertThat(directs)
        .isNotEmpty()
        .allSatisfy(directNode -> assertDirectInfo(directNode, directInnerSourceComponents, innerSourceApp));
    assertThat(directInnerSourceComponents).hasSize(expectedInnerSourceComponents);
  }

  private void assertDirectInfo(
      final JsonNode directDependency,
      final List<JsonNode> directInnerSourceComponents,
      final Application appInnerSource)
  {
    assertThat(directDependency).isNotNull();
    assertThat(directDependency.get("componentIdentifier")).isNotNull();
    assertThat(directDependency.get("packageUrl")).isNotNull();
    assertThat(directDependency.get("directDependency").asBoolean()).isTrue();
    assertThat(directDependency.get("matchState").asText()).isEqualTo(MatchState.EXACT.getId());

    JsonNode innerSourceData = directDependency.get(ComponentLoader.INNER_SOURCE_DATA_FIELD);
    if (innerSourceData != null) {
      JsonNode innerSource = directDependency.get("innerSource");
      boolean isInnerSource = false;
      if (innerSource != null) {
        if (innerSource.asBoolean()) {
          directInnerSourceComponents.add(directDependency);
          assertThat(directDependency.get("innerSourceData")).isNotNull();
        }
        isInnerSource = innerSource.asBoolean();
      }
      assertInnerSourceTree(directDependency.get(ComponentLoader.INNER_SOURCE_DATA_FIELD), appInnerSource,
          isInnerSource,
          true);
    }
    assertIdentificationSourceAndAnalyzerFeatures(directDependency);
  }

  private void assertSummaryCounters(
      final JsonNode summaryJson,
      final JsonNode dataJson,
      final int expectedCount,
      final Integer totalArtifactCount)
  {
    assertThat(summaryJson).isNotNull();
    assertThat(summaryJson.get("knownArtifactCount").asInt()).isEqualTo(expectedCount);
    assertThat(summaryJson.get("totalArtifactCount").asInt()).isEqualTo(totalArtifactCount);

    assertThat(dataJson).isNotNull();
    assertThat(dataJson.get("exactlyMatchedComponentCount").asInt()).isEqualTo(expectedCount);
    assertThat(dataJson.get("knownArtifactCount").asInt()).isEqualTo(expectedCount);
    assertThat(dataJson.get("totalArtifactCount").asInt()).isEqualTo(totalArtifactCount);
  }

  private void assertIdentificationSourceAndAnalyzerFeatures(final JsonNode bomInnerSource) {
    try {
      AnalyzerFeatures analyzerFeaturesInBom =
          JsonUtils.asPojo(bomInnerSource.get("analyzerFeatures"), AnalyzerFeatures.class);

      assertThat(analyzerFeaturesInBom).isNotNull();

      AnalyzerFeatures analyzerFeaturesExpected = null;
      if (analyzerFeaturesInBom.getAnalysisSource() == AnalysisSource.THIRD_PARTY) {

        String contentType = null;
        if ("SBOM".equals(analyzerFeaturesInBom.getManifestContentType())) {
          assertThat(bomInnerSource.get("identificationSource").asText()).isNotNull();
          contentType = "SBOM";
        }
        else {
          assertThat(bomInnerSource.get("identificationSource").asText()).isEqualTo(
              IdentificationSource.PACKAGE_MANIFEST.getId());
        }
        analyzerFeaturesExpected =
            new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.COORDINATE, "mvn", contentType);
      }
      else if (analyzerFeaturesInBom.getAnalysisSource() == AnalysisSource.SDS) {
        assertThat(bomInnerSource.get("identificationSource").asText())
            .isEqualTo(IdentificationSource.SONATYPE.getId());
        analyzerFeaturesExpected =
            new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.HASH, "mvn", true, true, true);
      }
      else {
        assertThat(bomInnerSource.get("identificationSource")).isNull();
      }
      assertThat(analyzerFeaturesInBom).usingRecursiveComparison()
          .ignoringFields("analysisType", "scanClient", "manifestContentType")
          .isEqualTo(analyzerFeaturesExpected);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void assertTransitiveInformation(
      final List<JsonNode> bomInnerSourceDependencies,
      final int expectedTransitiveISComponents,
      final Application appInnerSource)
  {
    List<JsonNode> transitiveInnerSourceComponents = new ArrayList<>();
    assertThat(bomInnerSourceDependencies)
        .isNotEmpty()
        .allSatisfy(
            transitiveDependency -> assertTransitiveDependency(transitiveDependency, transitiveInnerSourceComponents,
                appInnerSource));
    assertThat(transitiveInnerSourceComponents).hasSize(expectedTransitiveISComponents);
  }

  private void assertTransitiveDependency(
      final JsonNode transitiveDependency,
      final List<JsonNode> transitiveInnerSourceComponents,
      final Application appInnerSource)
  {
    assertThat(transitiveDependency).isNotNull();
    assertThat(transitiveDependency.get("componentIdentifier")).isNotNull();
    assertThat(transitiveDependency.get("packageUrl")).isNotNull();
    assertThat(transitiveDependency.get("directDependency").asBoolean()).isFalse();
    assertThat(transitiveDependency.get(ComponentLoader.PARENT_COMPONENT_PURLS_FIELD)).isNotNull();
    assertThat(transitiveDependency.get("innerSource").asBoolean()).isFalse();

    JsonNode innerSourceData = transitiveDependency.get(ComponentLoader.INNER_SOURCE_DATA_FIELD);
    if (innerSourceData != null) {
      boolean isInnerSource = false;
      JsonNode innerSourceNodeParent = transitiveDependency.get("innerSource");
      if (innerSourceNodeParent != null) {
        if (!innerSourceNodeParent.asBoolean()) {
          transitiveInnerSourceComponents.add(transitiveDependency);
        }
        isInnerSource = innerSourceNodeParent.asBoolean();
      }
      assertInnerSourceTree(innerSourceData, appInnerSource, isInnerSource, false);
    }
  }

  private void assertComponentNameForTransitiveDependencies(
      final List<JsonNode> bomInnerSourceDependencies,
      final Map<ComponentIdentifier, String> dependencyComponentPurls)
  {
    for (JsonNode transitiveDependencies : bomInnerSourceDependencies) {
      ComponentIdentifier componentIdentifier =
          ComponentIdentifierAdapter.getComponentIdentifier(transitiveDependencies);
      String expectedComponentName = dependencyComponentPurls.get(componentIdentifier);
      assertThat(transitiveDependencies.get(ComponentLoader.INNER_SOURCE_DATA_FIELD)
          .get(0)
          .get("innerSourceComponentPurl")
          .asText()).isEqualTo(expectedComponentName);
    }
  }

  private void assertTelemetryInformation(
      final String consumerId,
      final Set<InnerSourceProducerComponentTelemetry> producerListInformation)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.INNER_SOURCE_REPORT_USAGE);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).hasSize(1);

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put(InnerSourceConsumerTelemetry.ATTRIBUTE_NAME,
        new InnerSourceConsumerTelemetry(consumerId, consumerId, producerListInformation));

    assertThat(telemetryData.getAttributes().keySet().iterator().next())
        .isEqualTo(expectedAttributes.keySet().iterator().next());
    assertThat((InnerSourceConsumerTelemetry) telemetryData.getAttributes().values().iterator().next())
        .usingRecursiveComparison()
        .isEqualTo(expectedAttributes.values().iterator().next());
  }

  private void assertInnerSourceTree(
      final JsonNode innerSourceDataArray,
      final Application app,
      final boolean isInnerSource,
      final boolean isDirect)
  {
    try {
      Set<InnerSourceData> innerSourceData = toInnerSourceDataSet(innerSourceDataArray);
      assertThat(innerSourceData).extracting(InnerSourceData::getOwnerApplicationName).containsOnly(app.getName());
      assertThat(innerSourceData).extracting(InnerSourceData::getOwnerApplicationId).containsOnly(app.getId());
      if (isInnerSource && isDirect) {
        assertThat(innerSourceData).extracting(InnerSourceData::getInnerSourceComponentPurl).containsNull();
      }
      else {
        assertThat(innerSourceData).extracting(InnerSourceData::getInnerSourceComponentPurl).doesNotContainNull();
      }
    }
    catch (Exception e) {
      throw new RuntimeException();
    }
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
    return newDependencyResolver(null);
  }

  private DependencyResolver newDependencyResolver(String stageTypeId) {
    return new DependencyResolver(null, null, null, null, stageTypeId, app, telemetrySender, telemetryUtils,
        observedInnerSourceApplicationDAO, innerSourceVersionDAO, null, null);
  }

  private JsonNode getJsonNodeInformation(String path) throws IOException {
    return objectMapper.readTree(getClass().getResource("/DependencyResolverTest/" + path));
  }

  private void assertUpdatedBomAttributeValue(
      JsonNode bomJson,
      PackageUrlIdentifier lookupId,
      String fieldName,
      String fieldValue)
  {
    for (JsonNode dependency : bomJson.get("aaData")) {
      PackageUrlIdentifier purl = ComponentIdentifierAdapter.getPackageUrlIdentifier(dependency);
      if (lookupId.equals(purl)) {
        assertThat(dependency.get(fieldName).asText()).isEqualTo(fieldValue);
        return;
      }
    }
    fail("component identifier " + lookupId + " not found");
  }
}
