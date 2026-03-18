/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingDefaultFilterDAO;
import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingFilterDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingFilter;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.sonatype.insight.error.exception.NotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.apache.commons.lang3.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class EnterpriseReportingFilterServiceTest
    extends AbstractComponentTest
{
  @Inject
  private EnterpriseReportingFilterService enterpriseReportingFilterService;

  @Inject
  private EnterpriseReportingFilterDAO enterpriseReportingFilterDAO;

  @Inject
  private EnterpriseReportingDefaultFilterDAO enterpriseReportingDefaultFilterDAO;

  @Inject
  private UserDAO userDAO;

  @Inject
  private SamlUserDAO samlUserDAO;

  @Inject
  private OAuth2UserDAO oAuth2UserDAO;

  @Before
  public void setUpUser() {
    tempEntity.newUser(USERNAME);
  }

  @After
  public void cleanupFilters() {
    String userId = getUsedUserId();

    for (EnterpriseReportingFilter f : enterpriseReportingFilterDAO.getFiltersByUserId(userId)) {
      enterpriseReportingFilterDAO.delete(f);
    }
    assertThat(enterpriseReportingFilterDAO.getFiltersByUserId(userId)).isEmpty();
    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId)).isNull();
  }

  @Test
  public void testUpsertFilterForCurrentUser__createFilter_Internal() {
    testUpsertFilterForCurrentUser__createFilter();
  }

  @Test
  public void testUpsertFilterForCurrentUser__createFilter_for_SAML() {
    setUpSAMLUser();
    testUpsertFilterForCurrentUser__createFilter();
  }

  @Test
  public void testUpsertFilterForCurrentUser__createFilter_for_OAuth2() {
    setUpOAuth2User();
    testUpsertFilterForCurrentUser__createFilter();
  }

  private void testUpsertFilterForCurrentUser__createFilter() {
    var request = new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"k\":1}", false);
    var expectedFilter = enterpriseReportingFilterService.upsertFilterForCurrentUser(request);

    assertThat(expectedFilter).isNotNull();
    assertThat(expectedFilter.id).isNotNull();
    assertThat(expectedFilter.name).isEqualTo("Filter 1");
    assertThat(expectedFilter.filter).isEqualTo("{\"k\":1}");

    String userId = getUsedUserId();
    List<EnterpriseReportingFilter> filterList = enterpriseReportingFilterDAO.getFiltersByUserId(userId);
    assertThat(filterList).hasSize(1);
    var persistedFilter = filterList.get(0);
    assertFilterEquality(expectedFilter, persistedFilter, userId);

    // Default should not be set when isDefault=false
    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId)).isNull();
  }

  @Test
  public void testUpsertFilterForCurrentUser__createFilterAndDefault_Internal() {
    testUpsertFilterForCurrentUser__createFilterAndDefault();
  }

  @Test
  public void testUpsertFilterForCurrentUser__createFilterAndDefault_SAML() {
    setUpSAMLUser();
    testUpsertFilterForCurrentUser__createFilterAndDefault();
  }

  @Test
  public void testUpsertFilterForCurrentUser__createFilterAndDefault_OAuth2() {
    setUpOAuth2User();
    testUpsertFilterForCurrentUser__createFilterAndDefault();
  }

  private void testUpsertFilterForCurrentUser__createFilterAndDefault() {
    String userId = getUsedUserId();
    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId)).isNull();

    var request = new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"k\":1}", true);
    var expectedFilter = enterpriseReportingFilterService.upsertFilterForCurrentUser(request);

    var defaultFilter = enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId);

    assertThat(expectedFilter.id).isNotNull();
    assertThat(defaultFilter).isNotNull();
    assertThat(defaultFilter.getFilterId()).isEqualTo(expectedFilter.id);
  }

  @Test
  public void testUpsertFilterForCurrentUser__trimsName() {
    String userId = getUserId();
    var expectedFilter = enterpriseReportingFilterService.upsertFilterForCurrentUser(
        new EnterpriseReportingDashboardFilterDTO(null, " Filter's 2  ", "{}", false));
    var persistedFilter = enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, expectedFilter.id);
    assertThat(persistedFilter.getFilterName()).isEqualTo("Filter's 2");
  }

  @Test
  public void testUpsertFilterForCurrentUser__LongNameThrowsError() {
    String longName = "a".repeat(100);
    int maxLength = EnterpriseReportingFilterService.MAX_NAME_SIZE;
    var request = new EnterpriseReportingDashboardFilterDTO(null, longName, "{\"k\":1}", false);
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(request))
        .withMessageContaining("Filter name must be " + maxLength + " characters or less");
  }

  @Test
  public void testUpsertFilterForCurrentUser__CreateWithDuplicateNameThrowsError() {
    var request = new EnterpriseReportingDashboardFilterDTO(null, "My Filter", "{\"k\":1}", false);
    enterpriseReportingFilterService.upsertFilterForCurrentUser(request);

    var duplicateRequest = new EnterpriseReportingDashboardFilterDTO(null, "my filter", "{\"k\":2}", false);
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(duplicateRequest))
        .withMessageContaining(duplicateRequest.name + " is already used as a name");
  }

  @Test
  public void testUpsertFilterForCurrentUser__CreateWithReservedNameThrowsError() {
    String defaultName = EnterpriseReportingFilterService.DEFAULT_FILTER_NAME;
    var request = new EnterpriseReportingDashboardFilterDTO(null, defaultName, "{\"k\":1}", false);
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(request))
        .withMessageContaining("The name " + defaultName + " is reserved and cannot be used");
  }

  @Test
  public void testUpsertFilterForCurrentUser__invalidCharactersThrowsError() {
    var request = new EnterpriseReportingDashboardFilterDTO(null, "test&", "{\"k\":1}", false);
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(request))
        .withMessageContaining("Filter name contains an invalid character: '&'");

    var secondRequest = new EnterpriseReportingDashboardFilterDTO(null, "test/", "{\"k\":1}", false);
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(secondRequest))
        .withMessageContaining("Filter name contains an invalid character: '/'");
  }

  @Test
  public void testUpsertFilterForCurrentUser__noNameThrowsError() {
    var request = new EnterpriseReportingDashboardFilterDTO(null, "", "{\"k\":1}", false);
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(request))
        .withMessageContaining("Filter name is required");
  }

  @Test
  public void testUpsertFilterForCurrentUser__nullNameThrowsError() {
    var request = new EnterpriseReportingDashboardFilterDTO(null, null, "{\"k\":1}", false);
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(request))
        .withMessageContaining("Filter name is required");
  }

  @Test
  public void testUpsertFilterForCurrentUser__emptyWhitespaceNameThrowsError() {
    var request = new EnterpriseReportingDashboardFilterDTO(null, " ", "{\"k\":1}", false);
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(request))
        .withMessageContaining("Filter name is required");
  }

  @Test
  public void testUpsertFilterForCurrentUser__updateFilter_Internal() {
    testUpsertFilterForCurrentUser__updateFilter();
  }

  @Test
  public void testUpsertFilterForCurrentUser__updateFilter_SAML() {
    setUpSAMLUser();
    testUpsertFilterForCurrentUser__updateFilter();
  }

  @Test
  public void testUpsertFilterForCurrentUser__updateFilter_OAuth2() {
    setUpOAuth2User();
    testUpsertFilterForCurrentUser__updateFilter();
  }

  private void testUpsertFilterForCurrentUser__updateFilter() {
    String userId = getUsedUserId();
    var request = new EnterpriseReportingDashboardFilterDTO(null, "My Filter", "{\"k\":1}", false);
    var expectedFilter = enterpriseReportingFilterService.upsertFilterForCurrentUser(request);
    assertThat(enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, expectedFilter.id).getFilter())
        .isEqualTo(request.filter);

    var updateRequest =
        new EnterpriseReportingDashboardFilterDTO(expectedFilter.id, expectedFilter.name, "{\"k\":2}", false);
    enterpriseReportingFilterService.upsertFilterForCurrentUser(updateRequest);
    assertThat(enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, expectedFilter.id).getFilter())
        .isEqualTo(updateRequest.filter);
  }

  @Test
  public void testUpsertFilterForCurrentUser__updateFilterAndDefault_Internal() {
    testUpsertFilterForCurrentUser__updateFilterAndDefault();
  }

  @Test
  public void testUpsertFilterForCurrentUser__updateFilterAndDefault_SAML() {
    setUpSAMLUser();
    testUpsertFilterForCurrentUser__updateFilterAndDefault();
  }

  @Test
  public void testUpsertFilterForCurrentUser__updateFilterAndDefault_OAuth2() {
    setUpOAuth2User();
    testUpsertFilterForCurrentUser__updateFilterAndDefault();
  }

  private void testUpsertFilterForCurrentUser__updateFilterAndDefault() {
    var request = new EnterpriseReportingDashboardFilterDTO(null, "My Filter", "{\"k\":1}", false);
    var expectedFilter = enterpriseReportingFilterService.upsertFilterForCurrentUser(request);

    var updateRequest =
        new EnterpriseReportingDashboardFilterDTO(expectedFilter.id, expectedFilter.name, "{\"k\":2}", true);
    enterpriseReportingFilterService.upsertFilterForCurrentUser(updateRequest);

    String userId = getUsedUserId();
    assertThat(enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, expectedFilter.id).getFilter())
        .isEqualTo(updateRequest.filter);
    var defaultFilter = enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId);
    assertThat(defaultFilter).isNotNull();
    assertThat(defaultFilter.getFilterId()).isEqualTo(updateRequest.id);
  }

  @Test
  public void testUpsertFilterForCurrentUser__updateToDuplicateNameThrowsError() {
    var firstRequest = new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"k\":1}", false);
    var firstExpectedFilter = enterpriseReportingFilterService.upsertFilterForCurrentUser(firstRequest);

    var secondRequest = new EnterpriseReportingDashboardFilterDTO(null, "Filter 2", "{\"k\":1}", false);
    enterpriseReportingFilterService.upsertFilterForCurrentUser(secondRequest);

    var duplicateNameRequest =
        new EnterpriseReportingDashboardFilterDTO(firstExpectedFilter.id, "filter 2", "{\"k\":2}", false);
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(duplicateNameRequest))
        .withMessageContaining(duplicateNameRequest.name + " is already used as a name");
  }

  @Test
  public void testUpsertFilterForCurrentUser__updateToReservedNameThrowsError() {
    String defaultName = EnterpriseReportingFilterService.DEFAULT_FILTER_NAME;
    var request = new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"k\":1}", false);
    var expectedFilter = enterpriseReportingFilterService.upsertFilterForCurrentUser(request);
    var updateRequest = new EnterpriseReportingDashboardFilterDTO(expectedFilter.id, defaultName, "{\"k\":2}", false);

    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(updateRequest))
        .withMessageContaining("The name " + defaultName + " is reserved and cannot be used");
  }

  @Test
  public void testUpsertFilterForCurrentUser__otherUsersId__createsNewForCurrentUser() {
    var otherUsername = "other-user";
    tempEntity.newUser(otherUsername);
    String otherUserId = userDAO.getByUsernameNotNull(otherUsername).getId();
    var otherUserFilter = createFilterForOtherUser(otherUserId);

    // attempt to upsert using other’s id as current user
    var request = new EnterpriseReportingDashboardFilterDTO(otherUserFilter.getId(), "My Name", "{\"k\":1}", false);
    var expectedFilter = enterpriseReportingFilterService.upsertFilterForCurrentUser(request);

    // assert new id was generated for current user, and other user’s filter unchanged
    String currentUserId = userDAO.getByUsernameNotNull(USERNAME).getId();
    assertThat(expectedFilter.id).isNotEqualTo(otherUserFilter.getId());
    assertThat(enterpriseReportingFilterDAO.getFilterByUserAndFilterId(currentUserId, expectedFilter.id)).isNotNull();
    assertThat(enterpriseReportingFilterDAO.getFilterByUserAndFilterId(otherUserId, otherUserFilter.getId()))
        .isNotNull();
  }

  @Test
  public void testUpsertFilterForCurrentUser__badUserThrowsNotFoundException() {
    deleteInternalUser();
    var request = new EnterpriseReportingDashboardFilterDTO(null, " ", "{\"k\":1}", false);
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.upsertFilterForCurrentUser(request))
        .withMessageContaining("User not found: " + USERNAME);
  }

  @Test
  public void testGetFiltersForCurrentUser_Internal() {
    testGetFiltersForCurrentUser();
  }

  @Test
  public void testGetFiltersForCurrentUser_SAML() {
    setUpSAMLUser();
    testGetFiltersForCurrentUser();
  }

  @Test
  public void testGetFiltersForCurrentUser_OAuth2() {
    setUpOAuth2User();
    testGetFiltersForCurrentUser();
  }

  private void testGetFiltersForCurrentUser() {
    List<EnterpriseReportingDashboardFilterDTO> emptyList = enterpriseReportingFilterService.getFiltersForCurrentUser();
    assertThat(emptyList).isEmpty();

    enterpriseReportingFilterService.upsertFilterForCurrentUser(
        new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"a\":1}", false));
    enterpriseReportingFilterService.upsertFilterForCurrentUser(
        new EnterpriseReportingDashboardFilterDTO(null, "Filter 2 ", "{\"b\":2}", false));

    List<EnterpriseReportingDashboardFilterDTO> filterList =
        enterpriseReportingFilterService.getFiltersForCurrentUser();
    assertThat(filterList).isNotNull();
    assertThat(filterList).hasSize(2);
    assertThat(filterList).extracting(f -> f.name).containsExactly("Filter 1", "Filter 2");
    assertThat(filterList).extracting(f -> f.filter).containsExactly("{\"a\":1}", "{\"b\":2}");

    String userId = getUsedUserId();
    var filter1 = enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, filterList.get(0).id);
    var filter2 = enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, filterList.get(1).id);
    assertFilterEquality(filterList.get(0), filter1, userId);
    assertFilterEquality(filterList.get(1), filter2, userId);
  }

  @Test
  public void testDeleteFilterForCurrentUser_Internal() {
    testDeleteFilterForCurrentUser();
  }

  @Test
  public void testDeleteFilterForCurrentUser_SAML() {
    setUpSAMLUser();
    testDeleteFilterForCurrentUser();
  }

  @Test
  public void testDeleteFilterForCurrentUser_OAuth2() {
    setUpOAuth2User();
    testDeleteFilterForCurrentUser();
  }

  private void testDeleteFilterForCurrentUser() {
    String userId = getUsedUserId();
    var dto = new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"a\":1}", false);
    var filter = enterpriseReportingFilterService.upsertFilterForCurrentUser(dto);
    enterpriseReportingFilterService.deleteFilterForCurrentUser(filter.id);
    assertThat(enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, filter.id)).isNull();
  }

  @Test
  public void testDeleteFilterForCurrentUser__notFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.deleteFilterForCurrentUser("does-not-exist"))
        .withMessageContaining("Cannot find filter to delete.");
  }

  @Test
  public void testDeleteFilterForCurrentUser__nullId() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.deleteFilterForCurrentUser(null))
        .withMessageContaining("Filter ID cannot be null.");
  }

  @Test
  public void testDeleteFilterForCurrentUser__nullStringId() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.deleteFilterForCurrentUser("null"))
        .withMessageContaining("Filter ID cannot be null.");
  }

  @Test
  public void testDeleteFilterForCurrentUser__undefinedStringId() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.deleteFilterForCurrentUser("undefined"))
        .withMessageContaining("Filter ID cannot be null.");
  }

  @Test
  public void testDeleteFilterByOtherUser__otherUsersId__notFound() {
    var otherUsername = "other-user";
    tempEntity.newUser(otherUsername);
    String otherUserId = userDAO.getByUsernameNotNull(otherUsername).getId();
    var otherUserFilter = createFilterForOtherUser(otherUserId);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.deleteFilterForCurrentUser(otherUserFilter.getId()))
        .withMessageContaining("Cannot find filter to delete. It may have already been removed or does not exist");
  }

  @Test
  public void testGetDefaultFilterForCurrentUser_Internal() {
    testGetDefaultFilterForCurrentUser();
  }

  @Test
  public void testGetDefaultFilterForCurrentUser_SAML() {
    setUpSAMLUser();
    testGetDefaultFilterForCurrentUser();
  }

  @Test
  public void testGetDefaultFilterForCurrentUser_OAuth2() {
    setUpOAuth2User();
    testGetDefaultFilterForCurrentUser();
  }

  private void testGetDefaultFilterForCurrentUser() {
    assertThat(enterpriseReportingFilterService.getDefaultFilterForCurrentUser()).isNull();
    enterpriseReportingFilterService.upsertFilterForCurrentUser(
        new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"a\":1}", true));

    var filter = enterpriseReportingFilterService.getFiltersForCurrentUser().get(0);
    String defaultFilterId = enterpriseReportingFilterService.getDefaultFilterForCurrentUser();
    assertThat(defaultFilterId).isEqualTo(filter.id);
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__createDefault_Internal() {
    testSetDefaultFilterForCurrentUser__createDefault();
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__createDefault_SAML() {
    setUpSAMLUser();
    testSetDefaultFilterForCurrentUser__createDefault();
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__createDefault_OAuth2() {
    setUpOAuth2User();
    testSetDefaultFilterForCurrentUser__createDefault();
  }

  private void testSetDefaultFilterForCurrentUser__createDefault() {
    var filter = enterpriseReportingFilterService.upsertFilterForCurrentUser(
        new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"a\":1}", false));
    assertThat(enterpriseReportingFilterService.getDefaultFilterForCurrentUser()).isNull();

    enterpriseReportingFilterService.setDefaultFilterForCurrentUser(filter.id);
    assertThat(enterpriseReportingFilterService.getDefaultFilterForCurrentUser()).isEqualTo(filter.id);
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__updatesDefault_Internal() {
    testSetDefaultFilterForCurrentUser__updatesDefault();
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__updatesDefault_SAML() {
    setUpSAMLUser();
    testSetDefaultFilterForCurrentUser__updatesDefault();
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__updatesDefault_OAuth2() {
    setUpOAuth2User();
    testSetDefaultFilterForCurrentUser__updatesDefault();
  }

  private void testSetDefaultFilterForCurrentUser__updatesDefault() {
    var filter1 = enterpriseReportingFilterService.upsertFilterForCurrentUser(
        new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"a\":1}", true));
    var filter2 = enterpriseReportingFilterService.upsertFilterForCurrentUser(
        new EnterpriseReportingDashboardFilterDTO(null, "Filter 2", "{\"a\":2}", false));
    assertThat(enterpriseReportingFilterService.getDefaultFilterForCurrentUser()).isEqualTo(filter1.id);

    enterpriseReportingFilterService.setDefaultFilterForCurrentUser(filter2.id);
    assertThat(enterpriseReportingFilterService.getDefaultFilterForCurrentUser()).isEqualTo(filter2.id);
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__noExistingFilterThrowsError() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.setDefaultFilterForCurrentUser("12345"))
        .withMessageContaining("Filter does not already exist to mark as default");
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__noFilterIdThrowsError() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.setDefaultFilterForCurrentUser(null))
        .withMessageContaining("Filter does not already exist to mark as default");
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__otherUsersId__notFound() {
    var otherUsername = "other-user";
    tempEntity.newUser(otherUsername);
    String otherUserId = userDAO.getByUsernameNotNull(otherUsername).getId();
    var otherUserFilter = createFilterForOtherUser(otherUserId);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> enterpriseReportingFilterService.setDefaultFilterForCurrentUser(otherUserFilter.getId()))
        .withMessageContaining("Filter does not already exist to mark as default");
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__sameIdTwiceWithNoError_Internal() {
    testSetDefaultFilterForCurrentUser__sameIdTwiceWithNoError();
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__sameIdTwiceWithNoError_SAML() {
    setUpSAMLUser();
    testSetDefaultFilterForCurrentUser__sameIdTwiceWithNoError();
  }

  @Test
  public void testSetDefaultFilterForCurrentUser__sameIdTwiceWithNoError_OAuth2() {
    setUpOAuth2User();
    testSetDefaultFilterForCurrentUser__sameIdTwiceWithNoError();
  }

  private void testSetDefaultFilterForCurrentUser__sameIdTwiceWithNoError() {
    var filter = enterpriseReportingFilterService.upsertFilterForCurrentUser(
        new EnterpriseReportingDashboardFilterDTO(null, "My Filter", "{}", false));
    enterpriseReportingFilterService.setDefaultFilterForCurrentUser(filter.id);
    enterpriseReportingFilterService.setDefaultFilterForCurrentUser(filter.id);
    assertThat(enterpriseReportingFilterService.getDefaultFilterForCurrentUser()).isEqualTo(filter.id);
  }

  @Test
  public void testDeleteDefaultFilter_Internal() {
    testDeleteDefaultFilter();
  }

  @Test
  public void testDeleteDefaultFilter_SAML() {
    setUpSAMLUser();
    testDeleteDefaultFilter();
  }

  @Test
  public void testDeleteDefaultFilter_OAuth2() {
    setUpOAuth2User();
    testDeleteDefaultFilter();
  }

  private void testDeleteDefaultFilter() {
    var filter = enterpriseReportingFilterService.upsertFilterForCurrentUser(
        new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"a\":1}", true));
    String userId = getUsedUserId();

    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId).getFilterId()).isEqualTo(filter.id);
    enterpriseReportingFilterService.deleteDefaultFilterForCurrentUser();
    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId)).isNull();
  }

  @Test
  public void testDeleteDefaultFilter__noExistingDefaultIsNoop() {
    assertThatCode(() -> enterpriseReportingFilterService.deleteDefaultFilterForCurrentUser())
        .doesNotThrowAnyException();
  }

  @Test
  public void testDeleteDefaultFilter__removedByCascade_Internal() {
    testDeleteDefaultFilter__removedByCascade();
  }

  @Test
  public void testDeleteDefaultFilter__removedByCascade_SAML() {
    setUpSAMLUser();
    testDeleteDefaultFilter__removedByCascade();
  }

  @Test
  public void testDeleteDefaultFilter__removedByCascade_OAuth2() {
    setUpOAuth2User();
    testDeleteDefaultFilter__removedByCascade();
  }

  private void testDeleteDefaultFilter__removedByCascade() {
    var filter = enterpriseReportingFilterService.upsertFilterForCurrentUser(
        new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"a\":1}", true));
    String userId = getUsedUserId();

    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId).getFilterId()).isEqualTo(filter.id);
    enterpriseReportingFilterService.deleteFilterForCurrentUser(filter.id);

    assertThat(enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, filter.id)).isNull();
    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId)).isNull();
  }

  private String getUserId() {
    User user = userDAO.getByUsername(USERNAME);

    if (user != null) {
      return user.getId();
    }

    return null;
  }

  private String getSAMLUserId() {
    try {
      return samlUserDAO.getByUsernameNotNull(USERNAME).getId();
    }
    catch (Exception e) {
      return null;
    }
  }

  private String getOAuth2UserId() {
    try {
      return oAuth2UserDAO.getByUsernameNotNull(USERNAME).getId();
    }
    catch (Exception e) {
      return null;
    }
  }

  private String getUsedUserId() {
    String userId = getUserId();

    if (StringUtils.isNotBlank(userId)) {
      return userId;
    }

    userId = getSAMLUserId();

    if (StringUtils.isNotBlank(userId)) {
      return userId;
    }

    return getOAuth2UserId();
  }

  public void setUpSAMLUser() {
    deleteInternalUser();
    tempEntity.newSamlUser(USERNAME);

    enableSsoWithSaml();
    disableSsoWithOAuth2();
  }

  public void setUpOAuth2User() {
    deleteInternalUser();
    tempEntity.newOAuth2User(USERNAME);

    enableSsoWithOAuth2();
    disableSsoWithSaml();
  }

  private void deleteInternalUser() {
    User user = userDAO.getByUsernameNotNull(USERNAME);
    userDAO.delete(user);
  }

  private void assertFilterEquality(
      EnterpriseReportingDashboardFilterDTO response,
      EnterpriseReportingFilter daoFilter,
      String userId)
  {
    assertThat(daoFilter.getFilterName()).isEqualTo(response.name);
    assertThat(daoFilter.getId()).isEqualTo(response.id);
    assertThat(daoFilter.getFilter()).isEqualTo(response.filter);
    assertThat(daoFilter.getUserId()).isEqualTo(userId);
  }

  private EnterpriseReportingFilter createFilterForOtherUser(String otherUserId) {
    // Create a filter owned by another user (via DAO)
    var otherUserFilter = new EnterpriseReportingFilter();
    otherUserFilter.setId(IdUtil.newUUID());
    otherUserFilter.setUserId(otherUserId);
    otherUserFilter.setFilterName("Other filter");
    otherUserFilter.setFilter("{}");
    enterpriseReportingFilterDAO.insert(otherUserFilter);

    return otherUserFilter;
  }
}
