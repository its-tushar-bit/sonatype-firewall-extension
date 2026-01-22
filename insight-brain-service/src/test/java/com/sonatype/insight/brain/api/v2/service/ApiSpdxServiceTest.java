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
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.file.UnsupportedSbomException;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.spdx.library.model.license.ConjunctiveLicenseSet;
import org.spdx.library.model.license.DisjunctiveLicenseSet;
import org.spdx.library.model.license.ExtractedLicenseInfo;
import org.spdx.library.model.license.SimpleLicensingInfo;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
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
    scanId = TemporaryEntity.uuid();
    application = tempEntity.newApplication(tempEntity.newOrganization().getId());
    setBaseUrl("http://localhost:8070/");
    createReportAndPolicyEvaluation("report");
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
        .withMessageContaining("Application with ID fake-app does not exist.");
  }

  @Test
  public void testGetByScanId_json_cpeAndSwid() throws Exception {
    SpdxDocument document = testGetByScanId("json", false, "2.3", false);
    verifyCpeAndSwidValuesForPackage(document, "org.apache.logging.log4j:log4j-core");
  }

  @Test
  public void testGetByScanId_xml_cpeAndSwid() throws Exception {
    SpdxDocument document = testGetByScanId("xml", false, "2.3", false);
    verifyCpeAndSwidValuesForPackage(document, "org.apache.logging.log4j:log4j-core");
  }

  private static void verifyCpeAndSwidValuesForPackage(final SpdxDocument document, String packageName)
      throws InvalidSPDXAnalysisException
  {
    List<? extends ModelObject> items =
        Read.getAllItems(document.getModelStore(), document.getDocumentUri(), SpdxConstants.CLASS_SPDX_PACKAGE)
            .collect(Collectors.toList());
    int cpeSwidCount = 0;
    for (ModelObject item : items) {
      SpdxPackage spdxPackage = (SpdxPackage) item;
      if (spdxPackage.getName().orElse("notFound").equals(packageName)) {
        for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
          if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY &&
              externalRef.getReferenceType().getIndividualURI()
                  .equals(SpdxConstants.SPDX_LISTED_REFERENCE_TYPES_PREFIX + "cpe23Type")) {
            assertThat(externalRef.getReferenceLocator()).startsWith("cpe:2.3:");
            cpeSwidCount++;
          }
          if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY &&
              externalRef.getReferenceType().getIndividualURI().endsWith("swid")) {
            assertThat(externalRef.getReferenceLocator()).startsWith("swid:");
            cpeSwidCount++;
          }
        }
      }
    }
    assertThat(cpeSwidCount).isEqualTo(2);
  }

  @Test
  public void testGetByScanId_json() throws Exception {
    testGetByScanId("json", false, "2.3", false);
  }

  @Test
  public void testGetByScanId_xml() throws Exception {
    testGetByScanId("xml", false, "2.3", false);
  }

  @Test
  public void testGetByScanId_json_sageReport() throws Exception {
    createReportAndPolicyEvaluation("sageReport");
    testGetByScanId("json", false, "2.3", true);
  }

  @Test
  public void testGetByScanId_json_cycloneDx() throws Exception {
    testGetByScanId("json", true, "2.3", false);
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
    assertThatExceptionOfType(UnsupportedSbomException.class)
        .isThrownBy(() -> service.getByScanId(application.getId(), scanId, "xml", false, "2.0"))
        .withMessageContaining(
            "SPDX 2.0 version is not valid. Supported SPDX versions: " +
                ThirdPartyUtils.SPDX_ACCEPTED_VERSIONS.values());
  }

  @Test
  public void testAddDependencyRelationships_NullChildren() throws Exception {
    ApiDependencyTreeNodeDTO nodeDTO = new ApiDependencyTreeNodeDTO();
    nodeDTO.setPackageUrl("packageUrl");

    Map<String, SpdxPackage> purlElementMap = new HashMap<>();
    purlElementMap.put("packageUrl", new SpdxPackage());

    assertThatNoException().isThrownBy(() -> {
      service.addDependencyRelationships(nodeDTO, new SpdxDocument("uri"), purlElementMap, true);
    });
  }

  @Test
  public void testGetByScanId_exportWithCorrectLicenseIdCase() throws Exception {
    createReportAndPolicyEvaluation("licenseIdSimilarCaseWrongId");

    Response response = service.getByScanId(application.getId(), scanId, "json", false, "2.3");
    SpdxDocument document = deserialize(response.getEntity().toString(), "json");

    // assert top level relationship
    Collection<Relationship> relationships = document.getRelationships();
    assertThat(relationships).hasSize(1);

    SpdxPackage onlyPackage = SbomSpdxUtils.getAllPackages(document).get(0);
    // Original license id with incorrect letter case were: MiT and WXwindows. We check case is corrected to comply
    // with IDs recognized by SPDX library(MIT and wxWindows)
    assertThat(onlyPackage.getLicenseConcluded().toString()).isEqualTo("(wxWindows AND CC0-1.0 AND MIT)");
    assertThat(onlyPackage.getLicenseDeclared().toString()).isEqualTo(
        "(wxWindows AND LicenseRef-Not-Supported AND CC0-1.0 AND MIT)");
  }

  private SpdxDocument testGetByScanId(
      String format,
      boolean generateCycloneDx,
      String spdxVersion,
      boolean isSage) throws Exception
  {
    when(versionService.getFullVersion()).thenReturn("1.0");

    Response response = service.getByScanId(application.getId(), scanId, format, generateCycloneDx, spdxVersion);
    SpdxDocument document;
    if (generateCycloneDx) {
      document = deserialize(extractSpdxContentFromArchive(response), format);
    }
    else {
      document = deserialize(response.getEntity().toString(), format);
    }

    assertFilename(response, "build", format, generateCycloneDx);
    assertDocument(document, spdxVersion, isSage);
    return document;
  }

  private String extractSpdxContentFromArchive(Response response) throws IOException {
    String spdxContent = null;
    String spdxFilename = null;
    String cdxContent = null;
    String cdxFilename = null;
    File inFile = (File) response.getEntity();
    try (InputStream inputStream = Files.newInputStream(inFile.toPath());
         GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(inputStream);
         TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
      TarArchiveEntry tarEntry = tarIn.getNextEntry();
      assertThat(tarEntry).isNotNull();
      if (tarEntry.getName().contains(".spdx.")) {
        spdxContent = IOUtils.toString(tarIn, StandardCharsets.UTF_8);
        spdxFilename = tarEntry.getName();
      }
      else {
        cdxContent = IOUtils.toString(tarIn, StandardCharsets.UTF_8);
        cdxFilename = tarEntry.getName();
      }
      tarEntry = tarIn.getNextEntry();
      assertThat(tarEntry).isNotNull();
      if (tarEntry.getName().contains(".spdx.")) {
        spdxContent = IOUtils.toString(tarIn, StandardCharsets.UTF_8);
        spdxFilename = tarEntry.getName();
      }
      else {
        cdxContent = IOUtils.toString(tarIn, StandardCharsets.UTF_8);
        cdxFilename = tarEntry.getName();
      }
    }
    assertThat(spdxContent).isNotEmpty();
    assertThat(spdxContent).contains("file://" + cdxFilename);
    assertThat(cdxContent).isNotEmpty();
    assertThat(cdxContent).contains("file://" + spdxFilename);
    return spdxContent;
  }

  private void assertFilename(Response response, String stageId, String format, boolean generateCycloneDx) {
    String contentHeader = response.getHeaderString("Content-Disposition");
    String actualFilename = contentHeader.substring(contentHeader.indexOf("=") + 1).split(";")[0].replaceAll("\"", "");
    String suffix = generateCycloneDx ? "tar.gz" : format.equals("json") ? "spdx.json" : "spdx.xml";
    String expectedFilename = String.format("%s-%s-%s.%s", application.getPublicId(), stageId, scanId, suffix);
    assertThat(actualFilename).isEqualTo(expectedFilename);
  }

  private void assertDocument(final SpdxDocument document, final String spdxVersion, boolean isSage) throws Exception {
    String dataDate = isSage ? "20230716" : null;
    assertMetadata(document, spdxVersion, dataDate);
    assertPackages(document, isSage, spdxVersion);
    assertExtractedLicenseInfo(document);
    assertTopLevelRelationship(document);

    // validate generated SPDX doc; filter out license deprecation warnings
    List<String> verificationErrors = document.verify();
    List<String> filteredErrors = verificationErrors.stream()
        .filter(s -> !s.matches(".*Relationship error: [^\\s]+ is deprecated\\..*"))
        .collect(Collectors.toList());
    assertThat(filteredErrors).isEmpty();
  }

  @Test
  public void testGetLatestForStage_json_23() throws Exception {
    testGetLatest("json", false, "2.3");
  }

  @Test
  public void testGetLatestForStage_xml_23() throws Exception {
    testGetLatest("xml", false, "2.3");
  }

  @Test
  public void testGetLatestForStage_json_cycloneDx_23() throws Exception {
    testGetLatest("json", true, "2.3");
  }

  @Test
  public void testGetLatestForStage_json_22() throws Exception {
    testGetLatest("json", false, "2.2");
  }

  @Test
  public void testGetLatestForStage_xml_22() throws Exception {
    testGetLatest("xml", false, "2.2");
  }

  @Test
  public void testGetLatestForStage_json_cycloneDx_22() throws Exception {
    testGetLatest("json", true, "2.2");
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
        .isThrownBy(() -> service.getLatestForStage(application.getId(), BuildStageType.ID, "yaml", false, "2.2"))
        .withMessageContaining("Invalid format: yaml. Supported formats: [json, xml]");
  }

  @Test
  public void testGetLatestForStage_invalidSpdxVersion() {
    assertThatExceptionOfType(UnsupportedSbomException.class).isThrownBy(
            () -> service.getLatestForStage(application.getId(), BuildStageType.ID, "xml", false, "2.1"))
        .withMessageContaining("SPDX 2.1 version is not valid. Supported SPDX versions: " +
            ThirdPartyUtils.SPDX_ACCEPTED_VERSIONS.values());
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
    SpdxDocument document;
    if (generateCycloneDx) {
      document = deserialize(extractSpdxContentFromArchive(response), format);
    }
    else {
      document = deserialize(response.getEntity().toString(), format);
    }

    assertFilename(response, stageId, format, generateCycloneDx);
    assertDocument(document, spdxVersion, false);
  }

  private void assertTopLevelRelationship(SpdxDocument document) throws InvalidSPDXAnalysisException {
    Collection<Relationship> relationships = document.getRelationships();
    assertThat(relationships).hasSize(1);
    Relationship relationship = relationships.stream().findFirst().get();
    assertThat(relationship.getRelationshipType()).isEqualTo(RelationshipType.DESCRIBES);
    assertThat(relationship.getRelatedSpdxElement().get().getName().get()).isEqualTo(
        "com.sonatype.testing:pr-comment-02");
  }

  private void assertMetadata(SpdxDocument document, String spdxVersion, String dataDate) throws Exception {
    assertThat(document.getSpecVersion()).isEqualTo("SPDX-" + spdxVersion);
    assertThat(document.getName()).isPresent();
    assertThat(document.getDataLicense()).isNotNull();
    assertThat(document.getCreationInfo().getCreated()).isNotNull();
    assertThat(document.getCreationInfo().getCreators().stream().findFirst().get()).isEqualTo(
        "Tool: Sonatype IQ Server - 1.0");

    if (StringUtils.isNotBlank(dataDate)) {
      assertThat(document.getCreationInfo().getComment().get()).isEqualTo("Data Date: 20230716");
    }
    else {
      assertThat(document.getCreationInfo().getComment()).isEmpty();
    }
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

  private void assertPackages(SpdxDocument document, boolean isSage, final String spdxVersion) throws Exception {
    List<? extends ModelObject> items =
        Read.getAllItems(document.getModelStore(), document.getDocumentUri(), SpdxConstants.CLASS_SPDX_PACKAGE)
            .collect(Collectors.toList());

    assertThat(items).hasSize(9);

    for (ModelObject item : items) {
      SpdxPackage spdxPackage = (SpdxPackage) item;
      assertThat(spdxPackage.getId()).startsWith(ApiSpdxService.SPDX_REF_PREFIX);
      assertThat(spdxPackage.getVersionInfo()).isPresent().get().isIn(expectedVersions);
      assertThat(spdxPackage.getName()).isPresent().get().isIn(expectedNames);
      assertThat(spdxPackage.getDownloadLocation()).isPresent().get().isEqualTo(SpdxConstants.NOASSERTION_VALUE);

      if (org.spdx.library.Version.TWO_POINT_TWO_VERSION.endsWith(spdxVersion)) {
        assertThat(spdxPackage.getCopyrightText()).isEqualTo(SpdxConstants.NOASSERTION_VALUE);
      }
      else {
        assertThat(spdxPackage.getCopyrightText()).isEmpty();
      }

      Collection<ExternalRef> externalRefs = spdxPackage.getExternalRefs();
      assertThat(externalRefs).isNotEmpty();
      boolean purlRefFound = false;
      int securityRefCount = 0;
      for (ExternalRef externalRef : externalRefs) {
        if (externalRef.getReferenceCategory() == ReferenceCategory.PACKAGE_MANAGER) {
          purlRefFound = true;
          assertThat(externalRef.getReferenceLocator()).isIn(expectedPurls);
        }
        if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY &&
            (!externalRef.getComment().isPresent() || !"type: CycloneDX".equals(externalRef.getComment().get()))
        ) {
          securityRefCount++;
        }
      }
      assertThat(purlRefFound).isTrue();
      String secCountStr = String.format("%s -> %d", spdxPackage.getName().get(), securityRefCount);
      if (isSage) {
        assertThat(secCountStr).isIn(sageSecurityRefs);
      }
      else {
        assertThat(secCountStr).isIn(expectedSecurityRefs);
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

  private void assertExtractedLicenseInfo(SpdxDocument document) throws Exception {
    final Collection<ExtractedLicenseInfo> extractedLicenseInfos = document.getExtractedLicenseInfos();
    assertThat(extractedLicenseInfos).hasSize(5);
  }

  private static final Set<String> expectedSecurityRefs = ImmutableSet.of(
      "com.sonatype.testing:pr-comment-02 -> 0",
      "org.apache.logging.log4j:log4j-core -> 5",
      "org.apache.logging.log4j:log4j-api -> 0",
      "org.slf4j:slf4j-api -> 0",
      "com.fasterxml.jackson.core:jackson-core -> 1",
      "com.fasterxml.jackson.core:jackson-databind -> 0",
      "com.fasterxml.jackson.core:jackson-annotations -> 0",
      "net.sf.ehcache:ehcache -> 66",
      "net.sf.ehcache:sizeof-agent -> 0"
  );

  private static final Set<String> sageSecurityRefs = ImmutableSet.of(
      "com.sonatype.testing:pr-comment-02 -> 0",
      "org.apache.logging.log4j:log4j-core -> 3",
      "org.apache.logging.log4j:log4j-api -> 0",
      "org.slf4j:slf4j-api -> 0",
      "com.fasterxml.jackson.core:jackson-core -> 1",
      "com.fasterxml.jackson.core:jackson-databind -> 0",
      "com.fasterxml.jackson.core:jackson-annotations -> 0",
      "net.sf.ehcache:ehcache -> 66",
      "net.sf.ehcache:sizeof-agent -> 0"
  );

  private static final Set<String> expectedLicenses = ImmutableSet.of(
      "NOASSERTION", "Apache-2.0", "MIT", "(Apache-2.0 AND MIT)",
      "(Apache-2.0 AND LicenseRef-COMMERCIAL)",
      "(Apache-2.0 AND LicenseRef-COMMERCIAL AND LicenseRef-No-Source-License)",
      "((Apache-2.0 OR EPL-1.0) AND (Apache-2.0 OR LGPL-2.1 OR LGPL-3.0 OR MPL-1.1) AND " +
          "(GPL-2.0-with-classpath-exception OR LicenseRef-CDDL-UNSPECIFIED) AND Apache-2.0 AND CC0-1.0 AND " +
          "EPL-1.0 AND LicenseRef-PUBLIC-DOMAIN AND LicenseRef-See-License-Clause AND MIT)"
  );

  private void assertLicenses(final SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
    AnyLicenseInfo licenseDeclared = spdxPackage.getLicenseDeclared();
    assertThat(licenseDeclared).isNotNull();
    assertThat(getSortedLicenseString(licenseDeclared)).isIn(expectedLicenses);

    AnyLicenseInfo licenseConcluded = spdxPackage.getLicenseConcluded();
    assertThat(licenseConcluded).isNotNull();
    assertThat(getSortedLicenseString(licenseConcluded)).isIn(expectedLicenses);
  }

  /**
   * This is needed because the order of the license IDs in the conjunctive or disjunctive sets is not fixed or
   * predictable, and it can vary between runs, which may lead to flaky tests.
   */
  private String getSortedLicenseString(AnyLicenseInfo licenseInfo) {
    if (licenseInfo instanceof SimpleLicensingInfo) {
      return ((SimpleLicensingInfo) licenseInfo).getLicenseId();
    }
    if (licenseInfo instanceof ConjunctiveLicenseSet) {
      ConjunctiveLicenseSet licenseSet = (ConjunctiveLicenseSet) licenseInfo;
      StringBuilder sb = new StringBuilder("(");

      final Collection<AnyLicenseInfo> members;
      try {
        members = licenseSet.getMembers();
      }
      catch (InvalidSPDXAnalysisException e) {
        throw new RuntimeException(e); // should not happen
      }
      members.stream().map(this::getSortedLicenseString).sorted().forEach(l -> sb.append(l).append(" AND "));
      int cutoff = sb.length() - 5;
      return sb.substring(0, cutoff) + ")";
    }
    if (licenseInfo instanceof DisjunctiveLicenseSet) {
      DisjunctiveLicenseSet licenseSet = (DisjunctiveLicenseSet) licenseInfo;
      StringBuilder sb = new StringBuilder("(");

      final Collection<AnyLicenseInfo> members;
      try {
        members = licenseSet.getMembers();
      }
      catch (InvalidSPDXAnalysisException e) {
        throw new RuntimeException(e); // should not happen
      }
      members.stream().map(this::getSortedLicenseString).sorted().forEach(l -> sb.append(l).append(" OR "));
      int cutoff = sb.length() - 4;
      return sb.substring(0, cutoff) + ")";
    }
    return "NOASSERTION";
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
    SpdxDocument document = deserialize(response.getEntity().toString(), "json");

    // assert top level relationship
    Collection<Relationship> relationships = document.getRelationships();
    assertThat(relationships).hasSize(1);
    Relationship relationship = relationships.stream().findFirst().get();
    assertThat(relationship.getRelationshipType()).isEqualTo(RelationshipType.DESCRIBES);
    assertThat(relationship.getRelatedSpdxElement().get().getName().get()).startsWith(
        "sonatype:iq_application_Test App");
  }

  private SpdxDocument deserialize(String content, String format)
      throws Exception
  {
    String uri;
    IModelStore modelStore = new InMemSpdxStore();
    try (MultiFormatStore multiFormatStore =
             new MultiFormatStore(modelStore, "json".equals(format) ? Format.JSON : Format.XML, Verbose.COMPACT);
         InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
      uri = multiFormatStore.deSerialize(in, true);
    }
    return new SpdxDocument(modelStore, uri, DefaultModelStore.getDefaultCopyManager(), true);
  }
}
