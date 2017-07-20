/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class SystemConfigurationPropertyDAOTest
    extends AbstractDbDAOTest
{
  private SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  @Test
  public void testGetByNameNotNullAndUpdate() throws Exception {
    // get by name
    SystemConfigurationProperty property = dao.getByNameNotNull("SUCCESS_METRICS_ENABLED");
    assertThat(property.getName(), is("SUCCESS_METRICS_ENABLED"));
    assertThat(property.getValue(), is("true"));

    // Update
    try {
      property.setValue("false");
      dao.update(property);

      // Read
      property = dao.getByNameNotNull("SUCCESS_METRICS_ENABLED");
      assertThat(property.getName(), is("SUCCESS_METRICS_ENABLED"));
      assertThat(property.getValue(), is("false"));
    } finally {
      // restore global value
      dao.update(new SystemConfigurationProperty("SUCCESS_METRICS_ENABLED", "true"));
    }
  }

  @Test
  public void testUpdateNonExistent() {
    try {
      dao.update(new SystemConfigurationProperty("FOO", "value"));
      fail("Expected exception");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is("A system configuration property 'FOO' does not exist."));
    }
  }

  @Test
  public void getGetByNameNotNullThrowsExceptionIfNotFound() {
    try {
      dao.getByNameNotNull("FOO");
      fail("Expected exception");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is("A system configuration property 'FOO' does not exist."));
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testInsert_throwsUnsupportedOperationException() {
    dao.insert(new SystemConfigurationProperty("foo", "bar"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testDelete_throwsUnsupportedOperationException() {
    dao.delete(new SystemConfigurationProperty("SUCCESS_METRICS_ENABLED", "true"));
  }
}
