/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ThirdPartySbomMetadataSummaryDTO
{
  private String applicationVersion;

  private String spec;

  private String specVersion;

  @JsonSerialize(using = ISODateSerializer.class)
  private Date importDate;

  private int none;

  private int low;

  private int medium;

  private int high;

  private int critical;

  private Boolean isValid;

  private double releaseStatusPercentage;

  public ThirdPartySbomMetadataSummaryDTO() {
    // for Jackson
  }

  public ThirdPartySbomMetadataSummaryDTO(Object[] array) {
    applicationVersion = (String) array[0];
    spec = (String) array[1];
    specVersion = (String) array[2];
    importDate = toDate(array[3]);
    isValid = (Boolean) array[4];

    none = longToInt(array[5]);
    low = longToInt(array[6]);
    medium = longToInt(array[7]);
    high = longToInt(array[8]);
    critical = longToInt(array[9]);
    releaseStatusPercentage = ((Number) array[11]).doubleValue();
  }

  public String getApplicationVersion() {
    return applicationVersion;
  }

  public String getSpec() {
    return spec;
  }

  public String getSpecVersion() {
    return specVersion;
  }

  public Date getImportDate() {
    return importDate;
  }

  public int getNone() {
    return none;
  }

  public int getLow() {
    return low;
  }

  public int getMedium() {
    return medium;
  }

  public int getHigh() {
    return high;
  }

  public int getCritical() {
    return critical;
  }

  public boolean getIsValid() {
    return isValid == null || isValid;
  }

  public void setApplicationVersion(final String applicationVersion) {
    this.applicationVersion = applicationVersion;
  }

  public void setSpec(final String spec) {
    this.spec = spec;
  }

  public void setSpecVersion(final String specVersion) {
    this.specVersion = specVersion;
  }

  public void setImportDate(final Date importDate) {
    this.importDate = importDate;
  }

  public void setNone(final int none) {
    this.none = none;
  }

  public void setLow(final int low) {
    this.low = low;
  }

  public void setMedium(final int medium) {
    this.medium = medium;
  }

  public void setHigh(final int high) {
    this.high = high;
  }

  public void setCritical(final int critical) {
    this.critical = critical;
  }

  public void setIsValid(Boolean isValid) {
    this.isValid = isValid;
  }

  private int longToInt(Object number) {
    return ((Number) number).intValue();
  }

  private Date toDate(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Date) {
      return (Date) value;
    }
    if (value instanceof LocalDateTime) {
      return Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant());
    }
    throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to Date");
  }

  public double getReleaseStatusPercentage() {
    return releaseStatusPercentage;
  }

  public void setReleaseStatusPercentage(final double releaseStatusPercentage) {
    this.releaseStatusPercentage = releaseStatusPercentage;
  }
}
