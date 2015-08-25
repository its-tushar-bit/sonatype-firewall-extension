/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global Insight, $, window, Slick, HealthCheck, currentDate, applicationId, reportId, earliestReleaseDate, dateFormat, Brain */
/*jslint sub:true, plusplus:true */
(function() {
  'use strict';
  var licenseThreats,
      coordinatesColumn = {
        id: 'coordinates',
        name: 'Component',
        field: 'coordinates',
        sortable: true,
        width: 295,
        toolTipGravity: 'se',
        formatter: function(row, cell, value, columnDef, dataContext) {
          var icon = '<img src="public/coord-unknown.png" alt="Unknown"/> ';
          if (dataContext.componentIdentifier) {
            switch(dataContext.componentIdentifier.format) {
              case 'nuget':
                icon = '<img src="public/coord-nuget.png" alt="NuGet"/> ';
                break;
              case 'maven':
                icon = '<img src="public/coord-maven.png" alt="Maven"/> ';
                break;
            }
          }
          return icon + Insight.util.encodeHtml($.map(dataContext.displayName.parts, function(p) {
            return p.value;
          }).join(''));
        }
      },
      processCoordinatesData = function(data) {
        var mavenFormat = 'maven',
            nuGetFormat = 'nuget',
            mavenSeparator = ' : ',
            nuGetSeparator = ' ';

        data = data || [];

        $.each(data, function(key, dataItem) {
          // Support for CLM server <= 1.12.0
          // Claimed components unknown to HDS
          if (Insight.util.isNullOrUndefined(dataItem.componentIdentifier) &&
              Insight.util.isNotNullOrUndefined(dataItem.groupId)) {
            dataItem.componentIdentifier = {
              format: mavenFormat,
              coordinates: {
                groupId: dataItem.groupId,
                artifactId: dataItem.artifactId,
                version: dataItem.version
              }
            };
          }

          // Generating display name
          if (Insight.util.isNullOrUndefined(dataItem.displayName)) {
            dataItem.displayName = {};
            if (Insight.util.isNotNullOrUndefined(dataItem.componentIdentifier)) {
              var coordinates = dataItem.componentIdentifier.coordinates;
              switch (dataItem.componentIdentifier.format) {
                case mavenFormat:
                  dataItem.displayName.parts = [
                    { field: 'Group', value: coordinates['groupId'] },
                    { value: mavenSeparator },
                    { field: 'Artifact', value: coordinates['artifactId'] },
                    { value: mavenSeparator },
                    { field: 'Version', value: coordinates['version'] }
                  ];
                  break;
                case nuGetFormat:
                  dataItem.displayName.parts = [
                    { field: 'ID', value: coordinates['packageId'] },
                    { value: nuGetSeparator },
                    { field: 'Version', value: coordinates['version'] }
                  ];
                  break;
                default:
                  dataItem.displayName.parts = [];
                  for (var property in coordinates) {
                    if (coordinates.hasOwnProperty(property)) {
                      dataItem.displayName.parts.push( { field: property, value: coordinates[property] });
                      dataItem.displayName.parts.push( { value: mavenSeparator });
                    }
                  }
                  dataItem.displayName.parts.pop();
                  break;
              }
            }
            else {
              if (dataItem.filenames && dataItem.filenames.length > 0) {
                dataItem.displayName.parts = [];
                for (var i = 0; i < dataItem.filenames.length; i++) {
                  dataItem.displayName.parts.push({ field: 'Filename', value: dataItem.filenames[i] });
                  if (i < dataItem.filenames.length - 1) {
                    dataItem.displayName.parts.push({ value: ', ' });
                  }
                }
              } else {
                dataItem.displayName.parts = [
                  { value: '(Anonymized Path) SHA1: ' },
                  { field: 'Hash', value: dataItem.hash }
                ];
              }
            }
          }

          // Set coordinates value for filtering and sorting
          dataItem.coordinates = $.map(dataItem.displayName.parts, function(p) {
            return p.value;
          }).join('');
        });
      };

  function loadLicenseThreats(callback) {
    if (Insight.util.isNotNullOrUndefined(licenseThreats)) {
      callback.apply(null, [licenseThreats]);
      return;
    }
    $.getJSON('licensethreats.json').success(function(jsonData) {
      licenseThreats = jsonData.aaData;
      callback.apply(null, [licenseThreats]);
    }).error(function(resp) {
      callback.apply(null, [null, 'Loading license categorization: ' + Insight.util.getErrorMessage(resp)]);
    });
  }

  loadLicenseThreats($.noop);

  function getLicenseThreatLevelFromArray() {
    var threat = null;
    for (var i = 0; i < arguments.length; i++) {
      var names = arguments[i];
      for (var j = 0; j < names.length; j++) {
        var name = names[j];
        var nameThreat = licenseThreats[name];
        if (Insight.util.isNotNullOrUndefined(nameThreat)) {
          threat = Math.max(nameThreat, threat);
        }
      }
    }
    return threat;
  }

  function getLicenseThreatLevelFromComponent(component) {
    var deprecatedThreat = useDeprecatedLicense(component.effectiveLicenseThreat);
    if (typeof(deprecatedThreat) !== 'undefined') {
      return deprecatedThreat;
    }
    if (Insight.util.isNotNullOrUndefined(component.overriddenLicenses)) {
      return getLicenseThreatLevelFromArray(component.overriddenLicenses);
    }
    return component.effectiveLicenseThreat;
  }

  // Deprecated as of Insight-Brain 1.2. Leave in for compatibility with Insight-Brain 1.1
  var deprecatedLicenseThreats = ['UNKCAT', 'LIBERAL', 'WEAKCOPYLEFT', 'NOT-PROVIDED', 'NON-STANDARD', 'COPYLEFT'];

  function useDeprecatedLicense(effectiveLicenseThreat) {
    if (deprecatedLicenseThreats.indexOf(effectiveLicenseThreat) >= 0) {
      return deprecatedLicenseThreatToEffectiveLicenseThreat(effectiveLicenseThreat);
    }
  }

  function deprecatedLicenseThreatToEffectiveLicenseThreat(effectiveLicenseThreat) {
    switch (effectiveLicenseThreat) {
      case 'COPYLEFT':
        return 10;
      case 'NON-STANDARD':
        return 7;
      case 'NOT-PROVIDED':
        return 6;
      case 'WEAKCOPYLEFT':
        return 3;
      case 'LIBERAL':
        return 0;
      default:
        return null;
    }
  }

  function numberCompare(number1, number2) {
    return number1 - number2;
  }

  function dedupLicenses(licenses1, licenses2) {
    var deduped = [],
        i = null;

    for (i = 0; i < licenses2.length; i++) {
      //Not Provided can be in both lists, but shown with different data
      if ('Not Provided' === licenses2[i] || licenses1.indexOf(licenses2[i]) === -1) {
        deduped.push(licenses2[i]);
      }
    }

    return deduped;
  }

  function renderLicenses(licenses) {
    return Insight.util.isNotNullOrUndefined(licenses) && licenses.length > 0 ? licenses.join(', ') : '';
  }

  function effectiveLicenseSortFn(dataRow1, dataRow2) {
    var x = getLicenseThreatLevelFromComponent(dataRow1),
        y = getLicenseThreatLevelFromComponent(dataRow2);
    x = Insight.util.isNullOrUndefined(x) ? -1 : x;
    y = Insight.util.isNullOrUndefined(y) ? -1 : y;

    return ((x < y) ? -1 : ((x > y) ? 1 : 0));
  }

  function getLicenseThreatClass(value) {
    var deprecatedThreat = useDeprecatedLicense(value);
    if (typeof(deprecatedThreat) !== 'undefined') {
      value = deprecatedThreat;
    }
    if (Insight.util.isNullOrUndefined(value)) {
      return 'unspecifiedScore';
    }
    else if (value > 7) {
      return 'criticalScore';
    }
    else if (value > 3) {
      return 'severeScore';
    }
    else if (value > 0) {
      return 'moderateScore';
    }
    else {
      return 'noScore';
    }
  }

  function effectiveLicenseFormatFn(overriddenLicenses, declaredLicenses, observedLicenses) {
    if (Insight.util.isNotNullOrUndefined(overriddenLicenses)) {
      return renderLicenses(overriddenLicenses);
    }
    else {
      var declared = renderLicenses(declaredLicenses),
          observed = renderLicenses(dedupLicenses(declaredLicenses, observedLicenses));
      return declared + (observed.length > 0 ? (', ' + observed) : '');
    }
  }

  function effectiveLicenseHtmlFormatFn(row, cell, value, columnDef, dataContext) {
    if (Insight.util.isNotNullOrUndefined(dataContext.overriddenLicenses)) {
      return '<div class="' + getLicenseThreatClass(getLicenseThreatLevelFromArray(dataContext.overriddenLicenses)) + '"><i>' +
          renderLicenses(dataContext.overriddenLicenses) + '</i></div>';
    }
    else {
      var declared = renderLicenses(dataContext.declaredLicenses),
          observed = renderLicenses(dedupLicenses(dataContext.declaredLicenses, dataContext.observedLicenses));
      return '<div class="' + getLicenseThreatClass(getLicenseThreatLevelFromComponent(dataContext)) + '"> <b>' + declared + '</b>' +
          (observed.length > 0 ? (', ' + observed) : '') + '</div>';
    }
  }

  function closeMessagebox(id) {
    $('#' + id + 'container .message').remove();
  }

  var componentTable = null,
      similarityTable = null,
      auditTable = null;

  function getActiveTable() {
    return componentTable;
  }

  function destroyActiveTable() {
    var table = getActiveTable();
    if (table) {
      table.destroy();
    }
    componentTable = null;
  }

  function destroyTable(id, table) {
    closeMessagebox(id);
    $(window).unbind('resize');
    if (table.pager) {
      table.pager.destroy();
      table.pager = null;
    }
    if (table.columnsResizedFn) {
      table.onColumnsResized.unsubscribe(table.columnsResizedFn);
      table.columnsResizedFn = null;
    }
    table.destroy();
  }

  function removeComponentTable() {
    if (Insight.util.isNotNullOrUndefined(componentTable)) {
      destroyTable('component', componentTable);
      $('#componentcontainer ul.nav a').unbind('click');
      componentTable = null;
    }
  }

  function removeSimilarityTable() {
    if (Insight.util.isNotNullOrUndefined(similarityTable)) {
      destroyTable('similarity', similarityTable);
      similarityTable = null;
    }
  }

  function removeAuditTable() {
    if (Insight.util.isNotNullOrUndefined(auditTable)) {
      destroyTable('audit', auditTable);
      auditTable = null;
    }
  }

  function createComponentTable() {
    var matchStates = ['unknown', 'similar', 'exact'],
        columnGrouping = new Slick.ColumnGrouping({ columnId: 'policyName', style: 'scoreCol' }),
        scoreStyler = columnGrouping.getCellStyler(),
        cellFormatter = columnGrouping.getCellRenderer(),
        time = HealthCheck.getAge(earliestReleaseDate, currentDate),
        data = {
          get: function() {
            var deferred = $.Deferred();

            $.when($.getJSON('policythreats.json'), $.getJSON('bom.json')).done(function(policyResult, bomResult) {
              function toKey(item) {
                return item.hash || 'error: ' + (item.pathnames || []).join('\t');
              }

              if (policyResult[0] === null || bomResult[0] === null) {
                deferred.reject([]);
                return;
              }

              var componentMap = {},
                  componentUsedMap = {}, // Used to find components w/o violations
                  componentWaivedMap = {}, // Used to find components with waived violations
                  entries = [];

              $.each(bomResult[0].aaData, function (index, component) {
                var componentKey = toKey(component);
                componentMap[componentKey] = component;
                componentUsedMap[componentKey] = false;
                componentWaivedMap[componentKey] = false;
              });

              if (policyResult[0].version) {
                $('#policy-violation-filter').show();
                $.each(policyResult[0].aaData, function (componentIndex, component) {
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
                          policyThreatLevel : waivedViolation.policyThreatLevel,
                          policyName : waivedViolation.policyName,
                          groupId : component.groupId,
                          artifactId : component.artifactId,
                          version : component.version,
                          hash : component.hash,
                          componentIdentifier: component.componentIdentifier,
                          summary : false,
                          waived : true,
                          all : true
                      }, componentMap[key]));
                    });
                  }
                });
              } else {
                // Support for policythreats.json generated by CLM Server 1.8 and earlier
                $.each(policyResult[0].aaData, function (index, violation) {
                  var key = toKey(violation);
                  if (!violation.hash || violation.hash === 'null') {
                    return true; // CLM-1863
                  }

                  componentUsedMap[key] = true;

                  entries.push($.extend({
                      policyThreatLevel : violation.policyThreatLevel,
                      policyName : violation.policyName,
                      groupId : violation.groupId,
                      artifactId : violation.artifactId,
                      version : violation.version,
                      hash : violation.hash,
                      summary : true,
                      waived : false,
                      all : true
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
                    all: componentWaivedMap[componentKey] === false
                  // if the component has a waived violation, we don't want to show none on the all view as it would already
                  // show the waived item
                  }, componentMap[componentKey]));
                }
              });

              deferred.resolve({ aaData : entries });
            }).fail(function() {
              deferred.reject(arguments[0]);
            });
            return deferred;
          }
        },
        columns = [
          {
            id: 'policyName',
            name: 'Policy Threat',
            shortName: 'Name',
            field: 'policyName',
            sortable: true,
            width: 225,
            styleFn: function(row, cell, value, columnDef, dataContext) {
              return 'nopad ' + scoreStyler(row, cell, value, columnDef, dataContext);
            },
            sortFn: function(dataRow1, dataRow2) {
              var levelA = dataRow1.policyThreatLevel,
                  levelB = dataRow2.policyThreatLevel,
                  nameA = dataRow1.policyName,
                  nameB = dataRow2.policyName;

              if (levelA < levelB) {
                return -1;
              }
              else if (levelA > levelB) {
                return 1;
              }
              else if (nameA < nameB) {
                return 1;
              }
              else if (nameA > nameB) {
                return -1;
              }

              return 0;
            },
            formatter: function(row, cell, value, columnDef, dataContext) {
              var colorCls;

              switch (dataContext.policyThreatLevel) {
                case 0:
                  colorCls = ' noScore';
                  break;
                case 1:
                  colorCls = ' ignoredScore';
                  break;
                case 2:
                case 3:
                  colorCls = ' moderateScore';
                  break;
                case 4:
                case 5:
                case 6:
                case 7:
                  colorCls = ' severeScore';
                  break;
                case 8:
                case 9:
                case 10:
                  colorCls = ' criticalScore';
                  break;
              }

              return '<div class="' + colorCls + '">' +
                  (cellFormatter(row, cell, value, columnDef, dataContext).length > 0 ? Insight.util.encodeHtml(value) : '') + '</div>';
            }
          },
          $.extend({}, coordinatesColumn, {
            styleFn : function(row, cell, value, columnDef, dataContext) {
              return (dataContext.modified || dataContext.identificationSource === 'Manual') ? 'modified' : '';
            },
            toolTipFn: function(row) {
              var tip = '';
              if (row.modified) {
                tip += '<li>been manually edited.  See the audit log for details.</li>';
              }
              if (row.identificationSource === 'Manual') {
                tip += '<li>been claimed from an unknown or similar component.  View the Component Information Panel (CIP) for more details.</li>';
              }
              if (row.waived) {
                tip += '<li>been waived.</li>';
              }

              if (tip.length > 0) {
                return 'This record has...<br><br><ul>' + tip + '</ul>';
              }
            },
            formatter: function(row, cell, value, columnDef, dataContext) {
              var result = '';
              if (dataContext.waived) {
                result = '<img src="flag_white.png" style="margin-right:4px" title="Violation has been waived">';
              }
              else {
                result = '<span style="padding-right:20px"></span>';
              }
              return result + coordinatesColumn.formatter(row, cell, value, columnDef, dataContext);
            }
          }),
          {
            id: 'versions',
            name: 'Popularity',
            field: 'relativePopularity',
            sortable: true,
            filterable: false,
            width: 85,
            sortFn: function(dataRow1, dataRow2) {
              if (dataRow1.matchState === matchStates[0]) {
                return dataRow2.matchState === matchStates[0] ? 0 : -1;
              }
              else if (dataRow2.matchState === matchStates[0]) {
                return 1;
              }
              else if (dataRow1.identificationSource === 'Manual') {
                return dataRow1.identificationSource === dataRow2.identificationSource ? 0 : -1;
              }
              else if (dataRow2.identificationSource === 'Manual') {
                return 1;
              }
              return ((dataRow1.relativePopularity < dataRow2.relativePopularity) ? -1 : ((dataRow1.relativePopularity >
                  dataRow2.relativePopularity) ? 1 : 0));
            },
            formatter: function(row, cell, value, columnDef, dataContext) {
              if (dataContext.matchState === matchStates[0] || dataContext.identificationSource === 'Manual') {
                return '';
              }
              var popImg;
              value = Math.round(value * 100);
              if (value > 79) {
                popImg = 'popularity-100';
              }
              else if (value > 59) {
                popImg = 'popularity-80';
              }
              else if (value > 39) {
                popImg = 'popularity-60';
              }
              else if (value > 19) {
                popImg = 'popularity-40';
              }
              else if (value >= 0) {
                popImg = 'popularity-20';
              }
              return popImg ? '<div class="' + popImg + '"></div>' : '';
            }
          },
          {
            id: 'age',
            name: 'Age',
            field: 'age',
            sortable: true,
            filterable: false,
            width: 85,
            sortFn: function(dataRow1, dataRow2) {
              var a = dataRow1['age'], b = dataRow2['age'];

              if (a === undefined && b === undefined) {
                return 0;
              }
              else if (a === undefined) {
                return -1;
              }
              else if (b === undefined) {
                return 1;
              }
              else if (a < b) {
                return -1;
              }
              else if (a > b) {
                return 1;
              }

              return 0;
            },
            formatter: function(row, cell, value) {
              if (value === undefined || value === null) {
                return '';
              }

              //this is special case where we can end up showing 12.x months since i assume 30 days per month
              //so we decided upon simply showing 1 year in this small window from 360 - 365
              if (value > 359) {
                return (value / 365).toFixed(1) + ' y';
              }
              else if (value > 30) {
                return Math.floor(value / 30) + ' m';
              }

              return Math.floor(value) + ' d';
            }
          },
          {
            id: 'releasehistory',
            name: 'Release History',
            field: 'componentIdentifier',
            sortable: false,
            filterable: false,
            resizable: false,
            width: 211,
            header: '<div class="release-header"><i class="icon-question-sign" title="<div style=\'background:url(release-tooltip.png);width:241px;height:78px\'></div>"></i><span>' +
                time + '</span></div>',
            formatter: function(row, cell, value, columnDef, dataContext) {
              if (value === null || dataContext.identificationSource === 'Manual') {
                return 'No Popularity Data';
              }
              var params = {
                    'componentIdentifier': JSON.stringify(dataContext.componentIdentifier),
                    // individual GAV fields for backward-compatibility with CLM 1.12.1
                    'groupId': dataContext.groupId,
                    'artifactId': dataContext.artifactId,
                    'version': dataContext.version
                  },
                  encodedParams = '';
              $.each(params, function(field, content) {
                encodedParams += '&' + encodeURIComponent(field) + '=' + encodeURIComponent(content);
              });
              return '<img src="../brain/rest/report/' + applicationId + '/' + reportId + '/releaseGraph?' +
                  encodedParams.substring(1) + '" alt="Release Popularity">';
            }
          }
        ],
        plugins = [columnGrouping, new Insight.InformationPanel({ byHash: true })];

    if (!Brain.hasFeature('release-graph')) {
      columns[1].width += columns.pop().width;
    }

    componentTable = new Insight.Table('component', data, {
      columns: columns,
      multiColumnSort: true,
      selectable: true,
      plugins: plugins,
      defaultSort: [
        { columnId: 'policyName', sortAsc: false },
        { columnId: 'coordinates', sortAsc: true }
      ],
      externalFilters: [
        function(item, args) {
          var visible = true;
          if (args) {
            $.each(args, function(field, value) {
              return (visible = (item[field] === value));
            });
          }
          return visible;
        }
      ],
      dataProcessor: function(data) {
        data = data || [];

        $.each(data, function(key, dataItem) {
          dataItem.id = key;
          if (dataItem.createTime) {
            dataItem.age = Math.floor((new Date().getTime() - dataItem.createTime) / (1000 * 60 * 60 * 24));
          }
        });

        processCoordinatesData(data);

        return data;
      }
    });

    $('#policy-match-state-filter').unbind('click'); // remove previously registered listener

    function filterChanged(event) {
      event.preventDefault();
      $(this).parents('ul').children('li').removeClass('active');
      $(this).parents('li').addClass('active');

      var matchStateFilter = $('#policy-match-state-filter li.active').text(),
          violationFilter = $('#policy-violation-filter li.active').text(),
          filter = {};

      if (matchStateFilter === 'All') {
        // do nothing
      }
      else if (matchStateFilter === 'Proprietary') {
        filter.proprietary = true;
      }
      else if (matchStateFilter === 'Error') {
        filter.scanError = true ;
      }
      else {
        filter.matchState = matchStateFilter.toLowerCase();
      }

      if (violationFilter === 'Summary') {
        filter.summary = true;
      } else if (violationFilter === 'Waived') {
        filter.waived = true;
      } else if (violationFilter === 'All') {
        filter.all = true;
      }

      componentTable.addLoadListener(function() {
        componentTable.table.getData().setFilterArgs(filter);
        componentTable.table.getData().refresh();
      });
    }

    $('#policy-match-state-filter li a').click(filterChanged);
    $('#policy-violation-filter li a').click(filterChanged);

    // fire current state
    $('#policy-match-state-filter li.active a').trigger('click');
    componentTable.addLoadListener(function() {
      $('.release-header > .icon-question-sign').tipsy({
        gravity: 'e',
        html: true,
        title: 'title',
        opacity: 1.0
      });
    });
  }

  function createSimilarityTable(data) {
    var columns = [{
        id: '_formattedEffectiveLicenseThreat',
        name: 'License Threat',
        field: '_formattedEffectiveLicenseThreat',
        sortable: true,
        width: 65,
        sortFn: effectiveLicenseSortFn,
        formatter: effectiveLicenseHtmlFormatFn
      }, {
        id: 'securityCounters',
        name: 'Security',
        field: 'securityCounters',
        sortable: true,
        width: 50,
        sortFn: function(dataRow1, dataRow2) {
          var result = numberCompare(dataRow1['securityCounters']['Critical'],
              dataRow2['securityCounters']['Critical']);
          if (result === 0) {
            result = numberCompare(dataRow1['securityCounters']['Severe'], dataRow2['securityCounters']['Severe']);
          }
          if (result === 0) {
            result = numberCompare(dataRow1['securityCounters']['Moderate'], dataRow2['securityCounters']['Moderate']);
          }
          return result;
        },
        formatter: function(row, cell, value, columnDef, dataContext) {
          var security = dataContext.securityCounters,
              result = '',
              total,
              offset,
              maxArrows,
              arrowWidth = 10,
              offsetCounter = 0,
              width = columnDef.width - arrowWidth,
              getSecurityCounterSpan = function(count, color) {
                var i;
                for (i = 0; i < count; i++) {
                  if (offsetCounter >= maxArrows) {
                    break;
                  }
                  result += '<span class="arrow ' + color + '_arrow" style="left:' + (offsetCounter++ * offset) +
                      'px"></span>';
                }
              };

          if (security) {
            total = security['Critical'] + security['Severe'] + security['Moderate'];

            for (offset = 0; offset > -arrowWidth + 1 && (total - 1) * (arrowWidth + offset) + arrowWidth > width;
                 offset--) {
              // Work is done in the loop description
            }

            maxArrows = (width - arrowWidth) / (arrowWidth + offset) + 1;
            if (security['Critical']) {
              getSecurityCounterSpan(security['Critical'], 'red');
            }
            if (security['Severe']) {
              getSecurityCounterSpan(security['Severe'], 'orange');
            }
            if (security['Moderate']) {
              getSecurityCounterSpan(security['Moderate'], 'yellow');
            }
          }
          return '<div style="white-space:nowrap;width:' +
              (offsetCounter > 0 ? 10 + (offsetCounter - 1) * (10 + offset) : 0) + 'px">' + result + '</div>';
        }
      },
      $.extend({}, coordinatesColumn, {
        width: 210
      })
    ];

    similarityTable = new Insight.Table('similarity', data, {
      selectable: true,
      disablePager: true,
      disableFilter: true,
      height: 250,
      columns: columns,
      dataProcessor: function(data) {
        if (!data) {
          data = [];
        }

        $.each(data, function(index, item) {
          item.id = index;
          item['_formattedEffectiveLicenseThreat'] = effectiveLicenseFormatFn(item.overriddenLicenses,
                  item.declaredLicenses, item.observedLicenses);
        });

        processCoordinatesData(data);

        return data;
      }
    });
    similarityTable.table.render(); // No default sort set so this is apparently required
    similarityTable.table.columnsResizedFn = function() {
      var i;
      for (i = 0; i < similarityTable.getDataLength(); i++) {
        similarityTable.updateCell(i, similarityTable.getColumnIndex('securityCounters'));
      }
    };

    similarityTable.table.onColumnsResized.subscribe(similarityTable.table.columnsResizedFn);

    $('#similarityTable').click(function(e) {
      e.stopPropagation();
    });
  }

  function createAuditTable(data) {
    var columns = [
      {
        id: 'date',
        name: 'Date',
        field: 'date',
        formatter: function(row, cell, val) {
          return dateFormat(val, 'mmm d yyyy, h:MM:ss tt');
        },
        sortable: true,
        width: 30
      },
      {
        id: 'user',
        name: 'User',
        field: 'user',
        toolTipGravity: 'n',
        toolTipFn: function(row) {
          if (Insight.util.isNullOrUndefined(row.where)) {
            return Insight.util.encodeHtml('Edited report from ' + row.ip);
          }
          return Insight.util.encodeHtml('Edited report in build \'' + row.where + '\' from ' + row.ip);
        },
        sortable: true,
        width: 20
      },
      {
        id: 'action',
        name: 'Action',
        field: 'action',
        sortable: true,
        width: 20
      },
      {
        id: 'detail',
        name: 'Detail',
        field: 'detail',
        sortable: true,
        width: 40
      },
      {
        id: 'comment',
        name: 'Comment',
        field: 'comment',
        toolTipGravity: 'n',
        toolTipFn: function(row) {
          if (row.comment) {
            return Insight.util.encodeHtml(row.comment);
          }
        },
        sortable: true,
        width: 80
      }
    ];
    auditTable = new Insight.Table('audit', data, {
      resizeFn: $.noop,
      selectable: true,
      disablePager: true,
      disableFilter: true,
      height: 250,
      columns: columns,
      defaultSort: { columnId: 'date', sortAsc: false },
      dataProcessor: function(data) {
        $.each(data, function(index, item) {
          var action = item.status,
              detail = '',
              overriddenLicenses,
              source,
              reference;

          item['id'] = index;
          item['date'] = new Date(item['time']);
          switch (item['status']) {
            case 'Open':
              action = 'Reopened';
              break;
            case 'Not Applicable':
              action = 'Ignored';
              break;
            case 'Overridden':
              action = 'Overrode';
              break;
          }
          item['action'] = action;

          if (Insight.util.isNullOrUndefined(item['user'])) {
            item['user'] = 'anonymous';
          }

          switch (item['filename']) {
            case 'licenses.json':
              detail = 'License ';
              overriddenLicenses = item['overriddenLicenses'];
              if (Insight.util.isNotNullOrUndefined(overriddenLicenses)) {
                detail += 'as ' + renderLicenses(overriddenLicenses);
              }
              else {
                detail += 'Analysis';
              }
              break;
            case 'security.json':
              detail = 'Vulnerability ';
              source = item['source'];
              reference = item['reference'];
              if (Insight.util.isNotNullOrUndefined(source) &&
                  reference.toUpperCase().indexOf(source.toUpperCase()) !== 0) {
                detail += source + '-';
              }
              detail += reference;
              break;
          }
          item['detail'] = detail;
        });
        return data;
      }
    });

    $('#' + auditTable.id + 'Table').click(function(e) {
      e.stopPropagation();
    });
  }

  // register namespace
  $.extend(true, window, {
    'InsightDatatable': {
      'createAuditTable': createAuditTable,
      'createComponentTable': createComponentTable,
      'createSimilarityTable': createSimilarityTable,
      'destroyActiveTable': destroyActiveTable,
      'getActiveTable': getActiveTable,
      'loadLicenseThreats': loadLicenseThreats,
      'getLicenseThreatLevelFromArray': getLicenseThreatLevelFromArray,
      'getLicenseThreatClass': getLicenseThreatClass,
      'removeAuditTable': removeAuditTable,
      'removeComponentTable': removeComponentTable,
      'removeSimilarityTable': removeSimilarityTable
    }
  });
}());
