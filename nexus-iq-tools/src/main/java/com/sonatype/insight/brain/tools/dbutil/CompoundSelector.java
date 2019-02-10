/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class CompoundSelector
    extends AbstractSelector
{
  private final List<AbstractSelector> selectors;

  public CompoundSelector(AbstractSelector... selectors) {
    this.selectors = Arrays.asList(selectors);
  }

  @Override
  protected Map<String, List<String>> loadSelections(Connection connection, DbUtilParameters params) {
    return null;
  }

  @Override
  public Replacer buildReplacer(Connection conn, DbUtilParameters params) throws Exception {
    List<Replacer> replacers = new ArrayList<>();
    for (AbstractSelector sel : selectors) {
      replacers.add(sel.buildReplacer(conn, params));
    }

    return new CompoundReplacer(replacers);
  }
}
