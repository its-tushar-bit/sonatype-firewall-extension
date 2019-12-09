/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.thirdparty.ThirdPartyBillOfMaterialsRowDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyReportComponentDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to get the components from HDS result and add information about third party scanner if necessary
 *
 * @since 1.72
 */
public class ComponentResolver
{
  private final ComponentDAO componentDAO;

  private final ThirdPartyComponentDAO thirdPartyComponentDAO;

  private static final Logger log = LoggerFactory.getLogger(ComponentResolver.class);

  @Inject
  public ComponentResolver(final ComponentDAO componentDAO, final ThirdPartyComponentDAO thirdPartyComponentDAO) {
    this.componentDAO = componentDAO;
    this.thirdPartyComponentDAO = thirdPartyComponentDAO;
  }

  public List<Component> getComponents(
      Application application,
      final byte[] licenseData,
      final byte[] securityData,
      final byte[] bomData,
      final File reportFile)
  {
    // Load data about components
    final List<Component> components = componentDAO.getAll(application, licenseData, securityData, bomData);
    identifyThirdPartyComponents(components, reportFile);
    return components;
  }

  private void identifyThirdPartyComponents(
      final List<Component> components,
      final File reportFile)
  {
    final Map<String, ThirdPartyReportComponentDTO> data = thirdPartyComponentDAO.getData(reportFile);
    if (data != null && !data.isEmpty()) {
      List<ThirdPartyBillOfMaterialsRowDTO> thirdPartyIdentifiedComponents = new ArrayList<>();
      components.stream().filter(c -> MatchState.UNKNOWN.equals(c.getMatchState())).forEach(unknownComponent -> {
        final ThirdPartyReportComponentDTO thirdPartyDTO = data.get(unknownComponent.getHash());
        if (thirdPartyDTO != null) {
          populateThirdPartyData(unknownComponent, thirdPartyDTO);
          thirdPartyIdentifiedComponents.add(thirdPartyDTO.bomRow);
        }
      });
      thirdPartyComponentDAO.applyIdentifiedComponentUpdates(thirdPartyIdentifiedComponents, reportFile);
    }
  }

  private void populateThirdPartyData(final Component component, final ThirdPartyReportComponentDTO thirdPartyDTO) {
    log.debug("Matched third party dependency {} with surrogate hash {}", thirdPartyDTO.bomRow.componentIdentifier,
        component.getHash());
    component.setComponentIdentifier(thirdPartyDTO.componentIdentifier);
    component.setMatchState(MatchState.EXACT);
    component.setIdentificationSource(IdentificationSource.getOrMake(thirdPartyDTO.bomRow.identificationSource));
    component.setSecurityVulnerabilities(
        thirdPartyDTO.securityRows.stream().map(this::toSecurityVulnerability).collect(Collectors.toList()));
    component.setDeclaredLicenseIds(
        thirdPartyDTO.licensesRow.declaredLicenses.stream().map(license -> license.id).collect(Collectors.toSet()));
  }

  private SecurityVulnerability toSecurityVulnerability(
      final ThirdPartyHealthCheckReportSecurityRowDTO secRow)
  {
    SecurityVulnerability securityVulnerability =
        new SecurityVulnerability(secRow.source, secRow.reference, secRow.score);
    securityVulnerability.setUrl(secRow.url);
    return securityVulnerability;
  }
}
