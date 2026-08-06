/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;

@Entity
@Table(name = "persisted_user_session")
public class PersistedUserSession
    implements HasStringId
{
  private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

  @Id
  @Column(name = "persisted_user_session_id")
  private String id;

  @Column(name = "session_json")
  private String sessionJson;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
    SimpleSession session = getSession();
    session.setId(this.id);
    setSession(session);
  }

  public PersistedUserSession() {
  }

  public PersistedUserSession(SimpleSession session) {
    setSession(session);
  }

  public String getSessionJson() {
    return sessionJson;
  }

  public SimpleSession getSession() {
    return simpleSessionFromJson(sessionJson);
  }

  public static SimpleSession simpleSessionFromJson(String sessionJson) {
    try {
      JsonNode sessionNode = OBJECT_MAPPER.readTree(sessionJson);
      SimpleSession session = OBJECT_MAPPER.convertValue(sessionNode, SimpleSession.class);
      JsonNode principalCollectionNode = sessionNode.findValue(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
      if (principalCollectionNode != null) {
        // Fix jackson deserializing PrincipalCollection as an ArrayList and UserPrincipal as a LinkedHashMap
        List<UserPrincipal> userPrincipals = OBJECT_MAPPER.readValue(
            OBJECT_MAPPER.treeAsTokens(principalCollectionNode),
            OBJECT_MAPPER.getTypeFactory().constructType(new TypeReference<List<UserPrincipal>>()
            {
            }));
        SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection();
        userPrincipals
            .forEach(userPrincipal -> simplePrincipalCollection.add(userPrincipal, userPrincipal.getRealmId()));
        session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, simplePrincipalCollection);
      }
      return session;
    }
    catch (IllegalArgumentException e) {
      throw new UnknownSessionException(e.getMessage(), e);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }

  public void setSession(SimpleSession session) {
    this.id = session.getId() == null ? null : session.getId().toString();
    sessionJson = simpleSessionToJson(session);
  }

  public static String simpleSessionToJson(SimpleSession session) {
    try {
      return OBJECT_MAPPER.writeValueAsString(session);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }

  private static ObjectMapper createObjectMapper() {
    return JsonMapper.builder() //
        .configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true) //
        .build();
  }
}
