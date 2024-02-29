/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sast;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

import static com.sonatype.insight.brain.model.sast.SastFindingSeverity.UNKNOWN;

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
  private int severity = UNKNOWN.ordinal();

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

  public int getSeverity() {
    return severity;
  }

  public void setSeverity(final int severity) {
    this.severity = severity;
  }

  public SastFindingSeverity getSeverityEnum() {
    return SastFindingSeverity.values()[severity];
  }

  public void setSeverity(final SastFindingSeverity severity) {
    setSeverity(severity.ordinal());
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
        ", severity='" + severity + '\'' +
        ", confidence='" + confidence + '\'' +
        ", ruleName='" + ruleName + '\'' +
        ", description='" + description + '\'' +
        '}';
  }
}
