/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.SourceControlProvider;

public class PullRequestPolicyEvaluationsDTO
{
  private String applicationId;

  private String featureBranchName;

  private PolicyEvaluation targetPolicyEvaluation;

  private PolicyEvaluation featureBranchPolicyEvaluation;

  private GitRepositoryInfo gitRepositoryInfo;

  private String pullRequestHeadCommit;

  private int pullRequestNumber;

  public String getApplicationId() {
    return applicationId;
  }

  public PullRequestPolicyEvaluationsDTO setApplicationId(String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  public PolicyEvaluation getTargetPolicyEvaluation() {
    return targetPolicyEvaluation;
  }

  public PullRequestPolicyEvaluationsDTO setTargetPolicyEvaluation(
      PolicyEvaluation targetPolicyEvaluation)
  {
    this.targetPolicyEvaluation = targetPolicyEvaluation;
    return this;
  }

  public String getTargetPolicyEvaluationId() {
    return null != targetPolicyEvaluation ? targetPolicyEvaluation.getId() : null;
  }

  public String getFeatureBranchName() {
    return featureBranchName;
  }

  public PullRequestPolicyEvaluationsDTO setFeatureBranchName(String featureBranchName) {
    this.featureBranchName = featureBranchName;
    return this;
  }

  public PolicyEvaluation getFeatureBranchPolicyEvaluation() {
    return featureBranchPolicyEvaluation;
  }

  public PullRequestPolicyEvaluationsDTO setFeatureBranchPolicyEvaluation(
      PolicyEvaluation featureBranchPolicyEvaluation)
  {
    this.featureBranchPolicyEvaluation = featureBranchPolicyEvaluation;
    return this;
  }

  public String getFeatureBranchPolicyEvaluationId() {
    return null != featureBranchPolicyEvaluation ? featureBranchPolicyEvaluation.getId() : null;
  }

  public GitRepositoryInfo getGitRepositoryInfo() {
    return gitRepositoryInfo;
  }

  public PullRequestPolicyEvaluationsDTO setGitRepositoryInfo(GitRepositoryInfo gitRepositoryInfo) {
    this.gitRepositoryInfo = gitRepositoryInfo;
    return this;
  }

  public SourceControlProvider getSourceControlProvider() {
    return null != gitRepositoryInfo ? gitRepositoryInfo.getProvider() : null;
  }

  public String getPullRequestHeadCommit() {
    return pullRequestHeadCommit;
  }

  public PullRequestPolicyEvaluationsDTO setPullRequestHeadCommit(String headCommit) {
    this.pullRequestHeadCommit = headCommit;
    return this;
  }

  public int getPullRequestNumber() {
    return pullRequestNumber;
  }

  public PullRequestPolicyEvaluationsDTO setPullRequestNumber(int pullRequestNumber) {
    this.pullRequestNumber = pullRequestNumber;
    return this;
  }
}
