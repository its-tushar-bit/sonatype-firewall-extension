/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class which defines the rules to run visual testing from a particular git branch.
 */
public class VisualTestBranchEnabler
{
  private static final String ENABLED_BRANCHES_FILENAME = "branchesEnabledForVisualTesting.list";

  private static final String UI_SUFFIX_TRIGGER = "_ui";

  private Set<String> branchesEnabledForVisualTesting;

  public VisualTestBranchEnabler() {
    final URL enabledBranchesResourceFile = this.getClass().getClassLoader().getResource(ENABLED_BRANCHES_FILENAME);
    try (Stream<String> lines = Files.lines(Paths.get(enabledBranchesResourceFile.toURI()))) {
      branchesEnabledForVisualTesting = lines.collect(Collectors.toSet());
    }
    catch (IOException | URISyntaxException e) {
      Logger log = LoggerFactory.getLogger(AbstractFunctionalTest.class);
      log.info("Error while loading enabled branches, defaulting to branch suffix strategy", e);
      branchesEnabledForVisualTesting = new HashSet<>();
    }
  }

  public boolean isVisualTestingEnabledForBranch(String localBranchName) {
    return branchHasUiSuffix(localBranchName) || branchesEnabledForVisualTesting.contains(localBranchName);
  }

  private boolean branchHasUiSuffix(final String localBranchName) {
    return localBranchName.toLowerCase(Locale.ENGLISH).contains(UI_SUFFIX_TRIGGER);
  }
}
