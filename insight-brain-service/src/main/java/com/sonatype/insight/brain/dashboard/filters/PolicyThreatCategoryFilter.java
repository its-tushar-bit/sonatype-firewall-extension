/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

public class PolicyThreatCategoryFilter
    implements Predicate<PolicyThreatCategory>
{
  private Set<PolicyThreatCategory> policyThreatCategories = EnumSet.noneOf(PolicyThreatCategory.class);

  public PolicyThreatCategoryFilter() {
    // No argument constructor for convenience.
  }

  /**
   * @param categories A comma delimited list of {@link PolicyThreatCategory}s.
   */
  public PolicyThreatCategoryFilter(String categories) {
    if (StringUtils.isBlank(categories)) {
      throw new BadRequestException("Unable to parse policy threat categories from empty or null categories.");
    }

    try {
      for (String category : categories.split(",")) {
        policyThreatCategories.add(PolicyThreatCategory.getByName(category.trim().toLowerCase(Locale.ENGLISH)));
      }
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  public PolicyThreatCategoryFilter(Collection<PolicyThreatCategory> categories) {
    if (categories != null) {
      policyThreatCategories.addAll(categories);
    }
  }

  public PolicyThreatCategoryFilter(PolicyThreatCategory... categories) {
    if (categories != null) {
      Collections.addAll(policyThreatCategories, categories);
    }
  }

  @Override
  public boolean test(PolicyThreatCategory policyThreatCategory) {
    return policyThreatCategories.contains(policyThreatCategory);
  }

  public Set<PolicyThreatCategory> getPolicyThreatCategories() {
    return policyThreatCategories;
  }
}
