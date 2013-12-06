/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.dataaccess.ProprietaryConfigDAO;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.io.ScanReader;

import com.google.inject.Binder;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ScannerTest
    extends InjectedTest
{

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Inject
  private Scanner scanner;

  @Inject
  private ScanReader scanReader;

  @Inject
  private InsightWork work;

  private ProprietaryConfigDAO proprietaryConfigDAO;

  @Override
  public void configure(Binder binder) {
    InsightConfig config = new InsightConfig();
    try {
      config.setSonatypeWork(tmpDir.newFolder("work").getAbsolutePath());
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    InsightWork work = new InsightWork(config);
    binder.bind(InsightWork.class).toInstance(work);
  }

  @Before
  public void init() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Arrays.asList("com.sonatype"));
    proprietaryConfigDAO = new ProprietaryConfigDAO(work.getDataDir());
    proprietaryConfigDAO.update(proprietaryConfig);
  }

  @Test
  public void testScan() throws Exception {
    File appFile = new File("src/test/resources/ScannerTest/app01.zip");
    File scanFile = scanner.scan(appFile);
    assertThat(scanFile, is(notNullValue()));
    assertThat(scanFile.isFile(), is(true));

    Scan scan = scanReader.read(scanFile);
    assertThat(scan, is(notNullValue()));
    assertThat(scan.getItems(), hasSize(1));
    ScanItem item = scan.getItems().get(0);
    assertThat(item.getPath(), is("app01.zip"));
    assertThat(item.getItems(), hasSize(1));
    item = item.getItems().get(0);
    assertThat(item.getPath(), is("proprietary.jar"));
    assertThat(item.getItems(), hasSize(1));
    item = item.getItems().get(0);
    assertThat(item.getSha1(), is("44a17e5a5594edeebc94"));
    assertThat(item.getSha1JA001(), is(notNullValue()));
    assertThat(item.getSha1JB001(), is(notNullValue()));
    assertThat(item.getSha1JC001(), is(notNullValue()));
    assertThat(item.getSha1JD001(), is(notNullValue()));
    assertThat(item.getPath(), is(nullValue()));
    assertThat(item.getNoPathReason(), is("proprietaryPackages"));
  }
}
