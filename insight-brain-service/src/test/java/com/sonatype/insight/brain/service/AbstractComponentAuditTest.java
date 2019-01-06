/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;

public abstract class AbstractComponentAuditTest
    extends AbstractComponentTest
    implements AuditTestSupport
{
  @Rule
  public LogOutput logOutput = new LogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }
}
