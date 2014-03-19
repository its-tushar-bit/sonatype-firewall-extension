/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 1.9.2
 */
@Named
public class LegacyPathAdjuster
    implements AssetPathAdjuster
{
  private static final List<PathAdjustmentStrategy> strategies = Lists.<PathAdjustmentStrategy>newArrayList(
      new LegacyEclipsePathAdjustmentStrategy());

  @Override
  public String adjustPath(final String path, final String userAgent) {
    for (PathAdjustmentStrategy strategy : strategies) {
      if (strategy.shouldAdjustPath(path, userAgent)) {
        return strategy.adjustPath(path);
      }
    }

    return path;
  }

  /**
   * A strategy that will adjust a given path if necessary.
   */
  private interface PathAdjustmentStrategy
  {
    /**
     * @return true iff this path and userAgent require adjustment of the path
     */
    boolean shouldAdjustPath(String path, String userAgent);

    /**
     * @return the adjusted path
     */
    String adjustPath(String path);
  }


  /**
   * Versions of the eclipse plugin < 2.5.1 are considered legacy for purposes of delivering clm-server content
   */
  private static final class LegacyEclipsePathAdjustmentStrategy
      implements PathAdjustmentStrategy
  {
    private final Set<String> paths = Sets.newHashSet("eclipse/index.html");

    private final Pattern ECLIPSE_IDE_USER_AGENT = Pattern.compile(
        "^Sonatype_CLM_IDE_Eclipse/(\\d+)\\.(\\d+)\\.(\\d+).*");

    @Override
    public boolean shouldAdjustPath(final String path, final String userAgent) {
      checkNotNull(path);
      checkNotNull(userAgent);
      return paths.contains(path) && isLegacyVersion(userAgent);
    }

    @Override
    public String adjustPath(final String path) {
      return path.replace("eclipse/", "eclipse-legacy/");
    }

    private boolean isLegacyVersion(final String userAgent) {
      Matcher matcher = ECLIPSE_IDE_USER_AGENT.matcher(userAgent);
      if (matcher.matches() && matcher.groupCount() == 3) {
        int major = Integer.valueOf(matcher.group(1));
        int minor = Integer.valueOf(matcher.group(2));
        int point = Integer.valueOf(matcher.group(3));
        return major < 2 || major == 2 && minor < 5 || major == 2 && minor == 5 && point < 1;
      }
      return false;
    }
  }
}
