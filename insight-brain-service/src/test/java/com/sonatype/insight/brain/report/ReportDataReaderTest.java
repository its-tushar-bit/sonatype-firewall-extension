/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.SCAN_ID;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType;
import jakarta.inject.Inject;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ReportDataReaderTest
    extends AbstractComponentTest
{
  @Inject
  private InsightWork insightWork;

  @Mock
  private HdsClient hdsClient;

  @Mock
  private H2ApplicationRiskService h2ApplicationRiskService;

  @Inject
  private ReportDataReader reportDataReader;

  @Inject
  private LicenseDAO licenseDAO;

  private Application app;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testGetComponentDetailsByIdentifier() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportDataReaderTest/report",
        app.getId(), SCAN_ID);

    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("commons-beanutils", "commons-beanutils", "1.6", "", "jar");
    NamedComponentDetails details =
        reportDataReader.getComponentDetailsByIdentifier(identifier, app.getId(), SCAN_ID);
    assertComponentDetails(details, identifier,
        new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.HASH, null));
  }

  @Test
  public void testGetComponentDetailsByIdentifier_component_notExists() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportDataReaderTest/report",
        app.getId(), SCAN_ID);

    ComponentIdentifier identifier =
        new PackageUrlIdentifier("pkg:maven/com.example/nonexistent@1.0.0").toComponentIdentifier();
    NamedComponentDetails details =
        reportDataReader.getComponentDetailsByIdentifier(identifier, app.getId(), SCAN_ID);

    assertThat(details).isNull();
  }

  @Test
  public void testGetComponentDetailsByIdentifier_report_notExists() {
    ComponentIdentifier identifier =
        new PackageUrlIdentifier("pkg:maven/com.example/nonexistent@1.0.0").toComponentIdentifier();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      reportDataReader.getComponentDetailsByIdentifier(identifier, app.getId(), SCAN_ID);
    }).withMessage("Could not find a report with ID " + SCAN_ID);
  }

  @Test
  public void testGetComponentDetailsByIdentifier_tenantAware() {
    String tenant1ScanId = "tenant1ScanId";
    String tenant2ScanId = "tenant2ScanId";
    Runnable mockRunnable = mock(Runnable.class);
    final NamedComponentDetails[] componentDetails = new NamedComponentDetails[1];
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("commons-beanutils", "commons-beanutils", "1.6", "", "jar");

    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      Application tenant1App = tempEntity.newApplicationWithParent();

      ReportHelper.saveMockReport(insightWork, tempDir, "/ReportDataReaderTest/report",
          tenant1App.getId(), tenant1ScanId);
      componentDetails[0] =
          reportDataReader.getComponentDetailsByIdentifier(identifier, tenant1App.getId(), tenant1ScanId);

      mockRunnable.run();
    });

    testAsTenant(tenant1, t1 -> {
      assertComponentDetails(componentDetails[0], identifier,
          new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.HASH, null));
      assertThat(reportDataReader.componentCache.get().getIfPresent(tenant1ScanId)).isNotNull();
      assertThat(reportDataReader.componentCache.get().getIfPresent(tenant2ScanId)).isNull();

      mockRunnable.run();
    });

    Tenant tenant2 = testAsNewTenant(testName, t2 -> {
      Application tenant2App = tempEntity.newApplicationWithParent();

      ReportHelper.saveMockReport(insightWork, tempDir, "/ReportDataReaderTest/report",
          tenant2App.getId(), tenant2ScanId);

      componentDetails[0] =
          reportDataReader.getComponentDetailsByIdentifier(identifier, tenant2App.getId(), tenant2ScanId);

      mockRunnable.run();
    });

    testAsTenant(tenant2, t2 -> {
      assertComponentDetails(componentDetails[0], identifier,
          new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.HASH, null));
      assertThat(reportDataReader.componentCache.get().getIfPresent(tenant2ScanId)).isNotNull();
      assertThat(reportDataReader.componentCache.get().getIfPresent(tenant1ScanId)).isNull();

      mockRunnable.run();
    });
  }

  @Test
  public void testGetComponentDetailsByIdentifier_emptyLicenses() throws IOException {
    String scanId = "scanId";
    final NamedComponentDetails[] componentDetails = new NamedComponentDetails[1];
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("commons-beanutils", "commons-beanutils", "1.6", "", "jar");

    Application app = tempEntity.newApplicationWithParent();

    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportDataReaderTest/report-empty-licenses", app.getId(),
        scanId);
    componentDetails[0] =
        reportDataReader.getComponentDetailsByIdentifier(identifier, app.getId(), scanId);

    assertThat(componentDetails[0].getDeclaredLicenseIds()).isEmpty();
    assertThat(componentDetails[0].getObservedLicenseIds()).isEmpty();
    assertThat(componentDetails[0].getEffectiveLicenses()).isEmpty();

    com.sonatype.insight.brain.model.license.License licenseNotProvided =
        licenseDAO.getByIdNotNull(com.sonatype.insight.brain.model.license.License.UNSPECIFIED_ID);
    License unspecifiedLicense = new License(licenseNotProvided.getId(), licenseNotProvided.getShortDisplayName());

    // Check that an empty collection of licenses can be augmented using the default Not Provided (unspecified) license
    componentDetails[0].getDeclaredLicenses().add(unspecifiedLicense);
    componentDetails[0].getObservedLicenses().add(unspecifiedLicense);
    componentDetails[0].getEffectiveLicenses().add(unspecifiedLicense);

    assertThat(componentDetails[0].getDeclaredLicenseIds()).containsExactly(unspecifiedLicense.getLicenseId());
    assertThat(componentDetails[0].getObservedLicenseIds()).containsExactly(unspecifiedLicense.getLicenseId());
    assertThat(componentDetails[0].getEffectiveLicenses()).containsExactly(unspecifiedLicense);
  }

  private static void assertComponentDetails(
      final NamedComponentDetails details,
      final ComponentIdentifier identifier,
      final AnalyzerFeatures analyzerFeatures)
  {
    assertThat(details).isNotNull();
    assertThat(details.getComponentIdentifier()).isEqualTo(identifier);
    assertThat(details.getDeclaredLicenseIds()).containsExactlyInAnyOrder("Apache-2.0");
    assertThat(details.getObservedLicenseIds()).containsExactlyInAnyOrder("Apache-2.0", "MIT");
    assertThat(details.getEffectiveLicenses()).isNotEmpty()
        .extracting("licenseId")
        .containsExactlyInAnyOrder("MIT");
    assertThat(details.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability vulnerability = details.getSecurityVulnerabilities().get(0);
    assertThat(vulnerability.getSeverity()).isEqualTo(9.0f);
    assertThat(vulnerability.getSource()).isEqualTo("NVD");
    assertThat(vulnerability.getRefId()).isEqualTo("CVE-2023-1234");
    assertThat(vulnerability.getCvssVectorSource()).isEqualTo("vector-source");
    assertThat(vulnerability.getCvssVector()).isEqualTo("vector-string");
    assertThat(vulnerability.getCwe()).isEqualTo("cwe-1,2.cwe");
    assertThat(vulnerability.getAliases()).containsExactlyInAnyOrder("Sonatype-2023-1234");
    assertThat(vulnerability.getResearchType()).isEqualTo(SecurityVulnerabilityResearchType.PUBLIC_RESEARCH.getId());
    assertThat(vulnerability.getDetectionType()).isEqualTo(SecurityVulnerabilityDetectionType.CPE_MATCH.getId());
    assertThat(vulnerability.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertAnalyzerFeatures(details, analyzerFeatures);
  }

  private static void assertAnalyzerFeatures(
      final NamedComponentDetails details,
      final AnalyzerFeatures expectedAnalyzerFeatures)
  {
    assertThat(details.getAnalyzerFeatures()).isNotNull();
    assertThat(details.getAnalyzerFeatures().getAnalysisType()).isEqualTo(expectedAnalyzerFeatures.getAnalysisType());
    assertThat(details.getAnalyzerFeatures().getAnalysisSource()).isEqualTo(
        expectedAnalyzerFeatures.getAnalysisSource());
  }
}
