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
@Table(name = "sbom_metadata")
public class ThirdPartySbomMetadata
    implements HasStringId
{
  public ThirdPartySbomMetadata() {
    // noop
  }

  @Id
  @Column(name = "sbom_metadata_id")
  private String id;

  @Column(name = "third_party_file_id")
  private String thirdPartyFileId;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "file_name")
  private String filename;

  @Column(name = "serial_number")
  private String serialNumber;

  @Column(name = "application_version")
  private String applicationVersion;

  @Column(name = "spec")
  private String spec;

  @Column(name = "spec_format")
  private String specFormat;

  @Column(name = "spec_version")
  private String specVersion;

  @Column(name = "status")
  private String status;

  @Column(name = "created_at")
  private Date createdAt;

  @Column(name = "metadata_json")
  private String metadataJson;

  public ThirdPartySbomMetadata(
      String thirdPartyFileId,
      String applicationId,
      String applicationVersion,
      String filename,
      String serialNumber,
      String spec,
      String specFormat,
      String specVersion,
      String status,
      Date createdAt,
      String metadataJson)
  {

    this.thirdPartyFileId = thirdPartyFileId;
    this.applicationId = applicationId;
    this.applicationVersion = applicationVersion;
    this.filename = filename;
    this.serialNumber = serialNumber;
    this.spec = spec;
    this.specFormat = specFormat;
    this.specVersion = specVersion;
    this.status = status;
    this.createdAt = createdAt;
    this.metadataJson = metadataJson;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getThirdPartyFileId() {
    return thirdPartyFileId;
  }

  public void setThirdPartyFileId(String thirdPartyFileId) {
    this.thirdPartyFileId = thirdPartyFileId;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public String getApplicationVersion() {
    return applicationVersion;
  }

  public void setApplicationVersion(String applicationVersion) {
    this.applicationVersion = applicationVersion;
  }

  public String getSpec() {
    return spec;
  }

  public void setSpec(String spec) {
    this.spec = spec;
  }

  public String getSpecFormat() {
    return specFormat;
  }

  public void setSpecFormat(String specFormat) {
    this.specFormat = specFormat;
  }

  public String getSpecVersion() {
    return specVersion;
  }

  public void setSpecVersion(String specVersion) {
    this.specVersion = specVersion;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public String getMetadataJson() {
    return metadataJson;
  }

  public void setMetadataJson(final String metadataJson) {
    this.metadataJson = metadataJson;
  }
}
