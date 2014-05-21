/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.codehaus.plexus.util.StringUtils;

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
  public boolean apply(PolicyThreatCategory policyThreatCategory) {
    return policyThreatCategories.contains(policyThreatCategory);
  }

  /**
   * Transforms this predicate into one that applies the same filtering to policy violations.
   */
  public Predicate<PolicyViolation> asPolicyViolationPredicate() {
    return Predicates.compose(this, new Function<PolicyViolation, PolicyThreatCategory>()
    {
      @Override
      @Nullable
      public PolicyThreatCategory apply(@Nullable PolicyViolation input) {
        return (input != null) ? input.getThreatCategory() : null;
      }
    });
  }

  /**
   * Transforms this predicate into one that applies the same filtering to policies.
   */
  public Predicate<Policy> asPolicyPredicate() {
    return Predicates.compose(this, new Function<Policy, PolicyThreatCategory>()
    {
      @Override
      @Nullable
      public PolicyThreatCategory apply(@Nullable Policy input) {
        return (input != null) ? input.getThreatCategory() : null;
      }
    });
  }
}
