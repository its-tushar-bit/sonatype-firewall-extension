/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.io.ScanReader;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ScannerTest
    extends AbstractComponentTest
{
  @Inject
  private Scanner scanner;

  @Inject
  private ScanReader scanReader;

  @Inject
  private InsightWork work;

  @Test
  public void testScan() throws Exception {
    tempEntity.newApplicationWithParent("testScan");
    tempEntity.newProprietaryConfig(Organization.ROOT_ORGANIZATION_ID, Collections.singletonList("com.sonatype"),
        Collections.<String> emptyList());

    File appFile = new File("src/test/resources/ScannerTest/app01.zip");
    File scanFile = scanner.scan(appFile, "test-app.zip", new File(tempDir.getRoot(), "not-yet-existent"), "testScan");
    assertThat(scanFile, is(notNullValue()));
    assertThat(scanFile.isFile(), is(true));

    Scan scan = scanReader.read(scanFile);
    assertThat(scan, is(notNullValue()));
    assertThat(scan.getItems(), hasSize(1));
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath(), is("test-app.zip"));
    assertThat(item.getItems(), hasSize(1));
    item = item.getItems().get(0);
    assertThat(item.getPath(), is("proprietary.jar"));
    assertThat(item.getItems(), hasSize(1));
    assertThat(item.isProprietary(), nullValue());
    item = item.getItems().get(0);
    assertThat(item.getSha1(), is("44a17e5a5594edeebc94"));
    assertThat(item.getSha1JA001(), is(notNullValue()));
    assertThat(item.getSha1JB001(), is(notNullValue()));
    assertThat(item.getSha1JC001(), is(notNullValue()));
    assertThat(item.getSha1JD001(), is(notNullValue()));
    assertThat(item.getPath(), is(nullValue()));
    assertThat(item.getNoPathReason(), is("proprietaryPackages"));
  }

  @Test
  public void testScanWithProprietaryRegex() throws Exception {
    tempEntity.newApplicationWithParent("testScanWithProprietaryRegex");
    tempEntity.newProprietaryConfig(Organization.ROOT_ORGANIZATION_ID, Collections.<String> emptyList(),
        Collections.singletonList(".*prop.*\\.jar"));

    File appFile = new File("src/test/resources/ScannerTest/app01.zip");
    File scanFile = scanner.scan(appFile, "test-app.zip", new File(tempDir.getRoot(), "not-yet-existent"),
        "testScanWithProprietaryRegex");
    assertThat(scanFile, is(notNullValue()));
    assertThat(scanFile.isFile(), is(true));

    Scan scan = scanReader.read(scanFile);
    assertThat(scan, is(notNullValue()));
    assertThat(scan.getItems(), hasSize(1));
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath(), is("test-app.zip"));
    assertThat(item.getItems(), hasSize(1));
    item = item.getItems().get(0);
    assertThat(item.getPath(), is("proprietary.jar"));
    assertThat(item.isProprietary(), is(true));
  }
}
