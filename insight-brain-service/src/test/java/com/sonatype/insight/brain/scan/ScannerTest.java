/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scan.datastore.FileScanEntity;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.ScanMetadata;
import com.sonatype.insight.scan.model.io.ScanReader;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.sonatype.insight.license.model.LicensedFeature.INFRASTRUCTURE_AS_CODE_PACK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScannerTest
    extends BrainInjectedTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Inject
  private Scanner scanner;

  @Inject
  private ScanReader scanReader;

  private final ProductLicense productLicense = mock(ProductLicense.class);

  private final FeaturesService featuresService = mock(FeaturesService.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(productLicense);
    binder.bind(FeaturesService.class).toInstance(featuresService);
    super.configure(binder);
  }

  @Before
  public void before() {
    when(productLicense.isValid()).thenReturn(true);
  }

  @Test
  public void testScan() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Collections.singletonList("com.sonatype"));

    File appFile = new File("src/test/resources/ScannerTest/app01.zip");
    ScanResult scanResult = scanner.scan(appFile, "test-app.zip", "appId", proprietaryConfig);
    assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE);

    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getItems()).hasSize(1);
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("test-app.zip");
    assertThat(item.getItems()).hasSize(1);
    item = item.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("proprietary.jar");
    assertThat(item.getItems()).hasSize(1);
    assertThat(item.isProprietary()).isNull();
    assertThat(item.getContentType()).isNull();
    item = item.getItems().get(0);
    assertThat(item.getSha512()).isEqualTo("b1c5fe2f797abc2da1df1a103abcfe391a1fdf4d3de08012c4f121065ff055742"
        + "f726550bbde22958baf3be18b5eeae12b39fdd4069736e620b5d0397e0c4e2c");
    assertThat(item.getSha1()).isEqualTo("44a17e5a5594edeebc94");
    assertThat(item.getSha1JA001()).isNotNull();
    assertThat(item.getSha1JB001()).isNotNull();
    assertThat(item.getSha1JC001()).isNotNull();
    assertThat(item.getSha1JD001()).isNotNull();
    assertThat(item.getPath()).isNull();
    assertThat(item.getNoPathReason()).isEqualTo("proprietaryPackages");
  }

  @Test
  public void testScan_CdxXmlZipFile() throws Exception {
    testFilenamePatterns("application.cdx.xml.zip", "application.cdx.xml", ItemContentType.SBOM);
  }

  @Test
  public void testScan_CdxJsonZipFile() throws Exception {
    testFilenamePatterns("application.cdx.json.zip", "application.cdx.json", ItemContentType.SBOM);
  }

  @Test
  public void testScan_SpdxXmlZipFile() throws Exception {
    testFilenamePatterns("valid-v2.3.spdx.xml.zip", "valid-v2.3.spdx.xml", ItemContentType.SPDX);
  }

  @Test
  public void testScan_SpdxJsonZipFile() throws Exception {
    testFilenamePatterns("valid-v2.3.spdx.json.zip", "valid-v2.3.spdx.json", ItemContentType.SPDX);
  }

  @Test
  public void testScan_ProprietaryRegex() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setRegexes(Collections.singletonList(".*prop.*\\.jar"));

    File appFile = new File("src/test/resources/ScannerTest/app01.zip");
    ScanResult scanResult = scanner.scan(appFile, "test-app.zip", "appId",
        proprietaryConfig);
    assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE);

    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getItems()).hasSize(1);
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("test-app.zip");
    assertThat(item.getItems()).hasSize(1);
    item = item.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("proprietary.jar");
    assertThat(item.isProprietary()).isTrue();
  }

  @Test
  public void testScanContent_SbomFile() throws Exception {
    String sbom =
        readFileToString(new File("src/test/resources/ScannerTest/iq-scan-sbom.xml"), UTF_8)
            .replace("\r\n", "\n");

    String scannerDriver = "thirdPartyApiTest";
    ScanResult scanResult =
        scanner.scanThirdPartyContent(sbom, "appId", ItemContentType.SBOM, "ABCD", SbomFormat.XML, null, scannerDriver);
    assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE_THIRD_PARTY);
    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getSummary().getScannerDriver()).isEqualTo(scannerDriver);
    assertThat(scan.getSummary().getClientInfo()).containsEntry("insight.scannerDriver", scannerDriver);
    assertThat(scan.getItems()).hasSize(1);
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("ABCD-bom.xml");
    assertThat(item.getItems()).isEmpty();
    assertThat(item.getContentType()).isEqualTo(ItemContentType.SBOM);

    assertThat(item.getSha1()).isEqualTo("6e263804dcfedb414bf3");
  }

  @Test
  public void testScanContent_SbomFile_Json() throws Exception {
    String sbom =
        readFileToString(new File("src/test/resources/ScannerTest/iq-scan-sbom.json"), UTF_8)
            .replace("\r\n", "\n");

    String scannerDriver = "thirdPartyApiTest";
    ScanResult scanResult =
        scanner.scanThirdPartyContent(sbom, "appId", ItemContentType.SBOM, "ABCD", SbomFormat.JSON, null,
            scannerDriver);
    assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE_THIRD_PARTY);
    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getSummary().getScannerDriver()).isEqualTo(scannerDriver);
    assertThat(scan.getSummary().getClientInfo()).containsEntry("insight.scannerDriver", scannerDriver);
    assertThat(scan.getItems()).hasSize(1);
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("ABCD-bom.json");
    assertThat(item.getItems()).isEmpty();
    assertThat(item.getContentType()).isEqualTo(ItemContentType.SBOM);

    assertThat(item.getSha1()).isEqualTo("54bffc1f3407804ceccb");
  }

  @Test
  public void testScanContent_invalidSbom_skipSbomValidationDisabled() throws Exception {
    File sbom = new File("src/test/resources/ScannerTest/cdx-v1.4-invalid-bom.xml");

    ScanResult scanResult = scanner.scan(sbom, "test-sbom.xml", "appId", new ProprietaryConfig());
    assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getItems()).hasSize(1);
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getContentType()).isNull();
    assertThat(item.getHasError()).isTrue();
  }

  @Test
  public void testScanContent_invalidSbom_skipSbomValidationEnabled() throws Exception {
    SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.setEnabled(true);
    File sbom = new File("src/test/resources/ScannerTest/cdx-v1.4-invalid-bom.xml");

    ScanResult scanResult = scanner.scan(sbom, "test-sbom.xml", "appId", new ProprietaryConfig());
    assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getItems()).hasSize(1);
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getContentType()).isNull();
    assertThat(item.getHasError()).isTrue();
  }

  @Test
  public void testScan_SourceControl() throws Exception {
    // given: setup and configs what would be used by a client for a source control scan
    File scanDir = new File("src/test/resources/ScannerTest/sourceControlScan");
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    ScanMetadata scanMetadata = new ScanMetadata().withCommitHash("commit-xyz");

    // when: perform the scan
    ScanResult scanResult =
        scanner.scan(Collections.singletonList(scanDir), "appId", proprietaryConfig, null, scanMetadata);
    // then: scan contains expected results
    assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE);

    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getMetadata().getCommitHash()).isEqualTo(scanMetadata.getCommitHash());

    assertThat(scan.getItems()).hasSize(3);

    ScanItem item = findScanItem(scan, "sourceControlScan/requirements.txt");
    assertThat(item.getItems()).hasSize(0);
    assertThat(item.isProprietary()).isNull();
    assertThat(item.getContentType()).isEqualTo(ItemContentType.PYTHON_REQUIREMENTS);

    findScanItem(scan, "sourceControlScan/src/test/a.txt");

    findScanItem(scan, "sourceControlScan/testmodule/src/test/b.txt");
  }

  private ScanItem findScanItem(Scan scan, String path) {
    for (ScanItem scanItem : scan.getItems()) {
      if (path.equals(scanItem.getPath())) {
        return scanItem;
      }
    }
    fail("Did not find a scan item with path=" + path);
    return null;
  }

  @Test
  public void testScan_SourceControl_WithScanConfiguration() throws Exception {
    // given: setup and configs what would be used by a client for a source control scan
    File scanDir = new File("src/test/resources/ScannerTest/sourceControlScan");
    ScanConfiguration scanConfiguration = new ScanConfiguration();
    scanConfiguration.setProperty("dirExcludes", "**/src/test");

    // when: perform the scan
    ScanResult scanResult =
        scanner.scan(Collections.singletonList(scanDir), "appId", null /* proprietaryConfig */, scanConfiguration,
            null /* scanMetadata */);

    // then: scan contains expected results
    assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE);

    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan.getItems()).hasSize(1);

    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("sourceControlScan/requirements.txt");
  }

  @Test
  public void testScan_ScanTerraformFile_IacFeatureExists() throws IOException {
    when(featuresService.getFeatures()).thenReturn(ImmutableSet.of(INFRASTRUCTURE_AS_CODE_PACK));
    File terraformFile = new File("src/test/resources/ScannerTest/sample-terraform.tfplan");

    ScanResult scanResult = scanner.scan(terraformFile, "sample-terraform.tfplan", "appId", null);
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE_THIRD_PARTY);

    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan.getItems()).hasSize(1);

    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("sample-terraform.tfplan");
    assertThat(item.getContent()).isEqualTo(readFileToString(terraformFile, UTF_8));
  }

  @Test
  public void testScan_ScanTerraformFile_IacFeatureMissing() throws IOException {
    when(featuresService.getFeatures()).thenReturn(ImmutableSet.of());
    File terraformFile = new File("src/test/resources/ScannerTest/sample-terraform.tfplan");

    ScanResult scanResult = scanner.scan(terraformFile, "sample-terraform.tfplan", "appId", null);
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE);

    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan.getItems()).hasSize(1);

    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("sample-terraform.tfplan");
    assertThat(item.getContent()).isNull();
  }

  @Test
  public void testScan_MustExclude__macosxFolder() throws IOException {
    when(featuresService.getFeatures()).thenReturn(ImmutableSet.of(INFRASTRUCTURE_AS_CODE_PACK));
    // macOS archive utility is adding a bogus folder in zip archives which causes exceptions bit.ly/3zMBiZF
    // This is a zip I created with macOS archive utility that includes the unwanted __MACOS folder in the archive
    File terraformFile = new File("src/test/resources/ScannerTest/aws.large.tfplan.zip");

    ScanResult scanResult = scanner.scan(terraformFile, "aws.large.tfplan.zip", "appId", null);
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE_THIRD_PARTY);

    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    // noinspection unchecked
    List<ScanItem> scanItems = (List<ScanItem>) scan.getItems().get(0).getItems();
    assertThat(scanItems).hasSize(1);
    ScanItem scanItem = scanItems.get(0);
    assertThat(scanItem.getPath()).isEqualTo("aws.large.tfplan");
  }

  @Test
  public void testScan_WithBuiltFromSourceDisabled_ExcludesSha256() throws Exception {
    ScanResult scanResult = scanner.scan(new File("src/test/resources/ScannerTest/app01.zip"), "test-app.zip",
        "appId", null);

    assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE);
    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getItems()).hasSize(1);
    assertThat(scan.getItems().get(0).getSha256()).isNull();
  }

  @Test
  public void testScan_WithBuiltFromSourceEnabled_IncludesSha256() throws Exception {
    try {
      SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);

      ScanResult scanResult = scanner.scan(new File("src/test/resources/ScannerTest/app01.zip"), "test-app.zip",
          "appId", null);

      assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
      assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE);
      Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
      assertThat(scan).isNotNull();
      assertThat(scan.getItems()).hasSize(1);
      assertThat(scan.getItems().get(0).getSha256()).isEqualTo(
          "d564e794805f7fe63e9cb9f51da36ace59a3ea42ecdc8a3a8e91fd5b840cb8b8");
    }
    finally {
      SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(false);
    }
  }

  private void testFilenamePatterns(
      String zipFilename,
      String unzippedFilename,
      ItemContentType itemContentType) throws IOException
  {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Collections.singletonList("com.sonatype"));

    File appFile = new File("src/test/resources/ScannerTest/" + zipFilename);
    ScanResult scanResult = scanner.scan(appFile, "test-app.zip", "appId", proprietaryConfig);
    assertThat(((FileScanEntity) scanResult.getScanEntity()).path()).isRegularFile();
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE_THIRD_PARTY);

    Scan scan = scanReader.read(((FileScanEntity) scanResult.getScanEntity()).path().toFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getItems()).hasSize(1);
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("test-app.zip");
    assertThat(item.getItems()).hasSize(1);
    item = item.getItems().get(0);
    assertThat(item.getPath()).isEqualTo(unzippedFilename);
    assertThat(item.getContentType()).isEqualTo(itemContentType);
  }
}
