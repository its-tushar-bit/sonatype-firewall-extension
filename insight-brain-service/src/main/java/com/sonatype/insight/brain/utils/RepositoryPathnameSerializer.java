/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.lqa.LqaComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;

public final class RepositoryPathnameSerializer
{
  private RepositoryPathnameSerializer() {
    throw new UnsupportedOperationException();
  }

  public static String toPathname(String packageUrl) {
    return toPathname(new PackageUrlIdentifier(packageUrl).ensureCompleteIdentifier());
  }

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
        // Type is unused by HDS
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
      default: {
        throw new UnsupportedOperationException(
            String.format("Unsupported format %s.", componentIdentifier.getFormat()));
      }
    }
  }
}
