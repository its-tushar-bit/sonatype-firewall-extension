/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

/**
 * Backend for persisting file-based parts of an application report. Maintains both the original report files
 * and any modifications/additions to them.
 */
public abstract class ApplicationReportPersistenceService
{
  /**
   * @return the report entity. If it exists both as an "additional file" and a part of the original
   *         report, the "additional file" is returned. If it does not exist in either, the returned entity's exists()
   *         method will return false, and writing to that entity will create a new non-additional file.
   */
  public final ReportEntity getReportEntity(
      final String applicationId,
      final String scanId,
      final String name) throws IOException
  {
    validateName(name);
    return doGetReportEntity(applicationId, scanId, name);
  }

  protected abstract ReportEntity doGetReportEntity(
      final String applicationId,
      final String scanId,
      final String name) throws IOException;

  /**
   * @return a sequence of entities representing all files in the report for the given application and scan (both
   *         additional files and original report files with any modifications). These entities should be treated as
   *         read-only:
   *         their `getOutputStream` methods _may_ throw an exception if called.
   *
   *         IMPORTANT: This stream must be closed when you are done using it, as it is typically backed by IO
   *         resources.
   *         The ReportEntities themselves are only valid until the Stream is closed. Do not use them after that.
   */
  public abstract Stream<ReportEntity> getAllReportEntities(
      final String applicationId,
      final String scanId) throws IOException;

  public abstract Stream<ReportEntity> getOriginalReportEntities(
      final String applicationId,
      final String scanId) throws IOException;

  /**
   * Saves the zip file for the original report from HDS
   */
  public abstract void saveOriginalReport(
      final String applicationId,
      final String scanId,
      final InputStream reportZipContents) throws IOException;

  public abstract void saveOriginalReportEntities(
      final String applicationId,
      final String scanId,
      final Stream<ReportEntity> originalReportEntities) throws IOException;

  /**
   * Within the same application, move the report of the source scan ID to that of the destination scan ID,
   * overwriting the destination scan ID's report if it exists.
   */
  public abstract void moveReport(
      final String appId,
      final String sourceScanId,
      final String destinationScanId) throws IOException;

  /**
   * Save an updated copy of an entity. The file is not required to already exist.
   */
  public final void saveReportFile(
      final String applicationId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    validateName(name);
    doSaveReportFile(applicationId, scanId, name, contents);
  }

  protected abstract void doSaveReportFile(
      final String applicationId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException;

  /**
   * Save the file as an "additional" file. The file is not required to already exist, and is overwritten if it does.
   * Additional files should always have distinct names from original report files or files saved via saveReportFile.
   * If there is a naming conflict between the two methods, the results of that call and future get calls are undefined.
   */
  public final void saveAdditionalReportFile(
      final String applicationId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    validateName(name);
    doSaveAdditionalReportFile(applicationId, scanId, name, contents);
  }

  protected abstract void doSaveAdditionalReportFile(
      final String applicationId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException;

  public abstract ReportPdfEntity getPdfEntity(final String applicationId, final String scanId);

  /**
   * Unfortunately, when this entity was added to the report (before this class existed), it wasn't placed in any of
   * the correct folders, so getReportEntity can't find it and it requires its own special case method
   */
  public abstract BaseReportEntity getVulnerabilitySignaturesEntity(final String applicationId, final String scanId);

  /**
   * @return a string (typically a path or URI) indicating the "location" of this report. No guarantees are made
   *         about the technical usefulness of this string, it is only informative.
   */
  public abstract String getReportLocation(final String applicationId, final String scanId);

  /**
   * @return whether a report for the given app and scan is saved by this backend
   */
  public abstract boolean reportExists(final String applicationId, final String scanId) throws IOException;

  /**
   * Delete any saved report for the given application and scan
   */
  public abstract void deleteReport(final String applicationId, final String scanId) throws IOException;

  /**
   * Delete all reports for the given application
   */
  public abstract void deleteReports(final String applicationId) throws IOException;

  public abstract void deleteReportEntity(final ReportEntity reportEntity) throws IOException;

  /**
   * Validates that the name is a relative path and does not contain any ".." or "." segments.
   */
  private void validateName(String name) {
    boolean invalid = false;

    invalid |= name.isEmpty();
    invalid |= name.startsWith("/"); // unix absolute path
    invalid |= name.startsWith("\\"); // Windows drive-relative absolute path
    invalid |= name.matches("^[a-zA-Z]:.*"); // Windows drive letter

    // split on directory separators
    for (String pathPart : name.split("/|\\\\")) {
      invalid |= pathPart.equals("..");
      invalid |= pathPart.equals(".");
      invalid |= !pathPart.strip().equals(pathPart); // leading/trailing whitespace is not allowed on Windows
    }

    if (invalid) {
      throw new IllegalArgumentException("Invalid name: " + name);
    }
  }

  /**
   * @return true if report files are accessible in the report directory and should be zipped and moved to the trash
   *         directory before being deleted.
   */
  public boolean supportsTrash() {
    return false;
  }

  /**
   * @return the {@link ReportEntity} class this handles.
   */
  public abstract Class<? extends ReportEntity> getReportEntityClass();
}
