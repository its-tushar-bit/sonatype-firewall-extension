/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
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

  private final InsightConfig insightConfig;

  @Inject
  public SupportInfoUtil(InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  public SupportInfo generateSupportInfo(
      final String tenantSlug,
      final List<SupportFile> filesToZip) throws IOException
  {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    String supportInformationName = generateUniqueName(tenantSlug + "-mtiq-support-");

    log.info("Generating Support info: {}", supportInformationName);
    try (final ZipOutputStream zos = new ZipOutputStream(byteArrayOutputStream)) {
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
    log.info("Generated Support Info: {}", supportInformationName);

    return new SupportInfo(byteArrayOutputStream, supportInformationName);
  }

  public String generateUniqueName(final String prefix) {
    return prefix.replaceAll("[\n\r]", "_") + new SimpleDateFormat("yyyyMMdd-HHmmss")
        .format(new Date()) + "-" + COUNTER.incrementAndGet();
  }

  public File writeTextToFile(final String fileContent, String fileName) throws IOException {
    File outputFile = new File(getWorkDir(), fileName);
    FileUtils.write(outputFile, fileContent, StandardCharsets.UTF_8);
    return outputFile;
  }

  public File getWorkDir() {
    return new File(insightConfig.getSonatypeWork(), WORK_DIR);
  }
}
