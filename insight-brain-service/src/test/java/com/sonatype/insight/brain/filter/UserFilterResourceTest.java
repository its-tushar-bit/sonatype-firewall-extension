/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class UserFilterResourceTest
    extends AbstractResourceTest
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
      userFilterDAO.deleteByUsernameAndRealmId(tx, getUsername(), InternalRealm.ID);
      tx.commit();
    }
  }

  @Test
  public void testCreateOrUpdateActiveUserFilterForCurrentUser_Insert() throws Exception {
    UserFilterDTO userFilterDTO = newUserFilterDTO(ACTIVE_FILTER_NAME);

    HttpResponse response = restRequest().path(UserFilterResource.ACTIVE_FILTERS_PATH).body(userFilterDTO).put();
    assertResponseStatus(200, response);

    UserFilterDTO result = response.getBody(UserFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().isEqualTo(userFilterDTO);

    UserFilter userFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(getUsername(), InternalRealm.ID,
        userFilterDTO.getName(), userFilterDTO.getType());
    assertFilter(userFilter, userFilterDTO);
  }

  @Test
  public void testCreateOrUpdateActiveUserFilterForCurrentUser_Update() throws Exception {
    String filterName = ACTIVE_FILTER_NAME;
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, filterName, ADVANCED_LEGAL_PACK_DASHBOARD, "");
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    HttpResponse response = restRequest().path(UserFilterResource.ACTIVE_FILTERS_PATH).body(userFilterDTO).put();
    assertResponseStatus(200, response);

    UserFilterDTO result = response.getBody(UserFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().isEqualTo(userFilterDTO);

    UserFilter userFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(getUsername(), InternalRealm.ID,
        userFilterDTO.getName(), userFilterDTO.getType());
    assertFilter(userFilter, result);
  }

  @Test
  public void testCreateOrUpdateNamedUserFilterForCurrentUser_Insert() throws Exception {
    UserFilterDTO userFilterDTO = newUserFilterDTO("test filter name");

    HttpResponse response = restRequest().path(UserFilterResource.NAMED_FILTERS_PATH).body(userFilterDTO).put();
    assertResponseStatus(200, response);

    UserFilterDTO result = response.getBody(UserFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().isEqualTo(userFilterDTO);

    UserFilter userFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(getUsername(), InternalRealm.ID,
        userFilterDTO.getName(), userFilterDTO.getType());
    assertFilter(userFilter, userFilterDTO);
  }

  @Test
  public void testCreateOrUpdateNamedUserFilterForCurrentUser_Update() throws Exception {
    String filterName = "test filter";
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, filterName, ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(new AdvancedLegalPackDashboardFilter()));
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    HttpResponse response = restRequest().path(UserFilterResource.NAMED_FILTERS_PATH).body(userFilterDTO).put();
    assertResponseStatus(200, response);

    UserFilterDTO result = response.getBody(UserFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().isEqualTo(userFilterDTO);

    UserFilter userFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(getUsername(), InternalRealm.ID,
        userFilterDTO.getName(), userFilterDTO.getType());
    assertFilter(userFilter, result);
  }

  @Test
  public void testGetActiveUserFilterForCurrentUser() throws Exception {
    UserFilter userFilter =
        tempEntity.newUserFilter(getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD,
            JsonUtils.format(new AdvancedLegalPackDashboardFilter()));

    HttpResponse response =
        restRequest().path(UserFilterResource.ACTIVE_FILTERS_PATH).query("type", ADVANCED_LEGAL_PACK_DASHBOARD).get();
    assertResponseStatus(200, response);

    UserFilterDTO result = response.getBody(UserFilterDTO.class);
    assertFilter(result, userFilter);
  }

  @Test
  public void testGetNamedFiltersForCurrentUser() throws Exception {
    UserFilter filter1 =
        tempEntity.newUserFilter(getUsername(), InternalRealm.ID, "filter1", ADVANCED_LEGAL_PACK_DASHBOARD,
            JsonUtils.format(new AdvancedLegalPackDashboardFilter()));
    UserFilter filter2 =
        tempEntity.newUserFilter(getUsername(), InternalRealm.ID, "filter2", ADVANCED_LEGAL_PACK_DASHBOARD,
            JsonUtils.format(new AdvancedLegalPackDashboardFilter()));

    HttpResponse response =
        restRequest().path(UserFilterResource.NAMED_FILTERS_PATH).query("type", ADVANCED_LEGAL_PACK_DASHBOARD).get();
    assertResponseStatus(200, response);

    List<UserFilterDTO> result = Arrays.asList(response.getBody(UserFilterDTO[].class));
    assertThat(result).isNotEmpty();
    assertFilter(result.get(0), filter1);
    assertFilter(result.get(1), filter2);
  }

  @Test
  public void deleteFilterForCurrentUserByNameAndType() throws Exception {
    UserFilter filter =
        tempEntity.newUserFilter(getUsername(), InternalRealm.ID, "filterName", ADVANCED_LEGAL_PACK_DASHBOARD,
            JsonUtils.format(new AdvancedLegalPackDashboardFilter()));

    HttpResponse response = restRequest().query("name", filter.getName()).query("type", filter.getType()).delete();
    assertResponseStatus(204, response);

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
    assertThat(actualFilter.getUsername()).isEqualTo(getUsername());
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
