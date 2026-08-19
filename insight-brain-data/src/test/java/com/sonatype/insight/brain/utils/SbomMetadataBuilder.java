/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.scan.file.SbomFormat;

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

  private ThirdPartySbomMetadataStatus status;

  private String metadataJson;

  private String scanType;

  private String originalBinaryFileName;

  private Boolean isValid;

  public SbomMetadataBuilder(DAOFactory daoFactory) {
    thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
    thirdPartySbomMetadataDAO = daoFactory.createThirdPartySbomMetadataDAO();

    ThirdPartyFile thirdPartyFile = new ThirdPartyFile("third-party-file", new Date());
    thirdPartyFileDAO.insert(thirdPartyFile);
    this.createdAt = new Date();
    this.thirdPartyFileId = thirdPartyFile.getId();
    this.applicationId = TemporaryEntity.uuid().substring(0, 10);
    this.filename = TemporaryEntity.uuid().substring(0, 10);
    this.serialNumber = TemporaryEntity.uuid().substring(0, 10);
    this.sbomVersion = TemporaryEntity.uuid().substring(0, 10);
    this.spec = TemporaryEntity.uuid().substring(0, 10);
    this.specFormat = SbomFormat.XML.toString();
    this.specVersion = TemporaryEntity.uuid().substring(0, 10);
    this.status = ThirdPartySbomMetadataStatus.ACTIVE;
    this.metadataJson = buildMetadataJson();
    this.scanType = "SBOM";
    this.isValid = true;
    this.originalBinaryFileName = "example-original-binary-file-name";
  }

  public static SbomMetadataBuilder newSbomMetadataBuilder(DAOFactory daoFactory) {
    return new SbomMetadataBuilder(daoFactory);
  }

  public static SbomMetadataBuilder newSbomSPDXMetadataBuilder(DAOFactory daoFactory) {
    SbomMetadataBuilder sbomMetaDataBuilderWithSPDX = new SbomMetadataBuilder(daoFactory);
    sbomMetaDataBuilderWithSPDX.metadataJson = buildSPDXMetadataJson();
    System.out.println(sbomMetaDataBuilderWithSPDX.metadataJson);
    return sbomMetaDataBuilderWithSPDX;
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

  public SbomMetadataBuilder withStatus(ThirdPartySbomMetadataStatus status) {
    this.status = status;
    return this;
  }

  public SbomMetadataBuilder withMetadataJson(String metadataJson) {
    this.metadataJson = metadataJson;
    return this;
  }

  public SbomMetadataBuilder withThirdPartyFileId(String thirdPartyFileId) {
    this.thirdPartyFileId = thirdPartyFileId;
    return this;
  }

  public SbomMetadataBuilder withSpecVersion(String specVersion) {
    this.specVersion = specVersion;
    return this;
  }

  public SbomMetadataBuilder withScanType(String scanType) {
    this.scanType = scanType;
    return this;
  }

  public SbomMetadataBuilder withIsValid(Boolean isValid) {
    this.isValid = isValid;
    return this;
  }

  public ThirdPartySbomMetadata build() {
    ThirdPartySbomMetadata thirdPartySbomMetadata = new ThirdPartySbomMetadata(
        thirdPartyFileId, applicationId, sbomVersion, filename, serialNumber, spec, specFormat, specVersion, status,
        createdAt, metadataJson, scanType, isValid, originalBinaryFileName);
    thirdPartySbomMetadataDAO.insert(thirdPartySbomMetadata);

    return thirdPartySbomMetadata;
  }

  public static String buildMetadataJson() {
    String json =
        "{\"created\":\"2020-01-01T00:00:00Z\",\"creators\":[{\"type\":\"Author\",\"name\":\"John Doe\"," +
            "\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"},{\"type\":\"Manufacturer\"," +
            "\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"," +
            "\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Manufacturer\"," +
            "\"name\":\"Jane Doe\",\"email\":\"jane.doe@example.com\",\"phone\":\"1-800-222-2222\"," +
            "\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Supplier\",\"name\":\"John Doe\"," +
            "\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"," +
            "\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Supplier\",\"name\":\"Jane Doe\"," +
            "\"email\":\"jane.doe@example.com\",\"phone\":\"1-800-222-2222\"," +
            "\"url\":\"example.com,example2.com,example3.com\"}],\"tools\":[{\"type\":\"application\"," +
            "\"name\":\"Tool\",\"version\":\"1.0-RELEASE\"}]}";

    return json;
  }

  private static String buildSPDXMetadataJson() {
    String json =
        "{\"created\":\"2020-01-01T00:00:00Z\",\"creators\":[{\"type\":\"Person\",\"name\":\"John Doe\"," +
            "\"email\":\"john.doe@example.com\"},{\"type\":\"Person\",\"name\":\"Jane Doe\"}," +
            "{\"type\":\"Organization\",\"name\":\"Example Organization\",\"email\":\"example@example.com\"}," +
            "{\"type\":\"Organization\",\"name\":\"Example Organization\"}]," +
            "\"tools\":[{\"name\":\"Sonatype IQ Server\",\"version\":\"1.175.0-SNAPSHOT\"}]}";
    return json;
  }
}
