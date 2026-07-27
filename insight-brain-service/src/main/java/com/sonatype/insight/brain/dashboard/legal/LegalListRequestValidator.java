/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.Set;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Validates Legal list request filters at the API boundary.
 */
@Named
@Singleton
final class LegalListRequestValidator
{
  static final String DEFAULT_ORDER_BY = "-licenseThreatLevel";

  private static final Set<String> SUPPORTED_ORDER_BY = Set.of("licenseThreatLevel", "-licenseThreatLevel");

  void validate(final LegalListRequestDTO request) {
    if (request == null) {
      return;
    }
    validateOrderBy(request.orderBy);
  }

  private static void validateOrderBy(final String orderBy) {
    if (StringUtils.isBlank(orderBy)) {
      return;
    }
    if (!SUPPORTED_ORDER_BY.contains(orderBy)) {
      throw new BadRequestException(
          "Invalid orderBy: " + orderBy + ". Supported values are licenseThreatLevel and -licenseThreatLevel.");
    }
  }
}
