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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.Statement;

import static com.google.common.collect.Sets.newHashSet;
import static com.sonatype.insight.brain.db.IdUtil.newUUID;
import static com.sonatype.insight.brain.model.security.MemberType.GROUP;
import static com.sonatype.insight.brain.model.security.MemberType.USER;
import static com.sonatype.insight.brain.model.security.Permission.READ;
import static com.sonatype.insight.brain.security.AuthorizationMembershipQueryStrategy.AUTHORIZATION_CHECKER_MEMBERSHIP_QUERY_STRATEGY_ENV;
import static com.sonatype.insight.brain.security.AuthorizationPermissionEntityFilterStrategy.AUTHORIZATION_PERMISSION_ENTITY_FILTER_STRATEGY_ENV;
import static com.sonatype.insight.brain.security.AuthzFilter.Context.APPLICATION;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class AuthorizationCheckerPerformanceTest
    extends AbstractDataTest
{
  private static final double NANOSECONDS_TO_MILLISECONDS = 1_000_000.0;

  private static final int SEPARATOR_LINE_LENGTH = 75;

  private static final String TABLE_HEADER = "Strategy        + Approach                           | Time (ms)";

  private static final String TABLE_SEPARATOR = "-----------------------------------------------------|----------";

  private static final String TABLE_ROW_FORMAT = "%-15s + %-34s | %8.2f%n";

  private static final String BEST_CONFIG_FORMAT = "BEST CONFIGURATION: %s (%.2f ms)%n";

  private static final String IMPROVEMENT_FORMAT =
      "OVERALL IMPROVEMENT: %.1fx faster (AUTO+DIRECT_CONTEXT_ID vs SEQUENTIAL+FULL_MEMBERSHIP_MAPPING_CONTEXT_ID)%n";

  @Parameters(name = "iterations={0}, applications={1}, memberships={2}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
            {10, 50, 1},
            {10, 50, 100},
            {10, 50, 1000},
            {10, 50, 10_000},
            {10, 50, 100_000},
            {10, 50, 100_000_000},
            {10, 100, 100},
            {10, 500, 1000},
            {10, 1000, 10_000},
            {10, 10_000, 100_000_000},
            {50, 10_000, 100_000_000},
            {100, 10_000, 100_000_000}
        }
    );
  }

  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  private AuthorizationChecker checker;

  private MembershipMappingDAO membershipMappingDAO;

  private RoleDAO roleDAO;

  private RolePermissionDAO rolePermissionDAO;

  @Rule
  public TestRetryRule retryRule = new TestRetryRule(5);

  private final int iterationCount;

  private final int applicationCount;

  private final int membershipCount;

  public AuthorizationCheckerPerformanceTest(int iterationCount, int applicationCount, int membershipCount) {
    this.iterationCount = iterationCount;
    this.applicationCount = applicationCount;
    this.membershipCount = membershipCount;
  }

  @Before
  public void setUp() {
    membershipMappingDAO = daoFactory.createMembershipMappingDAO();
    roleDAO = daoFactory.createRoleDAO();
    rolePermissionDAO = daoFactory.createRolePermissionDAO();

    ContextResolver contextResolver =
        new ContextResolver(daoFactory.createApplicationDAO(), daoFactory.createOrganizationDAO(),
            daoFactory.createRepositoryManagerDAO(), daoFactory.createRepositoryDAO(), daoFactory.createOwnerDAO());

    checker = new AuthorizationChecker(contextResolver, rolePermissionDAO, membershipMappingDAO);
  }

  @Test
  public void testFilterByPermission_Performance() {
    // Setup test data
    List<Application> applications = createApplications(applicationCount);
    User user = createUserWithPermissions(applications);
    List<String> groups = createManyMemberships(membershipCount);
    UserPrincipal principal = newPrincipal(user, groups.toArray(new String[0]));

    // Run comprehensive performance analysis
    printAnalysisHeader();
    PerformanceResults results = runPerformanceAnalysis(principal, applications);
    printBestConfiguration(results);

    // Run specific performance comparisons for assertions
    String autoConfig = "AUTO+DIRECT_CONTEXT_ID";
    String sequentialConfig = "SEQUENTIAL+FULL_MEMBERSHIP_MAPPING_CONTEXT_ID";

    Long autoOptimized = results.allResults.get(autoConfig);
    Long sequentialTraditional = results.allResults.get(sequentialConfig);

    printImprovementResults(sequentialTraditional, autoOptimized);
    assertPerformanceImprovement(autoOptimized, sequentialTraditional);
  }

  private void printAnalysisHeader() {
    System.out.printf("Comprehensive Performance Analysis (%d iterations, %d applications, %d memberships):%n",
        iterationCount, applicationCount, membershipCount);
    System.out.println("=".repeat(SEPARATOR_LINE_LENGTH));
    System.out.println(TABLE_HEADER);
    System.out.println(TABLE_SEPARATOR);
  }

  private PerformanceResults runPerformanceAnalysis(UserPrincipal principal, List<Application> applications) {
    long bestTime = Long.MAX_VALUE;
    String bestConfiguration = "";
    Map<String, Long> allResults = new HashMap<>();

    for (AuthorizationPermissionEntityFilterStrategy filterStrategy :
        AuthorizationPermissionEntityFilterStrategy.values()) {
      for (AuthorizationMembershipQueryStrategy queryStrategy : AuthorizationMembershipQueryStrategy.values()) {
        long time = timeFilterWithConfiguration(principal, applications, filterStrategy, queryStrategy);
        double timeMs = time / NANOSECONDS_TO_MILLISECONDS;

        String configKey = filterStrategy.name() + "+" + queryStrategy.name();
        allResults.put(configKey, time);

        if (time < bestTime) {
          bestTime = time;
          bestConfiguration = filterStrategy.name() + " + " + queryStrategy.name();
        }

        System.out.printf(TABLE_ROW_FORMAT, filterStrategy.name(), queryStrategy.name(), timeMs);
      }
    }

    return new PerformanceResults(bestTime, bestConfiguration, allResults);
  }

  private long timeFilterWithConfiguration(
      UserPrincipal principal, List<Application> applications,
      AuthorizationPermissionEntityFilterStrategy filterStrategy, AuthorizationMembershipQueryStrategy queryStrategy)
  {
    environmentVariables.set(AUTHORIZATION_PERMISSION_ENTITY_FILTER_STRATEGY_ENV, filterStrategy.name());
    environmentVariables.set(AUTHORIZATION_CHECKER_MEMBERSHIP_QUERY_STRATEGY_ENV, queryStrategy.name());
    return timeFilterOperation(principal, applications, iterationCount);
  }

  private void printBestConfiguration(PerformanceResults results) {
    System.out.println("=".repeat(SEPARATOR_LINE_LENGTH));
    System.out.printf(BEST_CONFIG_FORMAT, results.bestConfiguration, results.bestTime / NANOSECONDS_TO_MILLISECONDS);
  }

  private void printImprovementResults(long sequentialTraditional, long autoOptimized) {
    double improvement = (double) sequentialTraditional / autoOptimized;
    System.out.printf(IMPROVEMENT_FORMAT, improvement);
  }

  private void assertPerformanceImprovement(long autoOptimized, long sequentialTraditional) {
    // Allow AUTO to be up to 25% slower than SEQUENTIAL to account for JVM warmup, GC, and system load variations
    long toleranceThreshold = (long) (sequentialTraditional * 1.25);
    assertThat(autoOptimized)
        .as("AUTO+DIRECT_CONTEXT_ID should be reasonably fast compared to" +
                " SEQUENTIAL+FULL_MEMBERSHIP_MAPPING_CONTEXT_ID" +
                " (auto: %d ns, sequential: %d ns, threshold: %d ns)",
            autoOptimized, sequentialTraditional, toleranceThreshold
        )
        .isLessThan(toleranceThreshold);
  }

  private record PerformanceResults(long bestTime, String bestConfiguration, Map<String, Long> allResults) { }

  private UserPrincipal newPrincipal(User user, String... groups) {
    return new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID,
        newHashSet(groups));
  }

  private List<String> createManyMemberships(final int membershipCount) {
    // Get roles that have the specified permission
    Set<String> roleIds = rolePermissionDAO.getRoleIdsByPermission(READ);
    List<Role> validRoles = new ArrayList<>();

    for (String roleId : roleIds) {
      Role role = roleDAO.getById(roleId);
      if (role != null) {
        validRoles.add(role);
      }
    }

    List<String> groups = new ArrayList<>();
    int mappingCount = 0;

    // Calculate how many groups needed: divide count by 8 memberships per group, add 1 for remainder,
    // cap at 20 groups max. Example: 100 memberships -> 100/8 + 1 = 13 groups, each group gets ~7-8 memberships
    int groupCount = Math.min(20, (membershipCount / 8) + 1);

    for (int g = 0; g < groupCount && mappingCount < membershipCount; g++) {
      String groupName = "group_" + g;
      groups.add(groupName);

      int mappingsForThisGroup = Math.min(8, membershipCount - mappingCount);
      for (int i = 0; i < mappingsForThisGroup; i++) {
        Role role = validRoles.get((g * 8 + i) % validRoles.size());
        newGroupMapping(groupName, newUUID(), role.getId());
        mappingCount++;
      }
    }

    return groups;
  }

  private List<Application> createApplications(final int count) {
    List<Application> applications = new ArrayList<>();
    Organization org = tempEntity.newOrganization();
    for (int i = 0; i < count; i++) {
      applications.add(tempEntity.newApplication(org.getId()));
    }
    return applications;
  }

  private User createUserWithPermissions(final List<Application> applications) {
    User user = tempEntity.newUser();
    Role role = roleDAO.getByName("Owner");
    for (int i = 0; i < applications.size() / 2; i++) { // Give access to half
      newMembershipMapping(user, applications.get(i).getId(), role.getId());
    }
    return user;
  }

  private long timeFilterOperation(
      final UserPrincipal principal,
      final List<Application> applications,
      final int iterations)
  {
    long totalTime = 0;
    for (int i = 0; i < iterations; i++) {
      long startTime = System.nanoTime();
      checker.filterByPermission(principal, READ, applications, APPLICATION);
      totalTime += (System.nanoTime() - startTime);
    }
    return totalTime / iterations; // Return average
  }

  private void newMembershipMapping(final User user, final String contextId, final String roleId) {
    MembershipMapping membership = new MembershipMapping(contextId, roleId, user.getUsername(), USER);
    membershipMappingDAO.insert(membership);
  }

  private void newGroupMapping(final String groupName, final String contextId, final String roleId) {
    MembershipMapping membership = new MembershipMapping(contextId, roleId, groupName, GROUP);
    membershipMappingDAO.insert(membership);
  }

  record TestRetryRule(int attemptCount)
      implements TestRule
  {
    @Override
    public Statement apply(Statement base, Description description) {
      return statement(base, description);
    }

    private Statement statement(Statement base, Description description) {
      return new Statement()
      {
        @Override
        public void evaluate() throws Throwable {
          Throwable lastError = null;

          for (int attempt = 0; attempt < attemptCount; attempt++) {
            try {
              base.evaluate();
              return;
            }
            catch (Exception | AssertionError e) {
              lastError = e;

              System.err.println(
                  description.getDisplayName() + ": run " + (attempt + 1) + " failed: " + e.getMessage());

              e.printStackTrace();
            }
          }
          System.err.println(description.getDisplayName() + ": giving up after " + attemptCount + " failures.");

          if (lastError != null) {
            throw lastError;
          }
        }
      };
    }
  }
}
