/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.sonatype.nexus.scm.SourceControlProvider;

public final class PullRequestSizeLimit
{
  // GitHub caps issue/PR comment and PR description bodies at 65,536 characters.
  private static final int DEFAULT_MAX_CHARS = 65536;

  // GitLab caps notes and descriptions at ~1,000,000 characters.
  private static final int GITLAB_MAX_CHARS = 1_000_000;

  // Azure DevOps caps PR discussion comments at 150,000 characters.
  private static final int AZURE_COMMENT_MAX_CHARS = 150_000;

  // Azure DevOps caps PR descriptions at 4,000 characters (far below its comment limit).
  private static final int AZURE_DESCRIPTION_MAX_CHARS = 4_000;

  // Bitbucket Server/Data Center rejects both comments and descriptions over 32,768 characters (BSERV-14135).
  private static final int BITBUCKET_MAX_CHARS = 32_768;

  private static final String COMMENT_PROPERTY_PREFIX = "insight.scm.pullRequest.maxCommentChars.";

  private static final String DESCRIPTION_PROPERTY_PREFIX = "insight.scm.pullRequest.maxDescriptionChars.";

  public enum Notice
  {
    COMPONENTS_OMITTED("Some components were omitted to keep this comment within the size limit."),
    COMMENT_TRUNCATED("Comment truncated because it exceeded the size limit."),
    VIOLATIONS_OMITTED("Some policy violations were omitted to keep this description within the size limit."),
    DESCRIPTION_TRUNCATED("Description truncated because it exceeded the size limit.");

    private final String message;

    Notice(final String message) {
      this.message = message;
    }
  }

  @FunctionalInterface
  public interface CappedRenderer
  {
    String render(int cap) throws IOException;
  }

  public record CappedFit(int hiCap, CappedRenderer renderer)
  {
  }

  private PullRequestSizeLimit() {
  }

  public static int maxCommentChars(final SourceControlProvider provider) {
    final int defaultValue = switch (provider) {
      case GITLAB -> GITLAB_MAX_CHARS;
      case AZURE -> AZURE_COMMENT_MAX_CHARS;
      case BITBUCKET -> BITBUCKET_MAX_CHARS;
      default -> DEFAULT_MAX_CHARS;
    };
    return resolve(COMMENT_PROPERTY_PREFIX, provider, defaultValue);
  }

  public static int maxDescriptionChars(final SourceControlProvider provider) {
    final int defaultValue = switch (provider) {
      case GITLAB -> GITLAB_MAX_CHARS;
      case BITBUCKET -> BITBUCKET_MAX_CHARS;
      case AZURE -> AZURE_DESCRIPTION_MAX_CHARS;
      default -> DEFAULT_MAX_CHARS;
    };
    return resolve(DESCRIPTION_PROPERTY_PREFIX, provider, defaultValue);
  }

  private static int resolve(final String prefix, final SourceControlProvider provider, final int defaultValue) {
    // Integer.getInteger already falls back to defaultValue when the property is absent or unparseable; the guard
    // additionally ignores a configured but non-positive override.
    final int configured = Integer.getInteger(prefix + provider.name().toLowerCase(Locale.ROOT), defaultValue);
    return configured > 0 ? configured : defaultValue;
  }

  public static String footer(final Notice notice, final String reportUrl) {
    return "\n\n_" + notice.message + " [View the full report](" + reportUrl + ")._";
  }

  // Best-effort: rendered length is assumed non-decreasing in cap. Every returned candidate is re-checked against the
  // budget, so a non-monotonic edge can only under-select items, never exceed the budget.
  public static Optional<String> largestFittingRender(
      final int hiCap,
      final int budget,
      final CappedRenderer renderer) throws IOException
  {
    int lo = 0;
    int hi = hiCap;
    String best = null;
    while (lo <= hi) {
      final int mid = (lo + hi) >>> 1;
      final String candidate = renderer.render(mid);
      if (candidate.length() <= budget) {
        best = candidate;
        lo = mid + 1;
      }
      else {
        hi = mid - 1;
      }
    }
    return Optional.ofNullable(best);
  }

  // Tries each trim attempt in order, reserving room for the omitted-items footer, and appends that footer to the
  // first attempt whose largest fitting render is non-empty. Empty when even the smallest attempt overflows, i.e. the
  // caller should hard-truncate.
  public static Optional<String> largestFitWithNotice(
      final int budget,
      final String omittedFooter,
      final List<CappedFit> trimAttempts) throws IOException
  {
    final int trimmedBudget = Math.max(1, budget - omittedFooter.length());
    for (final CappedFit attempt : trimAttempts) {
      final Optional<String> fitted = largestFittingRender(attempt.hiCap(), trimmedBudget, attempt.renderer());
      if (fitted.isPresent()) {
        return Optional.of(fitted.get() + omittedFooter);
      }
    }
    return Optional.empty();
  }

  public static String truncate(final String body, final int maxChars, final String footer) {
    if (body == null || body.length() <= maxChars) {
      return body;
    }
    if (maxChars <= 0) {
      return "";
    }
    if (footer.length() >= maxChars) {
      return body.substring(0, surrogateSafeCut(body, maxChars));
    }
    final int room = maxChars - footer.length();
    final int newlineBoundary = body.lastIndexOf('\n', room);
    final int cut = newlineBoundary > 0 ? newlineBoundary : room;
    return body.substring(0, surrogateSafeCut(body, cut)) + footer;
  }

  private static int surrogateSafeCut(final String body, final int cut) {
    if (cut > 0 && Character.isHighSurrogate(body.charAt(cut - 1))) {
      return cut - 1;
    }
    return cut;
  }
}
