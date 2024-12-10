/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;

import com.fasterxml.jackson.databind.node.ContainerNode;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME;

public interface ApplicationReport
    extends ReportEntity
{
  String BOM_JSON_FILENAME = "bom.json";

  String DATA_JSON_FILENAME = "data.json";

  String SECURITY_JSON_FILENAME = "security.json";

  String SUMMARY_JSON_FILENAME = "summary.json";

  String LICENSES_JSON_FILENAME = "licenses.json";

  String DEPENDENCIES_JSON_FILENAME = "dependencies.json";

  String POLICY_THREATS = "policythreats.json";

  List<String> THIRD_PARTY_CACHED_FILES = Arrays.asList(THIRD_PARTY_BOM_JSON_FILENAME,
      THIRD_PARTY_SECURITY_JSON_FILENAME, THIRD_PARTY_LICENSE_JSON_FILENAME);

  ReportEntry getEntry(final String name) throws IOException;

  void putEntry(String name, byte[] buf) throws IOException;

  void putEntry(String name, String text) throws IOException;

  void saveReportEntry(String entryFileName, ContainerNode<?> jsonData)
      throws IOException;

  ContainerNode<?> loadReportEntry(String entryFileName) throws IOException;

  ReportEntry extractEntry(String name) throws IOException;

  void embedApplicationPublicId(Application application) throws IOException;

  void cacheThirdPartyData();

  void deletePdfReport();

  void appendToReport(ThirdPartyApplicationReportDTO dto)
      throws IOException;

  ReportType getType() throws IOException;

  void deleteCacheDir() throws IOException;

  /**
   * Gets the contents of the {@code template.properties} embedded in the report from the HDS or an empty map if none.
   */
  Properties getTemplateProperties() throws IOException;

  String getLocation();

  enum ReportType
  {
    FULL, ERROR
  }
}
