/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Copied from com.sonatype.insight.model.datamart.dto.License
@Entity
@Table(name = "license")
public class License
    implements HasStringId
{
  public static final String UNSPECIFIED_ID = "UNSPECIFIED";

  public static final String UNKNOWN_ID = "UNKNOWN";

  public static final String NOT_DECLARED_ID = "Not-Declared";

  public static final String NO_SOURCES_ID = "No-Sources";

  public static final String NO_SOURCE_LICENSE_ID = "No-Source-License";

  public static final String NOT_SUPPORTED_ID = "Not-Supported";

  public License() {
  }

  public License(String id, String shortDisplayName, String longDisplayName) {
    this.id = id;
    this.shortDisplayName = shortDisplayName;
    this.longDisplayName = longDisplayName;
  }

  @Id
  @Column(name = "license_id")
  private String id;

  @Column(name = "shortDisplayName")
  private String shortDisplayName;

  @Column(name = "longDisplayName")
  private String longDisplayName;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getShortDisplayName() {
    return shortDisplayName;
  }

  public void setShortDisplayName(String shortDisplayName) {
    this.shortDisplayName = shortDisplayName;
  }

  public String getLongDisplayName() {
    return longDisplayName;
  }

  public void setLongDisplayName(String longDisplayName) {
    this.longDisplayName = longDisplayName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
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
    License other = (License) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    }
    else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @JsonIgnore
  public boolean isUnspecified() {
    return UNSPECIFIED_ID.equals(id);
  }

  @JsonIgnore
  public boolean isUnknown() {
    return UNKNOWN_ID.equals(id);
  }

  @Override
  public String toString() {
    return id;
  }

  /**
   * @since 1.12.0
   */
  public static boolean isEffectivelyUnspecified(String id) {
    return NOT_DECLARED_ID.equals(id) || NO_SOURCES_ID.equals(id) || NO_SOURCE_LICENSE_ID.equals(id)
        || UNSPECIFIED_ID.equals(id) || NOT_SUPPORTED_ID.equals(id);
  }

  public static boolean isAlpObservedLicenseFormatHidden(String format) {
    return !ComponentIdentifier.FORMAT_MAVEN.equals(format);
  }
}
