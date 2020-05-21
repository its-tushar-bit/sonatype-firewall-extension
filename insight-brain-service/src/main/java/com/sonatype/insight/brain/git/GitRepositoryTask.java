/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Base class for tasks that involve repository cloning and SCM interactions.
 */
public abstract class GitRepositoryTask
{
  private static final Logger log = LoggerFactory.getLogger(GitRepositoryTask.class);

  private static final int HASH_LENGTH = 6;

  private static final int PUBLIC_ID_LENGTH = 25;

  private static final int BRANCH_LENGTH = 15;

  private static final String PATH_REGEX = "[^a-z0-9\\-]";

  private final FileCleaner fileCleaner;

  private final InsightConfig insightConfig;

  @Inject
  public GitRepositoryTask(
      final InsightConfig insightConfig,
      final FileCleaner fileCleaner)
  {
    this.insightConfig = insightConfig;
    this.fileCleaner = fileCleaner;
  }

  /**
   * Checks whether the checkout directory exists. If so, it is returned; otherwise it is created.
   */
  protected File getCheckoutDirectory(
      final String applicationPublicId,
      final String applicationId,
      final String branchName)
  {
    File checkoutDir = new File(
        insightConfig.getSourceControl().getCloneDirectory(),
        toSafePathname(applicationPublicId, branchName, applicationId));

    if (checkoutDir.exists()) {
      log.debug("Using existing directory for pull request task: {}", checkoutDir.getAbsolutePath());
    }
    else {
      boolean created = checkoutDir.mkdirs();
      log.debug("Created new directory for pull request task: {} result was {}", checkoutDir.getAbsolutePath(),
          created);
    }
    return checkoutDir;
  }

  /**
   * Deletes the checkout directory. To be used if the task fails.
   */
  protected void cleanDirectory(final File checkoutDir) {
    try {
      if (null != checkoutDir) {
        fileCleaner.delete(checkoutDir);
      }
    }
    catch (FileDeletionException ex) {
      log.error("Failed to remove checkout directory", ex);
    }
  }

  /**
   * Creates a human readable path name from an IQ application public ID and branch, along with a hash of the
   * application ID. This can be used for cloning as a path on (hopefully) all operating systems. The path takes the
   * form of:<BR><BR>
   * {@code <application public id>-<branch name>-<hash of application id>}<BR><BR>
   * The application public ID is truncated after {@link #PUBLIC_ID_LENGTH} and the branch name is truncated after
   * {@link #BRANCH_LENGTH}. The hash of the app ID is appended to reduce the probability of path name conflicts. The
   * application public ID and branch name are both lower-cased and sanitized to only include alphanumeric characters
   * and dashes.
   * @return url and filename safe string
   */
  protected String toSafePathname(
      final String applicationPublicId,
      final String branchName,
      final String applicationId)
  {
    String safeAppPublicId = applicationPublicId.toLowerCase(Locale.ENGLISH).replaceAll(PATH_REGEX, "");
    String safeBranchName = branchName.toLowerCase(Locale.ENGLISH).replaceAll(PATH_REGEX, "");

    safeAppPublicId = StringUtils.substring(safeAppPublicId, 0, PUBLIC_ID_LENGTH);
    safeBranchName = StringUtils.substring(safeBranchName, 0, BRANCH_LENGTH);

    return String.join("-", safeAppPublicId, safeBranchName, truncatedHashOf(applicationId));
  }

  /**
   * Creates a truncated hash for the given input which is safe to use in URLs and filenames in the following way:
   * Uses RFC 4648 base64 format for URL and filenames which replaces '+' with '-' and '/' with '_' and removes padding.
   * Only uses lower case characters because not all file systems are case sensitive.
   * @param input input
   * @return hash value
   */
  protected String truncatedHashOf(final String input) {
    try {
      byte[] hash = Base64.getUrlEncoder().encode(MessageDigest.getInstance("SHA1").digest(input.getBytes(UTF_8)));
      return new String(hash, UTF_8).replaceAll("=", "").substring(0, HASH_LENGTH).toLowerCase();
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to create SHA1 hash for checkout directory.", e);
    }
  }
}

