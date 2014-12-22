/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ComponentSummaryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ComponentSummaryService componentSummaryService;

  @Before
  public void init() {
    tempEntity.newApplicationComponent(app.getId(), StageTypes.BUILD.getId(), "hash",
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "ver"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentSummary_ExplicitApplicationComponent_Unauthenticated() {
    assertThat(getComponentSummaryTotal(false), is(0));
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentSummary_ExplicitApplicationComponent_Unauthorized() {
    login();
    assertThat(getComponentSummaryTotal(false), is(0));
  }

  @Test
  public void testGetComponentSummary_ExplicitApplicationComponent_Authorized() {
    grantReadPermission(app.getId());
    assertThat(getComponentSummaryTotal(false), is(1));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentSummary_ImplicitApplicationComponent_Unauthenticated() {
    assertThat(getComponentSummaryTotal(true), is(0));
  }

  @Test
  public void testGetComponentSummary_ImplicitApplicationComponent_Unauthorized() {
    login();
    assertThat(getComponentSummaryTotal(true), is(0));
  }

  @Test
  public void testGetComponentSummary_ImplicitApplicationComponent_Authorized() {
    grantReadPermission(app.getId());
    assertThat(getComponentSummaryTotal(true), is(1));
  }

  private int getComponentSummaryTotal(boolean all) {
    return componentSummaryService.getComponentSummary(all ? null : Collections.singleton(app.getId()), null, null).total;
  }
}
