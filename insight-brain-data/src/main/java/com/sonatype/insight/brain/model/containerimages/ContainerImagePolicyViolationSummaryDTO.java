/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.containerimages;

public class ContainerImagePolicyViolationSummaryDTO
{
  private long moderatePolicyViolationsCount;

  private long severePolicyViolationsCount;

  private long criticalPolicyViolationsCount;

  private long affectedContainerImagesCount;

  private long quarantinedContainerImagesCount;

  public ContainerImagePolicyViolationSummaryDTO(Object[] resultantObject) {
    criticalPolicyViolationsCount = ((Long) resultantObject[0]);
    severePolicyViolationsCount = ((Long) (resultantObject[1]));
    moderatePolicyViolationsCount = ((Long) (resultantObject[2]));
    affectedContainerImagesCount = ((Long) (resultantObject[3]));
    quarantinedContainerImagesCount = ((Long) (resultantObject[4]));
  }

  public long getQuarantinedContainerImagesCount() {
    return quarantinedContainerImagesCount;
  }

  public void setQuarantinedContainerImagesCount(int newQuarantineCount) {
    this.quarantinedContainerImagesCount = newQuarantineCount;
  }

  public long getAffectedContainerImagesCount() {
    return affectedContainerImagesCount;
  }

  public void setAffectedContainerImagesCount(int newAffectedCount) {
    this.affectedContainerImagesCount = newAffectedCount;
  }

  public long getModeratePolicyViolationsCount() {
    return moderatePolicyViolationsCount;
  }

  public void setModeratePolicyViolationsCount(int newModerateCount) {
    this.moderatePolicyViolationsCount = newModerateCount;
  }

  public long getSeverePolicyViolationsCount() {
    return severePolicyViolationsCount;
  }

  public void setSeverePolicyViolationsCount(int newSevereCount) {
    this.severePolicyViolationsCount = newSevereCount;
  }

  public long getCriticalPolicyViolationsCount() {
    return criticalPolicyViolationsCount;
  }

  public void setCriticalPolicyViolationsCount(int newCriticalCount) {
    this.criticalPolicyViolationsCount = newCriticalCount;
  }
}
