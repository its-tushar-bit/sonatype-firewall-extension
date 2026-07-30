/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.model.HasStringId;

import com.google.common.base.Joiner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;

/**
 * Association between applications and components.
 *
 * @since 1.11
 */
@Entity
@Table(name = "owner_component")
public class OwnerComponent
    extends HasComponentId
    implements HasStringId
{
  private static final char PATHNAMES_DELIMITER_CHAR = '\n';

  /** The pathnames delimiter character escaped for regular expressions. */
  private static final String PATHNAMES_DELIMITER_REGEX = "\\" + PATHNAMES_DELIMITER_CHAR;

  @Id
  @Column(name = "owner_component_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "stage_type_id")
  private String stageTypeId;

  @Column(name = "time")
  private Date time;

  @Column(name = "hash")
  private String hash;

  @Column(name = "match_state_id")
  private String matchStateId;

  @Column(name = "identification_source_id")
  private String identificationSourceId;

  @Column(name = "proprietary")
  private boolean proprietary;

  @Column(name = "pathnames")
  private String pathnamesString;

  public OwnerComponent() {
  }

  public OwnerComponent(
      String ownerId,
      String stageTypeId,
      Date time,
      String hash,
      ComponentIdentifier componentIdentifier,
      String matchStateId,
      String identificationSourceId,
      boolean proprietary,
      List<String> pathnames)
  {
    this.ownerId = ownerId;
    this.stageTypeId = stageTypeId;
    this.time = time;
    this.hash = hash;
    setComponentIdentifier(componentIdentifier);
    this.matchStateId = matchStateId;
    this.identificationSourceId = identificationSourceId;
    this.proprietary = proprietary;
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

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getStageTypeId() {
    return stageTypeId;
  }

  public void setStageTypeId(String stageTypeId) {
    this.stageTypeId = stageTypeId;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getMatchStateId() {
    return matchStateId;
  }

  public void setMatchStateId(String matchStateId) {
    this.matchStateId = matchStateId;
  }

  public String getIdentificationSourceId() {
    return identificationSourceId;
  }

  public void setIdentificationSourceId(String identificationSourceId) {
    this.identificationSourceId = identificationSourceId;
  }

  public boolean isProprietary() {
    return proprietary;
  }

  public void setProprietary(boolean proprietary) {
    this.proprietary = proprietary;
  }

  public String getPathnamesString() {
    return pathnamesString;
  }

  @SuppressWarnings("unused")
  /** For JPA's use only */
  private void setPathnamesString(String pathnamesString) {
    this.pathnamesString = pathnamesString;
  }

  private void setPathnames(List<String> pathnames) {
    pathnamesString = null;
    if (pathnames != null && !pathnames.isEmpty()) {
      Joiner joiner = Joiner.on(PATHNAMES_DELIMITER_CHAR);
      pathnamesString = joiner.join(pathnames);
    }
  }

  public List<String> getPathnames() {
    if (StringUtils.isBlank(pathnamesString)) {
      return Collections.emptyList();
    }

    return Arrays.asList(pathnamesString.split(PATHNAMES_DELIMITER_REGEX));
  }

  @Override
  public String toString() {
    return "OwnerComponent [ownerId=" + ownerId + ", stageTypeId=" + stageTypeId + ", hash=" + hash
        + ", componentIdentifier=" + getComponentIdentifier() + ", matchStateId=" + matchStateId + "]";
  }

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }
}
