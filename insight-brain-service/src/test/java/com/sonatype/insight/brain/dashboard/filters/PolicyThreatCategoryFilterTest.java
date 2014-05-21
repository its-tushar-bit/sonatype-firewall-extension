/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PolicyThreatCategoryFilterTest
{

  @Test
  public void testSinglePolicyThreatCategory() {
    PolicyThreatCategoryFilter filter = new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE);
    PolicyViolation trueViolation = new PolicyViolation();
    trueViolation.setThreatCategory(PolicyThreatCategory.LICENSE);

    PolicyViolation falseViolation = new PolicyViolation();
    falseViolation.setThreatCategory(PolicyThreatCategory.OTHER);

    assertThat(filter.asPolicyViolationPredicate().apply(trueViolation), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(falseViolation), is(false));
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

    assertThat(filter.asPolicyViolationPredicate().apply(v1), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(v2), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(v3), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(v4), is(true));
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

    assertThat(filter.asPolicyViolationPredicate().apply(v1), is(false));
    assertThat(filter.asPolicyViolationPredicate().apply(v2), is(false));
    assertThat(filter.asPolicyViolationPredicate().apply(v3), is(false));
    assertThat(filter.asPolicyViolationPredicate().apply(v4), is(false));
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

    assertThat(filter.asPolicyViolationPredicate().apply(v1), is(false));
    assertThat(filter.asPolicyViolationPredicate().apply(v2), is(false));
    assertThat(filter.asPolicyViolationPredicate().apply(v3), is(false));
    assertThat(filter.asPolicyViolationPredicate().apply(v4), is(false));
  }

  @Test
  public void testNullViolationThreatCategory() {
    PolicyThreatCategoryFilter filter = new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE);
    PolicyViolation v1 = new PolicyViolation();
    v1.setThreatCategory(null);

    assertThat(filter.asPolicyViolationPredicate().apply(v1), is(false));
  }

  @Test
  public void testNullViolation() {
    PolicyThreatCategoryFilter filter = new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE);

    assertThat(filter.asPolicyViolationPredicate().apply(null), is(false));
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
    PolicyThreatCategoryFilter singleCategoryWithSpacesFilter = new PolicyThreatCategoryFilter(singleCategoryWithSpaces);
    PolicyThreatCategoryFilter singleCategoryWithDanglingCommaFilter = new PolicyThreatCategoryFilter(
        singleCategoryWithDanglingComma);
    PolicyThreatCategoryFilter multiCategoryWithSpacesFilter = new PolicyThreatCategoryFilter(multiCategoryWithSpaces);
    PolicyThreatCategoryFilter multiCategoryFilter = new PolicyThreatCategoryFilter(multiCategory);

    assertTrue(singleCategoryFilter.asPolicyViolationPredicate().apply(v1));
    assertFalse(singleCategoryFilter.asPolicyViolationPredicate().apply(v2));

    assertTrue(singleCategoryWithSpacesFilter.asPolicyViolationPredicate().apply(v1));
    assertFalse(singleCategoryWithSpacesFilter.asPolicyViolationPredicate().apply(v2));

    assertTrue(singleCategoryWithDanglingCommaFilter.asPolicyViolationPredicate().apply(v1));
    assertFalse(singleCategoryWithDanglingCommaFilter.asPolicyViolationPredicate().apply(v2));

    assertTrue(multiCategoryWithSpacesFilter.asPolicyViolationPredicate().apply(v1));
    assertTrue(multiCategoryWithSpacesFilter.asPolicyViolationPredicate().apply(v2));
    assertFalse(multiCategoryWithSpacesFilter.asPolicyViolationPredicate().apply(v3));

    assertTrue(multiCategoryFilter.asPolicyViolationPredicate().apply(v1));
    assertTrue(multiCategoryFilter.asPolicyViolationPredicate().apply(v2));
    assertFalse(multiCategoryFilter.asPolicyViolationPredicate().apply(v3));
  }

  @Test
  public void testStringConstructionWithMalFormedStrings() {
    String emptyCategory = ",license";
    String nonExistentCategory = "category1,category2";
    String emptyString = "";
    String nullString = null;

    try {
      new PolicyThreatCategoryFilter(emptyCategory);
      fail("Filter should throw a bad request exception for the first category parsing as empty string.");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Unknown policy threat category with name: "));
    }

    try {
      new PolicyThreatCategoryFilter(nonExistentCategory);
      fail("Filter should throw a bad request exception for the first category not being a real category.");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Unknown policy threat category with name: category1"));
    }

    try {
      new PolicyThreatCategoryFilter(emptyString);
      fail("Filter should throw a bad request exception for empty strings.");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Unable to parse policy threat categories from empty or null categories."));
    }

    try {
      new PolicyThreatCategoryFilter(nullString);
      fail("Filter should throw a bad request exception for null strings.");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Unable to parse policy threat categories from empty or null categories."));
    }
  }
}
