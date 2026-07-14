/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SupportInfoUtil
{
  private static final Logger log = LoggerFactory.getLogger(SupportInfoUtil.class);

  /**
   * Counter used to generate unique names.
   */
  static final AtomicLong COUNTER = new AtomicLong();

  static final String WORK_DIR = "support";

  static final String BUNDLE_SUFFIX = ".zip";

  /**
   * Glob restricting the stale-bundle sweep to files this class itself creates, so a support
   * engineer who parks another {@code .zip} in {@code sonatype-work/support/} cannot have it
   * silently deleted on the next generation.
   */
  static final String BUNDLE_SWEEP_GLOB = "*-mtiq-support-*" + BUNDLE_SUFFIX;

  /**
   * Support-bundle ZIPs are streamed to disk and deleted after the response body is written to the
   * client. If Jersey never invokes the {@link jakarta.ws.rs.core.StreamingOutput} (e.g. the client
   * aborts before any bytes are read, or a response filter throws after the resource returns), the
   * in-lambda delete never runs. Every fresh generation therefore sweeps any bundle in the work
   * directory older than this threshold as a self-healing backstop — on the shared MTIQ task, disk
   * usage stays bounded even if a bundle is generated once a day.
   */
  static final Duration STALE_BUNDLE_THRESHOLD = Duration.ofMinutes(30);

  private final InsightConfig insightConfig;

  @Inject
  public SupportInfoUtil(InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  public SupportInfo generateSupportInfo(
      final String tenantSlug,
      final List<SupportFile> filesToZip) throws IOException
  {
    File workDir = getWorkDir();
    Files.createDirectories(workDir.toPath());

    // Sweep orphaned bundles from prior aborted requests. Best-effort; a failure here does not
    // block generation of a new bundle.
    sweepStaleBundles(workDir);

    String supportInformationName = generateUniqueName(tenantSlug + "-mtiq-support-");
    File supportZipFile = new File(workDir, supportInformationName + BUNDLE_SUFFIX);

    log.info("Generating Support info: {}", supportZipFile);
    try (final OutputStream fos = new BufferedOutputStream(Files.newOutputStream(supportZipFile.toPath()));
        final ZipOutputStream zos = new ZipOutputStream(fos))
    {
      for (final SupportFile fileToAdd : filesToZip) {
        final ZipEntry zipEntry = new ZipEntry(
            supportInformationName + "/" + fileToAdd.supportFileType.getDirName() + "/" + fileToAdd.file.getName());
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(fileToAdd.file)) {
          IOUtils.copy(fis, zos);
        }

        zos.closeEntry();
        if (fileToAdd.isDeleteAfterZipped) {
          try {
            Files.deleteIfExists(fileToAdd.file.toPath());
          }
          catch (IOException e) {
            log.warn("Failed to delete temporary support file: {}", fileToAdd.file.getAbsolutePath());
          }
        }
      }
    }
    catch (IOException e) {
      // If we blew up mid-write we must not leak a partial ZIP.
      try {
        Files.deleteIfExists(supportZipFile.toPath());
      }
      catch (IOException suppressed) {
        e.addSuppressed(suppressed);
      }
      throw e;
    }
    log.info("Generated Support Info: {}", supportZipFile);

    return new SupportInfo(supportZipFile, supportInformationName);
  }

  /**
   * Delete support bundle files that were left behind by prior generations older than
   * {@link #STALE_BUNDLE_THRESHOLD}. This runs opportunistically on each new generation so the
   * shared MTIQ ECS task's disk usage stays bounded without a dedicated scheduled task.
   */
  private void sweepStaleBundles(File workDir) {
    Path dir = workDir.toPath();
    Instant cutoff = Instant.now().minus(STALE_BUNDLE_THRESHOLD);
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, BUNDLE_SWEEP_GLOB)) {
      for (Path candidate : stream) {
        try {
          if (Files.isRegularFile(candidate)
              && Files.getLastModifiedTime(candidate).toInstant().isBefore(cutoff))
          {
            Files.deleteIfExists(candidate);
            log.info("Swept stale support bundle: {}", candidate);
          }
        }
        catch (IOException e) {
          log.warn("Failed to inspect/delete potential stale support bundle {}: {}", candidate, e.getMessage());
        }
      }
    }
    catch (IOException e) {
      log.warn("Failed to sweep stale support bundles in {}: {}", workDir, e.getMessage());
    }
  }

  /**
   * Strip characters that could escape the support work directory when appended to a filename.
   * Upstream tenant-slug validation already blocks these, but the bundle filename is derived by
   * string concatenation, so we defensively normalize path separators, drive letters, and
   * relative-path segments.
   */
  static String sanitizeForFilename(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replaceAll("[\\r\\n]", "_")
        .replace('/', '_')
        .replace('\\', '_')
        .replace(':', '_')
        .replace("..", "_");
  }

  public String generateUniqueName(final String prefix) {
    return sanitizeForFilename(prefix) + new SimpleDateFormat("yyyyMMdd-HHmmss")
        .format(new Date()) + "-" + COUNTER.incrementAndGet();
  }

  public File writeTextToFile(final String fileContent, String fileName) throws IOException {
    File outputFile = new File(getWorkDir(), fileName);
    FileUtils.write(outputFile, fileContent, StandardCharsets.UTF_8);
    return outputFile;
  }

  /**
   * Streams pretty-printed JSON for the given POJO straight to a file inside the support work
   * directory, without materializing the JSON as an in-memory {@code String} or {@code byte[]}.
   * Use this in preference to {@link #writeTextToFile(String, String)} whenever the payload may
   * be large (e.g. DB dumps), because a byte[]-backed representation cannot exceed
   * {@code Integer.MAX_VALUE - 8} and will crash the JVM with an {@link OutOfMemoryError} past
   * that ceiling — which restarts the shared MTIQ task.
   */
  public File writePojoAsJsonToFile(final Object pojo, final String fileName) throws IOException {
    File outputFile = new File(getWorkDir(), fileName);
    JsonUtils.generate(pojo, outputFile);
    return outputFile;
  }

  public File getWorkDir() {
    return new File(insightConfig.getSonatypeWork(), WORK_DIR);
  }
}
