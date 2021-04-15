/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import SlickGridTooltip from './slickgrid/slick.grid.bootstrap.tooltip';

/*global $, window, window, Slick, Insight, setTimeout*/
(function () {
  'use strict';

  var offset = null;
  if ($.browser) {
    if ($.browser.mozilla && parseFloat($.browser.version) < 4) {
      offset = 9;
    } else if ($.browser.msie && parseFloat($.browser.version) < 9) {
      offset = 15;
    }
  }
  if (offset === null) {
    offset = 14;
  }
  function getHeight(node) {
    var windowHeight = $(window).innerHeight(),
      containerTop = $(node).offset().top,
      pagerHeight = $('#' + node.attr('id') + 'Pager').height(),
      val;
    val = Math.max(150, windowHeight - containerTop - offset - pagerHeight);
    return val;
  }

  function closeMessagebox(id) {
    $('#' + id + 'container .message').remove();
  }

  function openMessagebox(id, msg) {
    var container = $('#' + id + 'container'),
      position = $('#' + id + 'Table').position(),
      message;
    if (position !== null) {
      closeMessagebox(id);
      container.append(
        '<div class="message" style="top:' +
          (position.top + 70) +
          'px">' +
          msg +
          '</div>'
      );
      message = $('.message', container);
      message.css(
        'left',
        position.left +
          container.outerWidth() / 2 -
          message.outerWidth() / 2 +
          'px'
      );
    }
  }

  function Table(id, data, options) {
    var groupMetadataProvider = new Slick.Data.GroupItemMetadataProvider(),
      config = $.extend(
        {
          editable: false,
          enableAddRow: false,
          enableCellNavigation: options.enableCellNavigation,
          enableColumnReorder: false,
          enableTextSelectionOnCells: true,
          fullWidthRows: true,
          forceFitColumns: true,
          rowHeight: 31,
          explicitInitialization: true,
          showHeaderRow: !options.disableFilter,
          multiSelect: false,
          autoHeight: options.autoHeight,
          multiColumnSort: options.multiColumnSort,
        },
        options.config || {}
      ),
      me = this,
      tableNode = $('#' + id + 'Table');

    this.options = options;
    this.id = id;
    this.dataView = new Slick.Data.DataView({
      groupItemMetadataProvider: groupMetadataProvider,
    });
    this.filter = new Slick.Filter();

    function postDataLoad(data) {
      if (me.destroyed) {
        return;
      }
      me.table = new Slick.Grid(
        '#' + id + 'Table',
        me.dataView,
        options.columns,
        config
      );

      //make sure this is done first, so any handlers provided will be called first, and can
      //properly stop propogation
      if (options.handlers) {
        for (var i = 0; i < options.handlers.length; i++) {
          me.table[options.handlers[i].event].subscribe(
            options.handlers[i].handler
          );
        }
      }

      if (options.selectable) {
        me.table.setSelectionModel(new Slick.RowSelectionModel());
        me.dataView.syncGridSelection(me.table, true);
      } else {
        // TODO Need to set another selection model to avoid exceptions with info panel
      }

      groupMetadataProvider.init(me.table);

      tableNode.css(
        'height',
        options.height || getHeight($('#' + id + 'Table')) + 'px'
      );
      me.processData(data);
      me.initialize();
    }

    function getErrorFn(reloadCallback) {
      return function (resp, type, msg) {
        var node = tableNode
          .empty()
          .append(Insight.templates.error.render({ message: msg }));
        $('button', node).click(reloadCallback);
      };
    }

    if ($.isArray(data)) {
      postDataLoad({ aaData: data });
    } else {
      window.console.log('Unable to load: ' + data);
    }
  }

  Table.prototype.updateHeight = function () {
    var node = $('#' + this.id + 'Table'),
      me = this;
    node.css('height', getHeight(node) + 'px');

    window.clearTimeout(this.resizeTimeout);

    this.resizeTimeout = window.setTimeout(function () {
      me.table.resizeCanvas();
    }, 50);
  };

  Table.prototype.destroy = function () {
    this.destroyed = true;

    closeMessagebox(this.id);
    $(window).unbind('resize', this.options.resizeFn);

    if (this.table) {
      if (this.table.pager) {
        this.table.pager.destroy();
        this.table.pager = null;
      }
      if (this.table.columnsResizedFn) {
        this.table.onColumnsResized.unsubscribe(this.table.columnsResizedFn);
        this.table.columnsResizedFn = null;
      }
      this.table.destroy();
    }
  };

  Table.prototype.initialize = function () {
    var me = this;
    if (this.initialized) {
      return;
    }
    this.initialized = true;

    if (this.options.defaultSort) {
      if ($.isArray(this.options.defaultSort)) {
        this.table.setSortColumns(this.options.defaultSort);
      } else {
        this.table.setSortColumns([this.options.defaultSort]);
      }
    }

    if ($.isArray(this.options.plugins)) {
      $.each(this.options.plugins, function (index, plugin) {
        me.table.registerPlugin(plugin);
      });
    }

    this.table.registerPlugin(new Slick.Sort());
    this.table.init();
    if (!this.options.disableFilter) {
      this.table.registerPlugin(this.filter);
    }
    this.table.registerPlugin(new SlickGridTooltip());

    if (!this.options.resizeFn) {
      this.options.resizeFn = function () {
        if (Insight.util.isNullOrUndefined(me.options.height)) {
          me.updateHeight();
        }
      };
    }

    $(window).resize(this.options.resizeFn);
    if (this.listeners) {
      $.each(this.listeners, function (key, listener) {
        setTimeout(listener, 0);
      });
      this.listeners = null;
    }
  };

  Table.prototype.processData = function (dd) {
    var me = this;
    this.initialize();
    if (Insight.util.isNotNullOrUndefined(dd)) {
      // Data processing callback
      if (this.options.dataProcessor) {
        dd.aaData = this.options.dataProcessor.call(null, dd.aaData);
      }
      this.dataView.beginUpdate();
      this.dataView.setItems(dd.aaData);
      if (!this.options.disableFilter) {
        if (this.options.externalFilters) {
          this.dataView.setFilter(function (item, args) {
            var valid = true;
            $.each(
              me.options.externalFilters,
              function (index, externalFilter) {
                if (!externalFilter(item, args)) {
                  valid = false;
                }
                return valid;
              }
            );
            return valid && me.filter.getFilter()(item, args);
          });
        } else {
          this.dataView.setFilter(this.filter.getFilter());
        }
      }
      if (this.options.groupInfo) {
        this.dataView.groupBy(
          this.options.groupInfo.field,
          this.options.groupInfo.groupHeaderFn,
          this.options.groupInfo.groupingComparer
        );
      }
      this.dataView.endUpdate();
      if (
        this.options.groupInfo &&
        this.options.groupInfo.initialState === 'collapsed'
      ) {
        //note that we need to do a second update here to collapse the groups, as this isn't possible until the dataview
        //has been updated with the groupInfo set above
        this.dataView.beginUpdate();
        $.each(this.dataView.getGroups(), function (index, group) {
          me.dataView.collapseGroup(group.value);
        });
        this.dataView.endUpdate();
      }
    }

    closeMessagebox(this.id);
    if (Insight.util.isNullOrUndefined(dd) || dd.aaData.length === 0) {
      openMessagebox(this.id, this.options.emptyText || 'None');
    }
    if (!this.options.disablePager) {
      this.table.pager = new Slick.Controls.Pager(
        this.dataView,
        this.table,
        $('#' + this.id + 'TablePager')
      );
    }
  };

  Table.prototype.addMessage = function (message, type, fadeOut) {
    var me = this,
      msgNode = $(
        '<div class="alert"><button class="close" data-dismiss="alert">&times;</button></div>'
      );
    msgNode.addClass(type);
    msgNode.append('<span></span>').text(message);

    msgNode.bind('closed', function () {
      me.updateHeight();
    });

    $('#' + me.id + 'container').prepend(msgNode);
    me.updateHeight();

    if (fadeOut) {
      setTimeout(function () {
        msgNode.fadeOut('fast', function () {
          msgNode.remove();
          me.updateHeight();
        });
      }, fadeOut * 1000);
    }

    return msgNode;
  };

  Table.prototype.addLoadListener = function (listener) {
    if (this.table) {
      setTimeout(listener, 0);
    } else {
      this.listeners = this.listeners || [];
      this.listeners.push(listener);
    }
  };

  Table.prototype.addSuccess = function (message, fadeOut) {
    this.addMessage(message, 'alert-success', fadeOut);
  };

  Table.prototype.addError = function (message, fadeOut) {
    this.addMessage(message, 'alert-error', fadeOut);
  };

  $.extend(true, window, {
    Insight: {
      Table: Table,
    },
  });
})();
