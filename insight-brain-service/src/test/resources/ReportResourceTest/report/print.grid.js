/**
 * @license Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/pro/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 (function(){
$.extend(true, window, {
	PrintTable: {
		Grid: PrintTable
	}
});
	var $style = $("<style type='text/css' rel='stylesheet' />").appendTo($("head")),
		css = '.simple-cell {border-right: 1px solid #CFD0D2;border-bottom: 1px dashed #E9E9E9;font-size: 11px;font-family: Arial, Helvetica, sans-serif;color: #656565;padding-left: 6px;padding-top:0px;padding-bottom:0px}';
	if ($style[0].styleSheet) { // IE
		$style[0].styleSheet.cssText = css;
	} else {
		$style[0].appendChild(document.createTextNode(css));
	}

function PrintTable(){
	var locationId = arguments[0],
		data = arguments[1],
		columns = arguments[2],
		self = this;

	function init() {
		data.onRowsChanged.subscribe(function(){setTimeout(createTable,1);});
	}

	function registerPlugin(plugin) {
		plugin.init(self);
	}

	function getColumns() {
		return columns;
	}

	function createTable(){
		$(locationId).css('height', '');
		var table = $('<table></table>').appendTo($(locationId).empty());
		table.css('width', '100%');
		var header = '<thead class="slick-header-columns">';
		$.each(columns,function(colNum, col){
			header += '<th class="slick-header-column">' + col.name + '</th>';
		});
		header += '</thead>';
		table.append(header);

		table.append('<tbody>');
		$.each(data.getItems(), function(row, item){
			var rowbody = '<tr class="slick-row ' + (row % 2 === 1 ? 'odd' : 'even' ) + '">';
			$.each(columns, function(cell, col){
				rowbody += '<td class="simple-cell ' + (col.styleFn ? col.styleFn(row, cell, item[col.field], col, item) : '') +'">';
				if (col.formatter) {
					rowbody += col.formatter(row, cell, item[col.field], col, item);
				} else {
					rowbody += item[col.field].toString().replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");
				}
				rowbody += '</td>';
			});
			rowbody += '</tr>';
			$('tbody', locationId).append(rowbody);
			//$(rowbody).appendTo(body);
		});
	}

	function getData() {
		return data;
	}

	function setSortColumn(columnId, ascending) {
		setSortColumns([{ columnId: columnId, sortAsc: ascending}]);
	}

	function setSortColumns(columns) {
		_sortCol = columns;
	}

	function getSortColumns() {
		return _sortCol;
	}

	function getSelectedRows() {
		return [];
	}

	$.extend(this,{
		"init" : init,
		"setSortColumn" : setSortColumn,
		"setSortColumns" : setSortColumns,
		"registerPlugin" : registerPlugin,
		"getData" : getData,
		"getColumns" : getColumns,
		"getSortColumns" : getSortColumns,
		"render" : createTable,
		"getSelectedRows" : getSelectedRows,
		"getDataItem" : data.getItem,
		// Events
		"onScroll": new Slick.Event(),
		"onSort": new Slick.Event(),
		"onHeaderContextMenu": new Slick.Event(),
		"onHeaderClick": new Slick.Event(),
		"onMouseEnter": new Slick.Event(),
		"onMouseLeave": new Slick.Event(),
		"onClick": new Slick.Event(),
		"onDblClick": new Slick.Event(),
		"onContextMenu": new Slick.Event(),
		"onKeyDown": new Slick.Event(),
		"onAddNewRow": new Slick.Event(),
		"onValidationError": new Slick.Event(),
		"onViewportChanged": new Slick.Event(),
		"onColumnsReordered": new Slick.Event(),
		"onColumnsResized": new Slick.Event(),
		"onColumnsChanged" : new Slick.Event(),
		"onCellChange": new Slick.Event(),
		"onBeforeEditCell": new Slick.Event(),
		"onBeforeCellEditorDestroy": new Slick.Event(),
		"onBeforeDestroy": new Slick.Event(),
		"onActiveCellChanged": new Slick.Event(),
		"onActiveCellPositionChanged": new Slick.Event(),
		"onDragInit": new Slick.Event(),
		"onDragStart": new Slick.Event(),
		"onDrag": new Slick.Event(),
		"onDragEnd": new Slick.Event(),
		"onSelectedRowsChanged": new Slick.Event(),
		"onCellCssStylesChanged": new Slick.Event(),

		"addCellCssStyles" : $.noop,
		"autosizeColumns" : $.noop,
		"canCellBeActive" : $.noop,
		"canCellBeSelected" : $.noop,
		"destroy" : $.noop,
		"editActiveCell" : $.noop,
		"flashCell" : $.noop,
		"getActiveCell" : $.noop,
		"getActiveCellNode" : $.noop,
		"getActiveCellPosition" : $.noop,
		"getCanvasNode" : $.noop,
		"getCellCssStyles" : $.noop,
		"getCellEditor" : $.noop,
		"getCellFromEvent" : $.noop,
		"getCellFromPoint" : $.noop,
		"getCellNode" : $.noop,
		"getCellNodeBox" : $.noop,
		"getColumnIndex" : $.noop,
		"getDataLength" : $.noop,
		"getEditController" : $.noop,
		"getEditorLock" : $.noop,
		"getGridPosition" : $.noop,
		"getHeader" : $.noop,
		"getHeaderRow" : $.noop,
		"getHeaderRowColumn" : $.noop,
		"getOptions" : $.noop,
		"getRenderedRange" : $.noop,
		"getSelectionModel" : $.noop,
		"getTopPanel" : $.noop,
		"getViewport" : $.noop,
		"gotoCell" : $.noop,
		"hideHeaderRowColumns" : $.noop,
		"hideTopPanel" : $.noop,
		"invalidate" : $.noop,
		"invalidateAllRows" : $.noop,
		"invalidateRow" : $.noop,
		"invalidateRows" : $.noop,
		"navigateDown" : $.noop,
		"navigateLeft" : $.noop,
		"navigateNext" : $.noop,
		"navigatePrev" : $.noop,
		"navigateRight" : $.noop,
		"navigateUp" : $.noop,
		"removeCellCssStyles" : $.noop,
		"resetActiveCell" : $.noop,
		"resizeCanvas" : $.noop,
		"scrollRowIntoView" : $.noop,
		"setActiveCell" : $.noop,
		"setCellCssStyles" : $.noop,
		"setColumns" : $.noop,
		"setData" : $.noop,
		"setOptions" : $.noop,
		"setSelectedRows" : $.noop,
		"setSelectionModel" : $.noop,
		"showHeaderRowColumns" : $.noop,
		"showTopPanel" : $.noop,
		"unregisterPlugin" : $.noop,
		"updateCell" : $.noop,
		"updateColumnHeader" : $.noop,
		"updateRow" : $.noop,
		"updateRowCount" : $.noop
	});
}

}());