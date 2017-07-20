/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SystemConfigurationPropertyServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SystemConfigurationPropertyService service;

  @Inject
  private SystemConfigurationPropertyDAO dao;

  @Test
  public void testGetByName() {
    SystemConfigurationProperty property = service.getByName("SUCCESS_METRICS_ENABLED");
    assertThat(property, is(notNullValue()));
  }

  @Test
  public void testGetByName_NonExisting() {
    try {
      service.getByName("foo");
      fail("Expected exception");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("A system configuration property 'foo' does not exist."));
    }
  }

  @Test
  public void testUpdate() {
    try {
      service.update(new SystemConfigurationProperty("SUCCESS_METRICS_ENABLED", "false"));
      SystemConfigurationProperty updated = dao.getByNameNotNull("SUCCESS_METRICS_ENABLED");
      assertThat(updated.getValue(), equalTo("false"));
    }
    finally {
      // restore global value
      new SystemConfigurationPropertyDAO().update(new SystemConfigurationProperty("SUCCESS_METRICS_ENABLED", "true"));
    }
  }
}
