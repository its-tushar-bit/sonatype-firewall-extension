/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.tools.reportgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sonatype.insight.brain.tools.common.PerfTestConfig;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportGenerator
{
  private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

  public static void main(String[] args) {
    ReportGeneratorParameters parameters = new ReportGeneratorParameters();
    if (args.length == 0) {
      parameters.printUsage();
      System.exit(1);
    }
    JCommander.newBuilder().addObject(parameters).build().parse(args);
    log.info("Parameters: {}", parameters);

    try {
      List<ApplicationReportFolderDTO> folders =
              parseFolderNamesFromTemplateAndQueries(parameters);

      List<String> paths = createFolders(parameters, folders);
      log.debug("paths={}", paths);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  static List<ApplicationReportFolderDTO> parseFolderNamesFromTemplateAndQueries(
          ReportGeneratorParameters parameters) throws Exception
  {
    log.info("Parsing folder names with template, queries, and database");

    ObjectMapper mapper = new ObjectMapper();
    PerfTestConfig template = mapper.readValue(new File(parameters.getTemplate()),
            PerfTestConfig.class);
    PerfTestConfig queries = mapper.readValue(new File(parameters.getQueries()),
            PerfTestConfig.class);
    log.info("{} templates and {} queries", template.getUrls().size(), queries.getUrls().size());

    List<String> urlPatterns = template.getUrls().stream()
            .filter(testUrl -> testUrl.getUrl().contains("{applicationPublicId}")
                    && testUrl.getUrl().contains("{scanId}"))
            .map(testUrl -> testUrl.getUrl().replace("{applicationPublicId}", "(\\w+)")
                    .replace("{scanId}", "(\\w+)"))
            .collect(Collectors.toList());

    log.info("Using {} database", parameters.isPostgres() ? "PostgreSQL" : "H2");
    try (Connection connection = getConnection(parameters)) {
      Map<String, String> publicIdNameMap = new HashMap<>();
      Map<String, ApplicationReportFolderDTO> folderNameMap = new TreeMap<>();
      for (PerfTestConfig.TestUrl testUrl : queries.getUrls()) {
        for (String pattern : urlPatterns) {
          Matcher matcher = Pattern.compile(pattern).matcher(testUrl.getUrl());
          if (matcher.matches()) {
            String applicationPublicId = matcher.group(1);

            String name = getFolderName(connection, publicIdNameMap, applicationPublicId);

            ApplicationReportFolderDTO folder = getFolder(folderNameMap, name);

            String scanId = matcher.group(2);
            if (!folder.scanFolderNames.contains(scanId)) {
              folder.scanFolderNames.add(scanId);
            }
          }
        }
      }

      return new ArrayList<>(folderNameMap.values());
    }
  }

  private static Connection getConnection(ReportGeneratorParameters parameters) throws Exception {
    String dbUrl;
    if (parameters.isPostgres()) {
      dbUrl = "jdbc:postgresql://" + parameters.getHostname() + ":"
              + parameters.getPort() + "/" + parameters.getDatabase()
              + "?currentSchema=insight_brain_ods";
    }
    else {
      dbUrl = "jdbc:h2:" + new File(parameters.getDatabase()).getAbsolutePath() +
              ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000" +
              ";SCHEMA=insight_brain_ods;MV_STORE=FALSE";
    }
    String user = parameters.getUser();
    String password = parameters.getPassword();
    return DriverManager.getConnection(dbUrl, user, password);
  }

  private static String getFolderName(Connection connection, Map<String, String> publicIdNameMap,
                                      String applicationPublicId) throws SQLException
  {
    String name;
    if (publicIdNameMap.containsKey(applicationPublicId)) {
      name = publicIdNameMap.get(applicationPublicId);
    }
    else {
      name = getNameFromDatabase(connection, applicationPublicId);
      publicIdNameMap.put(applicationPublicId, name);
    }
    return name;
  }

  private static String getNameFromDatabase(Connection connection, String applicationPublicId)
          throws SQLException
  {
    try (PreparedStatement statement = connection.prepareStatement(
            "SELECT application_id FROM application WHERE public_id = ?")) {
      statement.setString(1, applicationPublicId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new RuntimeException("There are no results: " + statement.toString());
        }

        return resultSet.getString(1);
      }
    }
  }

  private static ApplicationReportFolderDTO getFolder(Map<String, ApplicationReportFolderDTO> folderNameMap,
                                                      String name)
  {
    ApplicationReportFolderDTO folder;
    if (folderNameMap.containsKey(name)) {
      folder = folderNameMap.get(name);
    }
    else {
      folder = new ApplicationReportFolderDTO();
      folder.name = name;
      folderNameMap.put(name, folder);
    }
    return folder;
  }

  @VisibleForTesting
  static List<String> createFolders(ReportGeneratorParameters parameters,
                                    List<ApplicationReportFolderDTO> folderNames)
          throws IOException
  {
    log.info("Creating folders");

    List<String> paths = new ArrayList<>();
    for (ApplicationReportFolderDTO folder : folderNames) {
      Path path = Paths.get(parameters.getSonatypeWork(),"clm-server", "report", folder.name,
              folder.scanFolderNames.get(0));
      Path directory = Files.createDirectories(path);

      try (ZipInputStream zis = new ZipInputStream(
              new FileInputStream(parameters.getReportAndCacheZip()))) {
        byte[] buffer = new byte[1024];
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
          Path newPath = directory.resolve(zipEntry.getName());
          if (zipEntry.isDirectory()) {
            if (!Files.exists(newPath)) {
              Files.createDirectories(newPath);
            }
          }
          else {
            createFile(zis, buffer, newPath);
          }
          zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
      }

      paths.add(path.toString());
    }

    return paths;
  }

  private static void createFile(ZipInputStream zis, byte[] buffer, Path newPath) throws IOException {
    if (!Files.exists(newPath.getParent())) {
      Files.createDirectories(newPath.getParent());
    }
    try (FileOutputStream fos = new FileOutputStream(newPath.toFile())) {
      int len;
      while ((len = zis.read(buffer)) > 0) {
        fos.write(buffer, 0, len);
      }
    }
  }
}
