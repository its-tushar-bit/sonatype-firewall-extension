/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class BomPageMetadataDTO
{
  public List<String> author;

  public List<String> manufacturer;

  public List<String> supplier;

  public List<String> person;

  public List<String> organization;

  public String specification;

  public String specVersion;

  public String fileFormat;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date createdAt;

  public String scanId;

  public boolean isValid;

  public String originalFile;

  public boolean displayNameSortingEnabled;

  public BomPageMetadataDTO(
      final List<String> author,
      final List<String> manufacturer,
      final List<String> supplier,
      final List<String> person,
      final List<String> organization,
      final String specification,
      final String specVersion,
      final String fileFormat,
      final Date createdAt,
      final String scanId,
      final boolean isValid,
      final String originalFile,
      final boolean displayNameSortingEnabled)
  {
    this.author = author;
    this.manufacturer = manufacturer;
    this.supplier = supplier;
    this.person = person;
    this.organization = organization;
    this.specification = specification;
    this.specVersion = specVersion;
    this.fileFormat = fileFormat;
    this.createdAt = createdAt;
    this.scanId = scanId;
    this.isValid = isValid;
    this.originalFile = originalFile;
    this.displayNameSortingEnabled = displayNameSortingEnabled;
  }

  public BomPageMetadataDTO() {
    // For Jackson
  }
}
