/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PolicyThreatCategoryFilterTest
{
  @Test
  public void testStringConstruction() {
    String singleCategory = "license";
    String singleCategoryWithSpaces = "         license           ";
    String singleCategoryWithDanglingComma = "license,";
    String multiCategoryWithSpaces = "    license   ,     other   ";
    String multiCategory = "license,other";

    new PolicyThreatCategoryFilter(singleCategory);
    new PolicyThreatCategoryFilter(singleCategoryWithSpaces);
    new PolicyThreatCategoryFilter(singleCategoryWithDanglingComma);
    new PolicyThreatCategoryFilter(multiCategoryWithSpaces);
    new PolicyThreatCategoryFilter(multiCategory);
  }

  @Test
  public void testStringConstructionWithMalFormedStrings() {
    String emptyCategory = ",license";
    String nonExistentCategory = "category1,category2";
    String emptyString = "";
    String nullString = null;

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> new PolicyThreatCategoryFilter(emptyCategory))
        .withMessage("Unknown policy threat category with name: ");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> new PolicyThreatCategoryFilter(nonExistentCategory))
        .withMessage("Unknown policy threat category with name: category1");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> new PolicyThreatCategoryFilter(emptyString))
        .withMessage("Unable to parse policy threat categories from empty or null categories.");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> new PolicyThreatCategoryFilter(nullString))
        .withMessage("Unable to parse policy threat categories from empty or null categories.");
  }
}
