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
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @since 1.6
 */
public class LicenseOverrideDAOTest
    extends AbstractDbDAOTest
{

  private static final ComponentIdentifier MAVEN_COORDINATES = ComponentIdentifier.createMavenCoordinates("gid", "aid",
      "1.0");

  private void testCRUD(String ownerId) throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    LicenseOverrideStatus status = LicenseOverrideStatus.OVERRIDDEN;
    String licenseId = "Apache-2.0";
    String comment = null;

    // Create
    LicenseOverride licenseOverride = new LicenseOverride(ownerId, componentIdentifier, status, licenseId, comment);
    dao.insert(licenseOverride);
    assertNotNull(licenseOverride.getId());
    licenseOverride = dao.getById(licenseOverride.getId());
    assertNotNull(licenseOverride);
    assertLicenseOverride(ownerId, componentIdentifier, status, licenseId, comment, licenseOverride);

    // Update
    comment = "No comments";
    licenseOverride.setComment(comment);
    dao.update(licenseOverride);
    licenseOverride = dao.getById(licenseOverride.getId());
    assertNotNull(licenseOverride);
    assertLicenseOverride(ownerId, componentIdentifier, status, licenseId, comment, licenseOverride);

    // Delete
    dao.delete(licenseOverride);
    licenseOverride = dao.getById(licenseOverride.getId());
    assertNull(licenseOverride);
  }

  private void assertLicenseOverride(String ownerId,
                                     ComponentIdentifier componentIdentifier,
                                     LicenseOverrideStatus status,
                                     String licenseId,
                                     String comment,
                                     LicenseOverride actual)
  {
    assertLicenseOverride(ownerId, componentIdentifier, status, Collections.singleton(licenseId), comment, actual);
  }

  private void assertLicenseOverride(String ownerId,
                                     ComponentIdentifier componentIdentifier,
                                     LicenseOverrideStatus status,
                                     Set<String> licenseIds,
                                     String comment,
                                     LicenseOverride actual)
  {
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(componentIdentifier, actual.getComponentIdentifier());
    assertEquals(status, actual.getStatus());
    assertEquals(licenseIds, actual.getLicenseIds());
    assertEquals(comment, actual.getComment());
  }

  @Test
  public void testCRUD_Application() throws Exception {
    testCRUD(applicationId);
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
  public void testCommentTooLong() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();
    LicenseOverride override = new LicenseOverride(applicationId, MAVEN_COORDINATES, LicenseOverrideStatus.OPEN,
        (String) null, StringUtils.repeat("X", LicenseOverrideDAO.MAX_COMMENT_SIZE + 1));
    try {
      dao.insert(override);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("Comment length must not exceed 1000 characters.", expected.getMessage());
    }
    override.setComment(StringUtils.repeat("X", LicenseOverrideDAO.MAX_COMMENT_SIZE));
    dao.insert(override);
    override.setComment(StringUtils.repeat("X", LicenseOverrideDAO.MAX_COMMENT_SIZE + 1));
    try {
      dao.update(override);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("Comment length must not exceed 1000 characters.", expected.getMessage());
    }
  }

  @Test
  public void testInvalidLicenseId_Insert() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    LicenseOverride override = new LicenseOverride(applicationId, MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN,
        "FataMorganaId", "My comment");
    try {
      dao.insert(override);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("A license with ID 'FataMorganaId' does not exist.", expected.getMessage());
    }

    override = new LicenseOverride(applicationId, MAVEN_COORDINATES, LicenseOverrideStatus.SELECTED, "FataMorganaId",
        "My comment");
    try {
      dao.insert(override);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("A license with ID 'FataMorganaId' does not exist.", expected.getMessage());
    }
  }

  @Test
  public void testInvalidLicenseId_Update() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    LicenseOverride override = new LicenseOverride(applicationId, MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN,
        "Apache-2.0", "My comment");
    dao.insert(override);

    override.setLicenseIds(Collections.singleton("FataMorganaId"));
    try {
      dao.update(override);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("A license with ID 'FataMorganaId' does not exist.", expected.getMessage());
    }

    override.setStatus(LicenseOverrideStatus.SELECTED);
    try {
      dao.update(override);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("A license with ID 'FataMorganaId' does not exist.", expected.getMessage());
    }
  }

  @Test
  public void testNullLicenseId_Insert() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      LicenseOverride override = new LicenseOverride(applicationId, MAVEN_COORDINATES, status,
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
          try {
            dao.insert(override);
            fail("Expected BadRequestException");
          }
          catch (BadRequestException expected) {
            assertEquals("Expected at least one license ID for license override.", expected.getMessage());
          }
          break;
        default:
          fail("Unknown license override status: " + status.getId());
      }
    }
  }

  @Test
  public void testNullLicenseId_Update() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();
    LicenseOverride override = new LicenseOverride(applicationId, MAVEN_COORDINATES, LicenseOverrideStatus.OPEN,
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
          try {
            dao.update(override);
            fail("Expected BadRequestException");
          }
          catch (BadRequestException expected) {
            assertEquals("Expected at least one license ID for license override.", expected.getMessage());
          }
          break;
        default:
          fail("Unknown license override status: " + status.getId());
      }
    }
  }

  @Test
  public void testNotNullLicenseId_Insert() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      LicenseOverride override = new LicenseOverride(applicationId, MAVEN_COORDINATES, status, "Apache-2.0",
          "My comment");
      switch (status) {
        case ACKNOWLEDGED:
        case CONFIRMED:
        case OPEN:
          try {
            dao.insert(override);
            fail("Expected BadRequestException");
          }
          catch (BadRequestException expected) {
            assertEquals("Expected no license IDs for license override.", expected.getMessage());
          }
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
  public void testNotNullLicenseId_Update() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();
    LicenseOverride override = new LicenseOverride(applicationId, MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN,
        "Apache-2.0", "My comment");
    dao.insert(override);

    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      override.setStatus(status);
      switch (status) {
        case ACKNOWLEDGED:
        case CONFIRMED:
        case OPEN:
          try {
            dao.update(override);
            fail("Expected BadRequestException");
          }
          catch (BadRequestException expected) {
            assertEquals("Expected no license IDs for license override.", expected.getMessage());
          }
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
    LicenseOverrideDAO dao = new LicenseOverrideDAO();
    LicenseOverride override = tempEntity.newLicenseOverride(applicationId, MAVEN_COORDINATES,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    try {
      dao.insert(override);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("LicenseOverride already exists for this ownerId and component"));
    }
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier_MavenGav() {
    ComponentIdentifier gavIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier gavecIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(applicationId, gavIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    // Find by GAVEC
    LicenseOverride foundLicenseOverride = dao.getByOwnerIdAndComponentIdentifier(applicationId, gavecIdentifier);
    assertThat(foundLicenseOverride, is(notNullValue()));
    assertThat(foundLicenseOverride.getId(), is(licenseOverride.getId()));

    // Find by GAV
    foundLicenseOverride = dao.getByOwnerIdAndComponentIdentifier(applicationId, gavIdentifier);
    assertThat(foundLicenseOverride, is(notNullValue()));
    assertThat(foundLicenseOverride.getId(), is(licenseOverride.getId()));
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier_MavenGavec() {
    ComponentIdentifier gavIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier gavecIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(applicationId, gavecIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    // Find by GAVEC
    LicenseOverride foundLicenseOverride = dao.getByOwnerIdAndComponentIdentifier(applicationId, gavecIdentifier);
    assertThat(foundLicenseOverride, is(notNullValue()));
    assertThat(foundLicenseOverride.getId(), is(licenseOverride.getId()));

    // Find by GAV
    foundLicenseOverride = dao.getByOwnerIdAndComponentIdentifier(applicationId, gavIdentifier);
    assertThat(foundLicenseOverride, is(nullValue()));
  }

  @Test
  public void testGetByComponentIdentifier_MavenGavecAndGav() {
    ComponentIdentifier gavIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    LicenseOverride gavLicenseOverride = tempEntity.newLicenseOverride(applicationId, gavIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-1.0");

    ComponentIdentifier gavecIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    Application application2 = tempEntity.newApplication(organization.getId());
    LicenseOverride gavecLicenseOverride = tempEntity.newLicenseOverride(application2.getId(), gavecIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    // Find by GAVEC (expect only GAVEC matching)
    List<LicenseOverride> licenseOverrideList;
    try (TransactionContext tx = new LicenseOverrideLicenseInternalDAO().createTransactionContext()) {
      licenseOverrideList = dao.getByComponentIdentifier(tx, gavecIdentifier);
    }
    assertThat(licenseOverrideList, hasSize(1));
    assertThat(licenseOverrideList.get(0).getId(), is(gavecLicenseOverride.getId()));

    // Find by GAV (expect only GAV matching)
    try (TransactionContext tx = new LicenseOverrideLicenseInternalDAO().createTransactionContext()) {
      licenseOverrideList = dao.getByComponentIdentifier(tx, gavIdentifier);
    }
    assertThat(licenseOverrideList, hasSize(1));
    assertThat(licenseOverrideList.get(0).getId(), is(gavLicenseOverride.getId()));
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier_Nuget() {
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(applicationId, nugetIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    LicenseOverride foundLicenseOverride = dao.getByOwnerIdAndComponentIdentifier(applicationId, nugetIdentifier);
    assertThat(foundLicenseOverride, is(notNullValue()));
    assertThat(foundLicenseOverride.getId(), is(licenseOverride.getId()));
  }

  @Test
  public void testWithMultipleLicenseOverrides() {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    LicenseOverrideStatus status = LicenseOverrideStatus.OVERRIDDEN;
    Set<String> licenseIds = new HashSet<>(Arrays.asList("Apache-1.0", "Apache-2.0"));
    String comment = null;

    LicenseOverride licenseOverride = new LicenseOverride(applicationId, componentIdentifier, status, licenseIds,
        comment);
    dao.insert(licenseOverride);
    assertNotNull(licenseOverride.getId());
    licenseOverride = dao.getById(licenseOverride.getId());
    assertNotNull(licenseOverride);
    assertLicenseOverride(applicationId, componentIdentifier, status, licenseIds, comment, licenseOverride);
  }

  @Test
  public void testGetAppliedByOwnerIdAndComponentIdentifier() {
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    // No Override Set
    LicenseOverride override = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifier(application.getId(),
        componentIdentifier);
    assertNull(override);

    // Set @ Root
    tempEntity.newLicenseOverride(organization.getParentOrganizationId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "ANTLR-PD");
    override = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifier(application.getId(), componentIdentifier);
    assertNotNull(override);
    assertLicenseOverride(organization.getParentOrganizationId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "ANTLR-PD", "testing", override);

    // Set @ Organization
    tempEntity.newLicenseOverride(organization.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "BSD-2-Clause");
    override = new LicenseOverrideDAO().getAppliedByOwnerIdAndComponentIdentifier(application.getId(),
        componentIdentifier);
    assertNotNull(override);
    assertLicenseOverride(organization.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "BSD-2-Clause",
        "testing", override);

    // Set @ Application
    tempEntity.newLicenseOverride(application.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "BSD-3-Clause");

    override = new LicenseOverrideDAO().getAppliedByOwnerIdAndComponentIdentifier(application.getId(),
        componentIdentifier);
    assertNotNull(override);
    assertLicenseOverride(application.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "BSD-3-Clause",
        "testing", override);

    // Set @ Repository
    tempEntity.newLicenseOverride(repository.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "BSD-4-Clause");

    override = new LicenseOverrideDAO().getAppliedByOwnerIdAndComponentIdentifier(repository.getId(),
        componentIdentifier);
    assertNotNull(override);
    assertLicenseOverride(repository.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "BSD-4-Clause",
        "testing", override);
  }
}
