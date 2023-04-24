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

import javax.sql.DataSource;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.PostIncrementalMigrator;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProprietaryComponentNamePatternMigrator
    implements PostIncrementalMigrator
{
  private static final Logger log = LoggerFactory.getLogger(ProprietaryComponentNamePatternMigrator.class);

  @Override
  public void migrate(DataSource dataSource) throws Exception {
    long start = System.currentTimeMillis();
    log.info("Migrating proprietary component name patterns...");

    RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();
    RepositoryDAO repositoryDAO = new RepositoryDAO();
    ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO = new ProprietaryComponentNamePatternDAO();
    
    List<OldProprietaryComponentNamePattern> oldPatterns = OldProprietaryComponentNamePattern.getAll(dataSource);
    log.info("Found {} proprietary component name patterns.", oldPatterns.size());

    for (OldProprietaryComponentNamePattern oldPattern : oldPatterns) {
      // Get or create the repository manager for this pattern
      RepositoryManager repositoryManager =
          repositoryManagerDAO.getByInstanceId(oldPattern.repositorymanagerInstanceId);
      if (repositoryManager == null) {
        repositoryManager = new RepositoryManager(oldPattern.repositorymanagerInstanceId);
        repositoryManagerDAO.insert(repositoryManager);
        log.info("Created repository manager with instance ID {}.", repositoryManager.getInstanceId());
      }

      // Get or create the repository for this pattern
      Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(
          repositoryManager.getInstanceId(), oldPattern.repositoryPublicId);
      if (repository == null) {
        repository = new Repository(repositoryManager.getId(), oldPattern.repositoryPublicId);
        repository.setRepositoryType(RepositoryType.hosted);
        repository.setFormat(oldPattern.format);
        repository.setNamespaceConfusionProtectionEnabled(true);
        repositoryDAO.insert(repository);
        log.info("Created repository with public ID {} for repository manager instance ID {} and format {}.",
            repository.getPublicId(), repositoryManager.getInstanceId(), repository.getFormat());
      }
      else {
        if (!RepositoryType.hosted.equals(repository.getRepositoryType())) {
          throw new BadRequestException(
              "Repository " + repository.getPublicId() + " (" + repository.getId() + ") is not a hosted repository");
        }
      }

      ProprietaryComponentNamePattern pattern = proprietaryComponentNamePatternDAO.getById(oldPattern.id);
      pattern.setRepositoryId(repository.getId());
      proprietaryComponentNamePatternDAO.update(pattern);
    }

    log.info("Migrated {} proprietary component name patterns in {} ms.", oldPatterns.size(),
        System.currentTimeMillis() - start);
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

    static List<OldProprietaryComponentNamePattern> getAll(DataSource dataSource) throws SQLException {
      try (Connection connection = dataSource.getConnection();
          PreparedStatement statement = connection.prepareStatement(
              "SELECT proprietary_component_name_pattern_id, repository_manager_instance_id, "
                  + "repository_public_id, format FROM "
                  + OperationalDataStoreProvider.getDatabaseSchema() + ".proprietary_component_name_pattern");) {
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
