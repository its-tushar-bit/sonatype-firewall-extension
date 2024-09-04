/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlunit.util.Predicate;

public class SbomTestHelper
{
  public static final List<String> CYCLONEDX_IGNORE_NODES = Arrays.asList("timestamp", "firstIssued", "lastUpdated");

  public static final List<String> CYCLONEDX_IGNORE_ATTRIBS = Collections.singletonList("serialNumber");

  public static final List<String> SPDX_IGNORE_NODES =
      Arrays.asList("created", "documentNamespace", "creators");

  public static final String CYCLONEDX_IGNORE_METADATA_COMPONENT_PATH = "metadata.component";

  public static final String CYCLONEDX_IGNORE_METADATA_TIMESTAMP_PATH = "metadata.timestamp";

  public static Predicate<Node> spdxDxIgnoreNodesFilter() {
    return node -> {
      if (SPDX_IGNORE_NODES.contains(node.getNodeName())) {
        return false;
      }

      if ("name".equals(node.getNodeName())) {
        Node p1 = node.getParentNode();
        return p1 == null || !"Document".equals(p1.getNodeName());
      }
      return true;
    };
  }

  public static Predicate<Node> cycloneDxIgnoreNodesFilter() {
    return node -> {
      if (CYCLONEDX_IGNORE_NODES.contains(node.getNodeName())) {
        return false;
      }

      //ignore name and version only if it is in the metadata component
      if ("name".equals(node.getNodeName()) || "version".equals(node.getNodeName())) {
        Node p = node.getParentNode();
        if (p != null && "component".equals(p.getNodeName())) {
          p = p.getParentNode();
          if (p != null && "metadata".equals(p.getNodeName())) {
            return false;
          }
        }
      }

      //ignore version only if it is in the tools section
      if ("version".equals(node.getNodeName())) {
        Node p = node.getParentNode();
        if (p != null && "component".equals(p.getNodeName())) {
          p = p.getParentNode();
          if (p != null && "components".equals(p.getNodeName())) {
            p = p.getParentNode();
            if (p != null && "tools".equals(p.getNodeName())) {
              return false;
            }
          }
        }
      }
      return true;
    };
  }

  public static Predicate<Attr> cycloneDxIgnoreAttributesFilter() {
    return attr -> {
      if (CYCLONEDX_IGNORE_ATTRIBS.contains(attr.getName())) {
        return false;
      }
      if ("bom-ref".equals(attr.getName()) && "component".equals(attr.getOwnerElement().getNodeName())) {
        Node p = attr.getOwnerElement().getParentNode();
        if (p != null && "metadata".equals(p.getNodeName())) {
          return false;
        }
      }
      if ("ref".equals(attr.getName()) && "dependency".equals(attr.getOwnerElement().getNodeName())) {
        NodeList elementsWithTagNameComponent = attr.getOwnerDocument().getElementsByTagName("component");
        Node bomComponent = null;
        for (int i = 0; i < elementsWithTagNameComponent.getLength(); i++) {
          if (elementsWithTagNameComponent.item(i).getParentNode().getNodeName().equals("metadata")) {
            bomComponent = elementsWithTagNameComponent.item(i);
          }
        }
        if (bomComponent != null &&
            attr.getValue().equals(bomComponent.getAttributes().getNamedItem("bom-ref").getNodeValue())) {
          return false;
        }
      }
      return true;
    };
  }

  public static Predicate<Node> spdxIgnoreAttributesFilter() {
    return node -> {
      if ("created".equals(node.getNodeName())) {
        Node p = node.getParentNode();
        if (p != null && "creationInfo".equals(p.getNodeName())) {
          return false;
        }
      }
      if ("name".equals(node.getNodeName())) {
        Node p = node.getParentNode();
        if (p != null && "Document".equals(p.getNodeName())) {
          return false;
        }
        if (p != null && "packages".equals(p.getNodeName())) {
          return false;
        }
      }
      if ("documentNamespace".equals(node.getNodeName())) {
        Node p = node.getParentNode();
        if (p != null && "Document".equals(p.getNodeName())) {
          return false;
        }
      }
      if ("creators".equals(node.getNodeName())) {
        Node p = node.getParentNode();
        if (p != null && "creationInfo".equals(p.getNodeName())) {
          return false;
        }
      }

      return true;
    };
  }

  public static Predicate<Node> ignoreSonatypeIdentifierFilter() {
    return node -> {
      if ("property".equals(node.getNodeName()) &&
          node.getAttributes().getNamedItem("name").getNodeValue().equals("sonatypeIdentifier")) {
        return false;
      }
      return true;
    };
  }

  public static String readFileToString(Class testClass, String fileName) throws Exception {
    URL resource = testClass.getResource("/" + testClass.getSimpleName() + "/" + fileName);
    return FileUtils.readFileToString(new File(Objects.requireNonNull(resource).toURI()), StandardCharsets.UTF_8);
  }

  public static Path mockOriginalSbom(Class testClass, String fileName, Path sbomDir)
      throws Exception
  {
    URL resource = testClass.getResource("/" + testClass.getSimpleName() + "/" + fileName);
    Path tmpGzippedFile = Files.createTempFile(sbomDir, null, "-bom.gz");
    try (GZIPOutputStream gzipStream = new GZIPOutputStream(Files.newOutputStream(tmpGzippedFile))) {
      FileUtils.copyFile(new File(Objects.requireNonNull(resource).toURI()), gzipStream);
    }
    return tmpGzippedFile;
  }

  public static ThirdPartySbomMetadata setupScenarioWithMetadataComponentSecurityLicenseAndVex(
      TemporaryEntity tempEntity,
      Application app,
      Path originalSbomFile,
      String sbomVersion,
      String spec,
      String specVersion,
      SbomFormat specFormat)
  {
    String fileName = originalSbomFile.getFileName().toString();
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile(fileName);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), sbomVersion, "ACTIVE", fileName, spec,
            specFormat.toString(), specVersion);

    ThirdPartyFileCoordinate tpComponent =
        tempEntity.newThirdPartyFileCoordinate(tpFile, "tpSource", "maven", "axis", "1.0", "7bdade27d8cd197d9b5c",
            "pkg:maven/apache/axis@1.0?type=jar");
    ThirdPartyCoordinateSecurity vulnSbom =
        tempEntity.newThirdPartyCoordinateSecurity(tpComponent, "CVE-2007-2353", "vuln description",
            "https://nvd.nist.gov/vuln/detail/CVE-2007-2353", 6.0, "", "NVD",
            "CVSS:3.1/AV:L/AC:L/PR:H/UI:N/S:C/C:H/I:N/A:N", "High", "59", "CVSSv3", "", "", "SBOM,Sonatype");
    ThirdPartyCoordinateSecurity vulnSonatype =
        tempEntity.newThirdPartyCoordinateSecurity(tpComponent, "Sonatype-2024-123", "example sonatype vuln",
            "http://link.to/vuln/Sonatype-2024-123", 9.0, "", "Sonatype", "", "Critical", "89", "",
            "example recommendation",
            "advisory", "Sonatype");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnSbom, "CVE-2007-2353", "exploitable",
        "requires_configuration", "workaround_available", "vex detail");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnSonatype, "Sonatype-2024-123", "in_triage", "",
        null,
        "");
    tempEntity.newThirdPartyCoordinateLicense(tpComponent, "Apache-2.0", "Apache-2.0", "", "SBOM,Sonatype");
    tempEntity.newThirdPartyCoordinateLicense(tpComponent, "MIT", "MIT", "", "Sonatype");
    return sbomMetadata;
  }
}
