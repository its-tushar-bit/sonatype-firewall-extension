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
import java.util.UUID;
import java.util.stream.Collectors;

import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.SbomTaxonomy;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.util.SbomUtils;

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

import static com.google.common.collect.Lists.newArrayList;

public class SbomExportUtils
{
  private static final String IDENTIFICATION_SOURCE_SONATYPE_CONTAINER =
      IdentificationSource.SONATYPE_CONTAINER.getName();

  public static Vulnerability createCycloneDxVulnerabilityFromDbData(
      Component bomComponent,
      ThirdPartyCoordinateSecurity sonatypeVulnerability,
      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    Vulnerability vulnerability = new Vulnerability();
    vulnerability.setBomRef(String.format("%s-%s", sonatypeVulnerability.getRefId(), uuid()));
    vulnerability.setId(sonatypeVulnerability.getRefId());
    vulnerability.setDescription(sonatypeVulnerability.getDescription());
    Affect newAffect = newAffectLinkingComponent(bomComponent);
    vulnerability.setAffects(newArrayList(newAffect));
    return updateCycloneDxVulnerabilityFromDbData(vulnerability, sonatypeVulnerability, sonatypeVexInformation);
  }

  public static Affect newAffectLinkingComponent(final Component bomComponent) {
    Affect newAffect = new Affect();
    String bomRef = bomComponent.getBomRef();
    if (StringUtils.isNotBlank(bomRef)) {
      newAffect.setRef(bomRef);
    }
    else {
      bomRef = uuid();
      bomComponent.setBomRef(bomRef);
      newAffect.setRef(bomRef);
    }
    return newAffect;
  }

  public static Vulnerability updateCycloneDxVulnerabilityFromDbData(
      Vulnerability bomVulnerability,
      ThirdPartyCoordinateSecurity sonatypeVulnerability,
      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    bomVulnerability.setDescription(sonatypeVulnerability.getDescription());
    if (bomVulnerability.getRecommendation() != null) {
      bomVulnerability.setRecommendation(StringUtils.normalizeSpace(bomVulnerability.getRecommendation()));
    }
    if (StringUtils.isNotBlank(sonatypeVulnerability.getCwes())) {
      bomVulnerability.setCwes(SbomMetadataUtils.convertCwesStringToIntegerList(sonatypeVulnerability.getCwes()));
    }

    if (StringUtils.isNotBlank(sonatypeVulnerability.getVulnerabilitySource())) {
      bomVulnerability.setSource(createVulnerabilitySourceWithSonatypeData(sonatypeVulnerability));
    }

    bomVulnerability.setRatings(Collections.singletonList(
        updateVulnerabilityRatingWithSonatypeData(new Rating(), sonatypeVulnerability)));

    String vulnerabilityIdentificationSource =
        IDENTIFICATION_SOURCE_SONATYPE_CONTAINER.equals(sonatypeVulnerability.getVulnerabilitySource())
            ? IDENTIFICATION_SOURCE_SONATYPE_CONTAINER
            : sonatypeVulnerability.getIdentificationSources();

    bomVulnerability.setProperties(addOrUpdateBomElementProperty(bomVulnerability.getProperties(),
        SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME, vulnerabilityIdentificationSource));

    // If there is sonatype vex information stored in the database, we use it to augment the original bom
    Analysis analysis = bomVulnerability.getAnalysis();
    if (sonatypeVexInformation != null) {
      if (analysis == null) {
        bomVulnerability.setAnalysis(createVexAnalysisWithSonatypeData(sonatypeVexInformation));
      }
      else {
        updateVexAnalysisWithSonatypeData(analysis, sonatypeVexInformation);
      }
    }
    else {
      // When no vex records were found in the db, this might indicate the vex annotations records might have been
      // deleted or never existed in first place. In this case, we check if the bom object has initial vex data and set
      // it to null, as the intention is to augment original bom with db data and not to export the original bom
      bomVulnerability.setAnalysis(null);
    }

    return bomVulnerability;
  }

  public static License createCycloneDxLicenseForThirdpartyLicense(ThirdPartyCoordinateLicense tpLicense) {
    return createCycloneDxLicense(tpLicense.getLicenseId(), tpLicense.getUrl(), tpLicense.getIdentificationSources());
  }

  public static License createCycloneDxLicenseForResolvedLicense(ResolvedLicenseDTO resolved) {
    return createCycloneDxLicense(resolved.licenseId(), resolved.licenseUrl(), resolved.identificationSources());
  }

  private static License createCycloneDxLicense(String licenseId, String licenseUrl, String identificationSources) {
    License license = new License();
    if (ListedLicenses.getListedLicenses().isSpdxListedLicenseId(licenseId)) {
      license.setId(licenseId);
    }
    else {
      license.setName(licenseId);
    }
    return updateCycloneDxLicenseAttributes(license, licenseUrl,
        identificationSources);
  }

  public static License updateCycloneDxLicenseAttributes(
      License bomLicense,
      String licenseUrl,
      String identificationSources)
  {
    if (licenseUrl != null) {
      bomLicense.setUrl(licenseUrl);
    }
    bomLicense.setProperties(addOrUpdateBomElementProperty(bomLicense.getProperties(),
        SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME, identificationSources));
    return bomLicense;
  }

  public static List<Property> addOrUpdateBomElementProperty(
      List<Property> properties,
      String propName,
      String propValue)
  {
    if (CollectionUtils.isEmpty(properties)) {
      if (StringUtils.isNotEmpty(propValue)) {
        properties = new ArrayList<>();
        properties.add(createCycloneDxProperty(propName, propValue));
      }
    }
    else {
      Optional<Property> existingProperty = properties.stream()
          .filter(
              property -> property.getName().equals(propName))
          .findFirst();
      if (existingProperty.isPresent()) {
        if (StringUtils.isNotEmpty(propValue)) {
          existingProperty.get().setValue(propValue);
        }
      }
      else {
        if (!updateCycloneDxLegacyPropertyIfPresent(properties, propName, propValue)) {
          if (StringUtils.isNotEmpty(propValue)) {
            properties.add(createCycloneDxProperty(propName, propValue));
          }
        }
      }
    }
    return properties;
  }

  public static Property createCycloneDxProperty(String propName, String propValue) {
    Property property = new Property();
    property.setName(propName);
    property.setValue(propValue);
    return property;
  }

  public static boolean updateCycloneDxLegacyPropertyIfPresent(
      List<Property> properties,
      String propName,
      String propValue)
  {
    if (SbomUtils.getLegacyPropertyForCdxProperty(propName) != null) {
      if (CollectionUtils.isNotEmpty(properties)) {
        Optional<Property> legacyProperty = properties.stream()
            .filter(
                property -> property.getName().equals(SbomUtils.getLegacyPropertyForCdxProperty(propName)))
            .findFirst();
        if (legacyProperty.isPresent()) {
          legacyProperty.get().setName(propName);
          if (StringUtils.isNotEmpty(propValue)) {
            legacyProperty.get().setValue(propValue);
          }
          return true;
        }
        // Check for identificationSources, another legacy property specific to SBOM Manager
        else if (propName.equals(SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME)) {
          Optional<Property> identificationSourcesProperty = properties.stream()
              .filter(
                  property -> property.getName().equals("identificationSources"))
              .findFirst();
          if (identificationSourcesProperty.isPresent()) {
            identificationSourcesProperty.get().setName(SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME);
            return true;
          }
        }
      }
    }
    return false;
  }

  private static Rating updateVulnerabilityRatingWithSonatypeData(
      Rating bomRating,
      ThirdPartyCoordinateSecurity sonatypeVulnerability)
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
      Analysis bomAnalysis,
      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
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
          Arrays.stream(sonatypeVexInformation.getResponse().split(","))
              .filter(Objects::nonNull)
              .map(String::trim)
              .map(Response::fromString)
              .collect(Collectors.toList());
    }

    // Always override existing responses. Empty sonatypeResponses means the original VEX responses are empty as well.
    if (CollectionUtils.isNotEmpty(sonatypeResponses)) {
      bomAnalysis.setResponses(sonatypeResponses);
    }
    return bomAnalysis;
  }

  private static String uuid() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
