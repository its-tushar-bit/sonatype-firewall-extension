/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.33
 */
@Entity
@Table(name = "system_configuration_property")
public class SystemConfigurationProperty
    implements HasStringId
{
  public static final String AUTOMATIC_APPLICATION_CREATION_ENABLED = "AUTOMATIC_APPLICATION_CREATION_ENABLED";

  public static final String AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID =
      "AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID";

  public static final String AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED =
      "AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED";

  public static final String ADVANCED_SEARCH_ENABLED = "ADVANCED_SEARCH_ENABLED";

  public static final String DASHBOARD_DISABLED = "DASHBOARD_DISABLED";

  public static final String REPORTS_LIST_DISABLED = "REPORTS_LIST_DISABLED";

  @Id
  @Column(name = "system_configuration_property_id")
  private String id;

  @Column(name = "name")
  private String name;

  @Column(name = "value")
  private String value;

  public SystemConfigurationProperty() {
  }

  public SystemConfigurationProperty(String name, String value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
