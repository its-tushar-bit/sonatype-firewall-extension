/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.sonatype.insight.model.HasStringId;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.104
 */
@Entity
@Table(name = "aggregate_file")
public class AggregateFile
    implements HasStringId
{
  private static final char PATHNAMES_DELIMITER_CHAR = '\n';

  /**
   * The pathnames delimiter character escaped for regular expressions.
   */
  private static final String PATHNAMES_DELIMITER_REGEX = "\\" + PATHNAMES_DELIMITER_CHAR;

  @Id
  @Column(name = "aggregate_file_id")
  private String id;

  @Column(name = "owner_component_id")
  private String ownerComponentId;

  @Column(name = "hash")
  private String hash;

  @Column(name = "pathnames")
  private String pathnamesString;

  public AggregateFile() {
  }

  public AggregateFile(String ownerComponentId, String hash, Set<String> pathnames) {
    this.ownerComponentId = ownerComponentId;
    this.hash = hash;
    setPathnames(pathnames);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOwnerComponentId() {
    return ownerComponentId;
  }

  public void setOwnerComponentId(String ownerComponentId) {
    this.ownerComponentId = ownerComponentId;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getPathnamesString() {
    return pathnamesString;
  }

  @SuppressWarnings("unused")
  /* For JPA's use only */
  private void setPathnamesString(String pathnamesString) {
    this.pathnamesString = pathnamesString;
  }

  private void setPathnames(Set<String> pathnames) {
    pathnamesString = null;
    if (pathnames != null && !pathnames.isEmpty()) {
      pathnamesString = Joiner.on(PATHNAMES_DELIMITER_CHAR).join(pathnames);
    }
  }

  public Set<String> getPathnames() {
    if (StringUtils.isBlank(pathnamesString)) {
      return Collections.emptySet();
    }
    return Sets.newLinkedHashSet(Arrays.asList(pathnamesString.split(PATHNAMES_DELIMITER_REGEX)));
  }
}
