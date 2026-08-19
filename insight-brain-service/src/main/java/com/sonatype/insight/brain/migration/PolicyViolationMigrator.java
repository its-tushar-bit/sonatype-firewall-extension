/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.component.ComponentDisplayFilename;
import com.sonatype.insight.brain.db.PostIncrementalMigrator;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyViolationMigrator
    implements PostIncrementalMigrator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationMigrator.class);

  static final String SCHEMA = "insight_brain_ods";

  private static class Application
  {
    final String id;

    final String name;

    private Application(ResultSet resultSet) throws SQLException {
      this.id = resultSet.getString(1);
      this.name = resultSet.getString(2);
    }

    static PreparedStatement queryStatement(Connection connection) throws SQLException {
      return connection.prepareStatement("SELECT application_id, name FROM " + SCHEMA + ".application");
    }

    static Collection<Application> query(PreparedStatement statement) throws SQLException {
      Collection<Application> applications = new ArrayList<>();
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          applications.add(new Application(resultSet));
        }
      }
      return applications;
    }
  }

  /**
   * This is the comparator used to compare policy violations at the time this migrator was implemented.
   * It is important to keep it unchanged in order to not change the semantics of "same policy violation" when we
   * migrate from the old model to the new model.
   */
  private static class PolicyViolationComparator
      implements Comparator<PolicyViolationComparable>
  {
    public static final Comparator<PolicyViolationComparable> COMPARATOR = new PolicyViolationComparator();

    @Override
    public int compare(PolicyViolationComparable v1, PolicyViolationComparable v2) {
      // Policy id
      int result = v1.getPolicyId().compareTo(v2.getPolicyId());
      if (result != 0) {
        return result;
      }

      // Policy name
      String v1PolicyName = NameHelper.normalize(v1.getPolicyName());
      String v2PolicyName = NameHelper.normalize(v2.getPolicyName());
      result = v1PolicyName.compareTo(v2PolicyName);
      if (result != 0) {
        return result;
      }

      // Threat level
      result = v1.getThreatLevel() - v2.getThreatLevel();
      if (result != 0) {
        return result;
      }

      // Hash
      result = compareNullableStrings(v1.getHash(), v2.getHash());
      if (result != 0) {
        return result;
      }

      // Component identifier
      result = nullCheck(v1.getComponentIdentifier(), v2.getComponentIdentifier());
      if (result != 0) {
        return result;
      }
      if (v1.getComponentIdentifier() != null) {
        return v1.getComponentIdentifier().compareTo(v2.getComponentIdentifier());
      }

      return 0;
    }

    // null is greater than not null
    private int compareNullableStrings(String s1, String s2) {
      int result = nullCheck(s1, s2);
      if (result != 0) {
        return result;
      }
      if (s1 == null) {
        return 0;
      }
      return s1.compareTo(s2);
    }

    /**
     * Null objects are treated as infinitely large.
     */
    private int nullCheck(Object o1, Object o2) {
      if (o1 == null && o2 != null) {
        return 1;
      }
      else if (o1 != null && o2 == null) {
        return -1;
      }

      return 0;
    }
  }

  private static class PolicyEvaluation
  {
    final String id;

    final Timestamp time;

    final boolean reevaluation;

    final boolean forMonitoring;

    private PolicyEvaluation(ResultSet resultSet) throws SQLException {
      id = resultSet.getString(1);
      time = resultSet.getTimestamp(2);
      reevaluation = resultSet.getBoolean(3);
      forMonitoring = resultSet.getBoolean(4);
    }

    static PreparedStatement queryStatement(Connection connection) throws SQLException {
      return connection.prepareStatement("SELECT policy_evaluation_id, time, reevaluation, for_monitoring FROM "
          + SCHEMA + ".policy_evaluation WHERE application_id = ?1 AND stage_type_id = ?2 AND for_obsolete_scan=false");
    }

    static List<PolicyEvaluation> query(
        PreparedStatement statement,
        String applicationId,
        String stageTypeId) throws SQLException
    {
      statement.setString(1, applicationId);
      statement.setString(2, stageTypeId);
      List<PolicyEvaluation> evaluations = new ArrayList<>();
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          evaluations.add(new PolicyEvaluation(resultSet));
        }
      }
      // NOTE: sort outside of H2 database to reduce time during which database is locked by current thread
      evaluations.sort((e1, e2) -> e1.time.compareTo(e2.time));
      return evaluations;
    }
  }

  private static class NewPolicyViolation
      implements PolicyViolationComparable
  {
    private final String applicationId;

    private final String stageTypeId;

    private final Timestamp openTime;

    private Timestamp waiveTime;

    private Timestamp fixTime;

    private boolean seenByPrimaryEvaluation;

    private boolean seenByMonitoringEvaluation;

    private OldPolicyViolation oldViolation;

    private WaivedViolation waivedViolation;

    private ComponentIdentifier componentIdentifier;

    NewPolicyViolation(
        String applicationId,
        String stageTypeId,
        PolicyEvaluation evaluation,
        OldPolicyViolation oldViolation,
        PreparedStatement selectWaivedViolation) throws SQLException
    {
      this.applicationId = applicationId;
      this.stageTypeId = stageTypeId;
      openTime = evaluation.time;
      update(evaluation, oldViolation, selectWaivedViolation);
    }

    static PreparedStatement insertStatement(Connection connection) throws SQLException {
      return connection.prepareStatement("INSERT INTO " + SCHEMA + ".new_policy_violation ("
          + "policy_violation_id, application_id, stage_type_id, policy_id, policy_name, threat_level, "
          + "threat_category, hash, component_id_format, component_id_coordinates_json, filename, "
          + "constraint_facts_json, action_type_id, open_time, waive_time, fix_time, "
          + "policy_waiver_id, policy_waiver_comment, seen_by_primary_evaluation, seen_by_monitoring_evaluation"
          + ") VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20)");
    }

    void insert(PreparedStatement statement) throws SQLException {
      statement.setString(1, oldViolation.id);
      statement.setString(2, applicationId);
      statement.setString(3, stageTypeId);
      statement.setString(4, oldViolation.policyId);
      statement.setString(5, oldViolation.policyName);
      statement.setInt(6, oldViolation.threatLevel);
      statement.setString(7, oldViolation.threatCategory);
      statement.setString(8, oldViolation.hash);
      statement.setString(9, oldViolation.componentIdFormat);
      statement.setString(10, oldViolation.componentIdCoordinatesJson);
      statement.setString(11, getFilename(oldViolation.pathnames));
      statement.setString(12, oldViolation.constraintFactsJson);
      statement.setString(13, oldViolation.actionTypeId);
      statement.setTimestamp(14, openTime);
      statement.setTimestamp(15, waiveTime);
      statement.setTimestamp(16, fixTime);
      statement.setString(17, waivedViolation != null ? waivedViolation.policyWaiverId : null);
      statement.setString(18, waivedViolation != null ? waivedViolation.policyWaiverComment : null);
      statement.setBoolean(19, seenByPrimaryEvaluation);
      statement.setBoolean(20, seenByMonitoringEvaluation);
      statement.addBatch();
    }

    private static String getFilename(String pathnames) {
      if (pathnames != null) {
        String[] splitted = pathnames.trim().split("\n");
        return new ComponentDisplayFilename().addPathnames(Arrays.asList(splitted)).getFilename().orElse(null);
      }
      return null;
    }

    @Override
    public String getPolicyId() {
      return oldViolation.getPolicyId();
    }

    @Override
    public String getPolicyName() {
      return oldViolation.getPolicyName();
    }

    @Override
    public int getThreatLevel() {
      return oldViolation.getThreatLevel();
    }

    @Override
    public String getHash() {
      return oldViolation.getHash();
    }

    @Override
    public ComponentIdentifier getComponentIdentifier() {
      if (componentIdentifier == null) {
        componentIdentifier = oldViolation.getComponentIdentifier();
      }
      return componentIdentifier;
    }

    // Not used. It is needed only because of the {@link PolicyViolationComparable} interface.
    @Override
    public List<ConstraintFact> getConstraintFacts() {
      throw new UnsupportedOperationException();
    }

    // Not used. It is needed only because of the {@link PolicyViolationComparable} interface.
    @Override
    public String getConstraintFactsId() {
      throw new UnsupportedOperationException();
    }

    boolean isWaived() {
      return waiveTime != null;
    }

    void fix(PolicyEvaluation evaluation) {
      fixTime = evaluation.time;
    }

    void update(
        PolicyEvaluation evaluation,
        OldPolicyViolation oldViolation,
        PreparedStatement selectWaivedViolation) throws SQLException
    {
      this.oldViolation = oldViolation;
      boolean waived = oldViolation.isWaived();
      if (waiveTime == null && waived) {
        waiveTime = evaluation.time;
        waivedViolation = WaivedViolation.query(selectWaivedViolation, oldViolation.id);
      }
      if (evaluation.forMonitoring) {
        seenByMonitoringEvaluation = !waived;
      }
      else if (!evaluation.reevaluation) {
        seenByPrimaryEvaluation = !waived;
      }
    }
  }

  private static class OldPolicyViolation
      implements PolicyViolationComparable
  {
    final String id;

    final boolean isWaived;

    final String policyId;

    final String policyName;

    final int threatLevel;

    final String threatCategory;

    final String hash;

    final String componentIdFormat;

    final String componentIdCoordinatesJson;

    ComponentIdentifier componentIdentifier;

    final String constraintFactsJson;

    final String actionTypeId;

    final String pathnames;

    private OldPolicyViolation(ResultSet resultSet) throws SQLException {
      id = resultSet.getString(1);
      isWaived = resultSet.getBoolean(2);
      policyId = resultSet.getString(3);
      policyName = resultSet.getString(4);
      threatLevel = resultSet.getInt(5);
      threatCategory = resultSet.getString(6);
      hash = resultSet.getString(7);
      componentIdFormat = resultSet.getString(8);
      componentIdCoordinatesJson = resultSet.getString(9);
      constraintFactsJson = resultSet.getString(10);
      actionTypeId = resultSet.getString(11);
      pathnames = resultSet.getString(12);
    }

    static PreparedStatement queryStatement(Connection connection) throws SQLException {
      return connection.prepareStatement("SELECT policy_violation_id, waived, policy_id, policy_name, threat_level"
          + ", threat_category, hash, component_id_format, component_id_coordinates_json"
          + ", constraint_facts_json, action_type_id, pathnames FROM " + SCHEMA
          + ".policy_violation WHERE policy_evaluation_id = ?1");
    }

    static Collection<OldPolicyViolation> query(PreparedStatement statement, String evaluationId) throws SQLException {
      statement.setString(1, evaluationId);
      Collection<OldPolicyViolation> violations = new ArrayList<>();
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          violations.add(new OldPolicyViolation(resultSet));
        }
      }
      return violations;
    }

    public boolean isWaived() {
      return isWaived;
    }

    @Override
    public String getPolicyId() {
      return policyId;
    }

    @Override
    public String getPolicyName() {
      return policyName;
    }

    @Override
    public int getThreatLevel() {
      return threatLevel;
    }

    @Override
    public String getHash() {
      return hash;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ComponentIdentifier getComponentIdentifier() {
      if (componentIdentifier == null && componentIdFormat != null) {
        try {
          componentIdentifier = new ComponentIdentifier(componentIdFormat,
              JsonUtils.parse(componentIdCoordinatesJson, Map.class));
        }
        catch (IOException e) {
          throw new UncheckedIOException("Invalid policy violation: " + id, e);
        }
      }
      return componentIdentifier;
    }

    // Not used. It is needed only because of the {@link PolicyViolationComparable} interface.
    @Override
    public List<ConstraintFact> getConstraintFacts() {
      throw new UnsupportedOperationException();
    }

    // Not used. It is needed only because of the {@link PolicyViolationComparable} interface.
    @Override
    public String getConstraintFactsId() {
      throw new UnsupportedOperationException();
    }
  }

  private static class WaivedViolation
  {
    final String policyWaiverId;

    final String policyWaiverComment;

    private WaivedViolation(ResultSet resultSet) throws SQLException {
      policyWaiverId = resultSet.getString(1);
      policyWaiverComment = resultSet.getString(2);
    }

    static PreparedStatement queryStatement(Connection connection) throws SQLException {
      return connection.prepareStatement("SELECT policy_waiver_id, comment FROM " + SCHEMA
          + ".waived_policy_violation WHERE policy_violation_id = ?1");
    }

    static WaivedViolation query(PreparedStatement statement, String violationId) throws SQLException {
      statement.setString(1, violationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? new WaivedViolation(resultSet) : null;
      }
    }
  }

  private static class MigrationContext
  {
    final DataSource dataSource;

    final Queue<Application> applications;

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();

    final Queue<NewPolicyViolation> newViolations = new ConcurrentLinkedQueue<>();

    final int totalApplications;

    final AtomicInteger processedApplications = new AtomicInteger();

    final AtomicInteger processedEvaluations = new AtomicInteger();

    final AtomicInteger processedViolations = new AtomicInteger();

    final AtomicInteger insertedViolations = new AtomicInteger();

    private long lastProgressLog = System.currentTimeMillis();

    MigrationContext(DataSource dataSource, Collection<Application> applications) {
      this.dataSource = dataSource;
      this.applications = new ConcurrentLinkedQueue<>(applications);
      totalApplications = applications.size();
    }

    void throwIfNeeded() throws Exception {
      Throwable t = throwableRef.get();
      if (t instanceof Error) {
        throw (Error) t;
      }
      else if (t instanceof Exception) {
        throw (Exception) t;
      }
    }

    void logProgressIfNeeded() {
      long now = System.currentTimeMillis();
      if (now - lastProgressLog >= 1000 * 30) {
        logProgress();
        lastProgressLog = now;
      }
    }

    void logProgress() {
      log.info("Processed {} of {} applications, {} policy evaluations, {} policy violations and {} migrated records",
          processedApplications.get(), totalApplications, processedEvaluations.get(), processedViolations.get(),
          insertedViolations.get());
    }
  }

  private static class ViolationMigrator
      extends Thread
  {
    private static final AtomicInteger index = new AtomicInteger();

    private final MigrationContext context;

    ViolationMigrator(MigrationContext context) {
      super("ViolationMigrator-" + index.incrementAndGet());
      setDaemon(true);
      this.context = context;
    }

    @Override
    public void run() {
      try (Connection connection = context.dataSource.getConnection();
          PreparedStatement selectEvaluations = PolicyEvaluation.queryStatement(connection);
          PreparedStatement selectViolations = OldPolicyViolation.queryStatement(connection);
          PreparedStatement selectWaivedViolation = WaivedViolation.queryStatement(connection))
      {
        while (context.throwableRef.get() == null) {
          Application application = context.applications.poll();
          if (application == null) {
            return;
          }
          log.info("Migrating policy violation data for application '{}' ({})", application.name, application.id);
          for (StageType stageType : StageTypes.getAll()) {
            String stageTypeId = stageType.getId();
            List<PolicyEvaluation> evaluations = PolicyEvaluation.query(selectEvaluations, application.id, stageTypeId);
            if (evaluations.isEmpty()) {
              continue;
            }
            log.info("  Migrating {} policy evaluations for stage '{}' of application '{}' ({})", evaluations.size(),
                stageTypeId, application.name, application.id);
            int oldViolationCount = 0;
            int newViolationCount = 0;
            Set<NewPolicyViolation> unfixedViolations = new HashSet<>();
            for (PolicyEvaluation evaluation : evaluations) {
              if (context.throwableRef.get() != null) {
                return;
              }
              Collection<OldPolicyViolation> latestViolations = OldPolicyViolation.query(selectViolations,
                  evaluation.id);
              oldViolationCount += latestViolations.size();
              PolicyViolationDiff<? extends PolicyViolationComparable> diff = PolicyViolationDigester
                  .digestPolicyViolations(unfixedViolations, latestViolations, PolicyViolationComparator.COMPARATOR);
              for (PolicyViolationComparable appeared : diff.getAppeared()) {
                NewPolicyViolation newViolation = new NewPolicyViolation(application.id, stageTypeId, evaluation,
                    (OldPolicyViolation) appeared, selectWaivedViolation);
                unfixedViolations.add(newViolation);
              }
              for (Map.Entry<? extends PolicyViolationComparable, ? extends PolicyViolationComparable> entry : diff
                  .getSame()
                  .entrySet())
              {
                NewPolicyViolation existingViolation = (NewPolicyViolation) entry.getKey();
                OldPolicyViolation latestViolation = (OldPolicyViolation) entry.getValue();
                if (existingViolation.isWaived() && !latestViolation.isWaived()) {
                  existingViolation.fix(evaluation);
                  context.newViolations.add(existingViolation);
                  newViolationCount++;
                  unfixedViolations.remove(existingViolation);
                  existingViolation = new NewPolicyViolation(application.id, stageTypeId, evaluation, latestViolation,
                      selectWaivedViolation);
                  unfixedViolations.add(existingViolation);
                }
                else {
                  existingViolation.update(evaluation, latestViolation, selectWaivedViolation);
                }
              }
              for (PolicyViolationComparable cleared : diff.getCleared()) {
                NewPolicyViolation clearedViolation = (NewPolicyViolation) cleared;
                clearedViolation.fix(evaluation);
                context.newViolations.add(clearedViolation);
                newViolationCount++;
                unfixedViolations.remove(clearedViolation);
              }
              context.processedViolations.addAndGet(latestViolations.size());
              context.processedEvaluations.incrementAndGet();
            }
            context.newViolations.addAll(unfixedViolations);
            newViolationCount += unfixedViolations.size();
            log.info("  Migrated {} policy violations to {} records for stage '{}' of application '{}' ({})",
                oldViolationCount, newViolationCount, stageTypeId, application.name, application.id);
          }
          log.info("Migrated all policy violations for application '{}' ({})", application.name, application.id);
          context.processedApplications.incrementAndGet();
        }
      }
      catch (Throwable t) {
        context.throwableRef.compareAndSet(null, t);
      }
    }
  }

  @Override
  public void migrate(final DataSource dataSource, final String databaseSchema) throws Exception {
    long start = System.currentTimeMillis();
    Collection<Application> applications = getApplications(dataSource);
    log.info("Migrating policy violation data for {} applications", applications.size());
    MigrationContext context = new MigrationContext(dataSource, applications);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement insertViolation = NewPolicyViolation.insertStatement(connection))
    {
      ViolationMigrator[] migrators = new ViolationMigrator[4];
      for (int i = 0; i < migrators.length; i++) {
        migrators[i] = new ViolationMigrator(context);
        migrators[i].start();
      }
      final int MAX_BATCH_SIZE = 100;
      for (boolean done = false; !done;) {
        for (int batchSize = 0; batchSize < MAX_BATCH_SIZE;) {
          context.throwIfNeeded();
          NewPolicyViolation newViolation = context.newViolations.poll();
          if (newViolation == null) {
            done = Arrays.stream(migrators).noneMatch(Thread::isAlive);
            if (done) {
              break;
            }
            else {
              Thread.sleep(100);
              context.logProgressIfNeeded();
            }
          }
          else {
            newViolation.insert(insertViolation);
            batchSize++;
          }
        }
        int inserted = insertViolation.executeBatch().length;
        context.insertedViolations.addAndGet(inserted);
        context.logProgressIfNeeded();
      }
      context.throwIfNeeded();
      if (!context.applications.isEmpty()) {
        throw new IllegalStateException("Migration failed to process all applications");
      }
      context.logProgress();
      log.info("Migrated policy violation data in {} sec", (System.currentTimeMillis() - start + 999) / 1000);
      log.info("Finalizing migration, please be patient while obsolete records are being removed next");
    }
    catch (Throwable e) {
      context.throwableRef.compareAndSet(null, e);
      throw e;
    }
  }

  private Collection<Application> getApplications(DataSource dataSource) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = Application.queryStatement(connection))
    {
      return Application.query(statement);
    }
  }
}
