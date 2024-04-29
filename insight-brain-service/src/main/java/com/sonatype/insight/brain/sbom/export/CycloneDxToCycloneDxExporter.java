/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;

import org.apache.commons.collections.CollectionUtils;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.metadata.ToolInformation;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.Justification;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.Response;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.State;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import org.cyclonedx.model.vulnerability.Vulnerability.Source;

@Named
public class CycloneDxToCycloneDxExporter extends AbstractSbomExporter
{
  private static final String IDENTIFICATION_SOURCES_PROPERTY = "identificationSources";

  private final IdUtils idUtils;

  private final VersionService versionService;

  @Inject
  public CycloneDxToCycloneDxExporter(
      final InsightWork insightWork,
      final IdUtils idUtils,
      final VersionService versionService,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO)
  {
    super(insightWork, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO, thirdPartyCoordinateLicenseDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO);
    this.idUtils = idUtils;
    this.versionService = versionService;
  }

  @Override
  public String export() {
    try {
      return generateTargetSbomString(generateCurrentSbom());
    }
    catch (GeneratorException generatorException) {
      throw new SbomExportException(String.format("Error when trying to generate bom format %s", generatorException
          .getMessage()), generatorException);
    }
  }

  private Bom generateCurrentSbom() {
    Bom bom = SbomCycloneDxUtils.parseContentNoValidation(getOriginalSbomContentAsString());
    bom.setMetadata(generateNewBomMetadata());
    List<ThirdPartyFileCoordinate> sonatypeComponents = thirdPartyFileCoordinateDAO.getByThirdPartyFileId(
        exportParams.sbomMetadata.getThirdPartyFileId());
    List<Vulnerability> bomVulnerabilitiesList = bom.getVulnerabilities();

    if ( sonatypeComponents != null ) {
      for (ThirdPartyFileCoordinate sonatypeComponent : sonatypeComponents) {
        List<ThirdPartyCoordinateSecurity> sonatypeComponentVulnerabilities = thirdPartyCoordinateSecurityDAO
            .getByFileCoordinateId(sonatypeComponent.getId());
        List<ThirdPartyCoordinateLicense> sonatypeComponentLicenses = thirdPartyCoordinateLicenseDAO
            .getByFileCoordinateId(sonatypeComponent.getId());

        Optional<Component> bomComponentFound = SbomCycloneDxUtils.findComponentByPackageUrl(
            sonatypeComponent.getPackageUrl(), bom);
        if (bomComponentFound.isPresent()) {
          Component bomComponent = bomComponentFound.get();

          // Merge sonatype vulnerabilities into bom
          mergeSonatypeDataVulnerabilities(bomComponent, sonatypeComponentVulnerabilities, bomVulnerabilitiesList);

          // If no new licenses were recovered from db, skip merge process (left current licenses unaltered)
          if (sonatypeComponentLicenses == null) {
            continue;
          }

          List<License> bomComponentLicenses = getBomComponentLicenses(bomComponent);

          //Merge new licenses here
          bomComponentLicenses.addAll(generateUpdatedBomLicenses(sonatypeComponentLicenses, bomComponentLicenses));
        }
      }
    }
    return bom;
  }

  private void mergeSonatypeDataVulnerabilities(
      Component bomComponent,
      List<ThirdPartyCoordinateSecurity> sonatypeVulnerabilities,
      List<Vulnerability> bomVulnerabilities)
  {
    List<Vulnerability> newBomVulnerabilities = new ArrayList<>();
    for (ThirdPartyCoordinateSecurity sonatypeVulnerability  : sonatypeVulnerabilities) {
      Optional<Vulnerability> vulnerabilityFromBom = bomVulnerabilities.stream()
          .filter((Vulnerability bomVulnerability) -> bomVulnerability.getId()
              .equals(sonatypeVulnerability.getRefId())).findAny();

      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation =
          thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(
              sonatypeVulnerability.getId(), sonatypeVulnerability.getRefId());
      if (vulnerabilityFromBom.isPresent()) {
        updateExistingVulnerabilityWithSonatypeData(vulnerabilityFromBom.get(), sonatypeVulnerability,
            sonatypeVexInformation);
      }
      else {
        newBomVulnerabilities.add(createNewBomVulnerabilityWithSonatypeData(bomComponent,  sonatypeVulnerability,
            sonatypeVexInformation));
      }
    }

    if (!newBomVulnerabilities.isEmpty()) {
      // If there are new vulnerabilities not present in the bom document, add them
      bomVulnerabilities.addAll(newBomVulnerabilities);
    }
  }

  private List<License> generateUpdatedBomLicenses(List<ThirdPartyCoordinateLicense> sonatypeComponentLicenses,
                                                   List<License> bomComponentLicenses)
  {
    List<License> newBomComponentLicenses = new ArrayList<>();
    for (ThirdPartyCoordinateLicense sonatypeComponentLicense : sonatypeComponentLicenses) {
      Optional<License> licenseFromBom = bomComponentLicenses.stream()
          .filter(it -> doLicensesMatch(sonatypeComponentLicense, it)).findFirst();

      if (licenseFromBom.isPresent()) {
        updateBomComponentLicenseWithSonatypeData(licenseFromBom.get(), sonatypeComponentLicense);
      }
      else {
        newBomComponentLicenses.add(createNewBomComponentLicenseWithSonatypeData(sonatypeComponentLicense));
      }
    }
    return newBomComponentLicenses;
  }

  private boolean doLicensesMatch(ThirdPartyCoordinateLicense sonatypeComponentLicense, License bomComponentLicense) {
    return bomComponentLicense.getId() != null && bomComponentLicense.getId().equals(sonatypeComponentLicense
        .getLicenseId()) || (bomComponentLicense.getName() != null && bomComponentLicense.getName()
        .equals(sonatypeComponentLicense.getName()));
  }

  private List<License> getBomComponentLicenses(Component bomComponent) {
    if (bomComponent.getLicenseChoice() == null) {
      // Initialize proper empty data structures for holding licenses to avoid null exceptions
      LicenseChoice licenseChoice = new LicenseChoice();
      licenseChoice.setLicenses(Collections.emptyList());
      bomComponent.setLicenseChoice(licenseChoice);
    }
    return bomComponent.getLicenseChoice().getLicenses();
  }

  private Source createVulnerabilitySourceWithSonatypeData(ThirdPartyCoordinateSecurity sonatypeVulnerability) {
    Source source = new Source();
    source.setName(sonatypeVulnerability.getVulnerabilitySource());
    source.setUrl(sonatypeVulnerability.getLink());
    return source;
  }

  private Rating updateVulnerabilityRatingWithSonatypeData(Rating bomRating, ThirdPartyCoordinateSecurity
      sonatypeVulnerability)
  {
    Severity bomSeverity = Severity.fromString(sonatypeVulnerability.getSeverityDescription().toLowerCase());
    Method bomMethod = Method.fromString(sonatypeVulnerability.getRatingMethod().toLowerCase());
    bomRating.setSeverity(bomSeverity == null ? Severity.UNKNOWN : bomSeverity);
    bomRating.setMethod(bomMethod == null ? Method.OTHER : bomMethod);
    bomRating.setScore(sonatypeVulnerability.getSeverity());
    bomRating.setVector(sonatypeVulnerability.getAttackVector());
    bomRating.setSource(createVulnerabilitySourceWithSonatypeData(sonatypeVulnerability));
    return bomRating;
  }

  private Vulnerability createNewBomVulnerabilityWithSonatypeData(
      Component bomComponent,
      ThirdPartyCoordinateSecurity sonatypeVulnerability,
      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    Vulnerability vulnerability = new Vulnerability();
    vulnerability.setId(sonatypeVulnerability.getRefId());
    vulnerability.setDescription(sonatypeVulnerability.getDescription());
    Affect bomNewAffect = new Affect();
    bomNewAffect.setRef(bomComponent.getBomRef());
    vulnerability.setAffects(Collections.singletonList(bomNewAffect));
    return updateExistingVulnerabilityWithSonatypeData(vulnerability, sonatypeVulnerability, sonatypeVexInformation);
  }

  private Vulnerability updateExistingVulnerabilityWithSonatypeData(
      Vulnerability bomVulnerability,
      ThirdPartyCoordinateSecurity sonatypeVulnerability,
      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    bomVulnerability.setDescription(sonatypeVulnerability.getDescription());
    bomVulnerability.setCwes(Collections.singletonList(Integer.valueOf(sonatypeVulnerability.getCwes())));
    bomVulnerability.setSource(createVulnerabilitySourceWithSonatypeData(sonatypeVulnerability));
    bomVulnerability.setRatings(Collections.singletonList(
        updateVulnerabilityRatingWithSonatypeData(new Rating(), sonatypeVulnerability)));

    if (CollectionUtils.isEmpty(bomVulnerability.getProperties())) {
      bomVulnerability.setProperties(Collections
          .singletonList(createVulnerabilityIdentificationSourcePropertyWithSonatypeData(sonatypeVulnerability)));
    }
    else {
      Optional<Property> identificationSources = bomVulnerability.getProperties().stream().filter(
          property -> property.getName().equals(IDENTIFICATION_SOURCES_PROPERTY)).findFirst();
      if (identificationSources.isPresent()) {
        identificationSources.get().setValue(sonatypeVulnerability.getIdentificationSources());
      }
      else {
        bomVulnerability.getProperties().add(createVulnerabilityIdentificationSourcePropertyWithSonatypeData(
            sonatypeVulnerability));
      }
    }

    if (sonatypeVexInformation != null) {
      Analysis analysis = bomVulnerability.getAnalysis();
      if (analysis == null) {
        bomVulnerability.setAnalysis(createVexAnalysisWithSonatypeData(sonatypeVexInformation));
      }
      else {
        updateVexAnalysisWithSonatypeData(analysis, sonatypeVexInformation);
      }
    }

    return bomVulnerability;
  }

  private Property createLicenseIdentificationSourcePropertyWithSonatypeData(
      ThirdPartyCoordinateLicense sonatypeComponentLicense)
  {
    Property property = new Property();
    property.setName(IDENTIFICATION_SOURCES_PROPERTY);
    property.setValue(sonatypeComponentLicense.getIdentificationSources());
    return property;
  }

  private Property createVulnerabilityIdentificationSourcePropertyWithSonatypeData(
      ThirdPartyCoordinateSecurity sonatypeComponentVulnerability)
  {
    Property property = new Property();
    property.setName(IDENTIFICATION_SOURCES_PROPERTY);
    property.setValue(sonatypeComponentVulnerability.getIdentificationSources());
    return property;
  }

  private License createNewBomComponentLicenseWithSonatypeData(ThirdPartyCoordinateLicense sonatypeComponentLicense) {
    License license = new License();

    if (sonatypeComponentLicense.getLicenseId() != null) {
      license.setId(sonatypeComponentLicense.getLicenseId());
    }
    else  {
      license.setName(sonatypeComponentLicense.getName());
    }
    license.setUrl(sonatypeComponentLicense.getUrl());
    license.setProperties(Collections.singletonList(createLicenseIdentificationSourcePropertyWithSonatypeData(
        sonatypeComponentLicense)));
    return license;
  }

  private void updateBomComponentLicenseWithSonatypeData(License bomLicense,
                                                         ThirdPartyCoordinateLicense sonatypeComponentLicense)
  {
    if (sonatypeComponentLicense.getUrl() != null) {
      bomLicense.setUrl(sonatypeComponentLicense.getUrl());
    }

    if (CollectionUtils.isEmpty(bomLicense.getProperties())) {
      bomLicense.setProperties(Collections.singletonList(
          createLicenseIdentificationSourcePropertyWithSonatypeData(sonatypeComponentLicense)));
    }
    else {
      Optional<Property> identificationSources = bomLicense.getProperties().stream().filter(
          property -> property.getName().equals(IDENTIFICATION_SOURCES_PROPERTY) ).findFirst();
      if (identificationSources.isPresent()) {
        identificationSources.get().setValue(sonatypeComponentLicense.getIdentificationSources());
      }
      else {
        bomLicense.getProperties().add(createLicenseIdentificationSourcePropertyWithSonatypeData(
            sonatypeComponentLicense));
      }
    }
  }

  private Analysis updateVexAnalysisWithSonatypeData(
      Analysis bomAnalysis, ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    bomAnalysis.setDetail(sonatypeVexInformation.getDetail());
    bomAnalysis.setJustification(Justification.fromString(sonatypeVexInformation.getJustification()));
    bomAnalysis.setDetail(sonatypeVexInformation.getDetail());
    bomAnalysis.setState(State.fromString(sonatypeVexInformation.getState()));
    bomAnalysis.setFirstIssued(sonatypeVexInformation.getCreatedAt());
    bomAnalysis.setLastUpdated(sonatypeVexInformation.getUpdatedAt());
    if (CollectionUtils.isEmpty(bomAnalysis.getResponses())) {
      bomAnalysis.setResponses(Collections.singletonList(Response.fromString(sonatypeVexInformation.getResponse())));
    }
    else {
      Optional<Response> bomResponseFound = bomAnalysis.getResponses().stream().filter(
          response -> sonatypeVexInformation.getResponse() != null && response.name()
              .equalsIgnoreCase(sonatypeVexInformation.getResponse())).findFirst();
      if (!bomResponseFound.isPresent()) {
        bomAnalysis.getResponses().add(Response.fromString(sonatypeVexInformation.getResponse()));
      }
    }

    return bomAnalysis;
  }

  private Analysis createVexAnalysisWithSonatypeData(
      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    return updateVexAnalysisWithSonatypeData(new Analysis(), sonatypeVexInformation);
  }

  private Metadata generateNewBomMetadata() {
    Metadata newBomMetadata = new Metadata();
    newBomMetadata.setTimestamp(new Date());

    ToolInformation toolInformation = new ToolInformation();
    Component generatorToolComponent = new Component();
    generatorToolComponent.setType(Type.APPLICATION);
    generatorToolComponent.setName("Sonatype SBOM Manager");
    generatorToolComponent.setVersion(versionService.getFullVersion());
    toolInformation.setComponents(Collections.singletonList(generatorToolComponent));
    newBomMetadata.setToolChoice(toolInformation);

    OrganizationalEntity organizationalEntity = new OrganizationalEntity();
    organizationalEntity.setName("Sonatype Inc.");
    organizationalEntity.setUrls(Collections.singletonList("https://www.sonatype.com/"));

    Component bomComponentInfo = new Component();
    bomComponentInfo.setType(Type.APPLICATION);
    bomComponentInfo.setName(idUtils.getPublicOwnerId(OwnerType.APPLICATION, exportParams.sbomMetadata
        .getApplicationId()));
    newBomMetadata.setComponent(bomComponentInfo);
    return newBomMetadata;
  }
}
