/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import $ from 'jquery';

// NOTE: originally copied from slick.grid.tipsy.js and then modified to use bootstrap instead of tipsy
export default function SlickGridTooltip(options) {
  if (!jQuery.fn.tooltip) {
    throw new Error('SlickGrid Tooltip plugin requires bootstrap-tooltip module to be loaded');
  }

  let _grid, _headerTimer, _cellTimer;

  const _defaults = {
    maxToolTipLength: null,
    disableAutoTooltips: true,
  };

  Object.assign(this, {
    init(grid) {
      _grid = grid;
      options = $.extend(true, {}, _defaults, options);
      _grid.onColumnsReordered.subscribe(_updateHeaderTooltip);
      _grid.onColumnsResized.subscribe(_updateHeaderTooltip);
      _grid.onViewportChanged.subscribe(_updateCellTooltip);
      _grid.onSelectedRowsChanged.subscribe(_updateCellTooltip);
      _grid.onClick.subscribe(_removeTooltips);
      _updateHeaderTooltip();
    },

    destroy() {
      _grid.onColumnsReordered.unsubscribe(_updateHeaderTooltip);
      _grid.onColumnsResized.unsubscribe(_updateHeaderTooltip);
      _grid.onViewportChanged.unsubscribe(_updateCellTooltip);
      _grid.onSelectedRowsChanged.unsubscribe(_updateCellTooltip);
      _grid.onClick.unsubscribe(_removeTooltips);
      if (_headerTimer) {
        clearTimeout(_headerTimer);
      }
      if (_cellTimer) {
        clearTimeout(_cellTimer);
      }

      _removeTooltips();
      _grid = null;
    },
  });

  /**
   * mapping from "gravity" configuration values (legacy from tipsy tooltips) to bootstrap "placement" settings.
   * gravity can include diagonal directions, like "ne" or "sw".  Map those to just n/s.
   * NOTE: tipsy's gravity settings were counter-intuitive.  A gravity of "e" corresponds to a bootstrap placement
   * of "left".  I guess the concept was that tooltips float away from the gravity direction, like balloons?
   */
  function getPlacement(gravity) {
    const placementMap = {
        s: 'top',
        n: 'bottom',
        w: 'right',
        e: 'left',
      },
      placement = gravity ? placementMap[gravity.charAt(0)] : undefined;

    return placement || 'left';
  }

  function _updateCellTooltip() {
    //i use a timeout here, otherwise seems dom isn't in proper state yet in certain cases
    if (_cellTimer) {
      clearTimeout(_cellTimer);
    }
    _cellTimer = setTimeout(function () {
      var columns = _grid.getColumns();
      for (var i = 0; i < columns.length; i++) {
        if (columns[i].id !== 'selector') {
          var index = i;
          var toolTipFn = columns[i].toolTipFn;
          var toolTipGravity = columns[i].toolTipGravity;
          _addTooltipToCellNodes(index, toolTipFn, getPlacement(toolTipGravity));
        }
      }
    }, 500);
  }

  function createTooltip($element, tooltip, placement) {
    $element.removeAttr('title');
    $element.tooltip({
      title: tooltip,
      placement,
      html: true,
      container: 'body',
    });
  }

  function destroyTooltip($element) {
    $element.tooltip('destroy');
  }

  function _updateHeaderTooltip() {
    //i use a timeout here, otherwise seems dom isn't in proper state yet in certain cases
    if (_headerTimer) {
      clearTimeout(_headerTimer);
    }
    _headerTimer = setTimeout(function () {
      var columns = _grid.getColumns();

      columns.forEach(function (column, i) {
        const placement = getPlacement(column.toolTipGravity);

        if (column.id !== 'selector') {
          // slickgrid gives no decent API for getting its container element, so we need to
          // get one of the children it gives us access to and then traverse up
          const slickGridContainer = $(_grid.getHeaderRow()).parent().parent(),
            headerIndex = _grid.getColumnIndex(column.id),
            header = slickGridContainer.find(
              `> .slick-header > .slick-header-columns > .slick-header-column:nth-child(${headerIndex + 1})`
            );

          if (column.toolTip) {
            createTooltip(header, column.toolTip, placement);
          }

          _addTooltipToCellNodes(i, column.toolTipFn, placement);
        }
      });
    }, 500);
  }

  function _addTooltipToCellNodes(column, toolTipFn, placement) {
    for (var j = 0; j < _grid.getDataLength(); j++) {
      var row = _grid.getDataItem(j);
      if (row) {
        var node = $(_grid.getCellNode(j, column));
        var fnVal = null;
        if (toolTipFn) {
          fnVal = toolTipFn(row);
        }

        //use requested tooltip
        if (fnVal) {
          createTooltip(node, fnVal, placement);
        } else if (!options.disableAutoTooltips) {
          //otherwise do the default
          var cellNode = _grid.getCellNode(j, column);
          if (cellNode) {
            if (node.innerWidth() < cellNode.scrollWidth) {
              var text = $.trim(node.text());
              if (options.maxToolTipLength && text.length > options.maxToolTipLength) {
                text = text.substr(0, options.maxToolTipLength - 3) + '...';
              }

              createTooltip(node, text, placement);
            } else {
              destroyTooltip(node);
            }
          }
        }
      }
    }
  }

  function _removeTooltips() {
    var columns = _grid.getColumns();
    for (var i = 0; i < columns.length; i++) {
      if (columns[i].id !== 'selector') {
        for (var j = 0; j < _grid.getDataLength(); j++) {
          var row = _grid.getDataItem(j);
          if (row) {
            var node = $(_grid.getCellNode(j, i));
            destroyTooltip(node);
          }
        }
      }
    }
  }
}
