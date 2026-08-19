/**
 * @license Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/pro/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, window, document, window, Slick, PrintTable, Insight, InsightDatatable, setTimeout*/
(function () {
    'use strict';

    var firstRun = true,
        offset = ($.browser && $.browser.mozilla && parseFloat($.browser.version) < 4) ? 60 : 65;
    function getHeight(node) {
		var windowHeight = window.innerHeight || $(document).height(),
			containerTop = $(node).offset().top,
			pagerHeight = $('#' + node.attr('id') + 'Pager').height(),
			val;
        if (firstRun) {
            firstRun = false;
            containerTop += 5;
        }
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
			container.append('<div class="message" style="top:' + (position.top + 70) + 'px">' + msg + '</div>');
			message = $('.message', container);
			message.css('left', (position.left + container.outerWidth() / 2 - message.outerWidth() / 2) + 'px');
        }
    }


    function Table(id, data, options) {
        openMessagebox(id, 'Loading');

        var groupMetadataProvider = new Slick.Data.GroupItemMetadataProvider(),
            config = $.extend({
                editable : false,
                enableAddRow : false,
                enableCellNavigation : options.enableCellNavigation,
                enableColumnReorder : false,
                enableTextSelectionOnCells : true,
                fullWidthRows : true,
                forceFitColumns : true,
                rowHeight : 31,
                explicitInitialization : true,
                showHeaderRow: !options.disableFilter,
                multiSelect: false,
                autoHeight: options.autoHeight,
                multiColumnSort: options.multiColumnSort
            }, options.config || {}),
            me = this;

        this.options = options;
        this.id = id;
        this.dataView = new Slick.Data.DataView({ 'groupItemMetadataProvider' : groupMetadataProvider});
        this.filter = new Slick.Filter();

		if (window.location.search === '?print=true') {
			this.table = new PrintTable.Grid("#" + id + "Table", this.dataView, options.columns, config);
		} else {
			this.table = new Slick.Grid("#" + id + "Table", this.dataView, options.columns, config);
		}

        if (options.selectable) {
            this.table.setSelectionModel(new Slick.RowSelectionModel());
            this.dataView.syncGridSelection(this.table, true);
        } else {
            // TODO Need to set another selection model to avoid exceptions with info panel
        }

        groupMetadataProvider.init(this.table);

        $('#' + id + 'Table').css('height', options.height || (getHeight($('#' + id + 'Table')) + 'px'));

        if (typeof data === 'string') {
            $.getJSON(data).success(function () {
                me.processData.apply(me, arguments);
            }).error(function () {
                openMessagebox(id, 'An Error Occurred');
            });
        } else {
            this.processData({aaData: data});
        }

        this.initialize();
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
        closeMessagebox(this.id);
        $(window).unbind('resize');
        if (this.table.pager) {
            this.table.pager.destroy();
            this.table.pager = null;
        }
        if (this.table.columnsResizedFn) {
            this.table.onColumnsResized.unsubscribe(this.table.columnsResizedFn);
            this.table.columnsResizedFn = null;
        }
        this.table.destroy();
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
        this.table.registerPlugin(new Slick.Tipsy());

        $(window).resize(this.options.resizeFn || function () {
            if (Insight.util.isNullOrUndefined(me.table.height)) {
                me.updateHeight();
            }
        });
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
                        $.each(me.options.externalFilters, function (index, externalFilter) {
                            if (!externalFilter(item, args)) {
                                valid = false;
                            }
                            return valid;
                        });
                        return valid && me.filter.getFilter()(item, args);
                    });
                } else {
                    this.dataView.setFilter(this.filter.getFilter());
                }
            }
            if (this.options.groupInfo) {
                this.dataView.groupBy(this.options.groupInfo.field, this.options.groupInfo.groupHeaderFn, this.options.groupInfo.groupingComparer);
            }
            this.dataView.endUpdate();
            if (this.options.groupInfo && this.options.groupInfo.initialState === 'collapsed') {
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
            this.table.pager = new Slick.Controls.Pager(this.dataView, this.table, $("#" + this.id + "TablePager"));
        }
    };
    
    Table.prototype.addMessage = function (message, type, fadeOut) {
		var me = this,
		    msgNode = $('<div class="alert"><button class="close" data-dismiss="alert">&times;</button></div>');
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

    Table.prototype.addSuccess = function (message, fadeOut) {
		this.addMessage(message, 'alert-success', fadeOut);
    };

	Table.prototype.addError = function (message, fadeOut) {
		this.addMessage(message, 'alert-error', fadeOut);
	};

    $.extend(true, window, {
        'Insight' : {
            'Table' : Table
        }
    });
}());