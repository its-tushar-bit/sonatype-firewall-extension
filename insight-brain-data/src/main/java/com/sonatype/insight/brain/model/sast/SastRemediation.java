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
@Table(name = "sast_remediation")
public class SastRemediation
    implements HasStringId
{
  @Id
  @Column(name = "sast_remediation_id")
  private String id;

  @Column(name = "sast_finding_id")
  private String sastFindingId;

  @Column(name = "content")
  private String content;

  public SastRemediation() {
  }

  public SastRemediation(final String findingId, final String content) {
    this.sastFindingId = findingId;
    this.content = content;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getSastFindingId() {
    return sastFindingId;
  }

  public void setSastFindingId(final String sastFindingId) {
    this.sastFindingId = sastFindingId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(final String content) {
    this.content = content;
  }

  @Override
  public String toString() {
    return "SastRemediation{" +
        "id='" + id + '\'' +
        ", sastFindingId='" + sastFindingId + '\'' +
        ", content='" + content + '\'' +
        '}';
  }
}
