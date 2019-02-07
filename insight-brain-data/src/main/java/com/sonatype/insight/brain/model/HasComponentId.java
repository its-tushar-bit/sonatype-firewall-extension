/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.json.store.JsonUtils;

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
      try {
        componentIdentifier = new ComponentIdentifier(componentIdFormat, JsonUtils.parse(componentIdCoordinatesJson,
            Map.class));
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
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
   * Copy the componentId values from another HasComponentId into this one. Copies the internal String
   * representations to avoid JSON parsing overhead
   * 
   * @since 1.33
   */
  protected void copyComponentIdentifierFrom(HasComponentId other) {
    this.componentIdFormat = other.componentIdFormat;
    this.componentIdCoordinatesJson = other.componentIdCoordinatesJson;
  }

  /**
   * exposed for testing
   */
  String getComponentIdFormat() {
    return componentIdFormat;
  }

  /**
   * exposed for testing
   */
  String getComponentIdCoordinatesJson() {
    return componentIdCoordinatesJson;
  }
}
