/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.model.HasStringId;

/**
 * The license of a component (identified by GAV) can be overridden at application or organization (i.e. owner) level.
 * If it is overridden at both application and organization levels, the application one wins.
 *
 * @since 1.6
 */
public class LicenseOverride
    implements HasStringId
{
  private String id;

  private String ownerId;

  private LicenseOverrideStatus status;

  private String comment;

  private Set<String> licenseIds = new LinkedHashSet<>();

  private ComponentIdentifier componentIdentifier;

  public LicenseOverride() {

  }

  public LicenseOverride(LicenseOverrideInternal internal, Set<String> licenseIds) {
    this(internal.getOwnerId(), internal.getComponentIdentifier(), internal.getStatus(), licenseIds, internal
        .getComment());
    setId(internal.getId());
  }

  public LicenseOverride(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status,
      String licenseId,
      String comment)
  {
    this(ownerId, componentIdentifier, status, licenseId != null ? Collections.singleton(licenseId) : null, comment);
  }

  public LicenseOverride(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status,
      Set<String> licenseIds,
      String comment)
  {
    this.ownerId = ownerId;
    this.componentIdentifier = componentIdentifier;
    this.status = status;
    this.comment = comment;
    setLicenseIds(licenseIds);
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setStatus(LicenseOverrideStatus status) {
    this.status = status;
  }

  public LicenseOverrideStatus getStatus() {
    return status;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getComment() {
    return comment;
  }

  public Set<String> getLicenseIds() {
    return licenseIds;
  }

  public void setLicenseIds(Set<String> licenseIds) {
    if (licenseIds == null) {
      this.licenseIds.clear();
    }
    else {
      this.licenseIds = licenseIds;
    }
  }

  public void addLicenseId(String licenseId) {
    licenseIds.add(licenseId);
  }

  public void setComponentIdentifier(ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  @Override
  public String toString() {
    return "LicenseOverride{" + "id='" + id + '\'' + ", ownerId='" + ownerId + '\'' + ", status=" + status
        + ", comment='" + comment + '\'' + ", componentIdentifier='" + getComponentIdentifier() + '\''
        + ", licenseIds='" + getLicenseIds() + '\'' + '}';
  }
}
