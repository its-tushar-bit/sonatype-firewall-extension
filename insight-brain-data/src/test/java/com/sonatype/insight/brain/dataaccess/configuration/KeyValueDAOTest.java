/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.configuration.KeyValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class KeyValueDAOTest
    extends AbstractDbDAOTest
{
  private KeyValueDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createKeyValueDAO();
  }

  @Test
  public void testCRUD() {
    KeyValue keyValue = new KeyValue();
    keyValue.setKey("test-key");
    keyValue.setValue("test-value");

    dao.insert(keyValue);

    assertThat(keyValue.getId()).isNotNull();

    KeyValue stored = dao.getById(keyValue.getId());

    assertThat(stored).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(keyValue);

    keyValue.setValue("updated-value");

    dao.update(keyValue);

    stored = dao.getById(keyValue.getId());

    assertThat(stored).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(keyValue);

    dao.delete(keyValue);

    assertThat(dao.getById(keyValue.getId())).isNull();
  }

  @Test
  public void testGetByKey() {
    KeyValue keyValue1 = tempEntity.newKeyValue("key1", "value1");
    tempEntity.newKeyValue("key2", "value2");

    KeyValue result = dao.getByKey("key1");

    assertThat(result).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(keyValue1);
  }

  @Test
  public void testGetByKey_NoResult() {
    tempEntity.newKeyValue("key1", "value1");

    KeyValue result = dao.getByKey("nonexistent");

    assertThat(result).isNull();
  }

  @Test
  public void testGetValue_NoKey() {
    assertThat(dao.getValue("doesNotExist")).isNull();
  }

  @Test
  public void testGetValue_ExistingKey() {
    dao.setValue("new-key", "new-value");

    assertThat(dao.getValue("new-key")).isEqualTo("new-value");
  }

  @Test
  public void testSetValue_NewKey() {
    dao.setValue("new-key", "new-value");

    KeyValue result = dao.getByKey("new-key");
    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo("new-key");
    assertThat(result.getValue()).isEqualTo("new-value");
  }

  @Test
  public void testSetValue_ExistingKey() {
    tempEntity.newKeyValue("existing-key", "original-value");

    dao.setValue("existing-key", "updated-value");

    KeyValue result = dao.getByKey("existing-key");
    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo("existing-key");
    assertThat(result.getValue()).isEqualTo("updated-value");
  }

  @Test
  public void testDelete_ByKey_DoesNotExist() {
    assertThatNoException().isThrownBy(() -> dao.deleteByKey("doesNotExist"));
  }

  @Test
  public void testDelete_ByKey_Exists() {
    tempEntity.newKeyValue("existing-key", "original-value");

    dao.deleteByKey("existing-key");

    assertThat(dao.getValue("existing-key")).isNull();
  }
}
