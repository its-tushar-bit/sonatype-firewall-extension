/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Slick, $, clmBuildTimestamp, Insight, ComponentInformationPanelPlugin */
(function () {
  'use strict';

  var encoder = $('<div></div>');
  function encodeHtml(text) {
    return encoder.text(text).html();
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
        width: 225,
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
          var icon = '<i class="unknown-format" title="Unknown"></i> ';
          if (dataContext.componentIdentifier) {
            switch(dataContext.componentIdentifier.format) {
              case 'nuget':
                icon = '<i class="nuget" title="NuGet"></i> ';
                break;
              case 'maven':
                icon = '<i class="maven" title="Maven"></i> ';
                break;
            }
          }
          return result + icon + encodeHtml(value);
        }
      },{
        id: 'quarantine',
        name: 'Quarantined',
        field: 'quarantined',
        sortable: true,
        filterable: false,
        width: 50,
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
      dataProcessor: function(data) {
        data = data || [];

        $.each(data, function(key, dataItem) {
          dataItem.id = key;
          dataItem.policyName = dataItem.policyName || 'No Violations';
        });

        return data;
      }
    });
  }

  function auditThreat() {
    return {
      templateUrl : 'audit/audit.threat.directive.html?' + clmBuildTimestamp,
      controllerAs: 'vm',
      controller : ['$scope', 'OwnerContext', '$http', function ($scope, OwnerContext, $http) {
        var vm = this;

        vm.error = undefined;
        vm.grid = undefined;

        vm.doLoad = doLoad;

        function doLoad() {
          delete vm.error;

          $http.get('/rest/repositories/' + OwnerContext.ownerId + '/report/details').success(function (data) {
            vm.grid = createTable(data, $scope);
            setFilter();
          }).error(function () {
            vm.error = arguments;
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
      }]
    };
  }

  angular.module('Audit').directive('auditThreat', auditThreat);
}());
