/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.io.input.CharSequenceInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.fasterxml.jackson.databind.node.ContainerNode;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME;

public class ApplicationReport
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationReport.class);

  public static final String INDEX_HTML_FILENAME = "index.html";

  public static final String BOM_JSON_FILENAME = "bom.json";

  public static final String DATA_JSON_FILENAME = "data.json";

  public static final String SECURITY_JSON_FILENAME = "security.json";

  public static final String SUMMARY_JSON_FILENAME = "summary.json";

  public static final String LICENSES_JSON_FILENAME = "licenses.json";

  public static final String PARTIAL_MATCHED_FILENAME = "partialmatched.json";

  public static final String LICENSE_THREATS_JSON_FILENAME = "licensethreats.json";

  public static final String DEPENDENCIES_JSON_FILENAME = "dependencies.json";

  public static final String POLICY_THREATS_FILENAME = "policythreats.json";

  public static final String POLICY_ALERTS_FILENAME = "policyalerts.json";

  public enum ReportType
  {
    FULL, ERROR
  }

  private final ApplicationReportPersistenceService persistenceService;

  private final Application application;

  private final String scanId;

  public ApplicationReport(
      ApplicationReportPersistenceService persistenceService,
      Application application,
      String scanId)
  {
    this.persistenceService = persistenceService;
    this.application = application;
    this.scanId = scanId;
  }

  public ReportEntry getEntry(final String name) throws IOException {
    ReportEntity entity = getEntity(name);

    if (entity.exists()) {
      try (var stream = entity.getInputStream()) {
        return new ReportEntry(name, entity.getTime(), stream.readAllBytes());
      }
    }
    else {
      return null;
    }
  }

  public ReportEntity getEntity(final String name) throws IOException {
    return persistenceService.getReportEntity(application.getId(), scanId, name);
  }

  /**
   * @return a stream of all entities in the report for the application and scan. This stream must be closed!
   */
  public Stream<ReportEntity> getAllEntities() throws IOException {
    return persistenceService.getAllReportEntities(application.getId(), scanId);
  }

  public void putEntry(String name, byte[] buf) throws IOException {
    try (var stream = new ByteArrayInputStream(buf)) {
      putEntry(name, stream);
    }
  }

  public void putEntry(String name, String text) throws IOException {
    var streamBuilder = CharSequenceInputStream.builder()
        .setCharset(StandardCharsets.UTF_8)
        .setCharSequence(text);

    try (var stream = streamBuilder.get()) {
      putEntry(name, stream);
    }
  }

  public void saveReportEntry(String entryFileName, ContainerNode<?> jsonData) throws IOException {
    putEntry(entryFileName, JsonUtils.generate(jsonData));
  }

  public ContainerNode<?> loadReportEntry(String entryFileName) throws IOException {
    ReportEntity entity = getEntity(entryFileName);
    return JsonUtils.read(entity.getInputStream());
  }

  public ReportEntry extractEntry(String name) throws IOException {
    ReportEntity entity = getEntity(name);

    if (entity.exists()) {
      try (var stream = entity.getInputStream()) {
        return new ReportEntry(name, entity.getTime(), stream.readAllBytes());
      }
    }
    else {
      return getDeduplicatedLegacyReportEntry(name).orElse(null);
    }
  }

  public void embedApplicationPublicId() throws IOException {
    String filename = "index.html";
    ReportEntry reportEntry = extractEntry(filename);
    String originalIndexHtmlContent = new String(reportEntry.buf, StandardCharsets.UTF_8);
    String augmentedIndexHtmlContent =
        originalIndexHtmlContent.replace("applicationId = ''", "applicationId = '" + application.getPublicId() + "'");
    if (!augmentedIndexHtmlContent.equals(originalIndexHtmlContent)) {
      var streamBuilder = CharSequenceInputStream.builder()
          .setCharset(StandardCharsets.UTF_8)
          .setCharSequence(augmentedIndexHtmlContent);

      try (var stream = streamBuilder.get()) {
        persistenceService.saveReportFile(application.getId(), scanId, filename, stream);
      }
    }
  }

  public void deletePdfReport() {
    try {
      persistenceService.getPdfEntity(application.getId(), scanId).deleteIfExists();
      log.debug("Deleted obsolete PDF report file for application {} and scan {}", application.getId(), scanId);
    }
    catch (Exception e) {
      log.error("Cannot delete obsolete PDF report file for application {} and scan {}. Cause: {}",
          application.getId(), scanId, e.getMessage(), e);
    }
  }

  public void appendToReport(final ThirdPartyApplicationReportDTO dto) throws IOException {
    saveAdditionalReportFile(THIRD_PARTY_BOM_JSON_FILENAME, dto.billOfMaterials);
    saveAdditionalReportFile(THIRD_PARTY_SECURITY_JSON_FILENAME, dto.securityRows);
    saveAdditionalReportFile(THIRD_PARTY_LICENSE_JSON_FILENAME, dto.licenseRows);
  }

  public ReportType getType() throws IOException {
    boolean hasSecurityJson = getEntity(SECURITY_JSON_FILENAME).exists();
    boolean hasLicensesJson = getEntity(LICENSES_JSON_FILENAME).exists();

    if (!hasSecurityJson && !hasLicensesJson) {
      return ReportType.ERROR;
    }
    return ReportType.FULL;
  }

  public void deleteCacheDir() throws IOException {
    persistenceService.restoreOriginalReport(application.getId(), scanId);
  }

  /**
   * Gets the contents of the {@code template.properties} embedded in the report from the HDS or an empty map if none.
   */
  public Properties getTemplateProperties() throws IOException {
    ReportEntity entity = getEntity("template.properties");
    Properties props = new Properties();
    if (entity.exists()) {
      try (var stream = entity.getInputStream()) {
        props.load(stream);
      }
    }
    return props;
  }

  public String getLocation() {
    return persistenceService.getReportLocation(application.getId(), scanId);
  }

  public boolean exists() throws IOException {
    return persistenceService.reportExists(application.getId(), scanId);
  }

  public Application getApplication() {
    return application;
  }

  public String getScanId() {
    return scanId;
  }

  /**
   * Save a file with the report that will not get deleted by deleteCacheDir
   */
  public void saveAdditionalReportFile(final String filename, final List<?> data) throws IOException {
    byte[] bytes = JsonUtils.generate(JsonUtils.aaData(data));
    try (var stream = new ByteArrayInputStream(bytes)) {
      persistenceService.saveAdditionalReportFile(application.getId(), scanId, filename, stream);
    }
  }

  private void putEntry(String name, InputStream contents) throws IOException {
    persistenceService.saveReportFile(application.getId(), scanId, name, contents);
  }

  /**
   * Starting with release 1.168, we serve shared resources for legacy report from the jar
   * HDS does not include these files in the report.zip when IQ client is v1.168 or higher
   */
  private Optional<ReportEntry> getDeduplicatedLegacyReportEntry(final String name) throws IOException {
    String resource = "/com/sonatype/insight/brain/legacy.report/" + name;
    try (InputStream stream = getClass().getResourceAsStream(resource)) {
      if (stream != null) {
        return Optional.of(new ReportEntry(name, Instant.now().toEpochMilli(), stream.readAllBytes()));
      }
      else {
        return Optional.empty();
      }
    }
  }
}
