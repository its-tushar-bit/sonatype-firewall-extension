/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default
function applicationReportService() {
  return {
    createReportEntries: createReportData
  };
}

// copied from HDS
function createReportData(policyResult, bomResult, unknownJsResult) {
  const $ = window.jQuery;
  function toKey(item) {
    return item.hash || 'error: ' + (item.pathnames || []).join('\t');
  }

  if (policyResult === null || bomResult === null) {
    return [];
  }

  var componentMap = {},
      componentUsedMap = {}, // Used to find components w/o violations
      componentWaivedMap = {}, // Used to find components with waived violations
      componentGrandfatheredMap = {}, // Used to find grandfathered components
      entries = [];

  $.each(bomResult.aaData, function (index, component) {
    var componentKey = toKey(component);
    componentMap[componentKey] = component;
    componentUsedMap[componentKey] = false;
    componentWaivedMap[componentKey] = false;
    componentGrandfatheredMap[componentKey] = false;
  });

  if (unknownJsResult) {
    $.each(unknownJsResult.aaData, function (index, component) {
      var componentKey = toKey(component);
      componentMap[componentKey] = component;
      componentUsedMap[componentKey] = false;
      componentWaivedMap[componentKey] = false;
      componentGrandfatheredMap[componentKey] = false;
    });
  }

  if (policyResult.version === 3) {
    $('#policy-violation-filter').show();
    $('#policy-violations-grandfathered').show();
    $.each(policyResult.aaData, function (componentIndex, component) {
      var key = toKey(component);
      if (!component.hash || component.hash === 'null') {
        return true; // CLM-1863
      }

      component.allViolations.sort(function (a, b) {
        var grandfathered = (b.grandfathered | 0) - (a.grandfathered | 0),
            waived = (b.waived | 0) - (a.waived | 0);
        if (grandfathered !== 0) {
          return grandfathered;
        }
        else if (waived !== 0) {
          return waived;
        }
        else {
          return b.policyThreatLevel - a.policyThreatLevel;
        }
      });

      var summary = true;
      $.each(component.allViolations, function(violationIndex, violation) {
        var active = !violation.waived && !violation.grandfathered;
        componentUsedMap[key] = active;
        componentWaivedMap[key] = violation.waived;
        componentGrandfatheredMap[key] = violation.grandfathered;
        entries.push($.extend({
          policyThreatLevel: violation.policyThreatLevel,
          policyName: violation.policyName,
          groupId: component.groupId,
          artifactId: component.artifactId,
          version: component.version,
          hash: component.hash,
          componentIdentifier: component.componentIdentifier,
          summary: active && summary,
          waived: violation.waived,
          grandfathered: violation.grandfathered,
          all: true
        }, componentMap[key]));
        if (active) {
          summary = false;
        }
      });
    });
  }
  else if (policyResult.version) {
    $('#policy-violation-filter').show();
    $.each(policyResult.aaData, function (componentIndex, component) {
      var key = toKey(component);
      if (!component.hash || component.hash === 'null') {
        return true; // CLM-1863
      }

      if (component.activeViolations.length) {
        componentUsedMap[key] = true;
        component.activeViolations.sort(function(a, b) {
          return b.policyThreatLevel - a.policyThreatLevel;
        });

        $.each(component.activeViolations, function(violationIndex, violation) {
          entries.push($.extend({
            policyThreatLevel: violation.policyThreatLevel,
            policyName: violation.policyName,
            groupId: component.groupId,
            artifactId: component.artifactId,
            version: component.version,
            hash: component.hash,
            componentIdentifier: component.componentIdentifier,
            summary: violationIndex === 0,
            waived: false,
            all: true
          }, componentMap[key]));
        });
      }

      if (component.waivedViolations.length) {
        componentWaivedMap[key] = true;

        $.each(component.waivedViolations, function (waivedViolationIndex, waivedViolation) {
          entries.push($.extend({
            policyThreatLevel: waivedViolation.policyThreatLevel,
            policyName: waivedViolation.policyName,
            groupId: component.groupId,
            artifactId: component.artifactId,
            version: component.version,
            hash: component.hash,
            componentIdentifier: component.componentIdentifier,
            summary: false,
            waived: true,
            all: true
          }, componentMap[key]));
        });
      }
    });
  }
  else {
    // Support for policythreats.json generated by CLM Server 1.8 and earlier
    $.each(policyResult.aaData, function (index, violation) {
      var key = toKey(violation);
      if (!violation.hash || violation.hash === 'null') {
        return true; // CLM-1863
      }

      componentUsedMap[key] = true;

      entries.push($.extend({
        policyThreatLevel: violation.policyThreatLevel,
        policyName: violation.policyName,
        groupId: violation.groupId,
        artifactId: violation.artifactId,
        version: violation.version,
        hash: violation.hash,
        summary: true,
        waived: false,
        all: true
      }, componentMap[key]));
    });
  }

  // Add components w/o violations
  $.each(componentUsedMap, function(componentKey, used) {
    if (!used) {
      entries.push($.extend(true, {
        policyThreatLevel: 0,
        policyName: 'None',
        summary: true,
        waived: false,
        grandfathered: false,
        all: componentWaivedMap[componentKey] === false && componentGrandfatheredMap[componentKey] === false
        // if the component has a waived violation, we don't want to show none on the all view as it would already
        // show the waived item
      }, componentMap[componentKey]));
    }
  });

  return { aaData: entries };
}
