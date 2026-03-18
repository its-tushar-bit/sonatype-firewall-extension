/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.roi;

import java.util.Arrays;
import com.sonatype.insight.error.exception.NotFoundException;

public enum CurrencyTypes
{
  USD;

  public static CurrencyTypes fromString(String currencyType) {
    return Arrays.stream(CurrencyTypes.values())
        .filter(currency -> currency.name().equalsIgnoreCase(currencyType))
        .findFirst()
        .orElseThrow(
            () -> new NotFoundException(String.format("Provided currency type %s is not found", currencyType)));
  }
}
