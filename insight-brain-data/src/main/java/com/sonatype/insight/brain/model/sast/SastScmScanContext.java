/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.model.sast;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "sast_scm_scan_context")
public class SastScmScanContext
    implements HasStringId
{
  @Id
  @Column(name = "sast_scm_scan_context_id")
  private String id;

  @Column(name = "branch_name")
  private String branchName;

  @Column(name = "commit_hash")
  private String commitHash;

  @Column(name = "created_at")
  private Date createdAt;

  public SastScmScanContext() {
  }

  public SastScmScanContext(final String branchName, final String commitHash) {
    this.branchName = branchName;
    this.commitHash = commitHash;
    this.createdAt = new Date();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public String getCommitHash() {
    return commitHash;
  }

  public void setCommitHash(String commitHash) {
    this.commitHash = commitHash;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "SastScmScanContext{" +
        "id='" + id + '\'' +
        ", branchName='" + branchName + '\'' +
        ", commitHash='" + commitHash + '\'' +
        ", createdAt=" + createdAt +
        '}';
  }
}
