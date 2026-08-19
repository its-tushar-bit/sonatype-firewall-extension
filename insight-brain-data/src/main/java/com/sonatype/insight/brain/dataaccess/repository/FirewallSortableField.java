/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

public enum FirewallSortableField
{
  EVALUATION_TIME("evaluationTime", "lastEvaluationTime"),
  QUARANTINE_TIME("quarantineTime", "quarantineTime"),
  RELEASE_QUARANTINE_TIME("releaseQuarantineTime", "unquarantineTime"),
  REPOSITORY_PUBLIC_ID("repositoryPublicId", "repositoryPublicId"),
  POLICY_NAME("policyName", "policyName"),
  COMPONENT_DISPLAY_NAME("componentDisplayName", "componentDisplayName");

  private final String label;

  private final String column;

  FirewallSortableField(final String label, final String column) {
    this.label = label;
    this.column = column;
  }

  public String getColumn() {
    return column;
  }

  public String getLabel() {
    return label;
  }

  public static FirewallSortableField getByLabel(String label) {
    if (label == null) {
      return null;
    }

    for (FirewallSortableField sortableField : values()) {
      if (label.equals(sortableField.getLabel())) {
        return sortableField;
      }
    }

    throw new IllegalArgumentException("Unknown sortable field with label: " + label);
  }
}
