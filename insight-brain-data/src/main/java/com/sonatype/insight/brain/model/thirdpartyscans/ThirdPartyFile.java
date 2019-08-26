/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "third_party_file")
public class ThirdPartyFile
    implements HasStringId
{
  public ThirdPartyFile() {
    //noop
  }

  public ThirdPartyFile(String hash, String filename, String image, Date created) {
    this.hash = hash;
    this.filename = filename;
    this.image = image;
    this.created = created;
  }

  @Id
  @Column(name = "third_party_file_id")
  private String id;

  @Column(name = "hash")
  private String hash;

  @Column(name = "filename")
  private String filename;

  @Column(name = "image")
  private String image;

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

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }
}
