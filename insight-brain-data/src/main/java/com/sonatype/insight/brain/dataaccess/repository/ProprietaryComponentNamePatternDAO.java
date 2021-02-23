/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.dataaccess.TransactionContext;

public class ProprietaryComponentNamePatternDAO
    extends AbstractOperationalSqlDAO<ProprietaryComponentNamePattern>
{
  @Override
  protected ProprietaryComponentNamePattern getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ProprietaryComponentNamePattern entity WHERE entity.id = ?1";
    return get(tx, sQuery, id);
  }

  @Override
  public void update(TransactionContext tx, ProprietaryComponentNamePattern entity) {
    throw new UnsupportedOperationException();
  }

  public List<ProprietaryComponentNamePattern> getByFormat(String format) {
    String sQuery = "SELECT entity FROM ProprietaryComponentNamePattern entity WHERE entity.format = ?1";
    return getList(sQuery, format);
  }

  public void deleteByRepositoryManager(String repositoryManagerInstanceId) {
    String sQuery = "SELECT DISTINCT entity.repositoryPublicId FROM ProprietaryComponentNamePattern entity"
        + " WHERE entity.repositoryManagerInstanceId=?1";
    List<String> repositoryPublicIds = new Query<String>(sQuery, repositoryManagerInstanceId).getList();
    for (String repositoryPublicId : repositoryPublicIds) {
      deleteByRepository(repositoryManagerInstanceId, repositoryPublicId);
    }
  }

  public void deleteByRepository(String repositoryManagerInstanceId, String repositoryPublicId) {
    String sQuery = "DELETE FROM ProprietaryComponentNamePattern entity"
        + " WHERE entity.repositoryManagerInstanceId=?1 AND entity.repositoryPublicId=?2";
    createQuery(sQuery, repositoryManagerInstanceId, repositoryPublicId).executeUpdate();
  }
}
