/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "coordinate_security")
public class ThirdPartyCoordinateSecurity
    implements HasStringId
{
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
}
