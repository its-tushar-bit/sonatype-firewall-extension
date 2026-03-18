/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

public record ApplicationRiskDTO(
    String organizationId,
    String organizationName,
    String applicationName,
    String publicId,
    String scanId,
    String stageTypeId,
    String applicationId,
    Long rank,
    int totalRiskPerStageUnique,
    int criticalPerStageUnique,
    int severePerStageUnique,
    int moderatePerStageUnique,
    int lowPerStageUnique,
    int totalRiskPerStage,
    int criticalPerStage,
    int severePerStage,
    int moderatePerStage,
    int lowPerStage)
{
}
