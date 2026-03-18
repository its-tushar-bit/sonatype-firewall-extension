/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

public record ApplicationComponentRisk(
    String hash,
    String filename,
    String componentIdFormat,
    String componentIdCoordinatesJson,
    int affectedApplications,
    int score,
    int scoreCritical,
    int scoreSevere,
    int scoreModerate,
    int scoreLow)
{
}
