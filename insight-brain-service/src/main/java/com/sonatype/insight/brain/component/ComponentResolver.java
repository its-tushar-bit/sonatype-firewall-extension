/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to get the components from HDS result and add information about third party scanner if necessary
 *
 * @since 1.72
 */
public class ComponentResolver
{
  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ComponentDAO componentDAO;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private static final Logger log = LoggerFactory.getLogger(ComponentResolver.class);

  @Inject
  public ComponentResolver(
      ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      ComponentDAO componentDAO,
      ThirdPartyScanDAO thirdPartyScanDAO)
  {
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.componentDAO = componentDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
  }

  public List<Component> getComponents(
      Application application,
      final byte[] licenseData,
      final byte[] securityData,
      final byte[] bomData,
      final String scanId)
  {
    // Load data about components
    final List<Component> components = componentDAO.getAll(application, licenseData, securityData, bomData);
    identifyThirdPartyComponents(components, scanId);
    return components;
  }

  private void identifyThirdPartyComponents(final List<Component> components, String scanId) {
    List<ThirdPartyScan> thirdPartyScanList = thirdPartyScanDAO.getByScanId(scanId);
    if (!thirdPartyScanList.isEmpty()) {
      log.debug("Found {} third party scan data files for scanId {}", thirdPartyScanList.size(), scanId);
      components.stream().filter(component -> component.getMatchState() == MatchState.UNKNOWN)
          .forEach(component -> matchWithThirdPartyScanMatching(scanId, component));
    }
  }

  private void matchWithThirdPartyScanMatching(String scanId, Component component) {
    List<ThirdPartyFileCoordinate> thirdPartyFileCoordinateList =
        thirdPartyFileCoordinateDAO.getByHashAndScanId(component.getHash(), scanId);

    if (!thirdPartyFileCoordinateList.isEmpty()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = thirdPartyFileCoordinateList.get(0);
      component.setMatchState(MatchState.EXACT);
      component.setIdentificationSource(IdentificationSource.getById(thirdPartyFileCoordinate.getSource()));
      component.setComponentIdentifier(ComponentIdentifierAdapter.createGenericIdentifier(thirdPartyFileCoordinate));
      addSecurityVulnerabilities(thirdPartyFileCoordinateList, component);
      log.debug("Matched third party dependency {}-{} with surrogate hash {}", component.getDisplayName(),
          component.getVersion(), component.getHash());
    }
  }

  private void addSecurityVulnerabilities(
      List<ThirdPartyFileCoordinate> thirdPartyFileCoordinateHashList,
      Component component)
  {
    List<String> listId =
        thirdPartyFileCoordinateHashList.stream().map(ThirdPartyFileCoordinate::getId).collect(Collectors.toList());

    List<ThirdPartyCoordinateSecurity> securityList = thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(listId);

    securityList
        .forEach(thirdPartyCoordinateSecurity -> addSecurityVulnerability(thirdPartyCoordinateSecurity, component));
  }

  private void addSecurityVulnerability(
      ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity,
      Component component)
  {
    SecurityVulnerability securityVulnerability = new SecurityVulnerability(component.getIdentificationSource().getId(),
        thirdPartyCoordinateSecurity.getRefId(), thirdPartyCoordinateSecurity.getSeverity());
    boolean vulnerabilityNotExists =
        component.getSecurityVulnerabilities().stream().map(SecurityVulnerability::getRefId)
        .noneMatch(securityVulnerability.getRefId()::equals);
    if (vulnerabilityNotExists) {
      securityVulnerability.setUrl(thirdPartyCoordinateSecurity.getLink());
      component.addSecurityVulnerability(securityVulnerability);
    }
  }
}
