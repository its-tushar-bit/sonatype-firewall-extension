/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "third_party_file")
public class ThirdPartyFile
    implements HasStringId
{
  public ThirdPartyFile() {
    // noop
  }

  public ThirdPartyFile(String filename, Date created) {
    this.filename = filename;
    this.created = created;
  }

  @Id
  @Column(name = "third_party_file_id")
  private String id;

  @Column(name = "filename")
  private String filename;

  @Column(name = "create_time")
  private Date created;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }
}
