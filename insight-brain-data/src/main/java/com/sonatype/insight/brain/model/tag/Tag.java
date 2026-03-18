/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.9
 */
@Entity
@Table(name = "tag")
public class Tag
    extends Nameable
    implements HasStringId
{
  @Id
  @Column(name = "tag_id")
  private String id;

  @Column(name = "organization_id")
  private String organizationId;

  @Column(name = "description")
  private String description;

  @Column(name = "color")
  @Enumerated(EnumType.STRING)
  private Color color = Color.light_green;

  public Tag() {
  }

  public Tag(String organizationId, String name, String description) {
    this.organizationId = organizationId;
    setName(name);
    this.description = description;
  }

  public Tag(String organizationId, String name, String description, Color color) {
    this(organizationId, name, description);
    this.color = color;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }
}
