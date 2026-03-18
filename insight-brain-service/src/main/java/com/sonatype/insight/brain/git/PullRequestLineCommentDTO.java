/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import java.util.LinkedList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.nexus.scm.api.DiffPosition;

/**
 * Holds information about a PR line comment as it is collected/used during the PR commenting flow
 */
public class PullRequestLineCommentDTO
{
  private ComponentIdentifier componentIdentifier;

  private DiffPosition diffPosition;

  private String suggestedVersion;

  private List<PolicyViolation> policyViolations = new LinkedList<>();

  private String markup;

  private Long scmId;

  private Integer scmVersion;

  // Component hash;
  private String hash;

  public PullRequestLineCommentDTO(
      final ComponentIdentifier componentIdentifier,
      final DiffPosition diffPosition)
  {
    this.componentIdentifier = componentIdentifier;
    this.diffPosition = diffPosition;
  }

  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  public void setComponentIdentifier(final ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public DiffPosition getDiffPosition() {
    return diffPosition;
  }

  public void setDiffPosition(final DiffPosition diffPosition) {
    this.diffPosition = diffPosition;
  }

  public String getSuggestedVersion() {
    return suggestedVersion;
  }

  public void setSuggestedVersion(final String suggestedVersion) {
    this.suggestedVersion = suggestedVersion;
  }

  public List<PolicyViolation> getPolicyViolations() {
    return policyViolations;
  }

  public void addPolicyViolations(final PolicyViolation policyViolation) {
    policyViolations.add(policyViolation);
  }

  public String getMarkup() {
    return markup;
  }

  public void setMarkup(final String markup) {
    this.markup = markup;
  }

  public boolean hasMarkup() {
    return markup != null && !markup.isEmpty();
  }

  public Long getScmId() {
    return scmId;
  }

  public void setScmId(final Long scmId) {
    this.scmId = scmId;
  }

  public Integer getScmVersion() {
    return scmVersion;
  }

  public void setScmVersion(final Integer scmVersion) {
    this.scmVersion = scmVersion;
  }

  public void setHash(final String hash) {
    this.hash = hash;
  }

  public String getHash() {
    return hash;
  }
}
