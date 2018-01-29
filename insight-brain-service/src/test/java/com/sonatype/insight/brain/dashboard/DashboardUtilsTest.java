/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DashboardUtilsTest
    extends InjectedTest
{
  @Inject
  private DashboardUtils dashboardUtils;

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private TestProductLicenseManager productLicenseManager;

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

  @Test
  public void testGetApplicationIds() {
    // Empty list
    List<Application> apps = new ArrayList<>();
    Set<String> appIds = dashboardUtils.getApplicationIds(apps);
    assertThat(appIds, hasSize(0));

    // One app
    Application app1 = new Application();
    app1.setId("app1");
    apps.add(app1);
    appIds = dashboardUtils.getApplicationIds(apps);
    assertThat(appIds, contains(app1.getId()));

    // Two apps
    Application app2 = new Application();
    app1.setId("app2");
    apps.add(app2);
    appIds = dashboardUtils.getApplicationIds(apps);
    assertThat(appIds, containsInAnyOrder(app1.getId(), app2.getId()));
  }

  @Test
  public void testGetStageTypes_StageTypeIdsNull() {
    assertThat(dashboardUtils.getStageTypes(null),
        contains(StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

  @Test
  public void testGetStageTypes_StageTypeIdsEmpty() {
    assertThat(dashboardUtils.getStageTypes(Collections.emptySet()),
        contains(StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

  @Test
  public void testGetStageTypes_InvalidStageTypeId() {
    try {
      dashboardUtils.getStageTypes(Collections.singleton("invalid-stage-type-id"));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid stage type: invalid-stage-type-id."));
    }
  }

  @Test
  public void testGetStageTypes_UnlicensedStageTypeId() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    try {
      dashboardUtils.getStageTypes(Collections.singleton(StageTypes.BUILD.getId()));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Current license does not support stage type: build."));
    }
  }
}
