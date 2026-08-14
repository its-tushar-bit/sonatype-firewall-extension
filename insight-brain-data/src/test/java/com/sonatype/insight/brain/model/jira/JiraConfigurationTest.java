/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.jira;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraConfigurationTest
{
  @Test
  public void testSetGetCustomFields_NullJson_NullValue() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();

    assertThat(jiraConfiguration.getCustomFieldsJson()).isNull();
    assertThat(jiraConfiguration.getCustomFields()).isNull();
  }

  @Test
  public void testSetGetCustomFields_Json_NullValue() throws Exception {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    String json = generateCustomFieldsJson();
    jiraConfiguration.setCustomFieldsJson(json);

    assertThat(jiraConfiguration.getCustomFieldsJson()).isEqualTo(json);
    assertThat(jiraConfiguration.getCustomFields()).isEqualTo(generateCustomFields());
  }

  @Test
  public void testSetGetCustomFields_NullJson_Value() throws Exception {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    Map<String, Object> value = generateCustomFields();
    jiraConfiguration.setCustomFields(value);

    assertThat(jiraConfiguration.getCustomFieldsJson()).isEqualTo(generateCustomFieldsJson());
    assertThat(jiraConfiguration.getCustomFields()).isEqualTo(value);
  }

  @Test
  public void testSetGetCustomFields_Json_Value() throws Exception {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    Map<String, Object> value = generateCustomFields();
    jiraConfiguration.setCustomFieldsJson("any");
    jiraConfiguration.setCustomFields(value);

    assertThat(jiraConfiguration.getCustomFieldsJson()).isEqualTo(generateCustomFieldsJson());
    assertThat(jiraConfiguration.getCustomFields()).isEqualTo(value);
  }

  private Map<String, Object> generateCustomFields() {
    Map<String, Object> customFields = new LinkedHashMap<>();
    Map<String, Object> reporter = new LinkedHashMap<>();
    reporter.put("name", "username");
    List<String> labels = Arrays.asList("test", "bug");
    Map<String, Object> customField12001 = new LinkedHashMap<>();
    customField12001.put("name", "example");
    Map<String, Object> customField10050 = new LinkedHashMap<>();
    customField10050.put("value", "pi");
    customFields.put("reporter", reporter);
    customFields.put("labels", labels);
    customFields.put("customfield_12001", customField12001);
    customFields.put("customfield_10050", customField10050);
    customFields.put("customfield_13001", 10);
    customFields.put("customfield_14000", "2016-11-01");
    return customFields;
  }

  private String generateCustomFieldsJson() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode customFieldsObject = objectMapper.createObjectNode();
    ObjectNode reporter = customFieldsObject.putObject("reporter");
    reporter.put("name", "username");
    ArrayNode labels = customFieldsObject.putArray("labels");
    labels.add("test");
    labels.add("bug");
    ObjectNode customField12001 = customFieldsObject.putObject("customfield_12001");
    customField12001.put("name", "example");
    ObjectNode customField10050 = customFieldsObject.putObject("customfield_10050");
    customField10050.put("value", "pi");
    customFieldsObject.put("customfield_13001", 10);
    customFieldsObject.put("customfield_14000", "2016-11-01");
    return objectMapper.writeValueAsString(customFieldsObject);
  }
}
