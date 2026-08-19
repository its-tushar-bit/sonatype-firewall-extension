/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.google.common.annotations.VisibleForTesting;

/**
 * @since 1.22
 */
@Entity
@Table(name = "proprietary_config")
public class ProprietaryConfig
    implements HasStringId
{
  @Id
  @Column(name = "proprietary_config_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "packages_json")
  private String packagesJson;

  @Column(name = "regexes_json")
  private String regexesJson;

  @Transient
  private List<String> packages;

  @Transient
  private List<String> regexes;

  public ProprietaryConfig() {
  }

  public ProprietaryConfig(String ownerId, List<String> packages, List<String> regexes) {
    this.ownerId = ownerId;
    setPackages(packages);
    setRegexes(regexes);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @SuppressWarnings("unchecked")
  public List<String> getPackages() {
    if (packages == null && packagesJson != null) {
      try {
        packages = JsonUtils.parse(packagesJson, List.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to read proprietary package configuration " + id, e);
      }
    }
    if (packages == null) {
      packages = new ArrayList<>();
    }
    return packages;
  }

  public void setPackages(List<String> packages) {
    if (packages == null || packages.isEmpty()) {
      this.packages = new ArrayList<>();
      packagesJson = null;
      return;
    }

    this.packages = packages;
    packagesJson = JsonUtils.writeUnformatted(packages);
  }

  @SuppressWarnings("unchecked")
  public List<String> getRegexes() {
    if (regexes == null && regexesJson != null) {
      try {
        regexes = JsonUtils.parse(regexesJson, List.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to read proprietary regex configuration " + id, e);
      }
    }
    if (regexes == null) {
      regexes = new ArrayList<>();
    }
    return regexes;
  }

  @VisibleForTesting
  String getPackagesJson() {
    return packagesJson;
  }

  public void setRegexes(List<String> regexes) {
    if (regexes == null || regexes.isEmpty()) {
      this.regexes = new ArrayList<>();
      regexesJson = null;
      return;
    }

    this.regexes = regexes;
    regexesJson = JsonUtils.writeUnformatted(regexes);
  }

  @VisibleForTesting
  String getRegexesJson() {
    return regexesJson;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }
}
