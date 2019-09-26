/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.application.BillOfMaterialsRowDTO;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @since 1.72
 */
public class ComponentResolverTest
    extends AbstractComponentTest
{
  @Inject
  private ComponentResolver componentResolver;

  @Mock
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Mock
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Mock
  private ThirdPartyScanDAO thirdPartyScanDAO;

  private ThirdPartyFileCoordinate coordinate1;

  private ThirdPartyFileCoordinate coordinate2;

  private ThirdPartyScan scan;

  private ThirdPartyCoordinateSecurity security;

  @Override
  public void configure(Binder binder) {
    binder.bind(ThirdPartyFileCoordinateDAO.class).toInstance(thirdPartyFileCoordinateDAO);
    binder.bind(ThirdPartyCoordinateSecurityDAO.class).toInstance(thirdPartyCoordinateSecurityDAO);
    binder.bind(ThirdPartyScanDAO.class).toInstance(thirdPartyScanDAO);
  }

  @Test
  public void testGetComponents_thirdPartyScanClair() throws Exception {
    Path path = Paths.get(getClass().getResource("/ComponentEvaluatorTest/TestThirdPartyClairBom.json").toURI());
    byte[] bomData = Files.readAllBytes(path);
    Application app1 = tempEntity.newApplicationWithParent("app1");
    String scanId = tempEntity.uuid();
    mockThirdPartyScanDAO(scanId);

    List<Component> components = componentResolver.getComponents(app1, null, null, bomData, scanId);
    assertMatchedComponents(components);
  }

  @Test
  public void testGetComponents_noThirdPartyScanClairData() throws Exception {
    Path path = Paths.get(getClass().getResource("/ComponentEvaluatorTest/TestThirdPartyClairBom.json").toURI());
    byte[] bomData = Files.readAllBytes(path);
    Application app1 = tempEntity.newApplicationWithParent("app1");
    String scanId = tempEntity.uuid();

    when(thirdPartyScanDAO.getByScanId(scanId)).thenReturn(Collections.emptyList());

    List<Component> components = componentResolver.getComponents(app1, null, null, bomData, scanId);
    assertThat(components).hasSize(2);
    components.forEach(component -> {
      assertUnknownComponent(component);
    });
  }

  @Test
  public void testIdentifyThirdPartyComponents_sameVulnerabilityDoesNotRepeat() {
    final Component component = unknownComponent("hash1");
    List<Component> components = asList(component);
    final String scanId = "scan-id";
    mockThirdPartyScan(scanId);
    List<ThirdPartyFileCoordinate> fileCoordinates =
        asList(createThirdPartyFileCoordinate("hash1", "deb", "fcid1", "name1", "f1", "v1"));
    when(thirdPartyFileCoordinateDAO.getByHashAndScanId("hash1", scanId)).thenReturn(fileCoordinates);

    final ThirdPartyCoordinateSecurity sec1 =
        createCoordinateSecurity("f1", "fb1", "REF1", 7);
    final ThirdPartyCoordinateSecurity sec2 =
        createCoordinateSecurity("f1", "fb2", "FEF2", 10);
    //edge case for an identical sec vulnerability for the same component, that should not repeat
    final ThirdPartyCoordinateSecurity sec2identical =
        createCoordinateSecurity("f1", "fb2", "FEF2", 10);
    final List<ThirdPartyCoordinateSecurity> securityVulnerabilities =
        asList(sec1, sec2, sec2identical);

    when(thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(asList("fcid1"))).thenReturn(securityVulnerabilities);

    final ThirdPartyApplicationReportDTO dto =
        componentResolver.identifyThirdPartyComponents(components, scanId);

    assertThat(dto.billOfMaterials).hasSize(1);
    assertThat(dto.securityRows).hasSize(2);
    assertBomRowsContain(dto.billOfMaterials, component);
    assertSecurityRowsContain(dto.securityRows, component, sec1);
    assertSecurityRowsContain(dto.securityRows, component, sec2);
  }

  @Test
  public void testIdentifyThirdPartyComponents_MatchedComponentsNoVulnerabilities() {
    final Component component = unknownComponent("hash1");
    List<Component> components = asList(component);
    final String scanId = "scan-id";
    mockThirdPartyScan(scanId);
    List<ThirdPartyFileCoordinate> fileCoordinates =
        asList(createThirdPartyFileCoordinate("hash1", "deb", "fcid1", "name1", "f1", "v1"));
    when(thirdPartyFileCoordinateDAO.getByHashAndScanId("hash1", scanId)).thenReturn(fileCoordinates);
    when(thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(asList("fcid1"))).thenReturn(Collections.emptyList());

    final ThirdPartyApplicationReportDTO dto =
        componentResolver.identifyThirdPartyComponents(components, scanId);

    assertThat(dto.billOfMaterials).hasSize(1);
    assertThat(dto.securityRows).hasSize(0);
  }

  @Test
  public void testIdentifyThirdPartyComponents_NoMatchingThirdPartyComponentsFound() {
    final Component component = unknownComponent("hash1");
    List<Component> components = asList(component);
    final String scanId = "scan-id";
    mockThirdPartyScan(scanId);
    when(thirdPartyFileCoordinateDAO.getByHashAndScanId("hash1", scanId)).thenReturn(Collections.emptyList());

    final ThirdPartyApplicationReportDTO dto =
        componentResolver.identifyThirdPartyComponents(components, scanId);

    assertThat(dto.billOfMaterials).hasSize(0);
    assertThat(dto.securityRows).hasSize(0);
  }

  @Test
  public void testIdentifyThirdPartyComponents_NoThirdPartyDataForScanId() {
    final Component component = unknownComponent("hash1");
    List<Component> components = asList(component);
    when(thirdPartyScanDAO.getByScanId("scan-id")).thenReturn(Collections.emptyList());

    final ThirdPartyApplicationReportDTO dto =
        componentResolver.identifyThirdPartyComponents(components, "scan-id");

    assertThat(dto).isNull();
  }

  private Component unknownComponent(String hash) {
    final Component component = new Component();
    component.setHash(hash);
    component.setMatchState(MatchState.UNKNOWN);
    component.addPathname("path1");
    component.addPathname("path2");
    return component;
  }

  private void assertMatchedComponents(List<Component> components) {
    assertThat(components).hasSize(2);
    Consumer<Component> action = component -> {
      if (component.getMatchState() == MatchState.EXACT
          && component.getIdentificationSource() == IdentificationSource.CLAIR) {
        assertClairComponent(component);
      }
      else {
        assertUnknownComponent(component);
      }

    };
    components.forEach(action);
  }

  private void assertClairComponent(Component component) {
    ComponentIdentifier ci = component.getComponentIdentifier();
    assertThat(ci).isNotNull();
    assertThat(ci.getFormat()).isEqualTo(coordinate1.getFormat());
    assertThat(ci.getCoordinates()).hasSize(2);
    assertCoordinates(ci.getCoordinates());

    List<SecurityVulnerability> vulnerabilities = component.getSecurityVulnerabilities();
    assertThat(vulnerabilities).hasSize(1);
    assertThat(vulnerabilities.get(0).getRefId()).isEqualTo(security.getRefId());
    assertThat(vulnerabilities.get(0).getSeverity()).isEqualTo(security.getSeverity());
    assertThat(vulnerabilities.get(0).getSource()).isEqualTo(coordinate1.getSource());
    assertThat(vulnerabilities.get(0).getStatus()).isEqualTo(SecurityVulnerabilityOverrideStatus.OPEN);
    assertThat(vulnerabilities.get(0).getUrl()).isEqualTo(security.getLink());
  }

  private void assertCoordinates(SortedMap<String, String> coordinates) {
    assertThat(coordinates.get("version")).isEqualTo(coordinate1.getVersion());
    assertThat(coordinates.get("name")).isEqualTo(coordinate1.getName());
  }

  private void assertUnknownComponent(Component component) {
    assertThat(component.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE);
    assertThat(component.getMatchState()).isEqualTo(MatchState.UNKNOWN);
    assertThat(component.getComponentIdentifier()).isNull();
    assertThat(component.getSecurityVulnerabilities()).isEmpty();
  }

  private void mockThirdPartyScanDAO(String scanId) {
    mockThirdPartyScan(scanId);
    mockCoordinateFile(scanId);
    mockSecurityCoordinate();

  }

  private void mockThirdPartyScan(String scanId) {
    scan = new ThirdPartyScan();
    scan.setId("6ecfe1df66fe43eb9d6bddee97be6f8c");
    scan.setThirdPartyFileId("0646b0f15c7c4588b004dd8cd70fd2e3");
    scan.setScanId(scanId);

    List<ThirdPartyScan> list = new ArrayList<>();
    list.add(scan);

    when(thirdPartyScanDAO.getByScanId(scanId)).thenReturn(list);
  }

  private void mockCoordinateFile(String scanId) {

    String hash = "e587ce87ed894c1d5283";
    List<ThirdPartyFileCoordinate> list = new ArrayList<>();
    coordinate1 =
        createThirdPartyFileCoordinate(hash, "deb", "6ecfe1df66fe43eb9d6bddee97be6f8c", "gclib", "f1",
            "2.24-11+deb9u3");
    coordinate2 =
        createThirdPartyFileCoordinate(hash, "deb", "6ecfe1df66fe43eb9d6bddee97be6f8c", "gclib", "f2",
            "2.24-11+deb9u3");
    list.add(coordinate1);
    list.add(coordinate2);

    when(thirdPartyFileCoordinateDAO.getByHashAndScanId(hash, scanId)).thenReturn(list);
  }

  private ThirdPartyFileCoordinate createThirdPartyFileCoordinate(
      final String hash,
      final String format,
      final String id, final String name, final String fileId, final String version)
  {
    ThirdPartyFileCoordinate coordinate = new ThirdPartyFileCoordinate();
    coordinate.setFormat(format);
    coordinate.setHash(hash);
    coordinate.setId(id);
    coordinate.setName(name);
    coordinate.setSource(IdentificationSource.CLAIR.getId());
    coordinate.setThirdPartyFileId(fileId);
    coordinate.setVersion(version);
    return coordinate;
  }

  private void mockSecurityCoordinate() {
    List<ThirdPartyCoordinateSecurity> securityList = new ArrayList<>();
    security = createCoordinateSecurity(coordinate1.getThirdPartyFileId(), "2.24-11+deb9u4", "CVE-2017-16997", 10);
    securityList.add(security);
    ThirdPartyCoordinateSecurity security2 =
        createCoordinateSecurity(coordinate2.getThirdPartyFileId(), "2.24-11+deb9u4", "CVE-2017-16997", 10);
    securityList.add(security2);

    List<String> coordinateIdList =
        Stream.of(coordinate1.getId(), coordinate2.getId()).collect(Collectors.toList());
    when(thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(coordinateIdList)).thenReturn(securityList);
  }

  private ThirdPartyCoordinateSecurity createCoordinateSecurity(
      final String thirdPartyFileId,
      final String fixedBy, final String refId, final int severity)
  {
    security = new ThirdPartyCoordinateSecurity();
    security.setRefId(refId);
    security.setLink("https://security-tracker.debian.org/tracker/" + refId);
    security.setSeverity(severity);
    security.setFixedBy(fixedBy);
    security.setFileCoordinateId(thirdPartyFileId);
    security.setDescription("description");
    return security;
  }

  private void assertBomRowsContain(final List<BillOfMaterialsRowDTO> boms, final Component component) {
    final Optional<BillOfMaterialsRowDTO> bomMaybe =
        boms.stream().filter(bom -> bom.hash.equals(component.getHash())).findFirst();

    assertThat(bomMaybe.isPresent()).isTrue();
    final BillOfMaterialsRowDTO bom = bomMaybe.get();
    assertThat(bom.createTime).isEqualTo(component.getCatalogDate());
    assertThat(bom.componentIdentifier).isEqualTo(component.getComponentIdentifier());
    assertThat(bom.matchState).isEqualTo(component.getMatchState().getName());
    assertThat(bom.pathnames).containsExactlyInAnyOrderElementsOf(component.getPathnames());
    assertThat(bom.proprietary).isEqualTo(component.isProprietary());
  }

  private void assertSecurityRowsContain(
      final List<ThirdPartyHealthCheckReportSecurityRowDTO> dtoList,
      final Component component,
      final ThirdPartyCoordinateSecurity thirdPartySecurity)
  {
    final Optional<ThirdPartyHealthCheckReportSecurityRowDTO> dtoMaybe =
        dtoList.stream().filter(d -> d.reference.equals(thirdPartySecurity.getRefId())).findFirst();

    assertThat(dtoMaybe.isPresent()).isTrue();
    final ThirdPartyHealthCheckReportSecurityRowDTO dto = dtoMaybe.get();
    assertThat(dto.hash).isEqualTo(component.getHash());
    assertThat(dto.matchState).isEqualTo(component.getMatchState().getName());
    assertThat(dto.componentIdentifier).isEqualTo(component.getComponentIdentifier());
    assertThat(dto.proprietary).isEqualTo(component.isProprietary());
    assertThat(dto.source).isEqualTo(component.getIdentificationSource().getName());
    assertThat(dto.description).isEqualTo(thirdPartySecurity.getDescription());
    assertThat(dto.url).isEqualTo(thirdPartySecurity.getLink());
    assertThat(dto.score).isEqualTo(thirdPartySecurity.getSeverity());
    assertThat(dto.fixedVersion).isEqualTo(thirdPartySecurity.getFixedBy());
  }
}
