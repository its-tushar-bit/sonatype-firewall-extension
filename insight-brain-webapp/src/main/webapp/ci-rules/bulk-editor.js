/**
 * @license Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/pro/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function ($) {
	// register namespace
	$.extend(true, window, {
		"Slick": {
			"BulkEditor": BulkEditor
		}
	});

	function BulkEditor(options) {
		var _grid,
			_inHandler,
			_self = this,
			_selectedRowsLookup = {},
			_handler = new Slick.EventHandler();

		options = $.extend({ columnId : 'bulkeditcol', width : 56 }, options);

		function init(grid) {
			_grid = grid;
			_handler.subscribe(_grid.onSelectedRowsChanged, wrapHandler(_selectionChanged));
			_grid.onClick.subscribe(handleClick);
			_grid.onHeaderClick.subscribe(handleHeaderClick);

			$(_grid.getHeaderRowColumn(options.columnId)).children('input:first').removeAttr('placeholder');
		}

		function destroy() {
			options.editor.destroy();
			_grid.onClick.unsubscribe(handleClick);
			_grid.onHeaderClick.unsubscribe(handleHeaderClick);
			$(options.editNode).css('display', 'none');
		}

		function _showEditor() {
			if ( _grid.getSelectedRows().length > 0 ) {
				var selectedItems = [];
				$.each(_grid.getSelectedRows(), function(index, item) {
					selectedItems.push(_grid.getData().getItem(item));
				});
				editor = options.editor.show($(options.editNode));
				$(options.editNode).show();
				//$(options.editNode).css('display', '');
				options.openCallback.apply(_grid,[]);
			}
		}

		function wrapHandler(handler) {
			return function () {
				if (!_inHandler) {
					_inHandler = true;
					handler.apply(this, arguments);
					_inHandler = false;
				}
			};
		}

		function _selectionChanged(e, args) {
			var selectedRows = args.rows;
			var lookup = {}, row, i;
			for (i = 0; i < selectedRows.length; i++) {
				row = selectedRows[i];
				lookup[row] = true;
				if (lookup[row] !== _selectedRowsLookup[row]) {
					_grid.invalidateRow(row);
					delete _selectedRowsLookup[row];
				}
			}
			for (i in _selectedRowsLookup) {
				_grid.invalidateRow(i);
			}
			_selectedRowsLookup = lookup;
			_grid.render();

			$('a',_grid.getHeader(options.columnId)).attr('disabled', args.rows.length > 0 ? null : 'disabled');
		}

		function getColumnDef() {
			return {
				id : options.columnId,
				name : "<a href='#' class='btn btn-primary' disabled='disabled'>Edit</a>",
				field : 'sel',
				width : options.width,
				resizable : false,
				sortable : false,
				cssClass : options.cssClass,
				formatter : checkboxSelectionFormatter,
				styleFn : function() { return 'center'; }
			};
		}

		function handleClick(e, args) {
			if (_grid.getColumns()[args.cell].id === options.columnId) {
				e.stopPropagation();
				e.stopImmediatePropagation();
				var widget = $(e.target);

				if (!widget.is(':checkbox')) {
					// did user click in surrounding div?
					widget = widget.children('input:first');
					if (!widget.is(':checkbox')) {
						return; // nope, ignore
					}
					// toggle state on behalf of the user
					widget[0].checked = !widget[0].checked;
				}

				if (!widget[0].checked) {
					_grid.setSelectedRows($.grep(_grid.getSelectedRows(), function(element){ return element !== args.row; }));
				} else {
					_grid.setSelectedRows($.merge(_grid.getSelectedRows(), [args.row]));
				}
			}
		}

		function handleHeaderClick(e, args) {
			if (e.target.tagName === 'A') {
				e.stopPropagation();
				e.stopImmediatePropagation();
				_showEditor();
			}
		}

		function checkboxSelectionFormatter(row, cell, value, columnDef, dataContext) {
			if (dataContext) {
				return _selectedRowsLookup[row] ?
					"<input type='checkbox' checked='checked'>"
					: "<input type='checkbox'>";
			}
			return null;
		}

		$.extend(this, {
			"init": init,
			"destroy": destroy,
			"getColumnDef" : getColumnDef
		});
	}
})(jQuery);
