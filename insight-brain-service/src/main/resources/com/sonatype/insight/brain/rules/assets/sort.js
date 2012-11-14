/**
 * @license Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/oss/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function($) {
    // register namespace
    $.extend(true, window, {
        "Slick" : {
            "Sort" : Sort
        }
    });

    function Sort(options) {
        var _grid;
        var _self = this;
        var _inHandler;
        var _handler = wrapHandler(_defaultSort);

        function init(grid) {
            _grid = grid;
            _handler = wrapHandler(_defaultSort);
            _grid.onSort.subscribe(_doSort);
            _grid.getData().onRowCountChanged.subscribe(_handler);
            _grid.getData().onRowsChanged.subscribe(_handler);
        }

        function destroy() {
            _grid.getData().onRowCountChanged.unsubscribe(_handler);
            _grid.getData().onRowsChanged.unsubscribe(_handler);
            _grid.onSort.unsubscribe(_doSort);
            _grid = null;
        }

        function _defaultSort() {
            var sortColumns = _grid.getSortColumns();
            if (sortColumns.length > 0) {
                var sortCols = [];
                $.each(sortColumns, function(index, item) {
                    sortCols.push({
                        sortCol : {
                            id : item.columnId
                        },
                        sortAsc : item.sortAsc
                    });
                });
                _doSort(null, {
                    multiColumnSort : true,
                    sortCols : sortCols
                });
            }
        }

        function wrapHandler(handler) {
            return function() {
                if (!_inHandler) {
                    _inHandler = true;
                    handler.apply(this, arguments);
                    _inHandler = false;
                }
            };
        }

        function _doSort(e, args) {
            var sortFn = null;

            if (args.multiColumnSort) {
                var sortData = [];

                $.each(args.sortCols, function(index, item) {
                    $.each(_grid.getColumns(), function(subindex, subitem) {
                        if (item.sortCol.id === subitem.id) {
                            sortData.push({
                                sortFn : subitem.sortFn,
                                field : subitem.field,
                                sortAsc : item.sortAsc
                            });
                            return false;
                        }
                    });
                });

                //create our own uber sort function to do all the levels of sorting
                sortFn = function(dataRow1, dataRow2) {
                    var sortResult = 0;
                    $.each(sortData, function(index, item) {
                        if (item.sortFn) {
                            var result = item.sortFn(dataRow1, dataRow2)
                            if (result != 0) {
                                sortResult = item.sortAsc === false ? -result : result;
                                return false;
                            }
                        } else if (dataRow1[item.field] > dataRow2[item.field]) {
                            sortResult = item.sortAsc === false ? -1 : 1;
                            return false;
                        } else if (dataRow1[item.field] < dataRow2[item.field]) {
                            sortResult = item.sortAsc === false ? 1 : -1;
                            return false;
                        }
                    });

                    return sortResult;
                };

                _grid.getData().sort(sortFn, true);
            } else {
                var sortColumn = args.sortCol, gridColumns = _grid.getColumns(), key;

                $.each(gridColumns, function(index, item) {
                    if (item.id === sortColumn.id) {
                        sortFn = item.sortFn;
                        sortColumn.field = item.field;
                        return false;
                    }
                });

                sortFn = sortFn || function(dataRow1, dataRow2) {
                    var a = dataRow1[sortColumn.field], b = dataRow2[sortColumn.field];
                    return a > b ? 1 : (a < b ? -1 : 0);
                };

                _grid.getData().sort(sortFn, args.sortAsc);
            }

            if (_grid) {
                _grid.invalidate();
                _grid.onViewportChanged.notify({}, new Slick.EventData(), _grid);
            }
        }

        $.extend(this, {
            "init" : init,
            "destroy" : destroy
        });
    }
})(jQuery);