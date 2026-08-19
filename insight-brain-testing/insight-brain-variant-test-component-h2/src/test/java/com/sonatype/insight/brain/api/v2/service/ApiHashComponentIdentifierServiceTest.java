/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifierDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifiersDTO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ApiHashComponentIdentifierServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApiHashComponentIdentifierService apiHashComponentIdentifierService;

  @Inject
  private HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Mock
  private HdsClient mockHdsClient;

  @Test
  public void testGet_ShortHash() {
    HashComponentIdentifier hashComponentIdentifier =
        tempEntity.newClaimedComponent("h", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));

    assertClaimedComponent(apiHashComponentIdentifierService.get(hashComponentIdentifier.getHash()),
        hashComponentIdentifier);
  }

  @Test
  public void testGet_LongHash() {
    HashComponentIdentifier hashComponentIdentifier = tempEntity
        .newClaimedComponent("0000000000000000000000000000000000000040",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));

    assertClaimedComponent(apiHashComponentIdentifierService.get("0000000000000000000000000000000000000040"),
        hashComponentIdentifier);
  }

  @Test
  public void testGet_NotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiHashComponentIdentifierService.get("doesNotExist"))
        .withMessageContaining("Cannot find component claim for hash doesNotExist");
  }

  @Test
  public void testGetAll() {
    HashComponentIdentifier hashComponentIdentifier1 = tempEntity
        .newClaimedComponent("hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    HashComponentIdentifier hashComponentIdentifier2 = tempEntity
        .newClaimedComponent("hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"));
    HashComponentIdentifier hashComponentIdentifier3 = tempEntity
        .newClaimedComponent("hash3", ComponentIdentifier.createAnameCoordinates("n3", "q3", "v3"));

    ApiHashComponentIdentifiersDTO apiHashComponentIdentifiersDTO = apiHashComponentIdentifierService.getAll();
    List<ApiHashComponentIdentifierDTO> apiHashComponentIdentifierDTOs =
        apiHashComponentIdentifiersDTO.componentClaims;
    assertThat(apiHashComponentIdentifierDTOs).hasSize(3);
    assertClaimedComponent(apiHashComponentIdentifierDTOs.get(0), hashComponentIdentifier1);
    assertClaimedComponent(apiHashComponentIdentifierDTOs.get(1), hashComponentIdentifier2);
    assertClaimedComponent(apiHashComponentIdentifierDTOs.get(2), hashComponentIdentifier3);
  }

  @Test
  public void testGetAll_Empty() {
    assertThat(apiHashComponentIdentifierService.getAll().componentClaims).isEmpty();
  }

  @Test
  public void testSet_Insert_ComponentIdentifier() {
    ApiHashComponentIdentifierDTO givenDTO = newApiHashComponentIdentifierDTO("hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null);
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));

    ApiHashComponentIdentifierDTO returnedDTO = apiHashComponentIdentifierService.set(givenDTO);

    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(givenDTO.hash);
    assertClaimedComponent(returnedDTO, hashComponentIdentifier);
  }

  @Test
  public void testSet_Update_ComponentIdentifier() {
    tempEntity.newClaimedComponent("hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));
    ApiHashComponentIdentifierDTO givenDTO = newApiHashComponentIdentifierDTO("hash1",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"), null);

    ApiHashComponentIdentifierDTO returnedDTO = apiHashComponentIdentifierService.set(givenDTO);

    assertClaimedComponent(returnedDTO, hashComponentIdentifierDAO.getByHash(givenDTO.hash));
  }

  @Test
  public void testSet_Insert_PackageUrl() {
    ApiHashComponentIdentifierDTO givenDTO =
        newApiHashComponentIdentifierDTO("hash", null, "pkg:maven/g/a@v?classifier=c&type=e");
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));

    ApiHashComponentIdentifierDTO returnedDTO = apiHashComponentIdentifierService.set(givenDTO);

    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(givenDTO.hash);
    assertClaimedComponent(returnedDTO, hashComponentIdentifier);
  }

  @Test
  public void testSet_Update_PackageUrl() {
    tempEntity.newClaimedComponent("hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));
    ApiHashComponentIdentifierDTO givenDTO =
        newApiHashComponentIdentifierDTO("hash1", null, "pkg:maven/g2/a2@v2?classifier=c2&type=e2");

    ApiHashComponentIdentifierDTO returnedDTO = apiHashComponentIdentifierService.set(givenDTO);

    assertClaimedComponent(returnedDTO, hashComponentIdentifierDAO.getByHash(givenDTO.hash));
  }

  @Test
  public void testSet_Insert_LongHash() {
    ApiHashComponentIdentifierDTO givenDTO =
        newApiHashComponentIdentifierDTO("0000000000000000000000000000000000000040",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null);
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));

    ApiHashComponentIdentifierDTO returnedDTO = apiHashComponentIdentifierService.set(givenDTO);

    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(givenDTO.hash);
    assertThat(hashComponentIdentifier.getHash()).isEqualTo(HashHelper.truncateHash(givenDTO.hash));
    assertClaimedComponent(returnedDTO, hashComponentIdentifier);
  }

  @Test
  public void testSet_Update_LongHash() {
    tempEntity.newClaimedComponent("0000000000000000000000000000000000000040",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));
    ApiHashComponentIdentifierDTO givenDTO =
        newApiHashComponentIdentifierDTO("0000000000000000000000000000000000000040",
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"), null);

    ApiHashComponentIdentifierDTO returnedDTO = apiHashComponentIdentifierService.set(givenDTO);

    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(givenDTO.hash);
    assertThat(hashComponentIdentifier.getHash()).isEqualTo(HashHelper.truncateHash(givenDTO.hash));
    assertClaimedComponent(returnedDTO, hashComponentIdentifier);
  }

  @Test
  public void testSet_ComponentIdentifierWithoutOptionalCoordinates() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", null, "e");
    ApiHashComponentIdentifierDTO givenDTO = newApiHashComponentIdentifierDTO("hash", componentIdentifier, null);
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));

    ApiHashComponentIdentifierDTO returnedDTO = apiHashComponentIdentifierService.set(givenDTO);

    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(givenDTO.hash);
    componentIdentifier.ensureComplete();
    assertThat(hashComponentIdentifier.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertClaimedComponent(returnedDTO, hashComponentIdentifier);
  }

  @Test
  public void testSet_PackageUrlWithoutOptionalCoordinates() {
    ApiHashComponentIdentifierDTO givenDTO = newApiHashComponentIdentifierDTO("hash", null, "pkg:maven/g/a@v?type=e");
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));

    ApiHashComponentIdentifierDTO returnedDTO = apiHashComponentIdentifierService.set(givenDTO);

    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(givenDTO.hash);
    assertThat(hashComponentIdentifier.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e"));
    assertClaimedComponent(returnedDTO, hashComponentIdentifier);
  }

  @Test
  public void testSet_NoDTO() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiHashComponentIdentifierService.set(null))
        .withMessageContaining("A component hash and identifier/package url are required.");
  }

  @Test
  public void testSet_NoHash() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiHashComponentIdentifierService.set(new ApiHashComponentIdentifierDTO()))
        .withMessageContaining("A component hash and identifier/package url are required.");
  }

  @Test
  public void testSet_NoComponentIdentifierOrPackageUrl() {
    ApiHashComponentIdentifierDTO apiHashComponentIdentifierDTO = new ApiHashComponentIdentifierDTO();
    apiHashComponentIdentifierDTO.hash = "hash";
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiHashComponentIdentifierService.set(apiHashComponentIdentifierDTO))
        .withMessageContaining("A component hash and identifier/package url are required.");
  }

  @Test
  public void testSet_UnexpectedCoordinates() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    ComponentIdentifier componentIdentifier = objectMapper.readValue(
        "{\"format\":\"maven\",\"coordinates\":{\"unknown\":\"x\",\"artifactId\":\"a\",\"classifier\":\"c\"," +
            "\"extension\":\"e\",\"groupId\":\"g\",\"version\":\"v\"}}",
        ComponentIdentifier.class);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiHashComponentIdentifierService
            .set(newApiHashComponentIdentifierDTO("hash", componentIdentifier, null)))
        .withMessageContaining("Coordinates contain the following incorrect entries for the given format: [unknown]");
  }

  @Test
  public void testSet_MissingCoordinates() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiHashComponentIdentifierService.set(
        newApiHashComponentIdentifierDTO("hash",
            ComponentIdentifier.createMavenCoordinates("g", null, null, null, null), null)))
        .withMessageContaining(
            "The following coordinates are missing for given format: [artifactId, extension, version]");
  }

  @Test
  public void testSet_MissingCoordinates_PackageUrl() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiHashComponentIdentifierService
        .set(newApiHashComponentIdentifierDTO("hash", null, "pkg:maven/g/a@v?classifier=c")))
        .withMessageContaining("The following coordinates are missing for given format: [extension]");
  }

  @Test
  public void testSet_EmptyRequiredCoordinates() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiHashComponentIdentifierService.set(
        newApiHashComponentIdentifierDTO("hash",
            ComponentIdentifier.createMavenCoordinates("", "", "", "", ""), null)))
        .withMessageContaining(
            "The following coordinates cannot be empty for given format: [extension, groupId, artifactId, version]");
  }

  @Test
  public void testSet_MismatchedCoordinates() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiHashComponentIdentifierService.set(
        newApiHashComponentIdentifierDTO("hash",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
            "pkg:maven/g2/a2@v2?classifier=c2&type=e2")))
        .withMessageContaining("Mismatched component identifier and package url.");
  }

  @Test
  public void testSet_ComponentIdentifierWithOptionalCoordinateAndPackageUrlWithoutOptionalCoordinate() {
    ApiHashComponentIdentifierDTO givenDTO = newApiHashComponentIdentifierDTO("hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e"), "pkg:maven/g/a@v?type=e");
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));

    ApiHashComponentIdentifierDTO returnedDTO = apiHashComponentIdentifierService.set(givenDTO);

    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(givenDTO.hash);
    assertThat(hashComponentIdentifier.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e"));
    assertClaimedComponent(returnedDTO, hashComponentIdentifier);
  }

  @Test
  public void testDelete_ShortHash() {
    HashComponentIdentifier hashComponentIdentifier =
        tempEntity.newClaimedComponent("h", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));

    apiHashComponentIdentifierService.delete(hashComponentIdentifier.getHash());

    assertThat(hashComponentIdentifierDAO.getByHash(hashComponentIdentifier.getHash())).isNull();
  }

  @Test
  public void testDelete_LongHash() {
    HashComponentIdentifier hashComponentIdentifier = tempEntity
        .newClaimedComponent("0000000000000000000000000000000000000040",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));

    apiHashComponentIdentifierService.delete("0000000000000000000000000000000000000040");

    assertThat(hashComponentIdentifierDAO.getByHash(hashComponentIdentifier.getHash())).isNull();
  }

  @Test
  public void testDelete_NotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiHashComponentIdentifierService.delete("doesNotExist"))
        .withMessageContaining("Cannot find component claim for hash doesNotExist");
  }

  @Test
  public void testGet_ClaimerInfo() {
    tempEntity.newClaimedComponent("hash", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"),
        "oldUser", "Old User");

    ApiHashComponentIdentifierDTO getResponseDTO = apiHashComponentIdentifierService.get("hash");

    assertThat(getResponseDTO.claimerId).isEqualTo("oldUser");
    assertThat(getResponseDTO.claimerName).isEqualTo("Old User");
  }

  @Test
  public void testSet_Insert_ClaimerInfo() {
    ApiHashComponentIdentifierDTO givenDTO = newApiHashComponentIdentifierDTO("hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null);
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));

    ApiHashComponentIdentifierDTO setResponseDTO = apiHashComponentIdentifierService.set(givenDTO);
    HashComponentIdentifier persisted = hashComponentIdentifierDAO.getByHash(givenDTO.hash);

    String userPrincipalUsername = USERNAME;
    String userPrincipalDisplayName = "Test User";

    assertThat(setResponseDTO.claimerId).isEqualTo(userPrincipalUsername);
    assertThat(setResponseDTO.claimerName).isEqualTo(userPrincipalDisplayName);

    assertThat(persisted.getClaimerId()).isEqualTo(userPrincipalUsername);
    assertThat(persisted.getClaimerName()).isEqualTo(userPrincipalDisplayName);
  }

  @Test
  public void testSet_Update_ClaimerInfo() {
    tempEntity.newClaimedComponent("hash", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"),
        "oldUser", "Old User");

    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));
    ApiHashComponentIdentifierDTO givenDTO = newApiHashComponentIdentifierDTO("hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null);

    ApiHashComponentIdentifierDTO setResponseDTO = apiHashComponentIdentifierService.set(givenDTO);
    HashComponentIdentifier persisted = hashComponentIdentifierDAO.getByHash(givenDTO.hash);

    String userPrincipalUsername = USERNAME;
    String userPrincipalDisplayName = "Test User";

    assertThat(setResponseDTO.claimerId).isEqualTo(userPrincipalUsername);
    assertThat(setResponseDTO.claimerName).isEqualTo(userPrincipalDisplayName);

    assertThat(persisted.getClaimerId()).isEqualTo(userPrincipalUsername);
    assertThat(persisted.getClaimerName()).isEqualTo(userPrincipalDisplayName);
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

  private ApiHashComponentIdentifierDTO newApiHashComponentIdentifierDTO(
      String hash,
      ComponentIdentifier componentIdentifier,
      String packageUrl)
  {
    ApiHashComponentIdentifierDTO apiHashComponentIdentifierDTO = new ApiHashComponentIdentifierDTO();
    apiHashComponentIdentifierDTO.hash = hash;
    apiHashComponentIdentifierDTO.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    apiHashComponentIdentifierDTO.packageUrl = packageUrl;
    apiHashComponentIdentifierDTO.comment = "comment";
    apiHashComponentIdentifierDTO.createTime = new Date();
    return apiHashComponentIdentifierDTO;
  }
}
