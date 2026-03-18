/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.keycloak.adapters.saml.SamlPrincipal;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.dom.saml.v2.assertion.AssertionType;

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
      // Fix jackson deserializing CurrentAction as a String
      setSamlCurrentActionIfNeeded(session);
      // Fix jackson deserializing SamlSession as a LinkedHashMap
      setSamlSessionIfNeeded(sessionNode, session);
      return session;
    }
    catch (IllegalArgumentException e) {
      throw new UnknownSessionException(e.getMessage(), e);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }

  private static void setSamlCurrentActionIfNeeded(SimpleSession session) {
    Object samlCurrentActionAttribute = session.getAttribute(SamlSessionStore.CURRENT_ACTION);
    if (samlCurrentActionAttribute instanceof String) {
      session.setAttribute(SamlSessionStore.CURRENT_ACTION,
          SamlSessionStore.CurrentAction.valueOf((String) samlCurrentActionAttribute));
    }
  }

  private static void setSamlSessionIfNeeded(JsonNode sessionNode, SimpleSession session) throws IOException {
    JsonNode samlSessionNode = sessionNode.findValue(SamlSession.class.getName());
    if (samlSessionNode != null) {
      session.setAttribute(SamlSession.class.getName(),
          OBJECT_MAPPER.readValue(OBJECT_MAPPER.treeAsTokens(samlSessionNode), SamlSession.class));
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
    ObjectMapper objectMapper = JsonMapper.builder() //
        .configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true) //
        .addMixIn(SamlSession.class, SamlSessionMixIn.class) //
        .addMixIn(SamlPrincipal.class, SamlPrincipalMixIn.class) //
        .build();
    return objectMapper;
  }

  @JsonAutoDetect(fieldVisibility = Visibility.ANY)
  abstract static class SamlSessionMixIn
  {
    @JsonDeserialize(using = XMLGregorianCalendarDeserializer.class)
    private XMLGregorianCalendar sessionNotOnOrAfter;
  }

  static class XMLGregorianCalendarDeserializer
      extends JsonDeserializer<XMLGregorianCalendar>
  {
    @Override
    public XMLGregorianCalendar deserialize(
        JsonParser jsonParser,
        DeserializationContext deserializationContext) throws IOException
    {
      JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
      if (!jsonNode.isNumber()) {
        return null;
      }
      long sessionNotOnOrAfter = jsonNode.asLong();
      Instant instant = Instant.ofEpochMilli(sessionNotOnOrAfter);
      ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
      GregorianCalendar gregorianCalendar = GregorianCalendar.from(zonedDateTime);
      try {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
      }
      catch (DatatypeConfigurationException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

  @JsonAutoDetect(fieldVisibility = Visibility.ANY)
  abstract static class SamlPrincipalMixIn
  {
    @JsonIgnore
    abstract AssertionType getAssertion();
  }
}
