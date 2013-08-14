/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto;

import java.util.List;

/**
 * Some objects (policy waivers, license overrides, etc) can be applied in the context of an application or an
 * organization. This class contains the hierarchy of organizations and applications for which such objects can be
 * applied.
 * 
 * @since 1.6
 */
public class ApplicableContext
{
  public String id;

  public String name;

  /**
   * "application" or "organization"
   */
  public String type;

  public List<ApplicableContext> children;

  public ApplicableContext() {
  }

  public ApplicableContext(String id, String name, String type) {
    this.id = id;
    this.name = name;
    this.type = type;
  }
}