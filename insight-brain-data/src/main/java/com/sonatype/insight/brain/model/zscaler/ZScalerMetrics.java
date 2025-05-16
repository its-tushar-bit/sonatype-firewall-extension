/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.zscaler;

import java.util.Date;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "zscaler_metrics")
public class ZScalerMetrics
    implements HasStringId
{
  @Id
  @Column(name = "zscaler_metrics_id")
  private String id;

  @Column(name = "maven_urls_from_hds")
  private int mavenUrlsFromHds;

  @Column(name = "npm_urls_from_hds")
  private int npmUrlsFromHds;

  @Column(name = "pypi_urls_from_hds")
  private int pypiUrlsFromHds;

  @Column(name = "nuget_urls_from_hds")
  private int nugetUrlsFromHds;

  @Column(name = "maven_urls_to_zscaler")
  private int mavenUrlsToZscaler;

  @Column(name = "npm_urls_to_zscaler")
  private int npmUrlsToZscaler;

  @Column(name = "pypi_urls_to_zscaler")
  private int pypiUrlsToZscaler;

  @Column(name = "nuget_urls_to_zscaler")
  private int nugetUrlsToZscaler;

  @Column(name = "updated_at")
  private Date updatedAt;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public int getMavenUrlsFromHds() {
    return mavenUrlsFromHds;
  }

  public void setMavenUrlsFromHds(final int mavenUrlsFromHds) {
    this.mavenUrlsFromHds = mavenUrlsFromHds;
  }

  public int getNpmUrlsFromHds() {
    return npmUrlsFromHds;
  }

  public void setNpmUrlsFromHds(final int npmUrlsFromHds) {
    this.npmUrlsFromHds = npmUrlsFromHds;
  }

  public int getPypiUrlsFromHds() {
    return pypiUrlsFromHds;
  }

  public void setPypiUrlsFromHds(final int pypiUrlsFromHds) {
    this.pypiUrlsFromHds = pypiUrlsFromHds;
  }

  public int getNugetUrlsFromHds() {
    return nugetUrlsFromHds;
  }

  public void setNugetUrlsFromHds(final int nugetUrlsFromHds) {
    this.nugetUrlsFromHds = nugetUrlsFromHds;
  }

  public int getMavenUrlsToZscaler() {
    return mavenUrlsToZscaler;
  }

  public void setMavenUrlsToZscaler(final int mavenUrlsToZscaler) {
    this.mavenUrlsToZscaler = mavenUrlsToZscaler;
  }

  public int getNpmUrlsToZscaler() {
    return npmUrlsToZscaler;
  }

  public void setNpmUrlsToZscaler(final int npmUrlsToZscaler) {
    this.npmUrlsToZscaler = npmUrlsToZscaler;
  }

  public int getPypiUrlsToZscaler() {
    return pypiUrlsToZscaler;
  }

  public void setPypiUrlsToZscaler(final int pypiUrlsToZscaler) {
    this.pypiUrlsToZscaler = pypiUrlsToZscaler;
  }

  public int getNugetUrlsToZscaler() {
    return nugetUrlsToZscaler;
  }

  public void setNugetUrlsToZscaler(final int nugetUrlsToZscaler) {
    this.nugetUrlsToZscaler = nugetUrlsToZscaler;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(final Date updatedAt) {
    this.updatedAt = updatedAt;
  }
}
