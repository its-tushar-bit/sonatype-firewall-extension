/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

// Copied from com.sonatype.insight.model.datamart.dto.LicenseCategory
@Entity
@Table(name = "license_category")
public class LicenseCategory
    implements HasStringId
{
  public static final String COPYLEFT_ID = "COPYLEFT";

  public static final String WEAKCOPYLEFT_ID = "WEAKCOPYLEFT";

  public static final String LIBERAL_ID = "LIBERAL";

  public static final String NON_STANDARD_ID = "NON-STANDARD";

  // not actually in the DB but still a handy constant
  public static final String NOT_PROVIDED_ID = "NOT-PROVIDED";

  @Id
  @Column(name = "license_category_id")
  private String id;

  @Column(name = "name")
  private String name;

  @Column(name = "severity")
  private int severity;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getSeverity() {
    return severity;
  }

  public void setSeverity(int severity) {
    this.severity = severity;
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
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    LicenseCategory other = (LicenseCategory) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    }
    else if (!id.equals(other.id))
      return false;
    return true;
  }
}