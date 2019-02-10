/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mail;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.micromailer.ClasspathResource;
import org.sonatype.micromailer.imp.AbstractMailType;

@Named(InsightMailType.ID)
@Singleton
public class InsightMailType
    extends AbstractMailType
{
  public static final String ID = "insight";

  public InsightMailType() {
    setTypeId(ID);
    setBodyIsHtml(true);
    getInlineResources().put("<plain-logo@sonatype.com>", // <img src="cid:plain-logo@sonatype.com" ...>
        new ClasspathResource("mail/sonatype.png", "sonatype.png", "image/png", getClass().getClassLoader()));
  }
}
