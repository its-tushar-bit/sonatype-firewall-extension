/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;

import com.ning.http.client.Response;

import com.yammer.dropwizard.testing.JsonHelpers;

import org.junit.Before;
import org.junit.Rule;

/**
 * Provides boilerplate fixture for authorization tests.
 */
public abstract class AbstractResourceAuthzTest
    extends AbstractResourceTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  protected Organization org;

  protected Application app;

  protected User unauthorized;

  protected User authorized;

  @Before
  public void createEntities() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    unauthorized = tempEntity.newUser();
    authorized = tempEntity.newUser();
  }

  protected String getRestUrl(String templateUrl, Object... paramValues) {
    return getRestBaseUrl() + expandRestUrl(templateUrl, paramValues);
  }

  protected String toJson(Object object) {
    try {
      return JsonHelpers.asJson(object);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected <T> T fromJson(Response response, Class<T> type) {
    try {
      return JsonHelpers.fromJson(response.getResponseBody(), type);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
