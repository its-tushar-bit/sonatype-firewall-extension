/**
 * @license Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/oss/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 (function ($) {
  // register namespace
  $.extend(true, window, {
		"Slick": {
		  "Filter": Filter
		}
	});

	function Filter(options) {
		var _grid;
		var _self = this;
		var _inHandler;
		var _columnFilters = {};
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
			_grid.onColumnsReordered.subscribe(_updateHeaderRow);
			_grid.onColumnsResized.subscribe(_updateHeaderRow);
			_grid.onColumnsChanged.subscribe(_updateHeaderRow);
			_grid.getData().onRowsChanged.subscribe(_rowsChanged);
			_grid.getData().onRowCountChanged.subscribe(_rowCountChanged);
			_updateHeaderRow();
			$(_grid.getHeaderRow()).delegate(":input", "change keyup", function (e) {
				_columnFilters[$(this).data("columnId")] = $.trim($(this).val()).toUpperCase();
				_grid.getData().refresh();
			});
		}

		function destroy() {
			_grid.getData().onRowsChanged.unsubscribe(_rowsChanged);
			_grid.getData().onRowCountChanged.unsubscribe(_rowCountChanged);
			_grid.onColumnsReordered.unsubscribe(_updateHeaderRow);
			_grid.onColumnsResized.unsubscribe(_updateHeaderRow);
			_grid.onColumnsChanged.unsubscribe(_updateHeaderRow);
			_grid = null;
		}

		function _rowsChanged(e, args) {
			_grid.invalidateRows(args.rows);
			_grid.render();
		}

		function _rowCountChanged(e,args) {
			_grid.updateRowCount();
			_grid.render();
		}

		function _updateHeaderRow() {
			var columns = _grid.getColumns(),
				header, node;
			for (var i = 0; i < columns.length; i++) {
				if (columns[i].id !== "selector") {
					header = _grid.getHeaderRowColumn(columns[i].id);
					$(header).empty();
					if (columns[i].filterable === false) {
						node = $("<div style='height:21px'></div>");
						if (columns[i].header) {
							node.html(columns[i].header);
						}
					} else {
						node = $("<input type='text' style='width:100%'>")
							.data("columnId", columns[i].id)
							.attr('placeholder', ' Search ' + ((columns[i].shortName != null) ? columns[i].shortName : columns[i].name))
							.val(_columnFilters[columns[i].id]);
					}
					node.appendTo(header);
				}
			}
		}

		function getFilter() {
			return function filter(item) {
				for (var columnId in _columnFilters) {
					if (columnId !== undefined && _columnFilters[columnId] !== "") {
						var c = _grid.getColumns()[_grid.getColumnIndex(columnId)];
						
						var fieldData = item[c.field];
						
						//just in case we have an array of data
						if ( fieldData instanceof Array ) {
						    fieldData = fieldData.join();
						}

						//just being safe in case the field wasn't present in the data
                        if ( fieldData === undefined || fieldData === null ) {
                            fieldData = '';
                        }

                        //force the field into a string
                        if (('' + fieldData).toUpperCase().indexOf(_columnFilters[columnId]) === -1) {
                            return false;
                        }
					}
				}
				return true;
			};
		}

		$.extend(this, {
			"init": init,
			"destroy": destroy,
			"getFilter" : getFilter
		});
	}
})(jQuery);