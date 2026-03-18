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
  SOURCE_CONTROL_API("Source Control API", true), //
  SOURCE_CONTROL_INTERNAL_ONBOARDING("Source Control Onboarding", true), //
  SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING("Source Control Default Branch Monitoring", true), //
  SOURCE_CONTROL_INTERNAL_PULL_REQUEST("Source Control Pull Request", true), //
  THIRD_PARTY("Third Party", true), //
  WEB_UI("Web UI", true), //
  IDE("IDE"), //
  SBOM_UI("SBOM Manager UI", true), //
  SBOM_API("SBOM Manager API", true), //
  SONATYPE_CONTAINER_IMAGE_SCANNER_API("Container Image Scanner API", true), //
  // The Unknown trigger type is only for policy evaluations created before scan trigger type was introduced.
  // It should never be used in new code anywhere.
  UNKNOWN("Unknown", null);

  private final String displayName;

  /**
   * True if the scan was triggered internally by IQ
   */
  private final Boolean internal;

  ScanTriggerType(String displayName) {
    this(displayName, false);
  }

  ScanTriggerType(String displayName, Boolean internal) {
    this.displayName = displayName;
    this.internal = internal;
  }

  public String getId() {
    return name();
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public Boolean isInternal() {
    return internal;
  }

  // TODO: check if this can be refactored to use `isInternal` see CLM-34546
  public static final List<ScanTriggerType> internalScanTypes = Arrays.asList(
      SOURCE_CONTROL_INTERNAL_ONBOARDING,
      SOURCE_CONTROL_INTERNAL_PULL_REQUEST,
      SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING);
}
