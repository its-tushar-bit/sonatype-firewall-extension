/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "component_category")
public class ComponentCategory
    implements HasStringId
{
  @Id
  @Column(name = "component_category_id")
  private String id;

  @Column(name = "path")
  private String path;

  // for JPA
  public ComponentCategory() {
  }

  public ComponentCategory(String id, String path) {
    this.id = id;
    this.path = path;
  }

  public ComponentCategory(com.sonatype.clm.dto.model.component.ComponentCategory componentCategoryDTO) {
    this.id = String.valueOf(componentCategoryDTO.getComponentCategoryId());
    this.path = componentCategoryDTO.getPath();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  // required for policy editor
  public String getName() {
    return this.path;
  }

  @Override
  public String toString() {
    return getId();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ComponentCategory other = (ComponentCategory) obj;
    return Objects.equals(id, other.id);
  }
}
