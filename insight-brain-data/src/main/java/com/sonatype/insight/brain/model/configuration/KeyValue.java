/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * This class represents a key-value pair to be stored in the database.
 * <p>
 * It IS intended to provide a cluster-friendly way to share simple pieces of information and help avoid table bloat.
 * <p>
 * It IS NOT intended to:
 * <ul>
 *   <li>Store an excessive number of rows - instead prefer a dedicated database table.</li>
 *   <li>Store configuration - instead prefer {@link SystemConfigurationProperty} which backs configuration and feature flags.</li>
 *   <li>Be searchable - rows are only intended to be managed by their key/id one at a time.</li>
 * </ul>
 * The key/id must be unique and non-null, the value must be non-null.
 * <p>
 * Both key/id and value are stored as Strings, but otherwise have no restrictions other than length.
 */
@Entity
@Table(name = "key_value")
public class KeyValue
    implements HasStringId
{
  @Id
  @Column(name = "key")
  private String id;

  @Column(name = "value")
  private String value;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getKey() {
    return getId();
  }

  public void setKey(final String key) {
    setId(key);
  }

  public String getValue() {
    return value;
  }

  public void setValue(final String value) {
    this.value = value;
  }
}
