/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
  public void testGetComponentDetails_UnknownComponent() throws Exception {
    String hash = "testHash";

    // Component is unknown and it is not in the repository
    HttpRequest request =
        detailsRequest(getOwnerId(), null /* componentIdentifier */, hash, MatchState.UNKNOWN, false /* proprietary */);
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    NamedComponentDetails componentDetails = response.getBody(NamedComponentDetails.class);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isNull();
    assertThat(componentDetails.getDisplayName()).isNull();
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.UNKNOWN.getId());
    assertThat(componentDetails.getIdentificationSource()).isNull();

    // Add the component to the repository and test again - it should have a display name
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN,
        "testPathname", hash, null /* identifier */, false /* quarantined */);
    request =
        detailsRequest(getOwnerId(), null /* componentIdentifier */, hash, MatchState.UNKNOWN, false /* proprietary */);
    response = request.get();
    assertResponseStatus(200, response);

    componentDetails = response.getBody(NamedComponentDetails.class);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isNull();
    assertThat(componentDetails.getDisplayName().toString()).isEqualTo(repositoryComponent.getDisplayName());
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.UNKNOWN.getId());
    assertThat(componentDetails.getIdentificationSource()).isNull();
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
