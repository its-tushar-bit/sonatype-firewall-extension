/**
 * @license Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/pro/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global Insight, InsightDatatable, $, document, window, freemium, Slick, HealthCheck, currentDate, applicationId, reportId, earliestReleaseDate, dateFormat */
/*jslint sub:true, plusplus:true */
(function () {
    "use strict";
	var blue = '#6e99d0',
		grey = '#d9dade',
		orange = '#f7941d',
		yellow = '#fedf15',
		red = '#ee1b24',
		white = '#ffffff',
		licenseColors = [red, orange, yellow, blue, white],
		securityColors = [red, orange, yellow, white],
        licenseThreats,
        cachedComponentThreatLevels = new Array(),
        versionComparison = {
            preReleaseStrings : ["alpha", "beta", "milestone", "rc", "snapshot"],
            isPrerelease : function (val) {
                var i = null;
                val = val.toLowerCase();
                if (Insight.util.isNotNullOrUndefined(val) && (val.length === 1 || !isNaN(parseFloat(val.charAt(1))))) {
                    switch (val.charAt(0)) {
                    case 'a':
                    case 'b':
                    case 'm':
                        return true;
                    }
                }
                for (i = 0; i < this.preReleaseStrings.length; i++) {
                    if (val.substr(0, this.preReleaseStrings[i].length) == this.preReleaseStrings[i]) {
                        return true;
                    }
                }
                return false;
            },
            versionItemCompare : function (a, b) {
                if (Insight.util.isNullOrUndefined(a)) {
                    return -this.versionItemCompare(b, a);
                }
                var aInt = parseInt(a, 10),
                    bInt = parseInt(b, 10);
                if (Insight.util.isNullOrUndefined(b)) {
                    return this.isPrerelease(a) ? -1 : 1;
                } else if (!isNaN(aInt) && !isNaN(bInt)) {
                    return aInt === bInt ? 0 : (aInt > bInt ? 1 : -1);
                } else if (!isNaN(aInt) || !isNaN(bInt)) {
                    return isNaN(aInt) ? (this.isPrerelease(a) ? -1 : 1) : (this.isPrerelease(b) ? 1 : -1);
                } else {
                    return ((a < b) ? -1 : ((a > b) ? 1 : 0));
                }
            },
            compare : function (a, b) {
                var i = null,
                    pattern = /(\w+)/g,
                    aMatches = null,
                    bMatches = null;
                if (Insight.util.isNullOrUndefined(a)) {
                    if (Insight.util.isNullOrUndefined(b)) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (Insight.util.isNullOrUndefined(b)) {
                    return 1;
                }
                aMatches = a.match(pattern);
                bMatches = b.match(pattern);

                for (i = 0; i < Math.max(aMatches.length, bMatches.length); i++) {
                    switch (this.versionItemCompare(aMatches[i], bMatches[i])) {
                    case 1:
                        return 1;
                    case -1:
                        return -1;
                    }
                }
                return 0;
            }
        };
	
	$.getJSON('licensethreats.json').success(function (jsonData) {
        licenseThreats = jsonData.aaData;
    });
	
	function loadLicenseThreats(callback) {
        if (Insight.util.isNotNullOrUndefined(licenseThreats)) {
            callback.apply(null, [ licenseThreats ]);
            return;
        }
        $.getJSON('licensethreats.json').success(function (jsonData) {
            licenseThreats = jsonData.aaData;
            callback.apply(null, [ licenseThreats ]);
        }).error(function (resp, type, msg) {
        	callback.apply(null, [ null,  'Loading license categorization: ' + msg ]);
        });
    }
	
    function getLicenseThreatLevelFromArray() {
    	var threat = null;
    	for (var i = 0; i < arguments.length; i++) {
    		var names = arguments[i];
	    	for (var j = 0; j < names.length; j++) {
	    		var name = names[j];
	    		var nameThreat = licenseThreats[name];
	    		if (Insight.util.isNotNullOrUndefined(nameThreat)) {
	    		    threat = Math.max(nameThreat, threat);
	    		}
	    	}
    	}
    	return threat;
    }
    
    function getLicenseThreatLevelFromComponent(component) {
    	var deprecatedThreat = useDeprecatedLicense(component.effectiveLicenseThreat);
    	if (typeof(deprecatedThreat) !== 'undefined') {
    		return deprecatedThreat;
    	}
    	if (Insight.util.isNotNullOrUndefined(component.overriddenLicenses)) {
    		return getLicenseThreatLevelFromArray(component.overriddenLicenses);
    	}
	    return component.effectiveLicenseThreat;
    }
    
    // Deprecated as of Insight-Brain 1.2. Leave in for compatibility with Insight-Brain 1.1
    var deprecatedLicenseThreats = ["UNKCAT", "LIBERAL", "WEAKCOPYLEFT", "NOT-PROVIDED", "NON-STANDARD", "COPYLEFT"];
    function useDeprecatedLicense(effectiveLicenseThreat) {
    	if (deprecatedLicenseThreats.indexOf(effectiveLicenseThreat) >= 0) {
    		return deprecatedLicenseThreatToEffectiveLicenseThreat(effectiveLicenseThreat);
    	}
    }
    
    function deprecatedLicenseThreatToEffectiveLicenseThreat(effectiveLicenseThreat) {
        switch (effectiveLicenseThreat) {
	        case "COPYLEFT":
	            return 10;
	        case "NON-STANDARD":
	            return 7;
	        case "NOT-PROVIDED":
	            return 6;
	        case "WEAKCOPYLEFT":
	            return 3;
	        case "LIBERAL":
	            return 0;
	        default:
	            return null;
        }
    }
    
	function numberCompare(number1, number2) {
        return number1 - number2;
    }

    function dedupLicenses(licenses1, licenses2) {
        var deduped = [],
            i = null;

        for (i = 0; i < licenses2.length; i++) {
            //Not Provided can be in both lists, but shown with different data
            if ('Not Provided' === licenses2[i] || licenses1.indexOf(licenses2[i]) === -1) {
                deduped.push(licenses2[i]);
            }
        }

        return deduped;
    }

    function renderLicenses(licenses) {
        return Insight.util.isNotNullOrUndefined(licenses) && licenses.length > 0 ? licenses.join(', ') : '';
    }

    function effectiveLicenseSortFn(dataRow1, dataRow2) {
        var x = getLicenseThreatLevelFromComponent(dataRow1),
            y = getLicenseThreatLevelFromComponent(dataRow2);
        x = Insight.util.isNullOrUndefined(x) ? -1 : x;
        y = Insight.util.isNullOrUndefined(y) ? -1 : y;

        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
    }
    
    function getLicenseThreatImg(value) {
    	var deprecatedThreat = useDeprecatedLicense(value);
    	if (typeof(deprecatedThreat) !== 'undefined') {
    		value = deprecatedThreat;
    	}
    	if (Insight.util.isNullOrUndefined(value)) {
    		return '<img src="public/grey.png" alt="UNKCAT"/>';
    	} else if (value > 7) {
			return '<img src="public/red.png" alt="CRITICAL"/>';
		} else if (value > 3) {
			return '<img src="public/orange.png" alt="SEVERE"/>';
		} else if (value > 0) {
			return '<img src="public/yellow.png" alt="MODERATE"/>';
		} else {
			return '<img src="public/blue.png" alt="NO THREAT"/>';
		}
    }

    function effectiveLicenseFormatFn(overriddenLicenses, declaredLicenses, observedLicenses) {
		if (Insight.util.isNotNullOrUndefined(overriddenLicenses)) {
			return renderLicenses(overriddenLicenses);
		} else {
			var declared = renderLicenses(declaredLicenses).replace("Not Provided", "Not Declared"),
				observed = renderLicenses(dedupLicenses(declaredLicenses, observedLicenses)).replace("Not Provided", "No Sources");
			return declared + (observed.length > 0 ? (", " + observed) : "");
		}
    }

    function effectiveLicenseHtmlFormatFn(row, cell, value, columnDef, dataContext) {
		if (Insight.util.isNotNullOrUndefined(dataContext.overriddenLicenses)) {
			return getLicenseThreatImg(getLicenseThreatLevelFromArray(dataContext.overriddenLicenses)) + ' <i>' + renderLicenses(dataContext.overriddenLicenses) + '</i>';
		} else {
			var declared = renderLicenses(dataContext.declaredLicenses).replace("Not Provided", "Not Declared"),
				observed = renderLicenses(dedupLicenses(dataContext.declaredLicenses, dataContext.observedLicenses)).replace("Not Provided", "No Sources");
			return getLicenseThreatImg(getLicenseThreatLevelFromComponent(dataContext)) + " <b>" + declared + "</b>" + (observed.length > 0 ? (", " + observed) : "");
		}
    }

    function message(grid, id, msg) {
        $('#' + id + 'container > .alert-info').remove();

        var msgNode = $('<div class="alert alert-info"></div>');
        $('#' + id + 'container').prepend(msgNode);
        msgNode.append($('<span></span>').html(msg));
        InsightDatatable.updateTableHeight(grid, id);
        return msgNode;
    }

    function closeMessagebox(id) {
        $('#' + id + 'container .message').remove();
    }

    function openMessagebox(id, msg) {
        var container = $('#' +  id + 'container'),
            position = $('#' + id + 'Table').position(),
            message;
        if (position !== null) {
			closeMessagebox(id);
			container.append('<div class="message" style="top:' + (position.top + 70) + 'px">' + msg + '</div>');
			message = $('.message', container);
			message.css('left', (position.left + container.outerWidth() / 2 - message.outerWidth() / 2) + 'px');
        }
    }

    var securityTable = null,
        licenseTable = null,
        componentTable = null,
        similarityTable = null,
        auditTable = null;

	function getActiveTable() {
		return securityTable || licenseTable || componentTable;
	}

	function destroyActiveTable() {
		var table = getActiveTable();
		if (table) {
			table.destroy();
		}
		securityTable = null;
		licenseTable = null;
		componentTable = null;
	}

    function destroyTable(id, table) {
        closeMessagebox(id);
        $(window).unbind('resize');
        if (table.pager) {
            table.pager.destroy();
            table.pager = null;
        }
        if (table.columnsResizedFn) {
            table.onColumnsResized.unsubscribe(table.columnsResizedFn);
            table.columnsResizedFn = null;
        }
        table.destroy();
    }

    function removeSecurityTable() {
        if (Insight.util.isNotNullOrUndefined(securityTable)) {
            destroyTable('security', securityTable);
            securityTable = null;
        }
    }
    function removeLicenseTable() {
        if (Insight.util.isNotNullOrUndefined(licenseTable)) {
            destroyTable('license', licenseTable);
            licenseTable = null;
        }
    }

    function removeComponentTable() {
        if (Insight.util.isNotNullOrUndefined(componentTable)) {
            destroyTable('component', componentTable);
            $('#componentcontainer ul.nav a').unbind('click');
            componentTable = null;
        }
    }
    function removeSimilarityTable() {
        if (Insight.util.isNotNullOrUndefined(similarityTable)) {
            destroyTable('similarity', similarityTable);
            similarityTable = null;
        }
    }
    function removeAuditTable() {
        if (Insight.util.isNotNullOrUndefined(auditTable)) {
            destroyTable('audit', auditTable);
            auditTable = null;
        }
    }

    function createLicenseTable(config) {
        var columns = [{
                id : "_formattedEffectiveLicenseThreat",
                name : "License Threat",
                shortName : 'Licenses',
                field : "_formattedEffectiveLicenseThreat",
                sortable : true,
                width : 200,
                sortFn : effectiveLicenseSortFn,
                formatter : effectiveLicenseHtmlFormatFn
            }, {
                id : 'groupId',
                name : 'Group',
                field : 'groupId',
                sortable : true,
                width : 300,
                cssClass : 'gridcell'
            }, {
                id : 'artifactId',
                name : 'Artifact',
                field : 'artifactId',
                sortable : true,
                width : 300,
                cssClass : 'gridcell'
            }, {
                id : 'version',
                name : 'Version',
                field : 'version',
                sortable : true,
                width : 150,
                sortFn : function (a, b) { return versionComparison.compare(a['version'], b['version']); }
            }, {
                id : 'status',
                name : 'Status',
                field : 'status',
                sortable : true,
                width : 150
            }],
            plugins = [new Insight.InformationPanel({ sampleData: freemium })];

        licenseTable =  new Insight.Table('license', 'licenses.json', {
            columns : columns,
            config : config,
            plugins : plugins,
            defaultSort : { columnId : '_formattedEffectiveLicenseThreat', sortAsc : false },
            dataProcessor : function (data) {
                var i;
                for (i = 0; i < data.length; i++) {
                    data[i]['id'] = i;
                    data[i].status = Insight.util.isNullOrUndefined(data[i].status) ? 'Open' : data[i].status;
                }

                return data;
            }
        });
        licenseTable.table.getData().onRowsChanged.subscribe(function (event, data) {
			var item,
                i;
			data = data.rows;
			for (i = 0; i < data.length; i++) {
				item = licenseTable.table.getData().getItem(data[i]);
				item['_formattedEffectiveLicenseThreat'] = effectiveLicenseFormatFn(item.overriddenLicenses, item.declaredLicenses, item.observedLicenses);
			}
        });

        if (freemium) {
            message(licenseTable.table, 'license', 'The data shown below is sample data. To see the license data for <b>your</b> application, <a href="#" data-action="buypro">purchase a subscription.</a>');
        }

        return licenseTable;
    }

    function createSecurityTable(config) {
        var columnGrouping = new Slick.ColumnGrouping({ columnId : "_score", style : "scoreCol" }),
            scoreStyler = columnGrouping.getCellStyler(),
            cellFormatter = columnGrouping.getCellRenderer(),
            columns = [{
                id: "_score",
                name: "Threat Level",
                shortName : 'Level',
                field: "_score",
                sortable : true,
                width: 120,
                toolTip : "'Threat Level' highlights the CVSS (Common Vulnerability Scoring System, version 2) base score for each listed vulnerability.",
                styleFn : function (row, cell, value, columnDef, dataContext) {
                    return 'nopad ' + scoreStyler(row, cell, value, columnDef, dataContext);
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
                    return '<div class="' + colorCls + '">' + (cellFormatter(row, cell, value, columnDef, dataContext) || "&nbsp;") + '</div>';
                }
            }, {
                id: "problemCode",
                name: "Problem Code",
                shortName : 'Code',
                field: "_reference",
                sortable : true,
                width: 105,
                formatter : function (row, cell, value, columnDef, dataContext) {
                    var reference = dataContext['_reference'],
                        url = dataContext["url"];
                    if (Insight.util.isNullOrUndefined(reference)) {
                        return '';
                    }
                    return '<a href="' + url + '" target="_blank">' + reference + '</a>';
                }
            }, {
                id: "groupId",
                name: "Group",
                field: "groupId",
                sortable : true,
                width: 255
            }, {
                id: "artifactId",
                name: "Artifact",
                field: "artifactId",
                sortable : true,
                width: 250
            }, {
                id: "version",
                name: "Version",
                field: "version",
                sortable : true,
                width: 200,
                sortFn : function (a, b) { return versionComparison.compare(a['version'], b['version']); }
            }, {
                id : 'status',
                name : 'Status',
                field : 'status',
                sortable : true,
                width : 200
            }],
            plugins = [columnGrouping, new Insight.InformationPanel({ sampleData: freemium })];

        securityTable = new Insight.Table('security', 'security.json', {
            columns : columns,
            config : config,
            defaultSort : { columnId : '_score', sortAsc : false },
            dataProcessor : function (data) {
                var source,
                    reference,
                    i;
                for (i = 0; i < data.length; i++) {
                    data[i]['id'] = i;
                    data[i]['_score'] = Insight.util.isNullOrUndefined(data[i]['score']) ? "Unscored" : String(Math.floor(data[i]['score']));

                    source = data[i]['source'];
                    reference = data[i]['reference'];
                    data[i]['_reference'] = (Insight.util.isNullOrUndefined(source) || reference.toUpperCase().indexOf(source.toUpperCase()) === 0) ? reference : source + "-" + reference;

                    data[i].status = Insight.util.isNullOrUndefined(data[i].status) ? 'Open' : data[i].status;
                }
                return data;
            },
            plugins : plugins
        });

        if (freemium) {
            message(securityTable, 'security', 'The data shown below is sample data. To see the security vulnerabilities for <b>your</b> application, <a href="#" data-action="buypro">purchase a subscription.</a>');
        }

        return securityTable;
    }

	function buildComponentTable(bomJsonData, policyJsonData) {
		var matchStates = ['unknown', 'similar', 'exact'],
			columnGrouping = new Slick.ColumnGrouping({ columnId : "threatLevel", style : "scoreCol" }),
			scoreStyler = columnGrouping.getCellStyler(),
			cellFormatter = columnGrouping.getCellRenderer(),
			time = HealthCheck.getAge(earliestReleaseDate, currentDate),
			escape = $('<div/>'),
			columns = [{
				id: "threatLevel",
				name: "Threat Level",
				shortName : 'Level',
				field: "threatLevel",
				sortable : true,
				width: 225,
				styleFn : function (row, cell, value, columnDef, dataContext) {
					return 'nopad ' + scoreStyler(row, cell, value, columnDef, dataContext);
				},
				sortFn : function (dataRow1, dataRow2) {
					var a = dataRow1['threatLevel'].split('-'),
						b = dataRow2['threatLevel'].split('-'),
						levelA = parseInt(a[0], 10),
                        levelB = parseInt(b[0], 10),
						nameA = dataRow1['threatLevel'].substring(a[0].length + 1),
						nameB = dataRow2['threatLevel'].substring(b[0].length + 1);

					if (levelA < levelB) {
						return -1;
					} else if (levelA > levelB) {
						return 1;
					} else if (nameA < nameB) {
						return -1;
					} else if (nameA > nameB) {
						return 1;
					}

					return 0;
				},
				formatter : function (row, cell, value, columnDef, dataContext) {
					var colorCls,
                        threatLevel = parseInt(value.substring(0, value.indexOf('-')), 10),
                        threatName = value.substring(value.indexOf('-') + 1);
					switch (threatLevel) {
					case 0:
						colorCls = ' noScore';
						break;
					case 1:
						colorCls = ' moderateScore';
						break;
					case 2:
						colorCls = ' severeScore';
						break;
					case 3:
						colorCls = ' criticalScore';
						break;
					}
					threatName = escape.text(threatName).html();
					return '<div class="' + colorCls + '">' + (cellFormatter(row, cell, value, columnDef, dataContext).length > 0 ? threatName : '') + '</div>';
				}
			}, {
				id : 'coordinates',
				name : 'Coordinates',
				field : 'coordinates',
				sortable : true,
				width : 295,
				toolTipGravity : 'se',
				toolTipFn : function (row) {
					if (row.modified) {
						return "This record has been manually edited, see the audit log for details";
					}
				},
				formatter : function (row, cell, value, columnDef, dataContext) {
					var result = '';
					if (dataContext.modified) {
						result = '<img src="dirty.gif" style="float:left;margin-top:-2px;margin-left:-6px;"/>';
					} else {
						result = '<div style="width:4px;height:1px;float:left;"></div>';
					}
					if (Insight.util.isNotNullOrUndefined(dataContext.groupId)) {
						result += '<img src="public/coord-gav.png" alt="GAV"/> ';
						result += dataContext.groupId + ' <b>:</b> ' + dataContext.artifactId + ' <b>:</b> ' + dataContext.version;
					} else {
						result += '<img src="public/coord-file.png" alt="File"/> ';
						result += dataContext.filenames.join(', ');
					}
					return result;
				}
			}, {
				id : 'versions',
				name : 'Popularity',
				field : 'relativePopularity',
				sortable : true,
				filterable : false,
				width : 85,
				sortFn : function (dataRow1, dataRow2) {
					if (dataRow1.matchState === matchStates[0]) {
						return dataRow2.matchState === matchStates[0] ? 0 : -1;
					} else if (dataRow2.matchState === matchStates[0]) {
						return 1;
					}
					return ((dataRow1.relativePopularity < dataRow2.relativePopularity) ? -1 : ((dataRow1.relativePopularity > dataRow2.relativePopularity) ? 1 : 0));
				},
				formatter : function (row, cell, value, columnDef, dataContext) {
					if (dataContext.matchState === matchStates[0]) {
						return "";
					}
					var popImg;
					value = Math.round(value * 100);
					if (value > 79) {
						popImg = 'popularity-100.png';
					} else if (value > 59) {
						popImg = 'popularity-80.png';
					} else if (value > 39) {
						popImg = 'popularity-60.png';
					} else if (value > 19) {
						popImg = 'popularity-40.png';
					} else if (value >= 0) {
						popImg = 'popularity-20.png';
					}
					return "<div style='text-align:center;margin-top:-2px;'><img src='" + popImg + "'/></div>";
				}
			}, {
				id : 'age',
				name : 'Age',
				field : 'age',
				sortable : true,
				filterable : false,
				width : 85,
				sortFn : function (dataRow1, dataRow2) {
					var a = dataRow1['age'], b = dataRow2['age'];
					
					if (a === undefined && b === undefined) {
						return 0;
					} else if (a === undefined) {
						return -1;
					} else if (b === undefined) {
						return 1;
					} else if (a < b) {
						return -1;
					} else if (a > b) {
						return 1;
					}
					
					return 0;
				},
				formatter : function (row, cell, value, columnDef, dataContext) {
					if (value === undefined || value === null) {
						return '';
					}
					
					//this is special case where we can end up showing 12.x months since i assume 30 days per month
					//so we decided upon simply showing 1 year in this small window from 360 - 365
					if (value > 359) {
						return (value / 365).toFixed(1) + ' y';
					} else if (value > 30) {
						return Math.floor(value / 30) + ' m';
					}
					
					return Math.floor(value) + ' d';
				}
			}, {
				id : 'releasehistory',
				name : 'Release History',
				field : 'groupId',
				sortable : false,
				filterable : false,
				resizable : false,
				width : 211,
				header : '<div class="release-header"><div class="help" title="<div style=\'background:url(release-tooltip.png);width:241px;height:78px\'></div>"></div></span><span>' + time + '</span></div>',
				formatter : function (row, cell, value, columnDef, dataContext) {
					if (value === null) {
						return '';
					}
					var params = {
							'groupId' : dataContext.groupId,
							'artifactId' : dataContext.artifactId,
							'version' : dataContext.version
						},
						encodedParams = '';
					$.each(params, function (field, content) {
						encodedParams += '&' + encodeURIComponent(field) + '=' + encodeURIComponent(content);
					});
					return '<img src="../brain/rest/report/' + applicationId + '/' + reportId + '/releaseGraph?' + encodedParams.substring(1) + '" alt="Release Popularity">';
				}
			}],
			plugins = [columnGrouping, new Insight.InformationPanel({ partialDisplay : freemium, byHash : true })];

		if (!Brain.hasFeature("release-graph")) {
			columns[1].width += columns.pop().width;
		}

		componentTable = new Insight.Table('component', bomJsonData.aaData, {
			columns : columns,
			multiColumnSort : true,
			selectable : true,
			plugins : plugins,
			defaultSort : [{ columnId : 'threatLevel', sortAsc : false }, { columnId : 'coordinates', sortAsc : true }],
			externalFilters : [function (item, args) {
				if (args && !args.allMatchStates && item.matchState.toLowerCase() !== args.matchState.toLowerCase()) {
					return false;
				}

				return true;
			}],
			dataProcessor : function (data) {
				if (!data) {
					data = [];
				}
				var levelMap = [[0], [1, 2, 3], [4, 5, 6, 7], [8, 9, 10]],
                    i,
                    j,
                    k;
				for (i = 0; i < data.length; i++) {
					data[i]['id'] = i;
					
					//convert the createTime to an age
					if (data[i].createTime) {
						data[i].age = Math.floor((new Date().getTime() - data[i].createTime) / (1000 * 60 * 60 * 24));
					}

					//pull in any policy threats
					for (j = 0; j < policyJsonData.aaData.length; j++) {
						if (policyJsonData.aaData[j].groupId === data[i].groupId
                                && policyJsonData.aaData[j].artifactId === data[i].artifactId
                                && policyJsonData.aaData[j].version === data[i].version) {
							for (k = 0; k < levelMap.length; k++) {
								if ($.inArray(policyJsonData.aaData[j].policyThreatLevel, levelMap[k]) > -1) {
									data[i].threatLevel = k + '-' + policyJsonData.aaData[j].policyName;
									break;
								}
							}
						}
					}

					if (!data[i].threatLevel) {
						data[i].threatLevel = '0-None';
					}

					if (Insight.util.isNotNullOrUndefined(data[i].groupId)) {
						data[i].coordinates = data[i].groupId + ':' + data[i].artifactId + ':' + data[i].version;
					} else {
						data[i].coordinates = data[i].filenames.join(', ');
					}
				}
				return data;
			}
		});

		$('#componentcontainer ul.nav a').click(function (e) {
			e.preventDefault();
			$(this).parents('ul').children('li').removeClass('active');
			$(this).parents('li').addClass('active');

			var text = $(this).text();
			if (text === 'All') {
				componentTable.table.getData().setFilterArgs({ allMatchStates : true });
			} else {
				componentTable.table.getData().setFilterArgs({ matchState : text });
			}
			componentTable.table.getData().refresh();
		});
		// fire current state
		$('#componentcontainer ul.nav li.active a').trigger('click');
		$('.release-header > .help').tipsy({
			gravity : 'e',
			html : true,
			title : 'title',
			opacity : 1.0
		});
	}

    function createComponentTable() {
		var policyJsonData = null,
			bomJsonData = null,
			success = function () {
				if (policyJsonData !== null && bomJsonData !== null) {
					buildComponentTable(bomJsonData, policyJsonData);
				}
			};

		$.getJSON('policythreats.json').success(function (data) {
			policyJsonData = data;
			success();
		}).error(function () {
            openMessagebox('component', 'An error occurred retreiving component policy data');
        });

		$.getJSON('bom.json').success(function (data) {
			bomJsonData = data;
			success();
		}).error(function () {
            openMessagebox('component', 'An error occurred retreiving component data');
        });
    }

    function createSimilarityTable(data) {
        var columns = [];

        if (!freemium) {
            columns.push({
                id : "_formattedEffectiveLicenseThreat",
                name : "License Threat",
                field : "_formattedEffectiveLicenseThreat",
                sortable : true,
                width : 65,
                sortFn : effectiveLicenseSortFn,
                formatter : effectiveLicenseHtmlFormatFn
            }, {
                id : "securityCounters",
                name : "Security",
                field : "securityCounters",
                sortable : true,
                width : 50,
                sortFn : function (dataRow1, dataRow2) {
                    var result = numberCompare(dataRow1['securityCounters']["Critical"], dataRow2['securityCounters']["Critical"]);
                    if (result === 0) {
                        result = numberCompare(dataRow1['securityCounters']["Severe"], dataRow2['securityCounters']["Severe"]);
                    }
                    if (result === 0) {
                        result = numberCompare(dataRow1['securityCounters']["Moderate"], dataRow2['securityCounters']["Moderate"]);
                    }
                    return result;
                },
                formatter : function (row, cell, value, columnDef, dataContext) {
                    var security = dataContext.securityCounters,
                        result = '',
                        total,
                        offset,
                        maxArrows,
                        arrowWidth = 10,
                        offsetCounter = 0,
                        width = columnDef.width - arrowWidth,
                        getSecurityCounterSpan = function (count, color) {
                            var i;
                            for (i = 0; i < count; i++) {
                                if (offsetCounter >= maxArrows) {
                                    break;
                                }
                                result += '<span class="arrow ' + color + '_arrow" style="left:' + (offsetCounter++ * offset) + 'px"></span>';
                            }
                        };

                    if (security) {
                        total = security["Critical"] + security["Severe"] + security["Moderate"];

                        for (offset = 0; offset > -arrowWidth + 1 && (total - 1) * (arrowWidth + offset) + arrowWidth > width; offset--) {
                            // Work is done in the loop description
                        }

                        maxArrows = (width - arrowWidth) / (arrowWidth + offset) + 1;
                        if (security["Critical"]) {
                            getSecurityCounterSpan(security["Critical"], 'red');
                        }
                        if (security["Severe"]) {
                            getSecurityCounterSpan(security["Severe"], 'orange');
                        }
                        if (security["Moderate"]) {
                            getSecurityCounterSpan(security["Moderate"], 'yellow');
                        }
                    }
                    return '<div style="white-space:nowrap;width:' + (offsetCounter > 0 ? 10 + (offsetCounter - 1) * (10 + offset) : 0) + 'px">' + result + "</div>";
                }
            });
        }
        columns.push({
            id : "groupId",
            name : "Group",
            field : "groupId",
            sortable : true,
            width : 90
        }, {
            id : "artifactId",
            name : "Artifact",
            field : "artifactId",
            sortable : true,
            width : 90
        }, {
            id : "version",
            name : "Version",
            field : "version",
            sortable : true,
            width : 30
        });
        similarityTable = new Insight.Table('similarity', data, {
            selectable : true,
            disablePager : true,
            disableFilter : true,
            height : 250,
            columns : columns,
            dataProcessor : function (data) {
                if (!data) {
                    data = [];
                }

                $.each(data, function (index, item) {
                    item.id = index;
                    if (!freemium) {
                        item['_formattedEffectiveLicenseThreat'] =  effectiveLicenseFormatFn(item.overriddenLicenses, item.declaredLicenses, item.observedLicenses);
                    }
                });
                return data;
            }
        });
        similarityTable.table.render(); // No default sort set so this is apparently required
        similarityTable.table.columnsResizedFn = function () {
            var i;
            for (i = 0; i < similarityTable.getDataLength(); i++) {
                similarityTable.updateCell(i, similarityTable.getColumnIndex('securityCounters'));
            }
        };

        similarityTable.table.onColumnsResized.subscribe(similarityTable.table.columnsResizedFn);

        $("#informationPanelPartialMatch").click(function (e) {
            e.stopPropagation();
        });
    }

    function createAuditTable(data) {
        var columns = [{
            id : "date",
            name : "Date",
            field : "date",
            formatter : function (row, cell, val, columnDef, dataContext) {
                return dateFormat(val, "mmm d yyyy, h:MM:ss tt");
            },
            sortable : true,
            width : 30
        }, {
            id : "user",
            name : "User",
            field : "user",
            toolTipGravity : 'n',
            toolTipFn : function (row) {
                return $('<div/>').text("Edited report in build '" + row.where + "' from " + row.ip).html();
            },
            sortable : true,
            width : 20
        }, {
            id : "action",
            name : "Action",
            field : "action",
            sortable : true,
            width : 20
        }, {
            id : "detail",
            name : "Detail",
            field : "detail",
            sortable : true,
            width : 40
        }, {
            id : "comment",
            name : "Comment",
            field : "comment",
            toolTipGravity : 'n',
            toolTipFn : function (row) {
                return $('<div/>').text(row.comment).html();
            },
            sortable : true,
            width : 80
        }];
        auditTable = new Insight.Table('audit', data, {
            resizeFn : $.noop,
            selectable : true,
            disablePager : true,
            disableFilter : true,
            height : 250,
            columns : columns,
            defaultSort : { columnId : 'date', sortAsc : false },
            dataProcessor : function (data) {
                $.each(data, function (index, item) {
                    var action = item.status,
                        detail = "",
                        overriddenLicenses,
                        source,
                        reference;

                    item['id'] = index;
                    item['date'] = new Date(item['time']);
                    switch (item['status']) {
                        case "Open":
                            action = "Reopened";
                            break;
                        case "Not Applicable":
                            action = "Ignored";
                            break;
                        case "Overridden":
                            action = "Overrode";
                            break;
                    }
                    item['action'] = action;

                    switch (item['filename']) {
                        case "licenses.json":
                            detail = "License ";
                            overriddenLicenses = item['overriddenLicenses'];
                            if (Insight.util.isNotNullOrUndefined(overriddenLicenses)) {
                                detail += "as " + renderLicenses(overriddenLicenses);
                            } else {
                                detail += "Analysis";
                            }
                            break;
                        case "security.json":
                            detail = "Vulnerability ";
                            source = item['source'];
                            reference = item['reference'];
                            if (Insight.util.isNotNullOrUndefined(source) && reference.toUpperCase().indexOf(source.toUpperCase()) !== 0) {
                                detail += source + "-";
                            }
                            detail += reference;
                            break;
                    }
                    item['detail'] = detail;
                });
                return data;
            }
        });

        $("#auditTable").click(function (e) {
            e.stopPropagation();
        });
    }

    // register namespace
	$.extend(true, window, {
		"InsightDatatable" : {
			"createAuditTable" : createAuditTable,
			"createComponentTable" : createComponentTable,
			"createLicenseTable" : createLicenseTable,
			"createSecurityTable" : createSecurityTable,
			"createSimilarityTable" : createSimilarityTable,
			"destroyActiveTable" : destroyActiveTable,
			"getActiveTable" : getActiveTable,
			"loadLicenseThreats" : loadLicenseThreats,
			"getLicenseThreatLevelFromArray" : getLicenseThreatLevelFromArray,
			"getLicenseThreatImg" : getLicenseThreatImg,
			"removeAuditTable" : removeAuditTable,
			"removeComponentTable" : removeComponentTable,
			"removeLicenseTable" : removeLicenseTable,
			"removeSecurityTable" : removeSecurityTable,
			"removeSimilarityTable" : removeSimilarityTable,
			"updateTableHeight" : function () {
			    window.alert('update height!');
	        }
		}
	});
}());
