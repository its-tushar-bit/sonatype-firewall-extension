/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Slick, $, clmBuildTimestamp, Insight, ComponentInformationPanelPlugin, Brain */
import template from './audit.threat.directive.html';

var encoder = $('<div></div>');
function encodeHtml(text) {
  return encoder.text(text).html();
}

function processData(data, idBase) {
  data = data || [];
  var componentWaivedMap = [], idx = idBase ? idBase : 0;

  $.each(data, function(key, dataItem) {
    if (dataItem.waived) {
      componentWaivedMap[dataItem.pathname] = true;
    }

    //pseudo violation, marked as a not a violation, simply to get component in list
    //checking for existing waived item so that we don't dump components that simply have
    //no violations
    //it is expected that items are sorted by threat level descending
    if (!dataItem.policyName && componentWaivedMap[dataItem.pathname]) {
      dataItem.pseudo = true;
    }
    else {
      dataItem.pseudo = false;
    }

    dataItem.id = idx++;

    dataItem.policyName = dataItem.policyName || 'No Violations';
  });

  return data;
}

function createTable(data, $scope) {
  var columnGrouping = new Slick.ColumnGrouping({ columnId: 'policyName', style: 'scoreCol' }),
    scoreStyler = columnGrouping.getCellStyler(),
    cellFormatter = columnGrouping.getCellRenderer(),
    columns = [{
      id: 'policyName',
      name: 'Policy Threat',
      shortName: 'Name',
      field: 'policyName',
      sortable: true,
      width: 215,
      styleFn: function(row, cell, value, columnDef, dataContext) {
        return 'nopad ' + scoreStyler(row, cell, value, columnDef, dataContext);
      },
      sortFn: function(dataRow1, dataRow2) {
        var levelA = dataRow1.threatLevel,
            levelB = dataRow2.threatLevel,
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

        if (dataContext.threatLevel > 7) {
          colorCls = 'criticalScore';
        }
        else if (dataContext.threatLevel > 3) {
          colorCls = 'severeScore';
        }
        else if (dataContext.threatLevel > 1) {
          colorCls = 'moderateScore';
        }
        else if (dataContext.threatLevel > 0) {
          colorCls = 'ignoredScore';
        }
        else {
          colorCls = 'noScore';
        }

        return '<div class="' + colorCls + '">' +
            (cellFormatter(row, cell, value, columnDef, dataContext).length > 0 ? encodeHtml(value) : '') + '</div>';
      }
    },{
      id: 'coordinates',
      name: 'Component',
      field: 'componentDisplayText',
      shortName: 'Coordinates',
      sortable: true,
      width: 295,
      toolTipGravity: 'se',
      styleFn: function(row, cell, value, columnDef, dataContext) {
        return (dataContext.modified || dataContext.identificationSource === 'Manual') ? 'modified' : '';
      },
      toolTipFn: function(row) {
        var tip = '';
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
          result = '<i class="waived" title="Violation has been waived"></i>';
        }
        else {
          result = '<i class="not-waived"></i> ';
        }

        var icon;
        if (dataContext.componentIdentifier) {
          icon = '<i class="known-format" title="' + dataContext.componentIdentifier.format + '"></i> ';
        }
        else {
          icon = '<i class="unknown-format" title="Unknown"></i> ';
        }
        return result + icon + encodeHtml(value);
      }
    },{
      id: 'quarantine',
      name: 'Quarantined',
      field: 'quarantined',
      sortable: true,
      filterable: false,
      width: 60,
      styleFn: function () {
        return 'middle';
      },
      formatter: function(row, cell, value) {
        if (value) {
          return '<i class="icon icon-ban-circle"></i>';
        }
        return '';
      }
    }],
    plugins = [columnGrouping, new ComponentInformationPanelPlugin($scope)];

  return new Insight.Table('component', data, {
    columns: columns,
    multiColumnSort: true,
    selectable: true,
    plugins: plugins,
    defaultSort: [{
      columnId: 'policyName',
      sortAsc: false
    }, {
      columnId: 'coordinates',
      sortAsc: true
    }],
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
    dataProcessor: processData
  });
}

export default function auditThreat() {
  return {
    template,
    controllerAs: 'vm',
    controller : ['$scope', 'OwnerContext', '$http', function ($scope, OwnerContext, $http) {
      var vm = this;

      vm.error = undefined;
      vm.grid = undefined;

      vm.doLoad = doLoad;

      function doLoad() {
        delete vm.error;

        $http.get(Brain.getRepositoryResultsUrl(OwnerContext.ownerId)).then(function (response) {
          vm.loaded = true;
          $scope.$applyAsync(function () {
            vm.grid = createTable(response.data, $scope);
            setFilter();
          });
        }, function (error) {
          vm.error = error;
        });
      }

      function setFilter() {
        if (vm.grid && vm.filter) {
          vm.grid.dataView.setFilterArgs(vm.filter);
          vm.grid.dataView.refresh();
        }
      }

      vm.doLoad();

      $scope.$watch('vm.filter', setFilter);

      $scope.$on('$destroy', function () {
        if (vm.grid) {
          vm.grid.destroy();
        }
      });

      $scope.$on('component.evaluation.updated', function (event, componentKey, promises) {
        function matches(component) {
          return !Object.keys(componentKey).some(function (key) {
            return component[key] !== componentKey[key];
          });
        }

        promises.push($http.get(Brain.getRepositoryResultsUrl(OwnerContext.ownerId, componentKey))
          .then(function(response) {
              var data = response.data,
                  dataView = vm.grid.dataView,
                  maxId = -1,
                  idsToRemove = [],
                  newItemMap = {},
                  updatedItemMap = {};

              processData(data, 0);

              data.forEach(function (item) {
                (newItemMap[item.pathname] = newItemMap[item.pathname] || {})[item.policyName] = item;
                updatedItemMap[item.pathname] = updatedItemMap[item.pathname] || {};
              });

              dataView.beginUpdate();
              // update existing rows
              dataView.getItems().forEach(function(item) {
                maxId = Math.max(maxId, item.id);

                if (matches(item)) {
                  if (newItemMap[item.pathname] && newItemMap[item.pathname][item.policyName]) {
                    // update id
                    newItemMap[item.pathname][item.policyName].id = item.id;
                    // update entry
                    dataView.updateItem(item.id, newItemMap[item.pathname][item.policyName]);
                    // don't need to add this one
                    updatedItemMap[item.pathname][item.policyName] = true;
                  }
                  else {
                    // can't delete during iteration, collect for later
                    idsToRemove.push(item.id);
                  }
                }
              });

              idsToRemove.forEach(function (id) {
                dataView.deleteItem(parseInt(id, 10));
              });

              // reduce to the new entries
              data = data.filter(function (item) {
                return !updatedItemMap[item.pathname][item.policyName];
              });

              //add new entries
              data.forEach(function (newItem) {
                newItem.id = ++maxId;
                dataView.addItem(newItem);
              });

              dataView.endUpdate();
            }));
      });
    }]
  };
}
