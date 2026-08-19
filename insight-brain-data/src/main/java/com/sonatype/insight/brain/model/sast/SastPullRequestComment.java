/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sast;

import java.util.Date;
import java.util.StringJoiner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "sast_pull_request_comment")
public class SastPullRequestComment
    implements HasStringId
{
  @Id
  @Column(name = "sast_pull_request_comment_id")
  private String id;

  @Column(name = "sast_scan_id")
  private String sastScanId;

  @Column(name = "pull_request_url")
  private String pullRequestUrl;

  @Column(name = "created_at")
  private Date createdAt;

  @Column(name = "last_updated_at")
  private Date lastUpdatedAt;

  @Column(name = "commit_hash")
  private String commitHash;

  @Column(name = "content_hash")
  private String contentHash;

  @Column(name = "pull_request_comment_id")
  private String pullRequestCommentId;

  @Column(name = "pull_request_comment_version")
  private Integer pullRequestCommentVersion;

  public SastPullRequestComment() {
  }

  public SastPullRequestComment(
      final String sastScanId,
      final String pullRequestUrl,
      final Date createdAt,
      final Date lastUpdatedAt,
      final String commitHash,
      final String contentHash,
      final String pullRequestCommentId,
      final Integer pullRequestCommentVersion)
  {
    this.sastScanId = sastScanId;
    this.pullRequestUrl = pullRequestUrl;
    this.createdAt = createdAt;
    this.lastUpdatedAt = lastUpdatedAt;
    this.commitHash = commitHash;
    this.contentHash = contentHash;
    this.pullRequestCommentId = pullRequestCommentId;
    this.pullRequestCommentVersion = pullRequestCommentVersion;
  }

  // Prefer this constructor which takes care of the initial createdAt and lastUpdatedAt timestamps.
  public SastPullRequestComment(
      final String sastScanId,
      final String pullRequestUrl,
      final String commitHash,
      final String contentHash,
      final String pullRequestCommentId,
      final Integer pullRequestCommentVersion)
  {
    this(sastScanId, pullRequestUrl, new Date(), new Date(), commitHash, contentHash, pullRequestCommentId,
        pullRequestCommentVersion);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getSastScanId() {
    return sastScanId;
  }

  public void setSastScanId(final String sastScanId) {
    this.sastScanId = sastScanId;
  }

  public String getPullRequestUrl() {
    return pullRequestUrl;
  }

  public void setPullRequestUrl(final String pullRequestUrl) {
    this.pullRequestUrl = pullRequestUrl;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(final Date lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public String getCommitHash() {
    return commitHash;
  }

  public void setCommitHash(final String commitHash) {
    this.commitHash = commitHash;
  }

  public String getContentHash() {
    return contentHash;
  }

  public void setContentHash(final String contentHash) {
    this.contentHash = contentHash;
  }

  public String getPullRequestCommentId() {
    return pullRequestCommentId;
  }

  public void setPullRequestCommentId(final String pullRequestCommentId) {
    this.pullRequestCommentId = pullRequestCommentId;
  }

  public Integer getPullRequestCommentVersion() {
    return pullRequestCommentVersion;
  }

  public void setPullRequestCommentVersion(final Integer pullRequestCommentVersion) {
    this.pullRequestCommentVersion = pullRequestCommentVersion;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SastPullRequestComment.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .add("sastScanId='" + sastScanId + "'")
        .add("pullRequestUrl='" + pullRequestUrl + "'")
        .add("createdAt=" + createdAt)
        .add("lastUpdatedAt=" + lastUpdatedAt)
        .add("commitHash='" + commitHash + "'")
        .add("contentHash='" + contentHash + "'")
        .add("pullRequestCommentId='" + pullRequestCommentId + "'")
        .add("pullRequestCommentVersion='" + pullRequestCommentVersion + "'")
        .toString();
  }
}
