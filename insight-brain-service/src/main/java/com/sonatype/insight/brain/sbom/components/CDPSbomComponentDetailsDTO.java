/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.util.List;

import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;

public class CDPSbomComponentDetailsDTO
    extends SbomComponentDTO
{
  private CDPSbomMetadataDTO metadata;

  private VulnerabilitySummaryDTO vulnerabilitySummaryDTO;

  private List<VulnerabilityDetailsDTO> disclosedVulnerabilities;

  private List<VulnerabilityDetailsDTO> sonatypeIdentifiedVulnerabilities;

  private List<String> occurrences;

  public CDPSbomComponentDetailsDTO() {
    // no op
  }

  public CDPSbomComponentDetailsDTO(String hash, String packageUrl, String name, String version) {
    super(new String[]{hash, packageUrl, name, version});
  }

  public VulnerabilitySummaryDTO getVulnerabilitySummary() {
    return vulnerabilitySummaryDTO;
  }

  public void setVulnerabilitySummary(VulnerabilitySummaryDTO vulnerabilitySummaryDTO) {
    this.vulnerabilitySummaryDTO = vulnerabilitySummaryDTO;
  }

  public List<VulnerabilityDetailsDTO> getDisclosedVulnerabilities() {
    return disclosedVulnerabilities;
  }

  public void setDisclosedVulnerabilities(List<VulnerabilityDetailsDTO> disclosedVulnerabilities) {
    this.disclosedVulnerabilities = disclosedVulnerabilities;
  }

  public List<VulnerabilityDetailsDTO> getSonatypeIdentifiedVulnerabilities() {
    return sonatypeIdentifiedVulnerabilities;
  }

  public void setSonatypeIdentifiedVulnerabilities(List<VulnerabilityDetailsDTO> sonatypeIdentifiedVulnerabilities) {
    this.sonatypeIdentifiedVulnerabilities = sonatypeIdentifiedVulnerabilities;
  }

  public List<String> getOccurrences() {
    return occurrences;
  }

  public void setOccurrences(final List<String> occurrences) {
    this.occurrences = occurrences;
  }

  public CDPSbomMetadataDTO getMetadata() {
    return metadata;
  }

  public void setMetadata(final CDPSbomMetadataDTO metadata) {
    this.metadata = metadata;
  }
}
