/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @since 1.6
 */
public class LicenseOverrideDAOTest
    extends AbstractDbDAOTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES = ComponentIdentifier.createMavenCoordinates("gid", "aid",
      "1.0");

  private LicenseOverrideLicenseInternalDAO licenseOverrideLicenseInternalDAO;

  private LicenseOverrideDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    licenseOverrideLicenseInternalDAO = daoFactory.createLicenseOverrideLicenseInternalDAO();
    dao = daoFactory.createLicenseOverrideDAO();
  }

  private void testCRUD(String ownerId) {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    LicenseOverrideStatus status = LicenseOverrideStatus.OVERRIDDEN;
    String licenseId = "Apache-2.0";
    String comment = null;

    // Create
    LicenseOverride licenseOverride = new LicenseOverride(ownerId, componentIdentifier, status, licenseId, comment);
    dao.insert(licenseOverride);
    assertThat(licenseOverride.getId()).isNotNull();
    licenseOverride = dao.getById(licenseOverride.getId());
    assertThat(licenseOverride).isNotNull();
    assertLicenseOverride(ownerId, componentIdentifier, status, licenseId, comment, licenseOverride);

    // Update
    comment = "No comments";
    licenseOverride.setComment(comment);
    dao.update(licenseOverride);
    licenseOverride = dao.getById(licenseOverride.getId());
    assertThat(licenseOverride).isNotNull();
    assertLicenseOverride(ownerId, componentIdentifier, status, licenseId, comment, licenseOverride);

    // Delete
    dao.delete(licenseOverride);
    licenseOverride = dao.getById(licenseOverride.getId());
    assertThat(licenseOverride).isNull();
  }

  private void assertLicenseOverride(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status,
      String licenseId,
      String comment,
      LicenseOverride actual)
  {
    assertLicenseOverride(ownerId, componentIdentifier, status, Collections.singleton(licenseId), comment, actual);
  }

  private void assertLicenseOverride(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status,
      Set<String> licenseIds,
      String comment,
      LicenseOverride actual)
  {
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getStatus()).isEqualTo(status);
    assertThat(actual.getLicenseIds()).isEqualTo(licenseIds);
    assertThat(actual.getComment()).isEqualTo(comment);
  }

  @Test
  public void testCRUD_Application() throws Exception {
    testCRUD(application.getId());
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    testCRUD(organization.getId());
  }

  @Test
  public void testCRUD_Repository() throws Exception {
    testCRUD(repository.getId());
  }

  @Test
  public void testGetCountByOwnerId() {
    assertThat(dao.getCountByOwnerId("xyz123xyz")).isZero();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    LicenseOverride licenseOverride =
        new LicenseOverride(application.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
            null);
    dao.insert(licenseOverride);
    assertThat(dao.getCountByOwnerId(application.getId())).isEqualTo(1);
    dao.delete(licenseOverride);
    assertThat(dao.getCountByOwnerId(application.getId())).isZero();
  }

  @Test
  public void testCommentTooLong() {
    LicenseOverride override = new LicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.OPEN,
        (String) null, StringUtils.repeat("X", LicenseOverrideDAO.MAX_COMMENT_SIZE + 1));
    assertThatThrownBy(() -> dao.insert(override)).isInstanceOf(BadRequestException.class)
        .hasMessage("Comment length must not exceed 1000 characters.");
    override.setComment(StringUtils.repeat("X", LicenseOverrideDAO.MAX_COMMENT_SIZE));
    dao.insert(override);
    override.setComment(StringUtils.repeat("X", LicenseOverrideDAO.MAX_COMMENT_SIZE + 1));
    assertThatThrownBy(() -> dao.update(override)).isInstanceOf(BadRequestException.class)
        .hasMessage("Comment length must not exceed 1000 characters.");
  }

  @Test
  public void testInvalidLicenseId_Insert() {
    assertThatThrownBy(
        () -> dao.insert(new LicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN,
            "FataMorganaId", "My comment"))).isInstanceOf(NotFoundException.class)
                .hasMessage("A license with ID 'FataMorganaId' does not exist.");

    assertThatThrownBy(
        () -> dao.insert(new LicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.SELECTED,
            "FataMorganaId", "My comment"))).isInstanceOf(NotFoundException.class)
                .hasMessage("A license with ID 'FataMorganaId' does not exist.");
  }

  @Test
  public void testInvalidLicenseId_Update() {
    LicenseOverride override =
        new LicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN,
            "Apache-2.0", "My comment");
    dao.insert(override);

    override.setLicenseIds(Collections.singleton("FataMorganaId"));
    assertThatThrownBy(() -> dao.update(override)).isInstanceOf(NotFoundException.class)
        .hasMessage("A license with ID 'FataMorganaId' does not exist.");

    override.setStatus(LicenseOverrideStatus.SELECTED);
    assertThatThrownBy(() -> dao.update(override)).isInstanceOf(NotFoundException.class)
        .hasMessage("A license with ID 'FataMorganaId' does not exist.");
  }

  @Test
  public void testNullLicenseId_Insert() {
    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      LicenseOverride override = new LicenseOverride(application.getId(), MAVEN_COORDINATES, status,
          (String) null /* licenseId */, "My comment");
      switch (status) {
        case ACKNOWLEDGED:
        case CONFIRMED:
        case OPEN:
          dao.insert(override);
          dao.delete(override);
          break;
        case OVERRIDDEN:
        case SELECTED:
          assertThatThrownBy(() -> dao.insert(override)).isInstanceOf(BadRequestException.class)
              .hasMessage("Expected at least one license ID for license override.");
          break;
        default:
          fail("Unknown license override status: " + status.getId());
      }
    }
  }

  @Test
  public void testNullLicenseId_Update() {
    LicenseOverride override = new LicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.OPEN,
        (String) null /* licenseId */, "My comment");
    dao.insert(override);

    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      override.setStatus(status);
      switch (status) {
        case ACKNOWLEDGED:
        case CONFIRMED:
        case OPEN:
          dao.update(override);
          break;
        case OVERRIDDEN:
        case SELECTED:
          assertThatThrownBy(() -> dao.update(override)).isInstanceOf(BadRequestException.class)
              .hasMessage("Expected at least one license ID for license override.");
          break;
        default:
          fail("Unknown license override status: " + status.getId());
      }
    }
  }

  @Test
  public void testNotNullLicenseId_Insert() {
    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      LicenseOverride override = new LicenseOverride(application.getId(), MAVEN_COORDINATES, status, "Apache-2.0",
          "My comment");
      switch (status) {
        case ACKNOWLEDGED:
        case CONFIRMED:
        case OPEN:
          assertThatThrownBy(() -> dao.insert(override)).isInstanceOf(BadRequestException.class)
              .hasMessage("Expected no license IDs for license override.");
          break;
        case OVERRIDDEN:
        case SELECTED:
          dao.insert(override);
          dao.delete(override);
          break;
        default:
          fail("Unknown license override status: " + status.getId());
      }
    }
  }

  @Test
  public void testNotNullLicenseId_Update() {
    LicenseOverride override =
        new LicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN,
            "Apache-2.0", "My comment");
    dao.insert(override);

    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      override.setStatus(status);
      switch (status) {
        case ACKNOWLEDGED:
        case CONFIRMED:
        case OPEN:
          assertThatThrownBy(() -> dao.update(override)).isInstanceOf(BadRequestException.class)
              .hasMessage("Expected no license IDs for license override.");
          break;
        case OVERRIDDEN:
        case SELECTED:
          dao.update(override);
          break;
        default:
          fail("Unknown license override status: " + status.getId());
      }
    }
  }

  @Test
  public void testUniqueValidation() {
    LicenseOverride override = tempEntity.newLicenseOverride(application.getId(), MAVEN_COORDINATES,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    assertThatThrownBy(() -> dao.insert(override)).isInstanceOf(BadRequestException.class)
        .hasMessage("LicenseOverride already exists for this ownerId and component");
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier_MavenGav() {
    ComponentIdentifier gavIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier gavecIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(application.getId(), gavIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    // Find by GAVEC
    LicenseOverride foundLicenseOverride = dao.getByOwnerIdAndComponentIdentifier(application.getId(), gavecIdentifier);
    assertThat(foundLicenseOverride).isNotNull();
    assertThat(foundLicenseOverride.getId()).isEqualTo(licenseOverride.getId());

    // Find by GAV
    foundLicenseOverride = dao.getByOwnerIdAndComponentIdentifier(application.getId(), gavIdentifier);
    assertThat(foundLicenseOverride).isNotNull();
    assertThat(foundLicenseOverride.getId()).isEqualTo(licenseOverride.getId());
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier_MavenGavec() {
    ComponentIdentifier gavIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier gavecIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(application.getId(), gavecIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    // Find by GAVEC
    LicenseOverride foundLicenseOverride = dao.getByOwnerIdAndComponentIdentifier(application.getId(), gavecIdentifier);
    assertThat(foundLicenseOverride).isNotNull();
    assertThat(foundLicenseOverride.getId()).isEqualTo(licenseOverride.getId());

    // Find by GAV
    foundLicenseOverride = dao.getByOwnerIdAndComponentIdentifier(application.getId(), gavIdentifier);
    assertThat(foundLicenseOverride).isNull();
  }

  @Test
  public void testGetByComponentIdentifier_MavenGavecAndGav() {
    ComponentIdentifier gavIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    LicenseOverride gavLicenseOverride = tempEntity.newLicenseOverride(application.getId(), gavIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-1.0");

    ComponentIdentifier gavecIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    Application application2 = tempEntity.newApplication(organization.getId());
    LicenseOverride gavecLicenseOverride = tempEntity.newLicenseOverride(application2.getId(), gavecIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    // Find by GAVEC (expect only GAVEC matching)
    List<LicenseOverride> licenseOverrideList;
    try (TransactionContext tx = licenseOverrideLicenseInternalDAO.createTransactionContext()) {
      licenseOverrideList = dao.getByComponentIdentifier(tx, gavecIdentifier);
    }
    assertThat(licenseOverrideList).extracting(LicenseOverride::getId).containsExactly(gavecLicenseOverride.getId());

    // Find by GAV (expect only GAV matching)
    try (TransactionContext tx = licenseOverrideLicenseInternalDAO.createTransactionContext()) {
      licenseOverrideList = dao.getByComponentIdentifier(tx, gavIdentifier);
    }
    assertThat(licenseOverrideList).extracting(LicenseOverride::getId).containsExactly(gavLicenseOverride.getId());
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier_Nuget() {
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(application.getId(), nugetIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    LicenseOverride foundLicenseOverride = dao.getByOwnerIdAndComponentIdentifier(application.getId(), nugetIdentifier);
    assertThat(foundLicenseOverride).isNotNull();
    assertThat(foundLicenseOverride.getId()).isEqualTo(licenseOverride.getId());
  }

  @Test
  public void testWithMultipleLicenseOverrides() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    LicenseOverrideStatus status = LicenseOverrideStatus.OVERRIDDEN;
    Set<String> licenseIds = new HashSet<>(Arrays.asList("Apache-1.0", "Apache-2.0"));
    String comment = null;

    LicenseOverride licenseOverride = new LicenseOverride(application.getId(), componentIdentifier, status, licenseIds,
        comment);
    dao.insert(licenseOverride);
    assertThat(licenseOverride.getId()).isNotNull();
    licenseOverride = dao.getById(licenseOverride.getId());
    assertThat(licenseOverride).isNotNull();
    assertLicenseOverride(application.getId(), componentIdentifier, status, licenseIds, comment, licenseOverride);
  }

  @Test
  public void testGetAppliedByOwnerIdAndComponentIdentifierWithHierarchy() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    // No Override Set
    LicenseOverride override =
        dao.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(application, componentIdentifier);
    assertThat(override).isNull();

    // Set @ Root
    tempEntity.newLicenseOverride(organization.getParentOrganizationId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "ANTLR-PD");
    override =
        dao.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(application, componentIdentifier);
    assertThat(override).isNotNull();
    assertLicenseOverride(organization.getParentOrganizationId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "ANTLR-PD", "testing", override);

    // Set @ Organization
    tempEntity.newLicenseOverride(organization.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "BSD-2-Clause");
    override = dao.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(application,
        componentIdentifier);
    assertThat(override).isNotNull();
    assertLicenseOverride(organization.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "BSD-2-Clause",
        "testing", override);

    // Set @ Application
    tempEntity.newLicenseOverride(application.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "BSD-3-Clause");

    override = dao.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(application,
        componentIdentifier);
    assertThat(override).isNotNull();
    assertLicenseOverride(application.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "BSD-3-Clause",
        "testing", override);

    // Set @ Repository
    tempEntity.newLicenseOverride(repository.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "BSD-4-Clause");

    override = dao.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(repository,
        componentIdentifier);
    assertThat(override).isNotNull();
    assertLicenseOverride(repository.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "BSD-4-Clause",
        "testing", override);
  }

  @Test
  public void testGetByOwnerId() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a", "v", "c", "e");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a", "v", "c", "e");
    // License overrides for the app
    LicenseOverride licenseOverride1 = tempEntity.newLicenseOverride(application.getId(), componentIdentifier1,
        LicenseOverrideStatus.OVERRIDDEN, Sets.newHashSet("Apache-2.0", "EPL-1.0"));
    LicenseOverride licenseOverride2 = tempEntity.newLicenseOverride(application.getId(), componentIdentifier2,
        LicenseOverrideStatus.OVERRIDDEN, Sets.newHashSet("Apache-1.0"));
    // License override for the org
    tempEntity.newLicenseOverride(organization.getId(), componentIdentifier1, LicenseOverrideStatus.SELECTED,
        Sets.newHashSet("GPL-2.0"));

    List<LicenseOverride> foundLicenseOverrides = dao.getByOwnerId(application.getId());
    assertThat(foundLicenseOverrides.size()).isEqualTo(2);
    assertThat(foundLicenseOverrides).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(licenseOverride1, licenseOverride2);
  }

  @Test
  public void testGetByOwnerIdsAndComponentIdentifier_returnsOneOverridePerOwner() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    LicenseOverride orgOverride = tempEntity.newLicenseOverride(organization.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");
    LicenseOverride appOverride = tempEntity.newLicenseOverride(application.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-1.0");

    Map<String, LicenseOverride> result = dao.getByOwnerIdsAndComponentIdentifier(
        Arrays.asList(organization.getId(), application.getId()), componentIdentifier);

    assertThat(result).hasSize(2);
    assertThat(result.get(organization.getId()).getId()).isEqualTo(orgOverride.getId());
    assertThat(result.get(application.getId()).getId()).isEqualTo(appOverride.getId());
  }

  @Test
  public void testGetByOwnerIdsAndComponentIdentifier_prefersGavecOverGavFallback() {
    ComponentIdentifier gavIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier gavecIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    LicenseOverride gavecOverride = tempEntity.newLicenseOverride(organization.getId(), gavecIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");
    Application app2 = tempEntity.newApplication(organization.getId());
    LicenseOverride gavOverride = tempEntity.newLicenseOverride(app2.getId(), gavIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-1.0");

    Map<String, LicenseOverride> result = dao.getByOwnerIdsAndComponentIdentifier(
        Arrays.asList(organization.getId(), app2.getId()), gavecIdentifier);

    assertThat(result).hasSize(2);
    assertThat(result.get(organization.getId()).getId()).isEqualTo(gavecOverride.getId());
    assertThat(result.get(app2.getId()).getId()).isEqualTo(gavOverride.getId());
  }

  @Test
  public void testGetByOwnerIdsAndComponentIdentifier_prefersGavecOverGavWithinSameOwner() {
    ComponentIdentifier gavIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier gavecIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    LicenseOverride gavecOverride = tempEntity.newLicenseOverride(organization.getId(), gavecIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");
    tempEntity.newLicenseOverride(organization.getId(), gavIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-1.0");

    Map<String, LicenseOverride> result = dao.getByOwnerIdsAndComponentIdentifier(
        Arrays.asList(organization.getId()), gavecIdentifier);

    assertThat(result).hasSize(1);
    assertThat(result.get(organization.getId()).getId()).isEqualTo(gavecOverride.getId());
  }

  @Test
  public void testGetByOwnerIdsAndComponentIdentifier_emptyWhenNoOverrides() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    Map<String, LicenseOverride> result = dao.getByOwnerIdsAndComponentIdentifier(
        Arrays.asList(organization.getId(), application.getId()), componentIdentifier);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_returnsOverridesAcrossAncestors() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a", "v");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a", "v");
    LicenseOverride orgOverride = tempEntity.newLicenseOverride(organization.getId(), componentIdentifier1,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");
    LicenseOverride appOverride = tempEntity.newLicenseOverride(application.getId(), componentIdentifier2,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-1.0");

    List<LicenseOverride> result;
    try (TransactionContext tx = licenseOverrideLicenseInternalDAO.createTransactionContext()) {
      result = dao.getByOwnerIdWithHierarchy(tx, application.getId());
    }

    assertThat(result).extracting(LicenseOverride::getId)
        .containsExactly(appOverride.getId(), orgOverride.getId());
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_emptyWhenNoOverrides() {
    List<LicenseOverride> result;
    try (TransactionContext tx = licenseOverrideLicenseInternalDAO.createTransactionContext()) {
      result = dao.getByOwnerIdWithHierarchy(tx, application.getId());
    }

    assertThat(result).isEmpty();
  }
}
