/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.enterprisereporting;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingFilter;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class EnterpriseReportingFilterDAOTest
    extends AbstractDbDAOTest
{
  private EnterpriseReportingFilterDAO filterDao;

  @Before
  @Override
  public void setup() {
    super.setup();
    filterDao = daoFactory.createEnterpriseReportingFilterDAO();
  }

  @Test
  public void testCRUD() {
    User user = tempEntity.newUser();
    final var userId = user.getId();

    // Add filter
    var expectedFilter = createFilter(userId, "Filter A", "{\"k\":\"v\"}");
    filterDao.insert(expectedFilter);

    // ensure filterId is generated on insert
    assertThat(expectedFilter.getId()).isNotBlank();

    // Retrieve filter
    var persistedFilter = filterDao.getFilterByUserAndFilterId(user.getId(), expectedFilter.getId());
    assertFilter(persistedFilter, expectedFilter);

    // Update Filter
    expectedFilter.setFilter("{\"k\":\"v2\"}");
    filterDao.update(expectedFilter);
    persistedFilter = filterDao.getFilterByUserAndFilterId(user.getId(), expectedFilter.getId());
    assertFilter(persistedFilter, expectedFilter);

    // Delete Filter
    filterDao.delete(expectedFilter);
    assertThat(filterDao.getFilterByUserAndFilterId(user.getId(), expectedFilter.getId())).isNull();
  }

  @Test
  public void testInsert_largeFilterPayload() {
    User user = tempEntity.newUser();
    final var userId = user.getId();
    String largeValue = "a".repeat(100 * 1024);
    String largeJson = "{\"data\":" + "\"" + largeValue + "\"}";

    var expectedFilter = createFilter(userId, "Big", largeJson);
    filterDao.insert(expectedFilter);

    var persistedFilter = filterDao.getFilterByUserAndFilterId(user.getId(), expectedFilter.getId());
    assertFilter(persistedFilter, expectedFilter);
  }

  @Test
  public void testGetFiltersByUserId_orderedByName() {
    User user = tempEntity.newUser();
    final var userId = user.getId();

    var filter1 = createFilter(userId, "foo", "{}");
    filterDao.insert(filter1);
    var filter2 = createFilter(userId, "bar", "{}");
    filterDao.insert(filter2);

    // other user's filter should not be returned
    User otherUser = tempEntity.newUser();
    tempEntity.newEnterpriseReportingFilter(otherUser.getId(), "baz", "{}");

    List<EnterpriseReportingFilter> filtersList = filterDao.getFiltersByUserId(userId);
    assertThat(filtersList).hasSize(2);
    assertFilter(filtersList.get(0), filter2);
    assertFilter(filtersList.get(1), filter1);
  }

  @Test
  public void testGetFiltersByUserId_nullOrBlank() {
    assertThat(filterDao.getFiltersByUserId(null)).isEmpty();
    assertThat(filterDao.getFiltersByUserId(" ")).isEmpty();
  }

  @Test
  public void testGetFilterByUserIdAndName_caseInsensitive() {
    User user = tempEntity.newUser();
    final var userId = user.getId();

    var expectedFilter = createFilter(userId, "Foo", "{}");
    filterDao.insert(expectedFilter);

    EnterpriseReportingFilter persistedFilter = filterDao.getFilterByUserIdAndName(userId, "foo");
    assertFilter(persistedFilter, expectedFilter);
  }

  @Test
  public void testGetFilterByUserIdAndName_notFoundReturnsNull() {
    User user = tempEntity.newUser();
    final var userId = user.getId();

    // No filters created for this user with name "absent"
    assertThat(filterDao.getFilterByUserIdAndName(userId, "absent")).isNull();
  }

  @Test
  public void testGetFilterByUserIdAndName_noNameThrowsError() {
    User user = tempEntity.newUser();
    final var userId = user.getId();

    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> filterDao.getFilterByUserIdAndName(userId, null))
        .withMessageContaining("Filter name is required");
  }

  @Test
  public void testGetFilterByUserAndFilterId() {
    User user = tempEntity.newUser();
    final var userId = user.getId();

    var expectedFilter = createFilter(userId, "Foo", "{\"k\":\"v\"}");
    filterDao.insert(expectedFilter);
    String expectedFilterId = expectedFilter.getId();

    var persistedFilter = filterDao.getFilterByUserAndFilterId(user.getId(), expectedFilterId);
    assertFilter(persistedFilter, expectedFilter);
  }

  @Test
  public void testGetFilterByUserAndFilterId_wrongUserReturnsNull() {
    User owner = tempEntity.newUser();
    User otherOwner = tempEntity.newUser();
    var expectedFilter = createFilter(owner.getId(), "Foo", "{}");

    assertThat(filterDao.getFilterByUserAndFilterId(otherOwner.getId(), expectedFilter.getId())).isNull();
  }

  private EnterpriseReportingFilter createFilter(String userId, String filterName, String filterJson) {
    var expectedFilter = new EnterpriseReportingFilter();
    expectedFilter.setUserId(userId);
    expectedFilter.setFilterName(filterName);
    expectedFilter.setFilter(filterJson);
    return expectedFilter;
  }

  private void assertFilter(EnterpriseReportingFilter persisted, EnterpriseReportingFilter expected) {
    assertThat(persisted).isNotNull();
    assertThat(persisted.getId()).isEqualTo(expected.getId());
    assertThat(persisted.getUserId()).isEqualTo(expected.getUserId());
    assertThat(persisted.getFilterName()).isEqualTo(expected.getFilterName());
    assertThat(persisted.getFilter()).isEqualTo(expected.getFilter());
  }
}
