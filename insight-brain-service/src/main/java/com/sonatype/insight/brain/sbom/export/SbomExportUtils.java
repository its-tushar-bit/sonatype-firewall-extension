/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.License;
import org.cyclonedx.model.Property;
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
import org.spdx.library.model.license.ListedLicenses;

public class SbomExportUtils
{
  public static final String IDENTIFICATION_SOURCES_PROPERTY = "identificationSources";

  public static Vulnerability createCycloneDxVulnerabilityFromDbData(
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
    return updateCycloneDxVulnerabilityFromDbData(vulnerability, sonatypeVulnerability, sonatypeVexInformation);
  }

  public static Vulnerability updateCycloneDxVulnerabilityFromDbData(
      Vulnerability bomVulnerability,
      ThirdPartyCoordinateSecurity sonatypeVulnerability,
      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    bomVulnerability.setDescription(sonatypeVulnerability.getDescription());
    if (StringUtils.isNotBlank(sonatypeVulnerability.getCwes())) {
      bomVulnerability.setCwes(SbomMetadataUtils.convertCwesStringToIntegerList(sonatypeVulnerability.getCwes()));
    }

    if (StringUtils.isNotBlank(sonatypeVulnerability.getVulnerabilitySource())) {
      bomVulnerability.setSource(createVulnerabilitySourceWithSonatypeData(sonatypeVulnerability));
    }

    bomVulnerability.setRatings(Collections.singletonList(
        updateVulnerabilityRatingWithSonatypeData(new Rating(), sonatypeVulnerability)));

    if (CollectionUtils.isEmpty(bomVulnerability.getProperties())) {
      if (StringUtils.isNotEmpty(sonatypeVulnerability.getIdentificationSources())) {
        bomVulnerability.setProperties(Collections.singletonList(createCycloneDxIdentificationSourceProperty(
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
        bomVulnerability.getProperties().add(createCycloneDxIdentificationSourceProperty(
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

  public static Property createCycloneDxIdentificationSourceProperty(String value) {
    Property property = new Property();
    property.setName(IDENTIFICATION_SOURCES_PROPERTY);
    property.setValue(value);
    return property;
  }

  public static License createCycloneDxLicenseFromDbData(ThirdPartyCoordinateLicense sonatypeComponentLicense) {
    License license = new License();
    String licenseId = sonatypeComponentLicense.getLicenseId();

    if (ListedLicenses.getListedLicenses().isSpdxListedLicenseId(licenseId)) {
      license.setId(licenseId);
    }
    else {
      license.setName(licenseId);
    }

    license.setUrl(sonatypeComponentLicense.getUrl());
    if (StringUtils.isNotEmpty(sonatypeComponentLicense.getIdentificationSources())) {
      if (license.getProperties() == null) {
        license.setProperties(Collections.singletonList(createCycloneDxIdentificationSourceProperty(
            sonatypeComponentLicense.getIdentificationSources())));
      }
      else {
        license.getProperties().add(createCycloneDxIdentificationSourceProperty(
            sonatypeComponentLicense.getIdentificationSources()));
      }
    }
    return license;
  }

  private static Rating updateVulnerabilityRatingWithSonatypeData(
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
      bomMethod = SbomCycloneDxUtils.resolveRatingMethod(sonatypeVulnerability.getRatingMethod());
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

  private static Source createVulnerabilitySourceWithSonatypeData(ThirdPartyCoordinateSecurity sonatypeVulnerability) {
    Source source = new Source();
    source.setName(sonatypeVulnerability.getVulnerabilitySource());
    source.setUrl(sonatypeVulnerability.getLink());
    return source;
  }

  private static Analysis createVexAnalysisWithSonatypeData(
      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    return updateVexAnalysisWithSonatypeData(new Analysis(), sonatypeVexInformation);
  }

  private static Analysis updateVexAnalysisWithSonatypeData(
      Analysis bomAnalysis, ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    bomAnalysis.setDetail(sonatypeVexInformation.getDetail());
    bomAnalysis.setJustification(Justification.fromString(sonatypeVexInformation.getJustification()));
    bomAnalysis.setDetail(sonatypeVexInformation.getDetail());
    bomAnalysis.setState(State.fromString(sonatypeVexInformation.getState()));
    bomAnalysis.setFirstIssued(sonatypeVexInformation.getCreatedAt());
    bomAnalysis.setLastUpdated(sonatypeVexInformation.getUpdatedAt());

    List<Response> sonatypeResponses = new ArrayList<>();

    if (StringUtils.isNotBlank(sonatypeVexInformation.getResponse())) {
      sonatypeResponses =
          Arrays.stream(sonatypeVexInformation.getResponse().split(",")).filter(Objects::nonNull).map(String::trim)
              .map(Response::fromString).collect(Collectors.toList());
    }

    //Always override existing responses. Empty sonatypeResponses means the original VEX responses are empty as well.
    if (CollectionUtils.isNotEmpty(sonatypeResponses)) {
      bomAnalysis.setResponses(sonatypeResponses);
    }
    return bomAnalysis;
  }
}
