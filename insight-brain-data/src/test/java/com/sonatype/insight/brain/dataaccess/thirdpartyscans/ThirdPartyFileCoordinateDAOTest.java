/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import com.google.common.base.Throwables;
import org.jooq.exception.DataAccessException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.thirdpartyscans.BomPageSbomSummaryDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomDependencyTypeDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.uuid;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.DIRECT;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.TRANSITIVE;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyFileCoordinateDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private ThirdPartyFileCoordinate fileCoordinate;

  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private ThirdPartyFileDAO thirdPartyFileDAO;

  private ThirdPartyVulnerabilityExploitabilityExchangeDAO vexDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    thirdPartyCoordinateSecurityDAO = daoFactory.createThirdPartyCoordinateSecurityDAO();
    thirdPartyFileCoordinateDAO = daoFactory.createThirdPartyFileCoordinateDAO();
    thirdPartyCoordinateLicenseDAO = daoFactory.createThirdPartyCoordinateLicenseDAO();
    fileCoordinate = tempEntity.newThirdPartyFileCoordinate();
    thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
    vexDAO = daoFactory.createThirdPartyVulnerabilityExploitabilityExchangeDAO();
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

    final ThirdPartyVulnerabilityExploitabilityExchange vex1 = tempEntity
        .newThirdPartyVulnerabilityExploitabilityExchange(sec1, "r1", "state", "just", "resp", "detail");

    try (TransactionContext tx = thirdPartyFileCoordinateDAO.createTransactionContext()) {
      tx.begin();
      thirdPartyFileCoordinateDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());
      tx.commit();
    }

    assertThat(thirdPartyFileCoordinateDAO.getById(coord1.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(coord2.getId())).isNull();
    assertThat(thirdPartyCoordinateSecurityDAO.getById(sec1.getId())).isNull();
    assertThat(thirdPartyCoordinateSecurityDAO.getById(sec2.getId())).isNull();
    assertThat(vexDAO.getById(vex1.getId())).isNull();
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
    final ThirdPartyVulnerabilityExploitabilityExchange vex = tempEntity
        .newThirdPartyVulnerabilityExploitabilityExchange(sec, "r1", "state", "just", "resp", "detail");

    thirdPartyFileCoordinateDAO.delete(coordinate);

    assertThat(thirdPartyFileCoordinateDAO.getById(coordinate.getId())).isNull();
    assertThat(thirdPartyCoordinateSecurityDAO.getById(sec.getId())).isNull();
    assertThat(thirdPartyCoordinateLicenseDAO.getById(lic.getId())).isNull();
    assertThat(vexDAO.getById(vex.getId())).isNull();
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

  @Test
  public void testInsertBatch_generatesIdsAndDisplayNames() {
    final ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyFileCoordinate a = new ThirdPartyFileCoordinate();
    a.setThirdPartyFileId(thirdPartyFile.getId());
    a.setHash("h-a");
    a.setSource("SBOM");
    a.setFormat("maven");
    a.setName("group-a:artifact-a");
    a.setVersion("1.0");
    a.setPackageUrl("pkg:maven/group-a/artifact-a@1.0");
    ThirdPartyFileCoordinate b = new ThirdPartyFileCoordinate();
    b.setThirdPartyFileId(thirdPartyFile.getId());
    b.setHash("h-b");
    b.setSource("SBOM");
    b.setFormat("maven");
    b.setName("group-b:artifact-b");
    b.setVersion("2.0");
    b.setPackageUrl("pkg:maven/group-b/artifact-b@2.0");

    try (TransactionContext tx = thirdPartyFileCoordinateDAO.createTransactionContext()) {
      tx.begin();
      thirdPartyFileCoordinateDAO.insertBatch(tx, List.of(a, b));
      tx.commit();
    }

    assertThat(a.getId()).isNotNull();
    assertThat(a.getDisplayName()).isNotBlank();
    assertThat(b.getId()).isNotNull();
    assertThat(b.getDisplayName()).isNotBlank();

    ThirdPartyFileCoordinate retrievedA = thirdPartyFileCoordinateDAO.getById(a.getId());
    ThirdPartyFileCoordinate retrievedB = thirdPartyFileCoordinateDAO.getById(b.getId());
    assertThat(retrievedA).isNotNull();
    assertThat(retrievedA.getHash()).isEqualTo("h-a");
    assertThat(retrievedA.getDisplayName()).isEqualTo(a.getDisplayName());
    assertThat(retrievedB).isNotNull();
    assertThat(retrievedB.getHash()).isEqualTo("h-b");
    assertThat(retrievedB.getDisplayName()).isEqualTo(b.getDisplayName());
  }

  @Test
  public void testInsertBatch_normalizesBlankOccurrencesAndFilenamesColumnsToNull() {
    final ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyFileCoordinate empty = new ThirdPartyFileCoordinate();
    empty.setThirdPartyFileId(thirdPartyFile.getId());
    empty.setHash("h-empty");
    empty.setSource("SBOM");
    empty.setFormat("maven");
    empty.setName("org.empty:mod");
    empty.setVersion("1.0");
    empty.setFilenames("");

    ThirdPartyFileCoordinate whitespace = new ThirdPartyFileCoordinate();
    whitespace.setThirdPartyFileId(thirdPartyFile.getId());
    whitespace.setHash("h-ws");
    whitespace.setSource("SBOM");
    whitespace.setFormat("maven");
    whitespace.setName("org.ws:mod");
    whitespace.setVersion("1.0");
    whitespace.setFilenames("\t ");
    whitespace.setOccurrencesList(List.of(" "));

    try (TransactionContext tx = thirdPartyFileCoordinateDAO.createTransactionContext()) {
      tx.begin();
      thirdPartyFileCoordinateDAO.insertBatch(tx, List.of(empty, whitespace));
      tx.commit();

      // Query raw columns to bypass the entity's read-side isBlank filter.
      String emptyOccurrences = tx.dsl()
          .select(
              com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE.OCCURRENCES)
          .from(com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE)
          .where(
              com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE.FILE_COORDINATE_ID
                  .eq(empty.getId()))
          .fetchOne(0, String.class);
      String emptyFilenames = tx.dsl()
          .select(
              com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE.FILENAMES)
          .from(com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE)
          .where(
              com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE.FILE_COORDINATE_ID
                  .eq(empty.getId()))
          .fetchOne(0, String.class);
      String wsOccurrences = tx.dsl()
          .select(
              com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE.OCCURRENCES)
          .from(com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE)
          .where(
              com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE.FILE_COORDINATE_ID
                  .eq(whitespace.getId()))
          .fetchOne(0, String.class);
      String wsFilenames = tx.dsl()
          .select(
              com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE.FILENAMES)
          .from(com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE)
          .where(
              com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE.FILE_COORDINATE_ID
                  .eq(whitespace.getId()))
          .fetchOne(0, String.class);

      assertThat(emptyOccurrences).isNull();
      assertThat(emptyFilenames).isNull();
      assertThat(wsOccurrences).isNull();
      assertThat(wsFilenames).isNull();
    }
  }
}
