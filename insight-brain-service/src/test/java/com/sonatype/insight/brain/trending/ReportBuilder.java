/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.codehaus.plexus.util.IOUtil;

public class ReportBuilder
{
  private final TemporaryEntity tempEntity;

  private ObjectMapper mapper = new ObjectMapper();

  private List<JsonNode> bomNodes = new ArrayList<JsonNode>();

  private List<PolicyViolation> policyViolations = new ArrayList<PolicyViolation>();

  public ReportBuilder(TemporaryEntity tempEntity) {
    this.tempEntity = tempEntity;
  }

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

    public ComponentBuilder setProprietary(boolean proprietary) {
      component.put("proprietary", proprietary);
      return this;
    }
  }

  public ComponentBuilder addComponent() {
    ObjectNode component = mapper.createObjectNode();
    bomNodes.add(component);
    return new ComponentBuilder(component);
  }

  public void addPolicyViolation(String policyId, int threatLevel, String hash) {
    addPolicyViolation(policyId, threatLevel, PolicyThreatCategory.OTHER, hash);
  }

  public void addPolicyViolation(String policyId, int threatLevel, PolicyThreatCategory threatCategory, String hash) {
    addPolicyViolation(policyId, threatLevel, threatCategory, hash, null, null, null);
  }

  public void addPolicyViolation(String policyId, int threatLevel, PolicyThreatCategory threatCategory, String hash,
      String g, String a, String v)
  {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setPolicyId(policyId);
    policyViolation.setThreatLevel(threatLevel);
    policyViolation.setThreatCategory(threatCategory);
    policyViolation.setHash(hash);
    policyViolation.setGroupId(g);
    policyViolation.setArtifactId(a);
    policyViolation.setVersion(v);
    policyViolations.add(policyViolation);
  }

  private void createPolicy(String appId, String policyId) {
    if (new PolicyDAO().getById(policyId) == null) {
      tempEntity.newPolicy(appId, policyId, policyId);
    }
  }

  public void build(Application app, String scanId, Date time, File reportDir) throws IOException {
    // Create policies, policy evaluation and policy violations
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId, time);
    for (PolicyViolation policyViolation : policyViolations) {
      createPolicy(app.getId(), policyViolation.getPolicyId());
      policyViolation.setId(null);
      policyViolation.setPolicyEvaluationId(policyEvaluation.getId());
      policyViolation.setPolicyName(policyViolation.getPolicyId());
      policyViolation.setConstraintFactsJson("constraint facts");
      policyViolationDAO.insert(policyViolation);
    }

    File cacheDir = new File(reportDir, "report.cache");

    if (!cacheDir.exists() && !cacheDir.mkdirs()) {
      throw new IOException("Could not create " + cacheDir);
    }

    ObjectNode bom = mapper.createObjectNode();
    bom.putArray("aaData").addAll(bomNodes);
    write(bom, new File(cacheDir, "bom.json"));

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
    write(policythreats, new File(cacheDir, PolicyEvaluationUtils.POLICY_THREATS_FILENAME));

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
