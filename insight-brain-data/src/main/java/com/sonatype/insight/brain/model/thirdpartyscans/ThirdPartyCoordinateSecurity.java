/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "coordinate_security")
public class ThirdPartyCoordinateSecurity
    implements HasStringId
{
  private static final Logger LOG = LoggerFactory.getLogger(ThirdPartyCoordinateSecurity.class);

  private static final ObjectMapper VULN_IDS_MAPPER = new ObjectMapper();

  public ThirdPartyCoordinateSecurity() {
  }

  public ThirdPartyCoordinateSecurity(
      String fileCoordinateId,
      String refId,
      String sbomMetadataId,
      String description,
      String link,
      double severity,
      String fixedBy)
  {

    this.fileCoordinateId = fileCoordinateId;
    this.refId = refId;
    this.sbomMetadataId = sbomMetadataId;
    this.description = description;
    this.link = link;
    this.severity = severity;
    this.fixedBy = fixedBy;
  }

  @Id
  @Column(name = "coordinate_security_id")
  private String id;

  @Column(name = "file_coordinate_id")
  private String fileCoordinateId;

  // This column was denormalized because of a performance issue see SBOM-272.
  @Column(name = "sbom_metadata_id")
  private String sbomMetadataId;

  @Column(name = "ref_id")
  private String refId;

  @Column(name = "description")
  private String description;

  @Column(name = "link")
  private String link;

  @Column(name = "severity")
  private double severity;

  @Column(name = "fixed_by")
  private String fixedBy;

  @Column(name = "vulnerability_source")
  private String vulnerabilitySource;

  @Column(name = "severity_description")
  private String severityDescription;

  @Column(name = "attack_vector")
  private String attackVector;

  @Column(name = "rating_method")
  private String ratingMethod;

  @Column(name = "cwes")
  private String cwes;

  @Column(name = "recommendations")
  private String recommendations;

  @Column(name = "advisories")
  private String advisories;

  @Column(name = "identification_sources")
  private String identificationSources;

  @Column(name = "research_type")
  private String researchType;

  @Column(name = "detection_type")
  private String detectionType;

  @Column(name = "vuln_ids")
  private String vulnIds;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getFileCoordinateId() {
    return fileCoordinateId;
  }

  public void setFileCoordinateId(String fileCoordinateId) {
    this.fileCoordinateId = fileCoordinateId;
  }

  public String getSbomMetadataId() {
    return sbomMetadataId;
  }

  public void setSbomMetadataId(final String sbomMetadataId) {
    this.sbomMetadataId = sbomMetadataId;
  }

  public String getRefId() {
    return refId;
  }

  public void setRefId(String refId) {
    this.refId = refId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public double getSeverity() {
    return severity;
  }

  public void setSeverity(double severity) {
    this.severity = BigDecimal.valueOf(severity).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
  }

  public String getFixedBy() {
    return fixedBy;
  }

  public void setFixedBy(String fixedBy) {
    this.fixedBy = fixedBy;
  }

  public String getVulnerabilitySource() {
    return vulnerabilitySource;
  }

  public void setVulnerabilitySource(String vulnerabilitySource) {
    this.vulnerabilitySource = vulnerabilitySource;
  }

  public String getSeverityDescription() {
    return severityDescription;
  }

  public void setSeverityDescription(String severityDescription) {
    this.severityDescription = severityDescription;
  }

  public String getAttackVector() {
    return attackVector;
  }

  public void setAttackVector(String attackVector) {
    this.attackVector = attackVector;
  }

  public String getRatingMethod() {
    return ratingMethod;
  }

  public void setRatingMethod(String ratingMethod) {
    this.ratingMethod = ratingMethod;
  }

  public String getCwes() {
    return cwes;
  }

  public void setCwes(String cwes) {
    this.cwes = cwes;
  }

  public String getRecommendations() {
    return recommendations;
  }

  public void setRecommendations(String recommendations) {
    this.recommendations = recommendations;
  }

  public String getAdvisories() {
    return advisories;
  }

  public void setAdvisories(String advisories) {
    this.advisories = advisories;
  }

  public String getIdentificationSources() {
    return identificationSources;
  }

  public void setIdentificationSources(String identificationSources) {
    this.identificationSources = identificationSources;
  }

  public void addIdentificationSource(String identificationSource) {
    if (this.identificationSources == null) {
      setIdentificationSources(identificationSource);
    }
    else if (!this.identificationSources.contains(identificationSource)) {
      setIdentificationSources(getIdentificationSources() + "," + identificationSource);
    }
  }

  public String getResearchType() {
    return researchType;
  }

  public void setResearchType(final String researchType) {
    this.researchType = researchType;
  }

  public String getDetectionType() {
    return detectionType;
  }

  public void setDetectionType(final String detectionType) {
    this.detectionType = detectionType;
  }

  public String getVulnIds() {
    return vulnIds;
  }

  public void setVulnIds(final String vulnIds) {
    this.vulnIds = vulnIds;
  }

  public List<String> getVulnIdsParsed() {
    if (StringUtils.isBlank(vulnIds)) {
      return Collections.emptyList();
    }
    try {
      return Arrays.stream(VULN_IDS_MAPPER.readValue(vulnIds, String[].class))
          .filter(StringUtils::isNotBlank)
          .collect(Collectors.toList());
    }
    catch (Exception e) {
      LOG.warn("Failed to parse vuln_ids for coordinate_security id={}; returning empty list", id, e);
      return Collections.emptyList();
    }
  }

  public void setVulnIdsFromList(final List<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return;
    }
    List<String> cleaned = ids.stream()
        .filter(StringUtils::isNotBlank)
        .filter(id -> !id.equalsIgnoreCase(refId))
        .distinct()
        .collect(Collectors.toList());
    if (cleaned.isEmpty()) {
      return;
    }
    try {
      this.vulnIds = VULN_IDS_MAPPER.writeValueAsString(cleaned);
    }
    catch (JsonProcessingException e) {
      LOG.error("Failed to serialize vuln_ids for coordinate_security refId={} — alias references"
          + " will be missing from this row's exports", refId, e);
    }
  }
}
