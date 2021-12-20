/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IacControl
{
  private static final Map<String, IacControl> controlByName = new LinkedHashMap<>();

  private static final Map<String, IacControl> controlById = new LinkedHashMap<>();

  private static final List<IacControl> all = new ArrayList<>();

  // Note: The order the statuses are defined here determines the order they are displayed in the UI
  public static final IacControl CIS_AWS_v1_2_0 =
      new IacControl("CIS-AWS_v1.2.0", "CIS AWS Foundations Benchmark (v1.2.0)");

  public static final IacControl CIS_AWS_v1_4_0 =
      new IacControl("CIS-AWS_v1.4.0", "CIS AWS Foundations Benchmark (v1.4.0)");

  public static final IacControl CIS_Azure_v1_1_0 = new IacControl("CIS-Azure_v1.1.0", "CIS Azure (v1.1.0)");

  public static final IacControl CIS_Azure_v1_3_0 = new IacControl("CIS-Azure_v1.3.0", "CIS Azure (v1.3.0)");

  public static final IacControl CIS_Controls_v7_1 = new IacControl("CIS-Controls_v7.1", "CIS Controls (v7.1)");

  public static final IacControl CIS_Google_v1_1_0 = new IacControl("CIS-Google_v1.1.0", "CIS Google (v1.1.0)");

  public static final IacControl CIS_Google_v1_2_0 = new IacControl("CIS-Google_v1.2.0", "CIS Google (v1.2.0)");

  public static final IacControl CIS_Kubernetes_v1_6_1 =
      new IacControl("CIS-Kubernetes_v1.6.1", "CIS Kubernetes Benchmark (v1.6.1)");

  public static final IacControl CSA_CCM_v3_0_1 = new IacControl("CSA-CCM_v3.0.1", "CSA CCM (v3.0.1)");

  public static final IacControl GDPR_v2016 = new IacControl("GDPR_v2016", "GDPR (v2016)");

  public static final IacControl HIPAA_v2013 = new IacControl("HIPAA_v2013", "HIPAA (v2013)");

  public static final IacControl ISO_27001_v2013 = new IacControl("ISO-27001_v2013", "ISO 27001 (v2013)");

  public static final IacControl NIST_800_53_vRev4 = new IacControl("NIST-800-53_vRev4", "NIST 800-53 (vRev4)");

  public static final IacControl PCI_DSS_v3_2_1 = new IacControl("PCI-DSS_v3.2.1", "PCI DSS (v3.2.1)");

  public static final IacControl SOC_2_v2017 = new IacControl("SOC-2_v2017", "SOC 2 (v2017)");

  private final String id;

  private final String name;

  static {
    all.add(CIS_AWS_v1_2_0);
    all.add(CIS_AWS_v1_4_0);
    all.add(CIS_Azure_v1_1_0);
    all.add(CIS_Azure_v1_3_0);
    all.add(CIS_Controls_v7_1);
    all.add(CIS_Google_v1_1_0);
    all.add(CIS_Google_v1_2_0);
    all.add(CIS_Kubernetes_v1_6_1);
    all.add(CSA_CCM_v3_0_1);
    all.add(GDPR_v2016);
    all.add(HIPAA_v2013);
    all.add(ISO_27001_v2013);
    all.add(NIST_800_53_vRev4);
    all.add(PCI_DSS_v3_2_1);
    all.add(SOC_2_v2017);
  }

  private IacControl(String id, String name) {
    this.id = id;
    this.name = name;
    controlById.put(id, this);
    controlByName.put(name, this);
  }

  public static IacControl getByName(String name) {
    return controlByName.get(name);
  }

  public static IacControl getById(String id) {
    return controlById.get(id);
  }

  public static List<IacControl> getAll() {
    return Collections.unmodifiableList(all);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return getId();
  }
}
