/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.base.Predicate;
import org.codehaus.plexus.util.StringUtils;

public class PolicyThreatCategoryFilter
    implements Predicate<PolicyViolation>
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

  @Override
  public boolean apply(PolicyViolation input) {
    return (input != null) ? policyThreatCategories.contains(input.getThreatCategory()) : false;
  }

}
