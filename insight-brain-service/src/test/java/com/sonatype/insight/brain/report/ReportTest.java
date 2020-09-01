/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import static com.sonatype.insight.brain.model.license.LicenseOverrideStatus.OVERRIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ReportTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();
  
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private Set<Integer> depths(Integer... depths) {
    return Sets.newHashSet(depths);
  }

  private InnerSourceComponentDAO innerSourceComponentDAOSpy;

  @Test
  public void testParseDependencyDepths_PreferNewStructure() throws Exception {
    JsonNode dependenciesJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependencies.json"));
    assertThat(dependenciesJson.path("gavDepths").isObject()).isTrue();

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier = Report.parseDependencyDepths(dependenciesJson);
    assertThat(depthsByIdentifier)
        .containsEntry(ComponentIdentifier.createMavenCoordinates("junit", "junit", "4.9", "", "jar"), depths(1));
    assertThat(depthsByIdentifier).containsEntry(
        ComponentIdentifier.createMavenCoordinates("org.slf4j", "slf4j-api", "1.6", "", "jar"), depths(1, 2, 3));
    assertThat(depthsByIdentifier).hasSize(2);
  }

  @Test
  public void testParseDependencyDepths_FallbackToOldStructure() throws Exception {
    JsonNode dependenciesJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependencies.json"));
    ((ObjectNode) dependenciesJson).remove("componentDepths");

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier = Report.parseDependencyDepths(dependenciesJson);
    assertThat(depthsByIdentifier).containsEntry(ComponentIdentifier.createMavenCoordinates("junit", "junit", "4.9"),
        depths(1));
    assertThat(depthsByIdentifier)
        .containsEntry(ComponentIdentifier.createMavenCoordinates("org.slf4j", "slf4j-api", "1.6"), depths(1, 2, 3));
    assertThat(depthsByIdentifier).hasSize(2);
  }

  @Test
  public void testAppendCacheBustingParams() throws Exception {
    String indexContent = "<script type='text/javascript' src='../brain/policy-assets/js/brain.client.js'></script>"
        + "<script type='text/javascript' src='../brain/policy-assets/js/cip-loader.js'></script>";
    String expectedIndexContent = "<script type='text/javascript' src='../brain/policy-assets/js/brain.client.js?1.0'>"
        + "</script><script type='text/javascript' src='../brain/policy-assets/js/cip-loader.js?1.0'></script>";

    ReportEntry entry = new ReportEntry("index.html", System.currentTimeMillis(), indexContent.getBytes("UTF-8"));
    entry = Report.appendCacheBustingParams(entry, "1.0");

    assertThat(entry.buf).isEqualTo(expectedIndexContent.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void testAugmentModified_NoLicenseOverrides() throws Exception {
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));

    JsonNode bomJsonAugmented = bomJson.deepCopy();
    Report.augmentModified(new HashSet<>(), bomJsonAugmented);

    assertThat(bomJson).isEqualTo(bomJsonAugmented);
    assertThat(bomJsonAugmented.get("aaData").get(0).has("modified")).isFalse();
    assertThat(bomJsonAugmented.get("aaData").get(1).has("modified")).isFalse();
  }

  @Test
  public void testAugmentModified_AppLicenseOverride() throws Exception {
    ComponentIdentifier anameHawk111 = ComponentIdentifier.createAnameCoordinates("hawk", "", "1.1.1");
    tempEntity.newLicenseOverride(tempEntity.newApplicationWithParent().getId(), anameHawk111, OVERRIDDEN, "Beerware");
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));

    JsonNode bomJsonAugmented = bomJson.deepCopy();
    Report.augmentModified(Sets.newHashSet(anameHawk111), bomJsonAugmented);

    assertThat(bomJson).isNotEqualTo(bomJsonAugmented);
    assertThat(bomJsonAugmented.get("aaData").get(0).has("modified")).isTrue();
    assertThat(bomJsonAugmented.get("aaData").get(1).has("modified")).isFalse();
  }

  @Test
  public void testAugmentModified_OrgLicenseOverrides() throws Exception {
    ComponentIdentifier npmHawk111 = ComponentIdentifier.createNpmCoordinates("hawk", "1.1.1");
    tempEntity.newLicenseOverride(tempEntity.newApplicationWithParent().getParentOwnerId(), npmHawk111,
        LicenseOverrideStatus.OVERRIDDEN, "Beerware");
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));

    JsonNode bomJsonAugmented = bomJson.deepCopy();
    Report.augmentModified(Sets.newHashSet(npmHawk111), bomJsonAugmented);

    assertThat(bomJson).isNotEqualTo(bomJsonAugmented);
    assertThat(bomJsonAugmented.get("aaData").get(0).has("modified")).isFalse();
    assertThat(bomJsonAugmented.get("aaData").get(1).has("modified")).isTrue();
  }

  @Test
  public void testAugmentDependenciesGraph_WithoutDependencyGraphNode() throws Exception {
    JsonNode dependenciesJson =
        new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependencies.json"));

    JsonNode dependenciesJsonAugmented = dependenciesJson.deepCopy();
    Report.augmentDependenciesGraph(dependenciesJsonAugmented);

    assertThat(dependenciesJson).isEqualTo(dependenciesJsonAugmented);
  }

  @Test
  public void testAugmentDependenciesGraph_WithDependencyGraphNode() throws Exception {
    JsonNode dependenciesJson =
        new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependenciesWithGraph.json"));
    JsonNode dependenciesJsonAugmented = dependenciesJson.deepCopy();

    Report.augmentDependenciesGraph(dependenciesJsonAugmented);

    assertThat(dependenciesJson).isNotEqualTo(dependenciesJsonAugmented);
    JsonNode dependencyGraphNode = dependenciesJsonAugmented.get("dependencyGraph");

    int expectedNumDirectDependencies = 15;
    assertThat(dependencyGraphNode.get(0).get("children")).hasSize(expectedNumDirectDependencies);
    assertThat(dependencyGraphNode.get(0).has("directDependency")).isFalse();
    for (int i = 0; i < expectedNumDirectDependencies; i++) {
      assertThat(dependencyGraphNode.get(0).get("children").get(i).get("directDependency").asBoolean()).isTrue();
    }

    int expectedTotalDependencies = 29;
    assertThat(dependencyGraphNode).hasSize(expectedTotalDependencies);
    int numDirect = 0;
    for (int i = 1; i < dependencyGraphNode.size(); i++) {
      assertThat(dependencyGraphNode.get(i).has("directDependency")).isTrue();
      if (dependencyGraphNode.get(i).get("directDependency").asBoolean()) {
        numDirect++;
      }
    }
    assertThat(numDirect).isEqualTo(expectedNumDirectDependencies);
  }

  @Test
  public void testWriteLicenseThreatsToReportFile() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newLicenseThreatGroup(app.getId(), "My group 1", 0, "Apache-2.0", "GPL-2.0");
    tempEntity.newLicenseThreatGroup(org.getId(), "My group 2", 5, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(org.getParentOrganizationId(), "My group 3", 9, "GPL-3.0");

    File reportFile = new File(tempDir.getRoot(), "test");

    Report.writeLicenseThreatsToReportFile(app, reportFile);

    File licenseThreatsFile = Report.getCacheFile(reportFile, "licensethreats.json");

    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
    ContainerNode<?> licenseThreats = JsonUtils.parse(Files.readAllBytes(licenseThreatsFile.toPath()));
    int countNotZero = 0;
    @SuppressWarnings("unchecked")
    Map<String, Integer> threatLevelsByMultiLicenseId = JsonUtils.asPojo(licenseThreats.get("aaData"), Map.class);
    for (String multiLicenseShortName : threatLevelsByMultiLicenseId.keySet()) {
      Set<String> simpleLicenseIds = multiLicenseDAO
          .getLicensesByMultiLicenseIdNotNull(multiLicenseDAO.getByNameNotNull(multiLicenseShortName).getId()).stream()
          .map(License::getId).collect(Collectors.toSet());
      if (simpleLicenseIds.contains("GPL-3.0")) {
        assertThat(threatLevelsByMultiLicenseId.get(multiLicenseShortName)).isEqualTo(9);
        countNotZero++;
      }
      else if (simpleLicenseIds.contains("GPL-2.0")) {
        assertThat(threatLevelsByMultiLicenseId.get(multiLicenseShortName)).isEqualTo(5);
        countNotZero++;
      }
      else if (simpleLicenseIds.contains("Apache-2.0")) {
        assertThat(threatLevelsByMultiLicenseId.get(multiLicenseShortName)).isEqualTo(0);
        countNotZero++;
      }
      else {
        assertThat(threatLevelsByMultiLicenseId.get(multiLicenseShortName))
            .as("Threat for multi license %s", multiLicenseShortName)
            .isNull();
      }
    }
    assertThat(threatLevelsByMultiLicenseId).hasSize(multiLicenseDAO.getAll().size());
    assertThat(countNotZero).isPositive();
  }

  @Test
  public void testFetchReport_CreateInnerSourceComponent() throws Exception {
    InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();
    innerSourceComponentDAOSpy = spy(innerSourceComponentDAO);
    Application app = tempEntity.newApplicationWithParent();

    JsonNode dependenciesJson =
        new ObjectMapper().readTree(getClass().getResource("/ReportServiceTest/report-innersource/dependencies.json"));
    Report.createInnerSourceComponent(dependenciesJson, app.getId(), innerSourceComponentDAO);

    List<InnerSourceComponent> innerSourceComponents = innerSourceComponentDAOSpy.getByApplicationId(app.getId());
    assertThat(innerSourceComponents).hasSize(1);
    innerSourceComponentDAOSpy.delete(innerSourceComponents.get(0));

    assertThat(innerSourceComponents.get(0).getApplicationId()).isEqualTo(app.getId());

    assertThat(dependenciesJson).isNotNull();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifierAdapter.getComponentIdentifier(dependenciesJson.path("rootArtifact"));
    PackageUrlIdentifier expectedPurl = new PackageUrlIdentifier(String.format("pkg:maven/%s/%s",
        componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
        componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID)));

    assertThat(innerSourceComponents.get(0).getPackageUrl()).isEqualTo(expectedPurl.getPackageUrl());
  }

  @Test
  public void testFetchReport_UpdateInnerSourceComponentApp() throws Exception {
    InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();
    innerSourceComponentDAOSpy = spy(innerSourceComponentDAO);
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    JsonNode dependenciesJson =
        new ObjectMapper().readTree(getClass().getResource("/ReportServiceTest/report-innersource/dependencies.json"));
    app = tempEntity.newApplicationWithParent();
    Report.createInnerSourceComponent(dependenciesJson, app.getId(), innerSourceComponentDAOSpy);
    ArgumentCaptor<InnerSourceComponent> argument = ArgumentCaptor.forClass(InnerSourceComponent.class);
    verify(innerSourceComponentDAOSpy).update(argument.capture());
    assertThat(argument.getValue().getApplicationId()).isEqualTo(app.getId());
  }

  @Test
  public void testFetchReport_CreateInnerSourceComponent_NotRootArtifact() throws Exception {
    InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();
    innerSourceComponentDAOSpy = spy(innerSourceComponentDAO);
    Application app = tempEntity.newApplicationWithParent();

    JsonNode dependenciesJson =
        new ObjectMapper()
            .readTree(
                getClass().getResource(
                    "/ReportServiceTest/report-innersource-not-root/dependencies.json"));
    assertThat(Report.createInnerSourceComponent(dependenciesJson, app.getId(), innerSourceComponentDAO)).isFalse();
  }

  @Test
  public void testFetchReport_ExistingInnerSourceComponent() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();
    innerSourceComponentDAOSpy = spy(innerSourceComponentDAO);
    InnerSourceComponent innerSourceComponent =
        tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    JsonNode dependenciesJson =
        new ObjectMapper().readTree(getClass().getResource("/ReportServiceTest/report-innersource/dependencies.json"));
    Report.createInnerSourceComponent(dependenciesJson, app.getId(), innerSourceComponentDAO);
    verify(innerSourceComponentDAOSpy, never()).insert(innerSourceComponent);
  }

  @Test
  public void testFetchReport_NoInnerSourceComponent() throws Exception {
    InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();
    Application app = tempEntity.newApplicationWithParent();

    JsonNode dependenciesJson =
        new ObjectMapper().readTree(getClass().getResource("/ReportServiceTest/report/dependencies.json"));
    Report.createInnerSourceComponent(dependenciesJson, app.getId(), innerSourceComponentDAO);

    List<InnerSourceComponent> innerSourceComponents = innerSourceComponentDAO.getByApplicationId(app.getId());
    assertThat(innerSourceComponents).isEmpty();
  }

  @Test
  public void testProcessInnerSourceDependencies() throws Exception {

    Application app = tempEntity.newApplicationWithParent();

    ComponentIdentifier innerSourceModel = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.innersource.model", "innersource-model", "1.0.0-SNAPSHOT", "", "jar");
    ComponentIdentifier innerSourceData = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.innersource.data", "innersource-data", "1.0.0-SNAPSHOT", "", "jar");
    ComponentIdentifier innerSourceFront = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.innersource.frontend", "innersource-frontend", "1.0.0-SNAPSHOT", "",
            "jar");

    List<ComponentIdentifier> componentIdentifiers = Arrays.asList(innerSourceModel, innerSourceData, innerSourceFront);

    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.model/innersource-model", app);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.data/innersource-data", app);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.frontend/innersource-frontend", app);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson =
        objectMapper.readTree(getClass().getResource("/ReportServiceTest/report-innersource/dependencies.json"));
    JsonNode bomJson =
        objectMapper.readTree(getClass().getResource("/ReportServiceTest/report-innersource/bom.json"));
    JsonNode summaryJson =
        objectMapper.readTree(getClass().getResource("/ReportServiceTest/report-innersource/summary.json"));
    JsonNode dataJson =
        objectMapper.readTree(getClass().getResource("/ReportServiceTest/report-innersource/data.json"));
    Report.processInnerSourceComponents(dependenciesJson, bomJson, dataJson, summaryJson, app);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();

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
    assertThat(bomInnerSourceParent).hasSize(3);
    assertThat(bomInnerSourceDependencies).hasSize(4);

    assertThat(summaryJson).isNotNull();
    assertThat(summaryJson.get("knownArtifactCount").asInt()).isEqualTo(11);

    assertThat(dataJson).isNotNull();
    assertThat(dataJson.get("exactlyMatchedComponentCount").asInt()).isEqualTo(11);
    assertThat(dataJson.get("knownArtifactCount").asInt()).isEqualTo(11);

    for (JsonNode bom : bomInnerSourceParent) {
      assertInnerSourceParent(bom, app, componentIdentifiers);
    }

    for (JsonNode transitiveDependencies : bomInnerSourceDependencies) {
      assertThat(transitiveDependencies).isNotNull();
      assertThat(transitiveDependencies.get("ownerApplicationName").asText()).isEqualTo(app.getName());
    }
  }

  @Test
  public void testProcessInnerSourceDependencies_Without_RootArtifact() throws Exception {

    Application app = tempEntity.newApplicationWithParent();

    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.data/innersource-data", app);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson =
        new ObjectMapper()
            .readTree(
                getClass().getResource(
                    "/ReportServiceTest/report-innersource-not-root/dependencies.json"));
    JsonNode summaryJson =
        objectMapper.readTree(
            getClass().getResource(
                "/ReportServiceTest/report-innersource-not-root/summary.json"));
    JsonNode bomJson =
        objectMapper.readTree(
            getClass().getResource(
                "/ReportServiceTest/report-innersource-not-root/bom.json"));
    JsonNode dataJson =
        objectMapper.readTree(
            getClass().getResource(
                "/ReportServiceTest/report-innersource-not-root/data.json"));
    Report.processInnerSourceComponents(dependenciesJson, bomJson, dataJson, summaryJson, app);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();

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
    assertThat(bomInnerSourceParent).isEmpty();
    assertThat(bomInnerSourceDependencies).isEmpty();
  }

  @Test
  public void testProcessInnerSourceDependencies_without_children() throws Exception {

    Application app = tempEntity.newApplicationWithParent();

    ComponentIdentifier innerSourceData = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.innersource.data", "innersource-data", "1.0.0-SNAPSHOT", "", "jar");

    List<ComponentIdentifier> componentIdentifiers = Arrays.asList(innerSourceData);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.data/innersource-data", app);
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson =
        objectMapper.readTree(getClass()
            .getResource(
                "/ReportServiceTest/report-innersource-not-children/dependencies.json"));
    JsonNode bomJson =
        objectMapper.readTree(
            getClass().getResource(
                "/ReportServiceTest/report-innersource-not-children/bom.json"));
    JsonNode summaryJson =
        objectMapper.readTree(
            getClass().getResource(
                "/ReportServiceTest/report-innersource-not-children/summary.json"));
    JsonNode dataJson =
        objectMapper.readTree(
            getClass().getResource(
                "/ReportServiceTest/report-innersource-not-children/data.json"));
    Report.processInnerSourceComponents(dependenciesJson, bomJson, dataJson, summaryJson, app);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();
    List<JsonNode> bomInnerSourceDependencies = new ArrayList<>();

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
    assertThat(bomInnerSourceParent).hasSize(1);
    assertThat(bomInnerSourceDependencies).isEmpty();

    assertThat(summaryJson).isNotNull();
    assertThat(summaryJson.get("knownArtifactCount").asInt()).isEqualTo(4);

    assertThat(dataJson).isNotNull();
    assertThat(dataJson.get("exactlyMatchedComponentCount").asInt()).isEqualTo(4);
    assertThat(dataJson.get("knownArtifactCount").asInt()).isEqualTo(4);

    for (JsonNode bom : bomInnerSourceParent) {
      assertInnerSourceParent(bom, app, componentIdentifiers);
    }
  }

  @Test
  public void testProcessInnerSourceDependencies_producer_not_exists() throws Exception {

    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent("pkg:maven/com.sonatype.innersource.main/innersource-main", app);
    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode dependenciesJson =
        objectMapper.readTree(getClass()
            .getResource(
                "/ReportServiceTest/report-innersource-not-children/dependencies.json"));
    JsonNode bomJson =
        objectMapper.readTree(
            getClass().getResource(
                "/ReportServiceTest/report-innersource-not-children/bom.json"));
    JsonNode summaryJson =
        objectMapper.readTree(
            getClass().getResource(
                "/ReportServiceTest/report-innersource-not-children/summary.json"));
    JsonNode dataJson =
        objectMapper.readTree(
            getClass().getResource(
                "/ReportServiceTest/report-innersource-not-children/data.json"));
    Report.processInnerSourceComponents(dependenciesJson, bomJson, dataJson, summaryJson, app);

    List<JsonNode> bomInnerSourceParent = new ArrayList<>();

    for (JsonNode bomChild : bomJson.get("aaData")) {
      JsonNode innerSourceNode = bomChild.get("ownerApplicationName");
      if (innerSourceNode != null) {
        JsonNode innerSourceNodeParent = bomChild.get("innerSource");
        if (innerSourceNodeParent != null && innerSourceNodeParent.asBoolean()) {
          bomInnerSourceParent.add(bomChild);
        }
      }
    }
    assertThat(bomInnerSourceParent).isEmpty();
  }

  private void assertInnerSourceParent(
      JsonNode bomInnerSource,
      Application app,
      List<ComponentIdentifier> componentIdentifiers) throws Exception
  {
    assertThat(bomInnerSource).isNotNull();
    assertThat(bomInnerSource.get("innerSource").asBoolean()).isTrue();
    assertThat(bomInnerSource.get("matchState").asText()).isEqualTo(MatchState.EXACT.getId());
    assertThat(bomInnerSource.get("identificationSource").asText())
        .isEqualTo(IdentificationSource.PACKAGE_MANIFEST.getId());

    assertThat(bomInnerSource.get(ComponentIdentifier.MAVEN_GROUP_ID).asText()).isNotNull();
    assertThat(bomInnerSource.get(ComponentIdentifier.MAVEN_ARTIFACT_ID).asText()).isNotNull();
    assertThat(bomInnerSource.get(ComponentIdentifier.VERSION).asText()).isNotNull();

    assertThat(componentIdentifiers).contains(ComponentIdentifierAdapter.getComponentIdentifier(bomInnerSource));

    AnalyzerFeatures analyzerFeaturesInBom =
        JsonUtils.asPojo(bomInnerSource.get("analyzerFeatures"), AnalyzerFeatures.class);
    AnalyzerFeatures analyzerFeaturesExpected =
        new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.COORDINATE, "mvn");
    assertThat(analyzerFeaturesInBom).isEqualToComparingFieldByField(analyzerFeaturesExpected);

    assertThat(bomInnerSource.get("ownerApplicationName").asText()).isEqualTo(app.getName());
  }
}
