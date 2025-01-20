/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

// Copied from com.sonatype.insight.model.datamart.dto.MultiLicense
@Entity
@Table(name = "multi_license")
public class MultiLicense
    implements HasStringId
{
  @Id
  @Column(name = "multi_license_id")
  private String id;

  @Column(name = "shortDisplayName")
  private String shortDisplayName;

  @Column(name = "longDisplayName")
  private String longDisplayName;

  public MultiLicense() {
  }

  public MultiLicense(String id, String shortDisplayName, String longDisplayName) {
    this.id = id;
    this.shortDisplayName = shortDisplayName;
    this.longDisplayName = longDisplayName;
  }

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
    MultiLicense other = (MultiLicense) obj;
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

  public boolean isUnspecified() {
    return License.UNSPECIFIED_ID.equals(id);
  }

  public boolean isUnknown() {
    return License.UNKNOWN_ID.equals(id);
  }

  @Override
  public String toString() {
    return id;
  }

  public static void prunePreciseLicenses(Set<MultiLicense> licenses) {
    for (Iterator<MultiLicense> it = licenses.iterator(); it.hasNext();) {
      String licenseId = it.next().getId();
      for (MultiLicense license : licenses) {
        String id = license.getId();
        if (id.equals(licenseId)) {
          // keep current
        }
        else if (id.endsWith("-UNSPECIFIED") && licenseId.regionMatches(true, 0, id, 0, id.length() - 12)) {
          it.remove();
          break;
        }
      }
    }
  }

  public static class ByNameComparator
      implements Comparator<MultiLicense>
  {
    @Override
    public int compare(MultiLicense l1, MultiLicense l2) {
      return l1.getShortDisplayName().compareTo(l2.getShortDisplayName());
    }
  }
}
