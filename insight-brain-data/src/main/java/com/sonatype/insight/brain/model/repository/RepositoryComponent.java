/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.model.HasStringId;

/**
 * Details about a component in a repository.
 * 
 * @since 1.17
 */
@Entity
@Table(name = "repository_component")
public class RepositoryComponent
    extends HasComponentId
    implements HasStringId
{
  @Id
  @Column(name = "repository_component_id")
  private String id;

  @Column(name = "repository_id")
  private String repositoryId;

  @Column(name = "pathname")
  private String pathname;

  @Column(name = "time")
  private Date time;

  @Column(name = "hash")
  private String hash;

  @Column(name = "match_state_id")
  private String matchStateId;

  @Column(name = "identification_source_id")
  private String identificationSourceId;

  @Column(name = "last_evaluation_time")
  private Date lastEvaluationTime;

  @Column(name = "quarantine_time")
  private Date quarantineTime;

  @Column(name = "unquarantine_time")
  private Date unquarantineTime;

  public RepositoryComponent() {
  }

  public RepositoryComponent(String repositoryId,
                             String pathname,
                             Date time,
                             String hash,
                             ComponentIdentifier componentIdentifier,
                             String matchStateId,
                             String identificationSourceId,
                             Date lastEvaluationTime)
  {
    this.repositoryId = repositoryId;
    this.pathname = pathname;
    this.time = time;
    this.hash = hash;
    setComponentIdentifier(componentIdentifier);
    this.matchStateId = matchStateId;
    this.identificationSourceId = identificationSourceId;
    this.lastEvaluationTime = lastEvaluationTime;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
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

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public Date getLastEvaluationTime() {
    return lastEvaluationTime;
  }

  public void setLastEvaluationTime(Date lastEvaluationTime) {
    this.lastEvaluationTime = lastEvaluationTime;
  }

  public Date getQuarantineTime() {
    return quarantineTime;
  }

  public void setQuarantineTime(Date quarantineTime) {
    this.quarantineTime = quarantineTime;
  }

  public Date getUnquarantineTime() {
    return unquarantineTime;
  }

  public void setUnquarantineTime(Date unquarantineTime) {
    this.unquarantineTime = unquarantineTime;
  }

  @Transient
  public boolean isQuarantined() {
    return quarantineTime != null && unquarantineTime == null;
  }

  @Override
  public String toString() {
    return "RepositoryComponent [id=" + id + ", repositoryId=" + repositoryId + ", time=" + time + ", hash=" + hash
        + ", matchStateId=" + matchStateId + ", identificationSourceId=" + identificationSourceId
        + ", lastEvaluationTime=" + lastEvaluationTime + ", quarantineTime="
        + quarantineTime + ", unquarantineTime=" + unquarantineTime + ", isQuarantined=" + isQuarantined() + "]";
  }

  public String getPathname() {
    return pathname;
  }

  public void setPathname(String pathname) {
    this.pathname = pathname;
  }
}
