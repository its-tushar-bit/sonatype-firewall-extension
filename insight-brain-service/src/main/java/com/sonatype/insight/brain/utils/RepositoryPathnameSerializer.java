/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.lqa.LqaComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;

/**
 * This class is a stand-in to allow us to use the same IQ code paths and HDS endpoint as the Firewall plugins.
 * <p>
 * The HDS endpoint FirewallComponentDetailsResource.getComponentData requires (format, sha1, and pathname) to be
 * specified for each repository component request.
 * <p>
 * The IQ endpoint ApiFirewallResource.evaluateComponents uses this to convert pURLs into pathnames to be compatible
 * with the HDS endpoint.
 * <p>
 * Moving forward we should either update the existing HDS endpoint or add a new one to also accept pURLs instead of
 * doing a double conversion.
 * <p>
 * This class is deprecated as it should not be further used.
 **/
@Deprecated
public final class RepositoryPathnameSerializer
{
  private RepositoryPathnameSerializer() {
    throw new UnsupportedOperationException();
  }

  public static String toPathname(String packageUrl) {
    return toPathname(new PackageUrlIdentifier(packageUrl).ensureCompleteIdentifier());
  }

  // In general this method does the opposite of RepositoryPathnameParser in HDS
  // see https://github.com/sonatype/hosted-data-services/blob/731f4e98105b272588c473e6a6ec18ccb30a8cf4/
  // insight-portal-webapp/src/main/java/com/sonatype/insight/portal/rest/service/component/
  // RepositoryPathnameParser.java
  public static String toPathname(ComponentIdentifier componentIdentifier) {
    switch (componentIdentifier.getFormat()) {
      case ComponentIdentifier.FORMAT_MAVEN: {
        String groupId = componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID);
        String artifactId = componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String classifier = componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER);
        String extension = componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION);
        String filename =
            artifactId + "-" + version + (StringUtils.isBlank(classifier) ? "" : "-" + classifier) + "." + extension;
        return String.join("/", groupId.replace(".", "/"), artifactId, version, filename);
      }
      case ComponentIdentifier.FORMAT_NUGET: {
        String packageId = componentIdentifier.get(ComponentIdentifier.NUGET_PACKAGE_ID);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String filename = packageId + "-" + version + ".nupkg";
        return String.join("/", packageId, version, filename);
      }
      case ComponentIdentifier.FORMAT_NPM: {
        String scopeAndPackageId = componentIdentifier.get(ComponentIdentifier.NPM_PACKAGE_ID);
        String[] scopeAndPackageIdSplit = scopeAndPackageId.split("/");
        String packageId = scopeAndPackageIdSplit.length > 1 ? scopeAndPackageIdSplit[1] : scopeAndPackageIdSplit[0];
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String filename = packageId + "-" + version + ".tgz";
        return String.join("/", scopeAndPackageId, "-", filename);
      }
      case ComponentIdentifier.FORMAT_PYPI: {
        // HDS identifies pypi components by hash rather than by parsing the pathname
        // see https://github.com/sonatype/hosted-data-services/blob/731f4e98105b272588c473e6a6ec18ccb30a8cf4/
        // insight-portal-webapp/src/main/java/com/sonatype/insight/portal/rest/service/component/
        // RepositoryPathnameParser.java#L111-L114
        // The pathname still has to identify the component within IQ, because (repository_id, pathname) is unique in
        // the proxy_repository_component table. It also has to end in the real distribution filename, which HDS
        // compares against its stored filenames to break ties when one hash matches several files.
        String name = componentIdentifier.get(ComponentIdentifier.PYPI_NAME);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String qualifier = componentIdentifier.get(ComponentIdentifier.PYPI_QUALIFIER);
        String extension = componentIdentifier.get(ComponentIdentifier.PYPI_EXTENSION);
        String filename = name + "-" + version + (StringUtils.isBlank(qualifier) ? "" : "-" + qualifier) +
            (StringUtils.isBlank(extension) ? "" : "." + extension);
        return String.join("/", "packages", name, version, filename);
      }
      case ComponentIdentifier.FORMAT_RUBYGEMS: {
        String name = componentIdentifier.get(ComponentIdentifier.RUBYGEMS_NAME);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String platform = componentIdentifier.get(ComponentIdentifier.RUBYGEMS_PLATFORM);
        String filename = name + "-" + version + (StringUtils.isBlank(platform) ? "" : "-" + platform) + ".gem";
        return String.join("/", "gems", filename);
      }
      case ComponentIdentifier.FORMAT_GOLANG: {
        String name = componentIdentifier.get(ComponentIdentifier.GOLANG_NAME);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String filename = version + ".zip";
        return String.join("/", name, "@v", filename);
      }
      case ComponentIdentifier.FORMAT_CONAN: {
        String owner = componentIdentifier.get(ComponentIdentifier.CONAN_OWNER);
        String name = componentIdentifier.get(ComponentIdentifier.CONAN_NAME);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String channel = componentIdentifier.get(ComponentIdentifier.CONAN_CHANNEL);
        String filename = "conan_package.tgz";
        return String.join("/", "conans", StringUtils.isBlank(owner) ? "_" : owner, name, version,
            StringUtils.isBlank(channel) ? "_" : channel, filename);
      }
      case ComponentIdentifier.FORMAT_CONDA: {
        String channel = componentIdentifier.get(ComponentIdentifier.CONDA_CHANNEL);
        String name = componentIdentifier.get(ComponentIdentifier.CONDA_NAME);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String build = componentIdentifier.get(ComponentIdentifier.CONDA_BUILD);
        String subdir = componentIdentifier.get(ComponentIdentifier.CONDA_SUBDIR);
        String type = componentIdentifier.get(ComponentIdentifier.CONDA_TYPE);
        String filename = name + "-" + version + "-" + build + "." + type;
        return (StringUtils.isBlank(channel) ? "" : channel + "/") + String.join("/", subdir, filename);
      }
      case ComponentIdentifier.FORMAT_COCOAPODS: {
        String name = componentIdentifier.get(ComponentIdentifier.COCOAPODS_NAME);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        return String.join("/", "pods", name, version);
      }
      case ComponentIdentifier.FORMAT_COMPOSER: {
        String namespace = componentIdentifier.get(ComponentIdentifier.COMPOSER_NAMESPACE);
        String name = componentIdentifier.get(ComponentIdentifier.COMPOSER_NAME);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String filename = namespace + "-" + name + "-" + version + ".zip";
        return String.join("/", namespace, name, version, filename);
      }
      case ComponentIdentifier.FORMAT_CRAN: {
        String name = componentIdentifier.get(ComponentIdentifier.CRAN_NAME);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        // type is unused by HDS
        // see https://github.com/sonatype/hosted-data-services/blob/d38ddeef714f2eb9e2334abf63709119f04ab163/
        // insight-scan-processor/src/main/java/com/sonatype/insight/scan/matcher/firewall/CranPathnameParser.java#L36
        // String type = componentIdentifier.get(ComponentIdentifier.CRAN_TYPE);
        String filename = name + "_" + version + ".tgz";
        return String.join("/", "bin", "os", filename);
      }
      case LqaComponentIdentifier.FORMAT_BOWER: {
        String name = componentIdentifier.get("name");
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String filename = "package.tar.gz";
        return String.join("/", name, version, filename);
      }
      case LqaComponentIdentifier.FORMAT_ALPINE: {
        String name = componentIdentifier.get("name");
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String filename = name + "-" + version + ".apk";
        return String.join("/", "path", filename);
      }
      case LqaComponentIdentifier.FORMAT_DEBIAN: {
        String namespace = componentIdentifier.get("namespace");
        String name = componentIdentifier.get("name");
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String filename =
            name + "_" + version + (StringUtils.isBlank(namespace) ? "" : "-" + namespace) + "_" + "amd64" + ".deb";
        return String.join("/", "path", filename);
      }
      case ComponentIdentifier.FORMAT_CARGO: {
        String name = componentIdentifier.get(ComponentIdentifier.CARGO_NAME);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        return String.join("/", "crates", name, version, "download");
      }
      case ComponentIdentifier.FORMAT_SWIFT: {
        String name = componentIdentifier.get(ComponentIdentifier.SWIFT_NAME);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        return String.join("/", name, version) + ".zip";
      }
      case ComponentIdentifier.FORMAT_HUGGINGFACE_REPO: {
        String repoId = componentIdentifier.get(ComponentIdentifier.HUGGING_FACE_REPO_ID);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        return String.join("/", repoId, "resolve", version);
      }
      case ComponentIdentifier.FORMAT_HUGGINGFACE_MODEL: {
        String repoId = componentIdentifier.get(ComponentIdentifier.HUGGING_FACE_REPO_ID);
        String version = componentIdentifier.get(ComponentIdentifier.VERSION);
        String model = componentIdentifier.get(ComponentIdentifier.HUGGING_FACE_MODEL);
        String extension = componentIdentifier.get(ComponentIdentifier.HUGGING_FACE_EXTENSION);
        return String.join("/", repoId, "resolve", version, model + "." + extension);
      }
      default: {
        throw new BadRequestException(
            String.format("Unsupported format %s.", componentIdentifier.getFormat()));
      }
    }
  }
}
