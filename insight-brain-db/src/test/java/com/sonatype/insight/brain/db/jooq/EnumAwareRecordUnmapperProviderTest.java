/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.jooq;

import java.util.Locale;

import jakarta.persistence.Column;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.SQLDataType.VARCHAR;

public class EnumAwareRecordUnmapperProviderTest
{
  enum SimpleEnum
  {
    ACTIVE,
    INACTIVE
  }

  /**
   * Enum that overrides toString() to return a different value than name(),
   * which is the scenario that exposes jOOQ's asymmetric enum handling.
   */
  enum OverriddenToStringEnum
  {
    BITBUCKET,
    GITHUB;

    @Override
    public String toString() {
      return name().toLowerCase(Locale.ENGLISH);
    }
  }

  static class EntityWithSimpleEnum
  {
    String name;

    SimpleEnum status;
  }

  static class EntityWithOverriddenToStringEnum
  {
    String name;

    OverriddenToStringEnum provider;
  }

  static class EntityWithNoEnums
  {
    String name;

    String value;
  }

  static class EntityWithNullEnum
  {
    String name;

    SimpleEnum status;
  }

  static class EntityWithColumnAnnotation
  {
    String name;

    @Column(name = "status_code")
    SimpleEnum status;
  }

  static class ParentEntity
  {
    SimpleEnum parentStatus;
  }

  static class ChildEntity
      extends ParentEntity
  {
    String name;

    OverriddenToStringEnum provider;
  }

  static class EntityWithMultipleEnums
  {
    SimpleEnum status;

    OverriddenToStringEnum provider;
  }

  private DefaultConfiguration createConfiguration() {
    DefaultConfiguration config = new DefaultConfiguration();
    config.set(SQLDialect.H2);
    config.set(new Settings().withMapJPAAnnotations(true));
    config.set(new EnumAwareRecordUnmapperProvider(config));
    return config;
  }

  @Test
  public void testProvide_simpleEnumUsesName() throws Exception {
    DefaultConfiguration config = createConfiguration();

    EntityWithSimpleEnum entity = new EntityWithSimpleEnum();
    entity.name = "test";
    entity.status = SimpleEnum.ACTIVE;

    Record record = DSL.using(config)
        .newRecord(
            field("name", VARCHAR),
            field("status", VARCHAR));
    record.from(entity);

    assertThat(record.get(field("status", VARCHAR))).isEqualTo("ACTIVE");
  }

  @Test
  public void testProvide_overriddenToStringEnumUsesNameNotToString() throws Exception {
    DefaultConfiguration config = createConfiguration();

    EntityWithOverriddenToStringEnum entity = new EntityWithOverriddenToStringEnum();
    entity.name = "test";
    entity.provider = OverriddenToStringEnum.BITBUCKET;

    Record record = DSL.using(config)
        .newRecord(
            field("name", VARCHAR),
            field("provider", VARCHAR));
    record.from(entity);

    assertThat(record.get(field("provider", VARCHAR)))
        .as("Should use name() 'BITBUCKET', not toString() 'bitbucket'")
        .isEqualTo("BITBUCKET");
  }

  @Test
  public void testProvide_entityWithNoEnumsDelegatesDirectly() throws Exception {
    DefaultConfiguration config = createConfiguration();

    EntityWithNoEnums entity = new EntityWithNoEnums();
    entity.name = "test";
    entity.value = "hello";

    Record record = DSL.using(config)
        .newRecord(
            field("name", VARCHAR),
            field("value", VARCHAR));
    record.from(entity);

    assertThat(record.get(field("name", VARCHAR))).isEqualTo("test");
    assertThat(record.get(field("value", VARCHAR))).isEqualTo("hello");
  }

  @Test
  public void testProvide_nullEnumFieldMapsToNull() throws Exception {
    DefaultConfiguration config = createConfiguration();

    EntityWithNullEnum entity = new EntityWithNullEnum();
    entity.name = "test";
    entity.status = null;

    Record record = DSL.using(config)
        .newRecord(
            field("name", VARCHAR),
            field("status", VARCHAR));
    record.from(entity);

    assertThat(record.get(field("status", VARCHAR))).isNull();
  }

  @Test
  public void testProvide_columnAnnotationResolvesCorrectField() throws Exception {
    DefaultConfiguration config = createConfiguration();

    EntityWithColumnAnnotation entity = new EntityWithColumnAnnotation();
    entity.name = "test";
    entity.status = SimpleEnum.INACTIVE;

    Record record = DSL.using(config)
        .newRecord(
            field("name", VARCHAR),
            field("status_code", VARCHAR));
    record.from(entity);

    assertThat(record.get(field("status_code", VARCHAR))).isEqualTo("INACTIVE");
  }

  @Test
  public void testProvide_inheritedEnumFieldsAreDiscovered() throws Exception {
    DefaultConfiguration config = createConfiguration();

    ChildEntity entity = new ChildEntity();
    entity.name = "test";
    entity.provider = OverriddenToStringEnum.GITHUB;
    entity.parentStatus = SimpleEnum.ACTIVE;

    Record record = DSL.using(config)
        .newRecord(
            field("name", VARCHAR),
            field("provider", VARCHAR),
            field("parentStatus", VARCHAR));
    record.from(entity);

    assertThat(record.get(field("provider", VARCHAR)))
        .as("Child enum field should use name()")
        .isEqualTo("GITHUB");
    assertThat(record.get(field("parentStatus", VARCHAR)))
        .as("Inherited enum field should use name()")
        .isEqualTo("ACTIVE");
  }

  @Test
  public void testProvide_multipleEnumFieldsAllCorrected() throws Exception {
    DefaultConfiguration config = createConfiguration();

    EntityWithMultipleEnums entity = new EntityWithMultipleEnums();
    entity.status = SimpleEnum.INACTIVE;
    entity.provider = OverriddenToStringEnum.BITBUCKET;

    Record record = DSL.using(config)
        .newRecord(
            field("status", VARCHAR),
            field("provider", VARCHAR));
    record.from(entity);

    assertThat(record.get(field("status", VARCHAR))).isEqualTo("INACTIVE");
    assertThat(record.get(field("provider", VARCHAR)))
        .as("Should use name() 'BITBUCKET', not toString() 'bitbucket'")
        .isEqualTo("BITBUCKET");
  }
}
