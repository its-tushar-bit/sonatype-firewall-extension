/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.enterprisereporting;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingDefaultFilter;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnterpriseReportingDefaultFilterDAOTest
    extends AbstractDbDAOTest
{
  private EnterpriseReportingDefaultFilterDAO defaultFilterDao;

  private EnterpriseReportingFilterDAO filterDao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    defaultFilterDao = daoFactory.createEnterpriseReportingDefaultFilterDAO();
    filterDao = daoFactory.createEnterpriseReportingFilterDAO();
  }

  @Test
  public void testCRUD() {
    User user = tempEntity.newUser();
    final var userId = user.getId();

    // Create two enterprise reporting filters for this user to satisfy FK on default filter
    String filterIdA = tempEntity.newEnterpriseReportingFilter(userId, "Foo", "{}").getId();
    String filterIdB = tempEntity.newEnterpriseReportingFilter(userId, "Bar", "{}").getId();

    // Add default filter pointing to filter Foo
    var expectedDefaultFilter = createDefaultFilter(userId, filterIdA);
    defaultFilterDao.insert(expectedDefaultFilter);

    // Retrieve default filter by user id
    var persistedDefaultFilter = defaultFilterDao.getDefaultFilterByUserId(expectedDefaultFilter.getId());
    assertFilter(persistedDefaultFilter, expectedDefaultFilter);

    // Update default filter to point to filter B
    expectedDefaultFilter.setFilterId(filterIdB);
    defaultFilterDao.update(expectedDefaultFilter);
    persistedDefaultFilter = defaultFilterDao.getDefaultFilterByUserId(expectedDefaultFilter.getId());
    assertFilter(persistedDefaultFilter, expectedDefaultFilter);

    // Delete Filter
    defaultFilterDao.delete(expectedDefaultFilter);
    assertThat(defaultFilterDao.getDefaultFilterByUserId(expectedDefaultFilter.getId())).isNull();
  }

  @Test
  public void testCascadeDeleteWhenFilterIsDeleted() {
    User user = tempEntity.newUser();
    final var userId = user.getId();

    var filter = tempEntity.newEnterpriseReportingFilter(userId, "Foo", "{}");
    var defaultFilter = createDefaultFilter(userId, filter.getId());
    defaultFilterDao.insert(defaultFilter);

    filterDao.delete(filter);
    // assert that the cascade works correctly and related default filter is also deleted
    assertThat(defaultFilterDao.getDefaultFilterByUserId(defaultFilter.getId())).isNull();
  }

  @Test
  public void testGetDefaultFilterByUserId_returnsNullWhenNone() {
    User user = tempEntity.newUser();
    assertThat(defaultFilterDao.getDefaultFilterByUserId(user.getId())).isNull();
  }

  @Test
  public void testInsert_FailsForDuplicateUserIds() {
    User user = tempEntity.newUser();
    final var userId = user.getId();

    String filterIdA = tempEntity.newEnterpriseReportingFilter(userId, "Foo", "{}").getId();
    String filterIdB = tempEntity.newEnterpriseReportingFilter(userId, "Bar", "{}").getId();

    var defaultFilter = createDefaultFilter(userId, filterIdA);
    defaultFilterDao.insert(defaultFilter);

    // Second default for same user should violate primary key
    assertThatThrownBy(() -> defaultFilterDao.insert(createDefaultFilter(userId, filterIdB)))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testInsert_ldapUserWithLongDn() {
    String longDn = "cn=ldapuser,ou=ipausers,ou=users,dc=standalone,dc=localdomain";
    String filterIdA = tempEntity.newEnterpriseReportingFilter(longDn, "My Filter", "{}").getId();

    var defaultFilter = createDefaultFilter(longDn, filterIdA);
    defaultFilterDao.insert(defaultFilter);

    var persisted = defaultFilterDao.getDefaultFilterByUserId(longDn);
    assertFilter(persisted, defaultFilter);
  }

  @Test
  public void testInsert_FailsWithNonExistentFilterId() {
    User user = tempEntity.newUser();
    final var userId = user.getId();
    String bogusFilterId = "does-not-exist";

    var defaultFilter = createDefaultFilter(userId, bogusFilterId);

    assertThatThrownBy(() -> defaultFilterDao.insert(defaultFilter))
        .isInstanceOf(RuntimeException.class);
  }

  private EnterpriseReportingDefaultFilter createDefaultFilter(String userId, String filterId) {
    var expectedDefaultFilter = new EnterpriseReportingDefaultFilter();
    expectedDefaultFilter.setId(userId);
    expectedDefaultFilter.setFilterId(filterId);
    return expectedDefaultFilter;
  }

  private void assertFilter(EnterpriseReportingDefaultFilter actual, EnterpriseReportingDefaultFilter expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getFilterId()).isEqualTo(expected.getFilterId());
  }
}
