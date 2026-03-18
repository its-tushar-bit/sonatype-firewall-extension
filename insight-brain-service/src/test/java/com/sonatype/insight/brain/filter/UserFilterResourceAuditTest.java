/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.DashboardFilterService.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static com.sonatype.insight.brain.model.security.User.ADMIN_USERNAME;

@Category(SlowTest.class)
public class UserFilterResourceAuditTest
    extends AbstractAuditTest
{
  private UserFilterDAO userFilterDAO;

  @Before
  public void setUp() {
    userFilterDAO = lookup(UserFilterDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserFilterResource.RESOURCE_PATH);
  }

  @After
  public void after() {
    // required in order to avoid clashes between create/delete tests
    try (TransactionContext tx = userFilterDAO.createTransactionContext()) {
      tx.begin();
      userFilterDAO.deleteByUsernameAndRealmId(tx, ADMIN_USERNAME, InternalRealm.ID);
      tx.commit();
    }
  }

  @Test
  public void testCreateOrUpdateActiveUserFilterForCurrentUser_InsertActiveFilter() throws Exception {
    String filterName = ACTIVE_FILTER_NAME;
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    restRequest().path(UserFilterResource.ACTIVE_FILTERS_PATH).body(userFilterDTO).put();
    assertUserFilterAudit(filterName, userFilterDTO.getType());
  }

  @Test
  public void testCreateOrUpdateActiveUserFilterForCurrentUser_UpdateActiveFilter() throws Exception {
    String filterName = ACTIVE_FILTER_NAME;
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, filterName, ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(new AdvancedLegalPackDashboardFilter()));
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    restRequest().path(UserFilterResource.ACTIVE_FILTERS_PATH).body(userFilterDTO).put();
    assertUserFilterAudit(filterName, userFilterDTO.getType());
  }

  @Test
  public void testCreateOrUpdateNamedUserFilterForCurrentUser_Insert() throws Exception {
    String filterName = "test filter name";
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    restRequest().path(UserFilterResource.NAMED_FILTERS_PATH).body(userFilterDTO).put();
    assertUserFilterAudit(filterName, userFilterDTO.getType());
  }

  @Test
  public void testCreateOrUpdateNamedUserFilterForCurrentUser_Update() throws Exception {
    String filterName = "test filter";
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, filterName, ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(new AdvancedLegalPackDashboardFilter()));
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    restRequest().path(UserFilterResource.NAMED_FILTERS_PATH).body(userFilterDTO).put();
    assertUserFilterAudit(filterName, userFilterDTO.getType());
  }

  @Test
  public void deleteFilterForCurrentUserByNameAndType() throws Exception {
    UserFilter filter =
        tempEntity.newUserFilter(getUsername(), InternalRealm.ID, "filterName", ADVANCED_LEGAL_PACK_DASHBOARD,
            JsonUtils.format(new AdvancedLegalPackDashboardFilter()));

    restRequest().query("name", filter.getName()).query("type", filter.getType()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_FILTER, null);
    assertUserFilterAuditData(auditDTO, filter);
  }

  private UserFilterDTO newUserFilterDTO(String filterName) {
    JsonNode node = JsonUtils.asTree(ImmutableMap.of("key1", "value 1"));
    UserFilterDTO userFilterDTO = new UserFilterDTO(filterName, null, ADVANCED_LEGAL_PACK_DASHBOARD, node);
    return userFilterDTO;
  }

  private void assertUserFilterAudit(String filterName, UserFilterType type) {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.SAVE_USER_FILTER, null);
    UserFilter persistedFilter =
        userFilterDAO.getByUsernameAndRealmIdAndNameAndType(ADMIN_USERNAME, InternalRealm.ID, filterName, type);

    assertUserFilterAuditData(auditDTO, persistedFilter);
  }

  private void assertUserFilterAuditData(AuditDTO auditDTO, UserFilter filter) {
    assertCustomData(auditDTO, "filterId", filter.getId());
    assertCustomData(auditDTO, "filterName",
        ACTIVE_FILTER_NAME.equals(filter.getName()) ? "(active)" : filter.getName());
  }
}
