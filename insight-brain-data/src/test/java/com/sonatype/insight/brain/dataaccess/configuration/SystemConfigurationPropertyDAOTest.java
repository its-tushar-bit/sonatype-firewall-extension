/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SystemConfigurationPropertyDAOTest
    extends AbstractDbDAOTest
{
  private SystemConfigurationPropertyDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createSystemConfigurationPropertyDAO();
  }

  private static final String DUMMY_PROPERTY_NAME = "TEST-NAME";

  @After
  public void after() {
    SystemConfigurationProperty property = dao.getByName(DUMMY_PROPERTY_NAME);
    if (property != null) {
      dao.delete(property);
    }
  }

  @Test
  public void testCRUD() {
    SystemConfigurationProperty property = new SystemConfigurationProperty(DUMMY_PROPERTY_NAME, "TEST-VALUE");
    dao.insert(property);

    property = dao.getByNameNotNull(DUMMY_PROPERTY_NAME);
    assertThat(property.getName()).isEqualTo(DUMMY_PROPERTY_NAME);
    assertThat(property.getValue()).isEqualTo("TEST-VALUE");

    property.setValue("UPDATED-VALUE");
    dao.update(property);

    property = dao.getByNameNotNull(DUMMY_PROPERTY_NAME);
    assertThat(property.getValue()).isEqualTo("UPDATED-VALUE");

    dao.delete(property);

    property = dao.getByName(DUMMY_PROPERTY_NAME);
    assertThat(property).isNull();
    assertThatThrownBy(() -> dao.getByNameNotNull(DUMMY_PROPERTY_NAME)).isInstanceOf(NotFoundException.class)
        .hasMessageContaining(DUMMY_PROPERTY_NAME);
  }

  @Test
  public void testUpdateNonExistent() {
    assertThatThrownBy(() -> dao.update(new SystemConfigurationProperty("FOO", "value"))).isInstanceOf(
        NotFoundException.class).hasMessage("A system configuration property 'FOO' does not exist.");
  }

  @Test
  public void getGetByNameNotNullThrowsExceptionIfNotFound() {
    assertThatThrownBy(() -> dao.getByNameNotNull("FOO")).isInstanceOf(NotFoundException.class)
        .hasMessage("A system configuration property 'FOO' does not exist.");
  }

  @Test
  public void testSet_NewProperty() {
    SystemConfigurationProperty property = new SystemConfigurationProperty(DUMMY_PROPERTY_NAME, "value");

    dao.set(property.getName(), property.getValue());

    SystemConfigurationProperty newProperty = dao.getByName(property.getName());
    assertThat(newProperty).isNotNull();
    assertThat(newProperty.getName()).isEqualTo(property.getName());
    assertThat(newProperty.getValue()).isEqualTo(property.getValue());
  }

  @Test
  public void testSet_NewProperty_NullValue() {
    SystemConfigurationProperty property = new SystemConfigurationProperty(DUMMY_PROPERTY_NAME, null);

    dao.set(property.getName(), property.getValue());

    assertThat(dao.getByName(property.getName())).isNull();
  }

  @Test
  public void testSet_ExistingProperty() {
    SystemConfigurationProperty property = new SystemConfigurationProperty(DUMMY_PROPERTY_NAME, "value");
    dao.insert(property);
    String newValue = property.getValue() + "2";

    dao.set(property.getName(), newValue);

    SystemConfigurationProperty updatedProperty = dao.getByName(property.getName());
    assertThat(updatedProperty).isNotNull();
    assertThat(updatedProperty.getName()).isEqualTo(property.getName());
    assertThat(updatedProperty.getValue()).isEqualTo(newValue);
  }

  @Test
  public void testSet_ExistingProperty_NullValue() {
    SystemConfigurationProperty property = new SystemConfigurationProperty(DUMMY_PROPERTY_NAME, "value");
    dao.insert(property);

    dao.set(property.getName(), null);

    assertThat(dao.getByName(property.getName())).isNull();
  }
}
