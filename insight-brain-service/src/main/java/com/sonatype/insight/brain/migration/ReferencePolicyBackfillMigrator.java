/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.ReferencePolicyFetcher;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backfills reference policies that became defaults in newer reference policy doc versions
 * but are missing on long-running deployments that upgraded from older versions.
 * See CLM-37954.
 */
@Named
public class ReferencePolicyBackfillMigrator
{
  private static final Logger log = LoggerFactory.getLogger(ReferencePolicyBackfillMigrator.class);

  static final String MIGRATION_ID = "reference-policy-backfill-v1";

  static final List<String> BACKFILL_POLICY_NAMES = List.of(
      "Security-Malicious",
      "Integrity-Rating");

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final PolicyDAO policyDAO;

  private final ReferencePolicyFetcher referencePolicyFetcher;

  private final AuditRecorder auditRecorder;

  @Inject
  public ReferencePolicyBackfillMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      PolicyDAO policyDAO,
      ReferencePolicyFetcher referencePolicyFetcher,
      AuditRecorder auditRecorder)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.policyDAO = policyDAO;
    this.referencePolicyFetcher = referencePolicyFetcher;
    this.auditRecorder = auditRecorder;
  }

  public void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Reference policy backfill migration already completed.");
      return;
    }

    // Cheap indexed point-reads first: in the steady-state pre-convergence path
    // (already-installed deployments with both targets present, or air-gapped
    // deployments retrying every boot), this avoids the full-table policyDAO.getAll()
    // scan below.
    //
    // For each target absent from root, also check whether any descendant org
    // (or app/repo) already has a policy with that name. If so, PolicyDAO.insert
    // would throw InvalidPolicyException via validateNameWithinHierarchy, the
    // transaction would roll back, and the tracker would never be set. This
    // scenario matches the documented workaround for CLM-37954: customers who
    // manually recreated the missing policy at a child org while root remains
    // empty. We log a WARN naming the conflict so support can advise the
    // customer, skip that target, and treat it as "handled" for tracker purposes.
    List<String> missingNames = new ArrayList<>();
    for (String name : BACKFILL_POLICY_NAMES) {
      if (policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, name) != null) {
        log.info("Reference policy '{}' already present at root org; skipping backfill.", name);
        continue;
      }
      List<Policy> samenameDescendants = policyDAO.getByName(name);
      if (!samenameDescendants.isEmpty()) {
        log.warn("Reference policy backfill: cannot insert '{}' at root org because a "
            + "policy with the same name exists on a descendant owner ({}). Customer must "
            + "remove the descendant policy and clear the migration tracker to re-run. "
            + "See CLM-37954.", name, samenameDescendants.get(0).getOwnerId());
        continue;
      }
      missingNames.add(name);
    }

    if (missingNames.isEmpty()) {
      // Either all targets are present at root, or every still-missing target
      // has a descendant conflict the customer must resolve manually. Either
      // way there is no further automated work; set the tracker so we don't
      // re-check on every subsequent boot.
      migrationTrackerDAO.insert(new MigrationTracker(MIGRATION_ID));
      return;
    }

    if (policyDAO.getAll().isEmpty()) {
      // Fresh install: DataMigrator runs before NewInstancePopulator (see
      // DefaultApplicationLifecycle.boot()). Defer the full reference-policy
      // import to NewInstancePopulator and just record the migration as done.
      log.debug("No policies exist anywhere; skipping reference policy backfill "
          + "(NewInstancePopulator will handle).");
      migrationTrackerDAO.insert(new MigrationTracker(MIGRATION_ID));
      return;
    }

    PolicyExportResult exportResult;
    try {
      exportResult = referencePolicyFetcher.getReferencePolicies();
    }
    catch (Exception e) {
      log.warn("Reference policy backfill (CLM-37954) could not reach HDS to fetch missing "
          + "policies {}; will retry on next startup. Air-gapped deployments may need to add "
          + "these policies manually.", missingNames, e);
      return;
    }

    if (exportResult == null) {
      // PolicyExportResult.policies defaults to Collections.emptyList(), so it cannot be
      // null for any properly-deserialized payload — a null check on the field would be
      // dead. We only guard against the fetcher returning a null result outright.
      log.warn("Reference policy backfill (CLM-37954): HDS returned a null result; "
          + "attempted to fetch {}. Will retry on next startup.", missingNames);
      return;
    }

    Set<String> missingNamesSet = new HashSet<>(missingNames);
    List<Policy> toInsert = exportResult.policies.stream()
        .filter(policy -> missingNamesSet.contains(policy.getName()))
        .collect(Collectors.toList());

    boolean allTargetsFound = toInsert.size() == missingNames.size();
    if (!allTargetsFound) {
      Set<String> foundNames = toInsert.stream().map(Policy::getName).collect(Collectors.toSet());
      List<String> notInHds = missingNames.stream()
          .filter(name -> !foundNames.contains(name))
          .collect(Collectors.toList());
      log.warn("Reference policy backfill: HDS response did not contain expected policies {}; "
          + "will retry on next startup.", notInHds);
    }

    if (toInsert.isEmpty()) {
      // Nothing to insert (but missing locally). Do not set tracker — retry on next boot.
      return;
    }

    try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.IMPORT)) {
      try (TransactionContext tx = policyDAO.createTransactionContext()) {
        tx.begin();
        for (Policy policy : toInsert) {
          policy.setId(null);
          policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
          // Targeted reference policies (Security-Malicious, Integrity-Rating) have no
          // LicenseThreatGroup or Label condition references, so we can insert them
          // directly without the id-remapping that PolicyImportExport performs. If HDS
          // ever evolves these to include such conditions, validation will fail and
          // the migrator will retry forever — see CLM-37954 review notes.
          policyDAO.insert(tx, policy);
          try (AuditSession sub = AuditData.get().recordSubEvent(AuditEvent.IMPORT_POLICY, false)) {
            AuditData.get().setPolicyWithDetails(policy);
          }
        }
        if (allTargetsFound) {
          // Only set tracker when the HDS payload contained every still-missing target.
          migrationTrackerDAO.insert(tx, new MigrationTracker(MIGRATION_ID));
        }
        tx.commit();
        AuditData.get().commitSubEvents();
        log.info("Backfilled missing reference policies on root org: {}",
            toInsert.stream().map(Policy::getName).collect(Collectors.toList()));
      }
      catch (Exception e) {
        AuditData.get().setException(e);
        log.error("Reference policy backfill failed during insert; transaction rolled back. "
            + "Will retry on next startup.", e);
      }
    }
  }
}
