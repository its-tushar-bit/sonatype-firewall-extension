/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sast;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "sast_finding")
public class SastFinding
    implements HasStringId
{
  @Id
  @Column(name = "sast_finding_id")
  private String id;

  @Column(name = "sast_scan_id")
  private String sastScanId;

  @Column(name = "coordinate")
  private String coordinate;

  @Column(name = "line_number")
  private Integer lineNumber;

  @Column(name = "cwe")
  private String cwe;

  @Column(name = "severity")
  private int severityId = SastFindingSeverity.NONE.getId();

  @Column(name = "confidence")
  private int confidence = SastFindingConfidence.LOW.ordinal();

  @Column(name = "rule_name")
  private String ruleName;

  @Column(name = "description")
  private String description;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getSastScanId() {
    return sastScanId;
  }

  public void setSastScanId(final String sastScanId) {
    this.sastScanId = sastScanId;
  }

  public String getCoordinate() {
    return coordinate;
  }

  public void setCoordinate(final String coordinate) {
    this.coordinate = coordinate;
  }

  public Integer getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(final Integer lineNumber) {
    this.lineNumber = lineNumber;
  }

  public String getCwe() {
    return cwe;
  }

  public void setCwe(final String cwe) {
    this.cwe = cwe;
  }

  public int getSeverityId() {
    return severityId;
  }

  public void setSeverityId(final int severityId) {
    this.severityId = severityId;
  }

  public SastFindingSeverity getSeverity() {
    return SastFindingSeverity.getById(severityId);
  }

  public void setSeverity(final SastFindingSeverity severity) {
    setSeverityId(severity.getId());
  }

  public int getConfidence() {
    return confidence;
  }

  public void setConfidence(final int confidence) {
    this.confidence = confidence;
  }

  public SastFindingConfidence getConfidenceEnum() {
    return SastFindingConfidence.values()[confidence];
  }

  public void setConfidence(final SastFindingConfidence confidence) {
    setConfidence(confidence.ordinal());
  }

  public String getRuleName() {
    return ruleName;
  }

  public void setRuleName(final String ruleName) {
    this.ruleName = ruleName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "SastFinding{" +
        "id='" + id + '\'' +
        ", sastScanId='" + sastScanId + '\'' +
        ", coordinate='" + coordinate + '\'' +
        ", lineNumber=" + lineNumber +
        ", cwe='" + cwe + '\'' +
        ", severityId='" + severityId + '\'' +
        ", confidence='" + confidence + '\'' +
        ", ruleName='" + ruleName + '\'' +
        ", description='" + description + '\'' +
        '}';
  }
}
