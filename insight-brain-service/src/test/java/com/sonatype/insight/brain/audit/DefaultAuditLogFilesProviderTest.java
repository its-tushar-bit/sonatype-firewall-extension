/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultAuditLogFilesProviderTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private DefaultAuditLogFilesProvider defaultAuditLogFilesProvider;

  private InsightConfig config;

  private Path logDir;

  @Before
  public void setup() throws Exception {
    logDir = tempDir.newFolder("logs").toPath();

    copyTestResource("audit-2024-02-07.log.gz");
    copyTestResource("audit-2024-02-08.log.gz");
    copyTestResource("audit.log");

    config = new InsightConfig();
    config.setSonatypeWork(tempDir.getRoot().getAbsolutePath());

    defaultAuditLogFilesProvider = new DefaultAuditLogFilesProvider(config);
  }

  @Test
  public void testGetAuditLogFiles_NoFilesForTheRange() {
    List<File> result =
        defaultAuditLogFilesProvider.getAuditLogFiles(LocalDate.of(2024, 2, 4),
            LocalDate.of(2024, 2, 4));

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetAuditLogFiles_WhenTheRangeIsToday() {
    List<File> result =
        defaultAuditLogFilesProvider.getAuditLogFiles(LocalDate.now(), LocalDate.now());

    assertThat(getFileNames(result)).containsExactly("audit.log");
  }

  @Test
  public void testGetAuditLogFiles_ThereAreFilesForTheRange() {
    List<File> result =
        defaultAuditLogFilesProvider.getAuditLogFiles(LocalDate.of(2024, 2, 4),
            LocalDate.of(2024, 2, 8));

    assertThat(getFileNames(result)).containsExactly("audit-2024-02-07.log.gz",
        "audit-2024-02-08.log.gz");
  }

  @Test
  public void testGetAuditLogFiles_OnlyOneFileForTheRange() {
    LocalDate localDate = LocalDate.of(2024, 2, 8);
    List<File> result =
        defaultAuditLogFilesProvider.getAuditLogFiles(localDate, localDate);

    assertThat(getFileNames(result)).containsExactly("audit-2024-02-08.log.gz");
  }

  @Test
  public void testGetAuditLogFiles_UsesConfiguredAuditLogDirectoryOutsideSonatypeWorkLogs() throws Exception {
    Path configuredAuditDir = tempDir.newFolder("custom-audit").toPath();
    copyTestResourceAs("audit-2024-02-08.log.gz", configuredAuditDir.resolve("custom-audit-2024-02-08.log.gz"));
    copyTestResourceAs("audit.log", configuredAuditDir.resolve("custom-audit.log"));
    config.setAuditLogFilename(configuredAuditDir.resolve("custom-audit.log").toString());

    List<File> result = defaultAuditLogFilesProvider.getAuditLogFiles(LocalDate.of(2024, 2, 8),
        LocalDate.of(2024, 2, 8));

    assertThat(getFileNames(result)).containsExactly("custom-audit-2024-02-08.log.gz");
  }

  @Test
  public void testGetAuditLogFiles_FallsBackToSonatypeWorkLogsWhenConfiguredAuditDirectoryDoesNotExist() {
    config.setAuditLogFilename(tempDir.getRoot().toPath().resolve("missing").resolve("custom-audit.log").toString());

    List<File> result = defaultAuditLogFilesProvider.getAuditLogFiles(LocalDate.of(2024, 2, 8),
        LocalDate.of(2024, 2, 8));

    assertThat(getFileNames(result)).containsExactly("audit-2024-02-08.log.gz");
  }

  @Test
  public void testGetAuditLogFiles_NoLogDirectory() {
    InsightConfig missingLogConfig = new InsightConfig();
    missingLogConfig.setSonatypeWork("/nonexistent/path");

    DefaultAuditLogFilesProvider providerWithoutLogs = new DefaultAuditLogFilesProvider(missingLogConfig);

    assertThatThrownBy(() -> providerWithoutLogs.getAuditLogFiles(LocalDate.now(), LocalDate.now()))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot get the audit log path.");
  }

  private void copyTestResource(String filename) throws Exception {
    copyTestResourceAs(filename, logDir.resolve(filename));
  }

  private void copyTestResourceAs(final String filename, final Path target) throws Exception {
    URL url = getClass().getResource("/" + getClass().getSimpleName() + "/" + filename);
    if (url != null) {
      Files.copy(new File(url.getFile()).toPath(), target);
    }
  }

  private static List<String> getFileNames(final List<File> files) {
    return files.stream()
        .map(File::getName)
        .collect(Collectors.toList());
  }
}
