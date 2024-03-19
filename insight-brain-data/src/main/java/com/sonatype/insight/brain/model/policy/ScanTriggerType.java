/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Arrays;
import java.util.List;

/**
 * @since 1.105
 */
public enum ScanTriggerType
{
  CLI("CLI"), //
  CONTINUOUS_INTEGRATION("Continuous Integration"), //
  REPOSITORY_MANAGER("Repository Manager"), //
  SOURCE_CONTROL_API("Source Control API"), //
  SOURCE_CONTROL_INTERNAL_ONBOARDING("Source Control Onboarding"), //
  SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING("Source Control Default Branch Monitoring"), //
  SOURCE_CONTROL_INTERNAL_PULL_REQUEST("Source Control Pull Request"), //
  THIRD_PARTY("Third Party"), //
  WEB_UI("Web UI"), //
  IDE("IDE"), //
  SBOM_UI("SBOM Manager UI"), //
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

  public static final List<ScanTriggerType> internalScanTypes = Arrays.asList(
      SOURCE_CONTROL_INTERNAL_ONBOARDING,
      SOURCE_CONTROL_INTERNAL_PULL_REQUEST,
      SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING
  );
}
