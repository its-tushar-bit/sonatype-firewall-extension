/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HRC-scoped sibling of {@link ComponentInfoResourceRepositoryTest}. Exercises the polymorphic
 * {@code {ownerType}/{ownerId}} paths on {@link ComponentInfoResource} keyed on
 * {@code hosted_repository_component}.
 */
public class ComponentInfoResourceHostedRepositoryComponentTest
    extends AbstractComponentInfoResourceTest
{
  private HostedRepositoryComponent hrc;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    Repository repository = tempEntity.newRepository();
    hrc = tempEntity.newHostedRepositoryComponent(repository);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Override
  protected String getResourcePath() {
    return ComponentInfoResource.RESOURCE_PATH;
  }

  @Override
  protected Owner getOwner() {
    return hrc;
  }

  @Override
  protected String getOwnerId() {
    return hrc.getId();
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
