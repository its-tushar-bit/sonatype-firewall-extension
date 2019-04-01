/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class JsonFileStore
    implements JsonStore
{
  private static final ConcurrentMap<String, ReadWriteLock> LOCK_TABLE = new ConcurrentHashMap<>();

  private final File folder;

  public JsonFileStore(final File folder) {
    this.folder = folder;
  }

  @Override
  public void commit(final String path, final ContainerNode<?> data) throws IOException {
    final ReadWriteLock lock = lockFor(folder);

    lock.writeLock().lock();
    try {
      final File file = new File(folder, path);

      final ArrayNode log;
      if (file.exists()) {
        log = JsonUtils.read(file);
      }
      else {
        log = JsonUtils.arrayNode(data);
      }

      // newest entries appear at the top of the log
      JsonUtils.write(file, log.insert(0, data));
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public ContainerNode<?> restore(String path) throws IOException {
    final ReadWriteLock lock = lockFor(folder);

    lock.readLock().lock();
    try {
      final File file = new File(folder, path);

      if (file.exists()) {
        JsonNode data = JsonUtils.read(file).get(0);
        if (data != null && data.has("data")) // stamped data?
        {
          data = data.get("data");
        }
        return (ContainerNode<?>) data;
      }

      return null;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  private Iterable<String> list() {
    List<String> filenames = new ArrayList<>();
    File[] files = folder.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isFile()) {
          filenames.add(file.getName());
        }
      }
      Collections.sort(filenames);
    }
    return filenames;
  }

  @Override
  public ContainerNode<?> history(final ContainerNode<?> key, final String... paths) throws IOException {
    final ReadWriteLock lock = lockFor(folder);

    lock.readLock().lock();
    try {
      Iterable<String> filenames = Arrays.asList(paths);
      if (paths.length == 0 || paths[0].length() == 0) {
        filenames = list();
      }

      final ObjectNode log = JsonUtils.objectNode(key);
      final ArrayNode entries = log.putArray("aaData");

      for (final String name : filenames) {
        final File file = new File(folder, name);
        if (file.canRead()) {
          entries.addAll(filterLog(file, (ObjectNode) key));
        }
      }

      return entries.size() > 0 ? log : null;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  private static ArrayNode filterLog(final File file, final ObjectNode key) throws IOException {
    final ArrayNode log = JsonUtils.read(file);
    final ArrayNode filteredLog = JsonUtils.arrayNode(log);
    for (int x = 0; x < log.size(); x++) {
      final ObjectNode entry;
      ContainerNode<?> data = (ContainerNode<?>) log.get(x);
      if (data != null && data.has("data")) // stamped data?
      {
        entry = (ObjectNode) data;
        data = (ContainerNode<?>) entry.remove("data");
        entry.put("filename", file.getName());
      }
      else {
        entry = JsonUtils.objectNode(data);
      }
      if (data instanceof ArrayNode) {
        for (int y = 0; y < data.size(); y++) {
          try {
            filteredLog.add(augment(key, (ObjectNode) data.get(y)).setAll(entry));
          }
          catch (final JsonMappingException e) {
            // incompatible data, try next entry from audit log
          }
        }
      }
      else {
        try {
          filteredLog.add(augment(key, (ObjectNode) data).setAll(entry));
        }
        catch (final JsonMappingException e) {
          // incompatible data, try next entry from audit log
        }
      }
    }

    return filteredLog;
  }

  private static ObjectNode augment(final ObjectNode primary, final ObjectNode secondary) throws JsonMappingException {
    if (primary == null) {
      return secondary;
    }
    final ObjectNode[] result = { primary };
    for (final Entry<String, JsonNode> field : each(secondary.fields())) {
      final String name = field.getKey();
      final JsonNode primaryValue = primary.get(name);
      final JsonNode secondaryValue = field.getValue();
      if (primaryValue == null) {
        mutate(result, primary).set(name, secondaryValue); // pure augmented data
      }
      else if (primaryValue.isObject() && secondaryValue != null && secondaryValue.isObject()) {
        final ObjectNode value = augment((ObjectNode) primaryValue, (ObjectNode) secondaryValue);
        if (primaryValue != value) {
          mutate(result, primary).set(name, value); // patch in augmented result
        }
      }
      else if (!primaryValue.equals(secondaryValue)) {
        throw new JsonMappingException(null, "Inconsistent data");
      }
    }
    return result[0];
  }

  private static ObjectNode mutate(final ObjectNode[] result, final ObjectNode original) {
    if (result[0] == original) {
      // perform shallow copy so we can patch in any augmented fields
      result[0] = (ObjectNode) original.objectNode().setAll(original);
    }
    return result[0];
  }

  private static <T> Iterable<T> each(final Iterator<T> itr) {
    return new Iterable<T>()
    {
      @Override
      public Iterator<T> iterator() {
        return itr;
      }
    };
  }

  private static ReadWriteLock lockFor(final File folder) {
    ReadWriteLock lock = LOCK_TABLE.get(folder.getAbsolutePath());
    if (lock == null) {
      final ReadWriteLock newLock = new ReentrantReadWriteLock();
      lock = LOCK_TABLE.putIfAbsent(folder.getAbsolutePath(), newLock);
      if (lock == null) {
        lock = newLock;
      }
    }
    return lock;
  }
}
