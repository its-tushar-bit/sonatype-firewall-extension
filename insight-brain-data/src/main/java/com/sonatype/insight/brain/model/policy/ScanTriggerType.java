/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

/**
 * @since 1.105
 */
public enum ScanTriggerType
{
  CLI("CLI"), //
  CONTINUOUS_INTEGRATION("Continuous Integration"), //
  REPOSITORY_MANAGER("Repository Manager"), //
  SOURCE_CONTROL_API("Source Control API"), //
  SOURCE_CONTROL_INTERNAL("Source Control Internal"), //
  THIRD_PARTY("Third Party"), //
  WEB_UI("Web UI"), //
  // The Unknown trigger type is only for policy evaluations created before scan trigger type was introduced.
  // It should never be used in new code anywhere.
  UNKNOWN("Unknown");

  private final String displayName;

  ScanTriggerType(String displayName) {
    this.displayName = displayName;
  }

  public String getId() {
    return name();
  }

  public String getDisplayName() {
    return this.displayName;
  }
}
