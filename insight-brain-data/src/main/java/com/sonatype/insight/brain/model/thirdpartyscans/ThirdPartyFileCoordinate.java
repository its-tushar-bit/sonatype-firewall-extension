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
@Table(name = "file_coordinate")
public class ThirdPartyFileCoordinate
    implements HasStringId
{
  public ThirdPartyFileCoordinate() {
    //noop
  }

  public ThirdPartyFileCoordinate(
      String hash,
      String source,
      String format,
      String name,
      String version,
      String thirdPartyFileId)
  {
    this.hash = hash;
    this.source = source;
    this.format = format;
    this.name = name;
    this.version = version;
    this.thirdPartyFileId = thirdPartyFileId;
  }

  @Id
  @Column(name = "file_coordinate_id")
  private String id;

  @Column(name = "hash")
  private String hash;

  @Column(name = "source")
  private String source;

  @Column(name = "package_url")
  private String packageUrl;

  @Column(name = "format")
  private String format;

  @Column(name = "name")
  private String name;

  @Column(name = "version")
  private String version;

  @Column(name = "third_party_file_id")
  private String thirdPartyFileId;

  @Column(name = "cpe")
  private String cpe;

  @Column(name = "swid")
  private String swid;

  @Column(name = "identification_sources")
  private String identificationSources;

  @Column(name = "dependency_type")
  private String dependencyType;

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

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(String packageUrl) {
    this.packageUrl = packageUrl;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getThirdPartyFileId() {
    return thirdPartyFileId;
  }

  public void setThirdPartyFileId(String thirdPartyFileId) {
    this.thirdPartyFileId = thirdPartyFileId;
  }

  public String getCpe() {
    return cpe;
  }

  public void setCpe(String cpe) {
    this.cpe = cpe;
  }

  public String getSwid() {
    return swid;
  }

  public void setSwid(String swid) {
    this.swid = swid;
  }

  public String getIdentificationSources() {
    return identificationSources;
  }

  public void setIdentificationSources(String identificationSources) {
    this.identificationSources = identificationSources;
  }

  public void addIdentificationSource(String identificationSource) {
    if (this.identificationSources == null) {
      setIdentificationSources(identificationSource);
    }
    else if (!this.identificationSources.contains(identificationSource)) {
      setIdentificationSources(this.identificationSources + "," + identificationSource);
    }
  }

  public String getDependencyType() {
    return dependencyType;
  }

  public void setDependencyType(String dependencyType) {
    this.dependencyType = dependencyType;
  }
}
