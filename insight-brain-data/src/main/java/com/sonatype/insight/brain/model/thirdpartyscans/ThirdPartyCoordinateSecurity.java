/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

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
  @Id
  @Column(name = "coordinate_security_id")
  private String id;

  @Column(name = "file_coordinate_id")
  private String fileCoordinateId;

  @Column(name = "ref_id")
  private String refId;

  @Column(name = "description")
  private String description;

  @Column(name = "link")
  private String link;

  @Column(name = "severity")
  private float severity;

  @Column(name = "fixed_by")
  private String fixedBy;

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

  public float getSeverity() {
    return severity;
  }

  public void setSeverity(float severity) {
    this.severity = severity;
  }

  public String getFixedBy() {
    return fixedBy;
  }

  public void setFixedBy(String fixedBy) {
    this.fixedBy = fixedBy;
  }
}
