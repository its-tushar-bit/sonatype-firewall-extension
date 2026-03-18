/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.PostIncrementalMigrator;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProprietaryComponentNamePatternMigrator
    implements PostIncrementalMigrator
{
  private static final Logger log = LoggerFactory.getLogger(ProprietaryComponentNamePatternMigrator.class);

  @Override
  public void migrate(final DataSource dataSource, final String databaseSchema) throws Exception {
    long start = System.currentTimeMillis();
    log.info("Migrating proprietary component name patterns...");

    List<OldProprietaryComponentNamePattern> oldPatterns =
        OldProprietaryComponentNamePattern.getAll(dataSource, databaseSchema);
    log.info("Found {} proprietary component name patterns.", oldPatterns.size());

    for (OldProprietaryComponentNamePattern oldPattern : oldPatterns) {
      String repositoryManagerId =
          getOrCreateRepositoryManagerId(dataSource, oldPattern.repositorymanagerInstanceId, databaseSchema);
      String repositoryId =
          getOrCreateRepositoryId(dataSource, repositoryManagerId, oldPattern.repositoryPublicId, oldPattern.format,
              databaseSchema);

      updateProprietaryComponentNamePattern(dataSource, oldPattern.id, repositoryId, databaseSchema);
    }

    log.info("Migrated {} proprietary component name patterns in {} ms.", oldPatterns.size(),
        System.currentTimeMillis() - start);
  }

  private String getOrCreateRepositoryManagerId(
      final DataSource dataSource,
      final String repositoryManagerInstanceId,
      final String databaseSchema) throws SQLException
  {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT repository_manager_id FROM "
            + databaseSchema + ".repository_manager WHERE instance_id = ?");)
    {
      statement.setString(1, repositoryManagerInstanceId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          return resultSet.getString(1);
        }
      }
    }

    String repositoryManagerId = newUUID();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement insertStmt =
            connection.prepareStatement("INSERT INTO " + databaseSchema
                + ".repository_manager (repository_manager_id, instance_id) VALUES (?, ?)");)
    {
      connection.setAutoCommit(true);
      insertStmt.setString(1, repositoryManagerId);
      insertStmt.setString(2, repositoryManagerInstanceId);
      insertStmt.executeUpdate();

      log.info("Created repository manager with instance ID {}.", repositoryManagerInstanceId);

      return repositoryManagerId;
    }
  }

  private String getOrCreateRepositoryId(
      final DataSource dataSource,
      final String repositoryManagerId,
      final String repositoryPublicId,
      final String format,
      final String databaseSchema) throws SQLException
  {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT repository_id, repository_type FROM " + databaseSchema
                    + ".repository WHERE repository_manager_id = ? AND public_id = ?");)
    {
      statement.setString(1, repositoryManagerId);
      statement.setString(2, repositoryPublicId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          if (!"hosted".equals(resultSet.getString(2))) {
            throw new BadRequestException(
                "Repository " + repositoryPublicId + " (" + resultSet.getString(1) + ") is not a hosted repository");
          }
          return resultSet.getString(1);
        }
      }
    }

    String repositoryId = newUUID();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement insertStmt =
            connection.prepareStatement("INSERT INTO " + databaseSchema
                + ".repository (repository_id, repository_manager_id, public_id, repository_type, format, enabled, "
                + "quarantine_enabled, policy_compliant_component_selection_enabled, "
                + "namespace_confusion_protection_enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");)
    {
      connection.setAutoCommit(true);
      insertStmt.setString(1, repositoryId);
      insertStmt.setString(2, repositoryManagerId);
      insertStmt.setString(3, repositoryPublicId);
      insertStmt.setString(4, "hosted");
      insertStmt.setString(5, format);
      insertStmt.setBoolean(6, false);
      insertStmt.setBoolean(7, false);
      insertStmt.setBoolean(8, false);
      insertStmt.setBoolean(9, true);
      insertStmt.executeUpdate();

      log.info("Created repository with public ID {} for repository manager ID {} and format {}.", repositoryPublicId,
          repositoryManagerId, format);

      return repositoryId;
    }
  }

  private String newUUID() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private void updateProprietaryComponentNamePattern(
      final DataSource dataSource,
      final String proprietaryComponentNamePatternId,
      final String repositoryId,
      final String databaseSchema) throws SQLException
  {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement updateStmt = connection.prepareStatement("UPDATE "
            + databaseSchema + ".proprietary_component_name_pattern"
            + " SET repository_id=? WHERE proprietary_component_name_pattern_id=?");)
    {
      connection.setAutoCommit(true);
      updateStmt.setString(1, repositoryId);
      updateStmt.setString(2, proprietaryComponentNamePatternId);
      updateStmt.executeUpdate();
    }
  }

  private static class OldProprietaryComponentNamePattern
  {
    String id;

    String repositorymanagerInstanceId;

    String repositoryPublicId;

    String format;

    private OldProprietaryComponentNamePattern(ResultSet resultSet) throws SQLException {
      id = resultSet.getString(1);
      repositorymanagerInstanceId = resultSet.getString(2);
      repositoryPublicId = resultSet.getString(3);
      format = resultSet.getString(4);
    }

    static List<OldProprietaryComponentNamePattern> getAll(
        final DataSource dataSource,
        final String databaseSchema) throws SQLException
    {
      try (Connection connection = dataSource.getConnection();
          PreparedStatement statement = connection.prepareStatement(
              "SELECT proprietary_component_name_pattern_id, repository_manager_instance_id, "
                  + "repository_public_id, format FROM "
                  + databaseSchema + ".proprietary_component_name_pattern");)
      {
        List<OldProprietaryComponentNamePattern> result = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            result.add(new OldProprietaryComponentNamePattern(resultSet));
          }
        }
        return result;
      }
    }
  }
}
