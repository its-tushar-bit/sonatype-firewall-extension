/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class DuplicateAwareThirdPartyFileCoordinatePersisterTest
    extends AbstractComponentTest
{
  @Inject
  private DuplicateAwareThirdPartyFileCoordinatePersister persister;

  @Inject
  private ThirdPartyFileCoordinateDAO dao;

  @Inject
  private ThirdPartyCoordinateLicenseDAO licenseDao;

  @Inject
  private ThirdPartyCoordinateSecurityDAO securityDao;

  @Inject
  private ThirdPartyVulnerabilityExploitabilityExchangeDAO vexDao;

  private final boolean save = true;

  private ThirdPartyFile thirdPartyFile;

  @Before
  public void before() {
    thirdPartyFile = tempEntity.newThirdPartyFile();
  }

  @Test
  public void testPersist_noDuplicates() {
    String componentRef = RandomStringUtils.insecure().nextAlphanumeric(20);
    ThirdPartyFileCoordinate fileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile().getId(), "source", "format", "version",
            "name", "hash", "pkg:format/name@version", componentRef);

    fileCoordinate.setFilenamesList(List.of("file1", "file2"));
    fileCoordinate.setOccurrencesList(List.of("occ1", "occ2"));

    ThirdPartyFileCoordinate persisted = persister.persist(fileCoordinate);

    assertThat(persisted.getId()).isNotNull();
    assertThat(persisted.getComponentRef()).isNotNull();
    assertThat(persisted.getOccurrencesList()).contains("occ1", "occ2");
    assertThat(persisted.getFilenamesList()).contains("file1", "file2");
  }

  @Test
  public void testPersist_MergeWithDuplicateHash() {
    String random = RandomStringUtils.insecure().nextAlphanumeric(19);
    newThirdPartyComponentWith(random, random + "1", "format", "name", "v1", "pkg:format/name/v1", "D", "similar",
        "SBOM", "file1", "occ1", save);
    ThirdPartyFileCoordinate toPersist =
        newThirdPartyComponentWith(random, random + "2", "format", "name", "v2", "pkg:format/name/v2", "D", "exact",
            "Sonatype", "file2", "occ2", !save);

    ThirdPartyFileCoordinate persisted = persister.persist(toPersist);
    assertPersisted(persisted, random, random + "1", "v1", "D", "dependency:/pkg:format\\name\\v2", "similar");
  }

  @Test
  public void testPersist_MergeWithDuplicateComponentRef() throws Exception {
    String random = RandomStringUtils.insecure().nextAlphanumeric(19);
    newThirdPartyComponentWith(random + "1", random, "format", "name", "v1", "pkg:format/name/v1", "D", "similar",
        "SBOM", "file1", "occ1", save);
    ThirdPartyFileCoordinate toPersist =
        newThirdPartyComponentWith(random + "2", random, "format", "name", "v2", "pkg:format/name/v2", "D", "exact",
            "Sonatype", "file2", "occ2", !save);

    ThirdPartyFileCoordinate persisted = persister.persist(toPersist);
    assertPersisted(persisted, random + "1", random, "v1", "D", "dependency:/pkg:format\\name\\v2", "similar");
  }

  @Test
  public void testPersist_MergeDuplicateHashAndComponentRef() throws Exception {
    String random = RandomStringUtils.insecure().nextAlphanumeric(19);
    newThirdPartyComponentWith(random, random, "format", "name", "v1", "pkg:format/name/v1", "D", "similar",
        "SBOM", "file1", "occ1", save);
    ThirdPartyFileCoordinate toPersist =
        newThirdPartyComponentWith(random, random, "format", "name", "v2", "pkg:format/name/v2", "D", "exact",
            "Sonatype", "file2", "occ2", !save);

    ThirdPartyFileCoordinate persisted = persister.persist(toPersist);
    assertPersisted(persisted, random, random, "v1", "D", "dependency:/pkg:format\\name\\v2", "similar");
  }

  @Test
  public void testPersist_overrideDuplicateHash() throws Exception {
    String random = RandomStringUtils.insecure().nextAlphanumeric(19);
    newThirdPartyComponentWith(random, random + "1", "format", "name", "v1", "pkg:format/name/v1", "T", "similar",
        "SBOM", "file1", "occ1", save);
    ThirdPartyFileCoordinate toPersist =
        newThirdPartyComponentWith(random, random + "2", "format", "name", "v2", "pkg:format/name/v2", "D", "exact",
            "Sonatype", "file2", "occ2", !save);

    ThirdPartyFileCoordinate persisted = persister.persist(toPersist);
    assertPersisted(persisted, random, random + "2", "v2", "D", "dependency:/pkg:format\\name\\v1", "exact");
  }

  @Test
  public void testPersist_overrideDuplicateComponentRef() throws Exception {
    String random = RandomStringUtils.insecure().nextAlphanumeric(19);
    newThirdPartyComponentWith(random, random, "format", "name", "v1", "pkg:format/name/v1", "T", "similar",
        "SBOM", "file1", "occ1", save);
    ThirdPartyFileCoordinate toPersist =
        newThirdPartyComponentWith(random + "2", random, "format", "name", "v2", "pkg:format/name/v2", "D", "exact",
            "Sonatype", "file2", "occ2", !save);

    ThirdPartyFileCoordinate persisted = persister.persist(toPersist);
    assertPersisted(persisted, random + "2", random, "v2", "D", "dependency:/pkg:format\\name\\v1", "exact");
  }

  @Test
  public void testPersist_overrideDuplicateHashAndComponentRef() throws Exception {
    String random = RandomStringUtils.insecure().nextAlphanumeric(19);
    newThirdPartyComponentWith(random, random, "format", "name", "v1", "pkg:format/name/v1", "T", "similar",
        "SBOM", "file1", "occ1", save);
    ThirdPartyFileCoordinate toPersist =
        newThirdPartyComponentWith(random, random, "format", "name", "v2", "pkg:format/name/v2", "D", "exact",
            "Sonatype", "file2", "occ2", !save);

    ThirdPartyFileCoordinate persisted = persister.persist(toPersist);
    assertPersisted(persisted, random, random, "v2", "D", "dependency:/pkg:format\\name\\v1", "exact");
  }

  @Test
  public void testPersist_missingHash() {
    String random = RandomStringUtils.insecure().nextAlphanumeric(19);
    ThirdPartyFileCoordinate toPersist =
        newThirdPartyComponentWith(null, random, "format", "name", "v2", "pkg:format/name/v2", "D", "exact",
            "Sonatype", "file2", "occ2", !save);
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> persister.persist(toPersist));
  }

  @Test
  public void testPersist_missingComponentRef() {
    String random = RandomStringUtils.insecure().nextAlphanumeric(19);
    ThirdPartyFileCoordinate toPersist =
        newThirdPartyComponentWith(random, null, "format", "name", "v2", "pkg:format/name/v2", "D", "exact",
            "Sonatype", "file2", "occ2", !save);
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> persister.persist(toPersist));
  }

  @Test
  public void testPersist_nullToPersist() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> persister.persist(null));
  }

  @Test
  public void testConsolidate_MergeComponentsVulnerabilitiesLicenses() {
    ThirdPartyFile tpf = tempEntity.newThirdPartyFile();
    Pair<ThirdPartyFileCoordinate, ThirdPartyFileCoordinate> fileCoordinatePair = setupComponentsForConsolidation(tpf);
    ThirdPartyFileCoordinate toKeep = fileCoordinatePair.getLeft();
    ThirdPartyFileCoordinate toSave = fileCoordinatePair.getRight();

    tempEntity.newThirdPartyCoordinateLicense(toKeep, "licenseId", "licenseName", "licenseUrl");
    tempEntity.newThirdPartyCoordinateLicense(toKeep, "toKeepLicenseId", "toKeepLicenseName", "licenseUrl");
    tempEntity.newThirdPartyCoordinateLicense(toSave, "licenseId", "licenseName", "licenseUrl");
    tempEntity.newThirdPartyCoordinateLicense(toSave, "toSaveLicenseId", "toSaveLicenseName", "licenseUrl");

    tempEntity.newThirdPartyCoordinateSecurity(toKeep, "vulnId", "Description",
        "link", 9.0, "Description", "fixed by");
    tempEntity.newThirdPartyCoordinateSecurity(toKeep, "toKeep", "Description",
        "link", 9.0, "Description", "fixed by");
    tempEntity.newThirdPartyCoordinateSecurity(toSave, "vulnId", "Description",
        "link", 9.0, "Description", "fixed by");
    tempEntity.newThirdPartyCoordinateSecurity(toKeep, "toSave", "Description",
        "link", 9.0, "Description", "fixed by");

    Optional<String> persistedComponentRef =
        persister.consolidate(List.of(toKeep.getComponentRef(), toSave.getComponentRef()), tpf.getId());

    ThirdPartyFileCoordinate component = dao.getByComponentRef(persistedComponentRef.get(), tpf.getId());
    List<ThirdPartyCoordinateSecurity> vulns = securityDao.getByFileCoordinateId(component.getId());
    List<ThirdPartyCoordinateLicense> licenses = licenseDao.getByFileCoordinateId(component.getId());

    Set<String> refIds = vulns.stream().map(ThirdPartyCoordinateSecurity::getRefId).collect(Collectors.toSet());
    Set<String> licenseIds =
        licenses.stream().map(ThirdPartyCoordinateLicense::getLicenseId).collect(Collectors.toSet());

    assertPersisted(component, "hash", "a1a1a1a1a1a1a1a1a1a1", "v1", "D",
        "dependency:/pkg:format2\\name2\\v2", "exact");
    assertThat(vulns).hasSize(3);
    assertThat(licenses).hasSize(3);
    assertThat(refIds).hasSameElementsAs(Set.of("vulnId", "toKeep", "toSave"));
    assertThat(licenseIds).hasSameElementsAs(Set.of("licenseId", "toKeepLicenseId", "toSaveLicenseId"));
  }

  @Test
  public void testConsolidate_MergeVex() {
    ThirdPartyFile tpf = tempEntity.newThirdPartyFile();
    Pair<ThirdPartyFileCoordinate, ThirdPartyFileCoordinate> fileCoordinatePair = setupComponentsForConsolidation(tpf);
    ThirdPartyFileCoordinate toKeep = fileCoordinatePair.getLeft();
    ThirdPartyFileCoordinate toSave = fileCoordinatePair.getRight();
    ThirdPartyCoordinateSecurity toKeepCoordinateSecurity =
        tempEntity.newThirdPartyCoordinateSecurity(toKeep, "vulnId", "Description", "link",
            9.0, "Description", "fixed by");
    ThirdPartyCoordinateSecurity toKeepCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(toKeep, "vulnId2", "Description2", "link2",
            9.0, "Description2", "fixed b2y");
    tempEntity.newThirdPartyCoordinateSecurity(toKeep, "toKeep", "Description",
        "link", 9.0, "Description", "fixed by");
    ThirdPartyCoordinateSecurity toMergeCoordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity(toSave,
        "vulnId", "Description",
        "link", 9.0, "Description", "fixed by");
    ThirdPartyCoordinateSecurity toMergeCoordinateSecurity2 = tempEntity.newThirdPartyCoordinateSecurity(toSave,
        "vulnId2", "Description3",
        "link3", 9.0, "Description3", "fixed by3");
    tempEntity.newThirdPartyCoordinateSecurity(toKeep, "toSave", "Description",
        "link", 9.0, "Description", "fixed by");

    ThirdPartyVulnerabilityExploitabilityExchange toKeepVex =
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(toKeepCoordinateSecurity,
            toKeepCoordinateSecurity.getRefId(), "State", "resp", "detail", "detail");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(toMergeCoordinateSecurity,
        toMergeCoordinateSecurity.getRefId(), "State2", "resp2", "detail2", "detail2");
    ThirdPartyVulnerabilityExploitabilityExchange toKeepVex2 =
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(toMergeCoordinateSecurity2,
            toMergeCoordinateSecurity2.getRefId(), "State3", "resp3", "detail3", "detail3");

    Optional<String> persistedComponentRef =
        persister.consolidate(List.of(toKeep.getComponentRef(), toSave.getComponentRef()), tpf.getId());
    ThirdPartyFileCoordinate component = dao.getByComponentRef(persistedComponentRef.get(), tpf.getId());
    List<ThirdPartyCoordinateSecurity> vulns = securityDao.getByFileCoordinateId(component.getId());
    Set<String> refIds = vulns.stream().map(ThirdPartyCoordinateSecurity::getRefId).collect(Collectors.toSet());
    ThirdPartyVulnerabilityExploitabilityExchange actualVex =
        vexDao.getByCoordinateSecurityIdAndRefId(toKeepCoordinateSecurity.getId(), "vulnId");
    ThirdPartyVulnerabilityExploitabilityExchange actualVex2 =
        vexDao.getByCoordinateSecurityIdAndRefId(toKeepCoordinateSecurity2.getId(), "vulnId2");
    assertPersisted(component, "hash", "a1a1a1a1a1a1a1a1a1a1", "v1", "D",
        "dependency:/pkg:format2\\name2\\v2", "exact");
    assertThat(vulns).hasSize(4);
    assertThat(refIds).hasSameElementsAs(Set.of("vulnId", "vulnId2", "toKeep", "toSave"));
    assertThat(actualVex.getId()).isEqualTo(toKeepVex.getId());
    assertThat(actualVex2.getId()).isEqualTo(toKeepVex2.getId());
  }

  private void assertPersisted(
      final ThirdPartyFileCoordinate persisted,
      final String hash,
      final String componentRef,
      final String version,
      final String dependencyType,
      final String dependencyOccurrence,
      final String matchState)
  {
    assertThat(persisted.getId()).isNotNull();
    assertThat(persisted.getHash()).isEqualTo(hash);
    assertThat(persisted.getComponentRef()).isEqualTo(componentRef);
    assertThat(persisted.getFormat()).isEqualTo("format");
    assertThat(persisted.getName()).isEqualTo("name");
    assertThat(persisted.getVersion()).isEqualTo(version);
    assertThat(persisted.getDependencyType()).isEqualTo(dependencyType); // updated
    assertThat(persisted.getMatchStateId()).isEqualTo(matchState); // updated
    assertThat(persisted.getIdentificationSourcesAsSet()).contains("SBOM", "Sonatype"); // updated
    assertThat(persisted.getOccurrencesList()).containsExactlyInAnyOrder("occ1", "occ2",
        dependencyOccurrence);
    assertThat(persisted.getFilenamesList()).contains("file1", "file2");
  }

  private ThirdPartyFileCoordinate newThirdPartyComponentWith(
      final String hash,
      final String componentRef,
      final String format,
      final String name,
      final String version,
      final String packageUrl,
      final String dependencyType,
      final String matchState,
      final String identificationSources,
      final String filenames,
      final String occurrences,
      final boolean save)
  {
    ThirdPartyFileCoordinate coordinate = new ThirdPartyFileCoordinate();
    coordinate.setThirdPartyFileId(thirdPartyFile.getId());
    coordinate.setHash(hash);
    coordinate.setComponentRef(componentRef);
    coordinate.setSource("SBOM");
    coordinate.setFormat(format);
    coordinate.setName(name);
    coordinate.setVersion(version);
    coordinate.setPackageUrl(packageUrl);
    coordinate.setDependencyType(dependencyType);
    coordinate.setMatchStateId(matchState);
    coordinate.setIdentificationSources(identificationSources);
    coordinate.setFilenames(filenames);
    coordinate.setOccurrencesList(List.of(StringUtils.split(occurrences, ",")));
    if (save) {
      dao.insert(coordinate);
    }
    return coordinate;
  }

  private Pair<ThirdPartyFileCoordinate, ThirdPartyFileCoordinate> setupComponentsForConsolidation(ThirdPartyFile tpf) {
    ThirdPartyFileCoordinate toKeep =
        tempEntity.newThirdPartyFileCoordinate(tpf, "source", "format", "name", "v1", "hash", "pkg:format/name/v1",
            "a1a1a1a1a1a1a1a1a1a1");
    ThirdPartyFileCoordinate toSave =
        tempEntity.newThirdPartyFileCoordinate(tpf, "source2", "format2", "name2", "v2", "hash2",
            "pkg:format2/name2/v2", "b1b1b1b1b1b1b1b1b1b1");
    toKeep.setDependencyType("D");
    toKeep.setMatchStateId("exact");
    toKeep.setOccurrencesList(List.of("occ1"));
    toKeep.setFilenames("file1");
    toSave.setIdentificationSources("Sonatype");
    toSave.setOccurrencesList(List.of("occ2"));
    toSave.setFilenames("file2");
    dao.update(toKeep);
    dao.update(toSave);
    return Pair.of(toKeep, toSave);
  }
}
