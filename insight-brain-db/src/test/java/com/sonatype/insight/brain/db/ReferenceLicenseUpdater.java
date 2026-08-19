/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * See the docs in ReferencePolicyImportIntegrationTest on how to use this class. It should only be run from time to
 * time. It refreshes the sql files we have in insight_brain_dm.
 */
public class ReferenceLicenseUpdater
{
  public static void main(String[] args) throws Exception {
    try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      HttpGet request = new HttpGet("https://clm-staging.sonatype.com/rest/license");
      String responseBody = EntityUtils.toString(httpClient.execute(request).getEntity()).replaceAll("'", "''");
      LicenseResponseDto licenseResponseDto = new Gson().fromJson(responseBody, LicenseResponseDto.class);

      StringBuilder licenseSqlBuilder = new StringBuilder();
      StringBuilder multiLicenseSqlBuilder = new StringBuilder();
      StringBuilder multiLicenseLicenseSqlBuilder = new StringBuilder();

      Collections.sort(licenseResponseDto.licenses);
      Collections.sort(licenseResponseDto.multiLicenses);

      String sql;
      for (License license : licenseResponseDto.licenses) {
        sql =
            String.format(
                "INSERT INTO license (license_id,shortDisplayName,longDisplayName) VALUES ('%s','%s','%s');\n",
                license.id, license.shortDisplayName, license.longDisplayName);
        licenseSqlBuilder.append(sql);
      }

      for (License license : licenseResponseDto.multiLicenses) {
        sql = String.format(
            "INSERT INTO multi_license (multi_license_id,shortDisplayName,longDisplayName) VALUES ('%s','%s','%s');\n",
            license.id, license.shortDisplayName, license.longDisplayName);
        multiLicenseSqlBuilder.append(sql);
      }

      Map<String, Set<String>> sortedMap = new TreeMap<>(Comparator.comparing(s -> s.toLowerCase(Locale.ROOT)));
      sortedMap.putAll(licenseResponseDto.multiLicenseMappings);
      for (String key : sortedMap.keySet()) {
        Set<String> values = sortedMap.get(key);
        TreeSet<String> sortedValues = new TreeSet<>(Comparator.comparing(s -> s.toLowerCase(Locale.ROOT)));
        sortedValues.addAll(values);
        for (String value : sortedValues) {
          sql =
              String.format("INSERT INTO multi_license_license (multi_license_id,license_id) VALUES ('%s','%s');\n",
                  key,
                  value);
          multiLicenseLicenseSqlBuilder.append(sql);
        }
      }

      // When working directory is insight-brain
      Path brainDm = Paths.get("insight-brain-db", "src", "main", "resources", "db", "insight_brain_dm");
      if (Paths.get(".").toAbsolutePath().normalize().endsWith("insight-brain-db")) {
        // When working directory is insight-brain-db
        brainDm = Paths.get("src", "main", "resources", "db", "insight_brain_dm");
      }

      File licenseSql = brainDm.resolve("license.sql").toFile();
      File multiLicenseSql = brainDm.resolve("multi_license.sql").toFile();
      File multiLicenseLicenseSql = brainDm.resolve("multi_license_license.sql").toFile();

      FileUtils.writeStringToFile(licenseSql, licenseSqlBuilder.toString(), UTF_8);
      FileUtils.writeStringToFile(multiLicenseSql, multiLicenseSqlBuilder.toString(), UTF_8);
      FileUtils.writeStringToFile(multiLicenseLicenseSql, multiLicenseLicenseSqlBuilder.toString(), UTF_8);
    }
  }

  private static class LicenseResponseDto
  {
    public List<License> licenses;

    public List<License> multiLicenses;

    public Map<String, Set<String>> multiLicenseMappings;
  }

  private static class License
      implements Comparable<License>
  {
    private static final Comparator<License> COMPARATOR =
        Comparator.comparing((License l) -> l.id.toLowerCase(Locale.ROOT));

    public String id;

    public String shortDisplayName;

    public String longDisplayName;

    @Override
    public int compareTo(final License o) {
      return COMPARATOR.compare(this, o);
    }
  }
}
