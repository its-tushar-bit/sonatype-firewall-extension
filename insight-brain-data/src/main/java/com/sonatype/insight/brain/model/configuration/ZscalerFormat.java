/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "zscaler_format")
public class ZscalerFormat
    implements HasStringId
{
  @Id
  @Column(name = "zscaler_format_id")
  private String id;

  @Column(name = "zscaler_configuration_id")
  private String zscalerConfigurationId;

  @Column(name = "format")
  private String format;

  @Column(name = "enabled")
  private boolean enabled;

  public ZscalerFormat() {
  }

  public ZscalerFormat(final String format, final boolean enabled) {
    this.format = format;
    this.enabled = enabled;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getZscalerConfigurationId() {
    return zscalerConfigurationId;
  }

  public void setZscalerConfigurationId(final String zscalerConfigurationId) {
    this.zscalerConfigurationId = zscalerConfigurationId;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }
}
