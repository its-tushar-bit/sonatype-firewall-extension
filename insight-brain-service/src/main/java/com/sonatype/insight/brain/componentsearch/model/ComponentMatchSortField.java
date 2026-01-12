/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.componentsearch.model;

import java.util.Comparator;

import com.sonatype.insight.brain.componentsearch.dto.ApplicationComponentMatchDTO;

public enum ComponentMatchSortField
{
  APPLICATION_NAME("applicationName",
      Comparator.comparing(ApplicationComponentMatchDTO::getApplicationName, String.CASE_INSENSITIVE_ORDER)),

  COMPONENT_NAME("componentName",
      Comparator.comparing(ApplicationComponentMatchDTO::getComponentDisplayName, String.CASE_INSENSITIVE_ORDER)),

  EVALUATION_DATE("evaluationDate",
      Comparator.comparing(ApplicationComponentMatchDTO::getEvaluationDate)),

  STAGE("stage",
      Comparator.comparing(ApplicationComponentMatchDTO::getStage, String.CASE_INSENSITIVE_ORDER)),

  APPLICATION_ID("applicationId",
      Comparator.comparing(ApplicationComponentMatchDTO::getApplicationPublicId, String.CASE_INSENSITIVE_ORDER)),

  ACTIVE_WAIVER("activeWaiver",
      Comparator.comparing(ApplicationComponentMatchDTO::getActiveWaiver)),

  VIOLATING("violating",
      Comparator.comparing(ApplicationComponentMatchDTO::getViolating)),

  CVE_ID("cveId",
      Comparator.comparing(ApplicationComponentMatchDTO::getCveId, String.CASE_INSENSITIVE_ORDER));

  private final String fieldName;
  private final Comparator<ApplicationComponentMatchDTO> comparator;

  ComponentMatchSortField(final String fieldName, final Comparator<ApplicationComponentMatchDTO> comparator) {
    this.fieldName = fieldName;
    this.comparator = comparator;
  }

  public String getFieldName() {
    return fieldName;
  }

  public Comparator<ApplicationComponentMatchDTO> getComparator() {
    return comparator;
  }

  public static ComponentMatchSortField fromString(final String fieldName) {
    if (fieldName == null) {
      return null;
    }
    for (ComponentMatchSortField field : values()) {
      if (field.fieldName.equalsIgnoreCase(fieldName)) {
        return field;
      }
    }
    throw new IllegalArgumentException("Invalid sortBy field: '" + fieldName + "'. " +
        "Valid values: applicationName, applicationId, componentName, evaluationDate, " +
        "stage, activeWaiver, violating, cveId");
  }
}
