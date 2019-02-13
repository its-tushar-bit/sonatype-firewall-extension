/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PolicyThreatCategoryFilterTest
{
  @Test
  public void testSinglePolicyThreatCategory() {
    PolicyThreatCategoryFilter filter = new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE);
    PolicyViolation trueViolation = new PolicyViolation();
    trueViolation.setThreatCategory(PolicyThreatCategory.LICENSE);

    PolicyViolation falseViolation = new PolicyViolation();
    falseViolation.setThreatCategory(PolicyThreatCategory.OTHER);

    assertThat(filter.asPolicyViolationPredicate().test(trueViolation)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(falseViolation)).isFalse();
  }

  @Test
  public void testMultiplePolicyThreatCategories() {
    PolicyThreatCategoryFilter filter = new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE,
        PolicyThreatCategory.OTHER, PolicyThreatCategory.QUALITY, PolicyThreatCategory.SECURITY);
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    PolicyViolation v4 = new PolicyViolation();
    v1.setThreatCategory(PolicyThreatCategory.LICENSE);
    v2.setThreatCategory(PolicyThreatCategory.OTHER);
    v3.setThreatCategory(PolicyThreatCategory.QUALITY);
    v4.setThreatCategory(PolicyThreatCategory.SECURITY);

    assertThat(filter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v2)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v3)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v4)).isTrue();
  }

  @Test
  public void testEmptyPolicyThreatCategories() {
    PolicyThreatCategoryFilter filter = new PolicyThreatCategoryFilter();
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    PolicyViolation v4 = new PolicyViolation();
    v1.setThreatCategory(PolicyThreatCategory.LICENSE);
    v2.setThreatCategory(PolicyThreatCategory.OTHER);
    v3.setThreatCategory(PolicyThreatCategory.QUALITY);
    v4.setThreatCategory(PolicyThreatCategory.SECURITY);

    assertThat(filter.asPolicyViolationPredicate().test(v1)).isFalse();
    assertThat(filter.asPolicyViolationPredicate().test(v2)).isFalse();
    assertThat(filter.asPolicyViolationPredicate().test(v3)).isFalse();
    assertThat(filter.asPolicyViolationPredicate().test(v4)).isFalse();
  }

  @Test
  public void testNullPolicyThreatCategories() {
    List<PolicyThreatCategory> nullThreatCategory = null;
    PolicyThreatCategoryFilter filter = new PolicyThreatCategoryFilter(nullThreatCategory);
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    PolicyViolation v4 = new PolicyViolation();
    v1.setThreatCategory(PolicyThreatCategory.LICENSE);
    v2.setThreatCategory(PolicyThreatCategory.OTHER);
    v3.setThreatCategory(PolicyThreatCategory.QUALITY);
    v4.setThreatCategory(PolicyThreatCategory.SECURITY);

    assertThat(filter.asPolicyViolationPredicate().test(v1)).isFalse();
    assertThat(filter.asPolicyViolationPredicate().test(v2)).isFalse();
    assertThat(filter.asPolicyViolationPredicate().test(v3)).isFalse();
    assertThat(filter.asPolicyViolationPredicate().test(v4)).isFalse();
  }

  @Test
  public void testNullViolationThreatCategory() {
    PolicyThreatCategoryFilter filter = new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE);
    PolicyViolation v1 = new PolicyViolation();
    v1.setThreatCategory(null);

    assertThat(filter.asPolicyViolationPredicate().test(v1)).isFalse();
  }

  @Test
  public void testStringConstruction() {
    String singleCategory = "license";
    String singleCategoryWithSpaces = "         license           ";
    String singleCategoryWithDanglingComma = "license,";
    String multiCategoryWithSpaces = "    license   ,     other   ";
    String multiCategory = "license,other";

    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    v1.setThreatCategory(PolicyThreatCategory.LICENSE);
    v2.setThreatCategory(PolicyThreatCategory.OTHER);
    v3.setThreatCategory(PolicyThreatCategory.QUALITY);

    PolicyThreatCategoryFilter singleCategoryFilter = new PolicyThreatCategoryFilter(singleCategory);
    PolicyThreatCategoryFilter singleCategoryWithSpacesFilter 
        = new PolicyThreatCategoryFilter(singleCategoryWithSpaces);
    PolicyThreatCategoryFilter singleCategoryWithDanglingCommaFilter = new PolicyThreatCategoryFilter(
        singleCategoryWithDanglingComma);
    PolicyThreatCategoryFilter multiCategoryWithSpacesFilter = new PolicyThreatCategoryFilter(multiCategoryWithSpaces);
    PolicyThreatCategoryFilter multiCategoryFilter = new PolicyThreatCategoryFilter(multiCategory);

    assertThat(singleCategoryFilter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(singleCategoryFilter.asPolicyViolationPredicate().test(v2)).isFalse();

    assertThat(singleCategoryWithSpacesFilter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(singleCategoryWithSpacesFilter.asPolicyViolationPredicate().test(v2)).isFalse();

    assertThat(singleCategoryWithDanglingCommaFilter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(singleCategoryWithDanglingCommaFilter.asPolicyViolationPredicate().test(v2)).isFalse();

    assertThat(multiCategoryWithSpacesFilter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(multiCategoryWithSpacesFilter.asPolicyViolationPredicate().test(v2)).isTrue();
    assertThat(multiCategoryWithSpacesFilter.asPolicyViolationPredicate().test(v3)).isFalse();

    assertThat(multiCategoryFilter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(multiCategoryFilter.asPolicyViolationPredicate().test(v2)).isTrue();
    assertThat(multiCategoryFilter.asPolicyViolationPredicate().test(v3)).isFalse();
  }

  @Test
  public void testStringConstructionWithMalFormedStrings() {
    String emptyCategory = ",license";
    String nonExistentCategory = "category1,category2";
    String emptyString = "";
    String nullString = null;

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      new PolicyThreatCategoryFilter(emptyCategory);
    }).withMessage("Unknown policy threat category with name: ");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      new PolicyThreatCategoryFilter(nonExistentCategory);
    }).withMessage("Unknown policy threat category with name: category1");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      new PolicyThreatCategoryFilter(emptyString);
    }).withMessage("Unable to parse policy threat categories from empty or null categories.");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      new PolicyThreatCategoryFilter(nullString);
    }).withMessage("Unable to parse policy threat categories from empty or null categories.");
  }
}
