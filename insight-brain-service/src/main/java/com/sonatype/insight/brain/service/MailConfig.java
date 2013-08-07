/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

/**
 * Custom {@link com.sonatype.insight.portal.mail.MailConfig} with updated defaults. We used to set them externally in
 * InsightConfig, but if someone chose to customize one of the properties then the newly deserialized class would not
 * include our changes. Setting them in the constructor means they always get applied first.
 */
public class MailConfig
    extends com.sonatype.insight.portal.mail.MailConfig
{
  public MailConfig() {
    setHostname("127.0.0.1");
    setPort(587);
    setSystemEmail("SonatypeCLM@localhost");
    setSystemPersonal("Sonatype CLM");
  }
}
