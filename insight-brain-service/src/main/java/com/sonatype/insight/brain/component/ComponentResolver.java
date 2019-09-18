/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.HashSet;
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
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.application.BillOfMaterialsRowDTO;

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
    //TODO: result of identifyThirdPartyComponents will be used for persistence in CLM-13441
    return components;
  }

  // visible for testing
  ThirdPartyApplicationReportDTO identifyThirdPartyComponents(final List<Component> components, String scanId) {
    List<ThirdPartyScan> thirdPartyScanList = thirdPartyScanDAO.getByScanId(scanId);
    if (!thirdPartyScanList.isEmpty()) {
      final ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO = new ThirdPartyApplicationReportDTO();
      log.debug("Found {} third party scan data files for scanId {}", thirdPartyScanList.size(), scanId);
      components.stream().filter(component -> component.getMatchState() == MatchState.UNKNOWN)
          .forEach(component -> matchWithThirdPartyScanMatching(scanId, component, thirdPartyApplicationReportDTO));
      return thirdPartyApplicationReportDTO;
    }
    return null;
  }

  private void matchWithThirdPartyScanMatching(
      String scanId,
      Component component,
      ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO)
  {
    List<ThirdPartyFileCoordinate> thirdPartyFileCoordinateList =
        thirdPartyFileCoordinateDAO.getByHashAndScanId(component.getHash(), scanId);

    if (!thirdPartyFileCoordinateList.isEmpty()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = thirdPartyFileCoordinateList.get(0);
      component.setMatchState(MatchState.EXACT);
      component.setIdentificationSource(IdentificationSource.getById(thirdPartyFileCoordinate.getSource()));
      component.setComponentIdentifier(ComponentIdentifierAdapter.createGenericIdentifier(thirdPartyFileCoordinate));
      addSecurityVulnerabilities(thirdPartyFileCoordinateList, component, thirdPartyApplicationReportDTO);
      log.debug("Matched third party dependency {}-{} with surrogate hash {}", component.getDisplayName(),
          component.getVersion(), component.getHash());
      thirdPartyApplicationReportDTO.billOfMaterials.add(toBomRow(component));
    }
  }

  private void addSecurityVulnerabilities(
      List<ThirdPartyFileCoordinate> thirdPartyFileCoordinateHashList,
      Component component,
      ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO)
  {
    List<String> listId =
        thirdPartyFileCoordinateHashList.stream().map(ThirdPartyFileCoordinate::getId).collect(Collectors.toList());

    List<ThirdPartyCoordinateSecurity> securityList = thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(listId);

    securityList
        .forEach(thirdPartyCoordinateSecurity -> addSecurityVulnerability(thirdPartyCoordinateSecurity, component,
            thirdPartyApplicationReportDTO));
  }

  private void addSecurityVulnerability(
      ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity,
      Component component,
      ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO)
  {
    SecurityVulnerability securityVulnerability =
        new SecurityVulnerability(component.getIdentificationSource().getId(),
            thirdPartyCoordinateSecurity.getRefId(), thirdPartyCoordinateSecurity.getSeverity());
    component.getSecurityVulnerabilities().stream()
        .filter(sv -> sv.getRefId().equals(securityVulnerability.getRefId()))
        .findFirst()
        .orElseGet(() -> {
          securityVulnerability.setUrl(thirdPartyCoordinateSecurity.getLink());
          component.addSecurityVulnerability(securityVulnerability);
          thirdPartyApplicationReportDTO.securityRows
              .add(ComponentResolver.this.toSecurityRow(thirdPartyCoordinateSecurity, component));
          return null;
        });
  }

  private ThirdPartyHealthCheckReportSecurityRowDTO toSecurityRow(
      final ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity,
      final Component component)
  {
    final ThirdPartyHealthCheckReportSecurityRowDTO dto =
        new ThirdPartyHealthCheckReportSecurityRowDTO(component.getComponentIdentifier(), component.getHash());
    dto.matchState = component.getMatchState().getName();
    dto.reference = thirdPartyCoordinateSecurity.getRefId();
    dto.summary = thirdPartyCoordinateSecurity.getDescription();
    dto.score = thirdPartyCoordinateSecurity.getSeverity();
    dto.source = component.getIdentificationSource().getName();
    dto.url = thirdPartyCoordinateSecurity.getLink();
    dto.proprietary = component.isProprietary();
    dto.fixedVersion = thirdPartyCoordinateSecurity.getFixedBy();
    return dto;
  }

  private BillOfMaterialsRowDTO toBomRow(final Component component) {
    final BillOfMaterialsRowDTO dto =
        new BillOfMaterialsRowDTO(component.getComponentIdentifier(), component.getHash());
    dto.createTime = component.getCatalogDate();
    dto.matchState = component.getMatchState().getName();
    dto.pathnames = new HashSet<>(component.getPathnames());
    dto.proprietary = component.isProprietary();
    return dto;
  }
}
