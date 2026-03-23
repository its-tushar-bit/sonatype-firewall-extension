/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/**
 * @since 1.13.0
 */
@MappedSuperclass
public abstract class HasComponentId
{
  @Column(name = "component_id_format")
  private String componentIdFormat;

  @Column(name = "component_id_coordinates_json")
  private String componentIdCoordinatesJson;

  @Transient
  private ComponentIdentifier componentIdentifier;

  public ComponentIdentifier getComponentIdentifier() {
    if (componentIdFormat == null) {
      return null;
    }
    if (componentIdentifier == null) {
      componentIdentifier =
          ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(componentIdFormat, componentIdCoordinatesJson);
    }
    return componentIdentifier;
  }

  public void setComponentIdentifier(ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
    if (componentIdentifier == null) {
      componentIdFormat = null;
      componentIdCoordinatesJson = null;
    }
    else {
      componentIdFormat = componentIdentifier.getFormat();
      componentIdCoordinatesJson = ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates());
    }
  }

  /**
   * exposed for testing and DAO mapping
   */
  public String getComponentIdFormat() {
    return componentIdFormat;
  }

  /**
   * exposed for testing and DAO mapping
   */
  public void setComponentIdFormat(String componentIdFormat) {
    this.componentIdFormat = componentIdFormat;
    this.componentIdentifier = null; // clear cached value
  }

  /**
   * exposed for testing and DAO mapping
   */
  public String getComponentIdCoordinatesJson() {
    return componentIdCoordinatesJson;
  }

  /**
   * exposed for testing and DAO mapping
   */
  public void setComponentIdCoordinatesJson(String componentIdCoordinatesJson) {
    this.componentIdCoordinatesJson = componentIdCoordinatesJson;
    this.componentIdentifier = null; // clear cached value
  }
}
