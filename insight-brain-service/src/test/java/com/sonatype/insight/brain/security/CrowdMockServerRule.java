/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.integration.rest.entity.GroupEntity;
import com.atlassian.crowd.integration.rest.entity.GroupEntityList;
import com.atlassian.crowd.integration.rest.entity.PasswordEntity;
import com.atlassian.crowd.integration.rest.entity.UserEntity;
import com.atlassian.crowd.integration.rest.entity.UserEntityList;
import com.atlassian.crowd.model.group.GroupType;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.rules.ExternalResource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class CrowdMockServerRule
    extends ExternalResource
{
  private WireMockServer crowdMockServer;

  @Override
  protected void before() {
    crowdMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    crowdMockServer.start();
  }

  @Override
  protected void after() {
    crowdMockServer.stop();
  }

  public String getBaseUrl() {
    return crowdMockServer.baseUrl();
  }

  public void mockGetUser(String username, String displayName) throws Exception {
    UserEntity userEntity =
        new UserEntity(username, "firstName", "lastName", displayName, "email", new PasswordEntity("password"), true,
            null);
    mockGetUser(userEntity);
  }

  public void mockGetUser(UserEntity userEntity) throws Exception {
    crowdMockServer.stubFor(get(urlPathMatching("/crowd/rest/usermanagement/1/user")).withQueryParam("username",
        equalTo(userEntity.getName()))
        .willReturn(
            aResponse().withHeader("X-Embedded-Crowd-Version", "version")
                .withBody(marshall(userEntity))
                .withStatus(200)));
  }

  public void mockGetUserError(String username, int status) {
    crowdMockServer.stubFor(
        get(urlPathMatching("/crowd/rest/usermanagement/1/user")).withQueryParam("username", equalTo(username))
            .willReturn(
                aResponse().withHeader("X-Embedded-Crowd-Version", "version").withBody("Error").withStatus(status)));
  }

  public void mockAuthenticateUser(String username, String displayName) throws Exception {
    UserEntity userEntity =
        new UserEntity(username, "firstName", "lastName", displayName, "email", new PasswordEntity("password"),
            true, null);
    mockAuthenticateUser(userEntity);
  }

  public void mockAuthenticateUser(UserEntity userEntity) throws Exception {
    crowdMockServer.stubFor(
        post(urlPathMatching("/crowd/rest/usermanagement/1/authentication")).withQueryParam("username",
            equalTo(userEntity.getName()))
            .willReturn(aResponse().withHeader("X-Embedded-Crowd-Version", "version")
                .withBody(marshall(userEntity))
                .withStatus(200)));
  }

  public void mockAuthenticateUserError(String username, int status) {
    crowdMockServer.stubFor(
        post(urlPathMatching("/crowd/rest/usermanagement/1/authentication")).withQueryParam("username",
            equalTo(username))
            .willReturn(
                aResponse().withHeader("X-Embedded-Crowd-Version", "version").withBody("Error").withStatus(status)));
  }

  public void mockGetGroupsForNestedUser(String username, String... groupNames) throws Exception {
    List<GroupEntity> groupEntities = new ArrayList<>();
    for (String groupName : groupNames) {
      groupEntities.add(new GroupEntity(groupName, "description", GroupType.GROUP, true));
    }
    mockGetGroupsForNestedUser(username, groupEntities.toArray(new GroupEntity[0]));
  }

  public void mockGetGroupsForNestedUser(String username, GroupEntity... groupEntities) throws Exception {
    GroupEntityList groupEntityList = new GroupEntityList(Arrays.asList(groupEntities));
    crowdMockServer.stubFor(
        get(urlPathMatching("/crowd/rest/usermanagement/1/user/group/nested")).withQueryParam("username",
            equalTo(username))
            .withQueryParam("start-index", equalTo("0"))
            .withQueryParam("max-results", equalTo("-1"))
            .withQueryParam("expand", equalTo("group"))
            .willReturn(
                aResponse().withHeader("X-Embedded-Crowd-Version", "version")
                    .withBody(marshall(groupEntityList))
                    .withStatus(200)));
  }

  public void mockGetGroupsForNestedUserError(String username, int status) {
    crowdMockServer.stubFor(
        get(urlPathMatching("/crowd/rest/usermanagement/1/user/group/nested")).withQueryParam("username",
            equalTo(username))
            .withQueryParam("start-index", equalTo("0"))
            .withQueryParam("max-results", equalTo("-1"))
            .withQueryParam("expand", equalTo("group"))
            .willReturn(
                aResponse().withHeader("X-Embedded-Crowd-Version", "version").withBody("Error").withStatus(status)));
  }

  public void mockTestConnection() throws Exception {
    UserEntityList userEntityList = new UserEntityList(Collections.emptyList());
    crowdMockServer.stubFor(
        post(urlPathMatching("/crowd/rest/usermanagement/1/search")).withQueryParam("entity-type",
            equalTo("user"))
            .withQueryParam("start-index", equalTo("0"))
            .withQueryParam("max-results", equalTo("1"))
            .withQueryParam("expand", equalTo("user"))
            .willReturn(
                aResponse().withHeader("X-Embedded-Crowd-Version", "version")
                    .withBody(marshall(userEntityList))
                    .withStatus(200)));
  }

  public void mockTestConnectionError(int status) {
    crowdMockServer.stubFor(
        post(urlPathMatching("/crowd/rest/usermanagement/1/search")).withQueryParam("entity-type",
            equalTo("user"))
            .withQueryParam("start-index", equalTo("0"))
            .withQueryParam("max-results", equalTo("1"))
            .withQueryParam("expand", equalTo("user"))
            .willReturn(
                aResponse().withHeader("X-Embedded-Crowd-Version", "version").withBody("Error").withStatus(status)));
  }

  public void mockSearchUsers(SearchRestriction searchRestriction, String... usernames) throws Exception {
    List<UserEntity> userEntities = new ArrayList<>();
    for (String username : usernames) {
      userEntities.add(new UserEntity(username, "firstName", "lastName", username + "DisplayName", username + "Email",
          new PasswordEntity("password"), true, null));
    }
    mockSearchUsers(searchRestriction, userEntities.toArray(new UserEntity[0]));
  }

  public void mockSearchUsers(SearchRestriction searchRestriction, UserEntity... userEntities) throws Exception {
    UserEntityList userEntityList = new UserEntityList(Arrays.asList(userEntities));
    crowdMockServer.stubFor(
        post(urlPathMatching("/crowd/rest/usermanagement/1/search")).withQueryParam("entity-type", equalTo("user"))
            .withQueryParam("start-index", equalTo("0"))
            .withQueryParam("max-results", equalTo("-1"))
            .withQueryParam("expand", equalTo("user"))
            .willReturn(
                aResponse().withHeader("X-Embedded-Crowd-Version", "version").withBody(marshall(userEntityList))));
  }

  public void mockSearchUsersError(SearchRestriction searchRestriction, int status) throws Exception {
    crowdMockServer.stubFor(
        post(urlPathMatching("/crowd/rest/usermanagement/1/search")).withQueryParam("entity-type", equalTo("user"))
            .withQueryParam("start-index", equalTo("0"))
            .withQueryParam("max-results", equalTo("-1"))
            .withQueryParam("expand", equalTo("user"))
            .willReturn(
                aResponse().withHeader("X-Embedded-Crowd-Version", "version").withBody("Error").withStatus(status)));
  }

  public void mockSearchGroups(SearchRestriction searchRestriction, String... groupNames) throws Exception {
    List<GroupEntity> groupEntities = new ArrayList<>();
    for (String groupName : groupNames) {
      groupEntities.add(new GroupEntity(groupName, "description", GroupType.GROUP, true));
    }
    mockSearchGroups(searchRestriction, groupEntities.toArray(new GroupEntity[0]));
  }

  public void mockSearchGroups(SearchRestriction searchRestriction, GroupEntity... groupEntities) throws Exception {
    GroupEntityList groupEntityList = new GroupEntityList(Arrays.asList(groupEntities));
    crowdMockServer.stubFor(
        post(urlPathMatching("/crowd/rest/usermanagement/1/search")).withQueryParam("entity-type", equalTo("group"))
            .withQueryParam("start-index", equalTo("0"))
            .withQueryParam("max-results", equalTo("-1"))
            .withQueryParam("expand", equalTo("group"))
            .willReturn(
                aResponse().withHeader("X-Embedded-Crowd-Version", "version").withBody(marshall(groupEntityList))));
  }

  public void mockSearchGroupsError(SearchRestriction searchRestriction, int status) throws Exception {
    crowdMockServer.stubFor(
        post(urlPathMatching("/crowd/rest/usermanagement/1/search")).withQueryParam("entity-type", equalTo("group"))
            .withQueryParam("start-index", equalTo("0"))
            .withQueryParam("max-results", equalTo("-1"))
            .withQueryParam("expand", equalTo("group"))
            .willReturn(
                aResponse().withHeader("X-Embedded-Crowd-Version", "version").withBody("Error").withStatus(status)));
  }

  public void mockGetNestedUsersOfGroup(String groupName, UserEntity... userEntities) throws Exception {
    UserEntityList userEntityList = new UserEntityList(Arrays.asList(userEntities));
    crowdMockServer.stubFor(
        get(urlPathMatching("/crowd/rest/usermanagement/1/group/user/nested"))
            .withQueryParam("groupname", equalTo(groupName))
            .withQueryParam("start-index", equalTo("0"))
            .withQueryParam("max-results", equalTo("-1"))
            .withQueryParam("expand", equalTo("user"))
            .willReturn(aResponse().withHeader("X-Embedded-Crowd-Version", "version")
                .withBody(marshall(userEntityList))));
  }

  public void mockGetNestedUsersOfGroupError(String groupName, int status) {
    crowdMockServer.stubFor(
        get(urlPathMatching("/crowd/rest/usermanagement/1/group/user/nested"))
            .withQueryParam("groupname", equalTo(groupName))
            .withQueryParam("start-index", equalTo("0"))
            .withQueryParam("max-results", equalTo("-1"))
            .withQueryParam("expand", equalTo("user"))
            .willReturn(aResponse().withHeader("X-Embedded-Crowd-Version", "version")
                .withBody("Error")
                .withStatus(status)));
  }

  @SuppressWarnings("unchecked")
  private String marshall(Object object) throws Exception {
    StringWriter writer = new StringWriter();
    JAXBContext context = JAXBContext.newInstance(object.getClass());
    Marshaller marshaller = context.createMarshaller();
    // Wrap in JAXBElement to handle classes without @XmlRootElement annotation
    JAXBElement<?> element = new JAXBElement<>(
        new QName(object.getClass().getSimpleName()),
        (Class<Object>) object.getClass(),
        object);
    marshaller.marshal(element, writer);
    return writer.toString();
  }
}
