/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  @Column(name = "sbom_version")
  private String sbomVersion;

  @Column(name = "spec")
  private String spec;

  @Column(name = "spec_format")
  private String specFormat;

  @Column(name = "spec_version")
  private String specVersion;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private ThirdPartySbomMetadataStatus status;

  @Column(name = "created_at")
  private Date createdAt;

  @Column(name = "metadata_json")
  private String metadataJson;

  @Column(name = "scan_type")
  private String scanType;

  @Column(name = "is_valid")
  private Boolean isValid;

  @Column(name = "original_binary_file_name")
  private String originalBinaryFileName;

  @Column(name = "extended_profile_elements")
  private String extendedProfileElements;

  @Column(name = "root_component_ref")
  private String rootComponentRef;

  public ThirdPartySbomMetadata(
      String thirdPartyFileId,
      String applicationId,
      String sbomVersion,
      String filename,
      String serialNumber,
      String spec,
      String specFormat,
      String specVersion,
      ThirdPartySbomMetadataStatus status,
      Date createdAt,
      String metadataJson,
      String scanType,
      Boolean isValid,
      String originalBinaryFileName)
  {

    this.thirdPartyFileId = thirdPartyFileId;
    this.applicationId = applicationId;
    this.sbomVersion = sbomVersion;
    this.filename = filename;
    this.serialNumber = serialNumber;
    this.spec = spec;
    this.specFormat = specFormat;
    this.specVersion = specVersion;
    this.status = status;
    this.createdAt = createdAt;
    this.metadataJson = metadataJson;
    this.scanType = scanType;
    this.isValid = isValid;
    this.originalBinaryFileName = originalBinaryFileName;
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

  public String getSbomVersion() {
    return sbomVersion;
  }

  public void setSbomVersion(String sbomVersion) {
    this.sbomVersion = sbomVersion;
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

  public ThirdPartySbomMetadataStatus getStatus() {
    return status;
  }

  public void setStatus(ThirdPartySbomMetadataStatus status) {
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

  public String getScanType() {
    return scanType;
  }

  public void setScanType(final String scanType) {
    this.scanType = scanType;
  }

  public boolean getIsValid() {
    return isValid == null || isValid;
  }

  public void setIsValid(final Boolean isValid) {
    this.isValid = isValid == null || isValid;
  }

  public String getOriginalBinaryFileName() {
    return originalBinaryFileName;
  }

  public void setOriginalBinaryFileName(final String originalBinaryFileName) {
    this.originalBinaryFileName = originalBinaryFileName;
  }

  public String getExtendedProfileElements() {
    return extendedProfileElements;
  }

  public void setExtendedProfileElements(final String extendedProfileElements) {
    this.extendedProfileElements = extendedProfileElements;
  }

  public String getRootComponentRef() {
    return rootComponentRef;
  }

  public void setRootComponentRef(final String rootComponentRef) {
    this.rootComponentRef = rootComponentRef;
  }
}
