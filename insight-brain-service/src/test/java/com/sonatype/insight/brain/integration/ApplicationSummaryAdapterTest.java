/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.model.Application;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ApplicationSummaryAdapterTest
{
  private ApplicationSummaryAdapter applicationAdapter = new ApplicationSummaryAdapter();

  @Test
  public void testConvertApplication() {
    Application application = new Application();
    application.setId("testId");
    application.setPublicId("testPublicId");
    application.setName("testName");

    ApplicationSummary applicationSummary = applicationAdapter.convert(application);
    assertThat(applicationSummary.getId(), is(application.getId()));
    assertThat(applicationSummary.getPublicId(), is(application.getPublicId()));
    assertThat(applicationSummary.getName(), is(application.getName()));
  }

  @Test
  public void testConvertApplication_Null() {
    assertThat(applicationAdapter.convert((Application) null), nullValue());
  }

  @Test
  public void testConvertApplicationList() {
    List<Application> applicationList = new ArrayList<>();
    Application application = new Application();
    application.setId("testId");
    application.setPublicId("testPublicId");
    application.setName("testName");
    applicationList.add(application);

    ApplicationSummaryList applicationSummaryList = applicationAdapter.convert(applicationList);
    assertThat(applicationSummaryList, notNullValue());
    assertThat(applicationSummaryList.getApplicationSummaries(), hasSize(1));
    ApplicationSummary applicationSummary = applicationSummaryList.getApplicationSummaries().get(0);
    assertThat(applicationSummary.getId(), is(application.getId()));
    assertThat(applicationSummary.getPublicId(), is(application.getPublicId()));
    assertThat(applicationSummary.getName(), is(application.getName()));
  }

  @Test
  public void testConvertApplicationList_EmptyList() {
    ApplicationSummaryList applicationSummaryList = applicationAdapter.convert(Collections.<Application>emptyList());
    assertThat(applicationSummaryList, notNullValue());
    assertThat(applicationSummaryList.getApplicationSummaries(), hasSize(0));
  }

  @Test
  public void testConvertApplicationList_NullList() {
    List<Application> applicationList = null;
    ApplicationSummaryList applicationSummaryList = applicationAdapter.convert(applicationList);
    assertThat(applicationSummaryList, notNullValue());
    assertThat(applicationSummaryList.getApplicationSummaries(), hasSize(0));
  }
}
