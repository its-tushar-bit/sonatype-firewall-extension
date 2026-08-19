/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto;

import java.util.List;

import com.sonatype.insight.brain.model.OwnerType;

/**
 * @since 1.106
 */
public class OwnerHierarchyDTO
{
  private String id;

  private String publicId;

  private String name;

  private OwnerType type;

  private List<OwnerHierarchyDTO> children;

  public OwnerHierarchyDTO() {
    // for jackson
  }

  public OwnerHierarchyDTO(String id, String publicId, String name, OwnerType type, List<OwnerHierarchyDTO> children) {
    this.id = id;
    this.publicId = publicId;
    this.name = name;
    this.type = type;
    this.children = children;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public OwnerType getType() {
    return type;
  }

  public void setType(OwnerType type) {
    this.type = type;
  }

  public List<OwnerHierarchyDTO> getChildren() {
    return children;
  }

  public void setChildren(List<OwnerHierarchyDTO> children) {
    this.children = children;
  }
}
