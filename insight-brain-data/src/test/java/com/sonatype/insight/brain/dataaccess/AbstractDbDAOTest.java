/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractDbDAOTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  protected Application application;

  protected String applicationId;

  protected Organization organization;

  public static final String[] INVALID_ALPHANUMERIC = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };

  public static final String[] INVALID_SPACING_NAMES = {
      " leading space", "trailing space ", "double  space",
      "  starts with double space", "ends with double space  "
  };

  @Before
  public void setup() {
    organization = tempEntity.newOrganization("AbstractDbDAOTest");
    application = tempEntity
        .newApplication("AbstractDbDAOTest-AppName", "AbstractDbDAOTest_AppPublicId", organization.getId());
    applicationId = application.getId();
  }
}
