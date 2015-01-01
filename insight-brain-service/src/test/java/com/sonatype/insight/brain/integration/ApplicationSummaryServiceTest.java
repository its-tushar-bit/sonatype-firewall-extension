/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApplicationSummaryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationSummaryService service;

  @Test
  public void testGetApplications_SortedByCaseInsensitiveName() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent("y", "AA");
    Application app0 = tempEntity.newApplicationWithParent("z", "a b");
    Application app2 = tempEntity.newApplicationWithParent("x", "c");

    ApplicationSummaryList applicationListDTO = service.getApplications();
    assertThat(applicationListDTO, notNullValue());
    assertThat(applicationListDTO.getApplicationSummaries(), hasSize(3));
    assertThat(applicationListDTO.getApplicationSummaries().get(0).getId(), is(app0.getId()));
    assertThat(applicationListDTO.getApplicationSummaries().get(1).getId(), is(app1.getId()));
    assertThat(applicationListDTO.getApplicationSummaries().get(2).getId(), is(app2.getId()));
  }
}
