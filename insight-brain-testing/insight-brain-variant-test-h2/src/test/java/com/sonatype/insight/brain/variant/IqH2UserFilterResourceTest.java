/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.filter.AdvancedLegalPackDashboardFilter;
import com.sonatype.insight.brain.filter.UserFilterDTO;
import com.sonatype.insight.brain.filter.UserFilterResource;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2UserFilterResourceTest
{
  private IqTestContext ctx;

  private UserFilterDAO userFilterDAO;

  @BeforeEach
  void setUp() {
    userFilterDAO = ctx.lookup(UserFilterDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(UserFilterResource.RESOURCE_PATH);
  }

  @AfterEach
  void after() {
    // required in order to avoid clashes between create/delete tests
    try (TransactionContext tx = userFilterDAO.createTransactionContext()) {
      tx.begin();
      userFilterDAO.deleteByUsernameAndRealmId(tx, ctx.getUsername(), InternalRealm.ID);
      tx.commit();
    }
  }

  @Test
  void testCreateOrUpdateActiveUserFilterForCurrentUser_Insert() throws Exception {
    UserFilterDTO userFilterDTO = newUserFilterDTO(ACTIVE_FILTER_NAME);

    HttpResponse response = restRequest().path(UserFilterResource.ACTIVE_FILTERS_PATH).body(userFilterDTO).put();
    ctx.assertResponseStatus(200, response);

    UserFilterDTO result = response.getBody(UserFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().isEqualTo(userFilterDTO);

    UserFilter userFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(ctx.getUsername(), InternalRealm.ID,
        userFilterDTO.getName(), userFilterDTO.getType());
    assertFilter(userFilter, userFilterDTO);
  }

  @Test
  void testCreateOrUpdateActiveUserFilterForCurrentUser_Update() throws Exception {
    String filterName = ACTIVE_FILTER_NAME;
    ctx.tempEntity().newUserFilter(ctx.getUsername(), InternalRealm.ID, filterName, ADVANCED_LEGAL_PACK_DASHBOARD, "");
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    HttpResponse response = restRequest().path(UserFilterResource.ACTIVE_FILTERS_PATH).body(userFilterDTO).put();
    ctx.assertResponseStatus(200, response);

    UserFilterDTO result = response.getBody(UserFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().isEqualTo(userFilterDTO);

    UserFilter userFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(ctx.getUsername(), InternalRealm.ID,
        userFilterDTO.getName(), userFilterDTO.getType());
    assertFilter(userFilter, result);
  }

  @Test
  void testCreateOrUpdateNamedUserFilterForCurrentUser_Insert() throws Exception {
    UserFilterDTO userFilterDTO = newUserFilterDTO("test filter name");

    HttpResponse response = restRequest().path(UserFilterResource.NAMED_FILTERS_PATH).body(userFilterDTO).put();
    ctx.assertResponseStatus(200, response);

    UserFilterDTO result = response.getBody(UserFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().isEqualTo(userFilterDTO);

    UserFilter userFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(ctx.getUsername(), InternalRealm.ID,
        userFilterDTO.getName(), userFilterDTO.getType());
    assertFilter(userFilter, userFilterDTO);
  }

  @Test
  void testCreateOrUpdateNamedUserFilterForCurrentUser_Update() throws Exception {
    String filterName = "test filter";
    ctx.tempEntity()
        .newUserFilter(ctx.getUsername(), InternalRealm.ID, filterName, ADVANCED_LEGAL_PACK_DASHBOARD,
            JsonUtils.format(new AdvancedLegalPackDashboardFilter()));
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    HttpResponse response = restRequest().path(UserFilterResource.NAMED_FILTERS_PATH).body(userFilterDTO).put();
    ctx.assertResponseStatus(200, response);

    UserFilterDTO result = response.getBody(UserFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().isEqualTo(userFilterDTO);

    UserFilter userFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(ctx.getUsername(), InternalRealm.ID,
        userFilterDTO.getName(), userFilterDTO.getType());
    assertFilter(userFilter, result);
  }

  @Test
  void testGetActiveUserFilterForCurrentUser() throws Exception {
    UserFilter userFilter =
        ctx.tempEntity()
            .newUserFilter(ctx.getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME,
                ADVANCED_LEGAL_PACK_DASHBOARD, JsonUtils.format(new AdvancedLegalPackDashboardFilter()));

    HttpResponse response =
        restRequest().path(UserFilterResource.ACTIVE_FILTERS_PATH).query("type", ADVANCED_LEGAL_PACK_DASHBOARD).get();
    ctx.assertResponseStatus(200, response);

    UserFilterDTO result = response.getBody(UserFilterDTO.class);
    assertFilter(result, userFilter);
  }

  @Test
  void testGetNamedFiltersForCurrentUser() throws Exception {
    UserFilter filter1 =
        ctx.tempEntity()
            .newUserFilter(ctx.getUsername(), InternalRealm.ID, "filter1", ADVANCED_LEGAL_PACK_DASHBOARD,
                JsonUtils.format(new AdvancedLegalPackDashboardFilter()));
    UserFilter filter2 =
        ctx.tempEntity()
            .newUserFilter(ctx.getUsername(), InternalRealm.ID, "filter2", ADVANCED_LEGAL_PACK_DASHBOARD,
                JsonUtils.format(new AdvancedLegalPackDashboardFilter()));

    HttpResponse response =
        restRequest().path(UserFilterResource.NAMED_FILTERS_PATH).query("type", ADVANCED_LEGAL_PACK_DASHBOARD).get();
    ctx.assertResponseStatus(200, response);

    List<UserFilterDTO> result = Arrays.asList(response.getBody(UserFilterDTO[].class));
    assertThat(result).isNotEmpty();
    assertFilter(result.get(0), filter1);
    assertFilter(result.get(1), filter2);
  }

  @Test
  void deleteFilterForCurrentUserByNameAndType() throws Exception {
    UserFilter filter =
        ctx.tempEntity()
            .newUserFilter(ctx.getUsername(), InternalRealm.ID, "filterName", ADVANCED_LEGAL_PACK_DASHBOARD,
                JsonUtils.format(new AdvancedLegalPackDashboardFilter()));

    HttpResponse response = restRequest().query("name", filter.getName()).query("type", filter.getType()).delete();
    ctx.assertResponseStatus(204, response);

    assertThat(userFilterDAO.getById(filter.getId())).isNull();
  }

  private UserFilterDTO newUserFilterDTO(String filterName) {
    JsonNode node =
        JsonUtils.asTree(ImmutableMap.of("key1", "value 1", "key2", true, "key3", ImmutableMap.of("subKey1", 1)));
    UserFilterDTO userFilterDTO = new UserFilterDTO(filterName, null, ADVANCED_LEGAL_PACK_DASHBOARD, node);
    return userFilterDTO;
  }

  private void assertFilter(UserFilter actualFilter, UserFilterDTO expectedFilter) {
    assertThat(actualFilter).isNotNull();
    assertThat(actualFilter.getRealmId()).isEqualTo(InternalRealm.ID);
    assertThat(actualFilter.getType()).isEqualTo(expectedFilter.getType());
    assertThat(actualFilter.getUsername()).isEqualTo(ctx.getUsername());
    assertThat(actualFilter.getFilter()).isEqualTo(JsonUtils.format(expectedFilter.getFilter()));
    assertThat(actualFilter.getName()).isEqualTo(expectedFilter.getName());
    assertThat(actualFilter.getBasedOnFilterName()).isNull();
  }

  private void assertFilter(UserFilterDTO actualFilter, UserFilter expectedFilter) {
    assertThat(actualFilter).isNotNull();
    assertThat(actualFilter.getType()).isEqualTo(expectedFilter.getType());
    assertThat(actualFilter.getFilter()).isEqualTo(new UserFilterDTO(expectedFilter).getFilter());
    assertThat(actualFilter.getName()).isEqualTo(expectedFilter.getName());
    assertThat(actualFilter.getBasedOnFilterName()).isEqualTo(expectedFilter.getBasedOnFilterName());
  }
}
