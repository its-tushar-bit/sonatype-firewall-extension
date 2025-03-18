/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import org.apache.commons.lang3.StringUtils;

public enum PolicyWaiverStatus
{
  APPROVED, REJECTED, REQUESTED;

  public static PolicyWaiverStatus fromString(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }

    for (PolicyWaiverStatus status : values()) {
      if (name.equalsIgnoreCase(status.getId())) {
        return status;
      }
    }

    throw new IllegalArgumentException("Unknown policy waiver status with name: " + name);
  }

  public String getId() {
    return name();
  }
}
