/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.CurrentUser;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirewallPermissionGateTest
{
  @Mock
  private AuthorizationChecker authorizationChecker;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private OwnerDAO ownerDAO;

  @InjectMocks
  private FirewallPermissionGate gate;

  @Mock
  private UserPrincipal userPrincipal;

  @Before
  public void setUp() {
    when(userPrincipal.getUsername()).thenReturn("testuser");
    when(userPrincipal.getMembership()).thenReturn(Set.of());
  }

  @Test
  public void resolvePermittedRepositoryIds_anonymousUser_throwsUnauthenticatedException() {
    when(currentUser.isAnonymous()).thenReturn(true);

    assertThatThrownBy(() -> gate.resolvePermittedRepositoryIds())
        .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  public void resolvePermittedRepositoryIds_containerReadPermission_returnsNull() {
    when(currentUser.isAnonymous()).thenReturn(false);
    when(currentUser.getUserPrincipal()).thenReturn(userPrincipal);
    when(authorizationChecker.isPermitted(
        eq(userPrincipal),
        eq(Permission.READ),
        eq(Map.of(Key.OWNER, RepositoryContainer.SINGLETON))))
            .thenReturn(true);

    Set<String> result = gate.resolvePermittedRepositoryIds();

    assertThat(result).isNull();
  }

  @Test
  public void resolvePermittedRepositoryIds_scopedAccess_returnsPermittedRepoIds() {
    Set<String> permittedIds = Set.of("repo-1", "repo-2");
    when(currentUser.isAnonymous()).thenReturn(false);
    when(currentUser.getUserPrincipal()).thenReturn(userPrincipal);
    when(authorizationChecker.isPermitted(any(), any(), any())).thenReturn(false);
    when(ownerDAO.getPermittedProxyRepositoryIds(Permission.READ, "testuser", Set.of()))
        .thenReturn(permittedIds);

    Set<String> result = gate.resolvePermittedRepositoryIds();

    assertThat(result).isEqualTo(permittedIds);
  }

  @Test
  public void resolvePermittedRepositoryIds_noPermittedRepos_throwsUnauthorizedException() {
    when(currentUser.isAnonymous()).thenReturn(false);
    when(currentUser.getUserPrincipal()).thenReturn(userPrincipal);
    when(authorizationChecker.isPermitted(any(), any(), any())).thenReturn(false);
    when(ownerDAO.getPermittedProxyRepositoryIds(Permission.READ, "testuser", Set.of()))
        .thenReturn(Set.of());

    assertThatThrownBy(() -> gate.resolvePermittedRepositoryIds())
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("No access to any proxy repository");
  }
}
