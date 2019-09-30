/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

public class ThirdPartyDataServiceTest
    extends AbstractComponentTest
{
  public static final String SCAN_REQUEST_ID = "scan-request-id";

  @Inject
  private ThirdPartyDataService handler;

  private static final String SCAN_ID = "scanId";

  @Test
  public void testGetScanData() {

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "f1", "n1", "v1", "hash1");
    ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "f1", "n2", "v2", "hash2");

    final ThirdPartyCoordinateSecurity sec1coord1 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "desc1", "l1", 5f, null);
    final ThirdPartyCoordinateSecurity sec2coord1 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r2", "desc2", "l2", 1f, null);

    final ThirdPartyCoordinateSecurity sec1coord2 =
        tempEntity.newThirdPartyCoordinateSecurity(coord2, "r3", "desc3", "l3", 3f, null);

    final ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);

    assertThat(scanData.billOfMaterials).hasSize(2);
    assertThat(scanData.securityRows).hasSize(3);

    assertBomContains(scanData.billOfMaterials, coord1, file);
    assertBomContains(scanData.billOfMaterials, coord2, file);
    assertSecurityRowsForComponent(scanData.securityRows, coord1, sec1coord1, sec2coord1);
    assertSecurityRowsForComponent(scanData.securityRows, coord2, sec1coord2);
  }

  @Test
  public void testGetScanData_NoDuplicateComponents_HandlePaths() {
    final ThirdPartyFile file1 = tempEntity.newThirdPartyFile("fh1", "path1");
    final ThirdPartyFile file2 = tempEntity.newThirdPartyFile("fh2", "path2");
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file1);
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file2);
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(file1, "CLAIR", "f1", "n1", "v1", "hash1");
    ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(file2, "CLAIR", "f1", "n1", "v1", "hash1");

    final ThirdPartyCoordinateSecurity sec1coord1 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "desc1", "l1", 5f, null);
    tempEntity.newThirdPartyCoordinateSecurity(coord2, "r1", "desc1", "l1", 5f, null);

    final ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);

    assertThat(scanData.billOfMaterials).hasSize(1);
    assertThat(scanData.securityRows).hasSize(1);

    assertBomContains(scanData.billOfMaterials, coord1, file1, file2);
    assertSecurityRowsForComponent(scanData.securityRows, coord1, sec1coord1);
  }

  @Test
  public void testGetScanData_NoData() {
    ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);
    assertThat(scanData).isNull();
  }

  @Test
  public void testGetScanData_ScanExists_NoCoordinates() {
    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);
    assertThat(scanData).isNotNull();
    assertThat(scanData.billOfMaterials).hasSize(0);
    assertThat(scanData.securityRows).hasSize(0);
  }

  private void assertSecurityRowsForComponent(
      final List<ThirdPartyHealthCheckReportSecurityRowDTO> securityRows,
      final ThirdPartyFileCoordinate coordinate,
      final ThirdPartyCoordinateSecurity... expectedSecRows)
  {
    final List<ThirdPartyHealthCheckReportSecurityRowDTO> found =
        securityRows.stream().filter(sec -> sec.hash.equals(coordinate.getHash())).collect(Collectors.toList());
    assertThat(found).hasSize(expectedSecRows.length);

    for (ThirdPartyCoordinateSecurity expectedSecRow : expectedSecRows) {
      assertThat(found.stream().filter(sec -> sec.reference.equals(expectedSecRow.getRefId())).findFirst())
          .hasValueSatisfying(securityRow -> {
            assertThat(securityRow.componentIdentifier).isEqualTo(componentIdentifierOf(coordinate));
            assertThat(securityRow.matchState).isEqualTo(MatchState.EXACT.toString());
            assertThat(securityRow.description).isEqualTo(expectedSecRow.getDescription());
            assertThat(securityRow.score).isEqualTo(expectedSecRow.getSeverity());
            assertThat(securityRow.url).isEqualTo(expectedSecRow.getLink());
            assertThat(securityRow.fixedVersion).isEqualTo(expectedSecRow.getFixedBy());
          });
    }
  }

  private void assertBomContains(
      final List<ThirdPartyBillOfMaterialsRowDTO> bom,
      final ThirdPartyFileCoordinate coordinate,
      final ThirdPartyFile... files)
  {
    assertThat(
        bom.stream().filter(component -> component.hash.equals(coordinate.getHash())).findFirst())
        .hasValueSatisfying(bomRow -> {
          assertThat(bomRow.componentIdentifier).isEqualTo(componentIdentifierOf(coordinate));
          assertThat(bomRow.createTime).isCloseTo(files[0].getCreated().getTime(), withinPercentage(0.001));
          assertThat(bomRow.matchState).isEqualTo(MatchState.EXACT.toString());
          assertThat(bomRow.identificationSource).isEqualTo(coordinate.getSource());
        });
  }

  private ComponentIdentifier componentIdentifierOf(final ThirdPartyFileCoordinate coordinate) {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("name", coordinate.getName());
    coordinates.put(VERSION, coordinate.getVersion());
    return new ComponentIdentifier(coordinate.getFormat(), coordinates);
  }
}
