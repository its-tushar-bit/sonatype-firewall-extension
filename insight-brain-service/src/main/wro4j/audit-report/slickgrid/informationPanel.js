/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, document, setTimeout, window, Hogan, Slick, Brain, Insight, InsightDatatable, clearTimeout */
/*jslint nomen: true, plusplus: true */
(function() {
  'use strict';
  var _defaults = {
    byHash: false
  };

  function InformationPanel(options) {
    var me = this;
    this.options = $.extend(true, {}, _defaults, options);
    this.node = $('<div id="informationPanel" class="informationPanel fullBorderedTable"><div style="width:99%; padding-top: 5px; padding-left: 0.5%; padding-right: 0.5%;">' +
            '<a class="close">&times;</a>' +
            '<ul class="nav nav-tabs" style="margin-bottom:0px; !important"></ul>' +
            '<div class="tab-content" style="overflow:hidden"></div>' +
            '</div></div>');

    //setup the click handler to close the panel
    $('.close', this.node).click(function() {
      me.hide();
    });

    this.plugins = [];

    Insight.InformationPanelPlugins.sort(function(a, b) {
      return a.prototype.priority - b.prototype.priority;
    });

    $.each(Insight.InformationPanelPlugins, function(index, PluginFn) {
      var nav = $('<li data-info-plugin="' + index + '"><a data-target="infoPanel' + index +
              '" data-toggle="tab"></a></li>'),
          content = $('<div class="tab-pane" id="infoPanel' + index + '"></div>');
      $('ul.nav', me.node).append(nav);
      $('div.tab-content', me.node).append(content);
      me.plugins.push(new PluginFn(content, me.options));
    });

    //setup the click handler for switching views
    $('a[data-toggle="tab"]', this.node).on('show', function(e) {
      var pluginId,
          tabContent;
      // Previous Tab - e.relatedTarget
      if (e.relatedTarget !== null) {
        pluginId = $(e.relatedTarget.parentNode).data('info-plugin');
        // TODO we might want to keep these really until row switches
        me.plugins[pluginId].destroy();
        $('#' + $(e.relatedTarget).data('target'), me.node).hide();
      }
      // Active Tab - e.target
      if (e.target !== null) {
        pluginId = $(e.target.parentNode).data('info-plugin');
        tabContent = $('#' + $(e.target).data('target'), me.node);
        tabContent.show();
        me.plugins[pluginId].create(tabContent, me.node);
      }
    });
  }

  InformationPanel.prototype.destroy = function() {
    this.cleanPlugins();
  };

  InformationPanel.prototype.hide = function() {
    this.node.detach();
    this.grid.removeCellCssStyles('popout');
    this.cleanPlugins(); // may not be required
  };

  InformationPanel.prototype.init = function(grid) {
    var me = this;
    this.grid = grid;

    grid.onClick.subscribe(function() {
      me.gridClickedFn.apply(me, arguments);
    });

    //setup data handler to close panel on page/sort/filter
    grid.getData().onRowCountChanged.subscribe(function() {
      //when row count changes, because of filtering, I just don't have a solid way to know if the row previously
      //selected is still in the filtered list, so im just going to hide it
      me.hide();
    });
    grid.getData().onRowsChanged.subscribe(function() {
      me.rowChangedFn.apply(me, arguments);
    });
    $.each(this.plugins, function(pluginIndex, plugin) {
      plugin.setGrid(grid);
    });
  };

  InformationPanel.prototype.cleanPlugins = function() {
    $.each(this.plugins, function(index, plugin) {
      plugin.destroy();
    });
  };

  InformationPanel.prototype.gridClickedFn = function(e, args) {
    var me = this;
    //we only want to handle this when the user clicked on a part of the row that isn't
    //used for something else i.e. checkboxes
    //also has some logic in here to not process if a double click was performed
    if (e.target.tagName !== 'INPUT' && $(e.target).children('input:first').length === 0) {
      if (Insight.util.isNotNullOrUndefined(me.timer)) {
        clearTimeout(me.timer);
        me.timer = null;
      }
      else {
        me.timer = setTimeout(function() {
          me.timer = null;
          $.each(me.plugins, function(index, plugin) {
            plugin.setItem(me.grid.getDataItem(args.row));
          });

          me.toggle(e, args);
        }, 250);
      }
    }
  };

  InformationPanel.prototype.getItem = function() {
    return this.grid.getDataItem(this.currentRow);
  };

  InformationPanel.prototype.updatePanel = function() {
    var me = this,
        activePage = null;
    $.each(this.plugins, function(index, plugin) {
      var titleNode = $('li[data-info-plugin=' + index + ']', me.node);
      if (plugin.isVisible()) {
        // update title
        titleNode.show();
        $('a', titleNode).text(plugin.getTitle());
        // update content?
      }
      else {
        titleNode.hide();
        $('#infoPanel' + index, me.node).hide();
        if (titleNode.hasClass('active')) {
          titleNode.removeClass('active');
          $('ul.nav > li:first', me.node).addClass('active');
        }
      }
    });

    if ($('li.active', me.node).length === 0) {
      $('li', me.node).each(function(index, element) {
        element = $(element);
        if (element.css('display') !== 'none') {
          element.addClass('active');
          return false;
        }
      });
    }
    activePage = $('li.active', me.node).data('info-plugin');
    me.plugins[activePage].create();
    $('#infoPanel' + activePage, me.node).show();
  };

  InformationPanel.prototype.rowChangedFn = function() {

    var row = this.currentRow,
        me = this;

    //do in future to allow proper row to get selected
    setTimeout(function() {
      var rows = me.grid.getSelectedRows(),
          currentItem = null;

      if (Insight.util.isNotNullOrUndefined(row) && rows.length === 1) {
        //if the current record has switched rows, need to hide and show again
        if (row !== rows[0]) {
          me.hide();
          if (Insight.util.isNotNullOrUndefined(this.currentItemId)) {
            me.show({row: rows[0], cell: 1}, me.grid.getCanvasNode());
            //buildView(currentView, me.grid.getData().getItemById(currentItemId));
          }
        }
        else {
          me.position(row);
          currentItem = me.getItem();
          $.each(me.plugins, function(index, plugin) {
            plugin.setItem(currentItem);
          });
          me.updatePanel();
        }
      }
      else {
        me.hide();
      }
    }, 10);
  };

  InformationPanel.prototype.toggle = function(event, args) {
    this.grid.removeCellCssStyles('popout');

    if ($(this.node).is(':hidden') || this.currentRow !== args.row) {
      this.show(this.grid.getCellFromEvent(event), $(event.currentTarget));
    }
    else {
      this.hide();
    }
  };

  InformationPanel.prototype.show = function(cell, target) {
    var cellCss = {},
        cellNode = this.grid.getCellNode(cell.row, cell.cell),
        me = this;

    //just in case the row being moved too is out of the cache
    if (Insight.util.isNullOrUndefined(cellNode)) {
      this.grid.scrollRowIntoView(cell.row);
      cellNode = this.grid.getCellNode(cell.row, cell.cell);
    }

    //put on bottom of selected row
    this.node.css('top', cellNode.offsetParent.offsetTop + cellNode.offsetParent.clientHeight);
    this.node.addClass('shadowBottom');
    this.node.removeClass('shadowTop');

    // Append node to ensure it is in the DOM prior to plugin creation
    me.node.appendTo(target);

    this.updatePanel();

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

  InformationPanel.prototype.position = function(row) {
    var infopanelTop = this.node.offset().top,
        infopanelBottom = infopanelTop + this.node.height(),
        viewport = $(this.grid.getCanvasNode()).parent(),
        viewportTop = viewport.offset().top,
        viewportBottom = viewportTop + viewport.height(),
        cellNode = this.grid.getCellNode(row, 1);

    if (Insight.util.isNullOrUndefined(cellNode)) {
      this.grid.scrollRowIntoView(row);
      cellNode = this.grid.getCellNode(row, 1);
    }

    //scroll the div if necessary
    if (infopanelBottom > viewportBottom || infopanelTop < viewportTop) {
      viewport.scrollTop(viewport.scrollTop() + (this.node.offset().top - cellNode.offsetParent.clientHeight) -
          viewport.offset().top);
    }
    this.node.css('width', viewport.width() -
        (viewport.get(0).scrollHeight > viewport.height() ? this.grid.getScrollbarDimensions().width : 0) - /*border*/
        2 + 'px');
  };

  function InformationPanelPlugin(options) {
    options = options || {};
    this.priority = options.priority || 512; // Arbitrarily high default so tabs are inserted after other tabs but before Audit for backwards compatability
  }

  InformationPanelPlugin.prototype.getErrorFn = function(selector, retryFn, context) {
    return function() {
      var node = $(selector);
      node.html(Insight.templates.error.render());
      $('.btn', node).click(function() {
        retryFn.apply(context, []);
      });
    };
  };
  InformationPanelPlugin.prototype.isVisible = function() {
    return true;
  };
  InformationPanelPlugin.prototype.getTitle = function() {
    return '';
  };
  InformationPanelPlugin.prototype.setItem = function(component) {
    //synthesize the componentIdentifier if necessary
    if (Insight.util.isNullOrUndefined(component.componentIdentifier) &&
        Insight.util.isNotNullOrUndefined(component.groupId) &&
        Insight.util.isNotNullOrUndefined(component.artifactId) &&
        Insight.util.isNotNullOrUndefined(component.version)) {
      component.componentIdentifier = {
        format: 'maven',
        coordinates: {
          groupId: component.groupId,
          artifactId: component.artifactId,
          version: component.version
        }
      };
    }

    this.component = component;
    //simply for clm 1.12.1 and earlier backwards compat
    this.gav = component;
  };
  InformationPanelPlugin.prototype.message = function() {
    $('.alert', this.node).remove(); // limited space in CIP, don't allow messages to stack
    var msgNode = $('<div class="alert" style="width:92.5%"><button class="close" data-dismiss="alert">&times;</button></div>');
    $('div:first', this.node).prepend(msgNode);
    return msgNode;
  };
  InformationPanelPlugin.prototype.addErrorMessage = function(msg) {
    this.message(msg).addClass('alert-error').append('<strong>Error: </strong>', $('<span></span>').text(msg));
  };
  InformationPanelPlugin.prototype.addSuccessMessage = function(msg) {
    var msgNode = this.message(msg).addClass('alert-success').append($('<span></span>').text(msg));
    setTimeout(function() {
      msgNode.fadeOut('fast', function() {
        msgNode.remove();
      });
    }, 8000);
    return msgNode;
  };
  InformationPanelPlugin.prototype.setGrid = function(grid) {
    this.grid = grid;
  };

  InformationPanelPlugin.prototype.create = $.noop;
  InformationPanelPlugin.prototype.destroy = $.noop;

  // register namespace
  $.extend(true, window, {
    'Insight': {
      'InformationPanel': InformationPanel,
      'InformationPanelPlugin': InformationPanelPlugin,
      'InformationPanelPlugins': []
    }
  });
}());

(function() {
  'use strict';

  function load(item, file, property, fn, callback, errorCallback, grid, scope) {
    if (Insight.util.isNotNullOrUndefined(item[property])) {
      fn.call(scope, {
        aaData: grid.getData().getItems()
      }, true);
    }
    else {
      $.getJSON(file,function(data) {
        fn.call(this, data, false);
      }).error(errorCallback);
    }
  }

  function updateComponentTable(grid) {
    var id = $(grid.getCanvasNode()).parents('[data-container=true]').attr('id'),
        items,
        dataView,
        dataItem;
    id = id.substring(0, id.length - 9);

    if (id === 'component') {
      items = grid.getSelectedRows();
      if (items.length === 1) {
        dataView = grid.getData();
        dataView.beginUpdate();

        dataItem = grid.getDataItem(items[0]);
        dataItem.modified = true;
        dataView.updateItem(dataItem.id, dataItem);
        dataView.endUpdate();
      }
    }
  }

  /* SV Editor */
  (function() {
    var securityEditorTemplate = null;
    $(document).ready(function() {
      securityEditorTemplate = Hogan.compile($('#infoPanelSecurityEditor').html());
    });

    function SvEditorTab(node, options) {
      this.node = node;
      this.options = options;
    }

    SvEditorTab.prototype = new Insight.InformationPanelPlugin({ priority: 96 });

    SvEditorTab.prototype.toSV = function(callback, errorCallback) {
      var me = this;
      load(me.component, 'security.json', 'reference', function(data, active) {
        var security = [];
        $.each(data.aaData, function(index, dataItem) {
          if (Insight.util.componentsEqual(dataItem, me.component)) {
            security.push($.extend({}, dataItem, {
              id: index
            }));
          }
        });
        callback.call(this, security, active);
      }, callback, errorCallback, this.grid, this);
    };

    SvEditorTab.prototype.isVisible = function() {
      return this.component.matchState !== 'unknown' && this.component.identificationSource !== 'Manual';
    };

    SvEditorTab.prototype.create = function() {
      this.destroy();
      var timestamp = (new Date()).getTime(),
          container = $('<div id="svEditor' + timestamp + '" style="width:100%"></div>').appendTo(this.node),
          me = this,
          svGrid;

      this.toSV(
          function(artifacts, active) {
            var slickGridId = 'sv' + timestamp,
                plugin = new Slick.CheckboxSelectColumn(),
                options = {
                  'timestamp': timestamp,
                  'width': 300,
                  'callback': function(errorMsg) {
                    if (Insight.util.isNullOrUndefined(errorMsg)) {
                      updateComponentTable(me.grid);
                      var items = svGrid.table.getSelectedRows();
                      if (items.length === 1) {
                        me.addSuccessMessage('Updated ' + items.length + ' security vulnerability.');
                      }
                      else if (items.length > 1) {
                        me.addSuccessMessage('Updated ' + items.length + ' security vulnerabilities.');
                      }
                    }
                    else {
                      me.addErrorMessage(errorMsg);
                    }
                  },
                  'artifacts': []
                },
                securityEditor;
            $(container).html(securityEditorTemplate.render({ 'timestamp': timestamp }));

            var columnDefinitions = [plugin.getColumnDefinition(), {
              id: '_score',
              name: 'Threat Level',
              field: '_score',
              sortable: true,
              width: 90,
              minWidth: 90,
              toolTip: '\'Threat Level\' highlights the CVSS (Common Vulnerability Scoring System, version 2) base score for each listed vulnerability.',
              styleFn: function() {
                return 'nopad';
              },
              sortFn: function(dataRow1, dataRow2) {
                var a = dataRow1['_score'],
                    b = dataRow2['_score'];
                if (!isNaN(a) && !isNaN(b)) {
                  return parseInt(a) - parseInt(b);
                }
                return ((b < a) ? -1 : ((b > a) ? 1 : 0));
              },
              formatter: function(row, cell, value) {
                var colorCls;
                if (value >= 7) {
                  colorCls = ' criticalScore';
                }
                else if (value >= 4) {
                  colorCls = ' severeScore';
                }
                else {
                  colorCls = ' moderateScore';
                }
                return '<div class="' + colorCls + '">' + (value || '&nbsp;') + '</div>';
              }
            }, {
              id: 'problemCode',
              name: 'Problem Code',
              field: '_reference',
              sortable: true,
              width: 90,
              minWidth: 90,
              formatter: Insight.util.showVulnerabilityDetail() ? null : function(row, cell, value, columnDef, dataContext) {
                var reference = dataContext['_reference'],
                    url = dataContext.url;
                if (Insight.util.isNullOrUndefined(reference)) {
                  return '';
                }
                return '<a href="' + url + '" target="_blank">' + reference + '</a>';
              }
            }];

            if (Insight.util.showVulnerabilityDetail()) {
              columnDefinitions.push({
                id: 'info',
                name: 'Info',
                field: 'source',
                sortable: false,
                width: 30,
                minWidth: 30,
                formatter: function(row, cell, value, columnDef, dataContext) {
                  return '<div><button type="button" class="btn vulnerability-info-button" ' +
                    'source="' + dataContext.source + '" ' +
                    'refid="' + dataContext.reference + '"></button></div>';
                }
              });
            }

            columnDefinitions.push({
              id: 'status',
              name: 'Status',
              field: 'status',
              sortable: true,
              width: Insight.util.showVulnerabilityDetail() ? 60 : 90,
              minWidth: Insight.util.showVulnerabilityDetail() ? 60 : 90,
              sortFn: function(a, b) {
                a = Insight.util.isNotNullOrUndefined(a.status) ? a.status : 'Open';
                b = Insight.util.isNotNullOrUndefined(b.status) ? b.status : 'Open';
                return a > b ? 1 : a < b ? -1 : 0;
              },
              formatter: function(row, cell, value) {
                return Insight.util.isNotNullOrUndefined(value) ? value : 'Open';
              }
            });

            svGrid = new Insight.Table(slickGridId, artifacts, {
              resizeFn: $.noop,
              height: '220px',
              width: '100%',
              disableFilter: true,
              selectable: true,
              dataProcessor: function(data) {
                for (var i = 0; i < data.length; i++) {
                  data[i].id = i;
                  data[i]['_score'] = Insight.util.isNullOrUndefined(data[i].score) ? 'Unscored' : String(Math.floor(data[i].score));
                  data[i]['_reference'] = Insight.util.toSvReference(data[i].source, data[i].reference);
                }
                if (active) {
                  setTimeout(function() {
                    $.each(data, function(dataIndex, dataItem) {
                      if (dataItem.source === me.component.source && dataItem.reference === me.component.reference) {
                        svGrid.table.setSelectedRows([svGrid.table.getData().getRowById(dataItem.id)]);
                        return false;
                      }
                    });
                  }, 1);
                }
                return data;
              },
              defaultSort: {
                columnId: '_score',
                sortAsc: false
              },
              plugins: [plugin],
              columns: columnDefinitions
            });

            $.extend(options, {
              'dataViews': [svGrid.table.getData()],
              'grid': svGrid.table
            });

            function infoClickFn(e){
              var el = $(e.target);

              if (el.hasClass('vulnerability-info-button')) {
                Insight.util.showSvModal(el.attr('source'), el.attr('refid'));
              }
              else if (e.target.nodeName === 'A') {
                window.open($(e.target).attr('href'), '_blank');
              }

              return false;
            }

            $('#' + slickGridId + 'Table').data('data-slickgrid', svGrid.table).attr('data-slickgrid',
                    true).click(infoClickFn);

            if (active) {
              options.dataViews.push(this.grid.getData());
            }
            securityEditor = new InsightDatatable.SecurityEditor(options);
            securityEditor.show('#editor' + timestamp);
          },
          this.getErrorFn(this.node, this.create, this)
      );
    };

    SvEditorTab.prototype.destroy = function() {
      $('[data-slickgrid]', this.node).each(function() {
        $(this).data('data-slickgrid').destroy();
      });
      this.node.empty();
    };

    SvEditorTab.prototype.getTitle = function() {
      return 'Edit Vulnerabilities';
    };

    Insight.InformationPanelPlugins.push(SvEditorTab);
  }());

}());
