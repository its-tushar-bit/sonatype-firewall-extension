/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DuplicateAwareThirdPartyFileCoordinatePersisterTest
    extends AbstractComponentTest
{
  @Inject
  private DuplicateAwareThirdPartyFileCoordinatePersister persister;

  @Inject
  private ThirdPartyFileCoordinateDAO dao;

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

  private void assertPersisted(
      final ThirdPartyFileCoordinate persisted, final String hash, final String componentRef,
      final String version, final String dependencyType, final String dependencyOccurrence, final String matchState)
  {
    assertThat(persisted.getId()).isNotNull();
    assertThat(persisted.getHash()).isEqualTo(hash);
    assertThat(persisted.getComponentRef()).isEqualTo(componentRef);
    assertThat(persisted.getFormat()).isEqualTo("format");
    assertThat(persisted.getName()).isEqualTo("name");
    assertThat(persisted.getVersion()).isEqualTo(version);
    assertThat(persisted.getDependencyType()).isEqualTo(dependencyType); //updated
    assertThat(persisted.getMatchStateId()).isEqualTo(matchState); //updated
    assertThat(persisted.getIdentificationSourcesAsSet()).contains("SBOM", "Sonatype"); //updated
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
}
