/**
 * @license Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/oss/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 (function ($) {
  // register namespace
  $.extend(true, window, {
		"Slick": {
		  "ColumnGrouping": ColumnGrouping
		}
	});

	function ColumnGrouping(options) {
		var _grid;
		var _self = this;
		var _inHandler;
		var _handler = new Slick.EventHandler();
		var _styleHash = 'groupedColumn';
		var _cells; // cells with no bottoms
		var _firstCells; // cells we should show text
		var _shouldGroup;
		var _defaults = {
			groupedStyle : ' groupedCell',
			style : ''
		};

		options = $.extend(true, {}, _defaults, options);
		options.style = ' ' + options.style;

		function _invalidate() {
			this._cells = null;
			this._firstCells = null;
		}

		function init(grid) {
			_grid = grid;
			var columns = _grid.getSortColumns();
			_shouldGroup = columns.length > 0 && columns[0].columnId === options.columnId;
			_grid.onSort.subscribe(handleSort);
		}

		function destroy() {
			_grid = null;
		}

		function handleSort(e, args) {
			var columns = args.sortCols;
			if (!columns) {
				columns = [{sortCol:args.sortCol}];
			}
			_shouldGroup = columns.length > 0 && columns[0].sortCol.id === options.columnId;
		}

		function getCellRenderer() {
			var me = this;
			return function(row, cell, value, columnDef, dataContext) {
				var prevRowItem = _grid.getDataItem(row - 1);
				return (!_shouldGroup || !prevRowItem || prevRowItem[columnDef.id] !== value) ? value : '';
			};
		}

		function getCellStyler() {
			var me = this;
			return function(row, cell, value, columnDef, dataContext) {
				var nextRowItem = _grid.getDataItem(row + 1);
				if (_shouldGroup) {
					return (nextRowItem && (nextRowItem[columnDef.id] === value)) ? (options.groupedStyle + options.style) : options.style;
				}
				return '';
			};
		}

		$.extend(this, {
			"init": init,
			"destroy": destroy,
			"handleSort" : handleSort,
			"getCellRenderer" : getCellRenderer,
			"getCellStyler" : getCellStyler
		});
	}
})(jQuery);