/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.BomPageSbomSummaryDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomDependencyTypeDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomPolicyViolationSummaryDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.sbom.SbomDependencyType;
import com.sonatype.insight.brain.sbom.policy.SbomPolicyService;
import com.sonatype.insight.brain.sbom.utils.SbomCreationDetails;
import com.sonatype.insight.brain.sbom.utils.SbomCreationDetails.Creator;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;

import static com.sonatype.insight.brain.sbom.utils.SbomCreationDetails.CreatorType.parseCreatorType;

@Named
@Singleton
public class SbomComponentsService
{
  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final SbomPolicyService sbomPolicyService;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyVulnerabilityExploitabilityExchangeDAO vexDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private static final Logger log = LoggerFactory.getLogger(SbomComponentsService.class);

  @Inject
  public SbomComponentsService(
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO vexDAO,
      final SbomPolicyService sbomPolicyService)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.vexDAO = vexDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.sbomPolicyService = sbomPolicyService;
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

    CDPSbomComponentDetailsDTO componentDetailsDTO = new CDPSbomComponentDetailsDTO(component.getHash(),
        component.getPackageUrl(), component.getName(), component.getVersion(), component.getId());
    componentDetailsDTO.setDependencyType(getDependencyType(component.getDependencyType()));
    componentDetailsDTO.setMetadata(getSbomMetadata(applicationId, sbomMetadata.getCreatedAt()));
    componentDetailsDTO.setVulnerabilitySummary(getVulnerabilitySummary(vulnerabilityList));
    componentDetailsDTO.setOccurrences(component.getOccurrencesList());

    componentDetailsDTO.setDisclosedVulnerabilities(
        getVulnerabilitiesDetails(sbomMetadata, vulnerabilityList, vexAnnotationsMap, true));
    componentDetailsDTO.setSonatypeIdentifiedVulnerabilities(
        getVulnerabilitiesDetails(sbomMetadata, vulnerabilityList, vexAnnotationsMap, false));
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
      ThirdPartySbomMetadata sbomMetadata,
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
          ThirdPartyVulnerabilityExploitabilityExchange vex = vexAnnotationsMap.get(vulnerability.getId());

          VulnerabilityDetailsDTO vulnerabilityDetailsDTO =
              new VulnerabilityDetailsDTO(vulnerability.getSeverity(), vulnerability.getRefId(),
                  vulnerability.getDescription(),
                  vulnerability.getIdentificationSources().contains(IdentificationSource.SBOM.getName()) &&
                      vulnerability.getIdentificationSources().contains(IdentificationSource.SONATYPE.getName()));
          if (vex != null) {
            vulnerabilityDetailsDTO.setAnalysisStatus(vex.getState());
            vulnerabilityDetailsDTO.setJustification(vex.getJustification());
            vulnerabilityDetailsDTO.setResponse(vex.getResponse());
            vulnerabilityDetailsDTO.setDetails(vex.getDetail());
            vulnerabilityDetailsDTO.setUpdatedAt(vex.getUpdatedAt());
            vulnerabilityDetailsDTO.setLastUpdatedBy(vex.getLastUpdatedByWithoutRealm());
          }
          else {
            // get vex of vulnerability that was previously annotated in an earlier, most recent, SBOM version
            vulnerabilityDetailsDTO.setLatestPreviousAnnotation(
                vexDAO.getLatestVulnerabilityAnalysisByApplicationIdAndRefId(vulnerability.getRefId(),
                    sbomMetadata.getApplicationId(), sbomMetadata.getCreatedAt()));
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
        thirdPartyScanDAO.getByThirdPartyFileId(metadataEntity.getThirdPartyFileId());

    return buildSbomMetadataDTO(
        new SbomMetadataDTO(metadataEntity.getSpec(), metadataEntity.getSpecVersion(), metadataEntity.getSpecFormat(),
            metadataEntity.getMetadataJson(), scanEntity.getScanId(), metadataEntity.getCreatedAt()));
  }

  private BomPageMetadataDTO buildSbomMetadataDTO(SbomMetadataDTO sbomMetadataDTO) {
    String metadataJson = sbomMetadataDTO.metadataJson;
    List<String> manufacturerList = new ArrayList<>();
    List<String> supplierList = new ArrayList<>();
    List<String> authorList = new ArrayList<>();
    List<String> personList = new ArrayList<>();
    List<String> organizationList = new ArrayList<>();
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
        sbomMetadataDTO.createdAt,
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

    int policyViolationLow = 0;
    int policyViolationModerate = 0;
    int policyViolationSevere = 0;
    int policyViolationCritical = 0;
    try {
      PolicyThreats policyThreats = sbomPolicyService.getPolicyViolations(applicationId, version);
      List<PolicyThreats.Component> policyThreatComponents = policyThreats.aaData;
      for (PolicyThreats.Component policyThreatComponent : policyThreatComponents) {
        for (PolicyThreats.PolicyViolation policyViolation : policyThreatComponent.activeViolations) {
          ThreatLevel threatLevel = ThreatLevel.from(policyViolation.policyThreatLevel);
          switch (threatLevel) {
            case LOW:
              policyViolationLow++;
              break;
            case MODERATE:
              policyViolationModerate++;
              break;
            case SEVERE:
              policyViolationSevere++;
              break;
            case CRITICAL:
              policyViolationCritical++;
              break;
            default:
              log.error("Invalid threat level value {}", policyViolation.policyThreatLevel);
          }
        }
      }
      SbomPolicyViolationSummaryDTO sbomPolicyViolationSummaryDTO = new SbomPolicyViolationSummaryDTO();
      sbomPolicyViolationSummaryDTO.setLow(policyViolationLow);
      sbomPolicyViolationSummaryDTO.setModerate(policyViolationModerate);
      sbomPolicyViolationSummaryDTO.setSevere(policyViolationSevere);
      sbomPolicyViolationSummaryDTO.setCritical(policyViolationCritical);

      bomPageSbomSummaryDTO.setPolicyViolationSummary(sbomPolicyViolationSummaryDTO);
      return bomPageSbomSummaryDTO;
    }
    catch (IOException e) {
      log.error("Policy threats report cannot be parsed", e);
      throw new InternalServerException("Policy threat report can not be parsed", e);
    }
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
