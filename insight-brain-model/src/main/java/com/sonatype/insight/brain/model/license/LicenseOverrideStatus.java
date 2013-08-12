package com.sonatype.insight.brain.model.license;

/**
 * @since 1.6
 */
public enum LicenseOverrideStatus
{
  OPEN("Open"), ACKNOWLEDGED("Acknowledged"), OVERRIDDEN("Overridden"), SELECTED("Selected"), CONFIRMED("Confirmed");

  private final String displayName;

  LicenseOverrideStatus(String displayName) {
    this.displayName = displayName;
  }

  public static LicenseOverrideStatus getByDisplayName(String displayName) {
    if (displayName == null) {
      return null;
    }

    for (LicenseOverrideStatus status : values()) {
      if (displayName.equals(status.displayName)) {
        return status;
      }
    }

    throw new IllegalArgumentException("Unknown license override status with display name: " + displayName);
  }

  public String getDisplayName() {
    return displayName;
  }
}
