/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

public class BomPageSbomSummaryDTO
{
  private Long none;

  private Long low;

  private Long medium;

  private Long high;

  private Long critical;

  private SbomDependencyTypeDTO dependencyType;

  private Double annotatedPercentage;

  public BomPageSbomSummaryDTO() {
    // for Jackson
  }

  public BomPageSbomSummaryDTO(Object[] array) {
    none = Long.parseLong(String.valueOf(array[0]));
    low = Long.parseLong(String.valueOf(array[1]));
    medium = Long.parseLong(String.valueOf(array[2]));
    high = Long.parseLong(String.valueOf(array[3]));
    critical = Long.parseLong(String.valueOf(array[4]));
    annotatedPercentage = array[5] != null ? ((Number)array[5]).doubleValue() : null;
  }

  public SbomDependencyTypeDTO getDependencyType() {
    return dependencyType;
  }

  public void setDependencyType(final SbomDependencyTypeDTO dependencyType) {
    this.dependencyType = dependencyType;
  }

  public Long getNone() {
    return none;
  }

  public Long getLow() {
    return low;
  }

  public Long getMedium() {
    return medium;
  }

  public Long getHigh() {
    return high;
  }

  public Long getCritical() {
    return critical;
  }

  public Double getAnnotatedPercentage() {
    return annotatedPercentage;
  }

  public void setLow(final Long low) {
    this.low = low;
  }

  public void setMedium(final Long medium) {
    this.medium = medium;
  }

  public void setCritical(final Long critical) {
    this.critical = critical;
  }

  public void setAnnotatedPercentage(final Double annotatedPercentage) {
    this.annotatedPercentage = annotatedPercentage;
  }

  public void setAllValuesToNull() {
    this.setDependencyType(null);
    this.setAnnotatedPercentage(null);
    this.setNone(null);
    this.setCritical(null);
    this.setMedium(null);
    this.setLow(null);
    this.setHigh(null);
  }

  public void setNone(final Long none) {
    this.none = none;
  }

  public void setHigh(final Long high) {
    this.high = high;
  }
}
