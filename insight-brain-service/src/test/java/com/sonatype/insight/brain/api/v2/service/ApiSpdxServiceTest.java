/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.DefaultModelStore;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.Read;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.Checksum;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.ModelObject;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.enumerations.ChecksumAlgorithm;
import org.spdx.library.model.enumerations.ReferenceCategory;
import org.spdx.library.model.enumerations.RelationshipType;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

public class ApiSpdxServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSpdxService service;

  @Inject
  private InsightWork work;

  private Application application;

  private String scanId;

  @Mock
  private VersionService versionService;

  @Override
  public void configure(Binder binder) {
    binder.bind(VersionService.class).toInstance(versionService);
    super.configure(binder);
  }

  @Before
  public void setup() throws IOException {
    scanId = tempEntity.uuid();
    application = tempEntity.newApplication(tempEntity.newOrganization().getId());
    setBaseUrl("http://localhost:8070/");
    createReportAndPolicyEvaluation("report");

    SystemConfigurationPropertyFeature.SPDX_EXPORT.setEnabled(true);
  }

  @After
  public void teardown() {
    SystemConfigurationPropertyFeature.SPDX_EXPORT.setEnabled(false);
  }

  private void createReportAndPolicyEvaluation(String folder) throws IOException {
    File reportFile = work.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/" + getClass().getSimpleName() + "/" + folder, tempDir),
        reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
  }

  @Test
  public void testGetByScanId_unknownApplicationId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getByScanId("fake-app", "fake-scan-id", "json", false, "2.3"))
        .withMessageContaining("Could not find an application with ID fake-app");
  }

  @Test
  public void testGetByScanId_json() throws Exception {
    testGetByScanId("json", false, "2.3");
  }

  @Test
  public void testGetByScanId_xml() throws Exception {
    testGetByScanId("xml", false, "2.3");
  }

  @Test
  public void testGetByScanId_InvalidScanId() throws Exception {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getByScanId(application.getId(), "bogus", "xml", false, "2.3"))
        .withMessageContaining("Could not find a report with ID bogus");
  }

  @Test
  public void testGetByScanId_invalidFormat() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getByScanId(application.getId(), scanId, "yaml", false, "2.3"))
        .withMessageContaining("Invalid format: yaml. Supported formats: [json, xml]");
  }

  @Test
  public void testGetByScanId_invalidSpdxVersion() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getByScanId(application.getId(), scanId, "xml", false, "2.0"))
        .withMessageContaining("Invalid SPDX version: 2.0. Supported SPDX versions: [2.3]");
  }

  @Test
  public void testGetByScanId_spdxExportDisabled() {
    SystemConfigurationPropertyFeature.SPDX_EXPORT.setEnabled(false);

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> service.getByScanId(application.getId(), scanId, "xml", false, "2.3"))
        .withMessageContaining("This API endpoint is currently disabled.");
  }

  private void testGetByScanId(
      String format,
      boolean generateCycloneDx,
      String spdxVersion) throws Exception
  {
    when(versionService.getFullVersion()).thenReturn("1.0");

    Response response = service.getByScanId(application.getId(), scanId, format, generateCycloneDx, spdxVersion);
    SpdxDocument document = deserialize(response, format);

    assertMetadata(document, spdxVersion);
    assertPackages(document);
  }

  @Test
  public void testGetLatestForStage_json() throws Exception {
    testGetLatest("json", false, "2.3");
  }

  @Test
  public void testGetLatestForStage_xml() throws Exception {
    testGetLatest("xml", false, "2.3");
  }

  @Test
  public void testGetLatestForStage_InvalidStage() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getLatestForStage(application.getId(), "bogus", "xml", false, "2.3"))
        .withMessageContaining("Invalid stage: bogus.");
  }

  @Test
  public void testGetLatestForStage_invalidFormat() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getLatestForStage(application.getId(), BuildStageType.ID, "yaml", false, "2.0"))
        .withMessageContaining("Invalid format: yaml. Supported formats: [json, xml]");
  }

  @Test
  public void testGetLatestForStage_invalidSpdxVersion() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getLatestForStage(application.getId(), BuildStageType.ID, "xml", false, "2.1"))
        .withMessageContaining("Invalid SPDX version: 2.1. Supported SPDX versions: [2.3]");
  }

  @Test
  public void testGetLatestForStage_spdxExportDisabled() {
    SystemConfigurationPropertyFeature.SPDX_EXPORT.setEnabled(false);

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> service.getLatestForStage(application.getId(), BuildStageType.ID, "xml", false, "2.3"))
        .withMessageContaining("This API endpoint is currently disabled.");
  }

  private void testGetLatest(
      String format,
      boolean generateCycloneDx,
      String spdxVersion) throws Exception
  {
    when(versionService.getFullVersion()).thenReturn("1.0");

    String stageId = BuildStageType.ID;
    Response response =
        service.getLatestForStage(application.getId(), stageId, format, generateCycloneDx, spdxVersion);
    SpdxDocument document = deserialize(response, format);

    String contentHeader = response.getHeaderString("Content-Disposition");
    String actualFilename = contentHeader.substring(contentHeader.indexOf("=") + 1).split(";")[0].replaceAll("\"", "");
    String expectedFilename = String.format("%s-%s-%s.spdx.%s", application.getPublicId(), stageId, scanId, format);

    assertThat(actualFilename).isEqualTo(expectedFilename);
    assertMetadata(document, spdxVersion);
    assertPackages(document);
    assertTopLevelRelationship(document);
  }

  private void assertTopLevelRelationship(SpdxDocument document) throws InvalidSPDXAnalysisException {
    Collection<Relationship> relationships = document.getRelationships();
    assertThat(relationships).hasSize(1);
    Relationship relationship = relationships.stream().findFirst().get();
    assertThat(relationship.getRelationshipType()).isEqualTo(RelationshipType.DESCRIBES);
    assertThat(relationship.getRelatedSpdxElement().get().getName().get()).isEqualTo(
        "com.sonatype.testing:pr-comment-02");
  }

  private void assertMetadata(SpdxDocument document, String spdxVersion) throws Exception {
    assertThat(document.getSpecVersion()).isEqualTo("SPDX-" + spdxVersion);
    assertThat(document.getCreationInfo().getCreated()).isNotNull();
    assertThat(document.getCreationInfo().getCreators().stream().findFirst().get()).isEqualTo(
        "Tool: Sonatype IQ Server - 1.0");
  }

  private static final Set<String> expectedNames = ImmutableSet.of(
      "org.apache.logging.log4j:log4j-core", "org.apache.logging.log4j:log4j-api",
      "com.fasterxml.jackson.core:jackson-databind", "com.fasterxml.jackson.core:jackson-annotations",
      "com.fasterxml.jackson.core:jackson-core", "com.sonatype.testing:pr-comment-02",
      "net.sf.ehcache:ehcache", "org.slf4j:slf4j-api", "net.sf.ehcache:sizeof-agent"
  );

  private static final Set<String> expectedVersions = ImmutableSet.of(
      "2.14.0", "2.16.0", "1.7.25", "1.0.1", "2.10.7", "1.0-SNAPSHOT");

  private static final Set<String> expectedPurls = ImmutableSet.of(
      "pkg:maven/org.apache.logging.log4j/log4j-core@2.16.0?type=jar",
      "pkg:maven/org.apache.logging.log4j/log4j-api@2.16.0?type=jar",
      "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.14.0?type=jar",
      "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.14.0?type=jar",
      "pkg:maven/com.fasterxml.jackson.core/jackson-core@2.14.0?type=jar",
      "pkg:maven/com.sonatype.testing/pr-comment-02@1.0-SNAPSHOT?type=jar",
      "pkg:maven/net.sf.ehcache/ehcache@2.10.7?type=jar",
      "pkg:maven/org.slf4j/slf4j-api@1.7.25?type=jar",
      "pkg:maven/net.sf.ehcache/sizeof-agent@1.0.1?type=jar"
  );

  private void assertPackages(SpdxDocument document) throws Exception {
    List<? extends ModelObject> items =
        Read.getAllItems(document.getModelStore(), document.getDocumentUri(), SpdxConstants.CLASS_SPDX_PACKAGE)
            .collect(Collectors.toList());

    assertThat(items).hasSize(9);

    for (ModelObject item : items) {
      SpdxPackage spdxPackage = (SpdxPackage) item;
      assertThat(spdxPackage.getId()).startsWith(ApiSpdxService.SPDX_REF_PREFIX);
      assertThat(spdxPackage.getVersionInfo()).isPresent().get().isIn(expectedVersions);
      assertThat(spdxPackage.getName()).isPresent().get().isIn(expectedNames);

      Collection<ExternalRef> externalRefs = spdxPackage.getExternalRefs();
      assertThat(externalRefs).hasSize(1);
      for (ExternalRef externalRef : externalRefs) {
        assertThat(externalRef.getReferenceCategory()).isEqualTo(ReferenceCategory.PACKAGE_MANAGER);
        assertThat(externalRef.getReferenceLocator()).isIn(expectedPurls);
      }

      Collection<Checksum> checksums = spdxPackage.getChecksums();
      for (Checksum checksum : checksums) {
        assertThat(checksum.getAlgorithm()).isEqualTo(ChecksumAlgorithm.SHA256);
        assertThat(checksum.getValue()).isEqualTo("2fa0ab71b154da29ac134097bc6bbacd90987dd4c4005516159e6494d1d52ea2");
      }

      assertLicenses(spdxPackage);
      assertRelationships(spdxPackage);
    }
  }

  private static final Set<String> expectedLicenses = ImmutableSet.of(
      "NOASSERTION", "Apache-2.0", "MIT", "(Apache-2.0 AND MIT)",
      "(Apache-2.0 AND COMMERCIAL)", "(Apache-2.0 AND COMMERCIAL AND No-Source-License)",
      "(EPL-1.0 AND (CDDL-UNSPECIFIED OR GPL-2.0-with-classpath-exception) AND (EPL-1.0 OR Apache-2.0) AND " +
          "See-License-Clause AND Apache-2.0 AND CC0-1.0 AND MIT AND " +
          "(LGPL-2.1 OR LGPL-3.0 OR MPL-1.1 OR Apache-2.0) AND PUBLIC-DOMAIN)",
      "((LGPL-2.1 OR LGPL-3.0 OR MPL-1.1 OR Apache-2.0) AND EPL-1.0 AND (EPL-1.0 OR Apache-2.0) AND " +
          "See-License-Clause AND Apache-2.0 AND CC0-1.0 AND MIT AND " +
          "(CDDL-UNSPECIFIED OR GPL-2.0-with-classpath-exception) AND PUBLIC-DOMAIN)"
  );

  private void assertLicenses(final SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
    AnyLicenseInfo licenseDeclared = spdxPackage.getLicenseDeclared();
    assertThat(licenseDeclared).isNotNull();
    assertThat(licenseDeclared.toString()).isIn(expectedLicenses);

    AnyLicenseInfo licenseConcluded = spdxPackage.getLicenseConcluded();
    assertThat(licenseConcluded).isNotNull();
    assertThat(licenseConcluded.toString()).isIn(expectedLicenses);
  }

  private static final Set<String> expectedRelationships = ImmutableSet.of(
      "com.sonatype.testing:pr-comment-02 -> com.fasterxml.jackson.core:jackson-databind",
      "com.sonatype.testing:pr-comment-02 -> org.apache.logging.log4j:log4j-core",
      "org.apache.logging.log4j:log4j-core -> org.apache.logging.log4j:log4j-api",
      "com.fasterxml.jackson.core:jackson-databind -> com.fasterxml.jackson.core:jackson-annotations",
      "com.fasterxml.jackson.core:jackson-databind -> com.fasterxml.jackson.core:jackson-core",
      "com.sonatype.testing:pr-comment-02 -> net.sf.ehcache:ehcache",
      "com.sonatype.testing:pr-comment-02 -> net.sf.ehcache:sizeof-agent",
      "net.sf.ehcache:ehcache -> org.slf4j:slf4j-api"
  );

  private void assertRelationships(SpdxPackage spdxPackage) throws Exception {
    final Collection<Relationship> relationships = spdxPackage.getRelationships();

    for (Relationship relationship : relationships) {
      assertThat(relationship.getRelationshipType()).isEqualTo(RelationshipType.DEPENDS_ON);
      String relStr = String.format("%s -> %s",
          spdxPackage.getName().get(), relationship.getRelatedSpdxElement().get().getName().get());
      assertThat(relStr).isIn(expectedRelationships);
    }
  }

  @Test
  public void testGetByScanId_AddMissingParent_ForDependencyTree() throws Exception {
    createReportAndPolicyEvaluation("missingParentTree");

    Response response = service.getByScanId(application.getId(), scanId, "json", false, "2.3");
    SpdxDocument document = deserialize(response, "json");

    // assert top level relationship
    Collection<Relationship> relationships = document.getRelationships();
    assertThat(relationships).hasSize(1);
    Relationship relationship = relationships.stream().findFirst().get();
    assertThat(relationship.getRelationshipType()).isEqualTo(RelationshipType.DESCRIBES);
    assertThat(relationship.getRelatedSpdxElement().get().getName().get()).startsWith(
        "sonatype:iq_application_Test App");
  }

  private SpdxDocument deserialize(Response response, String format)
      throws IOException, InvalidSPDXAnalysisException
  {
    String uri;
    IModelStore modelStore = new InMemSpdxStore();
    MultiFormatStore multiFormatStore =
        new MultiFormatStore(modelStore, "json".equals(format) ? Format.JSON : Format.XML, Verbose.COMPACT);
    try (InputStream in = new ByteArrayInputStream(response.getEntity().toString().getBytes(StandardCharsets.UTF_8))) {
      uri = multiFormatStore.deSerialize(in, true);
    }
    return new SpdxDocument(modelStore, uri, DefaultModelStore.getDefaultCopyManager(), true);
  }
}
