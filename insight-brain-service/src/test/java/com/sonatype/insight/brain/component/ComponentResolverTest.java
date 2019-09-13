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

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

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
    coordinate1 = new ThirdPartyFileCoordinate();
    coordinate1.setFormat("deb");
    coordinate1.setHash(hash);
    coordinate1.setId("6ecfe1df66fe43eb9d6bddee97be6f8c");
    coordinate1.setName("gclib");
    coordinate1.setSource(IdentificationSource.CLAIR.getId());
    coordinate1.setThirdPartyFileId("f1");
    coordinate1.setVersion("2.24-11+deb9u3");
    list.add(coordinate1);

    coordinate2 = new ThirdPartyFileCoordinate();
    coordinate2.setFormat("deb");
    coordinate2.setHash(hash);
    coordinate2.setId("6ecfe1df66fe43eb9d6bddee97be6f8c");
    coordinate2.setName("gclib");
    coordinate2.setSource(IdentificationSource.CLAIR.getId());
    coordinate2.setThirdPartyFileId("f2");
    coordinate2.setVersion("2.24-11+deb9u3");
    list.add(coordinate2);

    when(thirdPartyFileCoordinateDAO.getByHashAndScanId(hash, scanId)).thenReturn(list);
  }

  private void mockSecurityCoordinate() {
    List<ThirdPartyCoordinateSecurity> securityList = new ArrayList<>();
    security = new ThirdPartyCoordinateSecurity();
    security.setRefId("CVE-2017-16997");
    security.setLink("https://security-tracker.debian.org/tracker/CVE-2017-16997");
    security.setSeverity(10);
    security.setFixedBy("2.24-11+deb9u4");
    security.setFileCoordinateId(coordinate1.getThirdPartyFileId());
    securityList.add(security);

    ThirdPartyCoordinateSecurity security2 = new ThirdPartyCoordinateSecurity();
    security2.setRefId("CVE-2017-16997");
    security2.setLink("https://security-tracker.debian.org/tracker/CVE-2017-16997");
    security2.setSeverity(10);
    security2.setFixedBy("2.24-11+deb9u4");
    security2.setFileCoordinateId(coordinate2.getThirdPartyFileId());
    securityList.add(security2);

    List<String> coordinateIdList =
        Stream.of(coordinate1.getId(), coordinate2.getId()).collect(Collectors.toList());
    when(thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(coordinateIdList)).thenReturn(securityList);
  }
}
