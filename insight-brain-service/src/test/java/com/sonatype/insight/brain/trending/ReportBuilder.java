/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.codehaus.plexus.util.IOUtil;

public class ReportBuilder
{
  private ObjectMapper mapper = new ObjectMapper();

  private List<JsonNode> bomNodes = new ArrayList<JsonNode>();

  private List<JsonNode> policyAlertsNodes = new ArrayList<JsonNode>();

  public class ComponentBuilder
  {
    private final ObjectNode component;

    public ComponentBuilder(ObjectNode component) {
      this.component = component;
    }

    public ComponentBuilder setGAV(String g, String a, String v) {
      component.put("groupId", g);
      component.put("artifactId", a);
      component.put("version", v);
      return this;
    }

    public ComponentBuilder setMatchState(MatchState matchState) {
      component.put("matchState", matchState.getId());
      return this;
    }

    public ComponentBuilder setHash(String hash) {
      component.put("hash", hash);
      return this;
    }
  }

  public class ConstraintFactBuilder
  {
    private final ArrayNode conditionFacts;

    public ConstraintFactBuilder(ObjectNode constraintFact) {
      conditionFacts = constraintFact.putArray("conditionFacts");
    }

    public void addConditionFact(String conditionTypeId) {
      ObjectNode conditionFact = conditionFacts.addObject();
      conditionFact.put("conditionTypeId", conditionTypeId);
    }
  }

  public class ComponentFactBuilder
  {
    private final ObjectNode component;
    private final ArrayNode constraintFacts;

    public ComponentFactBuilder(ObjectNode component) {
      this.component = component;
      this.constraintFacts = component.putArray("constraintFacts");
    }

    public ConstraintFactBuilder addConstraintFact() {
      return new ConstraintFactBuilder(constraintFacts.addObject());
    }

    public ComponentFactBuilder setGAV(String g, String a, String v) {
      component.put("groupId", g);
      component.put("artifactId", a);
      component.put("version", v);
      return this;
    }

  }

  public class PolicyAlertBuilder
  {
    private final ArrayNode componentFacts;

    public PolicyAlertBuilder(ObjectNode trigger) {
      componentFacts = trigger.putArray("componentFacts");
    }

    public ComponentFactBuilder addComponentFact(String hash) {
      ObjectNode componentFact = componentFacts.addObject();
      componentFact.put("hash", hash);
      return new ComponentFactBuilder(componentFact);
    }
  }

  public ComponentBuilder addComponent() {
    ObjectNode component = mapper.createObjectNode();
    bomNodes.add(component);
    return new ComponentBuilder(component);
  }

  public PolicyAlertBuilder addPolicyAlert(String policyId, int threatLevel) {
    ObjectNode policyAlertNode = mapper.createObjectNode();
    policyAlertsNodes.add(policyAlertNode);
    ObjectNode triggerNode = policyAlertNode.putObject("trigger");
    triggerNode.put("policyId", policyId);
    triggerNode.put("policyName", policyId);
    triggerNode.put("threatLevel", threatLevel);
    return new PolicyAlertBuilder(triggerNode);
  }

  public void build(File reportDir) throws IOException {
    File cacheDir = new File(reportDir, "report.cache");

    if (!cacheDir.exists() && !cacheDir.mkdirs()) {
      throw new IOException("Could not create " + cacheDir);
    }

    ObjectNode bom = mapper.createObjectNode();
    bom.putArray("aaData").addAll(bomNodes);
    write(bom, new File(cacheDir, "bom.json"));

    ObjectNode policyAlerts = mapper.createObjectNode();
    policyAlerts.putArray("aaData").addAll(policyAlertsNodes);
    write(policyAlerts, new File(cacheDir, "policyalerts.json"));

    ObjectNode licenses = mapper.createObjectNode();
    licenses.putArray("aaData");
    write(licenses, new File(cacheDir, "licenses.json"));

    ObjectNode security = mapper.createObjectNode();
    security.putArray("aaData");
    write(security, new File(cacheDir, "security.json"));

    ObjectNode partialmatched = mapper.createObjectNode();
    partialmatched.putArray("aaData");
    write(partialmatched, new File(cacheDir, "partialmatched.json"));

    ObjectNode policythreats = mapper.createObjectNode();
    policythreats.putArray("aaData");
    write(policythreats, new File(cacheDir, "policythreats.json"));

    ObjectNode dependencies = mapper.createObjectNode();
    write(dependencies, new File(cacheDir, "dependencies.json"));

    ObjectNode data = mapper.createObjectNode();
    write(data, new File(cacheDir, "data.json"));

    ZipOutputStream reportZip = new ZipOutputStream(new FileOutputStream(new File(reportDir, "report.zip")));
    try {
      // index.html must be present
      reportZip.putNextEntry(new ZipEntry("index.html"));
      reportZip.closeEntry();
      // copy "cached" files
      for (File file : cacheDir.listFiles()) {
        reportZip.putNextEntry(new ZipEntry(file.getName()));
        InputStream is = new FileInputStream(file);
        try {
          IOUtil.copy(is, reportZip);
        }
        finally {
          IOUtil.close(is);
        }
        reportZip.closeEntry();
      }
    }
    finally {
      IOUtil.close(reportZip);
    }
  }

  private void write(ObjectNode node, File file) throws FileNotFoundException, IOException {
    final OutputStream os = new FileOutputStream(file);
    try {
      IOUtil.copy(JsonUtils.generate(node), os);
    }
    finally {
      IOUtil.close(os);
    }
  }
}
