/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import jakarta.inject.Named;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds JMX information (mbean+readable-attributes dump) to support bundle.
 * Ported from Nexus 3:
 * https://github.com/sonatype/nexus-internal/blob/master/components/nexus-core/src/main/java/org/sonatype/nexus/
 * internal/atlas/customizers/JmxCustomizer.groovy
 *
 * @since 1.33
 */
@Named
public class JmxInfo
{
  private static final Logger log = LoggerFactory.getLogger(JmxInfo.class);

  private final MBeanServer server;

  JmxInfo() {
    server = ManagementFactory.getPlatformMBeanServer();
  }

  String getJmxInfoJson() {
    return JsonUtils.format(getJmxInfo());
  }

  SortedMap<String, SortedMap<String, Object>> getJmxInfo() {
    final SortedMap<String, SortedMap<String, Object>> entries = new TreeMap<>();

    log.trace("Querying mbeans");
    final Set<ObjectName> objectNames;
    try {
      objectNames = server.queryNames(new ObjectName("*:*"), null);
    }
    catch (MalformedObjectNameException e) {
      log.warn("Error querying mbeans", e);
      return entries;
    }

    log.trace("Building model");
    for (final ObjectName objectName : objectNames) {
      // normalize names, strip out quotes
      final String name = objectName.getCanonicalName().replace("\"", "").replace("'", "");
      log.trace("Processing MBean: {}", name);

      final MBeanInfo info;
      try {
        info = server.getMBeanInfo(objectName);
      }
      catch (JMException e) {
        log.warn("Error processing mbean {}", name, e);
        continue;
      }
      final SortedMap<String, Object> attrs = new TreeMap<>();
      for (final MBeanAttributeInfo attr : info.getAttributes()) {
        final String attrName = attr.getName();
        log.trace("Processing MBean attribute: {}", attrName);
        if (attr.isReadable() && !"ObjectName".equals(attrName)) {
          try {
            final Object value = server.getAttribute(objectName, attrName);
            attrs.put(attrName, render(value));
          }
          catch (Exception e) {
            log.trace("Unable to fetch attribute: {}; ignoring", attrName, e);
            // do not include attribute detail for failure
          }
        }
      }

      entries.put(name, attrs);
    }
    obfuscatePasswords(entries);
    return entries;
  }

  // Visible for testing
  @SuppressWarnings("unchecked")
  void obfuscatePasswords(Map<String, SortedMap<String, Object>> entries) {
    SortedMap<String, Object> runtime = entries.get("java.lang:type=Runtime");
    Object inputArguments = runtime.get("InputArguments");
    if (inputArguments instanceof List) {
      obfuscateInputArgumentsPasswords((List<Object>) inputArguments);
    }
    Object systemProperties = runtime.get("SystemProperties");
    if (systemProperties instanceof Collection) {
      obfuscateSystemProperties((Collection<Object>) systemProperties);
    }
  }

  private void obfuscateInputArgumentsPasswords(List<Object> inputArguments) {
    for (int i = 0; i < inputArguments.size(); i++) {
      if (!(inputArguments.get(i) instanceof String)) {
        continue;
      }
      String[] inputArgumentKeyValue = inputArguments.get(i).toString().split("=");
      if (!SystemInfo.isSensitiveKey(inputArgumentKeyValue[0])) {
        continue;
      }
      inputArguments.set(i, inputArgumentKeyValue[0] + "=" + SystemInfo.MASK);
    }
  }

  private void obfuscateSystemProperties(Collection<Object> systemProperties) {
    for (Object object : systemProperties) {
      if (!(object instanceof Map)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Map<Object, Object> systemProperty = (Map<Object, Object>) object;
      Object key = systemProperty.get("key");
      if (!(key instanceof String)) {
        continue;
      }
      if (!SystemInfo.isSensitiveKey((String) key)) {
        continue;
      }
      systemProperty.put("value", SystemInfo.MASK);
    }
  }

  private Object render(final Object value) {
    if (value == null) {
      return null;
    }

    final Class<?> type = value.getClass();
    log.trace("Rendering type: {}", type);

    if (value instanceof TabularData) {
      final TabularData valueTD = (TabularData) value;
      final Set<Object> result = new HashSet<>();
      for (final Object row : valueTD.values()) {
        result.add(render(row));
      }
      return result;
    }
    else if (value instanceof CompositeData) {
      final CompositeData valueCD = (CompositeData) value;
      final SortedMap<String, Object> result = new TreeMap<>();
      for (final String key : valueCD.getCompositeType().keySet()) {
        result.put(key, render(valueCD.get(key)));
      }
      return result;
    }
    else if (value instanceof ObjectName) {
      return ((ObjectName) value).getCanonicalName();
    }
    else if (value instanceof Collection) {
      final Collection<?> valueCollection = (Collection<?>) value;
      final ArrayList<Object> result = new ArrayList<>();
      for (final Object item : valueCollection) {
        result.add(render(item));
      }
      return result;
    }
    else if (type.isArray()) {
      final Class<?> componentType = type.getComponentType();
      if (!componentType.isPrimitive()) {
        final Object[] valueArray = (Object[]) value;
        final ArrayList<Object> result = new ArrayList<>();
        for (final Object item : valueArray) {
          result.add(render(item));
        }
        return result;
      }
      else {
        // we have an array of primitives
        return value;
      }
    }
    else if (value instanceof Map) {
      final Map<?, ?> valueMap = (Map<?, ?>) value;
      final SortedMap<Object, Object> result = new TreeMap<>();
      for (final Object k : valueMap.keySet()) {
        result.put(k, render(valueMap.get(k)));
      }
      return result;
    }
    else if (value instanceof Double) {
      final Double valueDouble = (Double) value;
      if (valueDouble.isInfinite() || valueDouble.isNaN()) {
        return valueDouble.toString();
      }
      else {
        return valueDouble;
      }
    }
    else if (value instanceof Float) {
      final Float valueFloat = (Float) value;
      if (valueFloat.isInfinite() || valueFloat.isNaN()) {
        return valueFloat.toString();
      }
      else {
        return valueFloat;
      }
    }
    else if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
      return value;
    }
    else if (value instanceof Enum) {
      final Enum<?> valueEnum = (Enum<?>) value;
      return valueEnum.name();
    }
    else {
      log.trace("Coercing to string: {} -> {}", type, value);
      return String.valueOf(value);
    }
  }
}
