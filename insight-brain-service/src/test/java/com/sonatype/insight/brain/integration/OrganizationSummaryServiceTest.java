/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.organization.OrganizationSummary;
import com.sonatype.clm.dto.model.organization.OrganizationSummaryList;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationSummaryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private OrganizationSummaryService service;

  @Test
  public void testGetOrganizations_SortedByCaseInsensitiveName_EVALUATE_APPLICATION() throws Exception {
    testGetOrganizations_SortedByCaseInsensitiveName(Goal.EVALUATE_APPLICATION);
  }

  @Test
  public void testGetOrganizations_SortedByCaseInsensitiveName_EVALUATE_COMPONENT() throws Exception {
    testGetOrganizations_SortedByCaseInsensitiveName(Goal.EVALUATE_COMPONENT);
  }

  @Test
  public void testGetOrganizations_SortedByCaseInsensitiveName_WithoutGoal() throws Exception {
    testGetOrganizations_SortedByCaseInsensitiveName(null);
  }

  private void testGetOrganizations_SortedByCaseInsensitiveName(Goal goal) {
    Organization org0 = tempEntity.newOrganization("A");
    Organization org1 = tempEntity.newOrganization("B");
    Organization org2 = tempEntity.newOrganization("C");

    OrganizationSummaryList organizationListDTO = service.getOrganizations(goal);
    assertThat(organizationListDTO).isNotNull();
    assertThat(organizationListDTO.getOrganizationSummaries()).extracting(OrganizationSummary::getId)
        .containsExactly(org0.getId(), org1.getId(), org2.getId());
  }
}
