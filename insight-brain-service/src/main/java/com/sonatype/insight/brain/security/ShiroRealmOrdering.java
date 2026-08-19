/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.configuration.ldap.LdapRealm;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.shiro.realm.Realm;

public final class ShiroRealmOrdering
{
  private static final List<Class<? extends Realm>> REALM_ORDER = List.of(
      InternalRealm.class,
      UserTokenRealm.class,
      LdapRealm.class,
      CrowdRealm.class,
      ReverseProxyRealm.class,
      SamlRealm.class,
      OAuth2Realm.class);

  private ShiroRealmOrdering() {
    // utility class
  }

  public static List<Realm> orderRealms(Collection<Realm> realms) {
    List<Realm> orderedRealms = new ArrayList<>();
    Set<Realm> remainingRealms = new LinkedHashSet<>(realms);

    for (Class<? extends Realm> realmType : REALM_ORDER) {
      realms.stream()
          .filter(realmType::isInstance)
          .findFirst()
          .ifPresent(realm -> {
            orderedRealms.add(realm);
            remainingRealms.remove(realm);
          });
    }

    remainingRealms.stream()
        .sorted(Comparator.comparing(realm -> realm.getClass().getName()))
        .forEach(orderedRealms::add);

    return orderedRealms;
  }
}
