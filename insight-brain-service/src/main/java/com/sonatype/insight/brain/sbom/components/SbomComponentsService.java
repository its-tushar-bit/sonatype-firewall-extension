/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;

import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.BomPageSbomSummaryDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomDependencyTypeDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.component.DependencyType;

import com.sonatype.insight.brain.sbom.SbomDependencyType;

import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

import com.sonatype.insight.error.exception.NotFoundException;

import com.sonatype.insight.brain.sbom.utils.SbomCreationDetails;
import com.sonatype.insight.brain.sbom.utils.SbomCreationDetails.Creator;

import com.sonatype.insight.json.store.JsonUtils;

import com.sonatype.insight.brain.audit.AuditData;

import org.apache.commons.collections.CollectionUtils;

import static com.sonatype.insight.brain.sbom.utils.SbomCreationDetails.CreatorType.parseCreatorType;

@Named
@Singleton
public class SbomComponentsService
{
  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyVulnerabilityExploitabilityExchangeDAO vexDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  @Inject
  public SbomComponentsService(
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO vexDAO)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.vexDAO = vexDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
  }

  @Authorize(permission = Permission.READ)
  public CDPSbomComponentDetailsDTO getSbomComponentDetails(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID)
      String applicationId,
      String sbomVersion,
      String componentHash)
  {
    AuditData.get().setComponentHash(componentHash);

    ThirdPartySbomMetadata sbomMetadata =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(applicationId, sbomVersion);
    if (sbomMetadata == null) {
      throw new NotFoundException("Could not find SBOM version " + sbomVersion + " for application " + applicationId);
    }

    ThirdPartyFileCoordinate component =
        thirdPartyFileCoordinateDAO.getBySbomMetadataIdAndComponentHash(sbomMetadata.getId(), componentHash);
    if (component == null) {
      throw new NotFoundException("Could not find component by hash " + componentHash);
    }

    List<ThirdPartyCoordinateSecurity> vulnerabilityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(component.getId());

    List<ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotations = getVexAnnotations(vulnerabilityList);

    Map<String, ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotationsMap =
        vexAnnotations.stream()
            .collect(
                Collectors.toMap(ThirdPartyVulnerabilityExploitabilityExchange::getCoordinateSecurityId, vex -> vex));

    CDPSbomComponentDetailsDTO componentDetailsDTO =
        new CDPSbomComponentDetailsDTO(component.getHash(), component.getPackageUrl(), component.getName(),
            component.getVersion());
    componentDetailsDTO.setDependencyType(getDependencyType(component.getDependencyType()));
    componentDetailsDTO.setMetadata(getSbomMetadata(applicationId, sbomMetadata.getCreatedAt()));
    componentDetailsDTO.setVulnerabilitySummary(getVulnerabilitySummary(vulnerabilityList));

    componentDetailsDTO.setDisclosedVulnerabilities(
        getVulnerabilitiesDetails(vulnerabilityList, vexAnnotationsMap, true));
    componentDetailsDTO.setSonatypeIdentifiedVulnerabilities(
        getVulnerabilitiesDetails(vulnerabilityList, vexAnnotationsMap, false));
    return componentDetailsDTO;
  }

  private CDPSbomMetadataDTO getSbomMetadata(String applicationId, Date sbomMetadataCreatedAt) {
    Application application = applicationDAO.getById(applicationId);
    Organization organization = organizationDAO.getById(application.getOrganizationId());
    CDPSbomMetadataDTO cdpSbomMetadataDTO = new CDPSbomMetadataDTO();
    cdpSbomMetadataDTO.setApplicationName(application.getName());
    cdpSbomMetadataDTO.setOrganizationName(organization.getName());
    cdpSbomMetadataDTO.setSbomCreationTime(sbomMetadataCreatedAt);
    return cdpSbomMetadataDTO;
  }

  private VulnerabilitySummaryDTO getVulnerabilitySummary(List<ThirdPartyCoordinateSecurity> vulnerabilityList) {
    Optional<Double> highestCvssScore = vulnerabilityList.stream()
        .map(ThirdPartyCoordinateSecurity::getSeverity)
        .max(Double::compare);

    Predicate<ThirdPartyCoordinateSecurity> isVerified = vulnerability ->
        vulnerability.getIdentificationSources() != null &&
            vulnerability.getIdentificationSources().contains(IdentificationSource.SBOM.getName()) &&
            vulnerability.getIdentificationSources().contains(IdentificationSource.SONATYPE.getName());

    Predicate<ThirdPartyCoordinateSecurity> isUnverified = vulnerability ->
        vulnerability.getIdentificationSources() == null ||
            vulnerability.getIdentificationSources().equals(IdentificationSource.SBOM.getName());

    Predicate<ThirdPartyCoordinateSecurity> isSonatypeIdentified = vulnerability ->
        vulnerability.getIdentificationSources() != null &&
            vulnerability.getIdentificationSources().equals(IdentificationSource.SONATYPE.getName());

    long verifiedVulnerabilities = vulnerabilityList.stream()
        .filter(isVerified)
        .count();

    long unverifiedVulnerabilities = vulnerabilityList.stream()
        .filter(isUnverified)
        .count();

    long sonatypeIdentified = vulnerabilityList.stream()
        .filter(isSonatypeIdentified)
        .count();

    VulnerabilitySummaryDTO vulnerabilitySummaryDTO = new VulnerabilitySummaryDTO();
    highestCvssScore.ifPresent(vulnerabilitySummaryDTO::setHighestCvssScore);
    vulnerabilitySummaryDTO.setVerifiedVulnerabilitiesCount(Math.toIntExact(verifiedVulnerabilities));
    vulnerabilitySummaryDTO.setUnverifiedVulnerabilities(Math.toIntExact(unverifiedVulnerabilities));
    vulnerabilitySummaryDTO.setSonatypeIdentifiedVulnerabilitiesCount(Math.toIntExact(sonatypeIdentified));
    return vulnerabilitySummaryDTO;
  }

  private List<VulnerabilityDetailsDTO> getVulnerabilitiesDetails(
      List<ThirdPartyCoordinateSecurity> vulnerabilityList,
      Map<String, ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotationsMap,
      boolean disclosedVulnerabilities)
  {
    Predicate<ThirdPartyCoordinateSecurity> isDisclosedVulnerability =
        coordinateSecurity -> coordinateSecurity.getIdentificationSources() != null &&
            coordinateSecurity.getIdentificationSources().contains(IdentificationSource.SBOM.getName());

    Predicate<ThirdPartyCoordinateSecurity> isSonatypeIdentifiedVulnerability =
        coordinateSecurity -> coordinateSecurity.getIdentificationSources() != null &&
            coordinateSecurity.getIdentificationSources().equals(IdentificationSource.SONATYPE.getName());

    return vulnerabilityList.stream()
        .filter(disclosedVulnerabilities ? isDisclosedVulnerability : isSonatypeIdentifiedVulnerability)
        .map(vulnerability -> {
          ThirdPartyVulnerabilityExploitabilityExchange vex =
              vexAnnotationsMap.get(vulnerability.getId());

          VulnerabilityDetailsDTO vulnerabilityDetailsDTO =
              new VulnerabilityDetailsDTO(vulnerability.getSeverity(), vulnerability.getRefId(),
                  vulnerability.getIdentificationSources().contains(IdentificationSource.SBOM.getName()) &&
                      vulnerability.getIdentificationSources().contains(IdentificationSource.SONATYPE.getName()));
          if (vex != null) {
            vulnerabilityDetailsDTO.setAnalysisStatus(vex.getState());
            vulnerabilityDetailsDTO.setJustification(vex.getJustification());
            vulnerabilityDetailsDTO.setResponse(vex.getResponse());
            vulnerabilityDetailsDTO.setDetails(vex.getDetail());
            vulnerabilityDetailsDTO.setUpdatedAt(vex.getUpdatedAt());
            vulnerabilityDetailsDTO.setLastUpdatedBy(vex.getLastUpdatedBy());
          }

          return vulnerabilityDetailsDTO;
        })
        .collect(Collectors.toList());
  }

  private List<ThirdPartyVulnerabilityExploitabilityExchange> getVexAnnotations(
      List<ThirdPartyCoordinateSecurity> vulnerabilityList)
  {
    if (CollectionUtils.isNotEmpty(vulnerabilityList)) {
      List<String> coordinateSecurityIds =
          vulnerabilityList.stream().map(ThirdPartyCoordinateSecurity::getId).collect(
              Collectors.toList());

      return vexDAO.getListByCoordinateSecurityIds(coordinateSecurityIds);
    }
    else {
      return Collections.emptyList();
    }
  }

  @Authorize(permission = Permission.READ)
  public BomPageMetadataDTO getBomPageMetadata(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion)
  {
    ThirdPartySbomMetadata metadataEntity =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(applicationId, sbomVersion);
    if (metadataEntity == null) {
      throw new NotFoundException(
          String.format("Cannot find version %s for application with ID %s.", sbomVersion, applicationId));
    }

    ThirdPartyScan scanEntity =
        thirdPartyScanDAO.getSingleByThirdPartyFileId(metadataEntity.getThirdPartyFileId());

    return buildSbomMetadataDTO(
        new SbomMetadataDTO(metadataEntity.getSpec(), metadataEntity.getSpecVersion(), metadataEntity.getSpecFormat(),
            metadataEntity.getMetadataJson(), scanEntity.getScanId()));
  }

  private BomPageMetadataDTO buildSbomMetadataDTO(SbomMetadataDTO sbomMetadataDTO) {
    String metadataJson = sbomMetadataDTO.metadataJson;
    List<String> manufacturerList = new ArrayList<>();
    List<String> supplierList = new ArrayList<>();
    List<String> authorList = new ArrayList<>();
    List<String> personList = new ArrayList<>();
    List<String> organizationList = new ArrayList<>();
    String createdAt = "";
    if (metadataJson != null) {
      try {
        SbomCreationDetails creationDetails = JsonUtils.parse(metadataJson, SbomCreationDetails.class);
        if (creationDetails.creators != null) {
          for (Creator creator : creationDetails.creators) {
            switch (parseCreatorType(creator.type)) {
              case Manufacturer:
                if (!organizationList.contains(creator.name)) {
                  manufacturerList.add(creator.name);
                }
                break;
              case Supplier:
                if (!supplierList.contains(creator.name)) {
                  supplierList.add(creator.name);
                }
                break;
              case Author:
                if (!authorList.contains(creator.name)) {
                  authorList.add(creator.name);
                }
                break;
              case Person:
                if (!personList.contains(creator.name)) {
                  personList.add(creator.name);
                }
                break;
              case Organization:
                if (!organizationList.contains(creator.name)) {
                  organizationList.add(creator.name);
                }
                break;
              default:
                break;
            }
          }
        }
        createdAt = creationDetails.created;
      }
      catch (IOException e) {
        throw new IllegalStateException("Can not read metadata json, incorrect format", e);
      }
    }
    return new BomPageMetadataDTO(
        authorList,
        manufacturerList,
        supplierList,
        personList,
        organizationList,
        sbomMetadataDTO.specification,
        sbomMetadataDTO.specVersion,
        sbomMetadataDTO.fileFormat,
        createdAt,
        sbomMetadataDTO.scanId
    );
  }

  @Authorize(permission = Permission.READ)
  public BomPageSbomSummaryDTO getSbomSummaryForComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String version)
  {
    ThirdPartySbomMetadata thirdPartySbomMetadata = thirdPartySbomMetadataDAO
        .getByApplicationIdAndSbomVersion(applicationId, version);
    if (thirdPartySbomMetadata == null) {
      throw new NotFoundException(
          String.format("Cannot find version %s for application with ID %s.", version, applicationId));
    }
    long componentCount = thirdPartyFileCoordinateDAO.getNumberOfComponentsForSbom(applicationId, version);
    // return null values for no components
    if (componentCount <= 0) {
      BomPageSbomSummaryDTO noComponentsbomPageSbomSummaryDTO = new BomPageSbomSummaryDTO();
      noComponentsbomPageSbomSummaryDTO.setAllValuesToNull();
      return noComponentsbomPageSbomSummaryDTO;
    }
    BomPageSbomSummaryDTO bomPageSbomSummaryDTO = thirdPartyFileCoordinateDAO
        .getSbomVunerabilitySummaryForComponents(applicationId, version);
    SbomDependencyTypeDTO sbomDependencyTypeDTO = thirdPartyFileCoordinateDAO
        .getSbomDependencyTypeSummaryForComponents(applicationId, version);
    if (sbomDependencyTypeDTO == null || bomPageSbomSummaryDTO == null) {
      throw new NotFoundException(
          String.format("Cannot find version %s for application with ID %s.", version, applicationId));
    }
    bomPageSbomSummaryDTO.setDependencyType(sbomDependencyTypeDTO);
    return bomPageSbomSummaryDTO;
  }

  private String getDependencyType(String code) {
    SbomDependencyType type = SbomDependencyType.fromCode(code);
    switch (type) {
      case DIRECT:
        return DependencyType.DIRECT.getName();
      case TRANSITIVE:
        return DependencyType.TRANSITIVE.getName();
      default:
        return SbomDependencyType.UNKNOWN.name();
    }
  }
}
