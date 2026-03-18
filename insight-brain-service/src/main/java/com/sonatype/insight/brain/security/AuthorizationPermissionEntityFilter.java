/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.security.AuthorizationPermissionEntityFilterStrategy.EXECUTOR_FIXED;
import static com.sonatype.insight.brain.security.AuthorizationPermissionEntityFilterStrategy.PARALLEL;
import static com.sonatype.insight.brain.security.AuthorizationPermissionEntityFilterStrategy.SEQUENTIAL;
import static com.sonatype.insight.brain.security.AuthorizationPermissionEntityFilterStrategy.getStrategyFromEnv;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * High-performance entity filtering service for authorization permission checks with adaptive processing strategies.
 * <p>
 * This component is responsible for filtering collections of entities based on user permissions, employing different
 * processing strategies optimized for various dataset sizes and performance requirements. It has been separated from
 * {@link AuthorizationChecker} to isolate filtering concerns and enable independent optimization.
 *
 * <h3>Processing Strategies</h3>
 * <ul>
 * <li><strong>SEQUENTIAL</strong> - Single-threaded processing with small cache (256 entries).
 * Optimal for small datasets (&lt;10k entities)</li>
 * <li><strong>PARALLEL</strong> - Parallel stream processing with large concurrent cache.
 * Optimal for medium datasets (10k-1M entities)</li>
 * <li><strong>EXECUTOR_FIXED</strong> - Custom thread pool with batch processing and thread-local caches.
 * Optimal for very large datasets (&gt;1M entities)</li>
 * <li><strong>AUTO</strong> - Automatically selects optimal strategy based on dataset size thresholds</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <p>
 * Strategy selection is controlled via the {@code AUTHORIZATION_PERMISSION_ENTITY_FILTER_STRATEGY} environment
 * variable. If not set or invalid, defaults to SEQUENTIAL strategy.
 *
 * @see AuthorizationChecker
 * @see AuthorizationPermissionEntityFilterStrategy
 */
public class AuthorizationPermissionEntityFilter
{
  private static final Logger log = LoggerFactory.getLogger(AuthorizationPermissionEntityFilter.class);

  // AUTO strategy
  private static final int AUTO_SEQUENTIAL_THRESHOLD = 10_000;

  private static final int AUTO_PARALLEL_THRESHOLD = 1_000_000;

  // SEQUENTIAL strategy
  private static final int SEQUENTIAL_CACHE_SIZE = 256;

  // PARALLEL strategy
  private static final int PARALLEL_CACHE_MULTIPLIER = 2;

  private static final int PARALLEL_CACHE_MIN_SIZE = 1024;

  // EXECUTOR_FIXED strategy
  private static final int THREAD_LOCAL_CACHE_SIZE = 256;

  private static final double ESTIMATED_PASS_RATE = 0.5; // Assumes 50% of entities pass filtering

  private static final int MIN_BATCH_RESULT_SIZE = 8;

  private static final int GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS = 60;

  private static final int FORCED_SHUTDOWN_TIMEOUT_SECONDS = 5;

  // pooling configuration
  private static final int INITIAL_UNCACHED_ARRAY_SIZE = 64;

  /**
   * Filters entities based on user permissions using the configured processing strategy.
   * <p>
   * This is the main entry point for permission-based entity filtering. The method selects the processing strategy
   * based on the input collection type and configured environment settings.
   * <p>
   * <strong>Processing Logic:</strong>
   * <ol>
   * <li>Creates output collection matching input type (Set → LinkedHashSet, List → ArrayList)</li>
   * <li>For non-List collections, falls back to SEQUENTIAL strategy</li>
   * <li>For List collections, applies the configured strategy (AUTO, PARALLEL, EXECUTOR_FIXED, or SEQUENTIAL)</li>
   * <li>Each entity is checked against user's authorized context IDs through the resolver</li>
   * </ol>
   * <p>
   * <strong>Performance Notes:</strong>
   * <ul>
   * <li>Hierarchical context inheritance is leveraged (parent permissions apply to children)</li>
   * <li>Strategy selection is optimized for different dataset sizes</li>
   * </ul>
   *
   * @param entities the entities to filter (must not be null)
   * @param resolver context ID resolver for mapping entities to authorization contexts (must not be null)
   * @param userContextIds set of context IDs the user has access to (must not be null)
   * @param <T> the type of entities being filtered
   * @return filtered collection containing only entities the user has permission to access, preserving the input he
   *         input collection type semantics
   * @throws RuntimeException if parallel processing fails during strategy execution
   */
  <T> Collection<T> filterWithPermissionCheck(
      final Iterable<T> entities,
      final ContextIdResolver<? super T> resolver,
      final Set<String> userContextIds)
  {
    Collection<T> filtered = newCollection(entities);

    if (!(entities instanceof List<T> entitiesList)) {
      log.trace("Using default strategy: {}", SEQUENTIAL.toString().toLowerCase());
      sequentiallyFilter(entities, resolver, userContextIds, filtered);
      return filtered;
    }

    switch (getStrategyFromEnv()) {
      case AUTO -> autoFilter(entitiesList, resolver, userContextIds, filtered);
      case PARALLEL -> parallelFilter(entitiesList, resolver, userContextIds, filtered);
      case EXECUTOR_FIXED -> executorFixedFilter(entitiesList, resolver, userContextIds, filtered);
      default -> sequentiallyFilter(entities, resolver, userContextIds, filtered);
    }

    return filtered;
  }

  /**
   * Creates a new collection of the same type as the prototype for authorization permission entity filtering.
   * <p>
   * This utility method preserves the collection type semantics when filtering entities based on user permissions. It
   * ensures that if the input entities are a Set (maintaining uniqueness), the filtered result is also a Set, and if
   * the input is a List (maintaining order), the filtered result is also a List.
   *
   * @param prototype the prototype object used to determine the collection type
   * @param <T> the type of elements in the collection
   * @return a new LinkedHashSet if the prototype is a Set, otherwise a new ArrayList
   */
  public static <T> Collection<T> newCollection(Object prototype) {
    if (prototype instanceof Set) {
      return new LinkedHashSet<>();
    }
    else {
      return new ArrayList<>();
    }
  }

  /**
   * Automatically selects the optimal filtering strategy based on dataset size thresholds.
   * <p>
   * This adaptive strategy dynamically chooses between SEQUENTIAL, PARALLEL, and EXECUTOR_FIXED strategies based on the
   * current filtered collection size, optimizing for different performance characteristics at various scales.
   * <p>
   * <strong>Strategy Selection Thresholds:</strong>
   * <ul>
   * <li><strong>&lt; 10,000 entities</strong> → SEQUENTIAL (single-threaded, minimal overhead)</li>
   * <li><strong>10,000 - 1,000,000 entities</strong> → PARALLEL (parallel streams, good CPU utilization)</li>
   * <li><strong>&gt; 1,000,000 entities</strong> → EXECUTOR_FIXED (custom thread pool, batch processing)</li>
   * </ul>
   * <p>
   * The selection is based on {@code filtered.size()} rather than {@code entities.size()} to account
   * for collections that may have been pre-filtered or partially processed.
   *
   * @param entities the entities to filter
   * @param resolver context ID resolver for entities
   * @param userContextIds set of context IDs the user has access to
   * @param filtered output collection for filtered results
   * @param <T> the type of entities being filtered
   */
  private <T> void autoFilter(
      final List<T> entities,
      final ContextIdResolver<? super T> resolver,
      final Set<String> userContextIds,
      final Collection<T> filtered)
  {
    int entitiesSize = entities.size();

    if (entitiesSize < AUTO_SEQUENTIAL_THRESHOLD) {
      log.trace("Auto strategy chose to use {} strategy", SEQUENTIAL.toString().toLowerCase());
      sequentiallyFilter(entities, resolver, userContextIds, filtered);
    }
    else if (entitiesSize < AUTO_PARALLEL_THRESHOLD) {
      log.trace("Auto strategy chose to use {} strategy", PARALLEL.toString().toLowerCase());
      parallelFilter(entities, resolver, userContextIds, filtered);
    }
    else {
      log.trace("Auto strategy chose to use {} strategy", EXECUTOR_FIXED.toString().toLowerCase());
      executorFixedFilter(entities, resolver, userContextIds, filtered);
    }
  }

  /**
   * Filters entities using parallel stream processing with concurrent permission caching.
   * <p>
   * This strategy leverages Java's parallel streams to distribute entity processing across multiple threads, making it
   * ideal for medium-sized datasets (10k-1M entities) where CPU-bound processing can benefit from parallelization
   * without excessive thread management overhead.
   * <p>
   * <strong>Implementation Details:</strong>
   * <ul>
   * <li><strong>Concurrent Cache</strong> - Uses {@code ConcurrentHashMap} sized at 2x entity count (min 1024)</li>
   * <li><strong>Parallel Streams</strong> - Automatically utilizes {@code ForkJoinPool.commonPool()}</li>
   * <li><strong>Thread Safety</strong> - All operations are thread-safe with minimal contention</li>
   * </ul>
   * <p>
   * <strong>Performance Characteristics:</strong>
   * <ul>
   * <li>Optimal for CPU-intensive permission checks with moderate concurrency</li>
   * <li>Lower thread management overhead compared to EXECUTOR_FIXED strategy</li>
   * <li>Cache size pre-allocated to minimize hash table resizing</li>
   * </ul>
   *
   * @param entities the entities to filter
   * @param resolver context ID resolver for entities
   * @param userContextIds set of context IDs the user has access to
   * @param filtered output collection for filtered results
   * @param <T> the type of entities being filtered
   */
  private <T> void parallelFilter(
      final List<T> entities,
      final ContextIdResolver<? super T> resolver,
      final Set<String> userContextIds,
      final Collection<T> filtered)
  {
    Map<String, Boolean> permitsByContextId = new ConcurrentHashMap<>(
        Math.max(PARALLEL_CACHE_MIN_SIZE, entities.size() * PARALLEL_CACHE_MULTIPLIER));

    // Use parallel stream to process entities concurrently
    // Collect results first, then add all to filtered collection (avoids concurrent modification)
    List<T> results = entities
        .parallelStream()
        .filter(entity -> isUserHavingAnyRoleInAnyContextPooled(
            userContextIds,
            resolver.resolveContextIds(entity),
            permitsByContextId))
        .toList();

    // Add all results to the output collection (thread-safe since single-threaded here)
    filtered.addAll(results);
  }

  /**
   * Filters entities using a custom ExecutorService with fixed thread pool and batch processing.
   *
   * <p>
   * This strategy is optimal for very large datasets (>1M entities) where thread contention
   * becomes a bottleneck with parallelStream(). Uses manual batching to control granularity and thread-local caches to
   * minimize synchronization overhead.
   *
   * <p>
   * Performance characteristics:
   * <ul>
   * <li>Batch size: entities.size() / availableProcessors</li>
   * <li>Thread-local permission caches reduce map contention</li>
   * <li>Pre-allocated result collections minimize GC pressure</li>
   * </ul>
   *
   * @param entities the entities to filter
   * @param resolver resolves context IDs for each entity
   * @param userContextIds set of context IDs the user has access to
   * @param filtered output collection for entities that pass permission check
   */
  private <T> void executorFixedFilter(
      final List<T> entities,
      final ContextIdResolver<? super T> resolver,
      final Set<String> userContextIds,
      final Collection<T> filtered)
  {
    final int processorCount = Runtime.getRuntime().availableProcessors();
    final int batchSize = Math.max(1, entities.size() / processorCount);
    final int expectedBatches = (entities.size() + batchSize - 1) / batchSize;

    ExecutorService executor = newFixedThreadPool(processorCount);
    List<Future<List<T>>> futures = new ArrayList<>(expectedBatches);

    try {
      // Submit work batches to thread pool
      submitBatchTasks(entities, resolver, userContextIds, batchSize, executor, futures);

      // Collect results from all worker threads
      collectFilteredResults(futures, filtered);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to execute parallel entity filtering", e);
    }
    finally {
      shutdownExecutor(executor);
    }
  }

  private <T> void submitBatchTasks(
      final List<T> entities,
      final ContextIdResolver<? super T> resolver,
      final Set<String> userContextIds,
      final int batchSize,
      final ExecutorService executor,
      final List<Future<List<T>>> futures)
  {
    for (int i = 0; i < entities.size(); i += batchSize) {
      final int batchStart = i;
      final int batchEnd = Math.min(i + batchSize, entities.size());

      Future<List<T>> future =
          executor.submit(() -> processBatch(entities, resolver, userContextIds, batchStart, batchEnd));

      futures.add(future);
    }
  }

  private <T> List<T> processBatch(
      final List<T> entities,
      final ContextIdResolver<? super T> resolver,
      final Set<String> userContextIds,
      final int start,
      final int end)
  {
    // Pre-allocate result list with estimated size based on historical pass rate
    final int estimatedResults = (int) ((end - start) * ESTIMATED_PASS_RATE);
    final List<T> batchResults = new ArrayList<>(Math.max(MIN_BATCH_RESULT_SIZE, estimatedResults));

    // Thread-local cache to minimize synchronization with shared ConcurrentHashMap
    final Map<String, Boolean> threadCache = new HashMap<>(THREAD_LOCAL_CACHE_SIZE);

    // Process entities in this batch
    for (int j = start; j < end; j++) {
      T entity = entities.get(j);
      Iterable<String> contextIds = resolver.resolveContextIds(entity);

      if (isUserHavingAnyRoleInAnyContextPooled(userContextIds, contextIds, threadCache)) {
        batchResults.add(entity);
      }
    }

    return batchResults;
  }

  /**
   * Collects results from all worker threads and adds them to the filtered collection. Uses parallel collection to
   * reduce latency when some futures complete earlier than others.
   */
  private <T> void collectFilteredResults(
      final List<Future<List<T>>> futures,
      final Collection<T> filtered)
  {
    // Collect all batch results in parallel, then add to output collection sequentially
    // This avoids thread-safety issues with the output collection while still parallelizing the .get() calls
    futures.parallelStream()
        .map(future -> {
          try {
            return future.get();
          }
          catch (Exception e) {
            throw new RuntimeException("Failed to collect batch results", e);
          }
        })
        .forEach(filtered::addAll);
  }

  private boolean isUserHavingAnyRoleInAnyContextPooled(
      final Set<String> userContextIds,
      final Iterable<String> contextIds,
      final Map<String, Boolean> permitsByContextId)
  {
    String[] tempUncached = new String[INITIAL_UNCACHED_ARRAY_SIZE]; // preallocate assuming a reasonable size
    int uncachedCount = 0;

    for (String contextId : contextIds) {
      Boolean permit = permitsByContextId.get(contextId);
      if (permit != null) {
        if (permit) {
          // mark all prior uncached as true
          for (int j = 0; j < uncachedCount; j++) {
            permitsByContextId.put(tempUncached[j], true);
          }
          return true;
        }
        break;
      }

      // store in array (resize if needed) with defensive array bounds checking
      if (uncachedCount >= tempUncached.length) {
        tempUncached = Arrays.copyOf(tempUncached, Math.max(tempUncached.length * 2, uncachedCount + 1));
      }
      tempUncached[uncachedCount++] = contextId;
    }

    // Walk down the hierarchy (in reverse)
    for (int i = uncachedCount - 1; i >= 0; i--) {
      String contextId = tempUncached[i];
      boolean permit = userContextIds.contains(contextId);
      permitsByContextId.put(contextId, permit);
      if (permit) {
        for (i = i - 1; i >= 0; i--) {
          permitsByContextId.put(tempUncached[i], true);
        }
        return true;
      }
    }

    return false;
  }

  /**
   * Properly shuts down the executor service with timeout handling and defensive exception management.
   */
  private void shutdownExecutor(final ExecutorService executor) {
    try {
      executor.shutdown();
    }
    catch (Exception e) {
      log.error("Executor shutdown() failed, forcing termination: {}", e.getMessage());
    }

    try {
      if (!executor.awaitTermination(GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS, SECONDS)) {
        executor.shutdownNow();

        // Wait briefly for tasks to respond to cancellation
        if (!executor.awaitTermination(FORCED_SHUTDOWN_TIMEOUT_SECONDS, SECONDS)) {
          // Log warning about tasks that didn't terminate gracefully
          log.warn("Thread pool did not terminate gracefully");
        }
      }
    }
    catch (Exception e) {
      // Handle InterruptedException immediately to preserve interrupt status
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      log.error("Executor termination failed: {}", e.getMessage());

      // Always force shutdown regardless of exception type for proper cleanup
      executor.shutdownNow();
    }
  }

  /**
   * Filters entities using single-threaded processing with optimized permission caching.
   * <p>
   * This is the default and most straightforward filtering strategy, optimized for small to medium datasets where the
   * overhead of parallelization would outweigh its benefits. It processes entities sequentially in a single thread with
   * efficient caching to minimize redundant permission checks.
   * <p>
   * <strong>Implementation Details:</strong>
   * <ul>
   * <li><strong>Sequential Processing</strong> - Single-threaded iteration with no synchronization overhead</li>
   * <li><strong>Compact Cache</strong> - Uses {@code HashMap} with 256-entry initial capacity</li>
   * <li>
   * <strong>Hierarchical Optimization</strong> - Leverages context inheritance for efficient permission propagation
   * </li>
   * </ul>
   * <p>
   * <strong>Performance Characteristics:</strong>
   * <ul>
   * <li>Minimal memory footprint and CPU overhead</li>
   * <li>Optimal for datasets under 10,000 entities</li>
   * <li>No thread contention or synchronization costs</li>
   * <li>Predictable, linear performance scaling</li>
   * </ul>
   *
   * @param entities the entities to filter
   * @param resolver context ID resolver for entities
   * @param userContextIds set of context IDs the user has access to
   * @param filtered output collection for filtered results
   * @param <T> the type of entities being filtered
   */
  private <T> void sequentiallyFilter(
      final Iterable<T> entities,
      final ContextIdResolver<? super T> resolver,
      final Set<String> userContextIds,
      final Collection<T> filtered)
  {
    Map<String, Boolean> permitsByContextId = new HashMap<>(SEQUENTIAL_CACHE_SIZE);

    for (T entity : entities) {
      Iterable<String> contextIds = resolver.resolveContextIds(entity);
      if (isUserHavingAnyRoleInAnyContext(userContextIds, contextIds, permitsByContextId)) {
        filtered.add(entity);
      }
    }
  }

  /**
   * Determines if a user has permission for any of the provided contexts using hierarchical inheritance and caching.
   * <p>
   * This method implements the core authorization logic with two key optimizations:
   * <ol>
   * <li><strong>Permission Caching</strong> - Caches results to avoid redundant permission checks</li>
   * <li><strong>Hierarchical Inheritance</strong> - Parent context permissions automatically apply to child contexts
   * </li>
   * </ol>
   * <p>
   * <strong>Algorithm:</strong>
   * <ol>
   * <li><strong>Cache Consultation (Ascending)</strong> - Walks up the context hierarchy checking cache</li>
   * <li><strong>Early Success</strong> - Returns true immediately if any cached parent has permission</li>
   * <li><strong>Database Query (Descending)</strong> - Queries uncached contexts from most specific to least specific
   * </li>
   * <li><strong>Inheritance Propagation</strong> - When permission found, marks all child contexts as permitted</li>
   * </ol>
   * <p>
   * The hierarchical approach means that if a user has permission on a parent context (e.g., organization),
   * they automatically have permission on all child contexts (e.g., applications within that organization).
   *
   * @param userContextIds set of context IDs the user has explicit access to
   * @param contextIds context IDs to check for the current entity (ordered from child to parent)
   * @param permitsByContextId cache of previously computed permission results
   * @return true if the user has permission for any of the contexts, false otherwise
   */
  private boolean isUserHavingAnyRoleInAnyContext(
      Set<String> userContextIds,
      Iterable<String> contextIds,
      Map<String, Boolean> permitsByContextId)
  {
    List<String> uncachedContextIds = new ArrayList<>();

    // consult the cache first (walking up the hierarchy)
    for (String contextId : contextIds) {
      Boolean permit = permitsByContextId.get(contextId);
      if (permit != null) {
        if (permit) {
          // due to inheritance, the permit also implies to all child contexts
          for (String childId : uncachedContextIds) {
            permitsByContextId.put(childId, true);
          }
          return true;
        }
        // this context and none of its ancestors permit access
        break;
      }
      uncachedContextIds.add(contextId);
    }

    // consult the database about the uncached contexts (walking down the hierarchy)
    for (int i = uncachedContextIds.size() - 1; i >= 0; i--) {
      String contextId = uncachedContextIds.get(i);
      boolean permit = userContextIds.contains(contextId);
      permitsByContextId.put(contextId, permit);
      if (permit) {
        // due to inheritance, the permit also implies to all child contexts
        for (i--; i >= 0; i--) {
          String childId = uncachedContextIds.get(i);
          permitsByContextId.put(childId, true);
        }
        return true;
      }
    }

    return false;
  }
}
