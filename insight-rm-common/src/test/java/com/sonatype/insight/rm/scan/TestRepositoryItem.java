/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRepositoryItem
    extends RepositoryItem
{
  private final File basedir;

  private final String path;

  private final Coords coords;

  public TestRepositoryItem(File basedir, String path, Coords coords) {
    this.basedir = basedir;
    this.path = path.replace('\\', '/');
    this.coords = coords;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public Coords getCoordinates() {
    return coords;
  }

  @Override
  public InputStream newInputStream() throws IOException {
    return new FileInputStream(new File(basedir, path));
  }

  @Override
  public File getFile() {
    // not returning actual file to check the scanner can handle file-less items
    return super.getFile();
  }

  public static void add(ScanConfiguration config, File basedir) {
    add(config, basedir, "", sort(basedir.listFiles()));
  }

  private static void add(ScanConfiguration config, File basedir, String prefix, File[] children) {
    if (children != null) {
      for (File child : children) {
        if (child.isFile()) {
          String path = prefix + child.getName();
          Coords coords = toCoords(path);
          if (coords != null) {
            config.addItem(new TestRepositoryItem(basedir, path, coords));
          }
        }
        add(config, basedir, prefix + child.getName() + '/', sort(child.listFiles()));
      }
    }
  }

  private static Coords toCoords(String path) {
    Pattern regex = Pattern.compile("(.+)/([^/]+)/([^/]+)/\\2-\\3(-([^./]+))?\\.([^/]+)");
    Matcher m = regex.matcher(path);
    if (m.matches()) {
      return new RepositoryItem.MavenCoords(m.group(1).replace('/', '.'), m.group(2), m.group(3), m.group(5),
          m.group(6));
    }
    return null;
  }

  private static File[] sort(final File[] files) {
    if (files == null) {
      return null;
    }
    Arrays.sort(files);
    return files;
  }
}
