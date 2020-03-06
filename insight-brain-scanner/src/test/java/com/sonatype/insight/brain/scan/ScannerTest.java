/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.io.ScanReader;

import org.apache.commons.io.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ScannerTest extends InjectedTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Inject
  private Scanner scanner;

  @Inject
  private ScanReader scanReader;

  @Test
  public void testScan() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Collections.singletonList("com.sonatype"));

    File appFile = new File("src/test/resources/ScannerTest/app01.zip");
    ScanResult scanResult = scanner.scan(appFile, "test-app.zip", new File(tempDir.getRoot(), "not-yet-existent"),
        proprietaryConfig);
    assertThat(scanResult.getScanFile()).isFile();

    Scan scan = scanReader.read(scanResult.getScanFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getItems()).hasSize(1);
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("test-app.zip");
    assertThat(item.getItems()).hasSize(1);
    item = item.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("proprietary.jar");
    assertThat(item.getItems()).hasSize(1);
    assertThat(item.isProprietary()).isNull();
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
  public void testScanWithProprietaryRegex() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setRegexes(Collections.singletonList(".*prop.*\\.jar"));

    File appFile = new File("src/test/resources/ScannerTest/app01.zip");
    ScanResult scanResult = scanner.scan(appFile, "test-app.zip", new File(tempDir.getRoot(), "not-yet-existent"),
        proprietaryConfig);
    assertThat(scanResult.getScanFile()).isFile();

    Scan scan = scanReader.read(scanResult.getScanFile());
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
  public void testScan_sbomFile() throws Exception {
    String sbom =
        FileUtils.readFileToString(new File("src/test/resources/ScannerTest/iq-scan-sbom.xml"), StandardCharsets.UTF_8)
            .replace("\r\n", "\n");

    String scannerDriver = "thirdPartyApiTest";
    ScanResult scanResult = scanner.scanContent(sbom, new File(tempDir.getRoot(), "sbom"), ItemContentType.SBOM, "ABCD",
        null, scannerDriver);
    assertThat(scanResult.getScanFile()).isFile();
    assertThat(scanResult.hasThirdPartyScanContent()).isTrue();
    Scan scan = scanReader.read(scanResult.getScanFile());
    assertThat(scan).isNotNull();
    assertThat(scan.getSummary().getScannerDriver()).isEqualTo(scannerDriver);
    assertThat(scan.getSummary().getClientInfo().get("insight.scannerDriver")).isEqualTo(scannerDriver);
    assertThat(scan.getItems()).hasSize(1);
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath()).isEqualTo("ABCD-bom.xml");
    assertThat(item.getItems()).hasSize(0);
    assertThat(item.getContentType()).isEqualTo(ItemContentType.SBOM);

    assertThat(item.getSha1()).isEqualTo("395849f1c53dac640ca8");
  }
}
