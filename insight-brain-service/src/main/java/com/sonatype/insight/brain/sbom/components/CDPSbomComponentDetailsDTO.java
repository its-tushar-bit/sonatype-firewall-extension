/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.util.List;

import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomPolicyViolationSummaryDTO;

public class CDPSbomComponentDetailsDTO
    extends SbomComponentDTO
{
  private CDPSbomMetadataDTO metadata;

  private VulnerabilitySummaryDTO vulnerabilitySummaryDTO;

  private List<VulnerabilityDetailsDTO> disclosedVulnerabilities;

  private List<VulnerabilityDetailsDTO> sonatypeIdentifiedVulnerabilities;

  private List<String> occurrences;

  private String matchState;

  private SbomPolicyViolationSummaryDTO policyViolationSummary;

  public CDPSbomComponentDetailsDTO() {
    // no op
  }

  public CDPSbomComponentDetailsDTO(
      String hash,
      String packageUrl,
      String name,
      String version,
      String format,
      String displayName,
      String componentRef,
      String fileCoordinateId)
  {
    super(new String[]{hash, packageUrl, name, version, format, displayName});
    setComponentRef(componentRef);
    // Deprecated: for old sbom versions
    if (componentRef == null) {
      setFileCoordinateId(fileCoordinateId);
    }
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

  public String getMatchState() {
    return matchState;
  }

  public void setMatchState(final String matchState) {
    this.matchState = matchState;
  }

  public SbomPolicyViolationSummaryDTO getPolicyViolationSummary() {
    return policyViolationSummary;
  }

  public void setPolicyViolationSummary(final SbomPolicyViolationSummaryDTO policyViolationSummary) {
    this.policyViolationSummary = policyViolationSummary;
  }
}
