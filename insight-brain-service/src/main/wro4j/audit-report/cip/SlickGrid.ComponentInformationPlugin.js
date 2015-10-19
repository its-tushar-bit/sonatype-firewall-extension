/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
  'use strict';

  function ComponentInformationPanelPlugin($parentScope) {
    this.scope = $parentScope.$new(true);

    var injector = angular.element('*[ng-app]').injector();
    this.selectedComponent = injector.get('SelectedComponent');
    this.compile = injector.get('$compile');
  }

  ComponentInformationPanelPlugin.prototype.init = function (grid) {
    var me = this;
    me.grid = grid;

    grid.onClick.subscribe(function(e, args) {
      me.toggle(args.row);
    });

    //setup data handler to close panel on page/sort/filter
    grid.getData().onRowCountChanged.subscribe(function() {
      //when row count changes, because of filtering, I just don't have a solid way to know if the row previously
      //selected is still in the filtered list, so im just going to hide it
      me.toggle();
    });

    grid.getData().onRowsChanged.subscribe(function() {
      me.rowChangedFn.apply(me, arguments);
    });

    this.node = this.compile('<div component-information-panel/>')(this.scope.$new());
    this.node.appendTo(grid.getCanvasNode());
  };

  ComponentInformationPanelPlugin.prototype.toggle = function (row) {
    var me = this,
        item = typeof row === "number" ? me.grid.getDataItem(row) : undefined;
    me.scope.$applyAsync(function () {
      me.selectedComponent.toggle(item);
    });

    if (typeof row === 'number') {
      me.show({ row : row, cell : 1 });
    }
  };

  ComponentInformationPanelPlugin.prototype.rowChangedFn = function() {
    var me = this;

    //do in future to allow proper row to get selected
    setTimeout(function() {
      var rows = me.grid.getSelectedRows();
      
      if (rows.length === 1) {
        me.toggle(rows[0]);
      }
      else {
        me.toggle();
      }
    }, 10);
  };

  // Moves the grid viewport to view the CIP
  ComponentInformationPanelPlugin.prototype.position = function(row) {
    var node = $('.informationPanel', this.node),
        infopanelTop = node.offset().top,
        infopanelBottom = infopanelTop + node.height(),
        viewport = $(this.grid.getCanvasNode()).parent(),
        viewportTop = viewport.offset().top,
        viewportBottom = viewportTop + viewport.height(),
        cellNode = this.grid.getCellNode(row, 1);

    if (!cellNode) {
      this.grid.scrollRowIntoView(row);
      cellNode = this.grid.getCellNode(row, 1);
    }

    //scroll the div if necessary
    if (infopanelBottom > viewportBottom || infopanelTop < viewportTop) {
      viewport.scrollTop(viewport.scrollTop() + (node.offset().top - cellNode.offsetParent.clientHeight) -
          viewport.offset().top);
    }
    node.css('width', viewport.width() -
        (viewport.get(0).scrollHeight > viewport.height() ? this.grid.getScrollbarDimensions().width : 0) - /*border*/
        2 + 'px');
  };

  // Positions the CIP at the correct row
  ComponentInformationPanelPlugin.prototype.show = function(cell) {
    var cellCss = {},
        cellNode = this.grid.getCellNode(cell.row, cell.cell),
        node = $('.informationPanel', this.node),
        me = this;

    //just in case the row being moved too is out of the cache
    if (!cellNode) {
      this.grid.scrollRowIntoView(cell.row);
      cellNode = this.grid.getCellNode(cell.row, cell.cell);
    }

    //put on bottom of selected row
    node.css('top', cellNode.offsetParent.offsetTop + cellNode.offsetParent.clientHeight);
    node.addClass('shadowBottom');
    node.removeClass('shadowTop');

    //move the panel into place
    me.position(cell.row);
    me.node.show();

    this.currentRow = cell.row;
    cellCss[this.currentRow] = {};
    $.each(me.grid.getColumns(), function(index, item) {
      cellCss[me.currentRow][item.id] = 'popout';
    });
    me.grid.setCellCssStyles('popout', cellCss);
  };

  ComponentInformationPanelPlugin.prototype.destroy = function () {
    if (this.scope) {
      this.scope.$destroy();
    }
  };

  window.ComponentInformationPanelPlugin = ComponentInformationPanelPlugin;
}());
