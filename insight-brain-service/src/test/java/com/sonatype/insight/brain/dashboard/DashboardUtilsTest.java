/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class DashboardUtilsTest
    extends InjectedTest
{
  @Inject
  private DashboardUtils dashboardUtils;

  @Test
  public void testPolicyViolationFilter() {
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    PolicyViolation v4 = new PolicyViolation();
    PolicyViolation v5 = new PolicyViolation();
    PolicyViolation v6 = new PolicyViolation();
    v1.setThreatCategory(PolicyThreatCategory.LICENSE);
    v2.setThreatCategory(PolicyThreatCategory.LICENSE);
    v3.setThreatCategory(PolicyThreatCategory.OTHER);
    v4.setThreatCategory(PolicyThreatCategory.QUALITY);
    v5.setThreatCategory(PolicyThreatCategory.SECURITY);
    v6.setThreatCategory(PolicyThreatCategory.SECURITY);
    v1.setThreatLevel(1);
    v2.setThreatLevel(2);
    v3.setThreatLevel(3);
    v4.setThreatLevel(4);
    v5.setThreatLevel(5);
    v6.setThreatLevel(6);

    List<PolicyViolation> violations = Lists.newArrayList(v1, v2, v3, v4, v5, v6);
    List<PolicyViolation> filtered;

    // Test minimum range.
    filtered = dashboardUtils.filter(violations, new PolicyThreatLevelFilter(3, null).asPolicyViolationPredicate());
    assertThat(filtered, contains(v3, v4, v5, v6));

    // Test maximum range.
    filtered = dashboardUtils.filter(violations, new PolicyThreatLevelFilter(null, 3).asPolicyViolationPredicate());
    assertThat(filtered, contains(v1, v2, v3));

    // Test minimum and maximum range.
    filtered = dashboardUtils.filter(violations, new PolicyThreatLevelFilter(3, 3).asPolicyViolationPredicate());
    assertThat(filtered, contains(v3));

    // Test single policy threat category.
    filtered = dashboardUtils.filter(violations,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE).asPolicyViolationPredicate());
    assertThat(filtered, contains(v1, v2));

    // Test multiple policy threat category.
    filtered = dashboardUtils.filter(violations, new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE,
        PolicyThreatCategory.SECURITY).asPolicyViolationPredicate());
    assertThat(filtered, contains(v1, v2, v5, v6));

    // Test multiple policy threat category and threat range.
    filtered = dashboardUtils.filter(violations, Predicates.and(new PolicyThreatCategoryFilter(
        PolicyThreatCategory.LICENSE, PolicyThreatCategory.SECURITY).asPolicyViolationPredicate(),
        new PolicyThreatLevelFilter(2, 5).asPolicyViolationPredicate()));
    assertThat(filtered, contains(v2, v5));
  }
}
