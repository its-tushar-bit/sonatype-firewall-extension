/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
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
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SpdxLicenseExpressionUtil;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.library.model.license.LicenseInfoFactory;

public abstract class AbstractCycloneDxExporter
    extends AbstractSbomExporter
{
  protected static final String IDENTIFICATION_SOURCES_PROPERTY = "identificationSources";

  protected final MultiLicenseDAO multiLicenseDAO;

  protected final SpdxLicenseExpressionUtil spdxLicenseExpressionUtil;

  protected AbstractCycloneDxExporter(
      final InsightWork insightWork,
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService)
  {
    super(insightWork, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO, thirdPartyCoordinateLicenseDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO, baseUrl, idUtils, versionService);
    this.multiLicenseDAO = multiLicenseDAO;
    this.spdxLicenseExpressionUtil = new SpdxLicenseExpressionUtil(multiLicenseDAO);
  }

  protected Bom mergeCurrentDatabaseState(Bom bom) {
    bom.setMetadata(generateNewBomMetadata());
    List<ThirdPartyFileCoordinate> sonatypeComponents = thirdPartyFileCoordinateDAO.getByThirdPartyFileId(
        exportParams.sbomMetadata.getThirdPartyFileId());
    List<Vulnerability> bomVulnerabilitiesList = bom.getVulnerabilities();

    if (sonatypeComponents != null) {
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
          LicenseChoice bomLicenseChoice = getBomComponentLicenses(bomComponent);
          //Merge new licenses here
          updateOrGenerateNewLicenseChoices(sonatypeComponentLicenses, bomLicenseChoice);
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
    for (ThirdPartyCoordinateSecurity sonatypeVulnerability : sonatypeVulnerabilities) {

      Optional<Vulnerability> vulnerabilityFromBom = Optional.empty();

      if (bomVulnerabilities == null) {
        bomVulnerabilities = new ArrayList<>();
      }
      else {
        vulnerabilityFromBom = bomVulnerabilities.stream()
            .filter((Vulnerability bomVulnerability) -> bomVulnerability.getId()
                .equals(sonatypeVulnerability.getRefId())).findAny();
      }

      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation =
          thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(
              sonatypeVulnerability.getId(), sonatypeVulnerability.getRefId());
      if (vulnerabilityFromBom.isPresent()) {
        updateExistingVulnerabilityWithSonatypeData(vulnerabilityFromBom.get(), sonatypeVulnerability,
            sonatypeVexInformation);
      }
      else {
        newBomVulnerabilities.add(createNewBomVulnerabilityWithSonatypeData(bomComponent, sonatypeVulnerability,
            sonatypeVexInformation));
      }
    }

    if (!newBomVulnerabilities.isEmpty()) {
      // If there are new vulnerabilities not present in the bom document, add them
      bomVulnerabilities.addAll(newBomVulnerabilities);
    }
  }

  private void updateOrGenerateNewLicenseChoices(
      List<ThirdPartyCoordinateLicense> sonatypeComponentLicenses,
      LicenseChoice bomLicenseChoice)
  {
    for (ThirdPartyCoordinateLicense sonatypeComponentLicense : sonatypeComponentLicenses) {
      if (CollectionUtils.isNotEmpty(bomLicenseChoice.getLicenses())) {
        Optional<License> licenseFromBom = bomLicenseChoice.getLicenses().stream()
            .filter(it -> doLicensesMatch(sonatypeComponentLicense, it)).findFirst();
        if (licenseFromBom.isPresent()) {
          updateBomComponentLicenseWithSonatypeData(licenseFromBom.get(), sonatypeComponentLicense);
          continue;
        }
      }
      else {
        bomLicenseChoice.setLicenses(new ArrayList<>());
      }
      bomLicenseChoice.addLicense(createNewBomComponentLicenseWithSonatypeData(sonatypeComponentLicense));
    }
  }

  private boolean doLicensesMatch(ThirdPartyCoordinateLicense sonatypeComponentLicense, License bomComponentLicense) {
    return bomComponentLicense.getId() != null && bomComponentLicense.getId().equals(sonatypeComponentLicense
        .getLicenseId()) || (bomComponentLicense.getName() != null && bomComponentLicense.getName()
        .equals(sonatypeComponentLicense.getName()));
  }

  private LicenseChoice getBomComponentLicenses(Component bomComponent) {
    if (bomComponent.getLicenseChoice() == null) {
      // Initialize proper empty data structures for holding licenses to avoid null exceptions
      LicenseChoice licenseChoice = new LicenseChoice();
      licenseChoice.setLicenses(Collections.emptyList());
      bomComponent.setLicenseChoice(licenseChoice);
    }
    else if (StringUtils.isNotEmpty(bomComponent.getLicenseChoice().getExpression())) {
      bomComponent.getLicenseChoice().setLicenses(new ArrayList<>());
      String purl = bomComponent.getPurl() != null ? bomComponent.getPurl() : "";
      bomComponent.getLicenseChoice().getLicenses().addAll(
          parseLicenseChoiceExpression(bomComponent.getLicenseChoice().getExpression(), purl,
              bomComponent.getBomRef()));
    }
    return bomComponent.getLicenseChoice();
  }

  private List<License> parseLicenseChoiceExpression(String expression, String purl, String bomRef) {
    List<License> licenses = new ArrayList<>();
    try {
      AnyLicenseInfo anyLicenseInfo = LicenseInfoFactory.parseSPDXLicenseString(expression);
      Map<String, String> processedLicenses = new HashMap<>();
      spdxLicenseExpressionUtil.parseLicenses(anyLicenseInfo, processedLicenses, purl);
      for (String licenseId : processedLicenses.keySet()) {
        License processedLicense = new License();
        processedLicense.setId(licenseId);
        processedLicense.setBomRef(bomRef);
        licenses.add(processedLicense);
      }
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Failed to process spdx license string: {}", expression);
    }
    return licenses;
  }

  private Source createVulnerabilitySourceWithSonatypeData(ThirdPartyCoordinateSecurity sonatypeVulnerability) {
    Source source = new Source();
    source.setName(sonatypeVulnerability.getVulnerabilitySource());
    source.setUrl(sonatypeVulnerability.getLink());
    return source;
  }

  private Rating updateVulnerabilityRatingWithSonatypeData(
      Rating bomRating, ThirdPartyCoordinateSecurity
      sonatypeVulnerability)
  {
    Severity bomSeverity = null;
    if (StringUtils.isNotBlank(sonatypeVulnerability.getSeverityDescription())) {
      bomSeverity = Severity.fromString(sonatypeVulnerability.getSeverityDescription().toLowerCase());
    }
    bomRating.setSeverity(bomSeverity == null ? Severity.UNKNOWN : bomSeverity);
    Method bomMethod = null;
    if (StringUtils.isNotBlank(sonatypeVulnerability.getRatingMethod())) {
      bomMethod = Method.fromString(sonatypeVulnerability.getRatingMethod().toLowerCase());
    }
    bomRating.setMethod(bomMethod == null ? Method.OTHER : bomMethod);
    bomRating.setScore(sonatypeVulnerability.getSeverity());
    if (StringUtils.isNotBlank(sonatypeVulnerability.getAttackVector())) {
      bomRating.setVector(sonatypeVulnerability.getAttackVector());
    }
    if (StringUtils.isNotBlank(sonatypeVulnerability.getVulnerabilitySource())) {
      bomRating.setSource(createVulnerabilitySourceWithSonatypeData(sonatypeVulnerability));
    }
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
    if (StringUtils.isNotBlank(bomComponent.getBomRef())) {
      bomNewAffect.setRef(bomComponent.getBomRef());
    }
    vulnerability.setAffects(Collections.singletonList(bomNewAffect));
    return updateExistingVulnerabilityWithSonatypeData(vulnerability, sonatypeVulnerability, sonatypeVexInformation);
  }

  private Vulnerability updateExistingVulnerabilityWithSonatypeData(
      Vulnerability bomVulnerability,
      ThirdPartyCoordinateSecurity sonatypeVulnerability,
      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    bomVulnerability.setDescription(sonatypeVulnerability.getDescription());
    if (StringUtils.isNotBlank(sonatypeVulnerability.getCwes())) {
      String cwesString = sonatypeVulnerability.getCwes();
      List<Integer> cwesList = Arrays.stream(cwesString.split(","))
          .map(String::trim)
          .map(Integer::valueOf)
          .collect(Collectors.toList());

      bomVulnerability.setCwes(cwesList);
    }

    if (StringUtils.isNotBlank(sonatypeVulnerability.getVulnerabilitySource())) {
      bomVulnerability.setSource(createVulnerabilitySourceWithSonatypeData(sonatypeVulnerability));
    }

    bomVulnerability.setRatings(Collections.singletonList(
        updateVulnerabilityRatingWithSonatypeData(new Rating(), sonatypeVulnerability)));

    if (CollectionUtils.isEmpty(bomVulnerability.getProperties())) {
      if (StringUtils.isNotEmpty(sonatypeVulnerability.getIdentificationSources())) {
        bomVulnerability.setProperties(Collections.singletonList(createIdentificationSourcePropertyWithSonatypeData(
            sonatypeVulnerability.getIdentificationSources())));
      }
    }
    else {
      Optional<Property> identificationSources = bomVulnerability.getProperties().stream().filter(
          property -> property.getName().equals(IDENTIFICATION_SOURCES_PROPERTY)).findFirst();
      if (identificationSources.isPresent()) {
        identificationSources.get().setValue(sonatypeVulnerability.getIdentificationSources());
      }
      else {
        bomVulnerability.getProperties().add(createIdentificationSourcePropertyWithSonatypeData(
            sonatypeVulnerability.getIdentificationSources()));
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

  private Property createIdentificationSourcePropertyWithSonatypeData(String value) {
    Property property = new Property();
    property.setName(IDENTIFICATION_SOURCES_PROPERTY);
    property.setValue(value);
    return property;
  }

  private License createNewBomComponentLicenseWithSonatypeData(ThirdPartyCoordinateLicense sonatypeComponentLicense) {
    License license = new License();

    if (sonatypeComponentLicense.getLicenseId() != null) {
      license.setId(sonatypeComponentLicense.getLicenseId());
    }
    else {
      license.setName(sonatypeComponentLicense.getName());
    }
    license.setUrl(sonatypeComponentLicense.getUrl());
    if (StringUtils.isNotEmpty(sonatypeComponentLicense.getIdentificationSources())) {
      if (license.getProperties() == null) {
        license.setProperties(Collections.singletonList(createIdentificationSourcePropertyWithSonatypeData(
            sonatypeComponentLicense.getIdentificationSources())));
      }
      else {
        license.getProperties().add(createIdentificationSourcePropertyWithSonatypeData(
            sonatypeComponentLicense.getIdentificationSources()));
      }
    }
    return license;
  }

  private void updateBomComponentLicenseWithSonatypeData(
      License bomLicense,
      ThirdPartyCoordinateLicense sonatypeComponentLicense)
  {
    if (sonatypeComponentLicense.getUrl() != null) {
      bomLicense.setUrl(sonatypeComponentLicense.getUrl());
    }

    if (CollectionUtils.isEmpty(bomLicense.getProperties())) {
      if (StringUtils.isNotEmpty(sonatypeComponentLicense.getIdentificationSources())) {
        bomLicense.setProperties(Collections.singletonList(createIdentificationSourcePropertyWithSonatypeData(
            sonatypeComponentLicense.getIdentificationSources())));
      }
    }
    else {
      Optional<Property> identificationSources = bomLicense.getProperties().stream().filter(
          property -> property.getName().equals(IDENTIFICATION_SOURCES_PROPERTY)).findFirst();
      if (identificationSources.isPresent()) {
        identificationSources.get().setValue(sonatypeComponentLicense.getIdentificationSources());
      }
      else {
        bomLicense.getProperties().add(createIdentificationSourcePropertyWithSonatypeData(
            sonatypeComponentLicense.getIdentificationSources()));
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
      if (StringUtils.isNotBlank(sonatypeVexInformation.getResponse())) {
        bomAnalysis.setResponses(Collections.singletonList(Response.fromString(sonatypeVexInformation.getResponse())));
      }
    }
    else {
      Optional<Response> bomResponseFound = bomAnalysis.getResponses().stream()
          .filter(Objects::nonNull)
          .filter(response -> sonatypeVexInformation.getResponse() != null && response.name()
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
