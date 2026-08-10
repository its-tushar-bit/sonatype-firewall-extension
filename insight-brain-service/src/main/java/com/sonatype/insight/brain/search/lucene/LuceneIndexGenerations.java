/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.sonatype.insight.brain.service.InsightWork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filesystem helpers for Lucene blue/green generations under {@code search/}.
 * <p>
 * Serving path stays {@code search/index}. A rebuild builds into
 * {@code search/generations/<uuid>}, then atomically renames green onto {@code index}
 * and retires the previous serving tree under {@code generations/} for deletion.
 */
final class LuceneIndexGenerations
{
  private static final Logger log = LoggerFactory.getLogger(LuceneIndexGenerations.class);

  /**
   * How long a generation must sit untouched before the sweep will delete it. Long enough that a slow rebuild on a
   * peer node is never mistaken for an orphan; short enough that abandoned trees do not outlive a day of rebuilds.
   */
  private static final Duration ORPHAN_MIN_AGE = Duration.ofHours(24);

  /**
   * Marks a copy staged beside its target by {@link #moveDirectory}. Named rather than anonymous so the sweep can
   * recognise one that a failed cutover left behind, since staging happens outside {@code generations/}.
   */
  private static final String STAGING_MARKER = ".incoming-";

  private final InsightWork insightWork;

  LuceneIndexGenerations(final InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  Path servingIndexPath() {
    return insightWork.getSearchIndexDir().toPath();
  }

  Path generationsRoot() {
    return insightWork.getSearchIndexGenerationsDir().toPath();
  }

  Path createBuildingGenerationDirectory() throws IOException {
    Path root = generationsRoot();
    Files.createDirectories(root);
    deleteOrphanedGenerations(root);
    deleteOrphanedStagingTrees();
    Path generation = root.resolve(UUID.randomUUID().toString());
    Files.createDirectory(generation);
    return generation;
  }

  /**
   * Nothing under {@code generations/} is ever served — serving is always {@code search/index} — so anything present
   * when a rebuild starts is left over from a rebuild that did not finish, and would otherwise accumulate across
   * restarts until the search volume fills.
   * <p>
   * Only generations untouched for {@link #ORPHAN_MIN_AGE} are removed. The rebuild flag that would otherwise prove a
   * generation is dead is per-process, so on a deployment where several nodes share the search volume it says nothing
   * about what a peer is doing. A generation being actively written keeps recent modification times, and treating a
   * peer's in-flight green tree as an orphan would delete the index out from under it mid-rebuild.
   */
  private static void deleteOrphanedGenerations(final Path root) {
    sweep(root, candidate -> true, "index generation");
  }

  /**
   * A copy staged by {@link #moveDirectory} lands beside its target rather than under {@code generations/}, so a
   * cutover that failed on the final rename leaves a tree the generations sweep cannot see. It is deliberately not
   * deleted at the point of failure — the serving path was untouched, which makes the staged tree the only copy of
   * the rebuilt index — but once it is a day old the rebuild that produced it is long gone.
   */
  private void deleteOrphanedStagingTrees() {
    Path servingParent = servingIndexPath().getParent();
    if (servingParent == null || !Files.isDirectory(servingParent)) {
      return;
    }
    sweep(servingParent, candidate -> candidate.getFileName().toString().contains(STAGING_MARKER),
        "staged cutover copy");
  }

  private static void sweep(final Path root, final Predicate<Path> selector, final String description) {
    Instant staleBefore = Instant.now().minus(ORPHAN_MIN_AGE);
    try (Stream<Path> entries = Files.list(root)) {
      entries.filter(selector).forEach(candidate -> {
        try {
          if (lastModified(candidate).isAfter(staleBefore)) {
            log.debug("Leaving recently modified Lucene {} at {}; it may belong to another node", description,
                candidate);
            return;
          }
          deleteRecursively(candidate);
          log.info("Deleted orphaned Lucene {} at {}", description, candidate);
        }
        catch (IOException e) {
          log.warn("Unable to delete orphaned Lucene {} at {}", description, candidate, e);
        }
      });
    }
    catch (IOException e) {
      // A sweep failure must never stop a rebuild; the worst case is the disk usage we already had.
      log.warn("Unable to list {} to delete orphaned Lucene {} entries", root, description, e);
    }
  }

  /**
   * Newest modification time anywhere in the tree. The directory's own timestamp only moves when entries are added or
   * removed, so a rebuild busy writing into an existing segment file would otherwise look untouched.
   */
  private static Instant lastModified(final Path root) throws IOException {
    try (Stream<Path> walk = Files.walk(root)) {
      return walk.map(path -> {
        try {
          return Files.getLastModifiedTime(path).toInstant();
        }
        catch (IOException e) {
          // Unreadable entries are treated as live so that a stat failure can never authorise a delete.
          return Instant.now();
        }
      }).max(Instant::compareTo).orElse(Instant.now());
    }
  }

  /**
   * Moves the current serving index aside (if present) and moves {@code greenPath} to the serving
   * location. Returns the retired path for later deletion, or {@code null} when there was no prior
   * serving tree.
   */
  Path cutover(final Path greenPath) throws IOException {
    Path serving = servingIndexPath();
    Path generations = generationsRoot();
    Files.createDirectories(generations);
    Files.createDirectories(serving.getParent());

    Path retired = null;
    if (Files.exists(serving)) {
      retired = generations.resolve("retired-" + UUID.randomUUID());
      moveDirectory(serving, retired);
    }
    try {
      moveDirectory(greenPath, serving);
    }
    catch (IOException e) {
      if (retired != null && Files.exists(retired) && !Files.exists(serving)) {
        try {
          moveDirectory(retired, serving);
          retired = null;
        }
        catch (IOException restoreFailure) {
          e.addSuppressed(restoreFailure);
        }
      }
      throw e;
    }
    return retired;
  }

  static void deleteRecursively(final Path root) throws IOException {
    if (root == null || !Files.exists(root)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(root)) {
      walk.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
    catch (RuntimeException e) {
      if (e.getCause() instanceof IOException ioException) {
        throw ioException;
      }
      throw e;
    }
  }

  /**
   * Moves a directory, preferring an atomic rename.
   * <p>
   * Where the two paths are on different filesystems a rename is impossible and the move degrades to copy-then-delete,
   * which would leave a half-written tree at {@code target} if it failed partway — and {@code target} is the serving
   * index. The copy therefore lands beside the target first and is put in place by a rename within one directory,
   * which is atomic. A failure of that final rename leaves the staged copy alone rather than deleting it: the serving
   * path was never touched, and the staged tree is the only remaining copy of the data. The path is logged so it can
   * be recovered by hand, and {@link #deleteOrphanedStagingTrees()} reclaims it once it is a day old.
   */
  private static void moveDirectory(final Path source, final Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
      return;
    }
    catch (AtomicMoveNotSupportedException e) {
      log.debug("Atomic rename unavailable moving {} to {}; staging the copy beside the target", source, target);
    }

    Path staging = target.resolveSibling(target.getFileName() + STAGING_MARKER + UUID.randomUUID());
    try {
      Files.move(source, staging, StandardCopyOption.REPLACE_EXISTING);
    }
    catch (IOException e) {
      try {
        deleteRecursively(staging);
      }
      catch (IOException cleanupFailure) {
        e.addSuppressed(cleanupFailure);
      }
      throw e;
    }
    try {
      Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
    }
    catch (IOException e) {
      log.error("Unable to move the staged Lucene index at {} onto {}. The staged tree holds the only copy of the "
          + "rebuilt index and has been left in place; move it onto {} by hand or rebuild the index.", staging, target,
          target, e);
      throw e;
    }
  }
}
