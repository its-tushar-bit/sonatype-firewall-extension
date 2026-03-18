/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

/**
 * @since 1.6
 */
public enum LicenseOverrideStatus
{
  // Note: The order the statuses are defined here determines the order they are displayed in the UI
  OPEN("Open"),
  ACKNOWLEDGED("Acknowledged"),
  OVERRIDDEN("Overridden"),
  SELECTED("Selected"),
  CONFIRMED("Confirmed");

  private final String name;

  LicenseOverrideStatus(String name) {
    this.name = name;
  }

  public static LicenseOverrideStatus getByName(String name) {
    if (name == null) {
      return null;
    }

    for (LicenseOverrideStatus status : values()) {
      if (name.equals(status.name)) {
        return status;
      }
    }

    throw new IllegalArgumentException("Unknown license override status with name: " + name);
  }

  public String getId() {
    return name();
  }

  public String getName() {
    return name;
  }
}
