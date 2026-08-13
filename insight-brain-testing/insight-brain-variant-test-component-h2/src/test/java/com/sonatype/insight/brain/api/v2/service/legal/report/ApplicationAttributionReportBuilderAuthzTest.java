/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal.report;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.service.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.filter.AdvancedLegalPackDashboardFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.json.store.JsonUtils;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

@ComponentH2Test
public class ApplicationAttributionReportBuilderAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApplicationAttributionReportBuilder applicationAttributionReportBuilder;

  @Mock
  private ApiLicenseLegalService apiLicenseLegalServiceMock;

  @Captor
  private ArgumentCaptor<List<Owner>> ownerCaptor;

  @Test
  public void testGenerateLegalAttributionApplicationReport_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(
            app,
            BuildStageType.ID,
            LegalCustomReportParameters.builder().buildWithDefaults("app")));
  }

  @Test
  public void testGenerateLegalAttributionApplicationReport_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(
            app,
            BuildStageType.ID,
            LegalCustomReportParameters.builder().buildWithDefaults("app")));
  }

  @Test
  public void testGenerateLegalAttributionApplicationReport_Authorized() {
    grantLegalReviewerPermission(app.getId());
    LegalCustomReportParameters params = LegalCustomReportParameters.builder().buildWithDefaults("app");

    when(apiLicenseLegalServiceMock.getLicenseLegalApplicationReport( //
        app, //
        BuildStageType.ID, //
        params.isIncludeInnerSource(), //
        params.isIncludeSonatypeSpecialLicenses())) //
            .thenReturn(new ApiLicenseLegalApplicationReportDTO());

    String result = applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(app,
        BuildStageType.ID, params);
    assertThat(result).isNotBlank();
  }

  @Test
  public void testGenerateLegalAttributionApplicationReportFormActiveFilter_Authorized() {
    grantLegalReviewerPermission(app.getId());
    String filterName = "test filter";
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add(app.getId());
    advancedLegalPackDashboardFilter.getStageTypeFilters().add(BuildStageType.ID);
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(advancedLegalPackDashboardFilter), filterName);

    LegalCustomReportParameters params = LegalCustomReportParameters.builder().buildWithDefaults("app");

    when(apiLicenseLegalServiceMock.getLicenseLegalMultiApplicationReport( //
        ownerCaptor.capture(), //
        eq(Collections.singletonList(BuildStageType.ID)), //
        eq(params.isIncludeInnerSource()), //
        eq(params.isIncludeSonatypeSpecialLicenses()))) //
            .thenReturn(Collections.emptySet());

    String result =
        applicationAttributionReportBuilder.generateLegalMultiApplicationAttributionReportFromActiveUserFilter(params);
    assertThat(result).isNotBlank();
    assertThat(ownerCaptor.getValue()).extracting(Owner::getId).containsExactly(app.getId());
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

    LegalCustomReportParameters params = LegalCustomReportParameters.builder().buildWithDefaults("app");

    when(apiLicenseLegalServiceMock.getLicenseLegalMultiApplicationReport( //
        ownerCaptor.capture(), //
        eq(Collections.singletonList(BuildStageType.ID)), //
        eq(params.isIncludeInnerSource()), //
        eq(params.isIncludeSonatypeSpecialLicenses()))) //
            .thenReturn(Collections.emptySet());

    String result =
        applicationAttributionReportBuilder.generateLegalMultiApplicationAttributionReportFromActiveUserFilter(params);

    assertThat(result).isNotBlank();
    assertThat(ownerCaptor.getValue()).extracting(Owner::getId).containsExactly(app.getId());
  }

  @Test
  public void testGenerateLegalAttributionApplicationReportFormActiveFilter_UnauthorizedApp() {
    login();
    String filterName = "test filter";
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add(app.getId());
    advancedLegalPackDashboardFilter.getStageTypeFilters().add(BuildStageType.ID);
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(advancedLegalPackDashboardFilter), filterName);
    assertThrows(UnauthorizedException.class,
        () -> applicationAttributionReportBuilder.generateLegalMultiApplicationAttributionReportFromActiveUserFilter(
            LegalCustomReportParameters.builder().buildWithDefaults("app")));
  }
}
