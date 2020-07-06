/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.tools.reportgenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportGeneratorTest
{
  @Test
  public void testParseFolderNamesFromTemplateAndQueries_Valid() throws Exception {
    ReportGeneratorParameters parameters = new ReportGeneratorParameters();
    parameters.setTemplate(Thread.currentThread().getContextClassLoader()
            .getResource("reportgenerator/template.json").getPath());
    parameters.setQueries(Thread.currentThread().getContextClassLoader()
            .getResource("reportgenerator/queries.json").getPath());
    String databasePath = Thread.currentThread().getContextClassLoader()
            .getResource("reportgenerator/ods.h2.db").getPath();
    parameters.setDatabase(databasePath.substring(0, databasePath.length() - 6));

    List<ApplicationReportFolderDTO> folders =
            ReportGenerator.parseFolderNamesFromTemplateAndQueries(parameters);

    assertThat(folders).hasSize(2);
    ApplicationReportFolderDTO folder = folders.get(0);
    assertThat(folder.name).isEqualTo("1d63393b05ac4f2abe06be360ded4ebb");
    assertThat(folder.scanFolderNames).containsExactly("0cb9f96a5bb84936b057d02886c5a00d");
    folder = folders.get(1);
    assertThat(folder.name).isEqualTo("5fb34ecdfa1c420d82389326477e7feb");
    assertThat(folder.scanFolderNames).containsExactly("95e398adaae7477284fc2005ce31b872");
  }

  @Test
  public void testCreateFolders_Valid() throws IOException {
    ReportGeneratorParameters parameters = new ReportGeneratorParameters();
    parameters.setReportAndCacheZip(Thread.currentThread().getContextClassLoader()
            .getResource("reportgenerator/report-and-cache.zip").getPath());
    Path temporaryDirectory = Files.createTempDirectory("reportgenerator-test");
    parameters.setSonatypeWork(temporaryDirectory.toString());

    List<ApplicationReportFolderDTO> folders = new ArrayList<>();
    ApplicationReportFolderDTO folder1 = new ApplicationReportFolderDTO();
    folder1.name = "1d63393b05ac4f2abe06be360ded4ebb";
    folder1.scanFolderNames.add("0cb9f96a5bb84936b057d02886c5a00d");
    String path1 =
        Paths.get(parameters.getSonatypeWork(), "clm-server", "report", folder1.name, folder1.scanFolderNames.get(0))
            .toString();
    folders.add(folder1);
    ApplicationReportFolderDTO folder2 = new ApplicationReportFolderDTO();
    folder2.name = "5fb34ecdfa1c420d82389326477e7feb";
    folder2.scanFolderNames.add("95e398adaae7477284fc2005ce31b872");
    String path2 =
        Paths.get(parameters.getSonatypeWork(), "clm-server", "report", folder2.name, folder2.scanFolderNames.get(0))
            .toString();
    folders.add(folder2);

    List<String> paths = ReportGenerator.createFolders(parameters, folders);

    assertThat(paths).containsExactly(path1, path2);
    assertThat(Paths.get(path1)).isDirectory();
    assertThat(Paths.get(path1, "report.zip")).isRegularFile();
    assertThat(Paths.get(path1, "report.cache")).isDirectory();
    assertThat(Paths.get(path2)).isDirectory();
  }
}
