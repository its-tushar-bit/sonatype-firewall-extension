/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto;

import java.util.List;

import com.sonatype.insight.brain.model.OwnerType;

/**
 * Some objects (policy waivers, license overrides, etc) can be applied in the context of an application or an
 * organization. This class contains the hierarchy of organizations and applications for which such objects can be
 * applied.
 *
 * @since 1.6
 */
public class ApplicableContext
{
  private String id;

  private String name;

  /**
   * "application", "organization", "repository", or "repository container"
   */
  private OwnerType type;

  private List<ApplicableContext> children;

  public ApplicableContext() {
  }

  public ApplicableContext(String id, String name, OwnerType type) {
    this(id, name, type, null);
  }

  public ApplicableContext(String id, String name, OwnerType type, List<ApplicableContext> children) {
    this.id = id;
    this.name = name;
    setType(type);
    setChildren(children);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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
    if (!OwnerType.APPLICATION.equals(type) && !(OwnerType.ORGANIZATION.equals(type))
        && !OwnerType.REPOSITORY_MANAGER.equals(type) && !OwnerType.REPOSITORY.equals(type)
        && !OwnerType.REPOSITORY_CONTAINER.equals(type))
    {
      throw new IllegalArgumentException("Unknown context type: " + type);
    }
    this.type = type;
  }

  public List<ApplicableContext> getChildren() {
    return children;
  }

  public void setChildren(List<ApplicableContext> children) {
    this.children = children;
  }
}
