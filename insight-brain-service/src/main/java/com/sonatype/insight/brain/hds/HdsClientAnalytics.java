/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Objects;

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.scan.util.HashUtils;

/**
 * Carries data that should be included in requests to the HDS for the purpose of analytics.
 *
 * @since 1.19
 */
public class HdsClientAnalytics
{
  private OwnerType ownerType;

  private String ownerId;

  private HdsClientAnalytics() {
    // outside callers should use the factory methods
  }

  /**
   * @since 1.43
   */
  public static HdsClientAnalytics forOwner(Owner owner) {
    HdsClientAnalytics analytics = new HdsClientAnalytics();
    analytics.ownerType = owner.getType();
    analytics.ownerId = obfuscate(owner.getId());
    return analytics;
  }

  public static HdsClientAnalytics forRepository(String repositoryId) {
    HdsClientAnalytics analytics = new HdsClientAnalytics();
    analytics.ownerType = OwnerType.REPOSITORY;
    analytics.ownerId = obfuscate(repositoryId);
    return analytics;
  }

  public OwnerType getOwnerType() {
    return ownerType;
  }

  public String getOwnerId() {
    return ownerId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HdsClientAnalytics that = (HdsClientAnalytics) o;

    return Objects.equals(this.ownerType, that.ownerType) && Objects.equals(this.ownerId, that.ownerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ownerType, ownerId);
  }

  /**
   * @since 1.20
   */
  public static String obfuscate(String source) {
    return HashUtils.hash(source, HashUtils.SHA1);
  }
}
