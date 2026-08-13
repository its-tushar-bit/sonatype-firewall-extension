/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/oss/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function($) {
    // register namespace
    $.extend(true, window, {
        "Slick" : {
            "Tipsy" : SlickGridTipsy
        }
    });

    function SlickGridTipsy(options) {
        if (!jQuery.fn.tipsy) {
            throw new Error("SlickGrid Tipsy plugin requires jquery.tipsy module to be loaded");
        }

        var _grid;

        var _defaults = {
            maxToolTipLength : null,
            disableAutoTooltips : true
        };

        var _headerTimer;
        var _cellTimer;

        function init(grid) {
            _grid = grid;
            options = $.extend(true, {}, _defaults, options);
            _grid.onColumnsReordered.subscribe(_updateHeaderTipsy);
            _grid.onColumnsResized.subscribe(_updateHeaderTipsy);
            _grid.onViewportChanged.subscribe(_updateCellTipsy);
            _updateHeaderTipsy();
        }

        function destroy() {
            _grid.onColumnsReordered.unsubscribe(_updateHeaderTipsy);
            _grid.onColumnsResized.unsubscribe(_updateHeaderTipsy);
            _grid.onViewportChanged.unsubscribe(_updateCellTipsy);
            if (_headerTimer) {
                clearTimeout(_headerTimer);
            }
            if (_cellTimer) {
                clearTimeout(_cellTimer);
            }
            $('.tipsy').remove();
            _grid = null;
        }

        function _updateCellTipsy() {
            //i use a timeout here, otherwise seems dom isn't in proper state yet in certain cases
            if (_cellTimer) {
                clearTimeout(_cellTimer);
            }
            _cellTimer = setTimeout(function() {
                var columns = _grid.getColumns();
                for ( var i = 0; i < columns.length; i++) {
                    if (columns[i].id !== "selector") {
                        var index = i;
                        var toolTipFn = columns[i].toolTipFn;
                        var toolTipGravity = columns[i].toolTipGravity;
                        _addTipsyToCellNodes(index, toolTipFn, toolTipGravity);
                    }
                }
            }, 500);
        }

        function _updateHeaderTipsy() {
            //i use a timeout here, otherwise seems dom isn't in proper state yet in certain cases
            if (_headerTimer) {
                clearTimeout(_headerTimer);
            }
            _headerTimer = setTimeout(function() {
                var columns = _grid.getColumns();
                for ( var i = 0; i < columns.length; i++) {
                    if (columns[i].id !== "selector") {
                        var header = _grid.getHeader(columns[i].id);
                        $(header).removeAttr('title');
                        if (columns[i].toolTip) {
                            $(header).attr('tooltip', columns[i].toolTip);
                            $(header).tipsy({
                                fade : true,
                                gravity : columns[i].toolTipGravity ? columns[i].toolTipGravity : 'w',
                                html : true,
                                opacity : 1.0,
                                delayOut : 0,
                                title : 'tooltip'
                            });
                        }

                        _addTipsyToCellNodes(i, columns[i].toolTipFn, columns[i].toolTipGravity);
                    }
                }
            }, 500);
        }

        function _addTipsyToCellNodes(column, toolTipFn, toolTipGravity) {
            for ( var j = 0; j < _grid.getDataLength(); j++) {
                var row = _grid.getDataItem(j);
                if (row) {
                    var node = $(_grid.getCellNode(j, column));
                    node.removeAttr('title');
                    var fnVal = null;
                    if (toolTipFn) {
                    	fnVal = toolTipFn(row);
                    }
                    //use requested tooltip
                    if (fnVal) {
                        node.attr('tooltip', fnVal);
                        node.tipsy({
                            gravity : toolTipGravity || 's',
                            html : true,
                            opacity : 1.0,
                            title : 'tooltip'
                        });
                    } else if (!options.disableAutoTooltips) {
                        //otherwise do the default
                        var cellNode = _grid.getCellNode(j, column);
                        if (cellNode) {
                            if (node.innerWidth() < cellNode.scrollWidth) {
                                var text = $.trim(node.text());
                                if (options.maxToolTipLength && text.length > options.maxToolTipLength) {
                                    text = text.substr(0, options.maxToolTipLength - 3) + "...";
                                }
                                node.attr('tooltip', text);
                                node.tipsy({
                                    gravity : toolTipGravity || 's',
                                    html : true,
                                    opacity : 1.0,
                                    title : 'tooltip'
                                });
                            } else {
                                node.attr('tooltip', '');
                            }
                        }
                    }
                }
            }
        }

        $.extend(this, {
            "init" : init,
            "destroy" : destroy
        });
    }
})(jQuery);
