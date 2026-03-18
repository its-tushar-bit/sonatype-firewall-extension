/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.service.InsightWork;

import static org.assertj.core.api.Assertions.assertThat;

@Named
public class ExistingFilesHelper
{
  @Inject
  private InsightWork insightWork;

  /**
   * Assert that only the specified files and their parent directories exist within the sonatype-work/clm-server/sboms
   * directory
   */
  public void assertExistingSbomFiles(String... filenames) throws IOException {
    Set<Path> alwaysExpectedDirs = Stream.of(
        insightWork.getSbomDir(),
        insightWork.getSbomTempDir(),
        insightWork.getSbomTransientDir(),
        insightWork.getSbomPersistentTempDir())
        .map(File::toPath)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toSet());

    Path sbomDir = insightWork.getSbomDir().toPath().toAbsolutePath();
    Set<Path> expectedFiles = Arrays.stream(filenames)
        .map(f -> sbomDir.resolve(f))
        .collect(Collectors.toSet());

    Set<Path> expectedDirs = Stream.concat(
        alwaysExpectedDirs.stream(),
        expectedFiles.stream()
            .flatMap(f -> Stream.iterate(f.getParent(), Predicate.not(alwaysExpectedDirs::contains), Path::getParent)))
        .collect(Collectors.toSet());

    Map<Boolean, List<Path>> existingFilesAndDirs;
    try (Stream<Path> walkedFile = Files.walk(sbomDir)) {
      existingFilesAndDirs = walkedFile.collect(Collectors.partitioningBy(Files::isRegularFile));
    }

    assertThat(existingFilesAndDirs.get(true)).containsExactlyInAnyOrderElementsOf(expectedFiles);
    assertThat(existingFilesAndDirs.get(false)).containsExactlyInAnyOrderElementsOf(expectedDirs);
  }
}
