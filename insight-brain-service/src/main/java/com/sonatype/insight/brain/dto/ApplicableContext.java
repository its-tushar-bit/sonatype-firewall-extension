/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto;

import java.util.List;

import com.sonatype.insight.brain.utils.IdUtils;

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
   * "application" or "organization"
   */
  private String type;

  private List<ApplicableContext> children;

  public ApplicableContext() {
  }

  public ApplicableContext(String id, String name, String type) {
    this.id = id;
    this.name = name;
    setType(type);
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    if (!IdUtils.TYPE_APPLICATION.equals(type) && !(IdUtils.TYPE_ORGANIZATION.equals(type))) {
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