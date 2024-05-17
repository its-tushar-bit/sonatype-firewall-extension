/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Set;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbomComponentDTO
{
  private static final Logger log = LoggerFactory.getLogger(SbomComponentDTO.class);

  private static final TypeReference<Set<License>> LICENSE_TYPE_REFERENCE = new TypeReference<Set<License>>() { };

  private String hash;

  private String packageUrl;

  private String name;

  private String version;

  private String dependencyType;

  private ComponentIdentifier componentIdentifier;

  private String displayName;

  private Set<License> licenses;

  private int vulnerabilitySeverityNoneCount;

  private int vulnerabilitySeverityLowCount;

  private int vulnerabilitySeverityMediumCount;

  private int vulnerabilitySeverityHighCount;

  private int vulnerabilitySeverityCriticalCount;

  private double percentageAnnotated;

  public SbomComponentDTO() {
    // for Jackson
  }

  public SbomComponentDTO(Object[] array) {
    hash = (String) array[0];
    packageUrl = (String) array[1];
    name = (String) array[2];
    version = (String) array[3];

    if (StringUtils.isNotBlank(packageUrl)) {
      componentIdentifier = new PackageUrlIdentifier(packageUrl).toComponentIdentifier();
      displayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
    }

    if (StringUtils.isBlank(displayName)) {
      displayName = name + ":" + version;
    }

    if ( array.length > 4 ) {
      String licensesJson = (String) array[4];
      if (StringUtils.isNotBlank(licensesJson)) {
        try {
          licenses = JsonUtils.parse(licensesJson, LICENSE_TYPE_REFERENCE);
        }
        catch (IOException e) {
          log.error("Error parsing licenses from {}", licensesJson, e);
        }
      }
      vulnerabilitySeverityNoneCount = longToInt(array[5]);
      vulnerabilitySeverityLowCount = longToInt(array[6]);
      vulnerabilitySeverityMediumCount = longToInt(array[7]);
      vulnerabilitySeverityHighCount = longToInt(array[8]);
      vulnerabilitySeverityCriticalCount = longToInt(array[9]);
      percentageAnnotated = bigDecimalToDouble(array[10]);

      String dependencyTypeValue = (String) array[11];
      if (StringUtils.isNotBlank(dependencyTypeValue)) {
        dependencyType = ThirdPartyDependencyType.fromValue(dependencyTypeValue).getDisplayName();
      }
    }
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

  public String getDependencyType() {
    return dependencyType;
  }

  public void setDependencyType(final String dependencyType) {
    this.dependencyType = dependencyType;
  }

  public Set<License> getLicenses() {
    return licenses;
  }

  public void setLicenses(Set<License> licenses) {
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

  private int longToInt(Object number) {
    return ((Long) number).intValue();
  }

  private double bigDecimalToDouble(Object number) {
    return  number == null ? 0.0 : ((BigDecimal) number).doubleValue();
  }
}
