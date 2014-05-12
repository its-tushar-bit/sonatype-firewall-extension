/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.ProprietaryConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProprietaryConfigDAOTest
{

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private ProprietaryConfigDAO dao;

  @Before
  public void init() throws Exception {
    dao = new ProprietaryConfigDAO(tmpDir.newFolder());
  }

  @Test
  public void testGet_NoConfigPersisted() {
    ProprietaryConfig config = dao.get();
    assertNotNull(config);
    assertEquals(0, config.getPackages().size());
    assertEquals(0, config.getRegexes().size());
  }

  @Test
  public void testUpdate() {
    List<String> packages = Arrays.asList("org.sonatype", "com.sonatype");
    List<String> regexes = Arrays.asList(".*\\.zip");
    ProprietaryConfig config = new ProprietaryConfig();
    config.setPackages(packages);
    config.setRegexes(regexes);
    dao.update(config);
    config = dao.get();
    assertEquals(packages, config.getPackages());
    assertEquals(regexes, config.getRegexes());
  }

  @Test(expected = InvalidProprietaryConfigRegexException.class)
  public void testInvalidRegex() {
    List<String> regexes = Arrays.asList("*");
    ProprietaryConfig config = new ProprietaryConfig();
    config.setRegexes(regexes);
    dao.update(config);
  }

  @Test(expected = InvalidProprietaryConfigRegexException.class)
  public void testInvalidRegexNPE() {
    List<String> regexes = new ArrayList<>();
    regexes.add(null);
    ProprietaryConfig config = new ProprietaryConfig();
    config.setRegexes(regexes);
    dao.update(config);
  }

}
