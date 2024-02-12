/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseConstraintTest
{
  /**
   * @see <a href="https://sonatype.atlassian.net/browse/INT-2835">INT-2835</a>
   */
  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testNoUniqueIndexesDefined() throws IOException {
    URL url = Resources.getResource("db/insight_brain_ods/schema.sql");
    assertThat(url).isNotNull();

    List<String> filesChecked = new ArrayList<>();
    for (File file : new File(url.getFile()).getParentFile().listFiles()) {
      if (file.isFile()) {
        filesChecked.add(file.getName());
        // we should be using a unique constraint (which will result in an auto-generated unique index) rather than
        // explicitly creating a unique index ourselves;
        assertThat(FileUtils.readFileToString(file, StandardCharsets.UTF_8)).as("error in %s", file.getName())
            .doesNotContain("CREATE UNIQUE INDEX ");
      }
    }
    assertThat(filesChecked).isNotEmpty();
  }
}
