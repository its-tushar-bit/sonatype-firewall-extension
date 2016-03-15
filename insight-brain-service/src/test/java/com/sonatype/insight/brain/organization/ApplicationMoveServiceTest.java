/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class ApplicationMoveServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationMoveService applicationMoveService;

  @Test
  public void testGetDestinationOrganizations_SortedByName() {
    Organization orgA = tempEntity.newOrganization("Org A");
    Organization orgC = tempEntity.newOrganization("Org C");
    Organization orgE = tempEntity.newOrganization("Org E");
    Organization orgB = tempEntity.newOrganization("Org B");
    Organization orgD = tempEntity.newOrganization("Org D");
    Application app = tempEntity.newApplicationWithParent("test-app-id");

    List<Organization> orgs = applicationMoveService.getDestinationOrganizations(app.getId());
    assertThat(orgs, hasSize(5));
    assertThat(orgs.get(0).getId(), is(orgA.getId()));
    assertThat(orgs.get(1).getId(), is(orgB.getId()));
    assertThat(orgs.get(2).getId(), is(orgC.getId()));
    assertThat(orgs.get(3).getId(), is(orgD.getId()));
    assertThat(orgs.get(4).getId(), is(orgE.getId()));
  }
}
