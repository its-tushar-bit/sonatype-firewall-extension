/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class RemediationBranchNamePrefixGenerator
{
  private static final int APP_ID_BRANCH_TRUNCATE_INDEX = 6;

  public String generatePrefixForApplication(String applicationId) {
    if (StringUtils.isBlank(applicationId)) {
      return "";
    }
    if (applicationId.trim().length() < APP_ID_BRANCH_TRUNCATE_INDEX) {
      return applicationId.trim();
    }
    return applicationId.trim().substring(0, APP_ID_BRANCH_TRUNCATE_INDEX).trim();
  }
}
