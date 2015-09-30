/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import com.sonatype.insight.client.utils.HttpClientUtils;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

public class ScanProcessorLoader
{

  public static void main(String[] args) throws Exception {
    File file;
    file = new File("M:/downloads/insight/hash_security_sha1.txt").getAbsoluteFile();
    file = new File("M:/downloads/insight/hash_coord_sha1.txt").getAbsoluteFile();
    Set<String> hashes = loadHashes(file);
    System.out.println("Loaded " + hashes.size() + " hashes from " + file);

    List<Set<String>> chunks = chunkHashes(hashes);
    System.out.println("Created " + chunks.size() + " chunks");

    CloseableHttpClient client = HttpClientUtils.create(new HttpClientUtils.Configuration()).build();
    try {
      for (int i = 300; i < Math.min(chunks.size(), 400); i++) {
        System.out.println("Uploading chunk " + i);
        Set<String> chunk = chunks.get(i);
        File scanFile = toScan(chunk);

        for (int j = 0; j < 2; j++) {
          HttpPut put = new HttpPut("https://clm-staging.sonatype.com/rest/ci/scan");
          put.addHeader("X-CLM-Token", "9b62e12c76e0efe7d5f626715900dde54d9a004d");
          put.setEntity(new FileEntity(scanFile));
          CloseableHttpResponse response = client.execute(put);
          try {
            System.out.println(response.getStatusLine());
            if (response.getEntity() != null) {
              System.out.println(EntityUtils.toString(response.getEntity(), "UTF-8"));
            }
          }
          finally {
            response.close();
          }
        }
      }
    }
    finally {
      client.close();
    }
  }

  private static Set<String> loadHashes(File file) throws Exception {
    Set<String> hashes = new LinkedHashSet<String>();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    try {
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        hashes.add(line.trim());
      }
    }
    finally {
      reader.close();
    }
    return hashes;
  }

  private static List<Set<String>> chunkHashes(Set<String> hashes) {
    List<Set<String>> chunks = new ArrayList<Set<String>>();
    Set<String> chunk = null;
    for (String hash : hashes) {
      if (chunk == null || chunk.size() >= 2500) {
        chunk = new LinkedHashSet<String>();
        chunks.add(chunk);
      }
      chunk.add(hash);
    }
    return chunks;
  }

  private static File toScan(Set<String> hashes) throws Exception {
    File scanFile = File.createTempFile("scan-", ".xml.gz");
    scanFile.deleteOnExit();
    Writer writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(scanFile)), "UTF-8");
    try {
      writer.write("<scan version=\"2.8\">\n");
      for (String hash : hashes) {
        writer.write("<dir sha1=\"" + hash + "\" path=\"" + hash + ".blob\" />\n");
      }
      writer.write("</scan>\n");
    }
    finally {
      writer.close();
    }
    return scanFile;
  }
}
