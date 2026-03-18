/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.label;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "label")
public class Label
    implements HasStringId
{
  @Id
  @Column(name = "label_id")
  private String id;

  /**
   * @since 1.6
   */
  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "label")
  private String label;

  @Column(name = "label_lowercase")
  private String labelLowercase;

  @Column(name = "description")
  private String description;

  @Column(name = "color")
  @Enumerated(EnumType.STRING)
  private Color color = Color.light_green;

  public Label() {
  }

  public Label(String ownerId, String label) {
    this.ownerId = ownerId;
    setLabel(label);
  }

  public Label(String ownerId, String label, Color color) {
    this(ownerId, label);
    this.color = color;
  }

  public Label(String ownerId, String label, String description, Color color) {
    this(ownerId, label, color);
    this.description = description;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @since 1.6
   */
  public String getOwnerId() {
    return ownerId;
  }

  /**
   * @since 1.6
   */
  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    if (label != null) {
      label = label.trim();
      labelLowercase = normalizeLabel(label);
    }
    else {
      labelLowercase = null;
    }
    this.label = label;
  }

  public String getLabelLowercase() {
    return labelLowercase;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "Label=" + label;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * labelLowercase field. If this method is not defined, jackson will set/access the labelLowercase field directly
   * via reflection, possibly setting it to an incorrect value.
   *
   * @deprecated This method should not be used explicitly.
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void setLabelLowercase(String labelLowercase) {
  }

  public static String normalizeLabel(String label) {
    return label.trim().toLowerCase(Locale.ENGLISH);
  }
}
