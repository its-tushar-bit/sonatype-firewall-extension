/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.db.PostIncrementalMigrator;

/**
 * This migrator populates the organization_ancestor table with records for any pre-existing orgs. This will
 * include the root org and all user-created orgs. Note that this only runs for schema upgrades. For fresh installs,
 * an INSERT statement in schema.sql covers this need instead
 */
public class OrganizationAncestorInitialPopulationMigrator
    implements PostIncrementalMigrator
{
  private static final String ORG_QUERY = "SELECT organization_id, parent_organization_id FROM %s.organization;";

  // Note: for some reason when these params are ?1 etc, postgres complains about ?5 even though there is no ?5.
  // It works with the numberless syntax.
  private static final String ORG_INSERT =
      "INSERT INTO %s.organization_ancestor (" +
          "  organization_ancestor_id, organization_id, ancestor_id, ancestor_distance" +
          ") VALUES (?, ?, ?, ?)";

  @Override
  public void migrate(DataSource dataSource, String databaseSchema) throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
      populateResultsFromOrgParentMap(conn, constructOrgParentMap(conn, databaseSchema), databaseSchema);
    }
  }

  // map from child to immediate parent
  private Map<String, String> constructOrgParentMap(Connection conn, String databaseSchema) throws SQLException {
    Map<String, String> orgParentMap = new HashMap<>();

    String orgQuery = String.format(ORG_QUERY, databaseSchema);
    try (Statement stmt = conn.createStatement(); ResultSet allOrgs = stmt.executeQuery(orgQuery)) {
      while (allOrgs.next()) {
        String orgId = allOrgs.getString("organization_id");
        String parentOrgId = allOrgs.getString("parent_organization_id");

        orgParentMap.put(orgId, parentOrgId);
      }
    }

    return orgParentMap;
  }

  private void populateResultsFromOrgParentMap(
      Connection conn,
      Map<String, String> orgParentMap,
      String databaseSchema) throws SQLException
  {
    try (PreparedStatement stmt = conn.prepareStatement(String.format(ORG_INSERT, databaseSchema))) {
      for (Map.Entry<String, String> childParent : orgParentMap.entrySet()) {
        String child = childParent.getKey();
        int distance = 0;

        // include mapping from org to itself
        addBatchRow(stmt, child, child, 0);
        distance++;

        for (String parent = childParent.getValue(); parent != null; parent = orgParentMap.get(parent), distance++) {
          addBatchRow(stmt, child, parent, distance);
        }
      }

      stmt.executeBatch();
    }
  }

  private void addBatchRow(PreparedStatement stmt, String orgId, String ancestorId, int distance) throws SQLException {
    stmt.setString(1, IdUtil.newUUID());
    stmt.setString(2, orgId);
    stmt.setString(3, ancestorId);
    stmt.setInt(4, distance);
    stmt.addBatch();
  }
}
