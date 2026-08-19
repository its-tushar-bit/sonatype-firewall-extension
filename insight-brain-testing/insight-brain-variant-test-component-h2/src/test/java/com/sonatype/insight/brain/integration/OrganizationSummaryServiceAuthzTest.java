/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.clm.dto.model.organization.OrganizationSummary;
import com.sonatype.clm.dto.model.organization.OrganizationSummaryList;
import com.sonatype.insight.brain.StaticInjectionTestHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class OrganizationSummaryServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private OrganizationSummaryService service;

  private Organization otherOrg;

  @Override
  @BeforeEach
  public void beforeTest() {
    // Ensure static dependencies are injected before parent's beforeTest() runs
    StaticInjectionTestHelper.inject(daoFactory);
    // Call parent's beforeTest which will call setUpSecurity()
    super.beforeTest();
    // Create organization after injector is created
    otherOrg = tempEntity.newOrganization("Z");
  }

  @Test
  public void testGetApplications_Authorized_NullGoal() {
    grantReadPermission(org.getId());
    grantReadPermission(otherOrg.getId());
    OrganizationSummaryList list = service.getOrganizations(null /* goal */);
    assertThat(list).isNotNull();
    assertThat(list.getOrganizationSummaries()).extracting(OrganizationSummary::getId)
        .containsExactly(org.getId(), otherOrg.getId());
  }

  @Test
  public void testGetApplications_Authorized_EVALUATE_APPLICATION() {
    grantPermission(org.getId(), Permission.EVALUATE_APPLICATION);
    OrganizationSummaryList list = service.getOrganizations(Goal.EVALUATE_APPLICATION);
    assertThat(list).isNotNull();
    assertThat(list.getOrganizationSummaries()).extracting(OrganizationSummary::getId)
        .containsExactly(org.getId());
  }

  @Test
  public void testGetApplications_Authorized_EVALUATE_COMPONENT() {
    grantReadPermission(org.getId());
    grantReadPermission(otherOrg.getId());
    OrganizationSummaryList list = service.getOrganizations(Goal.EVALUATE_COMPONENT);
    assertThat(list).isNotNull();
    assertThat(list.getOrganizationSummaries()).extracting(OrganizationSummary::getId)
        .containsExactly(org.getId(), otherOrg.getId());
  }

  @Test
  public void testGetApplications_Unauthorized_NullGoal() {
    login();
    OrganizationSummaryList list = service.getOrganizations(null /* goal */);
    assertThat(list).isNotNull();
    assertThat(list.getOrganizationSummaries()).isEmpty();
  }

  @Test
  public void testGetApplications_Unauthorized_EVALUATE_APPLICATION() {
    login();
    OrganizationSummaryList list = service.getOrganizations(Goal.EVALUATE_APPLICATION);
    assertThat(list).isNotNull();
    assertThat(list.getOrganizationSummaries()).isEmpty();
  }
}
