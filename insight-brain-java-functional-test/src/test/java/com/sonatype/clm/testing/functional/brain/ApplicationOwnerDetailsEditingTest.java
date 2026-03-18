/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.insight.brain.model.Application;

import org.junit.Before;

public class ApplicationOwnerDetailsEditingTest
    extends AbstractOwnerDetailsEditingTest
{
  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  @Before
  public void init() {
    // note the ȧ being used to force a character to be encoded
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION);
    super.init(application);
  }
}
