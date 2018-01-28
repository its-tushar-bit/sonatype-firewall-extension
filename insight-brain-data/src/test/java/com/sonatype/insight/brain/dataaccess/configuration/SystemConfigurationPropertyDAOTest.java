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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

public class SystemConfigurationPropertyDAOTest
    extends AbstractDbDAOTest
{
  private SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  @Test
  public void testCRUD() throws Exception {
    SystemConfigurationProperty property = new SystemConfigurationProperty("TEST-NAME", "TEST-VALUE");
    dao.insert(property);

    property = dao.getByNameNotNull("TEST-NAME");
    assertThat(property.getName(), is("TEST-NAME"));
    assertThat(property.getValue(), is("TEST-VALUE"));

    property.setValue("UPDATED-VALUE");
    dao.update(property);

    property = dao.getByNameNotNull("TEST-NAME");
    assertThat(property.getValue(), is("UPDATED-VALUE"));

    dao.delete(property);

    property = dao.getByName("TEST-NAME");
    assertThat(property, is(nullValue()));
    try {
      dao.getByNameNotNull("TEST-NAME");
      fail("Expected exception");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), containsString("TEST-NAME"));
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
}
