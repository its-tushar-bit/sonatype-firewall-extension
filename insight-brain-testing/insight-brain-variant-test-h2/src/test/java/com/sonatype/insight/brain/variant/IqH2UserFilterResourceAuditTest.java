/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.filter.AdvancedLegalPackDashboardFilter;
import com.sonatype.insight.brain.filter.UserFilterDTO;
import com.sonatype.insight.brain.filter.UserFilterResource;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.dashboard.DashboardFilterService.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static com.sonatype.insight.brain.model.security.User.ADMIN_USERNAME;

/**
 * Reproduces the {@code AbstractAuditTest} log-capture scaffolding that the legacy
 * {@code UserFilterResourceAuditTest} inherited from its base class.
 */
@IqH2Test
class IqH2UserFilterResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private UserFilterDAO userFilterDAO;

  @BeforeEach
  void setUp() {
    userFilterDAO = ctx.lookup(UserFilterDAO.class);
    logOutput.before();
    logOutput.clear();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(UserFilterResource.RESOURCE_PATH);
  }

  @AfterEach
  void after() {
    // required in order to avoid clashes between create/delete tests
    try (TransactionContext tx = userFilterDAO.createTransactionContext()) {
      tx.begin();
      userFilterDAO.deleteByUsernameAndRealmId(tx, ADMIN_USERNAME, InternalRealm.ID);
      tx.commit();
    }
    logOutput.tearDown();
  }

  @Test
  void testCreateOrUpdateActiveUserFilterForCurrentUser_InsertActiveFilter() throws Exception {
    String filterName = ACTIVE_FILTER_NAME;
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    restRequest().path(UserFilterResource.ACTIVE_FILTERS_PATH).body(userFilterDTO).put();
    assertUserFilterAudit(filterName, userFilterDTO.getType());
  }

  @Test
  void testCreateOrUpdateActiveUserFilterForCurrentUser_UpdateActiveFilter() throws Exception {
    String filterName = ACTIVE_FILTER_NAME;
    ctx.tempEntity()
        .newUserFilter(ctx.getUsername(), InternalRealm.ID, filterName, ADVANCED_LEGAL_PACK_DASHBOARD,
            JsonUtils.format(new AdvancedLegalPackDashboardFilter()));
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    restRequest().path(UserFilterResource.ACTIVE_FILTERS_PATH).body(userFilterDTO).put();
    assertUserFilterAudit(filterName, userFilterDTO.getType());
  }

  @Test
  void testCreateOrUpdateNamedUserFilterForCurrentUser_Insert() throws Exception {
    String filterName = "test filter name";
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    restRequest().path(UserFilterResource.NAMED_FILTERS_PATH).body(userFilterDTO).put();
    assertUserFilterAudit(filterName, userFilterDTO.getType());
  }

  @Test
  void testCreateOrUpdateNamedUserFilterForCurrentUser_Update() throws Exception {
    String filterName = "test filter";
    ctx.tempEntity()
        .newUserFilter(ctx.getUsername(), InternalRealm.ID, filterName, ADVANCED_LEGAL_PACK_DASHBOARD,
            JsonUtils.format(new AdvancedLegalPackDashboardFilter()));
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    restRequest().path(UserFilterResource.NAMED_FILTERS_PATH).body(userFilterDTO).put();
    assertUserFilterAudit(filterName, userFilterDTO.getType());
  }

  @Test
  void deleteFilterForCurrentUserByNameAndType() throws Exception {
    UserFilter filter =
        ctx.tempEntity()
            .newUserFilter(ctx.getUsername(), InternalRealm.ID, "filterName", ADVANCED_LEGAL_PACK_DASHBOARD,
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

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
