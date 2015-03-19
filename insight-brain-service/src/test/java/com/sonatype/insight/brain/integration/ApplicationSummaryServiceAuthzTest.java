/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApplicationSummaryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationSummaryService service;

  @Test
  public void testGetApplications_Anonymous() {
    ApplicationSummaryList list = service.getApplications();
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(1));
  }

  @Test
  public void testGetApplications_Authorized() {
    grantWritePermission(app.getId());
    ApplicationSummaryList list = service.getApplications();
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(1));
  }

  @Test
  public void testGetApplications_Unauthorized() {
    login();
    ApplicationSummaryList list = service.getApplications();
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(0));
  }
}
