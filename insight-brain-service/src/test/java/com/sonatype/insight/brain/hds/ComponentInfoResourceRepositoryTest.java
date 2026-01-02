/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ComponentInfoResourceRepositoryTest
    extends AbstractComponentInfoResourceTest
{
  private Repository repository;

  @Before
  public void setUp() {
    repository = tempEntity.newRepository();
    tempEntity.newRepositoryComponent(repository.getId());
  }

  @Override
  protected String getResourcePath() {
    return ComponentInfoResource.RESOURCE_PATH;
  }

  @Override
  protected Owner getOwner() {
    return repository;
  }

  @Override
  protected String getOwnerId() {
    return repository.getId();
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    testGetComponentDetails_ReadPermission();
  }

  @Test
  public void testGetComponentDetailsList() throws Exception {
    testGetComponentDetailsList_ReadPermission();
  }

  @Override
  protected void assertRemediation(ApiComponentRemediationValueDTO remediationValue) {
    assertThat(remediationValue).isNotNull();
  }
}
