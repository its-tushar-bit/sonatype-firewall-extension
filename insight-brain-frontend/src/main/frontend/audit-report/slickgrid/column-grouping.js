/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function ($) {
  'use strict';
  // register namespace
  $.extend(true, window, {
    Slick: {
      ColumnGrouping: ColumnGrouping,
    },
  });

  function ColumnGrouping(options) {
    var _grid;
    var _shouldGroup;
    var _defaults = {
      groupedStyle: ' groupedCell',
      style: '',
    };

    options = $.extend(true, {}, _defaults, options);
    options.style = ' ' + options.style;

    function init(grid) {
      _grid = grid;
      var columns = _grid.getSortColumns();
      _shouldGroup =
        columns.length > 0 && columns[0].columnId === options.columnId;
      _grid.onSort.subscribe(handleSort);
    }

    function destroy() {
      _grid = null;
    }

    function handleSort(e, args) {
      var columns = args.sortCols;
      if (!columns) {
        columns = [{ sortCol: args.sortCol }];
      }
      _shouldGroup =
        columns.length > 0 && columns[0].sortCol.id === options.columnId;
    }

    function getCellRenderer() {
      return function (row, cell, value, columnDef) {
        var prevRowItem = _grid.getDataItem(row - 1);
        return !_shouldGroup ||
          !prevRowItem ||
          prevRowItem[columnDef.id] !== value
          ? value
          : '';
      };
    }

    function getCellStyler() {
      return function (row, cell, value, columnDef) {
        var nextRowItem = _grid.getDataItem(row + 1);
        if (_shouldGroup) {
          return nextRowItem && nextRowItem[columnDef.id] === value
            ? options.groupedStyle + options.style
            : options.style;
        }
        return '';
      };
    }

    $.extend(this, {
      init: init,
      destroy: destroy,
      handleSort: handleSort,
      getCellRenderer: getCellRenderer,
      getCellStyler: getCellStyler,
    });
  }
})(jQuery);
