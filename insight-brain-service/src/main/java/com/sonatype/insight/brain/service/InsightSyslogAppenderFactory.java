/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.dropwizard.logging.SyslogAppenderFactory;

@JsonDeserialize(as = InsightSyslogAppenderFactory.class)
public class InsightSyslogAppenderFactory
    extends SyslogAppenderFactory
{
  public static class Module
      extends SimpleModule
  {
    public Module() {
      setMixInAnnotation(SyslogAppenderFactory.class, InsightSyslogAppenderFactory.class);
    }
  }

  public InsightSyslogAppenderFactory() {
    // Due to SyslogAppenderFactory setting an initial log format
    // https://github.com/dropwizard/dropwizard/blob/v1.2.3/dropwizard-logging/src/main/java/io/dropwizard/logging/SyslogAppenderFactory.java#L128-L129
    // this is necessary to allow InsightConfigurationFactory to determine if the user did not set the log format and 
    // thus if it should set the desired default log format
    setLogFormat(null);
  }
}
