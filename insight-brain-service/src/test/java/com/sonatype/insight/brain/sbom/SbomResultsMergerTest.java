/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils.PROPERTY_COMPONENT_REFS;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.CPE_MATCH;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.PRIMARY;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.SECONDARY;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.DEEP_DIVE;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.FAST_TRACK;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.PUBLIC_RESEARCH;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.VENDOR_RESEARCH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.cyclonedx.model.vulnerability.Vulnerability.Analysis.State.RESOLVED;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.SbomTaxonomy;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.FileLifecycleReportPersistenceService;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.brain.telemetry.CpeResultsTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.thirdparty.ThirdPartyDataServiceTest;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.io.File;
import java.lang.reflect.Field;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.Justification;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SbomResultsMergerTest
    extends AbstractComponentTest
{
  public static final String SCAN_REQUEST_ID = "scan-request-id";

  private static final String SCAN_ID = "scanId";

  @Inject
  private Provider<SbomResultsMerger> mergerProvider;

  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Inject
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Inject
  private ThirdPartyScanDAO thirdPartyScanDAO;

  @Inject
  private InsightWork insightWork;

  @Inject
  private ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  @Inject
  private TestProductLicense productLicense;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private FileLifecycleReportPersistenceService lifecycleReportPersistenceService;

  private TelemetrySender mockTelemetrySender;

  private SbomResultsMerger merger;

  private Application application;

  private CpeResultsTelemetry cpeResultsTelemetry;

  @Before
  public void before() throws Exception {
    mockTelemetrySender = mock(TelemetrySender.class);
    merger = mergerProvider.get();
    Field telemetryField = SbomResultsMerger.class.getDeclaredField("telemetrySender");
    telemetryField.setAccessible(true);
    telemetryField.set(merger, mockTelemetrySender);
    application = tempEntity.newApplicationWithParent();
    cpeResultsTelemetry = new CpeResultsTelemetry();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMergeSonatypeDataWithSbomData_BinaryScan_WithThirdPartyContent() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    PackageUrlIdentifier purl1 =
        new PackageUrlIdentifier("pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl");
    PackageUrlIdentifier purl2 = new PackageUrlIdentifier("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0");
    PackageUrlIdentifier purl3 = new PackageUrlIdentifier("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1");
    PackageUrlIdentifier purl4 =
        new PackageUrlIdentifier("pkg:maven/com.sun.istack/istack-commons-runtime@4.1.2?type=jar");

    final ThirdPartyFile file = tempEntity.newThirdPartyFile("binary.temp");
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadataForBinaryScan(app.getId(), "1", file, PENDING);
    String originalComponentRef = "1f5981c18853e7550d6ca7c3bed273c1c1b6d184";
    ThirdPartyFileCoordinate tpComponent =
        tempEntity.newThirdPartyFileCoordinate(file, "Sonatype", "pypi", "orange", "1.0.1", "093080a1a4bbd2750541",
            purl1.getPackageUrl(), originalComponentRef);
    ThirdPartyCoordinateSecurity tpVuln =
        tempEntity.newThirdPartyCoordinateSecurity(tpComponent, "FG-R00229", sbomMetadata.getId(), "desc", "1.url", 1d,
            "FG-R00229 test", "2.0");
    tempEntity.newThirdPartyCoordinateLicense(tpComponent, "MIT", "MIT", "https://opensource.org/licenses/MIT");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(tpVuln, "FG-R00229", "resolved", "code_not_reachable",
        "will_not_fix,update", null);

    File updatedReport = mockReportZipWithUpdatedThirdPartyData(
        "/SbomResultsMergerTest/report-for-binary-scan-with-thirdparty",
        tpComponent);
    ReportHelper.saveMockReport(insightWork, tempDir, updatedReport.toPath(), application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    ThirdPartyScan tpScan = thirdPartyScanDAO.getByThirdPartyFileId(updatedMetadata.getThirdPartyFileId());
    assertThat(tpScan.getFilteredScanFile()).isEqualTo("scan-" + tpScan.getScanId() + "-filtered.xml.gz");
    File filteredScanFile =
        new File(insightWork.getScanDir(updatedMetadata.getApplicationId()), tpScan.getFilteredScanFile());
    assertThat(filteredScanFile).exists();

    // verify all components
    List<ThirdPartyFileCoordinate> fileCoordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(updatedMetadata.getThirdPartyFileId());
    Map<PackageUrlIdentifier, ThirdPartyFileCoordinate> coords = fileCoordinates.stream()
        .collect(Collectors.toMap(tpCoord -> new PackageUrlIdentifier(tpCoord.getPackageUrl()), tpCoord -> tpCoord));
    assertThat(fileCoordinates).hasSize(4)
        .allSatisfy(tpc -> assertThat(tpc.getComponentRef()).isNotBlank().hasSize(40));
    List<PackageUrlIdentifier> expectedPurls = List.of(purl1, purl2, purl3, purl4);
    assertThat(coords.keySet()).containsExactlyInAnyOrderElementsOf(expectedPurls);

    // verify component is merged
    ThirdPartyFileCoordinate tpfc1 = coords.get(purl1);
    assertThat(tpfc1.getId()).isNotEmpty();
    assertThat(tpfc1.getThirdPartyFileId()).isEqualTo(updatedMetadata.getThirdPartyFileId());
    assertThat(tpfc1.getName()).isEqualTo("orange");
    assertThat(tpfc1.getVersion()).isEqualTo("1.0.1");
    assertThat(tpfc1.getHash()).isEqualTo("093080a1a4bbd2750541");
    assertThat(tpfc1.getIdentificationSources()).isEqualTo("SBOM");
    assertThat(tpfc1.getMatchStateId()).isEqualTo("exact");
    assertThat(tpfc1.getOccurrencesList()).isNotEmpty().hasSize(1);
    assertThat(tpfc1.getOccurrencesList().get(0)).isEqualTo("dependency:/SBOM-bom.json/pkg:pypi\\orange@1.0.1");
    assertThat(tpfc1.getFilenamesList()).isEqualTo(List.of("pkg:pypi/orange@1.0.1"));
    assertThat(tpfc1.getComponentRef()).isNotEqualTo(originalComponentRef);

    // verify dependency types correctly set
    assertThat(coords.get(purl1).getDependencyType()).isEqualTo("D");
    assertThat(coords.get(purl2).getDependencyType()).isEqualTo("T");
    assertThat(coords.get(purl3).getDependencyType()).isEqualTo("D");
    assertThat(coords.get(purl4).getDependencyType()).isEqualTo("T");

    // verify security vulnerabilities are merged
    List<ThirdPartyCoordinateSecurity> tpvListC1 = thirdPartyCoordinateSecurityDAO
        .getByFileCoordinateId(tpfc1.getId());

    assertThat(tpvListC1.size()).isEqualTo(2);
    ThirdPartyCoordinateSecurity fgR00229 = tpvListC1.get(0);
    assertThat(fgR00229.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    assertThat(fgR00229.getRefId()).isEqualTo("FG-R00229");

    ThirdPartyCoordinateSecurity fgr00274 = tpvListC1.get(1);
    assertThat(fgr00274.getRefId()).isEqualTo("FG-R00274");
    assertThat(fgr00274.getIdentificationSources()).isEqualTo("Sonatype");

    // verify licenses are merged
    List<ThirdPartyCoordinateLicense> tclListC1 = thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tpfc1.getId());
    assertThat(tclListC1.size()).isEqualTo(2);
    ThirdPartyCoordinateLicense component1License1 = tclListC1.get(0);
    assertThat(component1License1.getLicenseId()).isEqualTo("Apache-2.0");
    assertThat(component1License1.getIdentificationSources()).isEqualTo("Sonatype");
    ThirdPartyCoordinateLicense component1License2 = tclListC1.get(1);
    assertThat(component1License2.getLicenseId()).isEqualTo("MIT");
    assertThat(component1License2.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    // verify original SBOM
    Bom originalBom = merger.getOriginalBom();
    assertThat(originalBom.getMetadata().getProperties()).hasSize(1);
    assertThat(originalBom.getMetadata().getProperties().get(0).getName())
        .isEqualTo(SbomTaxonomy.CDX_ORIGINAL_FILE_PROPERTY_NAME);
    assertThat(originalBom.getMetadata().getProperties().get(0).getValue()).isEqualTo("binary.temp");
    assertThat(originalBom.getComponents()).hasSize(4);
    originalBom.getComponents().forEach(component -> {
      assertThat(component.getProperties()).hasSize(1);
      assertThat(component.getProperties().get(0).getName()).isEqualTo(SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME);
      assertThat(component.getProperties().get(0).getValue()).isNotEmpty();
    });
    Component bomComponent =
        originalBom.getComponents()
            .stream()
            .filter(c -> new PackageUrlIdentifier(c.getPurl()).equals(purl1))
            .findFirst()
            .get();
    assertThat(originalBom.getVulnerabilities()).hasSize(1);
    Vulnerability vuln = originalBom.getVulnerabilities().get(0);
    assertThat(vuln.getAffects()).hasSize(1);
    assertThat(vuln.getAffects().get(0).getRef()).isEqualTo(bomComponent.getBomRef());

    // disclosed vulnerabilities
    Analysis disclosedAnalysis = vuln.getAnalysis();
    assertThat(disclosedAnalysis).isNotNull();
    assertThat(disclosedAnalysis.getState()).isEqualTo(RESOLVED);
    assertThat(disclosedAnalysis.getJustification()).isEqualTo(Justification.CODE_NOT_REACHABLE);
    assertThat(disclosedAnalysis.getResponses().get(0)).isEqualTo(Response.WILL_NOT_FIX);

    // disclosed licenses
    assertThat(bomComponent.getLicenses()).isNotNull();
    assertThat(bomComponent.getLicenses().getLicenses()).hasSize(1);
    assertThat(bomComponent.getLicenses().getLicenses().get(0).getId()).isEqualTo("MIT");

    // verify filtered SBOM
    Bom filteredBom = merger.getFilteredBom();
    assertThat(filteredBom.getComponents()).hasSize(4)
        .allSatisfy(component -> {
          assertThat(component.getProperties()).hasSize(3);
          assertComponentRefProperty(component);
        });

    Optional<Component> tpBomComponentOptional = filteredBom.getComponents()
        .stream()
        .filter(component -> new PackageUrlIdentifier(component.getPurl()).equals(purl1))
        .findFirst();
    assertThat(tpBomComponentOptional).isPresent();
    List<Property> properties = tpBomComponentOptional.get().getProperties();
    assertThat(properties).hasSize(3);
    assertComponentRefProperty(tpBomComponentOptional.get());

    // verify telemetry data
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(mockTelemetrySender, times(1)).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryDataList).hasSize(1);
    TelemetryData telemetryData = telemetryDataList.get(0);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_POST_IMPORT_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    SbomPostImportMetricsTelemetry telemetry = (SbomPostImportMetricsTelemetry) telemetryData.getAttributes()
        .get("sbom_post_import_metrics");
    assertThat(telemetry.getVerifiedVulnerabilityCount()).isEqualTo(1);
    assertThat(telemetry.getUnverifiedVulnerabilityCount()).isEqualTo(0);
    assertThat(telemetry.getAdditionalVulnerabilitiesCount()).isEqualTo(1);
    assertThat(telemetry.getTotalVulnerabilitiesCount()).isEqualTo(1);

    // verify bom.json update
    ContainerNode<ObjectNode> bomJsonData =
        JsonUtils.parse(Objects.requireNonNull(lifecycleReport.getEntry(BOM_JSON.getName())).buf);
    ArrayNode bomArray = (ArrayNode) bomJsonData.get("aaData");
    for (JsonNode jsonNode : bomArray) {
      List<String> bomNodeComponentRefs = JsonUtils.getStringListFromArray(jsonNode.get(PROPERTY_COMPONENT_REFS));
      assertThat(bomNodeComponentRefs).hasSize(1).allMatch(ref -> ref.length() == 40);
    }
  }

  @Test
  public void testMergeSonatypeDataWithSbomDataWithIndexing_BinaryScan_NoThirdPartyContent() throws IOException {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile("binary.temp");
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadataForBinaryScan(null, "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-for-binary-scan",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    // original sbom is generated and saved as expected
    File actualSbomFile =
        new File(insightWork.getSbomDir(updatedMetadata.getApplicationId()), updatedMetadata.getFilename());
    try (InputStream actualInputStream = new GZIPInputStream(new FileInputStream(actualSbomFile));
        InputStream expectedInputStream =
            ThirdPartyDataServiceTest.class
                .getResourceAsStream("/SbomResultsMergerTest/binaryScanOriginalSboms/original-bom.json"))
    {
      String actualSbomAsString = IOUtils.toString(actualInputStream, Charset.defaultCharset());
      String expectedSbomAsString = IOUtils.toString(expectedInputStream, Charset.defaultCharset());
      assertThatJson(actualSbomAsString)
          .whenIgnoringPaths("metadata.timestamp", "components[*].bom-ref",
              "components[*].properties[0].value", "dependencies")
          .isEqualTo(expectedSbomAsString);
      Bom actualBom = SbomCycloneDxUtils.parseContentNoValidation(actualSbomAsString);
      List<Dependency> actualDependencies = actualBom.getDependencies();
      assertThat(actualDependencies).hasSize(1);
      Dependency actualDependency = actualDependencies.get(0);
      assertThat(actualDependency.getDependencies()).hasSize(1);
      String actualParentComponentBomRef = actualDependency.getRef();
      String actualChildComponentBomRef = actualDependency.getDependencies().get(0).getRef();
      Component actualParentComponent = actualBom.getComponents()
          .stream()
          .filter(it -> it.getBomRef().equals(actualParentComponentBomRef))
          .findFirst()
          .get();
      Component actualChildComponent = actualBom.getComponents()
          .stream()
          .filter(it -> it.getBomRef().equals(actualChildComponentBomRef))
          .findFirst()
          .get();
      assertThat(actualParentComponent.getPurl())
          .isEqualTo("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0");
      assertThat(actualChildComponent.getPurl()).isEqualTo("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1");
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }

    // filtered scan file is generated and exists
    ThirdPartyScan tpScan = thirdPartyScanDAO.getByThirdPartyFileId(updatedMetadata.getThirdPartyFileId());
    assertThat(tpScan.getFilteredScanFile()).isEqualTo("scan-" + tpScan.getScanId() + "-filtered.xml.gz");
    File filteredScanFile =
        new File(insightWork.getScanDir(updatedMetadata.getApplicationId()), tpScan.getFilteredScanFile());
    assertThat(filteredScanFile).exists();

    // components are saved as expected
    List<ThirdPartyFileCoordinate> fileCoordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(updatedMetadata.getThirdPartyFileId());
    Map<String, ThirdPartyFileCoordinate> coords = fileCoordinates.stream()
        .collect(Collectors.toMap(ThirdPartyFileCoordinate::getPackageUrl, Function.identity()));
    assertThat(fileCoordinates).hasSize(4);
    List<PackageUrlIdentifier> expectedUrls = Stream.of(
        "pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl",
        "pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0",
        "pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1",
        "pkg:maven/com.sun.istack/istack-commons-runtime@4.1.2?type=jar").map(PackageUrlIdentifier::new).toList();
    assertThat(coords.keySet().stream().map(PackageUrlIdentifier::new)).containsExactlyInAnyOrderElementsOf(
        expectedUrls);

    ThirdPartyFileCoordinate tpfc1 = coords.get(new PackageUrlIdentifier(
        "pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl").getPackageUrl());
    assertThat(tpfc1.getId()).isNotEmpty();
    assertThat(tpfc1.getThirdPartyFileId()).isEqualTo(updatedMetadata.getThirdPartyFileId());
    assertThat(tpfc1.getPackageUrl()).isEqualTo(new PackageUrlIdentifier(
        "pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl").getPackageUrl());
    assertThat(tpfc1.getName()).isEqualTo("orange");
    assertThat(tpfc1.getVersion()).isEqualTo("1.0.1");
    assertThat(tpfc1.getHash()).isEqualTo("093080a1a4bbd2750541");
    assertThat(tpfc1.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(tpfc1.getFormat()).isEqualTo("pypi");
    assertThat(tpfc1.getSource()).isEqualTo("Sonatype");
    assertThat(tpfc1.getDependencyType()).isNull();
    assertThat(tpfc1.getCpe()).isNull();
    assertThat(tpfc1.getSwid()).isNull();
    assertThat(tpfc1.getMatchStateId()).isEqualTo("exact");
    assertThat(tpfc1.getOccurrencesList()).isNotEmpty().hasSize(1);
    assertThat(tpfc1.getOccurrencesList().get(0)).isEqualTo("dependency:/SBOM-bom.json/pkg:pypi\\orange@1.0.1");
    assertThat(tpfc1.getFilenamesList()).isEqualTo(List.of("pkg:pypi/orange@1.0.1"));

    List<ThirdPartyCoordinateSecurity> tpvListC1 = thirdPartyCoordinateSecurityDAO
        .getByFileCoordinateId(tpfc1.getId());

    assertThat(tpvListC1.size()).isEqualTo(2);
    ThirdPartyCoordinateSecurity fgR00229 = tpvListC1.get(0);
    assertThat(fgR00229.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(fgR00229.getRefId()).isEqualTo("FG-R00229");
    assertThat(fgR00229.getAdvisories()).isNull();
    assertThat(fgR00229.getAttackVector()).isEqualTo("1.vectorString");
    assertThat(fgR00229.getCwes()).isEqualTo("cwe-1,2.cwe");
    assertThat(fgR00229.getDescription()).isBlank();
    assertThat(fgR00229.getLink()).isEqualTo("1.url");
    assertThat(fgR00229.getRecommendations()).isNull();
    assertThat(fgR00229.getSeverity()).isEqualTo(9.0d);
    assertThat(fgR00229.getSeverityDescription()).isEqualTo("CRITICAL");
    assertThat(fgR00229.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(fgR00229.getRatingMethod()).isNull();

    ThirdPartyCoordinateSecurity fgr00274 = tpvListC1.get(1);
    assertThat(fgr00274.getRefId()).isEqualTo("FG-R00274");
    assertThat(fgr00274.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(fgr00274.getAdvisories()).isNull();
    assertThat(fgr00274.getAttackVector()).isNull();
    assertThat(fgr00274.getCwes()).isNull();
    assertThat(fgr00274.getDescription()).isBlank();
    assertThat(fgr00274.getLink()).isNull();
    assertThat(fgr00274.getRecommendations()).isNull();
    assertThat(fgr00274.getSeverity()).isEqualTo(7.0d);
    assertThat(fgr00274.getSeverityDescription()).isEqualTo("HIGH");
    assertThat(fgr00274.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(fgr00274.getRatingMethod()).isNull();

    List<ThirdPartyCoordinateLicense> tclListC1 = thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tpfc1.getId());
    assertThat(tclListC1.size()).isEqualTo(2);
    ThirdPartyCoordinateLicense component1License1 = tclListC1.get(0);
    assertThat(component1License1.getLicenseId()).isEqualTo("Apache-2.0");
    ThirdPartyCoordinateLicense component1License2 = tclListC1.get(1);
    assertThat(component1License2.getLicenseId()).isEqualTo("MIT");

    ThirdPartyFileCoordinate tpfc2 = coords.get("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0");
    assertThat(tpfc2.getId()).isNotEmpty();
    assertThat(tpfc2.getThirdPartyFileId()).isEqualTo(updatedMetadata.getThirdPartyFileId());
    assertThat(tpfc2.getPackageUrl()).isEqualTo("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0");
    assertThat(tpfc2.getName()).isEqualTo("Microsoft.Identity.Client.Extensions.Msal");
    assertThat(tpfc2.getVersion()).isEqualTo("2.23.0");
    assertThat(tpfc2.getHash()).isEqualTo("00603c85922bf35d8edd");
    assertThat(tpfc2.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(tpfc2.getFormat()).isEqualTo("nuget");
    assertThat(tpfc2.getSource()).isEqualTo("Sonatype");
    assertThat(tpfc2.getDependencyType()).isEqualTo("T");
    assertThat(tpfc2.getCpe()).isNull();
    assertThat(tpfc2.getSwid()).isNull();
    assertThat(tpfc2.getMatchStateId()).isEqualTo("exact");
    assertThat(tpfc1.getOccurrencesList()).isNotEmpty().hasSize(1);
    assertThat(tpfc2.getOccurrencesList().get(0)).isEqualTo(
        "dependency:/app6-bom.xml/pkg:nuget\\Microsoft.Identity.Client.Extensions.Msal@2.23.0:00603c85922bf35d8edd");
    assertThat(tpfc2.getFilenamesList()).isEqualTo(
        List.of("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0:00603c85922bf35d8edd",
            "pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.24.0:00603c85922bf35d8edd"));

    ThirdPartyFileCoordinate tpfc3 = coords.get("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1");
    assertThat(tpfc3.getId()).isNotEmpty();
    assertThat(tpfc3.getThirdPartyFileId()).isEqualTo(updatedMetadata.getThirdPartyFileId());
    assertThat(tpfc3.getPackageUrl()).isEqualTo("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1");
    assertThat(tpfc3.getName()).isEqualTo("Microsoft.IdentityModel.Protocols");
    assertThat(tpfc3.getVersion()).isEqualTo("6.25.1");
    assertThat(tpfc3.getHash()).isEqualTo("c795e78734c2860bb627");
    assertThat(tpfc3.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(tpfc3.getFormat()).isEqualTo("nuget");
    assertThat(tpfc3.getSource()).isEqualTo("Sonatype");
    assertThat(tpfc3.getDependencyType()).isEqualTo("D");
    assertThat(tpfc3.getCpe()).isNull();
    assertThat(tpfc3.getSwid()).isNull();
    assertThat(tpfc3.getMatchStateId()).isEqualTo("exact");
    assertThat(tpfc1.getOccurrencesList()).isNotEmpty().hasSize(1);
    assertThat(tpfc3.getOccurrencesList().get(0)).isEqualTo(
        "dependency:/app6-bom.xml/pkg:nuget\\Microsoft.IdentityModel.Protocols@6.25.1:c795e78734c2860bb627");
    assertThat(tpfc3.getFilenamesList()).isEqualTo(
        List.of("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1:c795e78734c2860bb627"));

    ThirdPartyFileCoordinate tpfc4 = coords.get("pkg:maven/com.sun.istack/istack-commons-runtime@4.1.2?type=jar");
    assertThat(tpfc4.getId()).isNotEmpty();
    assertThat(tpfc4.getThirdPartyFileId()).isEqualTo(updatedMetadata.getThirdPartyFileId());
    assertThat(tpfc4.getPackageUrl()).isEqualTo("pkg:maven/com.sun.istack/istack-commons-runtime@4.1.2?type=jar");
    assertThat(tpfc4.getName()).isEqualTo("com.sun.istack:istack-commons-runtime");
    assertThat(tpfc4.getVersion()).isEqualTo("4.1.2");
    assertThat(tpfc4.getHash()).isEqualTo("18ec117c85f3ba0ac654");
    assertThat(tpfc4.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(tpfc4.getFormat()).isEqualTo("maven");
    assertThat(tpfc4.getSource()).isEqualTo("Sonatype");
    assertThat(tpfc4.getDependencyType()).isEqualTo("T");
    assertThat(tpfc4.getCpe()).isNull();
    assertThat(tpfc4.getSwid()).isNull();
    assertThat(tpfc4.getMatchStateId()).isEqualTo("exact");
    assertThat(tpfc4.getOccurrencesList()).isEmpty();
    assertThat(tpfc4.getFilenamesList()).isEqualTo(List.of("istack-commons-runtime-4.1.2.jar"));
  }

  @Test
  public void testMergeSonatypeDataWithSbomDataWithIndexing_BinaryScan_duplicatedVulnerabilities() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile("binary.temp");
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadataForBinaryScan(null, "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir,
        "/SbomResultsMergerTest/report-for-binary-scan-duplicated-vulnerabilities", application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    List<ThirdPartyFileCoordinate> fileCoordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(updatedMetadata.getThirdPartyFileId());
    Map<String, ThirdPartyFileCoordinate> coords = fileCoordinates.stream()
        .collect(Collectors.toMap(ThirdPartyFileCoordinate::getPackageUrl, Function.identity()));

    assertThat(updatedMetadata).isNotNull();
    assertThat(fileCoordinates).hasSize(4);

    ThirdPartyFileCoordinate tpfc1 = coords.get(new PackageUrlIdentifier(
        "pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl").getPackageUrl());
    assertThat(tpfc1.getId()).isNotEmpty();
    assertThat(tpfc1.getThirdPartyFileId()).isEqualTo(updatedMetadata.getThirdPartyFileId());
    assertThat(tpfc1.getPackageUrl()).isEqualTo(new PackageUrlIdentifier(
        "pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl").getPackageUrl());
    assertThat(tpfc1.getName()).isEqualTo("orange");
    assertThat(tpfc1.getVersion()).isEqualTo("1.0.1");
    assertThat(tpfc1.getHash()).isEqualTo("093080a1a4bbd2750541");
    assertThat(tpfc1.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(tpfc1.getFormat()).isEqualTo("pypi");
    assertThat(tpfc1.getSource()).isEqualTo("Sonatype");
    assertThat(tpfc1.getDependencyType()).isNull();
    assertThat(tpfc1.getCpe()).isNull();
    assertThat(tpfc1.getSwid()).isNull();

    List<ThirdPartyCoordinateSecurity> tpvListC1 = thirdPartyCoordinateSecurityDAO
        .getByFileCoordinateId(tpfc1.getId());

    // Security.json file has 5 vulnerabilities. 2 vulnerabilities are repeated once and 1 unique
    assertThat(tpvListC1.size()).isEqualTo(3);
    ThirdPartyCoordinateSecurity fgR00229 = tpvListC1.get(0);
    assertThat(fgR00229.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(fgR00229.getRefId()).isEqualTo("FG-R00229");
    assertThat(fgR00229.getAdvisories()).isNull();
    assertThat(fgR00229.getAttackVector()).isEqualTo("1.vectorString");
    assertThat(fgR00229.getCwes()).isEqualTo("cwe-1,2.cwe");
    assertThat(fgR00229.getDescription()).isBlank();
    assertThat(fgR00229.getLink()).isEqualTo("1.url");
    assertThat(fgR00229.getRecommendations()).isNull();
    assertThat(fgR00229.getSeverity()).isEqualTo(9.0d);
    assertThat(fgR00229.getSeverityDescription()).isEqualTo("CRITICAL");
    assertThat(fgR00229.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(fgR00229.getRatingMethod()).isNull();

    ThirdPartyCoordinateSecurity fgr00274 = tpvListC1.get(1);
    assertThat(fgr00274.getRefId()).isEqualTo("FG-R00274");
    assertThat(fgr00274.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(fgr00274.getAdvisories()).isNull();
    assertThat(fgr00274.getAttackVector()).isNull();
    assertThat(fgr00274.getCwes()).isNull();
    assertThat(fgr00274.getDescription()).isBlank();
    assertThat(fgr00274.getLink()).isNull();
    assertThat(fgr00274.getRecommendations()).isNull();
    assertThat(fgr00274.getSeverity()).isEqualTo(7.0d);
    assertThat(fgr00274.getSeverityDescription()).isEqualTo("HIGH");
    assertThat(fgr00274.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(fgr00274.getRatingMethod()).isNull();

    ThirdPartyCoordinateSecurity fgR0123 = tpvListC1.get(2);
    assertThat(fgR0123.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(fgR0123.getRefId()).isEqualTo("FG-R0123");
    assertThat(fgR0123.getAdvisories()).isNull();
    assertThat(fgR0123.getAttackVector()).isEqualTo("1.vectorString");
    assertThat(fgR0123.getCwes()).isNull();
    assertThat(fgR0123.getDescription()).isBlank();
    assertThat(fgR0123.getLink()).isEqualTo("1.url");
    assertThat(fgR0123.getRecommendations()).isNull();
    assertThat(fgR0123.getSeverity()).isEqualTo(4.0d);
    assertThat(fgR0123.getSeverityDescription()).isEqualTo("MEDIUM");
    assertThat(fgR0123.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(fgR0123.getRatingMethod()).isNull();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testMergeSonatypeDataWithSbomData_BestMatchWithSonatypeIdentifier() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(app.getId(), "1", file, PENDING);

    ThirdPartyFileCoordinate sbomComponent = null;
    try {
      sbomComponent =
          new ThirdPartyFileCoordinate("093080a1a4bbd2750540", "SBOM", "pypi", "orange", "1.0.1", file.getId());
      sbomComponent.setId("123456789"); // the same as in the bom.json results
      sbomComponent.setPackageUrl("pkg:pypi/citrus/orange@1.0.1?extension=whl&qualifier=py2.py3-none-any");
      sbomComponent.setIdentificationSources("SBOM");
      thirdPartyFileCoordinateDAO.insert(sbomComponent);
      ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-with-multiple-results",
          application.getId(), SCAN_ID);
      LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

      merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);
      ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);

      sbomComponent = thirdPartyFileCoordinateDAO.getById(sbomComponent.getId());
      assertThat(sbomComponent.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
      // updated purl from the best match result
      assertThat(sbomComponent.getPackageUrl()).isEqualTo(
          "pkg:pypi/citrus/orange@1.0.1?extension=whl&qualifier=py2.py3-none-any&arch=x86_64");

      // updated hash from the best match result
      assertThat(sbomComponent.getHash()).isEqualTo("093080a1a4bbd2750544");

      ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
      assertThat(updatedMetadata).isNotNull();

      List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(sbomComponent.getId());
      assertThat(thirdPartyCoordinateSecurityList).hasSize(2);

      List<ThirdPartyCoordinateLicense> licenses =
          thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(sbomComponent.getId());
      assertThat(licenses).hasSize(1);
      verify(mockTelemetrySender, times(1)).send(telemetryDataArgumentCaptor.capture());
      List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getValue();
      assertThat(telemetryDataList).hasSize(2);

      // verify telemetry data post import metrics
      TelemetryData telemetryData = telemetryDataList.get(0);
      assertThat(telemetryData).isNotNull();
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_POST_IMPORT_METRICS);
      assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
      SbomPostImportMetricsTelemetry importMetricsTelemetry =
          (SbomPostImportMetricsTelemetry) telemetryData.getAttributes().get("sbom_post_import_metrics");
      assertThat(importMetricsTelemetry.getVerifiedVulnerabilityCount()).isEqualTo(0);
      assertThat(importMetricsTelemetry.getUnverifiedVulnerabilityCount()).isEqualTo(0);
      assertThat(importMetricsTelemetry.getAdditionalVulnerabilitiesCount()).isEqualTo(2);
      assertThat(importMetricsTelemetry.getTotalVulnerabilitiesCount()).isEqualTo(0);

      // verify telemetry data best match metrics
      telemetryData = telemetryDataList.get(1);
      assertThat(telemetryData).isNotNull();
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_RESULT_BEST_MATCH_METRICS);
      assertThat(telemetryData.getAttributes()).hasSize(1).containsKey("sbom_results_matcher_stats");
      SbomResultsMatcherTelemetry resultsMatcherTelemetry =
          (SbomResultsMatcherTelemetry) telemetryData.getAttributes().get("sbom_results_matcher_stats");
      assertThat(resultsMatcherTelemetry.getWinnerStat())
          .extracting(s -> s.purlMatchScore, s -> s.hashMatchScore, s -> s.coordMatchScore)
          .containsExactly(20.0f, 0.0f, 15.0f);
      assertThat(resultsMatcherTelemetry.getMatchStats()).hasSize(4)
          .extracting(s -> s.purlMatchScore, s -> s.hashMatchScore, s -> s.coordMatchScore)
          .containsExactly(tuple(17.5f, 0.0f, 15.0f), tuple(16.25f, 0.0f, 15.0f),
              tuple(18.75f, 0.0f, 15.0f), tuple(20.0f, 0.0f, 15.0f));
    }
    finally {
      if (sbomComponent != null) {
        thirdPartyFileCoordinateDAO.delete(sbomComponent);
      }
    }
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_WithComponentRef() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(app.getId(), "1", file, PENDING);

    ThirdPartyFileCoordinate sbomComponent = null;
    try {
      sbomComponent = new ThirdPartyFileCoordinate("093080a1a4bbd2750540", "SBOM", "maven", "tomcat-catalina", "9.0.14",
          file.getId());
      sbomComponent.setId("123456789"); // the same as in the bom.json results
      sbomComponent.setPackageUrl("pkg:maven/org.apache.tomcat/tomcat-catalina@9.0.14?type=jar");
      sbomComponent.setIdentificationSources("SBOM");
      sbomComponent.setComponentRef("componentRef");
      thirdPartyFileCoordinateDAO.insert(sbomComponent);
      ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-with-component-ref",
          application.getId(), SCAN_ID);
      LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

      merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

      sbomComponent = thirdPartyFileCoordinateDAO.getById(sbomComponent.getId());
      assertThat(sbomComponent.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
      assertThat(sbomComponent.getPackageUrl()).isEqualTo(
          "pkg:maven/org.apache.tomcat/tomcat-catalina@9.0.14?type=jar");
      // updated hash from the matched result from HDS
      assertThat(sbomComponent.getHash()).isEqualTo("af008de6e523b6eeb5e8");

      List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(sbomComponent.getId());
      assertThat(thirdPartyCoordinateSecurityList).hasSize(8);

      List<ThirdPartyCoordinateLicense> licenses =
          thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(sbomComponent.getId());
      assertThat(licenses).hasSize(1);
    }
    finally {
      if (sbomComponent != null) {
        thirdPartyFileCoordinateDAO.delete(sbomComponent);
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testMergeSonatypeDataWithSbomData_VerifySecurityVulnerabilityUpdatesAndInsertsAndTelemetry() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "IaC", "terraform", "aws_s3_bucket.test01", "current",
            "0d8e3bd6ee4e6d50557a", "pkg:terraform/plan.tfplan/aws_s3_bucket.test01@current");

    // Update Scenario 1: existing third party security in db is not modified if not present in report zip or in
    // sonatype
    // FG-R00228 not in report zip but in db with minimal third party vulnerability data
    ThirdPartyCoordinateSecurity tpVuln1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00228", "", null, 0f, null, null);
    tpVuln1.setIdentificationSources("SBOM, Sonatype");
    thirdPartyCoordinateSecurityDAO.update(tpVuln1);

    // Update Scenario 2: existing third party coordinate security record in db is modified with sonatype data
    // FG-R00229 with complete third party vulnerability data
    ThirdPartyCoordinateSecurity tpVuln2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00229", "description1", "link1", 1.0f,
            "deepdive1", "fixedby1");
    tpVuln2.setIdentificationSources("SBOM");
    tpVuln2.setResearchType(DEEP_DIVE.name());
    tpVuln2.setDetectionType(SECONDARY.getId());
    thirdPartyCoordinateSecurityDAO.update(tpVuln2);

    // Insert Scenario 1: new third party coordinate security record is inserted in db with the minimal sonatype data
    // FG-R00274 with no third party vulnerability data in report zip or db

    // Insert Scenario 2: new third party coordinate security record is inserted in db with complete sonatype data
    // FG-R00275 with no third party vulnerability data in report zip or db

    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(application.getId(), "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-with-third-party-security-data",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    thirdPartyFileCoordinate = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyFileCoordinate.getIdentificationSources()).isEqualTo("SBOM");

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(7);

    // Update Scenario 1
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00228");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isEqualTo("<dd>a1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("v:1");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("<dd>1234</dd>");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isBlank();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isEqualTo("<dd>r1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isZero();
    assertThat(thirdPartyCoordinateSecurity.getSeverityDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getVulnerabilitySource()).isEqualTo("source");
    assertThat(thirdPartyCoordinateSecurity.getResearchType()).isEqualTo(VENDOR_RESEARCH.name());
    assertThat(thirdPartyCoordinateSecurity.getDetectionType()).isEqualTo(PRIMARY.getId());
    assertThat(thirdPartyCoordinateSecurity.getRatingMethod()).isEqualTo("m1");

    // Update Scenario 2
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00229");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isEqualTo("<dd>a1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString1");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("234");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("description1");
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new.link1");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isEqualTo("<dd>r1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(9.0d);
    assertThat(thirdPartyCoordinateSecurity.getSeverityDescription()).isEqualTo("CRITICAL");
    assertThat(thirdPartyCoordinateSecurity.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(thirdPartyCoordinateSecurity.getResearchType()).isEqualTo(DEEP_DIVE.name());
    assertThat(thirdPartyCoordinateSecurity.getDetectionType()).isEqualTo(SECONDARY.getId());
    assertThat(thirdPartyCoordinateSecurity.getRatingMethod()).isEqualTo("m1");

    // Insert Scenario 1
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00274");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isBlank();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(7.0d);
    assertThat(thirdPartyCoordinateSecurity.getSeverityDescription()).isEqualTo("HIGH");
    assertThat(thirdPartyCoordinateSecurity.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(thirdPartyCoordinateSecurity.getResearchType()).isEqualTo(FAST_TRACK.name());
    assertThat(thirdPartyCoordinateSecurity.getDetectionType()).isEqualTo(PRIMARY.getId());
    assertThat(thirdPartyCoordinateSecurity.getRatingMethod()).isNull();

    // Insert Scenario 2
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00275");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString5");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("789");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new.link5");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(7.0d);
    assertThat(thirdPartyCoordinateSecurity.getSeverityDescription()).isEqualTo("HIGH");
    assertThat(thirdPartyCoordinateSecurity.getVulnerabilitySource()).isEqualTo("NVD");
    assertThat(thirdPartyCoordinateSecurity.getResearchType()).isEqualTo(PUBLIC_RESEARCH.name());
    assertThat(thirdPartyCoordinateSecurity.getDetectionType()).isEqualTo(CPE_MATCH.getId());
    assertThat(thirdPartyCoordinateSecurity.getRatingMethod()).isEqualTo("CVSSV3");

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(updatedMetadata).isNotNull();

    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryDataList).hasSize(1);
    TelemetryData telemetryData = telemetryDataList.get(0);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_POST_IMPORT_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    SbomPostImportMetricsTelemetry telemetry = (SbomPostImportMetricsTelemetry) telemetryData.getAttributes()
        .get("sbom_post_import_metrics");
    assertThat(telemetry.getVerifiedVulnerabilityCount()).isEqualTo(1);
    assertThat(telemetry.getUnverifiedVulnerabilityCount()).isEqualTo(1);
    assertThat(telemetry.getAdditionalVulnerabilitiesCount()).isEqualTo(5);
    assertThat(telemetry.getTotalVulnerabilitiesCount()).isEqualTo(2);

    // verify cpe results telemetry
    assertThat(cpeResultsTelemetry.getCpeUnMatchedVulnerabilityCount()).isEqualTo(1);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testMergeSonatypeDataWithSbomData_vulnerabilities_mergeLogicForCMAndTelemetry() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "IaC", "terraform", "aws_s3_bucket.test01", "current",
            "0d8e3bd6ee4e6d50557a", "pkg:terraform/plan.tfplan/aws_s3_bucket.test01@current");

    // Vulnerability in DB - FG-R00230 - Only SBOM source identifier, Sonatype should be added.
    ThirdPartyCoordinateSecurity tpVuln1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00230", "description2", "link2", 1.0f,
            "deepdive2", "fixedby2");
    tpVuln1.setIdentificationSources("SBOM");
    thirdPartyCoordinateSecurityDAO.update(tpVuln1);

    // Vulnerability in DB - FG-R00231 - With both SBOM and Sonatype source identifiers. Nothing added.
    ThirdPartyCoordinateSecurity tpVuln2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00231", "description3", "link3", 1.0f,
            "deepdive3", "fixedby3");
    tpVuln2.setIdentificationSources("SBOM,Sonatype");
    tpVuln2.setCwes("");
    thirdPartyCoordinateSecurityDAO.update(tpVuln2);

    // Vulnerability in DB - FG-R00232 - With both SBOM and Sonatype source identifiers. No HDS results. Sonatype
    // source should be removed.
    ThirdPartyCoordinateSecurity tpVuln3 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00232", "description4", "link4", 1.0f,
            "deepdive4", "fixedby4");
    tpVuln3.setIdentificationSources("SBOM,Sonatype");
    thirdPartyCoordinateSecurityDAO.update(tpVuln3);

    // Vulnerability in DB - FG-R00233 - With only Sonatype source identifiers. No HDS results. Record should be
    // deleted from DB along with VEX annotations if any.
    ThirdPartyCoordinateSecurity tpVuln4 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00233", "description5", "link5", 1.0f,
            "deepdive5", "fixedby5");
    tpVuln4.setIdentificationSources("Sonatype");
    thirdPartyCoordinateSecurityDAO.update(tpVuln4);
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(tpVuln4, "FG-R00233", "resolved",
        "code_not_reachable", "will_not_fix,update", null);

    // Extra vulnerability in DB not in the file. It should be deleted.
    ThirdPartyCoordinateSecurity tpVuln5 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00234", "description6", "link6", 1.0f,
            "deepdive6", "fixedby6");
    tpVuln5.setIdentificationSources("Sonatype");
    thirdPartyCoordinateSecurityDAO.update(tpVuln5);

    // Extra vulnerability in DB not in the file. It should be deleted.
    ThirdPartyCoordinateSecurity tpVuln6 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00235", "description7", "link7", 1.0f,
            "deepdive7", "fixedby7");
    tpVuln6.setIdentificationSources("Sonatype");
    thirdPartyCoordinateSecurityDAO.update(tpVuln6);
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(tpVuln4, "FG-R00235", "resolved",
        "code_not_reachable", "will_not_fix,update", null);

    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata("appId", "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-with-third-party-security-data",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    thirdPartyFileCoordinate = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyFileCoordinate.getIdentificationSources()).isEqualTo("SBOM");

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(4);

    // Vulnerability not in DB - FG-R00229 - It should have Source Identifier = Sonatype
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00229");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString1");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new link1");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();

    // Vulnerability not in DB - FG-R00230 - It should have Source Identifier = SBOM,Sonatype
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00230");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isEqualTo("<dd>a1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString2");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("description2");
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new link2");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isEqualTo("<dd>r1<dd/>");

    // Vulnerability not in DB - FG-R00231 - It should have Source Identifier = SBOM,Sonatype
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00231");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isEqualTo("<dd>a1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString3");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("description3");
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new link3");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isEqualTo("<dd>r1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEmpty();

    // Vulnerability not in DB - FG-R00232 - It should have Source Identifier = SBOM
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00232");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isEqualTo(tpVuln3.getAdvisories());
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo(tpVuln3.getAttackVector());
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo(tpVuln3.getCwes());
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo(tpVuln3.getDescription());
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("link4");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isEqualTo(tpVuln3.getRecommendations());
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(1.0d);

    // Vulnerability not in DB - FG-R00233 - It should have been deleted from DB.
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00233");
    assertThat(thirdPartyCoordinateSecurity).isNull();
    ThirdPartyVulnerabilityExploitabilityExchange vexFromDB =
        thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(tpVuln5.getId(),
            "FG-R00233");
    assertThat(vexFromDB).isNull();

    // Vulnerability in DB not in file - FG-R00234 - It should have been deleted from DB.
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00234");
    assertThat(thirdPartyCoordinateSecurity).isNull();

    // Vulnerability in DB not in file - FG-R00234 - It should have been deleted from DB along with its VEX annotation.
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00235");
    assertThat(thirdPartyCoordinateSecurity).isNull();
    vexFromDB = thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(tpVuln6.getId(),
        "FG-R00235");
    assertThat(vexFromDB).isNull();

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(updatedMetadata).isNotNull();

    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryDataList).hasSize(1);
    TelemetryData telemetryData = telemetryDataList.get(0);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_POST_IMPORT_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    SbomPostImportMetricsTelemetry telemetry = (SbomPostImportMetricsTelemetry) telemetryData.getAttributes()
        .get("sbom_post_import_metrics");
    assertThat(telemetry.getVerifiedVulnerabilityCount()).isEqualTo(2);
    assertThat(telemetry.getUnverifiedVulnerabilityCount()).isEqualTo(1);
    assertThat(telemetry.getAdditionalVulnerabilitiesCount()).isEqualTo(1);
    assertThat(telemetry.getTotalVulnerabilitiesCount()).isEqualTo(6);
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_invalidPurlScenario() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(file, "SBOM", "maven",
        "commons-httpclient", "3.1", "964cd74171f427720480", null);
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "Apache", "Apache-2.0", "link1", "SBOM");

    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata("appId", "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-with-invalid-purl",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(updatedMetadata).isNotNull();
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_licenses_mergeLogicForCM() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "tp", "maven", "commons-httpclient", "3.1",
            "964cd74171f427720480", "pkg:maven/apache-httpclient/commons-httpclient@3.1?type=jar");

    // License from the json file, only with SBOM identification sources, so it should get Sonatype added to it.
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "Apache", "Apache-2.0", "link1", "SBOM");
    // License only in DB with both SBOM and Sonatype identification sources, so Sonatype should be removed.
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "AGPL-2.0", "AGPL-2.0", "link2",
        "SBOM,Sonatype");
    // License only in DB with both SBOM and Sonatype identification sources, so it should be deleted from the DB.
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "AGPL-3.0", "AGPL-3.0", "link3", "Sonatype");

    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(application.getId(), "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-with-third-party-license-data",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    mergerProvider.get().mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    thirdPartyFileCoordinate = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyFileCoordinate.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    List<ThirdPartyCoordinateLicense> thirdPartyCoordinateLicenseList = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyCoordinateLicenseList).hasSize(3);

    ThirdPartyCoordinateLicense thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "Apache");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("Apache");
    assertThat(thirdPartyCoordinateLicense.getName()).isEqualTo("Apache-2.0");
    assertThat(thirdPartyCoordinateLicense.getUrl()).isEqualTo("link1");
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "AGPL-2.0");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("AGPL-2.0");
    assertThat(thirdPartyCoordinateLicense.getName()).isEqualTo("AGPL-2.0");
    assertThat(thirdPartyCoordinateLicense.getUrl()).isEqualTo("link2");
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("SBOM");

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "AGPL-3.0");
    assertThat(thirdPartyCoordinateLicense).isNull();

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "AGPL-1.0");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("AGPL-1.0");
    assertThat(thirdPartyCoordinateLicense.getName()).isEqualTo("AGPL-1.0");
    assertThat(thirdPartyCoordinateLicense.getUrl()).isEqualTo("url.1");
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("Sonatype");

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(updatedMetadata).isNotNull();
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_componentWithNoLicenseDataGetsUnspecifiedSentinel() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    // Matches the rpm/sles/crypto-policies entry in licenses.json which has empty effectiveLicenses,
    // declaredLicenses, and observedLicenses — triggering the CLM-35969 sentinel injection.
    // The purl for pkg:rpm/sles/crypto-policies@20230920 gives getName()="crypto-policies" (namespace=sles),
    // which is what getByFormatNameVersionAndScanID uses to match the coordinate.
    ThirdPartyFileCoordinate rpmCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "cyclonedx", "rpm", "crypto-policies", "20230920",
            "9255ac9fe7070a2d0000", null);

    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(application.getId(), "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-with-third-party-license-data",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    mergerProvider.get().mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    ThirdPartyCoordinateLicense unspecifiedLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(rpmCoordinate.getId(), License.UNSPECIFIED_ID);
    assertThat(unspecifiedLicense).isNotNull();
    assertThat(unspecifiedLicense.getLicenseId()).isEqualTo(License.UNSPECIFIED_ID);
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_VerifyPythonSecurityAndVulnerabilityUpdates() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "SBOM", "pypi", "pip", "24.0", "964cd74171f427720480",
            "pkg:pypi/pip@24.0", "9137f8464d054304e0ce273e524d7a1f824463af");

    ThirdPartyCoordinateLicense license =
        tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "GPL-2.0", "GPL-2.0", null);
    license.setIdentificationSources("Sonatype");

    thirdPartyCoordinateLicenseDAO.update(license);

    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata("appId", "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-with-python-components",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    List<ThirdPartyCoordinateLicense> thirdPartyCoordinateLicenseList = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateId(thirdPartyFileCoordinate.getId());

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList = thirdPartyCoordinateSecurityDAO
        .getByFileCoordinateId(thirdPartyFileCoordinate.getId());

    assertThat(thirdPartyCoordinateLicenseList).hasSize(1);
    assertThat(thirdPartyCoordinateSecurityList).hasSize(3);

    ThirdPartyCoordinateLicense thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "GPL-2.0");
    assertThat(thirdPartyCoordinateLicense).isNull();

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "MIT");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("MIT");
    assertThat(thirdPartyCoordinateLicense.getName()).isNull();
    assertThat(thirdPartyCoordinateLicense.getUrl()).isNull();
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("Sonatype");

    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity = thirdPartyCoordinateSecurityDAO
        .getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "CVE-2018-20225");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString1");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("348");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new.link1");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(7.8d);

    thirdPartyCoordinateSecurity = thirdPartyCoordinateSecurityDAO
        .getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "CVE-2023-45803");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString2");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("200");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new.url2");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(4.2d);

    thirdPartyCoordinateSecurity = thirdPartyCoordinateSecurityDAO
        .getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "CVE-2024-3651");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString3");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("400");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new.link3");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(6.2d);

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(updatedMetadata).isNotNull();
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_duplicatedComponentsWithDifferentPurlHashGetsInsertedInsteadOfMerging() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile("binary.temp");
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    // Record existing in db before importing
    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        tempEntity.newThirdPartyFileCoordinate(file, "SBOM", "pypi", "pip", "24.0", "XYZ", "pkg:pypi/pip@24.0?type=zip",
            "9137f8464d054304e0ce273e524d7a1f824463af");

    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadataForBinaryScan(app.getId(), "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-with-python-components",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    List<ThirdPartyCoordinateLicense> thirdPartyCoordinateLicenseList = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateId(thirdPartyFileCoordinate1.getId());

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList = thirdPartyCoordinateSecurityDAO
        .getByFileCoordinateId(thirdPartyFileCoordinate1.getId());

    assertThat(thirdPartyCoordinateLicenseList).hasSize(1);
    assertThat(thirdPartyCoordinateSecurityList).hasSize(3);

    List<ThirdPartyFileCoordinate> componentsInserted = thirdPartyFileCoordinateDAO.getBySbomMetadataId(
        sbomMetadata.getId());
    // 2 components: 1 updated with data in db, the second one inserted
    assertThat(componentsInserted).hasSize(2);
    ComponentIdentifier expectedComponentIdentifier = ComponentIdentifier.createPypiCoordinates("pip",
        "24.0", null, "tar.gz");
    final PackageUrlIdentifier expectedPurlTarGz = PackageUrlIdentifier
        .fromComponentIdentifier(expectedComponentIdentifier);
    Optional<ThirdPartyFileCoordinate> componentUpdatedTarGzExtensionOptional = componentsInserted.stream()
        .filter(
            c -> c.getPackageUrl().equals(expectedPurlTarGz.getPackageUrl()))
        .findFirst();
    // Hash updated
    assertThat(componentUpdatedTarGzExtensionOptional.isPresent()).isTrue();
    assertThat(componentUpdatedTarGzExtensionOptional.get().getHash()).isEqualTo("964cd74171f427720480");

    // New component inserted
    expectedComponentIdentifier = ComponentIdentifier.createPypiCoordinates("pip", "24.0", "py3-none-any", "whl");
    final PackageUrlIdentifier expectedPurlWhl = PackageUrlIdentifier.fromComponentIdentifier(
        expectedComponentIdentifier);
    Optional<ThirdPartyFileCoordinate> newlyInsertedComponentUpdatedWhlOptional = componentsInserted.stream()
        .filter(
            c -> c.getPackageUrl().equals(expectedPurlWhl.getPackageUrl()))
        .findFirst();
    assertThat(newlyInsertedComponentUpdatedWhlOptional.isPresent()).isTrue();
    assertThat(newlyInsertedComponentUpdatedWhlOptional.get().getHash()).isEqualTo("e44313ae1e6af3c2bd3b");

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(updatedMetadata).isNotNull();
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_VerifyComponentIdentificationSourceAndDependencyType() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        tempEntity.newThirdPartyFileCoordinate(file, "src", "nuget", "Microsoft.IdentityModel.JsonWebTokens", "6.25.1",
            "0e3da21fd80b9853692d", "pkg:nuget/Microsoft.IdentityModel.JsonWebTokens@6.25.1");
    thirdPartyFileCoordinate1.setMatchStateId("similar");
    thirdPartyFileCoordinateDAO.update(thirdPartyFileCoordinate1);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        tempEntity.newThirdPartyFileCoordinate(file, "src", "nuget", "Microsoft.IdentityModel.Protocols", "6.25.1",
            "c795e78734c2860bb627", "pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1");
    thirdPartyFileCoordinate2.setFilenamesList(List.of("f1,f2"));
    thirdPartyFileCoordinateDAO.update(thirdPartyFileCoordinate2);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate3 =
        tempEntity.newThirdPartyFileCoordinate(file, "src", "nuget", "Microsoft.Extensions.Options", "5.0.0",
            "d98bcd35050378773586", "pkg:nuget/Microsoft.Extensions.Options@5.0.0");
    thirdPartyFileCoordinate3.setOccurrencesList(List.of("o1"));
    thirdPartyFileCoordinateDAO.update(thirdPartyFileCoordinate3);

    ThirdPartyCoordinateSecurity tpVuln1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1, "CVE-2024-21319", "description", null, 0f,
            null, null);
    tpVuln1.setIdentificationSources("SBOM");
    thirdPartyCoordinateSecurityDAO.update(tpVuln1);

    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2, "CVE-2022-38013", "description1", "link1",
        1.0f, "fixedBy1", "vulnSource1", "vectorString1", "high1", "cwes1", "deepdive1", "recommendations1",
        "advisories1", "SBOM");

    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(application.getId(), "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-with-dependencies",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    thirdPartyFileCoordinate1 = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate1.getId());
    assertThat(thirdPartyFileCoordinate1.getDependencyType()).isEqualTo("T");
    assertThat(thirdPartyFileCoordinate1.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    assertThat(thirdPartyFileCoordinate1.getMatchStateId()).isEqualTo("similar");
    assertThat(thirdPartyFileCoordinate1.getFilenamesList()).containsExactlyInAnyOrder(
        "pkg:nuget/Microsoft.IdentityModel.JsonWebTokens@6.25.1:0e3da21fd80b9853692d");
    assertThat(thirdPartyFileCoordinate1.getOccurrencesList()).containsExactlyInAnyOrder(
        "dependency:/app6-bom.xml/pkg:nuget\\Microsoft.IdentityModel.JsonWebTokens@6.25.1:0e3da21fd80b9853692d");

    thirdPartyFileCoordinate2 = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate2.getId());
    assertThat(thirdPartyFileCoordinate2.getDependencyType()).isEqualTo("D");
    assertThat(thirdPartyFileCoordinate2.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    assertThat(thirdPartyFileCoordinate2.getMatchStateId()).isEqualTo("exact");
    assertThat(thirdPartyFileCoordinate2.getFilenamesList()).containsExactlyInAnyOrder(
        "f1", "f2");
    assertThat(thirdPartyFileCoordinate2.getOccurrencesList()).containsExactlyInAnyOrder(
        "dependency:/app6-bom.xml/pkg:nuget\\Microsoft.IdentityModel.Protocols@6.25.1:c795e78734c2860bb627");

    thirdPartyFileCoordinate3 = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate3.getId());
    assertThat(thirdPartyFileCoordinate3.getDependencyType()).isNull();
    assertThat(thirdPartyFileCoordinate3.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    assertThat(thirdPartyFileCoordinate3.getMatchStateId()).isEqualTo("exact");
    assertThat(thirdPartyFileCoordinate3.getFilenamesList()).containsExactlyInAnyOrder(
        "pkg:nuget/Microsoft.Extensions.Options@5.0.0:d98bcd35050378773586");
    assertThat(thirdPartyFileCoordinate3.getOccurrencesList()).containsExactlyInAnyOrder(
        "o1");

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate1.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(1);

    thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate2.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(1);

    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate1.getId(),
            "CVE-2024-21319");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("description");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate2.getId(),
            "CVE-2022-38013");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM");

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(updatedMetadata).isNotNull();
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_CweIds() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "IaC", "terraform", "aws_s3_bucket.test01", "current",
            "0d8e3bd6ee4e6d50557a", "pkg:terraform/plan.tfplan/aws_s3_bucket.test01@current");

    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata("appId", "1", file, ACTIVE);

    ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-with-third-party-security-data",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    mergerProvider.get().mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    thirdPartyFileCoordinate = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate.getId());
    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(3);

    // Vulnerability not in DB - FG-R00229 - It should have Source Identifier = Sonatype
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00229");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("508");

    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00230");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isNull();
  }

  @Test
  public void testMergeResults_OriginalBinaryFileName() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    String filename = "postgres";
    ThirdPartyFile file = tempEntity.newThirdPartyFile(filename);
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadataForBinaryScan(null, "1", file, PENDING);
    sbomMetadata.setOriginalBinaryFileName(filename);

    ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-for-binary-scan",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    merger.mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);

    ThirdPartySbomMetadata updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());

    assertThat(updatedMetadata.getFilename()).isEqualTo(filename + "." + updatedMetadata.getSbomVersion() + ".json.gz");

    filename = "postgres.1.3";
    file = tempEntity.newThirdPartyFile(filename);
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, "scan2", file);
    sbomMetadata = tempEntity.createSbomMetadataForBinaryScan(null, "2", file, PENDING);
    sbomMetadata.setOriginalBinaryFileName(filename);

    ReportHelper.saveMockReport(insightWork, tempDir, "/SbomResultsMergerTest/report-for-binary-scan",
        application.getId(), "scan2");
    lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, "scan2");

    merger.mergeResults(sbomMetadata, "scan2", lifecycleReport, cpeResultsTelemetry);

    updatedMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());

    String binaryFileName = updatedMetadata.getOriginalBinaryFileName();
    int index = binaryFileName.lastIndexOf(".") == -1 ? binaryFileName.length() : binaryFileName.lastIndexOf(".");
    String compressedBinaryFileName =
        binaryFileName.substring(0, index) + "." + updatedMetadata.getSbomVersion() + ".json.gz";
    assertThat(updatedMetadata.getFilename()).isEqualTo(compressedBinaryFileName);
  }

  @Test
  public void testCleanUpPreviousReport() throws IOException {
    executeCleanUpPreviousReportTest(true, false);
  }

  @Test
  public void testCleanUpPreviousReport_FeatureDisabled() throws IOException {
    executeCleanUpPreviousReportTest(false, true);
  }

  @Test
  public void testMergeResults_DuplicateComponentsMergedByComponentRefs() throws IOException {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinateA =
        tempEntity.newThirdPartyFileCoordinate(file, "tp", "maven", "commons", "1.3",
            "fakehash", "pkg:maven/apache-httpclient/commons@1.3?type=jar", "componentRefA");

    tempEntity.newThirdPartyFileCoordinate(file, "tp", "maven", "commons-httpclient", "3.1",
        "964cd74171f427720480", "pkg:maven/apache-httpclient/commons-httpclient@3.1?type=jar", "componentRefB");

    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(application.getId(), "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir,
        "/SbomResultsMergerTest/report-for-binary-with-duplicate-components-and-component-refs",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    mergerProvider.get().mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);
    List<ThirdPartyFileCoordinate> components = thirdPartyFileCoordinateDAO.getByThirdPartyFileId(file.getId());
    assertThat(components).hasSize(1);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = components.get(0);
    assertThat(thirdPartyFileCoordinate.getId()).isEqualTo(thirdPartyFileCoordinateA.getId());
    assertThat(thirdPartyFileCoordinate.getFormat()).isEqualTo("maven");
    assertThat(thirdPartyFileCoordinate.getName()).isEqualTo("commons");
    assertThat(thirdPartyFileCoordinate.getVersion()).isEqualTo("1.3");
    assertThat(thirdPartyFileCoordinate.getHash()).isEqualTo("964cd74171f427720480");
    assertThat(thirdPartyFileCoordinate.getComponentRef()).isEqualTo("componentRefA");
    assertThat(thirdPartyFileCoordinate.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
  }

  @Test
  public void testMergeResults_DuplicateComponentsVulnsLicensesMergedByComponentRefs() throws IOException {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinateA =
        tempEntity.newThirdPartyFileCoordinate(file, "tp", "maven", "commons", "1.3",
            "fakehash", "pkg:maven/apache-httpclient/commons@1.3?type=jar", "componentRefA");

    ThirdPartyFileCoordinate thirdPartyFileCoordinateB =
        tempEntity.newThirdPartyFileCoordinate(file, "tp", "maven", "commons-httpclient", "3.1",
            "964cd74171f427720480", "pkg:maven/apache-httpclient/commons-httpclient@3.1?type=jar", "componentRefB");

    ThirdPartyCoordinateLicense componentRefBLicense =
        tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinateB, "licenseId", "licenseName", "licenseUrl");

    ThirdPartyCoordinateSecurity componentRefBVulnerability =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinateB, "vulnId", "Description",
            "link", 9.0, "Description", "fixed by");

    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata("appId", "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir,
        "/SbomResultsMergerTest/report-for-binary-with-duplicate-components-and-component-refs",
        application.getId(), SCAN_ID);
    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, application, SCAN_ID);

    mergerProvider.get().mergeResults(sbomMetadata, SCAN_ID, lifecycleReport, cpeResultsTelemetry);
    List<ThirdPartyFileCoordinate> components = thirdPartyFileCoordinateDAO.getByThirdPartyFileId(file.getId());
    assertThat(components).hasSize(1);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = components.get(0);
    assertThat(thirdPartyFileCoordinate.getId()).isEqualTo(thirdPartyFileCoordinateA.getId());
    assertThat(thirdPartyFileCoordinate.getFormat()).isEqualTo("maven");
    assertThat(thirdPartyFileCoordinate.getName()).isEqualTo("commons");
    assertThat(thirdPartyFileCoordinate.getVersion()).isEqualTo("1.3");
    assertThat(thirdPartyFileCoordinate.getHash()).isEqualTo("964cd74171f427720480");
    assertThat(thirdPartyFileCoordinate.getComponentRef()).isEqualTo("componentRefA");
    assertThat(thirdPartyFileCoordinate.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    List<ThirdPartyCoordinateSecurity> vulnerabilities =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(vulnerabilities).hasSize(3);
    assertThat(vulnerabilities.stream().map(ThirdPartyCoordinateSecurity::getRefId))
        .hasSameElementsAs(Set.of("CVE-2012-5783", "CVE-2012-5784", componentRefBVulnerability.getRefId()));
    List<ThirdPartyCoordinateLicense> licenses =
        thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(licenses).hasSize(3);
    assertThat(licenses.stream().map(ThirdPartyCoordinateLicense::getLicenseId))
        .hasSameElementsAs(Set.of("Apache-2.0", "AGPL-1.0", componentRefBLicense.getLicenseId()));
  }

  private File mockReportZipWithUpdatedThirdPartyData(
      final String resource,
      final ThirdPartyFileCoordinate tpComponent) throws Exception
  {
    File testReportDir = new File(getResource(resource).toURI());
    File tmpReportDir = tempDir.newFolder();
    FileUtils.copyDirectory(testReportDir, tmpReportDir);

    File bomJsonFile = new File(tmpReportDir, "bom.json");
    ContainerNode<?> bomJson = JsonUtils.parse(FileUtils.readFileToByteArray(bomJsonFile));

    enhanceBomJsonWithMockedThirdPartyComponent(bomJson, tpComponent);
    FileUtils.writeByteArrayToFile(bomJsonFile, JsonUtils.generate(bomJson));

    File zipTarget = tempDir.newFile("report.zip");
    Zipper.zipFilesInDirectory(tmpReportDir, zipTarget);
    return zipTarget;
  }

  private void executeCleanUpPreviousReportTest(
      boolean featureEnabled,
      boolean expectedPreviousReportDirExists) throws IOException
  {
    String appId = "appId";
    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan("scanRequestId", "scanId", file);
    thirdPartyScan.setPreviousScanId("previousScanId");
    thirdPartyScanDAO.update(thirdPartyScan);

    FileUtils.forceMkdir(insightWork.getReportDir(appId, thirdPartyScan.getPreviousScanId()));
    FileUtils.forceMkdir(insightWork.getReportDir(appId, thirdPartyScan.getScanId()));

    if (!featureEnabled) {
      SystemConfigurationPropertyFeature.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT.setEnabled(false);
    }

    mergerProvider.get().cleanUpPreviousReport(appId, file.getId(), thirdPartyScan.getScanId());

    ThirdPartyScan updatedThirdPartyScan = thirdPartyScanDAO.getById(thirdPartyScan.getId());
    assertThat(insightWork.getReportDir(appId, "previousScanId").exists()).isEqualTo(expectedPreviousReportDirExists);
    assertThat(insightWork.getReportDir(appId, thirdPartyScan.getScanId()).exists()).isTrue();
    assertThat(updatedThirdPartyScan.getPreviousScanId()).isNull();
    assertThat(thirdPartyScanDAO.getById(thirdPartyScan.getId())).isNotNull();
  }

  private URL getResource(final String name) {
    return getClass().getResource(name);
  }

  private void enhanceBomJsonWithMockedThirdPartyComponent(
      final ContainerNode<?> bomJson,
      final ThirdPartyFileCoordinate tpComponent)
  {
    ArrayNode aaNode = (ArrayNode) bomJson.get("aaData");
    ObjectNode componentNode = aaNode.insertObject(0);
    ObjectNode componentIdentifierNode = componentNode.putObject("componentIdentifier");
    componentIdentifierNode.put("format", "pypi");
    componentIdentifierNode.putObject("coordinates")
        .put("extension", "whl")
        .put("name", "orange")
        .put("qualifier", "py2.py3-none-any")
        .put("version", "1.0.1");

    componentNode.put("packageUrl", "pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl")
        .put("sonatypeIdentifier", tpComponent.getId())
        .put("filenames", "pkg:pypi/orange@1.0.1")
        .put("pathnames", "pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl")
        .put("matchState", "exact")
        .put("scanError", false)
        .put("proprietary", false)
        .put("hash", "093080a1a4bbd2750541")
        .put("createTime", "1706953998000")
        .put("directDependency", true)
        .put("identificationSource", "Sonatype");
    componentNode.putArray("filenames").insert(0, "pkg:pypi/orange@1.0.1");
    componentNode.putArray("pathnames").insert(0, "dependency:/SBOM-bom.json/pkg:pypi\\orange@1.0.1");
  }

  private void assertComponentRefProperty(Component component) {
    Property componentRefProperty = component.getProperties()
        .stream()
        .filter(property -> property.getName().equals("componentRef"))
        .findFirst()
        .orElse(null);
    assertThat(componentRefProperty).isNotNull();
    assertThat(componentRefProperty.getValue()).isNotBlank().hasSize(40);
  }
}
