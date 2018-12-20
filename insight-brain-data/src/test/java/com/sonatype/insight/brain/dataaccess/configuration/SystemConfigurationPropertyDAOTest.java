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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SystemConfigurationPropertyDAOTest
    extends AbstractDbDAOTest
{
  private SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  @Test
  public void testCRUD() throws Exception {
    SystemConfigurationProperty property = new SystemConfigurationProperty("TEST-NAME", "TEST-VALUE");
    dao.insert(property);

    property = dao.getByNameNotNull("TEST-NAME");
    assertThat(property.getName()).isEqualTo("TEST-NAME");
    assertThat(property.getValue()).isEqualTo("TEST-VALUE");

    property.setValue("UPDATED-VALUE");
    dao.update(property);

    property = dao.getByNameNotNull("TEST-NAME");
    assertThat(property.getValue()).isEqualTo("UPDATED-VALUE");

    dao.delete(property);

    property = dao.getByName("TEST-NAME");
    assertThat(property).isNull();
    assertThatThrownBy(() -> {
      dao.getByNameNotNull("TEST-NAME");
    }).isInstanceOf(NotFoundException.class).hasMessageContaining("TEST-NAME");
  }

  @Test
  public void testUpdateNonExistent() {
    assertThatThrownBy(() -> {
      dao.update(new SystemConfigurationProperty("FOO", "value"));
    }).isInstanceOf(NotFoundException.class).hasMessage("A system configuration property 'FOO' does not exist.");
  }

  @Test
  public void getGetByNameNotNullThrowsExceptionIfNotFound() {
    assertThatThrownBy(() -> {
      dao.getByNameNotNull("FOO");
    }).isInstanceOf(NotFoundException.class).hasMessage("A system configuration property 'FOO' does not exist.");
  }
}
