/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

import com.google.common.io.PatternFilenameFilter;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@Category(SlowTest.class)
public class MigrationScriptsTest
    extends AbstractDatabaseTest
{
  private OperationalDataStore operationalDataStore;

  private DataMartDataStore dataMartDataStore;

  private AggregationDataStore aggregationDataStore;

  private ThirdPartyScansDataStore thirdPartyScansDataStore;

  private DataStore[] allDataStores;

  @Before
  public void before() {
    operationalDataStore = databaseRule.getOperationalDataStore();
    dataMartDataStore = databaseRule.getDataMartDataStore();
    aggregationDataStore = databaseRule.getAggregationDataStore();
    thirdPartyScansDataStore = databaseRule.getThirdPartyScansDataStore();

    allDataStores =
        new DataStore[]{operationalDataStore, dataMartDataStore, aggregationDataStore, thirdPartyScansDataStore};
  }

  @Test
  public void testMigrationScripts_NoOverlapAndNotMissing() {
    for (DataStore dataStore : allDataStores) {
      File dbDirectory = new File(getClass().getClassLoader().getResource("db/" + dataStore.getID()).getFile());
      assertThat(dbDirectory).isDirectory();
      List<File> incrementalScripts = new ArrayList<>(
          Arrays.stream(dbDirectory.listFiles(new PatternFilenameFilter("schema_incremental.*sql"))).toList());
      Map<Integer, List<String>> numberToTypes = new TreeMap<>();
      for (File incrementalScript : incrementalScripts) {
        String name = incrementalScript.getName();
        String numberString = name.substring("schema_incremental_".length());
        numberString = numberString.substring(0, numberString.indexOf('.'));
        int number = Integer.parseInt(numberString);
        List<String> types = numberToTypes.computeIfAbsent(number, key -> new ArrayList<>());
        if (name.endsWith(".h2.sql")) {
          types.add("h2");
        }
        else if (name.endsWith(".pg.sql")) {
          types.add("pg");
        }
        else {
          types.add("generic");
        }
      }
      Integer previous = null;
      for (Entry<Integer, List<String>> entry : numberToTypes.entrySet()) {
        int number = entry.getKey();
        if (previous != null && number != previous + 1) {
          fail("Missing " + dataStore.getID() + " schema incremental " + (previous + 1));
        }
        List<String> types = entry.getValue();
        if (types.contains("generic") && (types.contains("h2") || types.contains("pg"))) {
          fail("Found generic and specific scripts for " + dataStore.getID() + " schema incremental " + number +
              ". Expected either 1 generic script OR 1 specific script per db.");
        }
        if ((types.contains("h2") && !types.contains("pg")) || (!types.contains("h2") && types.contains("pg"))) {
          fail("Missing specific script for both databases for " + dataStore.getID() + " schema incremental " + number +
              ".");
        }
        previous = number;
      }
    }
  }
}
