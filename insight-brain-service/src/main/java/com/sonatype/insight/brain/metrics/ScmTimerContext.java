/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics;

import io.micrometer.core.instrument.Timer;

public record ScmTimerContext(Timer.Sample sample, ScmCommentOperation operation, String provider)
{
}
