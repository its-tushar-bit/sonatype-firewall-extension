/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.util.HashMap;
import java.util.Map;

public class SbomComponentInfoTelemetry
{
  public static final String ATTRIBUTE_NAME = "sbom_data_summary";

  private String contentType = null;

  private String spec = null;

  private String specVersion = null;

  private int purlCount = 0;

  private int cpeCount = 0;

  private int swidCount = 0;

  private int hashCount = 0;

  private int coordinateCount = 0;

  private int vulnerabilitiesWithVexInfoCount;

  private boolean hasDependencies;

  private int invalidLicensesCount;

  private int validLicensesCount;

  public Map<String, Integer> ecosystemCount = new HashMap<>();

  public int validationErrorsCount;

  public SbomComponentInfoTelemetry() {
  }

  public SbomComponentInfoTelemetry(
      int purlCount,
      int cpeCount,
      int swidCount,
      int hashCount,
      int coordinateCount)
  {
    this.purlCount = purlCount;
    this.cpeCount = cpeCount;
    this.swidCount = swidCount;
    this.hashCount = hashCount;
    this.coordinateCount = coordinateCount;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(final String contentType) {
    this.contentType = contentType;
  }

  public String getSpec() {
    return spec;
  }

  public void setSpec(final String spec) {
    this.spec = spec;
  }

  public String getSpecVersion() {
    return specVersion;
  }

  public void setSpecVersion(final String specVersion) {
    this.specVersion = specVersion;
  }

  public int getPurlCount() {
    return purlCount;
  }

  public void incrementPurlCount() {
    purlCount++;
  }

  public int getCpeCount() {
    return cpeCount;
  }

  public void incrementCpeCount() {
    cpeCount++;
  }

  public int getSwidCount() {
    return swidCount;
  }

  public void incrementSwidCount() {
    swidCount++;
  }

  public int getHashCount() {
    return hashCount;
  }

  public void incrementHashCount() {
    hashCount++;
  }

  public int getCoordinateCount() {
    return coordinateCount;
  }

  public void incrementCoordinateCount() {
    coordinateCount++;
  }

  public int getVulnerabilitiesWithVexInfoCount() {
    return vulnerabilitiesWithVexInfoCount;
  }

  public void incrementVulnerabilitiesWithVexInfoCount() {
    this.vulnerabilitiesWithVexInfoCount++;
  }

  public boolean getHasDependencies() {
    return hasDependencies;
  }

  public void setHasDependencies(final boolean hasDependencies) {
    this.hasDependencies = hasDependencies;
  }

  public int getInvalidLicensesCount() {
    return invalidLicensesCount;
  }

  public void setInvalidLicensesCount(final int invalidLicensesCount) {
    this.invalidLicensesCount = invalidLicensesCount;
  }

  public void incrementInvalidLicensesCount() {
    this.invalidLicensesCount++;
  }

  public int getValidLicensesCount() {
    return validLicensesCount;
  }

  public void setValidLicensesCount(final int validLicensesCount) {
    this.validLicensesCount = validLicensesCount;
  }

  public void incrementValidLicensesCount() {
    this.validLicensesCount++;
  }

  public Map<String, Integer> getEcosystemCount() {
    return ecosystemCount;
  }

  public void incrementEcosystemCount(String ecosystem) {
    ecosystemCount.put(ecosystem, ecosystemCount.getOrDefault(ecosystem, 0) + 1);
  }

  public int getValidationErrorsCount() {
    return validationErrorsCount;
  }

  public void setValidationErrorsCount(final int validationErrorsCount) {
    this.validationErrorsCount = validationErrorsCount;
  }
}
