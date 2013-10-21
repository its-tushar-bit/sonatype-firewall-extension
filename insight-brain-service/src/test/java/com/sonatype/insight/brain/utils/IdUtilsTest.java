/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.List;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class IdUtilsTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testGetInternalOwnerIds_LegacyApp() {
    Application app = tempEntity.newApplication(null);
    List<String> ids = IdUtils.getInternalOwnerIds(IdUtils.TYPE_APPLICATION, app.getPublicId());
    assertThat(ids, contains(app.getId()));
  }

  @Test
  public void testGetInternalOwnerIds_NormalApp() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    List<String> ids = IdUtils.getInternalOwnerIds(IdUtils.TYPE_APPLICATION, app.getPublicId());
    assertThat(ids, contains(app.getId(), app.getOrganizationId()));
  }

  @Test
  public void testGetInternalOwnerIds_Org() {
    Organization org = tempEntity.newOrganization();
    List<String> ids = IdUtils.getInternalOwnerIds(IdUtils.TYPE_ORGANIZATION, org.getId());
    assertThat(ids, contains(org.getId()));
  }
}
