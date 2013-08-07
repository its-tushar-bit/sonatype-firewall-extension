package com.sonatype.insight.brain.model.license;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

// Copied from com.sonatype.insight.model.datamart.dto.License
@Entity
@Table(name = "license")
public class License
    implements HasStringId
{
  public static final String UNSPECIFIED_ID = "UNSPECIFIED";

  public static final String UNKNOWN_ID = "UNKNOWN";

  @Id
  @Column(name = "license_id")
  private String id;

  @Column(name = "shortDisplayName")
  private String shortDisplayName;

  @Column(name = "longDisplayName")
  private String longDisplayName;

  @Column(name = "description")
  private String description;

  @Column(name = "licenseUrl")
  private String licenseUrl;

  @Column(name = "license_category_id")
  private String licenseCategoryId;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLicenseUrl() {
    return licenseUrl;
  }

  public void setLicenseUrl(String licenseUrl) {
    this.licenseUrl = licenseUrl;
  }

  public String getLicenseCategoryId() {
    return licenseCategoryId;
  }

  public void setLicenseCategoryId(String licenseCategoryId) {
    this.licenseCategoryId = licenseCategoryId;
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
    License other = (License) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    }
    else if (!id.equals(other.id))
      return false;
    return true;
  }

  public boolean isUnspecified() {
    return UNSPECIFIED_ID.equals(id);
  }

  public boolean isUnknown() {
    return UNKNOWN_ID.equals(id);
  }

  @Override
  public String toString() {
    return id;
  }
}
