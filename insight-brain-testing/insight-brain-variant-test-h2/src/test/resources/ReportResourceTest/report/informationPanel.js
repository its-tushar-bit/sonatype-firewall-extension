/**
 * @license Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/pro/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, document, setTimeout, window, Hogan, Slick, Brain, Insight, InsightDatatable, HealthCheck, clearTimeout, demoMode, demoArtifactUrl, reportId, applicationId, freemium, trackEvent */
/*jslint nomen: true, plusplus: true */
(function () {
    "use strict";
	var infoPanelErrorTemplate,
        _defaults = {
            partialDisplay : false,
            sampleData : false,
            byHash : false
        };

	$(document).ready(function () {
		infoPanelErrorTemplate = Hogan.compile($('#infoPanelErrorTemplate').html());
	});

	function InformationPanel(options) {
        var me = this;
        this.options = $.extend(true, {}, _defaults, options);
        this.node = $("<div id='informationPanel' class='informationPanel fullBorderedTable'><div style='width:99%; padding-top: 5px; padding-left: 0.5%; padding-right: 0.5%;'>" +
                        "<a class='close'>&times;</a>" +
                        "<ul class='nav nav-tabs' style='margin-bottom:0px; !important'></ul>" +
                        "<div class='tab-content' style='overflow:hidden'></div>" +
                        "</div></div>");

        //setup the click handler to close the panel
        $('.close', this.node).click(function () {
            me.hide();
        });

        this.plugins = [];
        $.each(Insight.InformationPanelPlugins, function (index, PluginFn) {
            var nav = $('<li data-info-plugin="' + index + '"><a data-target="infoPanel' + index + '" data-toggle="tab"></a></li>'),
                content = $('<div class="tab-pane" id="infoPanel' + index + '"></div>');
            $('ul.nav', me.node).append(nav);
            $('div.tab-content', me.node).append(content);
            me.plugins.push(new PluginFn(content, options));
        });
        $('li:first', this.node).addClass('active');

        //setup the click handler for switching views
        $('a[data-toggle="tab"]', this.node).on('show', function (e) {
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

    InformationPanel.prototype.destroy = function () {
        this.cleanPlugins();
    };

    InformationPanel.prototype.hide = function () {
        this.node.detach();
        this.grid.removeCellCssStyles('popout');
        this.cleanPlugins(); // may not be required
    };

    InformationPanel.prototype.init = function (grid) {
        var me = this;
        this.grid = grid;

        grid.onClick.subscribe(function () {
            me.gridClickedFn.apply(me, arguments);
        });

        //setup data handler to close panel on page/sort/filter
        grid.getData().onRowCountChanged.subscribe(function () {
			//when row count changes, because of filtering, I just don't have a solid way to know if the row previously
			//selected is still in the filtered list, so im just going to hide it
			me.hide();
        });
        grid.getData().onRowsChanged.subscribe(function () {
            me.rowChangedFn.apply(me, arguments);
        });
        $.each(this.plugins, function (pluginIndex, plugin) {
            plugin.setGrid(grid);
        });
    };

    InformationPanel.prototype.cleanPlugins = function () {
        $.each(this.plugins, function (index, plugin) {
            plugin.destroy();
        });
    };

    InformationPanel.prototype.gridClickedFn = function (e, args) {
        var me = this;
        //we only want to handle this when the user clicked on a part of the row that isn't
        //used for something else i.e. checkboxes
        //also has some logic in here to not process if a double click was performed
        if (e.target.tagName !== "INPUT" && $(e.target).children('input:first').length === 0) {
            if (Insight.util.isNotNullOrUndefined(me.timer)) {
                clearTimeout(me.timer);
                me.timer = null;
            } else {
                me.timer = setTimeout(function () {
                    me.timer = null;
                    var currentItem = $.extend({}, me.grid.getDataItem(args.row));

                    $.each(me.plugins, function (index, plugin) {
                        plugin.setItem(currentItem);
                    });

                    me.toggle(e, args);
                }, 250);
            }
        }
    };

    InformationPanel.prototype.rowChangedFn = function (e, args) {
        var row = this.currentRow,
            me = this;

        //do in future to allow proper row to get selected
        setTimeout(function () {
            var rows = me.grid.getSelectedRows();

            if (Insight.util.isNotNullOrUndefined(row) && rows.length === 1) {
                //if the current record has switched rows, need to hide and show again
                if (row !== rows[0]) {
                    me.hide();
                    if (Insight.util.isNotNullOrUndefined(this.currentItemId)) {
                        me.show({row: rows[0], cell: 1}, me.grid.getCanvasNode());
                        //buildView(currentView, me.grid.getData().getItemById(currentItemId));
                    }
                } else {
                    me.position(row);
                }
            } else {
                me.hide();
            }
        }, 10);
    };

    InformationPanel.prototype.toggle = function (event, args) {
        this.grid.removeCellCssStyles('popout');

        if ($(this.node).is(':hidden') || this.currentRow !== args.row) {
            this.show(this.grid.getCellFromEvent(event), $(event.currentTarget));
        } else {
            this.hide();
        }
    };

    InformationPanel.prototype.show = function (cell, target) {
        var cellCss = {},
            cellNode = this.grid.getCellNode(cell.row, cell.cell),
            viewport = $(this.grid.getCanvasNode()).parent(),
            viewportBottom = viewport.offset().top + viewport.height(),
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

        $.each(this.plugins, function (index, plugin) {
            var titleNode = $('li[data-info-plugin=' + index + ']', me.node);
            if (plugin.isVisible()) {
                // update title
                titleNode.show();
                $('a', titleNode).text(plugin.getTitle());
                // update content?
            } else {
                titleNode.hide();
                $('#infoPanel' + index, me.node).hide();
                if (titleNode.hasClass('active')) {
	                titleNode.removeClass('active');
	                $('ul.nav > li:first', me.node).addClass('active');
                }
            }
        });

        if ($('li.active').length === 0) {
            $('li:first', me.node).addClass('active');
        }
        me.plugins[$('li.active', me.node).data('info-plugin')].create();
        $('#infoPanel' + $('li.active', me.node).data('info-plugin'), me.node).show();

        //move the panel into place
        me.node.appendTo(target);
        me.position(cell.row);
        me.node.show();

        this.currentRow = cell.row;
        cellCss[this.currentRow] = {};
        $.each(me.grid.getColumns(), function (index, item) {
            cellCss[me.currentRow][item.id] = 'popout';
        });
        me.grid.setCellCssStyles('popout', cellCss);
    };

    InformationPanel.prototype.position = function (row) {
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
            viewport.scrollTop(viewport.scrollTop() + (this.node.offset().top - cellNode.offsetParent.clientHeight) - viewport.offset().top);
        }
        this.node.css('width', viewport.width() - (viewport.get(0).scrollHeight > viewport.height() ? this.grid.getScrollbarDimensions().width : 0) - /*border*/ 2 + 'px');
    };

	function InformationPanelPlugin() {
	}

	InformationPanelPlugin.prototype.getErrorFn = function (selector, retryFn) {
		return function () {
			var node = $(selector);
			node.html(infoPanelErrorTemplate.render());
			$('.btn', node).click(function () {
				retryFn();
			});
		};
	};
	InformationPanelPlugin.prototype.isVisible = function () {
		return true;
	};
	InformationPanelPlugin.prototype.getTitle = function () {
		return '';
	};
	InformationPanelPlugin.prototype.setItem = function (gav) {
		this.gav = gav;

		if (gav.matchState === 'unknown') {
			if (gav.scanError) {
				this.matchState = 'error';
			} else if (gav.proprietary) {
				this.matchState = 'proprietary';
			} else {
				this.matchState = gav.matchState;
			}
		} else if (gav.matchState) {
			this.matchState = gav.matchState;
			if (gav.proprietary) {
				this.matchState += ' (proprietary)';
			}
		} else {
			//Not necessarily a huge fan of hardcoding exact here, but the sec/lic views only show exact matches
			this.matchState = 'exact';
		}
	};
	InformationPanelPlugin.prototype.message = function (msg) {
        $('.alert', this.node).remove(); // limited space in CIP, don't allow messages to stack
        var msgNode = $('<div class="alert"><button class="close" data-dismiss="alert">&times;</button></div>');
        $('div:first', this.node).prepend(msgNode);
        return msgNode;
    };
    InformationPanelPlugin.prototype.addErrorMessage = function (msg) {
        this.message(msg).addClass('alert-error').append('<strong>Error: </strong>', $('<span></span>').text(msg));
    };
    InformationPanelPlugin.prototype.addSuccessMessage = function (msg) {
        var msgNode = this.message(msg).addClass('alert-success').append($('<span></span>').text(msg));
        setTimeout(function () {
            msgNode.fadeOut('fast', function () {
                msgNode.remove();
            });
        }, 8000);
        return msgNode;
    };
    InformationPanelPlugin.prototype.setGrid = function (grid) {
        this.grid = grid;
    };

	InformationPanelPlugin.prototype.create = $.noop;
	InformationPanelPlugin.prototype.destroy = $.noop;

	// register namespace
	$.extend(true, window, {
		"Insight" : {
			"InformationPanel" : InformationPanel,
			"InformationPanelPlugin" : InformationPanelPlugin,
            "InformationPanelPlugins" : []
		}
	});
}());


/* ArtifactInformation */
(function () {
	"use strict";

    var componentTemplate;
    $(document).ready(function () {
		componentTemplate = Hogan.compile($('#infoPanelComponentTemplate').html());
    });

	function getVersionInfoUrl(currentGav) {
		var baseArtifactUrl = '../artifactDetails/';
		if (demoMode) {
			baseArtifactUrl = demoArtifactUrl;
		}
		return baseArtifactUrl + reportId + '?groupId=' + currentGav.groupId + '&artifactId=' + currentGav.artifactId + '&version=' + currentGav.version + (freemium ? '&freemium=true' : '');
	}

	function renderLicenses(licenses, emptyText) {
		var licenseString = '',
		    escape = $('<div></div>');
		if (Insight.util.isNotNullOrUndefined(licenses) && licenses.length > 0) {
			if (typeof licenses[0] === "object") {
				licenseString = '';
				$.each(licenses, function (index, item) {
					licenseString += ', <span class="license">' + escape.text(item.licenseName).html() + '</span>';
				});
				return licenseString.substring(2);
			} else {
				$.each(licenses, function (index, item) {
					licenses[index] = escape.text(item).html();
				});
				return licenses.join(', ');
			}
		}
		return emptyText || '';
	}

	function declaredLicenseFormat(data) {
		return renderLicenses(data.declaredLicenses).replace('Not Provided', 'Not Declared');
	}

	function observedLicenseFormat(data) {
		return renderLicenses(data.observedLicenses).replace('Not Provided', 'No Sources');
	}

	//  ||  gav !== me.gav
	function createContent(data, node, partialDisplay, matchState) {
		var colorCls = 'artifactInfoSecurity',
			lvl = Math.floor(data.securityThreatLevel);

		if ($(node).is(':hidden')) {
			return;
		}

		if (data.securityThreatCount <= 0) {
			lvl = 'NA';
			colorCls += ' artifactInfoSecurityUnspecified';
		} else if (lvl >= 8) {
			colorCls += ' artifactInfoSecurityCritical';
		} else if (lvl >= 4) {
			colorCls += ' artifactInfoSecuritySevere';
		} else if (lvl >= 0) {
			colorCls += ' artifactInfoSecurityModerate';
		} else {
			lvl = 'Unscored';
			colorCls += ' artifactInfoSecurityModerate';
		}

		data = $.extend(data, {
	        "partial" : partialDisplay,
	        "overriddenLicenses" : renderLicenses(data.overriddenLicenses, '-'),
	        "declaredLicenses" : declaredLicenseFormat(data),
	        "observedLicenses" : observedLicenseFormat(data),
	        "currentMatchState" : matchState,
	        "colorCls" : colorCls,
	        "lvl" : lvl,
	        "cataloged" : HealthCheck.getAge(new Date(data.catalogDate)),
	        "multipleSecurity" : data.securityThreatCount > 1
	    });

	    node.html(componentTemplate.render(data));

		Insight.ComponentInformation({
			data : data,
			partialDisplay : partialDisplay
		});
	}

	function ArtifactInformation(node, options) {
		this.node = node;
		this.options = options;
	}
	ArtifactInformation.prototype = new Insight.InformationPanelPlugin();

	if (Brain.ci && Brain.ci.getArtifactVersionInfoUrl) {
		ArtifactInformation.prototype.create = function () {
			var me = this,
				gav = me.gav,
				graphData = null,
				tableData = null,
				arg = null,
				successFn = function () {
					if (tableData !== null && graphData !== null && gav === me.gav) {
						tableData.securityThreatCount = 0;
						tableData.securityThreatLevel = null;
						$.each(tableData.securityVulnerabilities, function (index, item) {
							if (item.status !== 'Not Applicable') {
								tableData.securityThreatCount++;
								if (Insight.util.isNotNullOrUndefined(item.severity)) {
									tableData.securityThreatLevel = Math.max(tableData.securityThreatLevel, item.severity);
								}
							}
						});
						tableData.versions = graphData;
						createContent(tableData, me.node, me.options.partialDisplay, me.matchState);
					}
			    };

			if (Insight.util.isNullOrUndefined(this.gav.groupId) || Insight.util.isNullOrUndefined(this.gav.artifactId) || Insight.util.isNullOrUndefined(this.gav.version)) {
				this.node.html("<div class='infoPanelText'>Unknown Artifact</div>");
			} else {
				$('div:first', this.node).fadeOut();

				arg = $.extend({}, this.gav, { appId : applicationId, instanceId : instanceId });

				$.getJSON(Insight.toBrain(Brain.ci.getArtifactInfoUrl(arg)), function (data) {
					tableData = data;
					successFn();
				}).error(this.getErrorFn(this.node, this.create));

				$.getJSON(Insight.toBrain(Brain.ci.getArtifactVersionInfoUrl(arg)), function (data) {
					graphData = data;
					successFn();
				}).error(this.getErrorFn(this.node, this.create));
			}
		};
	} else {
		ArtifactInformation.prototype.create = function () {
			var me = this,
			    gav = me.gav;
			if (Insight.util.isNullOrUndefined(this.gav.groupId) || Insight.util.isNullOrUndefined(this.gav.artifactId) || Insight.util.isNullOrUndefined(this.gav.version)) {
				this.node.html("<div class='infoPanelText'>Unknown Artifact</div>");
			} else {
				$('div:first', this.node).fadeOut();
				$.getJSON(getVersionInfoUrl(this.gav), function (data) {
					if (me.gav === gav) {
					    createContent(data, $(me.node), me.options.partialDisplay, me.matchState);
					}
				}).error(this.getErrorFn(this.node, this.create));
			}
		};
	}
	ArtifactInformation.prototype.destroy = function () {
		this.node.empty();
	};

	ArtifactInformation.prototype.getTitle = function () {
		return 'Component Info';
	};

	Insight.InformationPanelPlugins.push(ArtifactInformation);
}());

/* Similar Components */
(function () {
	"use strict";

	function SimilarComponents(node, options) {
		this.node = node;
		this.options = options;
	}
	SimilarComponents.prototype = new Insight.InformationPanelPlugin();

	SimilarComponents.prototype.isVisible = function () {
		return true;
	};

	SimilarComponents.prototype.create = function () {
        var me = this;
		this.destroy();

		if (this.gav.hash) {
			$.getJSON((this.options.sampleData ? 'sample-' : '') + 'partialmatched.json', function (data) {
				//don't bother if there is no data to use
				var found = false,
                    matchDetails = null,
                    i;

				for (i = 0; i < data.aaData.length; i++) {
					if (data.aaData[i].hash === me.gav.hash) {
						matchDetails = data.aaData[i].matchDetails;
						break;
					}
				}
				if (matchDetails) {
					$(me.node).html("<div id='similarityTable' class='borderedTable'></div>");
					InsightDatatable.createSimilarityTable(matchDetails);
				} else {
					$(me.node).html("<div class='infoPanelText'>No similar matches were found for this component.</div>");
				}
			}, this.getErrorFn(me.node, function () {
                me.create();
            }));
		} else {
			$(me.node).html("<div class='infoPanelText'>No similar matches were found for this component.</div>");
		}
	};

	SimilarComponents.prototype.destroy = function () {
		InsightDatatable.removeSimilarityTable();
		this.node.empty();
	};

	SimilarComponents.prototype.getTitle = function () {
		return ((this.matchState && this.matchState.indexOf('similar') >= 0) ? '' : 'No ') + 'Similar Components';
	};

	Insight.InformationPanelPlugins.push(SimilarComponents);
}());

/* File Information */
(function () {
	"use strict";

	function splitPathname(pathname) {
		var idx,
		    file,
		    path;

		if (pathname.indexOf('dependency:/') === 0) {
			pathname = pathname.substring(12);
			idx = pathname.lastIndexOf('/');
			if (idx < 0) {
				return ['Dependency ' + pathname, ''];
			}
			return ['Dependency ' + pathname.substring(idx + 1), 'Module ' + pathname.substring(0, idx)];
		}
		idx = pathname.lastIndexOf('/');

		if (idx === -1) {
			return [pathname, ''];
		}

		file = pathname.substring(idx + 1);
		path = pathname.substring(0, idx);

		return [file, path];
	}

	function FileInfoTab(node, options) {
		this.node = node;
		this.options = options;
	}
	FileInfoTab.prototype = new Insight.InformationPanelPlugin();

	FileInfoTab.prototype.isVisible = function () {
		return !this.options.sampleData;
	};

	FileInfoTab.prototype.buildView = function (fileDetails) {
		if (fileDetails.length > 0) {
			fileDetails.sort();

			var html = "<div class='infoPanelText fileDetail'>";

			//go through each record
		    $.each(fileDetails, function (fileDetailIndex, fileDetail) {
		        $.each(fileDetail.pathnames, function (pathNamesIndex, pathName) {
					var splitpath = splitPathname(pathName);
					html += '<b>' + splitpath[0] + '</b>';
					if (splitpath[1].length > 0) {
						html += ' <i>located at</i> ' + splitpath[1];
					}
					html += '<br><br>';
		        });
		    });

			html += "</div>";
			$(this.node).html(html);
		} else {
			$(this.node).html("<div class='infoPanelText'>Invalid data encountered, no files are associated with this component.</div>");
		}
	};

	FileInfoTab.prototype.create = function () {
		var me = this;
		if (me.options.byHash) {
			//we are dealing with an item from the component view if byHash is true, so we will only show files associated with that single hash
			this.buildView([me.gav]);
		} else {
			//otherwise we are dealing with an item from the security/license view, so we want to show all files associated with any hash that matches (or similar matches) the gav
			$.getJSON('bom.json', function (data) {
				var fileDetails = [];
				$.each(data.aaData, function (index, item) {
					if (item.groupId === me.gav.groupId && item.artifactId === me.gav.artifactId && item.version === me.gav.version) {
						fileDetails.push(item);
					}
				});
				me.buildView(fileDetails);
			}, this.getErrorFn(me.node,  function () {
				me.create();
			}));
		}

	};

	FileInfoTab.prototype.destroy = function () {
	};

	FileInfoTab.prototype.getTitle = function () {
		return 'Referenced Files';
	};

	Insight.InformationPanelPlugins.push(FileInfoTab);
}());

(function () {
	"use strict";

    function load(item, file, property, fn, callback, errorCallback, grid, scope) {
        if (Insight.util.isNotNullOrUndefined(item[property])) {
            fn.call(scope, {
                aaData : grid.getData().getItems()
            }, true);
        } else {
            $.getJSON(file, function (data) {
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

    /* License Editor */
    (function () {
        var licenseEditorTemplate = null;
        $(document).ready(function () {
            licenseEditorTemplate = Hogan.compile($('#infoPanelLicenseEditor').html());
        });

        function LicenseEditorTab(node, options) {
            this.node = node;
            this.options = options;
        }
		LicenseEditorTab.prototype = new Insight.InformationPanelPlugin();

        LicenseEditorTab.prototype.toLicense = function (callback, errorCallback) {
            var me = this;
            load(this.gav, 'licenses.json', 'effectiveLicenseThreat', function (data, active) {
                $.each(data.aaData, function (index, dataItem) {
                    if (dataItem.groupId === me.gav.groupId && dataItem.artifactId === me.gav.artifactId && dataItem.version === me.gav.version) {
                        callback.call(me, dataItem, active);
                        return false;
                    }
                });
            }, callback, errorCallback, this.grid, this);
        };

        LicenseEditorTab.prototype.isVisible = function () {
            return !((freemium && !this.options.sampleData) || this.gav.matchState === 'unknown') && !Insight.isReadOnly();
        };

        LicenseEditorTab.prototype.create = function () {
            this.destroy();
            var timestamp = (new Date()).getTime(),
                container = $('<div id="licenseEditor' + timestamp + '"></div>').appendTo(this.node),
                me = this;

            this.toLicense(
                function (artifact, active) {
                    var options = {
                            "timestamp" : timestamp,
                            "width" : 400,
                            "callback" : function (errorMsg) {
                                trackEvent('component_info_panel', 'update_license');
                                if (Insight.util.isNullOrUndefined(errorMsg)) {
                                    updateComponentTable(me.grid);
                                    me.addSuccessMessage('Updated license for ' + artifact.groupId + ':' + artifact.artifactId + ':' + artifact.version);
                                } else {
                                    me.addErrorMessage(errorMsg);
                                }
                            }
                        },
                        licenseListId = "#licenseList" + options.timestamp,
                        licenseEditor;

                    if (active) {
                        options.dataView = me.grid.getData();
                        options.artifacts = [ me.gav ];
                    } else {
                        options.dataView = new Slick.Data.DataView();
                        artifact.id = 0;
                        options.dataView.setItems([ artifact ]);
                        options.artifacts = [ artifact ];
                    }
                    $(container).html(licenseEditorTemplate.render({ "timestamp" : timestamp }));
                    licenseEditor = new InsightDatatable.LicenseEditor(options);
                    licenseEditor.show('#editor' + timestamp);
                    
                    InsightDatatable.loadLicenseThreats(function (licenseThreats) {
	                    var licenseList = '';
	                    licenseList += '<li class="nav-header">Declared Licenses</li>';
	                    $.each(artifact.declaredLicenses, function (index, item) {
	                        licenseList += '<li>' + InsightDatatable.getLicenseThreatImg(licenseThreats[item]) + ' ' +
	                                        (item === 'Not Provided' ? 'Not Declared' : item) + '</li>';
	                    });
	                    licenseList += '<li class="divider"></li><li class="nav-header">Observed Licenses</li>';
	                    $.each(artifact.observedLicenses, function (index, item) {
	                        licenseList += '<li>' + InsightDatatable.getLicenseThreatImg(licenseThreats[item]) + ' ' +
	                                        (item === 'Not Provided' ? 'No Sources' : item) + '</li>';
	                    });
	                    $(licenseList).appendTo(licenseListId);
                    });
                },
                this.getErrorFn(this.node, function () {
                    me.create();
                })
            );
        };

        LicenseEditorTab.prototype.destroy = function () {
            $('a', this.node).unbind('click');
            $('select', this.node).each(function () {
                $(this).selectmenu('destroy');
            });
            this.node.empty();
        };

        LicenseEditorTab.prototype.getTitle = function () {
            return 'Edit License';
        };
        Insight.InformationPanelPlugins.push(LicenseEditorTab);
    }());

    /* SV Editor */
    (function () {
        var securityEditorTemplate = null;
        $(document).ready(function () {
            securityEditorTemplate = Hogan.compile($('#infoPanelSecurityEditor').html());
        });

        function SvEditorTab(node, options) {
            this.node = node;
			this.options = options;
        }
		SvEditorTab.prototype = new Insight.InformationPanelPlugin();

        SvEditorTab.prototype.toSV = function (callback, errorCallback) {
            var me = this;
            load(this.gav, 'security.json', 'reference', function (data, active) {
                var security = [];
                $.each(data.aaData, function (index, dataItem) {
                    if (dataItem.groupId === me.gav.groupId && dataItem.artifactId === me.gav.artifactId && dataItem.version === me.gav.version) {
                        security.push($.extend({}, dataItem, {
                            id : index
                        }));
                    }
                });
                callback.call(this, security, active);
            }, callback, errorCallback, this.grid, this);
        };

        SvEditorTab.prototype.isVisible = function () {
            return !((freemium && !this.options.sampleData) || this.gav.matchState === 'unknown') && !Insight.isReadOnly();
        };

        SvEditorTab.prototype.create = function () {
            var timestamp = (new Date()).getTime(),
                container = $('<div id="svEditor' + timestamp + '" style="width:100%"></div>').appendTo(this.node),
                me = this,
                svGrid;

            this.toSV(
                function (artifacts, active) {
                    var slickGridId = "sv" + timestamp, svStatusId = "#svStatus" + timestamp,
                        plugin = new Slick.CheckboxSelectColumn(),
                        options = {
                            "timestamp" : timestamp,
                            "width" : 300,
                            "callback" : function (errorMsg) {
                                trackEvent('component_info_panel', 'update_security');
                                if (Insight.util.isNullOrUndefined(errorMsg)) {
                                    updateComponentTable(me.grid);
                                    var items = svGrid.table.getSelectedRows();
                                    if (items.length === 1) {
                                        me.addSuccessMessage('Updated ' + items.length + ' security vulnerability.');
                                    } else if (items.length > 1) {
                                        me.addSuccessMessage('Updated ' + items.length + ' security vulnerabilities.');
                                    }
                                } else {
                                    me.addErrorMessage(errorMsg);
                                }
                            },
                            "artifacts" : []
                        },
                        securityEditor;
                    $(container).html(securityEditorTemplate.render({ "timestamp" : timestamp }));

                    svGrid = new Insight.Table(slickGridId, artifacts, {
                        resizeFn : $.noop,
                        height : '220px',
                        width : '100%',
                        disableFilter : true,
                        selectable : true,
                        dataProcessor : function (data) {
                            var source, reference, i;
                            for (i = 0; i < data.length; i++) {
                                data[i].id = i;
                                data[i]['_score'] = Insight.util.isNullOrUndefined(data[i].score) ? "Unscored" : String(Math.floor(data[i].score));

                                source = data[i].source;
                                reference = data[i].reference;
                                data[i]['_reference'] = (Insight.util.isNullOrUndefined(source) || reference.toUpperCase().indexOf(source.toUpperCase()) === 0) ? reference : source + "-" + reference;
                            }
                            if (active) {
                                setTimeout(function () {
                                    $.each(data, function (dataIndex, dataItem) {
                                        if (dataItem.source === me.gav.source && dataItem.reference === me.gav.reference) {
                                            svGrid.table.setSelectedRows([svGrid.table.getData().getRowById(dataItem.id)]);
                                            return false;
                                        }
                                    });
                                }, 1);
                            }
                            return data;
                        },
                        defaultSort : {
                            columnId : '_score',
                            sortAsc : false
                        },
                        plugins : [ plugin ],
                        columns : [ plugin.getColumnDefinition(), {
                            id : "_score",
                            name : "Threat Level",
                            field : "_score",
                            sortable : true,
                            width : 90,
                            minWidth : 90,
                            toolTip : "'Threat Level' highlights the CVSS (Common Vulnerability Scoring System, version 2) base score for each listed vulnerability.",
                            styleFn : function (row, cell, value, columnDef, dataContext) {
                                return 'nopad';
                            },
                            sortFn : function (dataRow1, dataRow2) {
                                var a = dataRow1['_score'],
                                    b = dataRow2['_score'];
                                if (isNaN(a) === isNaN(b)) {
                                    return ((a < b) ? -1 : ((a > b) ? 1 : 0));
                                }
                                return a.length < b.length ? 1 : -1;
                            },
                            formatter : function (row, cell, value, columnDef, dataContext) {
                                var colorCls;
                                if (value >= 8) {
                                    colorCls = ' criticalScore';
                                } else if (value >= 4) {
                                    colorCls = ' severeScore';
                                } else {
                                    colorCls = ' moderateScore';
                                }
                                return '<div class="' + colorCls + '">' + (value || "&nbsp;") + '</div>';
                            }
                        }, {
                            id : "problemCode",
                            name : "Problem Code",
                            field : "_reference",
                            sortable : true,
                            width : 90,
                            minWidth : 90,
                            formatter : function (row, cell, value, columnDef, dataContext) {
                                var reference = dataContext['_reference'],
                                    url = dataContext.url;
                                if (Insight.util.isNullOrUndefined(reference)) {
                                    return '';
                                }
                                return '<a href="' + url + '" target="_blank">' + reference + '</a>';
                            }
                        }, {
                            id : 'status',
                            name : 'Status',
                            field : 'status',
                            sortable : true,
                            width : 90,
                            minWidth : 90,
                            sortFn : function (a, b) {
                                a = Insight.util.isNotNullOrUndefined(a.status) ? a.status : 'Open';
                                b = Insight.util.isNotNullOrUndefined(b.status) ? b.status : 'Open';
                                return a > b ? 1 : a < b ? -1 : 0;
                            },
                            formatter : function (row, cell, value, columnDef, dataContext) {
                                return Insight.util.isNotNullOrUndefined(value) ? value : 'Open';
                            }
                        } ]
                    });

					$.extend(options, {
						"dataViews" : [svGrid.table.getData()],
						"grid" : svGrid.table
					});

					$('#' + slickGridId + 'Table').data('data-slickgrid', svGrid.table).attr('data-slickgrid', true).click(function (e) {
                        if (e.target.nodeName === 'A') {
                            window.open($(e.target).attr('href'), '_blank');
                        }
                        return false;
                    });

                    if (active) {
                        options.dataViews.push(this.grid.getData());
                    }
                    securityEditor = new InsightDatatable.SecurityEditor(options);
                    securityEditor.show('#editor' + timestamp);
                },
                this.getErrorFn(this.node, function () {
                    me.create();
                })
            );
        };

        SvEditorTab.prototype.destroy = function () {
            $('[data-slickgrid]').each(function () {
                $(this).data('data-slickgrid').destroy();
            });
            this.node.empty();
        };

        SvEditorTab.prototype.getTitle = function () {
            return 'Edit Vulnerabilities';
        };

        Insight.InformationPanelPlugins.push(SvEditorTab);
    }());

}());

/* Labels */
(function () {
	"use strict";

	function LabelTab(node, options) {
		this.node = node;
		this.options = options;
	}
	LabelTab.prototype = new Insight.InformationPanelPlugin();

	LabelTab.prototype.isVisible = function () {
		return !((freemium && !this.options.sampleData) || this.matchState === 'unknown') && Brain.hasFeature('labels');
	};

	LabelTab.prototype.create = function () {
		var timestamp = (new Date()).getTime(),
			container = $('<div id="labels-' + timestamp + '"></div>'),
			me = this,
			retry = function () {
				if (Insight.LabelEditor) {
					Insight.LabelEditor(container, applicationId, me.gav.hash);
				} else {
					setTimeout(retry, 1000);
				}
			};
		this.node.empty();
		container.appendTo(this.node);

		retry();
	};

	LabelTab.prototype.destroy = function () {
		this.node.empty();
	};

	LabelTab.prototype.getTitle = function () {
		return 'Labels';
	};

	Insight.InformationPanelPlugins.push(LabelTab);
}());

/* Policy Violations */
(function () {
	"use strict";

	function PolicyViolationTab(node, options) {
		this.node = node;
		this.options = options;
	}
	PolicyViolationTab.prototype = new Insight.InformationPanelPlugin();
	
	PolicyViolationTab.prototype.isVisible = function () {
        return !((freemium && !this.options.sampleData) || this.matchState === 'unknown') && Brain.hasFeature('policy-violations');
    };

	PolicyViolationTab.prototype.create = function () {
		var timestamp = (new Date()).getTime(),
			container = $('<div id="policy-violations-' + timestamp + '"></div>'),
			me = this,
			retry = function () {
				if (Insight.PolicyViolations) {
					Insight.PolicyViolations(container, applicationId, me.gav.hash);
				} else {
					setTimeout(retry, 1000);
				}
			};
		this.node.empty();
		container.appendTo(this.node);

		retry();
	};

	PolicyViolationTab.prototype.destroy = function () {
		this.node.empty();
	};

	PolicyViolationTab.prototype.getTitle = function () {
		return 'Policy';
	};

	Insight.InformationPanelPlugins.push(PolicyViolationTab);
}());

/* audit */
(function () {
	"use strict";
	function noAuditLog(node) {
		node.html("<div style='width:99%;padding:7px 0 5px 1%;vertical-align:-12px;'>No changes were found for this component.</div>");
	}

	function AuditTab(node, options) {
		this.node = node;
		this.options = options;
	}
	AuditTab.prototype = new Insight.InformationPanelPlugin();

	AuditTab.prototype.isVisible = function () {
		return !((freemium && !this.options.sampleData) || this.gav.matchState === 'unknown');
	};

	AuditTab.prototype.create = function () {
		var baseAuditUrl = '../auditLog/licenses.json+security.json',
			me = this;
		if (demoMode) {
			baseAuditUrl = 'auditLog-' + this.gav.artifactId + '.json';
		}

		if (this.gav.artifactId) {
			$.getJSON(baseAuditUrl, 'key=' + JSON.stringify(this.gav, ['hash', 'groupId', 'artifactId', 'version']), function (data) {
				if (data) {
					me.node.html('<div id="auditTable" class="borderedTable"></div>');
					InsightDatatable.createAuditTable(data.aaData);
				} else {
					noAuditLog(me.node);
				}
			}).error(demoMode ? noAuditLog : this.getErrorFn(this.node, this.create));
		} else {
			noAuditLog(this.node);
		}
	};

	AuditTab.prototype.destroy = function () {
		InsightDatatable.removeAuditTable();
		this.node.empty();
	};

	AuditTab.prototype.getTitle = function () {
		return 'Audit Log';
	};

	Insight.InformationPanelPlugins.push(AuditTab);
}());
