/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.List;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifierDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifiersDTO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ApiHashComponentIdentifierResourceTest
    extends AbstractResourceTest
{
  private HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Before
  public void setUp() {
    hashComponentIdentifierDAO = lookup(HashComponentIdentifierDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CLAIM_PATH_V2);
  }

  @Test
  public void testGet() throws Exception {
    HashComponentIdentifier hashComponentIdentifier =
        tempEntity.newClaimedComponent("hash", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));

    HttpResponse response = restRequest().path(hashComponentIdentifier.getHash()).get();

    assertResponseStatus(200, response);
    assertClaimedComponent(response.getBody(ApiHashComponentIdentifierDTO.class), hashComponentIdentifier);
  }

  @Test
  public void testGetAll() throws Exception {
    HashComponentIdentifier hashComponentIdentifier1 = tempEntity
        .newClaimedComponent("hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    HashComponentIdentifier hashComponentIdentifier2 = tempEntity
        .newClaimedComponent("hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"));
    HashComponentIdentifier hashComponentIdentifier3 =
        tempEntity.newClaimedComponent("hash3", ComponentIdentifier.createAnameCoordinates("n3", "q3", "v3"));

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);
    ApiHashComponentIdentifiersDTO apiHashComponentIdentifiersDTO =
        response.getBody(ApiHashComponentIdentifiersDTO.class);
    List<ApiHashComponentIdentifierDTO> apiHashComponentIdentifierDTOs = apiHashComponentIdentifiersDTO.componentClaims;
    assertThat(apiHashComponentIdentifierDTOs).hasSize(3);
    assertClaimedComponent(apiHashComponentIdentifierDTOs.get(0), hashComponentIdentifier1);
    assertClaimedComponent(apiHashComponentIdentifierDTOs.get(1), hashComponentIdentifier2);
    assertClaimedComponent(apiHashComponentIdentifierDTOs.get(2), hashComponentIdentifier3);
  }

  @Test
  public void testSet() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    ApiHashComponentIdentifierDTO givenDTO = new ApiHashComponentIdentifierDTO(
        new HashComponentIdentifier("hash", componentIdentifier));
    mockComponentSummary(componentIdentifier, ComponentSummary.create(false));

    HttpResponse response = restRequest().body(givenDTO).post();

    assertResponseStatus(200, response);
    HashComponentIdentifier storedHashComponentIdentifier =
        hashComponentIdentifierDAO.getByHash(givenDTO.hash);
    ApiHashComponentIdentifierDTO returnedDTO = response.getBody(ApiHashComponentIdentifierDTO.class);

    UserDAO userDAO = lookup(UserDAO.class);
    givenDTO.claimerId = User.ADMIN_USERNAME;
    givenDTO.claimerName = userDAO.getByUsername(User.ADMIN_USERNAME).calculateDisplayName();

    assertThat(givenDTO).usingRecursiveComparison().isEqualTo(returnedDTO);
    assertClaimedComponent(returnedDTO, storedHashComponentIdentifier);
  }

  @Test
  public void testDelete() throws Exception {
    HashComponentIdentifier hashComponentIdentifier =
        tempEntity.newClaimedComponent("hash", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));

    HttpResponse response = restRequest().path(hashComponentIdentifier.getHash()).delete();

    assertResponseStatus(204, response);
    assertThat(hashComponentIdentifierDAO.getByHash(hashComponentIdentifier.getHash())).isNull();
  }

  private void assertClaimedComponent(ApiHashComponentIdentifierDTO actual, HashComponentIdentifier expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.hash).isEqualTo(expected.getHash());
    assertThat(actual.comment).isEqualTo(expected.getComment());
    assertThat(actual.createTime).isEqualTo(expected.getCreateTime());
    assertThat(actual.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(expected.getComponentIdentifier()));
    assertThat(actual.packageUrl)
        .isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(expected.getComponentIdentifier()).getPackageUrl());
  }
}
