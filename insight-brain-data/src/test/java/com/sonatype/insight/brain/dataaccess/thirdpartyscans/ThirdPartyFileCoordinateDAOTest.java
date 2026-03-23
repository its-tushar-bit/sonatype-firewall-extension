/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import com.google.common.base.Throwables;
import org.jooq.exception.DataAccessException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.thirdpartyscans.BomPageSbomSummaryDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentListDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomDependencyTypeDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.uuid;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.DIRECT;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.TRANSITIVE;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.UNSPECIFIED;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class ThirdPartyFileCoordinateDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private ThirdPartyFileCoordinate fileCoordinate;

  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    thirdPartyCoordinateSecurityDAO = daoFactory.createThirdPartyCoordinateSecurityDAO();
    thirdPartyFileCoordinateDAO = daoFactory.createThirdPartyFileCoordinateDAO();
    thirdPartyCoordinateLicenseDAO = daoFactory.createThirdPartyCoordinateLicenseDAO();
    fileCoordinate = tempEntity.newThirdPartyFileCoordinate();
    thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();

    ThirdPartyFileCoordinate entity =
        new ThirdPartyFileCoordinate("filehash2", "source", "format",
            "name2", "version2", scannedFile.getId());
    entity.setIdentificationSources("SBOM");
    thirdPartyFileCoordinateDAO.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    ThirdPartyFileCoordinate retrievedCoordinateFile = thirdPartyFileCoordinateDAO.getById(entity.getId());
    assertThirdPartyCoordinateFile("filehash2", "source", "format", "name2", "version2", scannedFile.getId(), entity);
    assertThat(entity.getIdentificationSources()).isEqualTo("SBOM");

    // Update
    retrievedCoordinateFile.setName("UpdatedName");
    retrievedCoordinateFile.setIdentificationSources("OTHER");
    thirdPartyFileCoordinateDAO.update(retrievedCoordinateFile);
    ThirdPartyFileCoordinate updated = thirdPartyFileCoordinateDAO.getById(retrievedCoordinateFile.getId());
    assertThat(updated.getName()).isEqualTo("UpdatedName");
    assertThat(updated.getIdentificationSources()).isEqualTo("OTHER");

    // Delete
    thirdPartyFileCoordinateDAO.delete(retrievedCoordinateFile);
    retrievedCoordinateFile = thirdPartyFileCoordinateDAO.getById(retrievedCoordinateFile.getId());
    assertThat(retrievedCoordinateFile).isNull();
  }

  @Test
  public void testInsertWithBlankDisplayNameAndPackageUrl() {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();

    ThirdPartyFileCoordinate entity =
        new ThirdPartyFileCoordinate("filehash2", "SBOM", "maven",
            "log4j-core", "2.14.1", scannedFile.getId());
    entity.setIdentificationSources("SBOM");
    entity.setPackageUrl("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1?extension=jar");

    thirdPartyFileCoordinateDAO.insert(entity);
    assertThat(entity.getId()).isNotNull();

    ThirdPartyFileCoordinate retrievedCoordinateFile = thirdPartyFileCoordinateDAO.getById(entity.getId());
    retrievedCoordinateFile.getDisplayName();
    assertThat(retrievedCoordinateFile.getDisplayName()).isEqualTo("org.apache.logging.log4j : log4j-core : 2.14.1");
  }

  @Test
  public void testInsertWithBlankDisplayNameAndFormat_Name_And_Version() {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();

    ThirdPartyFileCoordinate entity =
        new ThirdPartyFileCoordinate("filehash2", "SBOM", "maven",
            "log4j-core", "2.14.1", scannedFile.getId());
    entity.setIdentificationSources("SBOM");

    thirdPartyFileCoordinateDAO.insert(entity);
    assertThat(entity.getId()).isNotNull();

    ThirdPartyFileCoordinate retrievedCoordinateFile = thirdPartyFileCoordinateDAO.getById(entity.getId());
    retrievedCoordinateFile.getDisplayName();
    assertThat(retrievedCoordinateFile.getDisplayName()).isEqualTo("log4j-core : 2.14.1");
  }

  @Test
  public void testGetByThirdPartyFileId() {
    List<ThirdPartyFileCoordinate> results =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(fileCoordinate.getThirdPartyFileId());

    assertThat(results).hasSize(1);
    assertThirdPartyCoordinateFile(fileCoordinate.getHash(), fileCoordinate.getSource(), fileCoordinate.getFormat(),
        fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getThirdPartyFileId(), results.get(0));
  }

  @Test
  public void testGetByPackageUrlAndScanId() {
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(
        thirdPartyFileDAO.getById(fileCoordinate.getThirdPartyFileId()));

    List<ThirdPartyFileCoordinate> result = thirdPartyFileCoordinateDAO.getByPackageUrlAndScanId(
        fileCoordinate.getPackageUrl(), thirdPartyScan.getScanId());

    assertThat(result).isNotEmpty();
    assertThirdPartyCoordinateFile(fileCoordinate.getHash(), fileCoordinate.getSource(), fileCoordinate.getFormat(),
        fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getThirdPartyFileId(), result.get(0));
  }

  @Test
  public void testGetByPackageUrlHashAndScanId() {
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(
        thirdPartyFileDAO.getById(fileCoordinate.getThirdPartyFileId()));

    ThirdPartyFileCoordinate result = thirdPartyFileCoordinateDAO.getByPackageUrlAndHashAndScanId(
        fileCoordinate.getPackageUrl(), fileCoordinate.getHash(), thirdPartyScan.getScanId());

    assertThat(result).isNotNull();
    assertThirdPartyCoordinateFile(fileCoordinate.getHash(), fileCoordinate.getSource(), fileCoordinate.getFormat(),
        fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getThirdPartyFileId(), result);
  }

  @Test
  public void testDeleteByThirdPartyFileId() {
    final ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    final ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");
    final ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n2", "v2");
    final ThirdPartyCoordinateSecurity sec1 =
        tempEntity
            .newThirdPartyCoordinateSecurity(coord1, "r1", "d1", "l1", 1.1f, "2.1", "CVE", "v:1", "Low", "<dd>c1</>",
                "CVSSv3", "<dd>r1</dd>", "<dd>a1</dd>", "SBOM");
    final ThirdPartyCoordinateSecurity sec2 = tempEntity
        .newThirdPartyCoordinateSecurity(coord1, "r2", "d2", "l2", 1.2f, "2.2", "CVE", "v:2", "Low", "<dd>c2</>",
            "CVSSv2", "<dd>r2</dd>", "<dd>a2</dd>", "SBOM");

    try (TransactionContext tx = thirdPartyFileCoordinateDAO.createTransactionContext()) {
      tx.begin();
      thirdPartyFileCoordinateDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());
      tx.commit();
    }

    assertThat(thirdPartyFileCoordinateDAO.getById(coord1.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(coord2.getId())).isNull();
    assertThat(thirdPartyCoordinateSecurityDAO.getById(sec1.getId())).isNull();
    assertThat(thirdPartyCoordinateSecurityDAO.getById(sec2.getId())).isNull();
  }

  @Test
  public void testDelete_Cascade() {
    final ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    final ThirdPartyFileCoordinate coordinate =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");
    final ThirdPartyCoordinateSecurity sec =
        tempEntity.newThirdPartyCoordinateSecurity(coordinate, "r1", "d1", "l1", 1.1f, "2.1", "CVE", "v:1", "Low",
            "<dd>c1</>", "CVSSv3", "<dd>r1</dd>", "<dd>a1</dd>", "SBOM");
    final ThirdPartyCoordinateLicense lic =
        tempEntity.newThirdPartyCoordinateLicense(coordinate, "lic1", "name1", "url");

    thirdPartyFileCoordinateDAO.delete(coordinate);

    assertThat(thirdPartyFileCoordinateDAO.getById(coordinate.getId())).isNull();
    assertThat(thirdPartyCoordinateSecurityDAO.getById(sec.getId())).isNull();
    assertThat(thirdPartyCoordinateLicenseDAO.getById(lic.getId())).isNull();
  }

  @Test
  public void testGetByScanId() {
    String scanId = uuid();
    String hash = tempEntity.newRandomHash();
    createThirdPartyScans(scanId, hash);

    List<ThirdPartyFileCoordinate> fileCoordinates = thirdPartyFileCoordinateDAO.getByScanId(scanId);

    assertThat(fileCoordinates).hasSize(3);
    assertThat(fileCoordinates.stream().map(ThirdPartyFileCoordinate::getSource))
        .containsExactlyInAnyOrder("s1", "s2", "s3");
  }

  @Test
  public void testGetByComponentRef() {
    String hash = tempEntity.newRandomHash();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyFileCoordinate fileCoordinate1 =
        new ThirdPartyFileCoordinate(hash, "s1", "f1", "n1", "v1", thirdPartyFile.getId());
    fileCoordinate1.setComponentRef("cr1");
    ThirdPartyFileCoordinate fileCoordinate2 =
        new ThirdPartyFileCoordinate(hash, "s2", "f2", "n2", "v2", thirdPartyFile.getId());
    ThirdPartyFileCoordinate fileCoordinate3 =
        new ThirdPartyFileCoordinate(hash, "s3", "f3", "n3", "v3", thirdPartyFile.getId());
    fileCoordinate3.setComponentRef("cr3");
    thirdPartyFileCoordinateDAO.insert(fileCoordinate1);
    thirdPartyFileCoordinateDAO.insert(fileCoordinate2);
    thirdPartyFileCoordinateDAO.insert(fileCoordinate3);

    ThirdPartyFileCoordinate fileCoordinates =
        thirdPartyFileCoordinateDAO.getByComponentRef("cr1", thirdPartyFile.getId());
    assertThat(fileCoordinates).isNotNull();
  }

  @Test
  public void testGetBySbomMetadataIdAndComponentHash() {
    Organization organization1 = tempEntity.newOrganization("org1");
    Application application = tempEntity.newApplication(organization1.getId());
    final ThirdPartyFile thirdPartyFileA = tempEntity.newThirdPartyFile();
    final ThirdPartySbomMetadata thirdPartySbomMetadataA =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFileA.getId(), application.getId(), ACTIVE,
            thirdPartyFileA.getFilename());
    final ThirdPartyFileCoordinate coordinateA =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFileA, "s1", "f1", "n1", "v1");

    final ThirdPartyFile thirdPartyFileB = tempEntity.newThirdPartyFile();
    final ThirdPartySbomMetadata thirdPartySbomMetadataB =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFileB.getId(), application.getId(), ACTIVE,
            thirdPartyFileB.getFilename());
    final ThirdPartyFileCoordinate coordinateB =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFileB, "s2", "f2", "n2", "v2");

    final ThirdPartyFile thirdPartyFileC = tempEntity.newThirdPartyFile();
    final ThirdPartySbomMetadata thirdPartySbomMetadataC =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFileC.getId(), application.getId(), ACTIVE,
            thirdPartyFileC.getFilename());
    final ThirdPartyFileCoordinate coordinateC =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFileC, "s3", "f3", "n3", "v3");

    ThirdPartyFileCoordinate actualA =
        thirdPartyFileCoordinateDAO.getBySbomMetadataIdAndComponentHash(thirdPartySbomMetadataA.getId(),
            coordinateA.getHash());

    ThirdPartyFileCoordinate actualB =
        thirdPartyFileCoordinateDAO.getBySbomMetadataIdAndComponentHash(thirdPartySbomMetadataB.getId(),
            coordinateB.getHash());

    assertThat(actualA).isNotNull().usingRecursiveComparison().isEqualTo(coordinateA);
    assertThat(actualB).isNotNull().usingRecursiveComparison().isEqualTo(coordinateB);

    assertThat(thirdPartyFileCoordinateDAO.getBySbomMetadataIdAndComponentHash(thirdPartySbomMetadataA.getId(),
        "anyHash")).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getBySbomMetadataIdAndComponentHash("anySbomId",
        coordinateA.getHash())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getBySbomMetadataIdAndComponentHash(thirdPartySbomMetadataA.getId(),
        coordinateC.getHash())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getBySbomMetadataIdAndComponentHash(thirdPartySbomMetadataC.getId(),
        coordinateC.getHash())).isNotNull();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetByThirdPartyFileIdAndPackageUrl() {
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", "npm", "juice", "1.0.1", "absdefghijklman",
        "pkg:npm/guice@1.0.1");
    tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", "npm", "juice", "1.0.2", "absdefghijklman",
        "pkg:npm/guice@1.0.2");

    ThirdPartyFileCoordinate result = thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(tpFile.getId(),
        "pkg:npm/guice@1.0.1");
    assertThat(result).isNotNull().hasFieldOrPropertyWithValue("packageUrl", "pkg:npm/guice@1.0.1");

    result = thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(tpFile.getId(), "pkg:npm/guice@1.0.2");
    assertThat(result).isNotNull().hasFieldOrPropertyWithValue("packageUrl", "pkg:npm/guice@1.0.2");

    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(tpFile.getId(),
        "pkg:npm/guice@0.0.0")).isNull();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_NoComponents() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO
        .getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null, null, null,
            null, true, 1, 2);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isZero();
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentsWithoutVulnerabilitiesAndWithoutLicenses() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl());

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO
        .getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null, null,
            null, null, true, 2, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    List<SbomComponentDTO> dtos = result.getResults();
    assertThat(dtos)
        .hasSize(2)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertSbomComponentEmpty(componentIdentifier1, packageUrlIdentifier1, coordinate1, dtos);
    assertSbomComponentEmpty(componentIdentifier2, packageUrlIdentifier2, coordinate2, dtos);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentsWithoutVulnerabilitiesAndWithLicenses() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl());

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license-1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-2", "License 2", "http://license-2");

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO
        .getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null, null,
            null, null, true, 2, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos)
        .hasSize(2)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertSbomComponentEmpty(componentIdentifier1, packageUrlIdentifier1, coordinate1, dtos);

    assertThat(dtos).filteredOn(component -> component.getHash().equals(coordinate2.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier2.getPackageUrl());
          assertThat(component.getFormat())
              .isEqualTo(componentIdentifier2.getFormat());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier2).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseId)
              .containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseName)
              .containsExactlyInAnyOrder("License 1", "License 2");
        });
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentsWithVulnerabilitiesAndLicenses() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createGolangCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl(), "componentRef-" + RandomStringUtils.insecure().nextAlphabetic(2));

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "cve-2", "description2", "link2", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix1");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity1);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity2 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2,
        "cve-2", "description2", "link2", CvssV3Severity.NONE.getStartScoreRange(),
        CvssV3Severity.NONE.getDisplayName(), "fix2");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-3", "description3", "link3",
        CvssV3Severity.LOW.getStartScoreRange() + 0.2f, CvssV3Severity.LOW.getDisplayName(), "fix3");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-4", "description4", "link4",
        CvssV3Severity.MEDIUM.getStartScoreRange() + 1f, CvssV3Severity.MEDIUM.getDisplayName(), "fix4");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-5", "description5", "link5",
        CvssV3Severity.HIGH.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix5");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-6", "description6", "link6",
        CvssV3Severity.HIGH.getEndScoreRange() - 0.1f, CvssV3Severity.HIGH.getDisplayName(), "fix6");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-7", "description7", "link7",
        CvssV3Severity.CRITICAL.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix7");

    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity2);

    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license-1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-2", "License 2", "http://license-2");

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, null,
        null, true, 2, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(2);
    assertThat(dtos)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertThat(dtos).filteredOn(component -> component.getHash().equals(coordinate1.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier1.getPackageUrl());
          assertThat(component.getFormat()).isEqualTo(componentIdentifier1.getFormat());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier1).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isOne();
          assertThat(component.getLicenses()).isNullOrEmpty();
          assertThat(component.getPercentageAnnotated()).isEqualTo(100.0);
          assertThat(component.getFileCoordinateId()).isNull();
          assertThat(component.getComponentRef()).isNotNull().startsWith("componentRef-");
        });

    assertThat(dtos).filteredOn(component -> component.getHash().equals(coordinate2.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier2.getPackageUrl());
          assertThat(component.getFormat()).isEqualTo(componentIdentifier2.getFormat());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier2).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isOne();
          assertThat(component.getVulnerabilitySeverityLowCount()).isOne();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isOne();
          assertThat(component.getVulnerabilitySeverityHighCount()).isEqualTo(2);
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isOne();
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseId)
              .containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseName)
              .containsExactlyInAnyOrder("License 1", "License 2");
          assertThat(component.getPercentageAnnotated()).isEqualTo(16.7);
          // for older records where the componentRef is not set.
          assertThat(component.getFileCoordinateId()).isEqualTo(coordinate2.getId());
          assertThat(component.getComponentRef()).isNull();
        });
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentsWithVulnerabilitiesSortingByVulnerabilities() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl());

    insertThirdPartyCoordinateSecurity(coordinate1, CvssV3Severity.CRITICAL, 0f, "cv1");
    insertThirdPartyCoordinateSecurity(coordinate1, CvssV3Severity.CRITICAL, 0f, "cv1-1");
    insertThirdPartyCoordinateSecurity(coordinate1, CvssV3Severity.CRITICAL, 0f, "cv1-2");

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.CRITICAL, 1f, "cv1");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.CRITICAL, 1f, "cv1-1");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.CRITICAL, 1f, "cv1-2");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.HIGH, 1f, "cv2");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.HIGH, 1f, "cv2-1");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.MEDIUM, 1f, "cv3");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.LOW, 1f, "cv4");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.NONE, 0f, "cv5");

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("p3", "v3");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    ThirdPartyFileCoordinate coordinate3 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl());
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.CRITICAL, 1f, "cv1");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.CRITICAL, 1f, "cv1-1");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.CRITICAL, 1f, "cv1-2");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.HIGH, 1f, "cv2");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.HIGH, 1f, "cv2-1");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.MEDIUM, 1f, "cv3");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.MEDIUM, 1f, "cv3-1");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.LOW, 1f, "cv4");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.NONE, 0f, "cv5");

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, null, SbomComponentSortableField.VULNERABILITIES,
        false, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(3);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(3);
    assertThat(dtos.get(0).getHash()).isEqualTo("h3");
    assertThat(dtos.get(1).getHash()).isEqualTo("h2");
    assertThat(dtos.get(2).getHash()).isEqualTo("h1");
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentsWithVulnerabilitiesSortingByPercentage() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl(), DIRECT);
    ThirdPartyCoordinateSecurity coordinateSecurity =
        insertThirdPartyCoordinateSecurity(coordinate1, CvssV3Severity.CRITICAL, 0f, "cv1");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl(), TRANSITIVE);
    ThirdPartyCoordinateSecurity coordinateSecurity2 =
        insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.CRITICAL, 1f, "cv1");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.CRITICAL, 1f, "cv1-1");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity2);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("p3", "v3");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    ThirdPartyFileCoordinate coordinate3 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl(), DIRECT);
    ThirdPartyCoordinateSecurity coordinateSecurity3 =
        insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.LOW, 1f, "cv1");
    ThirdPartyCoordinateSecurity coordinateSecurity4 =
        insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.NONE, 0f, "cv2");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.NONE, 0f, "cv3");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity4);
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity3);

    ComponentIdentifier componentIdentifierWithoutVex = ComponentIdentifier.createNpmCoordinates("p4", "v4");
    PackageUrlIdentifier packageUrlIdentifierWithoutVex =
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifierWithoutVex);
    ThirdPartyFileCoordinate coordinateWithoutVex =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s4",
            packageUrlIdentifierWithoutVex.getFormat(), packageUrlIdentifierWithoutVex.getName(),
            packageUrlIdentifierWithoutVex.getVersion(), "h4", packageUrlIdentifierWithoutVex.getPackageUrl(), DIRECT);
    insertThirdPartyCoordinateSecurity(coordinateWithoutVex, CvssV3Severity.LOW, 1f, "cv1");

    ComponentIdentifier componentIdentifierWithoutVulnerabilities =
        ComponentIdentifier.createNpmCoordinates("p5", "v5");
    PackageUrlIdentifier packageUrlIdentifierWithoutVulnerabilities =
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifierWithoutVulnerabilities);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s5",
        packageUrlIdentifierWithoutVulnerabilities.getFormat(), packageUrlIdentifierWithoutVulnerabilities.getName(),
        packageUrlIdentifierWithoutVulnerabilities.getVersion(), "h5",
        packageUrlIdentifierWithoutVulnerabilities.getPackageUrl(), DIRECT);

    // Percentages in descending order
    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, null, SbomComponentSortableField.PERCENTAGE_ANNOTATED,
        false, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(5);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(5);
    assertThat(dtos.get(0).getName()).isEqualTo("p1");
    assertThat(dtos.get(0).getPercentageAnnotated()).isEqualTo(100.0);
    assertThat(dtos.get(0).getReleaseStatusPercentage()).isEqualTo(100.0);
    assertThat(dtos.get(1).getName()).isEqualTo("p3");
    assertThat(dtos.get(1).getPercentageAnnotated()).isEqualTo(66.7);
    assertThat(dtos.get(1).getReleaseStatusPercentage()).isEqualTo(100.0);
    assertThat(dtos.get(2).getName()).isEqualTo("p2");
    assertThat(dtos.get(2).getPercentageAnnotated()).isEqualTo(50.0);
    assertThat(dtos.get(3).getName()).isEqualTo("p5");
    assertThat(dtos.get(3).getPercentageAnnotated()).isEqualTo(0);
    assertThat(dtos.get(3).getReleaseStatusPercentage()).isEqualTo(100.0);
    assertThat(dtos.get(4).getName()).isEqualTo("p4");
    assertThat(dtos.get(4).getPercentageAnnotated()).isEqualTo(0);
    assertThat(dtos.get(4).getReleaseStatusPercentage()).isEqualTo(100.0);

    // Percentages in ascending order
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null,
        null, null, SbomComponentSortableField.PERCENTAGE_ANNOTATED, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(5);

    dtos = result.getResults();

    assertThat(dtos).hasSize(5);
    assertThat(dtos.get(0).getName()).isEqualTo("p4");
    assertThat(dtos.get(0).getPercentageAnnotated()).isEqualTo(0);
    assertThat(dtos.get(1).getName()).isEqualTo("p5");
    assertThat(dtos.get(1).getPercentageAnnotated()).isEqualTo(0);
    assertThat(dtos.get(2).getName()).isEqualTo("p2");
    assertThat(dtos.get(2).getPercentageAnnotated()).isEqualTo(50.0);
    assertThat(dtos.get(3).getName()).isEqualTo("p3");
    assertThat(dtos.get(3).getPercentageAnnotated()).isEqualTo(66.7);
    assertThat(dtos.get(4).getName()).isEqualTo("p1");
    assertThat(dtos.get(4).getPercentageAnnotated()).isEqualTo(100.0);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentsWithVulnerabilitiesSortingByReleaseStatusPercentage() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1",
            packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(),
            packageUrlIdentifier1.getVersion(), "h1",
            packageUrlIdentifier1.getPackageUrl(), DIRECT);
    ThirdPartyCoordinateSecurity coordinateSecurity =
        insertThirdPartyCoordinateSecurity(coordinate1, CvssV3Severity.CRITICAL, 0f, "cv1");
    insertThirdPartyCoordinateSecurity(coordinate1, CvssV3Severity.MEDIUM,
        CvssV3Severity.MEDIUM.getEndScoreRange() - CvssV3Severity.MEDIUM.getStartScoreRange(), "cv1-2");
    insertThirdPartyCoordinateSecurity(coordinate1, CvssV3Severity.HIGH, 0.8f, "cv1-3");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s2",
            packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(),
            packageUrlIdentifier2.getVersion(), "h2",
            packageUrlIdentifier2.getPackageUrl(), TRANSITIVE);
    ThirdPartyCoordinateSecurity coordinateSecurity2 =
        insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.CRITICAL, 1f, "cv1");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.CRITICAL, 1f, "cv1-1");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.HIGH, 0.9f, "cv2");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.MEDIUM, 0.6f, "cv3");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.LOW, 0.5f, "cv4");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.NONE, 0f, "cv5");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity2);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("p3", "v3");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    ThirdPartyFileCoordinate coordinate3 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s3",
            packageUrlIdentifier3.getFormat(),
            packageUrlIdentifier3.getName(),
            packageUrlIdentifier3.getVersion(), "h3",
            packageUrlIdentifier3.getPackageUrl(), DIRECT);
    ThirdPartyCoordinateSecurity coordinateSecurity3 =
        insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.LOW, 1f, "cv1");
    ThirdPartyCoordinateSecurity coordinateSecurity4 =
        insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.NONE, 0f, "cv2");
    ThirdPartyCoordinateSecurity coordinateSecurityMedium =
        insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.MEDIUM, 0.6f, "cvmedium");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.NONE, 0f, "cv3");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.HIGH, 0.9f, "cv4");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurityMedium);
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity4);
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity3);

    ComponentIdentifier componentIdentifierWithoutVex = ComponentIdentifier.createNpmCoordinates("p4", "v4");
    PackageUrlIdentifier packageUrlIdentifierWithoutVex =
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifierWithoutVex);
    ThirdPartyFileCoordinate coordinateWithoutVex =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s4",
            packageUrlIdentifierWithoutVex.getFormat(),
            packageUrlIdentifierWithoutVex.getName(),
            packageUrlIdentifierWithoutVex.getVersion(), "h4",
            packageUrlIdentifierWithoutVex.getPackageUrl(), DIRECT);
    insertThirdPartyCoordinateSecurity(coordinateWithoutVex, CvssV3Severity.LOW, 1f, "cv1");

    ComponentIdentifier componentIdentifierWithoutVulnerabilities =
        ComponentIdentifier.createNpmCoordinates("p5", "v5");
    PackageUrlIdentifier packageUrlIdentifierWithoutVulnerabilities =
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifierWithoutVulnerabilities);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s5",
        packageUrlIdentifierWithoutVulnerabilities.getFormat(),
        packageUrlIdentifierWithoutVulnerabilities.getName(),
        packageUrlIdentifierWithoutVulnerabilities.getVersion(), "h5",
        packageUrlIdentifierWithoutVulnerabilities.getPackageUrl(), DIRECT);

    // Release status percentages in descending order
    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, null, SbomComponentSortableField.RELEASE_STATUS_PERCENTAGE,
        false, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(5);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(5);
    assertThat(dtos.get(0).getName()).isEqualTo("p5");
    assertThat(dtos.get(0).getReleaseStatusPercentage()).isEqualTo(100.0);
    assertThat(dtos.get(1).getName()).isEqualTo("p4");
    assertThat(dtos.get(1).getReleaseStatusPercentage()).isEqualTo(100.0);
    assertThat(dtos.get(2).getName()).isEqualTo("p1");
    assertThat(dtos.get(2).getReleaseStatusPercentage()).isEqualTo(50.0);
    assertThat(dtos.get(3).getName()).isEqualTo("p2");
    assertThat(dtos.get(3).getReleaseStatusPercentage()).isEqualTo(33.3);
    assertThat(dtos.get(4).getName()).isEqualTo("p3");
    assertThat(dtos.get(4).getReleaseStatusPercentage()).isEqualTo(0.0);

    // Release status percentages in ascending order
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null,
        null, null, SbomComponentSortableField.RELEASE_STATUS_PERCENTAGE, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(5);

    dtos = result.getResults();

    assertThat(dtos).hasSize(5);
    assertThat(dtos.get(0).getName()).isEqualTo("p3");
    assertThat(dtos.get(0).getReleaseStatusPercentage()).isEqualTo(0.0);
    assertThat(dtos.get(1).getName()).isEqualTo("p2");
    assertThat(dtos.get(1).getReleaseStatusPercentage()).isEqualTo(33.3);
    assertThat(dtos.get(2).getName()).isEqualTo("p1");
    assertThat(dtos.get(2).getReleaseStatusPercentage()).isEqualTo(50.0);
    assertThat(dtos.get(3).getName()).isEqualTo("p4");
    assertThat(dtos.get(3).getReleaseStatusPercentage()).isEqualTo(100.0);
    assertThat(dtos.get(4).getName()).isEqualTo("p5");
    assertThat(dtos.get(4).getReleaseStatusPercentage()).isEqualTo(100.0);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentsWithVulnerabilitiesSortingByType() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1", packageUrlIdentifier1.getFormat(),
        packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(), "h1",
        packageUrlIdentifier1.getPackageUrl(), DIRECT);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl(), TRANSITIVE);
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.CRITICAL, 1f, "cv1");
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.CRITICAL, 1f, "cv1-1");

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("p3", "v3");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    ThirdPartyFileCoordinate coordinate3 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl(), DIRECT);
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.LOW, 1f, "cv4");
    insertThirdPartyCoordinateSecurity(coordinate3, CvssV3Severity.NONE, 0f, "cv5");

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, null,
        SbomComponentSortableField.TYPE, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(3);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(3);
    assertThat(dtos.get(0).getHash()).isEqualTo("h1");
    assertThat(dtos.get(1).getHash()).isEqualTo("h3");
    assertThat(dtos.get(2).getHash()).isEqualTo("h2");
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_Pagination() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl(), TRANSITIVE);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl(), DIRECT);

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, null,
        SbomComponentSortableField.TYPE, true, 1, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    assertThat(result.getResults())
        .hasSize(1)
        .extracting(SbomComponentDTO::getHash)
        .containsExactly(coordinate2.getHash());

    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, null,
        SbomComponentSortableField.TYPE, true, 1, 2);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    assertThat(result.getResults())
        .hasSize(1)
        .extracting(SbomComponentDTO::getHash)
        .containsExactly(coordinate1.getHash());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_FilterByVulnerabilityThreatLevels() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl());
    insertThirdPartyCoordinateSecurity(coordinate1, CvssV3Severity.LOW, 1f, "cv1");

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());
    insertThirdPartyCoordinateSecurity(coordinate2, CvssV3Severity.CRITICAL, 0f, "cv2");

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), Collections.singleton(CvssV3Severity.LOW), null, null,
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();

    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getHash)
        .containsExactly(coordinate1.getHash());

    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), Collections.singleton(CvssV3Severity.CRITICAL), null, null,
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();

    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getHash)
        .containsExactly(coordinate2.getHash());

    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(),
        Sets.newHashSet(CvssV3Severity.LOW, CvssV3Severity.CRITICAL), null, null,
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_FilterByDependencyTypes() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl(), TRANSITIVE);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl(), DIRECT);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("p3", "v3");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    ThirdPartyFileCoordinate coordinate3 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl(), UNSPECIFIED);

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, Collections.singleton(DIRECT), null,
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();

    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getHash)
        .containsExactly(coordinate2.getHash());

    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null,
        Collections.singleton(TRANSITIVE), null, null,
        true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();

    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getHash)
        .containsExactly(coordinate1.getHash());

    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null,
        Collections.singleton(UNSPECIFIED), null, null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();

    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getHash)
        .containsExactly(coordinate3.getHash());

    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null,
        Sets.newHashSet(DIRECT, TRANSITIVE, UNSPECIFIED), null, null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(3);

    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash(), coordinate3.getHash());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentNameFilter() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("slf4j-log4j12", "1.7.12");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl(), TRANSITIVE);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("cxf-rt-transports-http-jetty",
        "3.0.4");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl(), DIRECT);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("slf4j-log4j", "2.4.0");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl(), UNSPECIFIED);

    PackageUrlIdentifier packageUrlIdentifier4 =
        new PackageUrlIdentifier("pkg:maven/com.github.jnr/jffi@1.3.1?classifier=native&type=jar");
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s4", packageUrlIdentifier4.getFormat(), packageUrlIdentifier4.getName(), packageUrlIdentifier4.getVersion(),
        "h4", packageUrlIdentifier4.getPackageUrl(), UNSPECIFIED);

    PackageUrlIdentifier packageUrlIdentifier5 =
        new PackageUrlIdentifier("pkg:npm/%40react-spring/web@9.7.3");
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s5", packageUrlIdentifier5.getFormat(),
        packageUrlIdentifier5.getName(), packageUrlIdentifier5.getVersion(), "h5",
        packageUrlIdentifier5.getPackageUrl(), UNSPECIFIED);

    // testing name
    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "slf4j-log4j",
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    // testing version
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "2.4",
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getPackageUrl)
        .containsExactly(packageUrlIdentifier3.getPackageUrl());

    // testing packageUrl - not part of namespace, name or version
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "native", null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getPackageUrl)
        .containsExactly(packageUrlIdentifier4.getPackageUrl());

    // testing packageUrl - URL encoding characters
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "@react", null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getPackageUrl)
        .containsExactly(packageUrlIdentifier5.getPackageUrl());

    // testing no componentName filter
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(),
        null, null, null, null,
        true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(5);

    // test excluding format name from search
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "npm",
        null, true, 5, 1);
    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isZero();
    assertThat(result.getResults()).isEmpty();

    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null,
        "com.github.jnr", null, true, 5, 1);
    assertThat(result).isNotNull();
    assertThat(result.getResults()).hasSize(1);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentNameFilter_SpecialCharacters() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    PackageUrlIdentifier packageUrlIdentifier1 =
        new PackageUrlIdentifier("pkg:maven/com.datadoghq/dd-java-agent@1.12.1?type=jar");
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl(), UNSPECIFIED);

    PackageUrlIdentifier packageUrlIdentifier2 =
        new PackageUrlIdentifier("pkg:golang/github.com/gorilla/context@234fd47e07d1004f0aed9c");
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl(), UNSPECIFIED);

    PackageUrlIdentifier packageUrlIdentifier3 =
        new PackageUrlIdentifier("pkg:maven/com.datadoghq/dd-trace-ot@1.12.1?type=jar");
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl(), UNSPECIFIED);

    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s4", "npm", "com.test.org",
        "0.29.1",
        "h4", null, UNSPECIFIED);

    PackageUrlIdentifier packageUrlIdentifier4 = new PackageUrlIdentifier(
        "pkg:generic/ubuntu%3A22.04/cyrus-sasl2%2Flibsasl2-2@2.1.27%2Bdfsg2-3ubuntu1.2?nexustype=container");

    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s4", packageUrlIdentifier4.getFormat(),
        packageUrlIdentifier4.getName(), packageUrlIdentifier4.getVersion(), "h4",
        packageUrlIdentifier4.getPackageUrl(), TRANSITIVE);

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null,
        "com.datadoghq : dd-java-agent ", null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();

    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null,
        "github.com/gorilla/context 234fd47e07d1004f0aed9c", null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getPackageUrl)
        .containsExactly(packageUrlIdentifier2.getPackageUrl());

    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null,
        "github.com/gorilla/context", null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getPackageUrl)
        .containsExactly(packageUrlIdentifier2.getPackageUrl());

    // testing looking for name when packageUrl null
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(),
        null, null, "com.test.org", null, true,
        5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    assertThat(result.getResults()).extracting(SbomComponentDTO::getName)
        .containsExactly("com.test.org");

    // testing looking for version when packageUrl null
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(),
        null, null, "0.29.1", null, true,
        5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    assertThat(result.getResults()).extracting(SbomComponentDTO::getVersion)
        .containsExactly("0.29.1");

    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null,
        null, "ubuntu:22.04 : cyrus-sasl2/libsasl2-2 : 2.1.27+dfsg2-3ubuntu1.2", null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getPackageUrl)
        .containsExactly(packageUrlIdentifier4.getPackageUrl());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentNameAndDependecyTypeFilters() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    PackageUrlIdentifier packageUrlIdentifier1 = new PackageUrlIdentifier("pkg:maven/a/b@c?type=jar");
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1", packageUrlIdentifier1.getFormat(),
        packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(), "h1",
        packageUrlIdentifier1.getPackageUrl(), DIRECT);

    PackageUrlIdentifier packageUrlIdentifier2 = new PackageUrlIdentifier("pkg:golang/d/e@f");
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s2", packageUrlIdentifier2.getFormat(),
        packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(), "h2",
        packageUrlIdentifier2.getPackageUrl(), DIRECT);

    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, Collections.singleton(DIRECT), "a : b : c", null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getPackageUrl)
        .containsExactly(packageUrlIdentifier1.getPackageUrl());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_LicenseFilter() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("a-slf4j-log4j12", "1.7.12");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl(), TRANSITIVE);

    ComponentIdentifier componentIdentifier2 =
        ComponentIdentifier.createNpmCoordinates("b-cxf-rt-transports-http-jetty",
            "3.0.4");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl(), DIRECT);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("c-slf4j-log4j", "2.4.0");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl(), UNSPECIFIED);

    ComponentIdentifier componentIdentifier4 = ComponentIdentifier.createNpmCoordinates("d-license-blah", "3.5.0");
    PackageUrlIdentifier packageUrlIdentifier4 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier4);
    ThirdPartyFileCoordinate coordinate4 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s4", packageUrlIdentifier4.getFormat(), packageUrlIdentifier4.getName(), packageUrlIdentifier4.getVersion(),
        "h4", packageUrlIdentifier4.getPackageUrl(), UNSPECIFIED);

    tempEntity.newThirdPartyCoordinateLicense(coordinate1, "license-1", "License 1", "http://license1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-2", "License 2", "http://license2");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-3", "SpecialChars %$3", "http://license3");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-4", "Another 4", "http://license4");
    tempEntity.newThirdPartyCoordinateLicense(coordinate4, "some-5", "Some 5", "http://some5");

    // testing with license id
    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "license-1",
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    SbomComponentDTO dto = result.getResults().get(0);
    assertThat(dto.getComponentIdentifier()).isEqualTo(componentIdentifier1);
    Set<ResolvedLicenseDTO> licenses = result.getResults().get(0).getLicenses();
    assertThat(licenses).hasSize(1);
    assertThat(licenses).extracting(ResolvedLicenseDTO::licenseId).containsExactly("license-1");

    // testing with license id partial
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "nse-",
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(3);
    // componentIdentifier1, componentIdentifier2 match based on license text (license-x)
    // while componentIdentifier4 is matched based on component name (d-license-blah)
    result.getResults().sort(Comparator.comparing(SbomComponentDTO::getName)); // sort results by name
    dto = result.getResults().get(0);
    assertThat(dto.getComponentIdentifier()).isEqualTo(componentIdentifier1);
    licenses = dto.getLicenses();
    assertThat(licenses).hasSize(1);
    assertThat(licenses).extracting(ResolvedLicenseDTO::licenseId).containsExactly("license-1");
    dto = result.getResults().get(1);
    assertThat(dto.getComponentIdentifier()).isEqualTo(componentIdentifier2);
    licenses = dto.getLicenses();
    assertThat(licenses).hasSize(3);
    assertThat(licenses).extracting(ResolvedLicenseDTO::licenseId)
        .containsExactlyInAnyOrder("license-2", "license-3", "license-4");
    dto = result.getResults().get(2);
    assertThat(dto.getComponentIdentifier()).isEqualTo(componentIdentifier4); // matches component name 'd-license-blah'
    licenses = dto.getLicenses();
    assertThat(licenses).hasSize(1);
    assertThat(licenses).extracting(ResolvedLicenseDTO::licenseId).containsExactly("some-5");

    // testing with license name
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "Chars",
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    dto = result.getResults().get(0);
    assertThat(dto.getComponentIdentifier()).isEqualTo(componentIdentifier2);
    licenses = dto.getLicenses();
    assertThat(licenses).hasSize(3);
    assertThat(licenses).extracting(ResolvedLicenseDTO::licenseId)
        .containsExactlyInAnyOrder("license-2", "license-3", "license-4");

    // testing with license name special characters
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "%$3",
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    dto = result.getResults().get(0);
    assertThat(dto.getComponentIdentifier()).isEqualTo(componentIdentifier2);
    licenses = dto.getLicenses();
    assertThat(licenses).hasSize(3);
    assertThat(licenses).extracting(ResolvedLicenseDTO::licenseId)
        .containsExactlyInAnyOrder("license-2", "license-3", "license-4");
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_SortByDisplayName() {
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();
    String a = newThirdPartyFileCoordinate(thirdPartySbomMetadata, "a").getComponentRef();
    String b = newThirdPartyFileCoordinate(thirdPartySbomMetadata, "b").getComponentRef();
    String c = newThirdPartyFileCoordinate(thirdPartySbomMetadata, "c").getComponentRef();
    SbomComponentListDTO result;

    // Ascending all results
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        thirdPartySbomMetadata.getThirdPartyFileId(),
        null,
        null,
        null,
        SbomComponentSortableField.DISPLAY_NAME,
        true,
        10,
        1);
    assertThat(result.getResults())
        .isNotNull()
        .extracting(SbomComponentDTO::getComponentRef)
        .containsExactly(a, b, c);

    // Descending all results
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        thirdPartySbomMetadata.getThirdPartyFileId(),
        null,
        null,
        null,
        SbomComponentSortableField.DISPLAY_NAME,
        false,
        10,
        1);
    assertThat(result.getResults())
        .isNotNull()
        .extracting(SbomComponentDTO::getComponentRef)
        .containsExactly(c, b, a);

    // Ascending paged results
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        thirdPartySbomMetadata.getThirdPartyFileId(),
        null,
        null,
        null,
        SbomComponentSortableField.DISPLAY_NAME,
        true,
        2,
        1);
    assertThat(result.getResults())
        .isNotNull()
        .extracting(SbomComponentDTO::getComponentRef)
        .containsExactly(a, b);
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        thirdPartySbomMetadata.getThirdPartyFileId(),
        null,
        null,
        null,
        SbomComponentSortableField.DISPLAY_NAME,
        true,
        2,
        2);
    assertThat(result.getResults())
        .isNotNull()
        .extracting(SbomComponentDTO::getComponentRef)
        .containsExactly(c);

    // Descending paged results
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        thirdPartySbomMetadata.getThirdPartyFileId(),
        null,
        null,
        null,
        SbomComponentSortableField.DISPLAY_NAME,
        false,
        2,
        1);
    assertThat(result.getResults())
        .isNotNull()
        .extracting(SbomComponentDTO::getComponentRef)
        .containsExactly(c, b);
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        thirdPartySbomMetadata.getThirdPartyFileId(),
        null,
        null,
        null,
        SbomComponentSortableField.DISPLAY_NAME,
        false,
        2,
        2);
    assertThat(result.getResults())
        .isNotNull()
        .extracting(SbomComponentDTO::getComponentRef)
        .containsExactly(a);

    // Tiebrakers
    String d = newThirdPartyFileCoordinate(thirdPartySbomMetadata, "d").getComponentRef();
    String a0 = newThirdPartyFileCoordinate(thirdPartySbomMetadata, "a", s -> s.setName("n0")).getComponentRef();
    String a2 = newThirdPartyFileCoordinate(thirdPartySbomMetadata, "a", s -> s.setName("n2")).getComponentRef();
    String b0 = newThirdPartyFileCoordinate(thirdPartySbomMetadata, "b", s -> s.setVersion("v0")).getComponentRef();
    String b2 = newThirdPartyFileCoordinate(thirdPartySbomMetadata, "b", s -> s.setVersion("v2")).getComponentRef();
    String c0 =
        newThirdPartyFileCoordinate(thirdPartySbomMetadata, "c", s -> s.setPackageUrl("pkg:f/p0@v")).getComponentRef();
    String c2 =
        newThirdPartyFileCoordinate(thirdPartySbomMetadata, "c", s -> s.setPackageUrl("pkg:f/p2@v")).getComponentRef();
    String d0 = newThirdPartyFileCoordinate(thirdPartySbomMetadata, "d", s -> s.setHash("h0")).getComponentRef();
    String d2 = newThirdPartyFileCoordinate(thirdPartySbomMetadata, "d", s -> s.setHash("h2")).getComponentRef();

    // Ascending all results with tiebrakers
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        thirdPartySbomMetadata.getThirdPartyFileId(),
        null,
        null,
        null,
        SbomComponentSortableField.DISPLAY_NAME,
        true,
        20,
        1);
    assertThat(result.getResults())
        .isNotNull()
        .extracting(SbomComponentDTO::getComponentRef)
        .containsExactly(a0, a, a2, b0, b, b2, c0, c, c2, d0, d, d2);

    // Descending all results with tiebrakers
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        thirdPartySbomMetadata.getThirdPartyFileId(),
        null,
        null,
        null,
        SbomComponentSortableField.DISPLAY_NAME,
        false,
        20,
        1);
    assertThat(result.getResults())
        .isNotNull()
        .extracting(SbomComponentDTO::getComponentRef)
        .containsExactly(d2, d, d0, c2, c, c0, b2, b, b0, a2, a, a0);
  }

  @Test
  public void testGetByHashOrComponentRefForThirdPartyFileId() {
    ThirdPartyFileCoordinate fc = tempEntity.newThirdPartyFileCoordinate();
    ThirdPartyFileCoordinate fc2 = tempEntity.newThirdPartyFileCoordinate();
    String hash = RandomStringUtils.insecure().nextAlphanumeric(19);
    String ref = hash + "-ref";
    fc.setHash(hash);
    fc.setComponentRef(ref);
    // a different sbom/thirdpartyFileId but having the same component
    fc2.setHash(hash);
    fc2.setComponentRef(ref);
    thirdPartyFileCoordinateDAO.update(fc);
    thirdPartyFileCoordinateDAO.update(fc2);

    // hash only
    List<ThirdPartyFileCoordinate> results =
        thirdPartyFileCoordinateDAO.getByHashOrComponentRefForThirdPartyFileId(fc.getThirdPartyFileId(), hash, null);
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).hasFieldOrPropertyWithValue("componentRef", ref);

    results =
        thirdPartyFileCoordinateDAO.getByHashOrComponentRefForThirdPartyFileId(fc.getThirdPartyFileId(), null, ref);
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).hasFieldOrPropertyWithValue("hash", hash);

    results =
        thirdPartyFileCoordinateDAO.getByHashOrComponentRefForThirdPartyFileId(fc.getThirdPartyFileId(), hash, ref);
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).hasFieldOrPropertyWithValue("componentRef", ref);
    assertThat(results.get(0)).hasFieldOrPropertyWithValue("hash", hash);
  }

  @Test
  public void testGetByHashOrComponentRefForThirdPartyFileId_NotFound() {
    String hash = RandomStringUtils.insecure().nextAlphanumeric(19);
    String ref = hash + "-ref";
    ThirdPartyFileCoordinate fc = tempEntity.newThirdPartyFileCoordinate();
    fc.setHash(hash);
    fc.setComponentRef(ref);
    thirdPartyFileCoordinateDAO.update(fc);

    assertThat(
        thirdPartyFileCoordinateDAO.getByHashOrComponentRefForThirdPartyFileId(fc.getThirdPartyFileId(), "notfound",
            "notfound")).isEmpty();
    assertThat(
        thirdPartyFileCoordinateDAO.getByHashOrComponentRefForThirdPartyFileId("notfound", hash,
            ref)).isEmpty();
  }

  private ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      ThirdPartySbomMetadata thirdPartySbomMetadata,
      String displayName)
  {
    return tempEntity.newThirdPartyFileCoordinate(
        null,
        thirdPartyFileDAO.getById(thirdPartySbomMetadata.getThirdPartyFileId()),
        "s1",
        "f1",
        "n1",
        "v1",
        "h1",
        "pkg:f/p1@v",
        MatchState.EXACT.getId(),
        List.of("occurrence"),
        List.of("filename"),
        displayName,
        "componentRef-" + RandomStringUtils.insecure().nextAlphabetic(2));
  }

  private ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      ThirdPartySbomMetadata thirdPartySbomMetadata,
      String displayName,
      Consumer<ThirdPartyFileCoordinate> thirdPartyFileCoordinateConsumer)
  {
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        newThirdPartyFileCoordinate(thirdPartySbomMetadata, displayName);
    thirdPartyFileCoordinateConsumer.accept(thirdPartyFileCoordinate);
    thirdPartyFileCoordinateDAO.update(thirdPartyFileCoordinate);
    return thirdPartyFileCoordinate;
  }

  private void insertVEXToThirdPartyCoordinateSecurity(ThirdPartyCoordinateSecurity coordinateSecurity) {
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
        "state", "justification", "response", "detail");
  }

  private ThirdPartyCoordinateSecurity insertThirdPartyCoordinateSecurity(
      ThirdPartyFileCoordinate coordinate,
      CvssV3Severity severity,
      float range,
      String refId)
  {
    return tempEntity.newThirdPartyCoordinateSecurity(coordinate, refId, "description", "link",
        severity.getStartScoreRange() + range, severity.getDisplayName(), "fix");
  }

  private void assertThirdPartyCoordinateFile(
      final String hash,
      final String source,
      final String format,
      final String name,
      final String version,
      final String thirdPartyFileId,
      final ThirdPartyFileCoordinate entity)
  {
    assertThat(entity.getHash()).isEqualTo(hash);
    assertThat(entity.getSource()).isEqualTo(source);
    assertThat(entity.getName()).isEqualTo(name);
    assertThat(entity.getFormat()).isEqualTo(format);
    assertThat(entity.getVersion()).isEqualTo(version);
    assertThat(entity.getThirdPartyFileId()).isEqualTo(thirdPartyFileId);
  }

  private List<ThirdPartyFileCoordinate> createThirdPartyScans(String scanId, String hash) {
    List<ThirdPartyFileCoordinate> fileCoordinateList = new ArrayList<>();

    String scanRequestId = uuid();

    ThirdPartyFileCoordinate fileCoordinate1 = new ThirdPartyFileCoordinate(hash, "s1", "f1", "n1", "v1", null);
    fileCoordinate1.setComponentRef("cr1");
    newThirdPartyScan(scanId, scanRequestId, fileCoordinate1);
    fileCoordinateList.add(fileCoordinate1);

    ThirdPartyFileCoordinate fileCoordinate2 = new ThirdPartyFileCoordinate(hash, "s2", "f2", "n2", "v2", null);
    newThirdPartyScan(scanId, scanRequestId, fileCoordinate2);
    fileCoordinateList.add(fileCoordinate2);

    ThirdPartyFileCoordinate fileCoordinate3 =
        new ThirdPartyFileCoordinate(tempEntity.newRandomHash(), "s3", "f3", "n3", "v3", null);
    newThirdPartyScan(scanId, scanRequestId, fileCoordinate3);
    fileCoordinateList.add(fileCoordinate3);

    return fileCoordinateList;
  }

  private void newThirdPartyScan(String scanId, String scanRequestId, ThirdPartyFileCoordinate fileCoordinate) {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, fileCoordinate.getSource(), fileCoordinate.getFormat(),
        fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getHash(),
        fileCoordinate.getPackageUrl());
    tempEntity.newThirdPartyScan(scanRequestId, scanId, thirdPartyFile);
  }

  private void assertSbomComponentEmpty(
      ComponentIdentifier componentIdentifier,
      PackageUrlIdentifier packageUrlIdentifier,
      ThirdPartyFileCoordinate coordinate,
      List<SbomComponentDTO> results)
  {
    assertThat(results)
        .filteredOn(component -> component.getHash().equals(coordinate.getHash()))
        .isNotEmpty()
        .allSatisfy(component -> {
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier.getPackageUrl());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses()).isNullOrEmpty();
        });
  }

  @Test
  public void testGetSbomDependencyTypeSummaryForComponents() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h1", "u1", DIRECT);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h1", "u1", TRANSITIVE);

    SbomDependencyTypeDTO sbomDependencyTypeDTO = thirdPartyFileCoordinateDAO.getSbomDependencyTypeSummaryForComponents(
        app.getId(), sbomMetadata.getSbomVersion());
    assertThat(sbomDependencyTypeDTO.getDirect()).isEqualTo(1L);
    assertThat(sbomDependencyTypeDTO.getTransitive()).isEqualTo(1L);
    assertThat(sbomDependencyTypeDTO.getUnspecified()).isEqualTo(0L);
  }

  @Test
  public void testGetSbomVunerabilitySummaryForComponents_noCoordinateSecurity() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h1", "u1");

    BomPageSbomSummaryDTO result = thirdPartyFileCoordinateDAO.getSbomVunerabilitySummaryForComponents(
        app.getId(), sbomMetadata.getSbomVersion());

    assertThat(result.getNone()).isEqualTo(0L);
    assertThat(result.getLow()).isEqualTo(0L);
    assertThat(result.getHigh()).isEqualTo(0L);
    assertThat(result.getMedium()).isEqualTo(0L);
    assertThat(result.getCritical()).isEqualTo(0L);
  }

  @Test
  public void testGetNumberOfComponentsForSbom() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();

    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h1", "u1");
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h2", "u1");
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h3", "u1");
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h4", "u1");

    long result = thirdPartyFileCoordinateDAO.getNumberOfComponentsForSbom(app.getId(), sbomMetadata.getSbomVersion());

    assertThat(result).isEqualTo(4L);
  }

  @Test
  public void testGetSbomVunerabilitySummaryForComponents() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h1", "u1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
            "r1", sbomMetadata.getId(), "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
            "r2", sbomMetadata.getId(), "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r3", sbomMetadata.getId(), "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
        "r1", "s1", "j1", "r1", "d1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
        "r1", "s1", "j1", "r1", "d1");

    BomPageSbomSummaryDTO result = thirdPartyFileCoordinateDAO.getSbomVunerabilitySummaryForComponents(
        app.getId(), sbomMetadata.getSbomVersion());

    assertThat(result.getNone()).isEqualTo(0L);
    assertThat(result.getLow()).isEqualTo(1L);
    assertThat(result.getHigh()).isEqualTo(1L);
    assertThat(result.getMedium()).isEqualTo(1L);
    assertThat(result.getCritical()).isEqualTo(0L);
  }

  @Test
  public void testHasNonNullComponentRefs_True() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyFile.getId())
        .build();
    // no component ref
    tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "s", "SPDX", "n1", "v1", "h1", "pkg:npm/n1@v1");
    // has component ref
    tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "s", "SPDX", "n2", "v1", "h2", "pkg:npm/n2@v1", "cr1");
    assertThat(thirdPartyFileCoordinateDAO.hasNonNullComponentRefs(thirdPartyFile.getId())).isTrue();
  }

  @Test
  public void testHasNonNullComponentRefs_False() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyFile.getId())
        .build();
    // no component refs
    tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "s", "SPDX", "n1", "v1", "h1", "pkg:npm/n1@v1");
    tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "s", "SPDX", "n2", "v1", "h2", "pkg:npm/n2@v1");
    assertThat(thirdPartyFileCoordinateDAO.hasNonNullComponentRefs(thirdPartyFile.getId())).isFalse();
  }

  @Test
  public void testInsertWillFailWhenCpeExceedsMaxLength() {
    final int MAX_CPE_LENGTH = 1000;
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();

    ThirdPartyFileCoordinate entity =
        new ThirdPartyFileCoordinate("filehash2", "SBOM", "maven",
            "log4j-core", "2.14.1", scannedFile.getId());
    entity.setIdentificationSources("SBOM");
    entity.setPackageUrl("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1?extension=jar");
    // exceeding max length of 1000
    StringBuilder cpe = new StringBuilder(
        "cpe:2.3:a:@apideck/better-ajv-errors:@apideck/better-ajv-errors:0.3.6:*:*:*:*:*:*:*:*:*:*:*:*");
    while (cpe.length() < MAX_CPE_LENGTH) {
      cpe.append(":*");
    }
    entity.setCpe(cpe.toString());

    try {
      thirdPartyFileCoordinateDAO.insert(entity);
    }
    catch (DataAccessException sqlEx) {
      assertThat(sqlEx).isInstanceOf(DataAccessException.class);
      Throwable rootCause = Throwables.getRootCause(sqlEx);
      assertThat(rootCause).hasMessageContaining("Value too long " +
          "for column \"\"\"cpe\"\" VARCHAR(" + MAX_CPE_LENGTH);
    }
  }
}
