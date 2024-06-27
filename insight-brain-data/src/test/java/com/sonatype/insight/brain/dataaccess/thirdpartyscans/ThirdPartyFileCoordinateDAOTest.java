/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.BomPageSbomSummaryDTO;
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
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.DIRECT;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.TRANSITIVE;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.UNSPECIFIED;
import static org.assertj.core.api.Assertions.assertThat;

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
  public void testGetBySourceFormatNameVersionAndScannedFileId() {
    List<ThirdPartyFileCoordinate> retrievedCoordinateFile = thirdPartyFileCoordinateDAO
        .getBySourceFormatNameVersionAndThirdPartyFileId(fileCoordinate.getSource(), fileCoordinate.getFormat(),
            fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getThirdPartyFileId());

    assertThirdPartyCoordinateFile(fileCoordinate.getHash(), fileCoordinate.getSource(), fileCoordinate.getFormat(),
        fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getThirdPartyFileId(),
        retrievedCoordinateFile.get(0));
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

    ThirdPartyFileCoordinate result = thirdPartyFileCoordinateDAO.getByPackageUrlAndScanId(
        fileCoordinate.getPackageUrl(), thirdPartyScan.getScanId());

    assertThat(result).isNotNull();
    assertThirdPartyCoordinateFile(fileCoordinate.getHash(), fileCoordinate.getSource(), fileCoordinate.getFormat(),
        fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getThirdPartyFileId(), result);
  }

  @Test
  public void testGetByHashAndScanId() {
    String scanId = TemporaryEntity.uuid();
    String hash = tempEntity.newRandomHash();
    List<ThirdPartyFileCoordinate> fileCoordinateList = createThirdPartyScans(scanId, hash);
    List<ThirdPartyFileCoordinate> results =
        thirdPartyFileCoordinateDAO.getByHashAndScanId(hash, scanId);

    assertThat(results).hasSize(2);

    Comparator<ThirdPartyFileCoordinate> thirdPartySecurityComparator =
        Comparator.comparing(ThirdPartyFileCoordinate::getHash);

    assertThat(results).usingElementComparator(thirdPartySecurityComparator)
        .containsAnyElementsOf(fileCoordinateList);
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
    String scanId = TemporaryEntity.uuid();
    String hash = tempEntity.newRandomHash();
    createThirdPartyScans(scanId, hash);

    List<ThirdPartyFileCoordinate> fileCoordinates = thirdPartyFileCoordinateDAO.getByScanId(scanId);

    assertThat(fileCoordinates).hasSize(3);
    assertThat(fileCoordinates.stream().map(ThirdPartyFileCoordinate::getSource))
        .containsExactlyInAnyOrder("s1", "s2", "s3");
  }

  @Test
  public void testGetBySbomMetadataIdAndComponentHash() {
    Organization organization1 = tempEntity.newOrganization("org1");
    Application application = tempEntity.newApplication(organization1.getId());
    final ThirdPartyFile thirdPartyFileA = tempEntity.newThirdPartyFile();
    final ThirdPartySbomMetadata thirdPartySbomMetadataA =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFileA.getId(), application.getId(), "ACTIVE",
            thirdPartyFileA.getFilename());
    final ThirdPartyFileCoordinate coordinateA =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFileA, "s1", "f1", "n1", "v1");

    final ThirdPartyFile thirdPartyFileB = tempEntity.newThirdPartyFile();
    final ThirdPartySbomMetadata thirdPartySbomMetadataB =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFileB.getId(), application.getId(), "ACTIVE",
            thirdPartyFileB.getFilename());
    final ThirdPartyFileCoordinate coordinateB =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFileB, "s2", "f2", "n2", "v2");

    final ThirdPartyFile thirdPartyFileC = tempEntity.newThirdPartyFile();
    final ThirdPartySbomMetadata thirdPartySbomMetadataC =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFileC.getId(), application.getId(), "ACTIVE",
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
  @PostgresTest
  public void testGetSbomApplicationVulnerabilities() {
    Organization organization1 = tempEntity.newOrganization("org1");
    Application application = tempEntity.newApplication(organization1.getId());

    ThirdPartyFile file1 = tempEntity.newThirdPartyFile("CycloneDX-bom.xml");
    ThirdPartyFile file2 = tempEntity.newThirdPartyFile("SPDX-spdx.json");
    ThirdPartyFile file3 = tempEntity.newThirdPartyFile("file.json");

    ThirdPartySbomMetadata sbom1 =
        tempEntity.newThirdPartySbomMetadata(file1.getId(), application.getId(), "ACTIVE", file1.getFilename());
    ThirdPartySbomMetadata sbom2 =
        tempEntity.newThirdPartySbomMetadata(file2.getId(), application.getId(), "ACTIVE", file2.getFilename());
    tempEntity.newThirdPartySbomMetadata(file3.getId(), application.getId(), "PENDING", file3.getFilename());

    ThirdPartyFileCoordinate c1 = tempEntity.newThirdPartyFileCoordinate(file1, "s1", "f1", "n1", "v1");
    ThirdPartyFileCoordinate c2 = tempEntity.newThirdPartyFileCoordinate(file2, "s2", "f2", "n2", "v2");
    ThirdPartyFileCoordinate c3 = tempEntity.newThirdPartyFileCoordinate(file3, "s3", "f3", "n3", "v3");

    tempEntity.newThirdPartyCoordinateSecurity(c1, "r1", "d1", "l1", 3.5F, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(c1, "r2", "d2", "l2", 7.5F, "sd2", "f2");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r3", "d3", "l3", 1.5F, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r4", "d4", "l4", 0.5F, "sd4", "f4");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r5", "d5", "l5", 4.7F, "sd5", "f5");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r6", "d6", "l6", 0F, "sd6", "f6");
    tempEntity.newThirdPartyCoordinateSecurity(c3, "r7", "d7", "l7", 1F, "sd7", "f7");

    ThirdPartySbomMetadataSummaryListDTO result =
        thirdPartyFileCoordinateDAO.getSbomApplicationVulnerabilities(application.getId(), "asc", 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    List<ThirdPartySbomMetadataSummaryDTO> results = result.getResults();
    Collections.sort(results, Comparator.comparingInt(ThirdPartySbomMetadataSummaryDTO::getHigh));

    assertThat(results).hasSize(2);
    assertThat(results.get(0).getLow()).isEqualTo(2);
    assertThat(results.get(0).getMedium()).isEqualTo(1);
    assertThat(results.get(0).getNone()).isEqualTo(1);
    assertThat(results.get(1).getLow()).isEqualTo(1);
    assertThat(results.get(1).getHigh()).isEqualTo(1);

    // test pagination
    result = thirdPartyFileCoordinateDAO.getSbomApplicationVulnerabilities(application.getId(), "desc", 1, 1);
    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    results = result.getResults();
    assertThat(results).hasSize(1);

    ThirdPartySbomMetadataSummaryDTO dto = results.get(0);
    assertThat(dto.getApplicationVersion()).isEqualTo(sbom2.getSbomVersion());
    assertThat(dto.getImportDate().toInstant()).isEqualTo(sbom2.getCreatedAt().toInstant());
    assertThat(dto.getSpec()).isEqualTo(sbom2.getSpec());
    assertThat(dto.getSpecVersion()).isEqualTo(sbom2.getSpecVersion());
    assertThat(dto.getLow()).isEqualTo(2);
    assertThat(dto.getMedium()).isEqualTo(1);
    assertThat(dto.getNone()).isEqualTo(1);

    result = thirdPartyFileCoordinateDAO.getSbomApplicationVulnerabilities(application.getId(), "desc", 1, 2);
    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    results = result.getResults();
    assertThat(results).hasSize(1);

    dto = results.get(0);
    assertThat(dto.getApplicationVersion()).isEqualTo(sbom1.getSbomVersion());
    assertThat(dto.getImportDate().toInstant()).isEqualTo(sbom1.getCreatedAt().toInstant());
    assertThat(dto.getSpec()).isEqualTo(sbom1.getSpec());
    assertThat(dto.getSpecVersion()).isEqualTo(sbom1.getSpecVersion());
    assertThat(dto.getLow()).isEqualTo(1);
    assertThat(dto.getHigh()).isEqualTo(1);
  }

  @Test
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
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier2).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses())
              .extracting(License::getLicenseId)
              .containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(License::getLicenseName)
              .containsExactlyInAnyOrder("License 1", "License 2");
        });
  }

  @Test
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
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentsWithVulnerabilitiesAndLicenses() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl());

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
        null,true, 2, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(2);
    assertThat(dtos.get(1).getPercentageAnnotated()).isEqualTo(16.7);
    assertThat(dtos)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertThat(dtos).filteredOn(component -> component.getHash().equals(coordinate1.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier1.getPackageUrl());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier1).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isOne();
          assertThat(component.getLicenses()).isNullOrEmpty();
        });

    assertThat(dtos).filteredOn(component -> component.getHash().equals(coordinate2.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier2.getPackageUrl());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier2).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isOne();
          assertThat(component.getVulnerabilitySeverityLowCount()).isOne();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isOne();
          assertThat(component.getVulnerabilitySeverityHighCount()).isEqualTo(2);
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isOne();
          assertThat(component.getLicenses())
              .extracting(License::getLicenseId)
              .containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(License::getLicenseName)
              .containsExactlyInAnyOrder("License 1", "License 2");
        });
  }

  @Test
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
        sbomMetadata.getThirdPartyFileId(), null, null, null,  SbomComponentSortableField.VULNERABILITIES,
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
    assertThat(dtos.get(1).getName()).isEqualTo("p3");
    assertThat(dtos.get(1).getPercentageAnnotated()).isEqualTo(66.7);
    assertThat(dtos.get(2).getName()).isEqualTo("p2");
    assertThat(dtos.get(2).getPercentageAnnotated()).isEqualTo(50.0);
    // component p5 goes before p4 even when both percentages are 0 as the name gets sorted first when descending
    assertThat(dtos.get(3).getName()).isEqualTo("p5");
    assertThat(dtos.get(3).getPercentageAnnotated()).isEqualTo(0);
    assertThat(dtos.get(4).getName()).isEqualTo("p4");
    assertThat(dtos.get(4).getPercentageAnnotated()).isEqualTo(0);

    // Percentages in ascending order
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null,
        null, null,  SbomComponentSortableField.PERCENTAGE_ANNOTATED, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(5);

    dtos = result.getResults();

    assertThat(dtos).hasSize(5);
    // component p4 goes before p5 even when both percentages are 0 as the name gets sorted first when ascending
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

    //testing name
    SbomComponentListDTO result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "slf4j-log4j",
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    //testing version
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "2.4",
        null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();
    assertThat(result.getResults())
        .extracting(SbomComponentDTO::getPackageUrl)
        .containsExactly(packageUrlIdentifier3.getPackageUrl());

    //testing packageUrl - not part of namespace, name or version
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

    //testing no componentName filter
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId(), null,
        null, null, null,
        true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(5);

    // test excluding format name from search
    result = thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(
        sbomMetadata.getThirdPartyFileId(), null, null, "npm", null, true, 5, 1);
    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isZero();
    assertThat(result.getResults()).isEmpty();
  }

  private void insertVEXToThirdPartyCoordinateSecurity(ThirdPartyCoordinateSecurity coordinateSecurity) {
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
        "state", "justification", "response", "detail");
  }

  private ThirdPartyCoordinateSecurity insertThirdPartyCoordinateSecurity(
      ThirdPartyFileCoordinate coordinate, CvssV3Severity severity,
      float range, String refId)
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
      final String thirdPartyFileId, final ThirdPartyFileCoordinate entity)
  {
    assertThat(entity.getHash()).isEqualTo(hash);
    assertThat(entity.getSource()).isEqualTo(source);
    assertThat(entity.getName()).isEqualTo(name);
    assertThat(entity.getFormat()).isEqualTo(format);
    assertThat(entity.getVersion()).isEqualTo(version);
    assertThat(entity.getThirdPartyFileId()).isEqualTo(thirdPartyFileId);
    assertThat(entity.getThirdPartyFileId()).isEqualTo(thirdPartyFileId);
  }

  private List<ThirdPartyFileCoordinate> createThirdPartyScans(String scanId, String hash) {
    List<ThirdPartyFileCoordinate> fileCoordinateList = new ArrayList<>();

    String scanRequestId = TemporaryEntity.uuid();

    ThirdPartyFileCoordinate fileCoordinate1 = new ThirdPartyFileCoordinate(hash, "s1", "f1", "n1", "v1", null);
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

    assertThat(result.getAnnotatedPercentage()).isEqualTo(null);
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
            "r1", "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
            "r2", "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
            "r3", "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
        "r1", "s1", "j1", "r1", "d1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
        "r1", "s1", "j1", "r1", "d1");

    BomPageSbomSummaryDTO result = thirdPartyFileCoordinateDAO.getSbomVunerabilitySummaryForComponents(
        app.getId(), sbomMetadata.getSbomVersion());

    assertThat(result.getAnnotatedPercentage()).isEqualTo(66.7);
    assertThat(result.getNone()).isEqualTo(0L);
    assertThat(result.getLow()).isEqualTo(1L);
    assertThat(result.getHigh()).isEqualTo(1L);
    assertThat(result.getMedium()).isEqualTo(1L);
    assertThat(result.getCritical()).isEqualTo(0L);
  }
}
