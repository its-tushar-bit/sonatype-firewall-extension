/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

public enum PolicyWaiverRequestStatus
{
  APPROVED,
  REJECTED,
  REQUESTED;

  public static PolicyWaiverRequestStatus fromString(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }

    for (PolicyWaiverRequestStatus status : values()) {
      if (name.equalsIgnoreCase(status.getId())) {
        return status;
      }
    }

    throw new BadRequestException("Unknown policy waiver request status with name: " + name);
  }

  public String getId() {
    return name();
  }
}
