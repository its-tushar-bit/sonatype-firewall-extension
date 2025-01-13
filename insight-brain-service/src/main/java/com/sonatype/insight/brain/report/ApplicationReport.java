/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.Properties;

import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;

import com.fasterxml.jackson.databind.node.ContainerNode;

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

  ReportEntry getEntry(final String name) throws IOException;

  void putEntry(String name, byte[] buf) throws IOException;

  void saveReportEntry(String entryFileName, ContainerNode<?> jsonData)
      throws IOException;

  ContainerNode<?> loadReportEntry(String entryFileName) throws IOException;

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
