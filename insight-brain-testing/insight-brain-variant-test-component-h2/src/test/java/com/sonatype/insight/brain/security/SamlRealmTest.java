/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class SamlRealmTest
    extends AbstractComponentH2Test
{
  @Inject
  private SamlRealm samlRealm;

  @Mock
  private ProductLicense mockProductLicense;

  @Inject
  private SamlUserDAO samlUserDAO;

  @Inject
  private SamlConfigurationService samlConfigurationService;

  @BeforeEach
  public void before() {
    enableSsoWithSaml();
  }

  @AfterEach
  public void after() {
    samlUserDAO.getAll().forEach(samlUserDAO::delete);
  }

  @Test
  public void testSamlRealm() {
    assertThat(samlRealm.getCredentialsMatcher()).isInstanceOf(AllowAllCredentialsMatcher.class);
    assertThat(samlRealm.getName()).isEqualTo("SAML");
    assertThat(samlRealm.getAuthenticationTokenClass()).isSameAs(SamlAuthenticationToken.class);
  }

  @Test
  public void testDoGetAuthenticationInfo() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();

    Attrs attributes = new Attrs();
    attributes.add(samlConfiguration.getUsernameAttributeName(), "jonny");
    attributes.add(samlConfiguration.getFirstNameAttributeName(), "john");
    attributes.add(samlConfiguration.getGroupsAttributeName(), "group1");
    Attrs friendlyAttributes = new Attrs();
    friendlyAttributes.add(samlConfiguration.getUsernameAttributeName(), "jonny2");
    friendlyAttributes.add(samlConfiguration.getLastNameAttributeName(), "smith");
    friendlyAttributes.add(samlConfiguration.getGroupsAttributeName(), "group2");

    UserPrincipal userPrincipal = getUserPrincipal(doGetAuthenticationInfo(attributes, friendlyAttributes));
    assertThat(userPrincipal.getUsername()).isEqualTo("jonny");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("john smith");
    assertThat(userPrincipal.getMembership())
        .containsExactlyInAnyOrder("group1", "group2", Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  @Test
  public void testDoGetAuthenticationInfo_NoAttributes() {
    UserPrincipal userPrincipal =
        getUserPrincipal(doGetAuthenticationInfo(new Attrs(), new Attrs()));
    assertThat(userPrincipal.getUsername()).isEqualTo("name");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("name");
    assertThat(userPrincipal.getMembership()).containsExactly(Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  @Test
  public void testDoGetAuthenticationInfo_UsernameInAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();

    Attrs attributes = new Attrs();
    attributes.add(samlConfiguration.getUsernameAttributeName(), "jonny");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, new Attrs())).getUsername())
        .isEqualTo("jonny");
  }

  @Test
  public void testDoGetAuthenticationInfo_UsernameInFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();

    Attrs friendlyAttributes = new Attrs();
    friendlyAttributes.add(samlConfiguration.getUsernameAttributeName(), "jonny");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(new Attrs(), friendlyAttributes)).getUsername())
        .isEqualTo("jonny");
  }

  @Test
  public void testDoGetAuthenticationInfo_UsernameInAttributesAndFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs attributes = new Attrs();
    attributes.add(samlConfiguration.getUsernameAttributeName(), "jonny1");
    Attrs friendlyAttributes = new Attrs();
    friendlyAttributes.add(samlConfiguration.getUsernameAttributeName(), "jonny2");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, friendlyAttributes)).getUsername())
        .isEqualTo("jonny1");
  }

  @Test
  public void testDoGetAuthenticationInfo_NoUsername() {
    assertThatThrownBy(() -> doGetAuthenticationInfo(null, new Attrs(), new Attrs()))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("username is required");
  }

  @Test
  public void testDoGetAuthenticationInfo_FirstNameInAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs attributes = new Attrs();
    attributes.add(samlConfiguration.getFirstNameAttributeName(), "john");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, new Attrs())).getDisplayName())
        .isEqualTo("john");
  }

  @Test
  public void testDoGetAuthenticationInfo_FirstNameInFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs friendlyAttributes = new Attrs();
    friendlyAttributes.add(samlConfiguration.getFirstNameAttributeName(), "john");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(new Attrs(), friendlyAttributes))
        .getDisplayName()).isEqualTo("john");
  }

  @Test
  public void testDoGetAuthenticationInfo_FirstNameInAttributesAndFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs attributes = new Attrs();
    attributes.add(samlConfiguration.getFirstNameAttributeName(), "john1");
    Attrs friendlyAttributes = new Attrs();
    friendlyAttributes.add(samlConfiguration.getFirstNameAttributeName(), "john2");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, friendlyAttributes)).getDisplayName())
        .isEqualTo("john1");
  }

  @Test
  public void testDoGetAuthenticationInfo_LastNameInAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs attributes = new Attrs();
    attributes.add(samlConfiguration.getLastNameAttributeName(), "smith");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, new Attrs())).getDisplayName())
        .isEqualTo("smith");
  }

  @Test
  public void testDoGetAuthenticationInfo_LastNameInFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs friendlyAttributes = new Attrs();
    friendlyAttributes.add(samlConfiguration.getLastNameAttributeName(), "smith");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(new Attrs(), friendlyAttributes))
        .getDisplayName()).isEqualTo("smith");
  }

  @Test
  public void testDoGetAuthenticationInfo_LastNameInAttributesAndFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs attributes = new Attrs();
    attributes.add(samlConfiguration.getLastNameAttributeName(), "smith1");
    Attrs friendlyAttributes = new Attrs();
    friendlyAttributes.add(samlConfiguration.getLastNameAttributeName(), "smith2");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, friendlyAttributes)).getDisplayName())
        .isEqualTo("smith1");
  }

  @Test
  public void testDoGetAuthenticationInfo_GroupsInAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs attributes = new Attrs();
    attributes.addAll(samlConfiguration.getGroupsAttributeName(), "group1", "group2", "", " ", null);

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, new Attrs())).getMembership())
        .containsExactlyInAnyOrder("group1", "group2", Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  // These friendly-attribute tests exercise SamlRealm.getAllAttributes() against the SamlPrincipalAttributes
  // contract using a test principal that populates the friendly-attribute accessors. The production
  // SpringSamlPrincipal folds FriendlyNames into the formal-name map upstream and returns empty from
  // getFriendlyAttributes(), so its friendly branch is a no-op; SamlRealm still supports both because it is
  // coded against the interface, not that one implementation.
  @Test
  public void testDoGetAuthenticationInfo_GroupsInFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs friendlyAttributes = new Attrs();
    friendlyAttributes.addAll(samlConfiguration.getGroupsAttributeName(), "group1", "group2", "", " ", null);

    assertThat(getUserPrincipal(doGetAuthenticationInfo(new Attrs(), friendlyAttributes))
        .getMembership()).containsExactlyInAnyOrder("group1", "group2", Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  @Test
  public void testDoGetAuthenticationInfo_GroupsInAttributesAndFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs attributes = new Attrs();
    attributes.addAll(samlConfiguration.getGroupsAttributeName(), "group1", "group2", "", " ", null);
    Attrs friendlyAttributes = new Attrs();
    friendlyAttributes.addAll(samlConfiguration.getGroupsAttributeName(), "group3", "group4", "", " ", null);

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, friendlyAttributes)).getMembership())
        .containsExactlyInAnyOrder("group1", "group2", "group3", "group4", Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  @Test
  public void testDoGetAuthenticationInfo_MultipleAttributeValues() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs attributes = new Attrs();
    attributes.addAll(samlConfiguration.getUsernameAttributeName(), "jonny", "jonny2");
    attributes.addAll(samlConfiguration.getFirstNameAttributeName(), "john", "john2");
    attributes.addAll(samlConfiguration.getLastNameAttributeName(), "smith", "smith2");

    UserPrincipal userPrincipal = getUserPrincipal(doGetAuthenticationInfo(attributes, new Attrs()));
    assertThat(userPrincipal.getUsername()).isEqualTo("jonny");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("john smith");
  }

  @Test
  public void testDoGetAuthenticationInfo_MultipleFriendlyAttributeValues() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs friendlyAttributes = new Attrs();
    friendlyAttributes.addAll(samlConfiguration.getUsernameAttributeName(), "jonny", "jonny2");
    friendlyAttributes.addAll(samlConfiguration.getFirstNameAttributeName(), "john", "john2");
    friendlyAttributes.addAll(samlConfiguration.getLastNameAttributeName(), "smith", "smith2");

    UserPrincipal userPrincipal =
        getUserPrincipal(doGetAuthenticationInfo(new Attrs(), friendlyAttributes));
    assertThat(userPrincipal.getUsername()).isEqualTo("jonny");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("john smith");
  }

  @Test
  public void testDoGetAuthenticationInfo_InsertsSamlUser() {
    SamlUser samlUser = createSamlUser();
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs attributes = new Attrs();
    attributes.add(samlConfiguration.getUsernameAttributeName(), samlUser.getUsername());
    attributes.add(samlConfiguration.getFirstNameAttributeName(), samlUser.getFirstName());
    attributes.add(samlConfiguration.getLastNameAttributeName(), samlUser.getLastName());
    attributes.add(samlConfiguration.getEmailAttributeName(), samlUser.getEmail());
    attributes.addAll(samlConfiguration.getGroupsAttributeName(), new ArrayList<>(samlUser.getGroups()));

    UserPrincipal userPrincipal = getUserPrincipal(doGetAuthenticationInfo(new Attrs(), attributes));

    SamlUser storedSamlUser = samlUserDAO.getByUsername(samlUser.getUsername());
    assertThat(storedSamlUser).isNotNull();
    samlUser.setId(storedSamlUser.getId());
    assertThat(storedSamlUser).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlUser);
    assertThat(userPrincipal.getUsername()).isEqualTo(samlUser.getUsername());
    assertThat(userPrincipal.getDisplayName()).isEqualTo(samlUser.calculateDisplayName());
    assertThat(userPrincipal.getRealmId()).isEqualTo(SamlRealm.ID);
    Set<String> expectedGroups = new LinkedHashSet<>(samlUser.getGroups());
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(userPrincipal.getMembership()).isEqualTo(expectedGroups);
  }

  @Test
  public void testDoGetAuthenticationInfo_UpdatesSamlUser() {
    SamlUser samlUser = tempEntity.newSamlUser();
    samlUser.setFirstName(samlUser.getFirstName() + "2");
    samlUser.setLastName(samlUser.getLastName() + "2");
    samlUser.setEmail(samlUser.getEmail() + "2");
    samlUser.setGroups(new LinkedHashSet<>(Arrays.asList("someGroup3", "someGroup4")));
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    Attrs attributes = new Attrs();
    attributes.add(samlConfiguration.getUsernameAttributeName(), samlUser.getUsername());
    attributes.add(samlConfiguration.getFirstNameAttributeName(), samlUser.getFirstName());
    attributes.add(samlConfiguration.getLastNameAttributeName(), samlUser.getLastName());
    attributes.add(samlConfiguration.getEmailAttributeName(), samlUser.getEmail());
    attributes.addAll(samlConfiguration.getGroupsAttributeName(), new ArrayList<>(samlUser.getGroups()));

    UserPrincipal userPrincipal = getUserPrincipal(doGetAuthenticationInfo(new Attrs(), attributes));

    SamlUser storedSamlUser = samlUserDAO.getByUsername(samlUser.getUsername());
    assertThat(storedSamlUser).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlUser);
    assertThat(userPrincipal.getUsername()).isEqualTo(samlUser.getUsername());
    assertThat(userPrincipal.getDisplayName()).isEqualTo(samlUser.calculateDisplayName());
    assertThat(userPrincipal.getRealmId()).isEqualTo(SamlRealm.ID);
    Set<String> expectedGroups = new LinkedHashSet<>(samlUser.getGroups());
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(userPrincipal.getMembership()).isEqualTo(expectedGroups);
  }

  private AuthenticationInfo doGetAuthenticationInfo(String name, Attrs attributes, Attrs friendlyAttributes) {
    SamlAuthenticationToken samlAuthenticationToken =
        new SamlAuthenticationToken(new MapBackedSamlPrincipal(name, attributes, friendlyAttributes));
    return samlRealm.doGetAuthenticationInfo(samlAuthenticationToken);
  }

  private AuthenticationInfo doGetAuthenticationInfo(Attrs attributes, Attrs friendlyAttributes) {
    return doGetAuthenticationInfo("name", attributes, friendlyAttributes);
  }

  private UserPrincipal getUserPrincipal(AuthenticationInfo authenticationInfo) {
    assertThat(authenticationInfo).isInstanceOf(SimpleAuthenticationInfo.class);
    SimpleAuthenticationInfo simpleAuthenticationInfo = (SimpleAuthenticationInfo) authenticationInfo;
    assertThat(simpleAuthenticationInfo.getCredentials()).isNull();
    assertThat(simpleAuthenticationInfo.getPrincipals()).isNotEmpty();
    assertThat(simpleAuthenticationInfo.getPrincipals().getRealmNames()).containsExactly("SAML");
    Object primaryPrincipal = simpleAuthenticationInfo.getPrincipals().getPrimaryPrincipal();
    assertThat(primaryPrincipal).isInstanceOf(UserPrincipal.class);
    UserPrincipal userPrincipal = (UserPrincipal) primaryPrincipal;
    assertThat(userPrincipal.getRealmId()).isEqualTo(SamlRealm.ID);
    return userPrincipal;
  }

  private SamlUser createSamlUser() {
    return new SamlUser("someUsername", "someFirstName", "someLastName", "someEmail@someDomain.com",
        new LinkedHashSet<>(Arrays.asList("someGroup1", "someGroup2")));
  }

  /**
   * Multi-valued attribute map with Keycloak-style {@code add}/{@code addAll} helpers.
   */
  private static class Attrs
      extends LinkedHashMap<String, List<String>>
  {
    void add(String key, String value) {
      computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    void addAll(String key, String... values) {
      computeIfAbsent(key, k -> new ArrayList<>()).addAll(Arrays.asList(values));
    }

    void addAll(String key, List<String> values) {
      computeIfAbsent(key, k -> new ArrayList<>()).addAll(values);
    }
  }

  /**
   * Test {@link SamlPrincipalAttributes} exposing both formal and friendly attributes, matching the
   * dual-map semantics that {@link SamlRealm} resolves against.
   */
  private static class MapBackedSamlPrincipal
      implements SamlPrincipalAttributes
  {
    private final String name;

    private final Map<String, List<String>> attributes;

    private final Map<String, List<String>> friendlyAttributes;

    MapBackedSamlPrincipal(
        String name,
        Map<String, List<String>> attributes,
        Map<String, List<String>> friendlyAttributes)
    {
      this.name = name;
      this.attributes = attributes;
      this.friendlyAttributes = friendlyAttributes;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getAttribute(String attributeName) {
      List<String> values = getAttributes(attributeName);
      return values.isEmpty() ? null : values.get(0);
    }

    @Override
    public List<String> getAttributes(String attributeName) {
      return attributes.getOrDefault(attributeName, Collections.emptyList());
    }

    @Override
    public String getFriendlyAttribute(String attributeName) {
      List<String> values = getFriendlyAttributes(attributeName);
      return values.isEmpty() ? null : values.get(0);
    }

    @Override
    public List<String> getFriendlyAttributes(String attributeName) {
      return friendlyAttributes.getOrDefault(attributeName, Collections.emptyList());
    }

    @Override
    public Set<String> getFriendlyNames() {
      return friendlyAttributes.keySet();
    }

    @Override
    public Map<String, List<String>> getAllAttributes() {
      return attributes;
    }
  }
}
