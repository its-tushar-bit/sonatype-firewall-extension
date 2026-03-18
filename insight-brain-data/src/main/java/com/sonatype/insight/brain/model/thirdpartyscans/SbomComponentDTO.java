/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.FileCoordinateDisplayNameGenerator.generateDisplayName;

public class SbomComponentDTO
{
  private static final Logger log = LoggerFactory.getLogger(SbomComponentDTO.class);

  private static final TypeReference<Set<ResolvedLicenseDTO>> LICENSE_TYPE_REFERENCE = new TypeReference<>()
  {
  };

  private String hash;

  private String packageUrl;

  private String name;

  private String version;

  private String format;

  private String displayName;

  private String matchStateId;

  private String dependencyType;

  private List<String> filenames;

  private ComponentIdentifier componentIdentifier;

  @JsonInclude(Include.NON_NULL)
  private Integer policyViolationCount;

  private Set<ResolvedLicenseDTO> licenses;

  private int vulnerabilitySeverityNoneCount;

  private int vulnerabilitySeverityLowCount;

  private int vulnerabilitySeverityMediumCount;

  private int vulnerabilitySeverityHighCount;

  private int vulnerabilitySeverityCriticalCount;

  private double percentageAnnotated;

  private String fileCoordinateId;

  private String componentRef;

  private double releaseStatusPercentage;

  public SbomComponentDTO() {
    // for Jackson
  }

  public SbomComponentDTO(Object[] array) {
    hash = (String) array[0];

    packageUrl = (String) array[1];
    if (StringUtils.isNotBlank(packageUrl)) {
      componentIdentifier = new PackageUrlIdentifier(packageUrl).toComponentIdentifier();
    }

    name = (String) array[2];
    version = (String) array[3];

    String formatString = (String) array[4];
    if (StringUtils.isNotBlank(formatString)) {
      format = formatString;
    }

    String displayNameString = (String) array[5];
    displayName = StringUtils.isNotBlank(displayNameString)
        ? displayNameString
        : generateDisplayName(componentIdentifier, format, name, version);

    if (array.length > 6) {
      String licensesJson = (String) array[6];
      if (StringUtils.isNotBlank(licensesJson)) {
        try {
          licenses = JsonUtils.parse(licensesJson, LICENSE_TYPE_REFERENCE);
        }
        catch (IOException e) {
          log.error("Error parsing licenses from {}", licensesJson, e);
        }
      }
      vulnerabilitySeverityNoneCount = longToInt(array[7]);
      vulnerabilitySeverityLowCount = longToInt(array[8]);
      vulnerabilitySeverityMediumCount = longToInt(array[9]);
      vulnerabilitySeverityHighCount = longToInt(array[10]);
      vulnerabilitySeverityCriticalCount = longToInt(array[11]);
      percentageAnnotated = bigDecimalToDouble(array[12]);
      releaseStatusPercentage = bigDecimalToDouble(array[13]);

      String dependencyTypeValue = (String) array[14];
      if (StringUtils.isNotBlank(dependencyTypeValue)) {
        dependencyType = ThirdPartyDependencyType.fromValue(dependencyTypeValue).getDisplayName();
      }
      componentRef = (String) array[18];
      if (StringUtils.isBlank(componentRef)) {
        fileCoordinateId = (String) array[15];
      }
      String filenamesString = (String) array[16];
      if (StringUtils.isNotBlank(filenamesString)) {
        filenames = List.of(filenamesString.split(","));
      }

      String matchStateIdString = (String) array[17];
      if (StringUtils.isNotBlank(matchStateIdString)) {
        matchStateId = matchStateIdString;
      }
    }
  }

  public Integer getPolicyViolationCount() {
    return policyViolationCount;
  }

  public void setPolicyViolationCount(Integer policyViolationCount) {
    this.policyViolationCount = policyViolationCount;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(String packageUrl) {
    this.packageUrl = packageUrl;
  }

  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  public void setComponentIdentifier(ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public String getMatchStateId() {
    return matchStateId;
  }

  public void setMatchStateId(String matchStateId) {
    this.matchStateId = matchStateId;
  }

  public String getDependencyType() {
    return dependencyType;
  }

  public void setDependencyType(final String dependencyType) {
    this.dependencyType = dependencyType;
  }

  public List<String> getFilenames() {
    return filenames;
  }

  public void setFilenames(final List<String> filenames) {
    this.filenames = filenames;
  }

  public Set<ResolvedLicenseDTO> getLicenses() {
    return licenses;
  }

  public void setLicenses(Set<ResolvedLicenseDTO> licenses) {
    this.licenses = licenses;
  }

  public int getVulnerabilitySeverityNoneCount() {
    return vulnerabilitySeverityNoneCount;
  }

  public void setVulnerabilitySeverityNoneCount(int vulnerabilitySeverityNoneCount) {
    this.vulnerabilitySeverityNoneCount = vulnerabilitySeverityNoneCount;
  }

  public int getVulnerabilitySeverityLowCount() {
    return vulnerabilitySeverityLowCount;
  }

  public void setVulnerabilitySeverityLowCount(int vulnerabilitySeverityLowCount) {
    this.vulnerabilitySeverityLowCount = vulnerabilitySeverityLowCount;
  }

  public int getVulnerabilitySeverityMediumCount() {
    return vulnerabilitySeverityMediumCount;
  }

  public void setVulnerabilitySeverityMediumCount(int vulnerabilitySeverityMediumCount) {
    this.vulnerabilitySeverityMediumCount = vulnerabilitySeverityMediumCount;
  }

  public int getVulnerabilitySeverityHighCount() {
    return vulnerabilitySeverityHighCount;
  }

  public void setVulnerabilitySeverityHighCount(int vulnerabilitySeverityHighCount) {
    this.vulnerabilitySeverityHighCount = vulnerabilitySeverityHighCount;
  }

  public int getVulnerabilitySeverityCriticalCount() {
    return vulnerabilitySeverityCriticalCount;
  }

  public void setVulnerabilitySeverityCriticalCount(int vulnerabilitySeverityCriticalCount) {
    this.vulnerabilitySeverityCriticalCount = vulnerabilitySeverityCriticalCount;
  }

  public double getPercentageAnnotated() {
    return percentageAnnotated;
  }

  public String getFileCoordinateId() {
    return fileCoordinateId;
  }

  /**
   * @deprecated Use {@link #setComponentRef} instead
   */
  @Deprecated
  public void setFileCoordinateId(String fileCoordinateId) {
    this.fileCoordinateId = fileCoordinateId;
  }

  public String getComponentRef() {
    return componentRef;
  }

  public void setComponentRef(final String componentRef) {
    this.componentRef = componentRef;
  }

  public double getReleaseStatusPercentage() {
    return releaseStatusPercentage;
  }

  public void setReleaseStatusPercentage(double releaseStatusPercentage) {
    this.releaseStatusPercentage = releaseStatusPercentage;
  }

  private int longToInt(Object number) {
    return ((Long) number).intValue();
  }

  private double bigDecimalToDouble(Object number) {
    return number == null ? 0.0 : ((BigDecimal) number).doubleValue();
  }
}
