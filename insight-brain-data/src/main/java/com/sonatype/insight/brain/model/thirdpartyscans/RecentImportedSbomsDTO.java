/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class RecentImportedSbomsDTO
{
  private String sbomVersion;

  @JsonIgnore
  private String applicationId;

  private String specification;

  @JsonSerialize(using = ISODateSerializer.class)
  private Date importDate;

  private int lowCount;

  private int mediumCount;

  private int highCount;

  private int criticalCount;

  private String applicationName;

  private String publicApplicationId;

  public RecentImportedSbomsDTO() {
  }

  public RecentImportedSbomsDTO(Object[] array) {
    applicationId = String.valueOf(array[0]);
    sbomVersion = String.valueOf(array[1]);
    specification = String.valueOf(array[2]);
    importDate = toDate(array[3]);
    lowCount = longToInt(array[4]);
    mediumCount = longToInt(array[5]);
    highCount = longToInt(array[6]);
    criticalCount = longToInt(array[7]);
  }

  public String getSbomVersion() {
    return sbomVersion;
  }

  public void setSbomVersion(final String sbomVersion) {
    this.sbomVersion = sbomVersion;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
  }

  public String getSpecification() {
    return specification;
  }

  public void setSpecification(final String specification) {
    this.specification = specification;
  }

  public Date getImportDate() {
    return importDate;
  }

  public void setImportDate(final Date importDate) {
    this.importDate = importDate;
  }

  public int getLowCount() {
    return lowCount;
  }

  public void setLowCount(final int lowCount) {
    this.lowCount = lowCount;
  }

  public int getMediumCount() {
    return mediumCount;
  }

  public void setMediumCount(final int mediumCount) {
    this.mediumCount = mediumCount;
  }

  public int getHighCount() {
    return highCount;
  }

  public void setHighCount(final int highCount) {
    this.highCount = highCount;
  }

  public int getCriticalCount() {
    return criticalCount;
  }

  public void setCriticalCount(final int criticalCount) {
    this.criticalCount = criticalCount;
  }

  private int longToInt(Object number) {
    return ((Number) number).intValue();
  }

  private Date toDate(Object value) {
    if (value instanceof Date) {
      return (Date) value;
    }
    if (value instanceof LocalDateTime) {
      return Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant());
    }
    return null;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
  }

  public String getPublicApplicationId() {
    return publicApplicationId;
  }

  public void setPublicApplicationId(final String publicApplicationId) {
    this.publicApplicationId = publicApplicationId;
  }
}
