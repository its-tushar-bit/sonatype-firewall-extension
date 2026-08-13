/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNamePart;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import jakarta.inject.Inject;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class HashComponentIdentifierServiceTest
    extends AbstractComponentH2Test
{
  private static final String HASH = "test-abcdef";

  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("gid",
      "aid", "1.0", "jdk15", "jar");

  private static final String COMMENT = "test-comment";

  private static final Date CREATED_TIME = new Date();

  @Inject
  private LicenseOverrideDAO licenseOverrideDAO;

  @Inject
  private HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Inject
  private HashComponentIdentifierService hashComponentIdentifierService;

  @Mock
  private HdsClient mockHdsClient;

  @Test
  public void testSet_KnownToHDS() {
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(true));

    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> hashComponentIdentifierService.set(hashComponentIdentifier))
        .withMessage("The 'gid : aid : jar : jdk15 : 1.0' coordinates are already in use.");
  }

  @Test
  public void testSet_NullComponentIdentifier() {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, null);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> hashComponentIdentifierService.set(hashComponentIdentifier))
        .withMessage("The component identifier cannot be null.");
  }

  @Test
  public void testSet_InvalidComponentIdentifier() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, JsonUtils.parse(
        "{\"format\":\"maven\",\"coordinates\":null}", ComponentIdentifier.class));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> hashComponentIdentifierService.set(hashComponentIdentifier))
        .withMessage("A component identifier must have at least one coordinate.");
  }

  @Test
  public void testUpdateClaimedComponentWithOverriddenLicense() {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifier.setComment(COMMENT);
    hashComponentIdentifier.setCreateTime(CREATED_TIME);

    // Component must be unknown or we cannot claim it
    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));

    // Create the claimed component
    HashComponentIdentifierDTO serverResponse = hashComponentIdentifierService.set(hashComponentIdentifier);
    assertHashComponentIdentifierDTO(serverResponse, COMPONENT_IDENTIFIER, COMMENT, CREATED_TIME);

    // Create the license override
    Application application = tempEntity.newApplicationWithParent("testPublicId", "testName");
    LicenseOverride expectedLicenseOverride = tempEntity.newLicenseOverride(application.getId(),
        hashComponentIdentifier.getComponentIdentifier(), LicenseOverrideStatus.OVERRIDDEN, "Apache-1.0");

    // Update the claimed component
    ComponentIdentifier updatedComponentIdentifier = COMPONENT_IDENTIFIER.createAlternativeVersion("updated-version");
    hashComponentIdentifier.setComponentIdentifier(updatedComponentIdentifier);

    HashComponentIdentifierDTO response = hashComponentIdentifierService.update(hashComponentIdentifier);
    assertHashComponentIdentifierDTO(response, updatedComponentIdentifier, COMMENT, CREATED_TIME);

    // Now check the license overrides
    LicenseOverride override = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(application.getId(),
        updatedComponentIdentifier);
    assertThat(override).isNotNull();
    assertThat(override.getId()).isEqualTo(expectedLicenseOverride.getId());

    // cleanup
    licenseOverrideDAO.delete(override);
    hashComponentIdentifierDAO.delete(hashComponentIdentifier);
  }

  @Test
  public void testGet_Found() {
    Date createTime = new Date();
    String comment = "Test Comment";

    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifier.setComment(comment);
    hashComponentIdentifier.setCreateTime(createTime);
    tempEntity.newClaimedComponent(hashComponentIdentifier);

    HashComponentIdentifierDTO dto = hashComponentIdentifierService.get(HASH);

    assertHashComponentIdentifierDTO(dto, COMPONENT_IDENTIFIER, comment, createTime);
  }

  @Test
  public void testGet_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> hashComponentIdentifierService.get(HASH));
  }

  @Test
  public void testGet_ClaimerInfo() {
    tempEntity.newClaimedComponent(HASH, COMPONENT_IDENTIFIER, "oldUser", "Old User");

    HashComponentIdentifierDTO getResponseDTO = hashComponentIdentifierService.get(HASH);

    assertThat(getResponseDTO.claimerId).isEqualTo("oldUser");
    assertThat(getResponseDTO.claimerName).isEqualTo("Old User");
  }

  @Test
  public void testSet_Insert_ClaimerInfo() {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifier.setComment(COMMENT);
    hashComponentIdentifier.setCreateTime(CREATED_TIME);

    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));

    HashComponentIdentifierDTO setResponseDTO = hashComponentIdentifierService.set(hashComponentIdentifier);
    HashComponentIdentifier persisted = hashComponentIdentifierDAO.getByHash(HASH);

    String userPrincipalUsername = USERNAME;
    String userPrincipalDisplayName = "Test User";

    assertThat(setResponseDTO.claimerId).isEqualTo(userPrincipalUsername);
    assertThat(setResponseDTO.claimerName).isEqualTo(userPrincipalDisplayName);

    assertThat(persisted.getClaimerId()).isEqualTo(userPrincipalUsername);
    assertThat(persisted.getClaimerName()).isEqualTo(userPrincipalDisplayName);
  }

  @Test
  public void testSet_Update_ClaimerInfo() {
    tempEntity.newClaimedComponent(HASH, COMPONENT_IDENTIFIER, "oldUser", "Old User");

    when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(false));

    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifier.setComment(COMMENT);
    hashComponentIdentifier.setCreateTime(CREATED_TIME);

    HashComponentIdentifierDTO setResponseDTO = hashComponentIdentifierService.update(hashComponentIdentifier);
    HashComponentIdentifier persisted = hashComponentIdentifierDAO.getByHash(HASH);

    String userPrincipalUsername = USERNAME;
    String userPrincipalDisplayName = "Test User";

    assertThat(setResponseDTO.claimerId).isEqualTo(userPrincipalUsername);
    assertThat(setResponseDTO.claimerName).isEqualTo(userPrincipalDisplayName);

    assertThat(persisted.getClaimerId()).isEqualTo(userPrincipalUsername);
    assertThat(persisted.getClaimerName()).isEqualTo(userPrincipalDisplayName);
  }

  private void assertHashComponentIdentifierDTO(
      final HashComponentIdentifierDTO hashComponentIdentifierDTO,
      final ComponentIdentifier componentIdentifier,
      final String comment,
      final Date createTime)
  {
    assertThat(hashComponentIdentifierDTO).isNotNull();
    assertThat(hashComponentIdentifierDTO.hash).isEqualTo(HASH);
    assertThat(hashComponentIdentifierDTO.componentIdentifier).isEqualTo(componentIdentifier);
    assertThat(hashComponentIdentifierDTO.comment).isEqualTo(comment);
    assertThat(hashComponentIdentifierDTO.createTime).isEqualTo(createTime);

    ComponentDisplayName componentDisplayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
    assertThat(hashComponentIdentifierDTO.displayName.parts).hasSameSizeAs(componentDisplayName.parts);
    for (int i = 0; i < componentDisplayName.parts.size(); i++) {
      ComponentDisplayNamePart expected = componentDisplayName.parts.get(i);
      ComponentDisplayNamePart actual = hashComponentIdentifierDTO.displayName.parts.get(i);
      assertThat(actual.field).isEqualTo(expected.field);
      assertThat(actual.value).isEqualTo(expected.value);
    }
    assertThat(hashComponentIdentifierDTO.coordinates).isEqualTo(componentDisplayName.toString());
  }
}
