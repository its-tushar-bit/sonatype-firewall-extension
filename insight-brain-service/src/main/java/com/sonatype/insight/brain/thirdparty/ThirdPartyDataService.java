/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.scan.application.BillOfMaterialsRowDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ThirdPartyDataService
{
  public static final String THIRD_PARTY_BOM_JSON_FILENAME = "thirdparty-bom.json";

  public static final String THIRD_PARTY_SECURITY_JSON_FILENAME = "thirdparty-security.json";

  private static final Logger log = LoggerFactory.getLogger(ThirdPartyDataService.class);

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  @Inject
  public ThirdPartyDataService(
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO)
  {
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
  }

  public ThirdPartyApplicationReportDTO getScanData(final String scanId) {
    List<ThirdPartyScan> scanData = thirdPartyScanDAO.getByScanId(scanId);
    if (!scanData.isEmpty()) {
      log.debug("Found {} third party scan data files for scanId {}", scanData.size(), scanId);
      return loadThirdPartyDataForScan(scanId, scanData.get(0).getCreateTime());
    }
    return null;
  }

  private ThirdPartyApplicationReportDTO loadThirdPartyDataForScan(String scanId, final Date scanTime) {
    ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO = new ThirdPartyApplicationReportDTO();

    List<ThirdPartyFile> scanFiles = thirdPartyFileDAO.getByScanId(scanId);
    Map<String, Set<String>> coordPaths = new HashMap<>(); //collect paths (if identical coordinates found)
    Map<String, ThirdPartyFileCoordinate> coordinates = new HashMap<>(); //filters out any identical components
    for (ThirdPartyFile scanFile : scanFiles) {
      thirdPartyFileCoordinateDAO.getByThirdPartyFileId(scanFile.getId())
          .forEach(coord -> {
            coordinates.put(coord.getHash(), coord);
            coordPaths.computeIfAbsent(coord.getHash(), k -> new HashSet<>()).add(scanFile.getFilename());
          });
    }

    for (ThirdPartyFileCoordinate coord : coordinates.values()) {
      final ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.createGenericIdentifier(coord);
      thirdPartyApplicationReportDTO.billOfMaterials.add(toBomRow(coord, componentIdentifier, coordPaths, scanTime));
      populateSecurityVulnerabilities(coord, componentIdentifier, thirdPartyApplicationReportDTO);
    }

    log.debug("Found {} third party components and {} vulnerabilities for scanId {}",
        thirdPartyApplicationReportDTO.billOfMaterials.size(),
        thirdPartyApplicationReportDTO.securityRows.size(),
        scanId);
    return thirdPartyApplicationReportDTO;
  }

  private void populateSecurityVulnerabilities(
      final ThirdPartyFileCoordinate coord,
      final ComponentIdentifier componentIdentifier,
      final ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO)
  {
    thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(coord.getId()).forEach(
        sec -> thirdPartyApplicationReportDTO.securityRows.add(toSecurityRow(sec, componentIdentifier, coord)));
  }

  private ThirdPartyHealthCheckReportSecurityRowDTO toSecurityRow(
      final ThirdPartyCoordinateSecurity coordinateSecurity,
      final ComponentIdentifier componentIdentifier,
      final ThirdPartyFileCoordinate coordinate)
  {
    final ThirdPartyHealthCheckReportSecurityRowDTO dto =
        new ThirdPartyHealthCheckReportSecurityRowDTO(componentIdentifier, coordinate.getHash());
    dto.matchState = MatchState.EXACT.getName();
    dto.reference = coordinateSecurity.getRefId();
    dto.description = coordinateSecurity.getDescription();
    dto.score = coordinateSecurity.getSeverity();
    dto.source = coordinate.getSource();
    dto.url = coordinateSecurity.getLink();
    dto.fixedVersion = coordinateSecurity.getFixedBy();
    return dto;
  }

  private BillOfMaterialsRowDTO toBomRow(
      final ThirdPartyFileCoordinate coordinate,
      ComponentIdentifier componentIdentifier,
      final Map<String, Set<String>> coordPaths, final Date scanTime)
  {
    final BillOfMaterialsRowDTO dto =
        new BillOfMaterialsRowDTO(componentIdentifier, coordinate.getHash());
    dto.createTime = scanTime.getTime();
    dto.matchState = MatchState.EXACT.getName();
    dto.pathnames = coordPaths.get(coordinate.getHash());
    return dto;
  }
}
