/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

/**
 * @since 1.21
 */
public abstract class Notification
{
  public static final String CONTINUOUS_MONITORING = "continuous-monitoring";

  private Set<String> stageIds = new TreeSet<>();

  protected Notification(String... stageIds) {
    Collections.addAll(this.stageIds, stageIds);
  }

  public boolean isApplicable(String stageId, boolean continuousMonitoring) {
    if (continuousMonitoring) {
      return stageIds.contains(CONTINUOUS_MONITORING);
    }
    return stageIds.contains(stageId);
  }

  public Set<String> getStageIds() {
    return stageIds;
  }

  public void setStageIds(Set<String> stageIds) {
    this.stageIds = stageIds != null ? stageIds : new TreeSet<>();
  }

  public ValidationResult validate() {
    ValidationResult validationResult = new ValidationResult();
    for (String stageId : stageIds) {
      if (!CONTINUOUS_MONITORING.equals(stageId) && StageTypes.getById(stageId) == null) {
        validationResult.addError("Invalid stage type id: '" + stageId + "'");
      }
    }
    validate(validationResult);
    return validationResult;
  }

  protected abstract void validate(ValidationResult validationResult);

  public abstract Action toAction();

  protected abstract void addToNotifications(Notifications notifications);

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);
}
