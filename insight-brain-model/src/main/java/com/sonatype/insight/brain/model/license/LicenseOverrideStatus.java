package com.sonatype.insight.brain.model.license;


/**
 * @since 1.6
 */
public enum LicenseOverrideStatus
{
  OPEN("Open"), ACKNOWLEDGED("Acknowledged"), OVERRIDDEN("Overridden"), SELECTED("Selected"), CONFIRMED("Confirmed");

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
