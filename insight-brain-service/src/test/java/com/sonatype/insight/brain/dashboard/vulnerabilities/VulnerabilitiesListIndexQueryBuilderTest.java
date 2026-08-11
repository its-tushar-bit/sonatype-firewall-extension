/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class VulnerabilitiesListIndexQueryBuilderTest
{
  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private Configuration configuration;

  private VulnerabilitiesListIndexQueryBuilder builder() {
    return new VulnerabilitiesListIndexQueryBuilder(
        new DashboardIndexDimensionQueryBuilder(organizationDAO, configuration));
  }

  @Test
  public void restrictToVulnerabilityIds_keepsBaseClausesAndAddsAnIdClause() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.ecosystems = Set.of("maven");
    String baseQuery = builder().buildMyScanDataQuery(request);

    String restricted =
        builder().restrictToVulnerabilityIds(baseQuery, List.of("cve-2021-44228", "cve-2021-45046"));

    assertThat(restricted).startsWith(baseQuery + " AND ");
    assertThat(restricted)
        .endsWith("vulnerabilityId:(cve\\-2021\\-44228 cve\\-2021\\-45046)");
  }

  @Test
  public void restrictToVulnerabilityIds_escapesLuceneSyntaxInIds() {
    String restricted = builder().restrictToVulnerabilityIds(
        "itemType:SECURITY_VULNERABILITY", List.of("cve-1*", "sonatype-2021-0001"));

    assertThat(restricted).isEqualTo(
        "itemType:SECURITY_VULNERABILITY AND vulnerabilityId:(cve\\-1\\* sonatype\\-2021\\-0001)");
  }

  @Test
  public void restrictToVulnerabilityIds_blankAndEmptyInputLeaveTheBaseQueryUnchanged() {
    String baseQuery = "itemType:SECURITY_VULNERABILITY";

    assertThat(builder().restrictToVulnerabilityIds(baseQuery, List.of())).isEqualTo(baseQuery);
    assertThat(builder().restrictToVulnerabilityIds(baseQuery, null)).isEqualTo(baseQuery);
    assertThat(builder().restrictToVulnerabilityIds(baseQuery, List.of("  "))).isEqualTo(baseQuery);
  }
}
