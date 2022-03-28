/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal.report;

import com.sonatype.insight.brain.filter.AdvancedLegalPackDashboardFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApplicationAttributionReportBuilderAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationAttributionReportBuilder applicationAttributionReportBuilder;

  @Test(expected = UnauthenticatedException.class)
  public void testGenerateLegalAttributionApplicationReport_Unauthenticated() {
    applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(
        app,
        BuildStageType.ID,
        LegalCustomReportParameters.builder().buildWithDefaults("app"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testGenerateLegalAttributionApplicationReport_Unauthorized() {
    login();
    applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(
        app,
        BuildStageType.ID,
        LegalCustomReportParameters.builder().buildWithDefaults("app"));
  }

  @Test(expected = NotFoundException.class)
  public void testGenerateLegalAttributionApplicationReport_Authorized() {
    grantLegalReviewerPermission(app.getId());
    applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(
        app,
        BuildStageType.ID,
        LegalCustomReportParameters.builder().buildWithDefaults("app"));
  }

  @Test(expected = NotFoundException.class)
  public void testGenerateLegalAttributionApplicationReportFormActiveFilter_Authorized() {
    grantLegalReviewerPermission(app.getId());
    String filterName = "test filter";
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add(app.getId());
    advancedLegalPackDashboardFilter.getStageTypeFilters().add(BuildStageType.ID);
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(advancedLegalPackDashboardFilter), filterName);
    applicationAttributionReportBuilder.generateLegalMultiApplicationAttributionReportFromActiveUserFilter();
  }

  @Test
  public void testGenerateLegalAttributionApplicationReportFormActiveFilter_AuthorizedOneApplicationOfTwo() {
    grantLegalReviewerPermission(app.getId());
    Application app2 = tempEntity.newApplicationWithParent();
    String filterName = "test filter";
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add(app.getId());
    advancedLegalPackDashboardFilter.getApplicationFilters().add(app2.getId());
    advancedLegalPackDashboardFilter.getStageTypeFilters().add(BuildStageType.ID);
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(advancedLegalPackDashboardFilter), filterName);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> applicationAttributionReportBuilder.generateLegalMultiApplicationAttributionReportFromActiveUserFilter())
        .withMessageContaining("Report for applications " + app.getId() + " not found");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGenerateLegalAttributionApplicationReportFormActiveFilter_UnauthorizedApp() {
    login();
    String filterName = "test filter";
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add(app.getId());
    advancedLegalPackDashboardFilter.getStageTypeFilters().add(BuildStageType.ID);
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(advancedLegalPackDashboardFilter), filterName);
    applicationAttributionReportBuilder.generateLegalMultiApplicationAttributionReportFromActiveUserFilter();
  }
}
