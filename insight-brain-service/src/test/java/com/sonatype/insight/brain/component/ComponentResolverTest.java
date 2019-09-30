/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.thirdparty.ThirdPartyBillOfMaterialsRowDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyReportComponentDTO;

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
  private ThirdPartyComponentDAO thirdPartyComponentDAO;

  @Mock
  private ComponentDAO componentDAO;

  @Mock
  private Application app;

  @Mock
  private File reportFile;

  @Override
  public void configure(Binder binder) {
    binder.bind(ThirdPartyComponentDAO.class).toInstance(thirdPartyComponentDAO);
    binder.bind(ComponentDAO.class).toInstance(componentDAO);
  }

  @Test
  public void testGetComponents_MatchedThirdPartyDataWithVulnerabilities() {
    when(componentDAO.getAll(app, null, null, null))
        .thenReturn(asList(newUnknownComponent("hash1"), newUnknownComponent("hash2")));

    Map<String, ThirdPartyReportComponentDTO> reportDto = new HashMap<>();
    reportDto.put("hash1", thirdPartyDTO("hash1", "ref1", "ref2"));
    reportDto.put("hash2", thirdPartyDTO("hash2", "ref3"));
    when(thirdPartyComponentDAO.getData(reportFile)).thenReturn(reportDto);

    List<Component> components =
        componentResolver.getComponents(app, null, null, null, reportFile);

    assertMatchedComponents(components, reportDto);
  }

  @Test
  public void testGetComponents_DoNotOverrideKnownComponents() {
    final Component knownComponent = newKnownComponent("known");
    when(componentDAO.getAll(app, null, null, null))
        .thenReturn(Collections.singletonList(knownComponent));

    Map<String, ThirdPartyReportComponentDTO> reportDto = new HashMap<>();
    reportDto.put("known", thirdPartyDTO("known", "ref1", "ref2"));
    when(thirdPartyComponentDAO.getData(reportFile)).thenReturn(reportDto);

    List<Component> components =
        componentResolver.getComponents(app, null, null, null, reportFile);

    assertThat(components).hasSize(1);
    assertThat(components.get(0)).satisfies(component -> {
      assertThat(component.getMatchState()).isEqualTo(knownComponent.getMatchState());
      assertThat(component.getComponentIdentifier()).isEqualTo(knownComponent.getComponentIdentifier());
      assertThat(component.getRelativePopularity()).isEqualTo(knownComponent.getRelativePopularity());
      assertThat(component.getPathnames()).containsExactlyElementsOf(knownComponent.getPathnames());

      final List<SecurityVulnerability> vulnerabilities = component.getSecurityVulnerabilities();
      assertThat(vulnerabilities).hasSize(1);
      assertThat(vulnerabilities.get(0)).satisfies(v -> {
        final SecurityVulnerability knownComponentVuln = knownComponent.getSecurityVulnerabilities().get(0);
        assertThat(v.getRefId()).isEqualTo(knownComponentVuln.getRefId());
        assertThat(v.getUrl()).isEqualTo(knownComponentVuln.getUrl());
        assertThat(v.getSource()).isEqualTo(knownComponentVuln.getSource());
        assertThat(v.getSeverity()).isEqualTo(knownComponentVuln.getSeverity());
      });
    });
  }

  @Test
  public void testGetComponents_NoMatchingThirdPartyData() {
    when(componentDAO.getAll(app, null, null, null))
        .thenReturn(asList(newUnknownComponent("hash1"), newUnknownComponent("hash2")));
    when(thirdPartyComponentDAO.getData(reportFile)).thenReturn(Collections.emptyMap());

    List<Component> components = componentResolver.getComponents(app, null, null, null, reportFile);

    assertThat(components).hasSize(2);
    components.forEach(component -> {
      assertUnknownComponent(component);
    });
  }

  @Test
  public void testIdentifyThirdPartyComponents_MatchedThirdPartyDataWithoutVulnerabilities() {
    when(componentDAO.getAll(app, null, null, null)).thenReturn(asList(newUnknownComponent("hash1")));
    Map<String, ThirdPartyReportComponentDTO> reportDto = new HashMap<>();
    reportDto.put("hash1", thirdPartyDTO("hash1"));
    when(thirdPartyComponentDAO.getData(reportFile)).thenReturn(reportDto);

    List<Component> components = componentResolver.getComponents(app, null, null, null, reportFile);

    assertMatchedComponents(components, reportDto);
  }

  private void assertMatchedComponents(
      final List<Component> components,
      final Map<String, ThirdPartyReportComponentDTO> reportDto)
  {
    for (Component component : components) {
      ThirdPartyReportComponentDTO expectedMatch = reportDto.get(component.getHash());
      assertThat(component.getMatchState()).isEqualTo(MatchState.EXACT);
      assertThat(component.getComponentIdentifier()).isEqualTo(expectedMatch.componentIdentifier);
      assertThat(component.getIdentificationSource())
          .isEqualTo(IdentificationSource.getById(expectedMatch.bomRow.identificationSource));
      assertMatchedSecurityRows(component.getSecurityVulnerabilities(), expectedMatch.securityRows);
    }
  }

  private void assertMatchedSecurityRows(
      final List<SecurityVulnerability> securityVulnerabilities,
      final List<ThirdPartyHealthCheckReportSecurityRowDTO> expectedRows)
  {
    assertThat(securityVulnerabilities).hasSize(expectedRows.size());
    securityVulnerabilities.forEach(sec -> {
      assertThat(expectedRows.stream().filter(row -> sec.getRefId().equals(row.reference)).findFirst())
          .hasValueSatisfying(matchedRow -> {
            assertThat(sec.getSeverity()).isEqualTo(matchedRow.score);
            assertThat(sec.getSource()).isEqualTo(matchedRow.source);
            assertThat(sec.getUrl()).isEqualTo(matchedRow.url);
          });
    });
  }

  private ThirdPartyReportComponentDTO thirdPartyDTO(final String hash, String... refs) {
    ThirdPartyBillOfMaterialsRowDTO bomRow =
        new ThirdPartyBillOfMaterialsRowDTO(ComponentIdentifier.createNpmCoordinates("id1", "v1"), hash);
    bomRow.identificationSource = IdentificationSource.CLAIR.getName();
    final ThirdPartyReportComponentDTO dto = new ThirdPartyReportComponentDTO(bomRow);
    for (String ref : refs) {
      ThirdPartyHealthCheckReportSecurityRowDTO secDto = newSecurityRowDTO(hash, bomRow.componentIdentifier, ref);
      dto.securityRows.add(secDto);
    }
    return dto;
  }

  private ThirdPartyHealthCheckReportSecurityRowDTO newSecurityRowDTO(
      final String hash,
      final ComponentIdentifier componentIdentifier,
      final String ref)
  {
    final ThirdPartyHealthCheckReportSecurityRowDTO dto =
        new ThirdPartyHealthCheckReportSecurityRowDTO(componentIdentifier, hash);
    dto.reference = ref;
    dto.url = "url-" + ref;
    dto.matchState = MatchState.EXACT.toString();
    dto.score = 1.1f;
    return dto;
  }

  private Component newUnknownComponent(String hash) {
    final Component component = new Component();
    component.setHash(hash);
    component.setMatchState(MatchState.UNKNOWN);
    component.addPathname("path1");
    component.addPathname("path2");
    return component;
  }

  private Component newKnownComponent(final String hash) {
    final Component component = new Component();
    component.setHash(hash);
    component.setMatchState(MatchState.EXACT);
    component.setComponentIdentifier(ComponentIdentifier.createRpmCoordinates("n1", "v1", "arch1"));
    component.setIdentificationSource(IdentificationSource.SONATYPE);
    component.addPathname("path10");
    component.setRelativePopularity(10);
    final SecurityVulnerability vuln = new SecurityVulnerability("Mitre", "CVE-100", 7.0f);
    vuln.setUrl("some-url/CVE-100");
    component
        .setSecurityVulnerabilities(Collections.singletonList(vuln));
    return component;
  }

  private void assertUnknownComponent(Component component) {
    assertThat(component.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE);
    assertThat(component.getMatchState()).isEqualTo(MatchState.UNKNOWN);
    assertThat(component.getComponentIdentifier()).isNull();
    assertThat(component.getSecurityVulnerabilities()).isEmpty();
  }
}
