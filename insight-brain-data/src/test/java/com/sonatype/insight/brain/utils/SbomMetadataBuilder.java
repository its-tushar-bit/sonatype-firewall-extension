/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.lang3.RandomStringUtils;

public class SbomMetadataBuilder
{
  ThirdPartyFileDAO thirdPartyFileDAO;

  ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private Date createdAt;

  private String thirdPartyFileId;

  private String applicationId;

  private String filename;

  private String serialNumber;

  private String sbomVersion;

  private String spec;

  private String specFormat;

  private String specVersion;

  private String status;

  private String metadataJson;

  public SbomMetadataBuilder(DAOFactory daoFactory) {
    thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
    thirdPartySbomMetadataDAO = daoFactory.createThirdPartySbomMetadataDAO();

    ThirdPartyFile thirdPartyFile = new ThirdPartyFile("third-party-file", new Date());
    thirdPartyFileDAO.insert(thirdPartyFile);

    this.createdAt = new Date();
    this.thirdPartyFileId = thirdPartyFile.getId();
    this.applicationId = RandomStringUtils.random(10, true, true);
    this.filename = RandomStringUtils.random(10, true, true);
    this.serialNumber = RandomStringUtils.random(10, true, true);
    this.sbomVersion = RandomStringUtils.random(10, true, true);
    this.spec = RandomStringUtils.random(10, true, true);
    this.specFormat = SbomFormat.XML.toString();
    this.specVersion = RandomStringUtils.random(10, true, true);
    //this.status = SbomStatus.ACTIVE.toString();
    this.status = "ACTIVE";
    this.metadataJson = RandomStringUtils.random(50, true, true);
  }

  public static SbomMetadataBuilder newSbomMetadataBuilder(DAOFactory daoFactory) {
    return new SbomMetadataBuilder(daoFactory);
  }

  public SbomMetadataBuilder withCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public SbomMetadataBuilder withApplicationId(String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  public SbomMetadataBuilder withFilename(String filename) {
    this.filename = filename;
    return this;
  }

  public SbomMetadataBuilder withSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
    return this;
  }

  public SbomMetadataBuilder withSbomVersion(String sbomVersion) {
    this.sbomVersion = sbomVersion;
    return this;
  }

  public SbomMetadataBuilder withSpec(String spec) {
    this.spec = spec;
    return this;
  }

  public SbomMetadataBuilder withJsonSpecFormat() {
    this.specFormat = SbomFormat.JSON.toString();
    return this;
  }

  public SbomMetadataBuilder withXmlSpecFormat() {
    this.specFormat = SbomFormat.XML.toString();
    return this;
  }

  public SbomMetadataBuilder withStatus(String status) {
    this.status = status;
    return this;
  }

  public SbomMetadataBuilder withMetadataJson(String metadataJson) {
    this.metadataJson = metadataJson;
    return this;
  }

  public ThirdPartySbomMetadata build() {
    ThirdPartySbomMetadata thirdPartySbomMetadata = new ThirdPartySbomMetadata(
        thirdPartyFileId, applicationId, sbomVersion, filename, serialNumber, spec, specFormat, specVersion, status,
        createdAt, metadataJson
    );
    thirdPartySbomMetadataDAO.insert(thirdPartySbomMetadata);

    return thirdPartySbomMetadata;
  }
}
