/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;

public class PolicyViolationDiff<T extends PolicyViolationComparable>
{
  private final List<T> appeared = new ArrayList<>();

  private final Map<T, T> same = new LinkedHashMap<>();

  private final List<T> cleared = new ArrayList<>();

  public List<T> getAppeared() {
    return appeared;
  }

  public void addAppeared(T policyViolation) {
    appeared.add(policyViolation);
  }

  public void addAppeared(Collection<? extends T> policyViolations) {
    appeared.addAll(policyViolations);
  }

  public List<T> getCleared() {
    return cleared;
  }

  public void addCleared(T policyViolation) {
    cleared.add(policyViolation);
  }

  public void addCleared(Collection<? extends T> policyViolations) {
    cleared.addAll(policyViolations);
  }

  public Map<T, T> getSame() {
    return same;
  }

  public void addSame(T newPolicyViolation, T oldPolicyViolation) {
    same.put(oldPolicyViolation, newPolicyViolation);
  }

  public boolean hasAppeared() {
    return !appeared.isEmpty();
  }

  public boolean hasCleared() {
    return !cleared.isEmpty();
  }
}
