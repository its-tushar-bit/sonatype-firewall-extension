/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LegalListIndexQueryBuilderTest
{
  private static final String BASE = "itemType:LEGAL_VIOLATION"
      + " AND applicationId:[* TO *]"
      + " AND componentHash:[* TO *]"
      + " AND componentEffectiveLicenseId:[* TO *]"
      + " AND policyEvaluationStage:[* TO *]";

  @Mock
  private Configuration configuration;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private StageTypeService stageTypeService;

  private LegalListIndexQueryBuilder newBuilder() {
    return new LegalListIndexQueryBuilder(
        new DashboardIndexDimensionQueryBuilder(organizationDAO, configuration),
        stageTypeService);
  }

  @BeforeEach
  public void stubLicensedStages() {
    StageType build = mock(StageType.class);
    lenient().when(build.getId()).thenReturn("build");
    StageType operate = mock(StageType.class);
    lenient().when(operate.getId()).thenReturn("operate");
    lenient().when(stageTypeService.getLicensedStageTypes(any()))
        .thenReturn(List.of(build, operate));
  }

  @Test
  public void buildLegalQuery_nullOrBlankSearch_returnsDisplayableBaseOnly() {
    assertThat(newBuilder().buildLegalQuery(null)).isEqualTo(BASE);

    LegalListRequestDTO request = new LegalListRequestDTO();
    assertThat(newBuilder().buildLegalQuery(request)).isEqualTo(BASE);

    request.search = "   ";
    assertThat(newBuilder().buildLegalQuery(request)).isEqualTo(BASE);
  }

  @Test
  public void buildLegalQuery_singleTerm_matchesAllSixSearchFields() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.search = "apache";

    assertThat(newBuilder().buildLegalQuery(request)).isEqualTo(
        BASE + " AND (componentName:*apache* OR applicationName:*apache* OR applicationPublicId:*apache*"
            + " OR organizationName:*apache* OR componentEffectiveLicenseName:*apache*"
            + " OR componentLicenseThreatGroupName:*apache*)");
  }

  @Test
  public void buildLegalQuery_multiWordSearch_andsTokensAcrossFields() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.search = "apache gpl";

    assertThat(newBuilder().buildLegalQuery(request)).isEqualTo(
        BASE + " AND ((componentName:*apache* OR applicationName:*apache* OR applicationPublicId:*apache*"
            + " OR organizationName:*apache* OR componentEffectiveLicenseName:*apache*"
            + " OR componentLicenseThreatGroupName:*apache*)"
            + " AND (componentName:*gpl* OR applicationName:*gpl* OR applicationPublicId:*gpl*"
            + " OR organizationName:*gpl* OR componentEffectiveLicenseName:*gpl*"
            + " OR componentLicenseThreatGroupName:*gpl*))");
  }

  @Test
  public void buildLegalQuery_threatLevelRange_bothBounds() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.licenseThreatLevelRange = new PolicyThreatLevelFilter(7, 10);

    assertThat(newBuilder().buildLegalQuery(request))
        .isEqualTo(BASE + " AND componentLicenseThreatLevel:[7 TO 10]");
  }

  @Test
  public void buildLegalQuery_threatLevelRange_minOnly_clampsMaxToTen() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.licenseThreatLevelRange = new PolicyThreatLevelFilter(7, null);

    assertThat(newBuilder().buildLegalQuery(request))
        .isEqualTo(BASE + " AND componentLicenseThreatLevel:[7 TO 10]");
  }

  @Test
  public void buildLegalQuery_threatLevelRange_maxOnly_clampsMinToZero() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.licenseThreatLevelRange = new PolicyThreatLevelFilter(null, 5);

    assertThat(newBuilder().buildLegalQuery(request))
        .isEqualTo(BASE + " AND componentLicenseThreatLevel:[0 TO 5]");
  }

  @Test
  public void buildLegalQuery_threatLevelRange_noBounds_omitsClause() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.licenseThreatLevelRange = new PolicyThreatLevelFilter((Integer) null, (Integer) null);

    assertThat(newBuilder().buildLegalQuery(request)).isEqualTo(BASE);
  }

  @Test
  public void buildLegalQuery_licenseThreatGroupNames_quotesMultiWordPhrases() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.licenseThreatGroupNames = Set.of("Copyleft", "Weak Copyleft");

    assertThat(newBuilder().buildLegalQuery(request))
        .isEqualTo(BASE + " AND componentLicenseThreatGroupName:(\"Copyleft\" \"Weak Copyleft\")");
  }

  @Test
  public void buildLegalQuery_stageFilter_matchesLicensedStageIds() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.stageIds = Set.of("build");

    assertThat(newBuilder().buildLegalQuery(request))
        .isEqualTo(BASE + " AND policyEvaluationStage:(build)");
  }

  @Test
  public void buildLegalQuery_rejectsUnlicensedStageId() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.stageIds = Set.of("not-a-licensed-stage");

    assertThatThrownBy(() -> newBuilder().buildLegalQuery(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("not-a-licensed-stage")
        .hasMessageContaining("dashboard-licensed");
  }

  @Test
  public void buildLegalQuery_rejectsBlankStageId() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    Set<String> stageIds = new LinkedHashSet<>();
    stageIds.add("build");
    stageIds.add("  ");
    request.stageIds = stageIds;

    assertThatThrownBy(() -> newBuilder().buildLegalQuery(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("stageIds");
  }

  @Test
  public void buildLegalQuery_rootOrgWithApplicationFilter_appFilterTakesPrecedence() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(10);

    LegalListRequestDTO request = new LegalListRequestDTO();
    request.organizationIds = Set.of(Organization.ROOT_ORGANIZATION_ID);
    request.applicationIds = Set.of("appx");

    assertThat(newBuilder().buildLegalQuery(request))
        .isEqualTo(BASE + " AND (applicationId:(appx))");
  }

  @Test
  public void buildLegalQuery_rejectsTooManyApplicationIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);

    Set<String> applicationIds = new LinkedHashSet<>();
    IntStream.range(0, 3).forEach(i -> applicationIds.add("app-" + i));
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.applicationIds = applicationIds;

    assertThatThrownBy(() -> newBuilder().buildLegalQuery(request))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void buildLegalQuery_combinesFiltersWithAnd() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.search = "mit";
    request.licenseThreatLevelRange = new PolicyThreatLevelFilter(7, 10);
    request.licenseThreatGroupNames = Set.of("Permissive");
    request.stageIds = Set.of("build");

    String query = newBuilder().buildLegalQuery(request);

    assertThat(query).startsWith(BASE + " AND ");
    assertThat(query).contains("componentEffectiveLicenseName:*mit*");
    assertThat(query).contains("componentLicenseThreatLevel:[7 TO 10]");
    assertThat(query).contains("componentLicenseThreatGroupName:(\"Permissive\")");
    assertThat(query).contains("policyEvaluationStage:(build)");
  }
}
