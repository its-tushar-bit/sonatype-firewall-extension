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
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyFileCoordinateDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private ThirdPartyFileCoordinate fileCoordinate;

  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    thirdPartyCoordinateSecurityDAO = daoFactory.createThirdPartyCoordinateSecurityDAO();
    thirdPartyFileCoordinateDAO = daoFactory.createThirdPartyFileCoordinateDAO();
    thirdPartyCoordinateLicenseDAO = daoFactory.createThirdPartyCoordinateLicenseDAO();
    fileCoordinate = tempEntity.newThirdPartyFileCoordinate();
  }

  @Test
  public void testCRUD() {
    // Create
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();

    ThirdPartyFileCoordinate entity =
        new ThirdPartyFileCoordinate("filehash2", "source", "format",
            "name2", "version2", scannedFile.getId());
    thirdPartyFileCoordinateDAO.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    ThirdPartyFileCoordinate retrievedCoordinateFile = thirdPartyFileCoordinateDAO.getById(entity.getId());
    assertThirdPartyCoordinateFile("filehash2", "source", "format", "name2", "version2", scannedFile.getId(), entity);

    // Update
    retrievedCoordinateFile.setName("UpdatedName");
    thirdPartyFileCoordinateDAO.update(retrievedCoordinateFile);
    ThirdPartyFileCoordinate updated = thirdPartyFileCoordinateDAO.getById(retrievedCoordinateFile.getId());
    assertThat(updated.getName()).isEqualTo("UpdatedName");

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
                "CVSSv3", "<dd>r1</dd>", "<dd>a1</dd>");
    final ThirdPartyCoordinateSecurity sec2 = tempEntity
        .newThirdPartyCoordinateSecurity(coord1, "r2", "d2", "l2", 1.2f, "2.2", "CVE", "v:2", "Low", "<dd>c2</>",
            "CVSSv2", "<dd>r2</dd>", "<dd>a2</dd>");

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
            "<dd>c1</>", "CVSSv3", "<dd>r1</dd>", "<dd>a1</dd>");
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

    List<SbomComponentDTO> results =
        thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId());

    assertThat(results).isEmpty();
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

    List<SbomComponentDTO> results =
        thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId());

    assertThat(results).isNotEmpty();
    assertThat(results)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertSbomComponentEmpty(componentIdentifier1, packageUrlIdentifier1, coordinate1, results);
    assertSbomComponentEmpty(componentIdentifier2, packageUrlIdentifier2, coordinate2, results);
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

    List<SbomComponentDTO> results =
        thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId());

    assertThat(results).isNotEmpty();
    assertThat(results)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertSbomComponentEmpty(componentIdentifier1, packageUrlIdentifier1, coordinate1, results);

    assertThat(results)
        .filteredOn(component -> component.getHash().equals(coordinate2.getHash()))
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
              .extracting(License::getLicenseId).containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(License::getLicenseName).containsExactlyInAnyOrder("License 1", "License 2");
        });
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
    tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-1", "description1", "link1",
        CvssV3Severity.CRITICAL.getEndScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "fix1");

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-2", "description2", "link2",
        CvssV3Severity.NONE.getStartScoreRange(), CvssV3Severity.NONE.getDisplayName(), "fix2");
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
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license-1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-2", "License 2", "http://license-2");

    List<SbomComponentDTO> results =
        thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(sbomMetadata.getThirdPartyFileId());

    assertThat(results).isNotEmpty();
    assertThat(results)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertThat(results)
        .filteredOn(component -> component.getHash().equals(coordinate1.getHash()))
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

    assertThat(results)
        .filteredOn(component -> component.getHash().equals(coordinate2.getHash()))
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
              .extracting(License::getLicenseId).containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(License::getLicenseName).containsExactlyInAnyOrder("License 1", "License 2");
        });
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
}
