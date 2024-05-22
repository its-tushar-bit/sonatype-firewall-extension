/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

public class VulnerabilitiesThreadLevelMetricDTO
{
  private long low;

  private long lowAnnotated;

  private long lowUnannotated;

  private long medium;

  private long mediumAnnotated;

  private long mediumUnannotated;

  private long high;

  private long highAnnotated;

  private long highUnannotated;

  private long critical;

  private long criticalAnnotated;

  private long criticalUnannotated;

  private long totalVulnerabilities;

  private long totalVulnerabilitiesAnnotated;

  private long totalVulnerabilitiesUnannotated;

  public VulnerabilitiesThreadLevelMetricDTO() {
    // Default with all values as zero
  }

  public VulnerabilitiesThreadLevelMetricDTO(Object[] array) {
    low = (Long) array[0];
    lowAnnotated = (Long) array[1];
    lowUnannotated = low - lowAnnotated;

    medium = (Long) array[2];
    mediumAnnotated = (Long) array[3];
    mediumUnannotated = medium - mediumAnnotated;

    high = (Long) array[4];
    highAnnotated = (Long) array[5];
    highUnannotated = high - highAnnotated;

    critical = (Long) array[6];
    criticalAnnotated = (Long) array[7];
    criticalUnannotated = critical - criticalAnnotated;

    totalVulnerabilities = low + medium + high + critical;
    totalVulnerabilitiesAnnotated = lowAnnotated + mediumAnnotated + highAnnotated + criticalAnnotated;
    totalVulnerabilitiesUnannotated = totalVulnerabilities - totalVulnerabilitiesAnnotated;
  }

  public long getLow() {
    return low;
  }

  public void setLow(long low) {
    this.low = low;
  }

  public long getLowAnnotated() {
    return lowAnnotated;
  }

  public void setLowAnnotated(long lowAnnotated) {
    this.lowAnnotated = lowAnnotated;
  }

  public long getLowUnannotated() {
    return lowUnannotated;
  }

  public void setLowUnannotated(long lowUnannotated) {
    this.lowUnannotated = lowUnannotated;
  }

  public long getMedium() {
    return medium;
  }

  public void setMedium(long medium) {
    this.medium = medium;
  }

  public long getMediumAnnotated() {
    return mediumAnnotated;
  }

  public void setMediumAnnotated(long mediumAnnotated) {
    this.mediumAnnotated = mediumAnnotated;
  }

  public long getMediumUnannotated() {
    return mediumUnannotated;
  }

  public void setMediumUnannotated(long mediumUnannotated) {
    this.mediumUnannotated = mediumUnannotated;
  }

  public long getHigh() {
    return high;
  }

  public void setHigh(long high) {
    this.high = high;
  }

  public long getHighAnnotated() {
    return highAnnotated;
  }

  public void setHighAnnotated(long highAnnotated) {
    this.highAnnotated = highAnnotated;
  }

  public long getHighUnannotated() {
    return highUnannotated;
  }

  public void setHighUnannotated(long highUnannotated) {
    this.highUnannotated = highUnannotated;
  }

  public long getCritical() {
    return critical;
  }

  public void setCritical(long critical) {
    this.critical = critical;
  }

  public long getCriticalAnnotated() {
    return criticalAnnotated;
  }

  public void setCriticalAnnotated(long criticalAnnotated) {
    this.criticalAnnotated = criticalAnnotated;
  }

  public long getCriticalUnannotated() {
    return criticalUnannotated;
  }

  public void setCriticalUnannotated(long criticalUnannotated) {
    this.criticalUnannotated = criticalUnannotated;
  }

  public long getTotalVulnerabilities() {
    return totalVulnerabilities;
  }

  public void setTotalVulnerabilities(long totalVulnerabilities) {
    this.totalVulnerabilities = totalVulnerabilities;
  }

  public long getTotalVulnerabilitiesAnnotated() {
    return totalVulnerabilitiesAnnotated;
  }

  public void setTotalVulnerabilitiesAnnotated(long totalVulnerabilitiesAnnotated) {
    this.totalVulnerabilitiesAnnotated = totalVulnerabilitiesAnnotated;
  }

  public long getTotalVulnerabilitiesUnannotated() {
    return totalVulnerabilitiesUnannotated;
  }

  public void setTotalVulnerabilitiesUnannotated(long totalVulnerabilitiesUnannotated) {
    this.totalVulnerabilitiesUnannotated = totalVulnerabilitiesUnannotated;
  }
}
