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
    tempEntity.newSystemConfigurationProperty("TEST-NAME", "TEST-VALUE");
    SystemConfigurationProperty property = service.getByName("TEST-NAME");
    assertThat(property, is(notNullValue()));
    assertThat(property.getValue(), is("TEST-VALUE"));
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
    tempEntity.newSystemConfigurationProperty("TEST-NAME", "TEST-VALUE");
    service.update(new SystemConfigurationProperty("TEST-NAME", "UPDATED-VALUE"));
    SystemConfigurationProperty updated = dao.getByNameNotNull("TEST-NAME");
    assertThat(updated.getValue(), is("UPDATED-VALUE"));
  }
}
