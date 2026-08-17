/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.rules.TemporaryFolder;

public class ReportHelper
{
  /**
   * Create zipped report given report dir
   *
   * @param reportResourceName resource directory with unzipped report
   * @param tempDir directory to put zipped report
   * @return URL to zipped report
   */
  public static URL zipReport(String reportResourceName, TemporaryFolder tempDir) {
    return zipReport(reportResourceName, tempDir.getRoot().toPath());
  }

  /**
   * Create zipped report given report dir, writing the zip under the supplied temp directory.
   *
   * @param reportResourceName resource directory with unzipped report
   * @param tempDir directory to put the zipped report (e.g. a JUnit 5 {@code @TempDir})
   * @return URL to zipped report
   */
  public static URL zipReport(String reportResourceName, Path tempDir) {
    URI reportResourceUrl = getClasspathURI(reportResourceName);
    URI zipURI = zipReport(reportResourceUrl, tempDir);

    try {
      return zipURI.toURL();
    }
    catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Create a report zip based on the contents of the classpath-relative reportResourcePath (which may point to either a
   * zip or a directory) and save it to the report directory for the given application and scan. Third-party files in
   * the directory or zip are extracted to an adjacent "additional.files" directory, matching the realistic behavior.
   *
   * @param reportResourcePath classpath-relative path to the report resource dir or zip
   */
  public static void saveMockReport(
      InsightWork insightWork,
      TemporaryFolder tempFolder,
      String reportResourcePath,
      String applicationId,
      String scanId) throws IOException
  {
    saveMockReport(insightWork, tempFolder, getClasspathURI(reportResourcePath), applicationId, scanId);
  }

  /**
   * Create a report zip based on the contents of reportResourcePath (which may point to either a zip or a directory)
   * and save it to the report directory for the given application and scan. Third-party files in the directory or zip
   * are extracted to an adjacent "additional.files" directory, matching the realistic behavior.
   */
  public static void saveMockReport(
      InsightWork insightWork,
      TemporaryFolder tempFolder,
      Path reportResourcePath,
      String applicationId,
      String scanId) throws IOException
  {
    saveMockReport(insightWork, tempFolder, reportResourcePath.toUri(), applicationId, scanId);
  }

  /**
   * Create a report zip that is empty aside from a minimal index.html and save it to the report directory for the given
   * application and scan.
   */
  public static void saveMockReport(InsightWork insightWork, String applicationId, String scanId) throws IOException {
    Path reportDir = insightWork.getReportDir(applicationId, scanId).toPath();
    Path zipPath = reportDir.resolve("report.zip");
    Files.createDirectories(reportDir);

    ensureIndexHtmlInZip(zipPath, true);
  }

  /**
   * Save the specified policy violations as a policythreats.json file in the report.cache dir
   */
  public static void createPolicyThreats(
      InsightWork insightWork,
      String appId,
      String scanId,
      List<PolicyViolation> policyViolations) throws IOException
  {
    PolicyThreats policyThreats = PolicyThreatsAdapter.createPolicyThreats(policyViolations, null, null);
    byte[] policyThreatsJson = JsonUtils.generate(policyThreats);

    try (var policyThreatsStream = new ByteArrayInputStream(policyThreatsJson)) {
      createPolicyThreats(insightWork, appId, scanId, policyThreatsStream);
    }
  }

  public static void createPolicyThreats(
      InsightWork insightWork,
      String appId,
      String scanId,
      InputStream jsonStream) throws IOException
  {
    Path filePath = insightWork.getReportDir(appId, scanId)
        .toPath()
        .resolve("report.cache")
        .resolve("policythreats.json");

    Files.createDirectories(filePath.getParent());
    Files.copy(jsonStream, filePath);
  }

  private static URI getClasspathURI(String resourcePath) {
    try {
      URL resourceURL = ReportHelper.class.getResource(resourcePath);
      if (resourceURL == null) {
        throw new IllegalArgumentException("Resource not found: " + resourcePath);
      }

      return resourceURL.toURI();
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static URI zipReport(URI reportResource, Path tempDir) {
    if (reportResource.toString().endsWith(".zip")) {
      return copyZip(reportResource, tempDir);
    }
    else {
      return zipResourceDir(reportResource, tempDir);
    }
  }

  private static URI zipResourceDir(URI resourceDirURI, Path tempDir) {
    try {
      File resourceDir = new File(resourceDirURI);
      if (!resourceDir.isDirectory()) {
        throw new RuntimeException("'" + resourceDir.getAbsolutePath() + "' is not a directory.");
      }
      File reportZipFile = new File(tempDir.toFile(), "MockReport-" + UUID.randomUUID() + ".zip");
      Zipper.zip(resourceDir, reportZipFile);
      return reportZipFile.toURI();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static URI copyZip(URI zipResourceURI, Path tempDir) {
    try {
      Path reportZipFile = Files.createTempFile(tempDir, "MockReport-", ".zip");
      Files.copy(Path.of(zipResourceURI), reportZipFile, StandardCopyOption.REPLACE_EXISTING);
      return reportZipFile.toUri();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void saveMockReport(
      InsightWork insightWork,
      TemporaryFolder tempFolder,
      URI reportResourceURI,
      String applicationId,
      String scanId) throws IOException
  {
    Path reportDir = insightWork.getReportDir(applicationId, scanId).toPath();
    Path reportZipPath = reportDir.resolve("report.zip");
    Files.createDirectories(reportDir);

    Path tempZipPath = Path.of(zipReport(reportResourceURI, tempFolder.getRoot().toPath()));
    Files.move(tempZipPath, reportZipPath);

    ensureIndexHtmlInZip(reportZipPath, false);
    moveThirdPartyFilesOutOfZip(reportZipPath);
  }

  private static void ensureIndexHtmlInZip(Path zipFilePath, boolean create) throws IOException {
    try (FileSystem zipFs = createZipFileSystem(zipFilePath, create)) {
      Path htmlPath = zipFs.getPath("index.html");
      if (!Files.exists(htmlPath)) {
        Files.writeString(htmlPath, "<html></html>");
      }
    }
  }

  /**
   * Removes files whose names begin with "thirdparty-" from the zip file, and places them in an adjacent
   * "additional.files" directory. This matches the behavior of the application, where the zip from HDS does not
   * contain these files and rather they are generated by IQ and placed in the additional.files dir.
   */
  private static void moveThirdPartyFilesOutOfZip(Path zipPath) throws IOException {
    Path additionalFilesDir = zipPath.getParent().resolve("additional.files");
    Files.createDirectory(additionalFilesDir);

    try (FileSystem zipFs = createZipFileSystem(zipPath, false)) {
      Files.walk(zipFs.getPath("/"))
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().startsWith("thirdparty-"))
          .forEach(path -> {
            try {
              Files.move(path, additionalFilesDir.resolve(path.getFileName().toString()));
            }
            catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    }
    catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private static FileSystem createZipFileSystem(Path zipPath, boolean create) throws IOException {
    URI zipUri = URI.create("jar:" + zipPath.toUri());
    Map<String, String> env = Map.of("create", Boolean.toString(create));
    return FileSystems.newFileSystem(zipUri, env);
  }

  public static void createEmptyZip(final Path zipPath) throws Exception {
    Files.createDirectories(zipPath.getParent());
    try (FileSystem fileSystem = createZipFileSystem(zipPath, true)) {
      // no-op
    }
  }

  public static void addToZip(
      final Path zipPath,
      final Path entryPath,
      final InputStream inputStream) throws Exception
  {
    try (FileSystem fileSystem = createZipFileSystem(zipPath, false)) {
      Path relative = zipPath.relativize(entryPath);
      Path zipFile = fileSystem.getPath(relative.toString());
      Files.copy(inputStream, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public static void addToZip(final Path zipPath, final Path entryPath, final String content) throws Exception {
    try (FileSystem fileSystem = createZipFileSystem(zipPath, false)) {
      Path relative = zipPath.relativize(entryPath);
      Path zipFile = fileSystem.getPath(relative.toString());
      Files.writeString(zipFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
  }

  public static String readFromZipStream(final InputStream inputStream, final String entryPath) throws Exception {
    StringBuilder content = new StringBuilder();
    try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.getName().equals(entryPath)) {
          BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));
          String line;
          while ((line = reader.readLine()) != null) {
            content.append(line);
          }
          break;
        }
      }
      zipInputStream.closeEntry();
    }
    return content.toString();
  }
}
