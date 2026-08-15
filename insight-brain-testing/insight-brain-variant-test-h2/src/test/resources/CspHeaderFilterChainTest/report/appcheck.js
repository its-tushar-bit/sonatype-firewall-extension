/**
 * @license Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/oss/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, pv, window, navigator, document */
(function() {

	function useGradients() {
		function isIE() {
			return $.browser.msie ? true : false;
		}
		function isFirefox() {
			return $.browser.mozilla ? true : false;
		}
		function ffVersion() {
			return $.browser.version;
		}
		return !(isIE() || (isFirefox() && ffVersion() < 4));
	}

	/* Helper Functions */
	function isNullOrUndefined(obj) {
		return obj === null || typeof obj === 'undefined';
	}

	function isNotNullOrUndefined(obj) {
		return !isNullOrUndefined(obj);
	}

	var blue = '#6e99d0',
		grey = '#d9dade',
		orange = '#f7941d',
		yellow = '#fedf15',
		red = '#ee1b24',
		darkRed = '#b71218',
		darkOrange = '#c67a22',
		darkYellow = '#ddbe18',
		darkBlue = '#6185b7',
		darkGrey = '#84858a',
		bgBlue = '#f7fbfe',
		bgBorder = '#eef2fb',
		textColor = '#575757',
		pillColor = '#cee8fb',
		white = '#ffffff',
		gridLine = "#dee6f3",
		hc_prevLoaded = false, // Used to determine if the gradients have already been appended
		ComponentInformation;

	function getAge(reportDate, endDate) {
		var val,
			unit,
			diff = (endDate ? endDate.getTime() : new Date().getTime()) - reportDate.getTime();
		if (diff > 12 * 30 * 24 * 60 * 60 * 1000) {
			val = diff / (12 * 30 * 24 * 60 * 60 * 1000);
			unit = 'year';
		} else if (diff > 30 * 24 * 60 * 60 * 1000) {
			val = diff / (30 * 24 * 60 * 60 * 1000);
			unit = 'month';
		} else if (diff > 24 * 60 * 60 * 1000) {
			val = diff / (24 * 60 * 60 * 1000);
			unit = 'day';
		} else if (diff > 60 * 60 * 1000) {
			val = diff / (60 * 60 * 1000);
			unit = 'hour';
		} else if (diff > 60 * 1000) {
			val = diff / (60 * 1000);
			unit = 'minute';
		} else {
			return 'moments ago';
		}
		val = Math.floor(val);
		if (val > 1) {
			unit += 's';
		}
		return val + ' ' + unit;
	}

	var _artifactsChartDefaults = {
			height : 50,
			width : 50,
			lineWidth : 1.5,
			innerRadius : 9,
			outerRadius : 21,
			outerRadiusStep : 0,
			showLabels : false,
			fillColors : ['#FDDD03', '#8DC63E'],
			strokeColors : ['#3A983F'],
			textColors : ['white']
	   };
	function artifactsChart(known, config) {
		config = $.extend({}, _artifactsChartDefaults, config);

		config.visTop = config.height / 2;
		config.visLeft = config.width / 2;
		config.h = config.height;
		config.w = config.width;

		this.donutChart([known, 1 - known], config);
	}

	var _licenseChartDefaults = {
			height : 193,
			width : 235,
			innerRadius : 30,
			outerRadius : 60,
			outerRadiusStep : 7,
			fontSize : 16,
			showLabels : true,
			lineWidth : 1
	};
	function licenseChart(data, config) {
		config = $.extend({}, _licenseChartDefaults, config);

		if (Math.max.apply(null, data) !== 0) {
			config.h = config.height;
			config.w = config.width;
			config.visTop = config.h / 2 + 5;
			config.visLeft = config.w / 2  - 3.5;
			config.fillColors = data.length === 3 ? ['#ed1b24', '#ffde15', blue] : ['#ed1b24', '#f89520', '#ffde15', blue];
			config.strokeColors = data.length === 3 ? [darkRed, darkYellow, darkBlue] : [darkRed, darkOrange, darkYellow, darkBlue];
			config.textColors = data.length === 3 ? ['#9d0c11', '#83740d', 'white'] : ['#9d0c11', darkOrange, '#83740d', 'white'];
			this.donutChart(data, config);
		} else {
			var vis = new pv.Panel().width(config.width).height(config.height);

			if (config.element != null) {
				vis.canvas(config.element);
			}

			_createLabel(vis, 'No Licenses Found', { height : config.height, width : config.width });
			vis.render();
		}
	}

	function donutChart(data, config) {
		var total = 0,
			dd = [],
			fillColors = [],
			strokeColors = [],
			textColors = [],
            maxData = -1,
            totalExtra = 0,
			vis,
            wedge,
            maxIndex,
            i,
            extra;

		for (i = 0; i < data.length; i++) {
			if (data[i] > 0) {
			    total += data[i];
			    dd.push(data[i]);
			    fillColors.push(config.fillColors[i % config.fillColors.length]);
			    strokeColors.push(config.strokeColors[i % config.strokeColors.length]);
			    textColors.push(config.textColors[i % config.textColors.length]);
			}
		}
		maxIndex = dd.length - 1;

		for (i = 0; i < dd.length; i++) {
			if (maxData < 0 || dd[i] >= dd[maxData]) {
				maxData = i;
			}
			extra = total * 0.01 - dd[i];
			if (extra > 0) {
				dd[i] = total * 0.01;
				totalExtra += extra;
			}
		}
		dd[maxData] -= totalExtra;

		vis = new pv.Panel()
			.width(config.w)
			.height(config.h);

		if (config.element != null) {
			vis.canvas(config.element);
		}

		wedge = vis.add(pv.Wedge)
			.data(dd)
			.left(config.visLeft)
			.top(config.visTop)
			.outerRadius(function() {return config.outerRadius + (maxIndex - this.index) * config.outerRadiusStep; })
			.innerRadius(config.innerRadius)
			.angle(function(d) {return d / total  * 2 * Math.PI; })
			.fillStyle(pv.colors(fillColors).by(function() {return this.index; }))
			.lineWidth(function(d) { return d > 0 ? config.lineWidth : 0; })
			.strokeStyle(pv.colors(strokeColors).by(function() {return this.index; }));

		if (config.showLabels) {
			wedge.add(pv.Label)
				.left(function() {
					if (this.index === 1 && wedge.angle() < 0.61) {
						return config.visLeft + (this.innerRadius() - 5) * Math.cos(wedge.midAngle());
					}
					return config.visLeft + (this.outerRadius() + 3) * Math.cos(wedge.midAngle());
                })
				.top(function() {
					if (this.index === 1 && wedge.angle() < 0.61) {
						return config.visTop + (this.innerRadius() - 5) * Math.sin(wedge.midAngle());
					}
					return config.visTop + (this.outerRadius() + 3) * Math.sin(wedge.midAngle());
                })
				.font('bold ' + config.fontSize + 'px arial')
				.text(function(d) { return d === 0 || this.index === (dd.length - 1) ? '' : Math.round(d / total * 100) + '%'; })
				.textStyle(pv.colors(textColors).by(function() {return this.index; }))
				.textAlign(function() {
					var angle = wedge.midAngle() < 0 ? Math.PI * 2 + wedge.midAngle() : wedge.midAngle();
					if (this.index === 2 && angle > Math.PI * 1.5) {
						return "left";
					} else if (Math.abs(angle % Math.PI - Math.PI / 2) < 0.31) {
						return "center";
					} else if (this.index === 1 && wedge.angle() < 0.61) {
						return (angle > (Math.PI / 2) && angle < (1.5 * Math.PI)) ? "left" : "right";
					}
					return (angle > (Math.PI / 2) && angle < (1.5 * Math.PI)) ? "right" : "left";
                })
				.textBaseline(function() {
					var angle = wedge.midAngle() < 0 ? Math.PI * 2 + wedge.midAngle() : wedge.midAngle();
					if (Math.abs((angle + Math.PI / 2) % Math.PI - Math.PI / 2) < 0.31) {
						return "middle";
					} else if (this.index === 1 && wedge.angle() < 0.61) {
						return angle > Math.PI ? "top" : "bottom";
					}
					return angle > Math.PI ? "bottom" : "top";
                });
		}
		vis.render();
	}

	var _barChartDefaults = {
			width : 267,
			height : 183,
			fontSize : 9
	   };
	function barChart(data, config) {
		config = $.extend({}, _barChartDefaults, config);

		var maxValue = data[0],
			useGradient = useGradients(),
			stepSize = 0,
			topPadding = 2 * config.fontSize,
			leftPadding = config.fontSize * 5,
			rightPadding = 10,
			y = pv.Scale.ordinal(pv.range(data.length)).splitBanded(0, config.height - topPadding, 3 / 5),
			noSV,
            x,
            i,
            svg,
            vis,
            bar,
            ticks;

		if (hc_prevLoaded !== true) {
			hc_prevLoaded = true;
			svg = document.createElement('div');
			svg.setAttribute('style', 'height:0px;width:0px;');
			svg.innerHTML = "<svg xmlns='http://www.w3.org/2000/svg' version='1.1'><defs><linearGradient id='orangebar' x1='0%' x2='0%' y1='0%' y2='100%'><stop offset='0%' style='stop-color:rgb(199,122,32);stop-opacity:1'></stop><stop offset='100%' style='stop-color:rgb(248,149,32);stop-opacity:1'></stop></linearGradient><linearGradient id='redbar' x1='0%' x2='0%' y1='0%' y2='100%'><stop offset='0%' style='stop-color:rgb(190,31,36);stop-opacity:1'></stop><stop offset='100%' style='stop-color:rgb(235,32,38);stop-opacity:1'></stop></linearGradient><linearGradient id='yellowbar' x1='0%' x2='0%' y1='0%' y2='100%'><stop offset='0%' style='stop-color:rgb(209,186,30);stop-opacity:1'></stop><stop offset='100%' style='stop-color:rgb(255,222,29);stop-opacity:1'></stop></linearGradient></defs></svg>";
			if (document.body.firstChild) {
				document.body.insertBefore(svg, document.body.firstChild);
			} else {
				document.body.appendChild(svg);
			}
		}

		for (i = 1; i < data.length; i++) {
			maxValue = Math.max(maxValue, data[i]);
		}
		noSV = maxValue === 0;

		stepSize = (function() {
			var validSteps = [1,2,5,10,20,25,50,100,200,250,500,1000,2000,2500,5000],
			    maxSteps   = [9,9,9, 9, 8, 8, 8,  8,  6,  6,  6,   5,   5,   4,   4],
				i;
			for (i = 0; i < validSteps.length; i++) {
				if (maxValue/validSteps[i] <= maxSteps[i]) {
					return validSteps[i];
				}
			}
			return validSteps[validSteps.length-1];
		}());
		maxValue = (maxValue % stepSize) === 0 ? maxValue + stepSize : Math.ceil(maxValue/stepSize) * stepSize;
		x = pv.Scale.linear(0, maxValue).range(0, config.width-leftPadding-rightPadding);

		/* The root panel. */
		vis = new pv.Panel()
			.width(config.width)
			.height(config.height);

		if (config.element != null) {
			vis.canvas(config.element);
		}

		vis.add(pv.Panel)
			.width(config.width - leftPadding)
			.height(config.height - topPadding - 5)
			.top(topPadding + 5)
			.left(leftPadding)
			.fillStyle(bgBlue)
			.strokeStyle('#F2F5F8');

		vis.add(pv.Label)
			.text('Threat')
			.top(config.fontSize - 1)
			.left(config.fontSize/2)
			.font('normal ' + config.fontSize + 'px arial')
			.textStyle(textColor)
			.events('all')
			.event('mouseover', pv.Behavior.tipsy({fade: true, gravity: 'w', html: true, opacity: 1.0, offset: 35, title: function() {return 'Sonatype utilizes the base score from the Common Vulnerability Scoring System, version 2.';}}))
			.textAlign('left')
			.textBaseline('middle');

		vis.add(pv.Label)
			.text('Level')
			.top(2 * config.fontSize - 1)
			.left(config.fontSize/2 + 3)
			.font('normal ' + config.fontSize + 'px arial')
			.textStyle(textColor)
			.textAlign('left')
			.textBaseline('middle');

		/* The bars. */
		bar = vis.add(pv.Bar)
			.data(data)
			.top(function() {return topPadding + y(this.index);})
			.text(function(d) {return d;})
			.event('mouseover', pv.Behavior.tipsy({fade: true, gravity: 'w', html: true, opacity: 1.0}))
			.height(y.range().band)
			.left(leftPadding)
			.width(x)
			.fillStyle(function(d) {
				if (this.index < 3) {
					return useGradient ? 'url(#redbar)' : 'rgb(234,32,44)';
				} else if (this.index < 7) {
					return useGradient ? 'url(#orangebar)' : 'rgb(248,149,32)';
				} else {
					return useGradient ? 'url(#yellowbar)' : 'rgb(255,222,29)';
				}
			});

		/* The variable label. */
		bar.anchor("left").add(pv.Label)
			.textAlign("center")
			.left(leftPadding/2)
			.width(leftPadding)
			.font('normal ' + config.fontSize + 'px arial')
			.textStyle(textColor)
			.strokeStyle(textColor)
			.text(function() { return 10 - this.index; });

		/* X-axis ticks. */
		ticks = pv.range(0, maxValue + stepSize, stepSize);
		vis.add(pv.Rule)
			.data(ticks)
			.top(topPadding + 5)
			.left(function(d) {return leftPadding + x(d);})
			.strokeStyle('#f1f4f9')
		.add(pv.Rule)
			.top(topPadding)
			.height(5)
			.strokeStyle(textColor)
			.anchor("top").add(pv.Label)
			.text(x.tickFormat)
			.font('normal ' + config.fontSize + 'px arial')
			.textStyle(textColor);

		if (noSV) {
			_createLabel(vis, 'No Vulnerabilities Found', { width : config.width, height : config.height, rightPadding : rightPadding, leftPadding : leftPadding });
		}
		vis.render();
	}

	function punchCard(data, config) {
		data = data || [];
		config= config || {};

		var excess = [],
			w = 87,
			h = 5 * 32,
			max = -1,
			i,j,root,vis;
		if (data.length > 5) {
			excess = data.splice(5, data.length - 5);
			for (i = 0; i < excess.length; i++) {
				for (j = 0; j < excess[i].length; j++) {
					data[4][j] += excess[i][j];
				}
			}
		}

		$.each(data, function(rowIndex, row){
			$.each(row, function(itemIndex, item){
				max = Math.max(max, item);
			});
		});
		root = new pv.Panel()
			.width(w + 20)
			.height(h + 20);

		if (config.element != null) {
			root.canvas(config.element);
		}

		root.add(pv.Label)
			.text('Dependency Depth')
			.right(0)
			.top(3)
			.font('9px arial')
			.textStyle(textColor)
			.textAlign('right')
			.textBaseline('top');

		for (i = 0; i < 5; i++) {
			root.add(pv.Label)
				.text((i >= 4 && excess.length > 0) ? '5+' : i + 1)
				.font('9px arial')
				.textStyle(textColor)
				.left(3)
				.top(36 + i * 32)
				.textBaseline('middle');
		}

		vis = root.add(pv.Panel)
			.bottom(0)
			.right(0)
			.width(w)
			.height(h)
			.strokeStyle(bgBorder)
			.fillStyle(bgBlue);

		for (i = 1; i < 5; i++) {
			vis.add(pv.Rule)
				.strokeStyle(bgBorder)
				.top(i * 32);
		}

		$.each(data, function(rowIndex, row){
			$.each(row, function(itemIndex, item){
				// This is required otherwise IE8 shows dots for 0s.
				if (item > 0) {
					vis.add(pv.Dot)
						.data([item])
						.left(function(d) { return 16 + itemIndex * 28; })
						.top(16 + rowIndex * 32)
						.size(function(d) { return d/max * 144.0; })
						.fillStyle(function(d){ return itemIndex === 0 ? red : itemIndex === 1 ? orange : yellow; })
						.strokeStyle(function(d){ return itemIndex === 0 ? darkRed : itemIndex === 1 ? darkOrange : darkYellow; });
				}
			});
		});
		
		if ( data.length === 0 ) {
            _createLabel( vis, 'No Components', { height : h, width : w });
		}

		root.render();
	}

	var _createLabelDefaults = {
			fontSize : 9,
			leftPadding : 0,
			rightPadding : 0
	};
	function _createLabel( vis, labelText, config) {
		config = $.extend({}, _createLabelDefaults, config);

		var labelHeight = config.fontSize * 2,
			barWidth =  (labelText.length * config.fontSize * 0.6),
			barTop = config.height/2 - labelHeight/2,
			barStart = config.leftPadding + ((config.width - config.leftPadding - config.rightPadding) - barWidth)/2;

        vis.add(pv.Bar)
            .top(barTop)
            .left(barStart)
            .lineWidth(0)
            .height(labelHeight)
            .width(barWidth)
            .strokeStyle(pillColor)
            .fillStyle(pillColor);
        vis.add(pv.Dot)
            .top(barTop + labelHeight/2)
            .left(barStart)
            .radius(labelHeight/2)
            .lineWidth(0)
            .strokeStyle(pillColor)
            .fillStyle(pillColor);
        vis.add(pv.Dot)
            .top(barTop + labelHeight/2)
            .left(barStart + barWidth)
            .radius(labelHeight/2)
            .lineWidth(0)
            .strokeStyle(pillColor)
            .fillStyle(pillColor);
        vis.add(pv.Label)
            .top(barTop + labelHeight/2)
            .left(barStart + barWidth/2)
            .width(barWidth)
            .font('normal ' + (config.fontSize+1) + 'px arial')
            .textAlign('center')
            .textBaseline('middle')
            .text(labelText);
	}

	ComponentInformation = (function(){
		"use strict";
		var	defaults = {
				partialDisplay: false,
				selectable : false,
				barGap : 3,
				barWidth : 8,
				contentWidth : 375,
				labelTop : 14,
				labelWidth : 100,
				topPadding : 20,
				vizPadding : 6,
				versionClick : $.noop,
				versionDblClick : $.noop
			},
			derivedValues = {
				actualHeight : function (config) {
					return this.height(config) + 20 + this.bottomPadding(config);
				},
				barHeight : function (config) {
					return (config.barWidth + config.barGap) * 3 - 1;
				},
				height : function (config) {
					return config.topPadding + (config.partialDisplay ? 4 : 21) * (config.barWidth + config.barGap);
				},
				bottomPadding : function (config) {
					return config.partialDisplay ? 20 : 0;
				},
				versionCount : function (config) {
					return config.data.versions ? config.data.versions.length : 0;
				},
				contentActualWidth : function (config) {
					return (config.barWidth + config.barGap) * this.versionCount(config);
				},
				width : function (config) {
					var currentIndex = config.data.currentVersionIndex;

					//dont bother doing anything if we already know its 0
					if (currentIndex > 0) {
						//the inner width is twice the size of the area needed for the chart,
						//simply so that the current version can always be directly in the middle
						return Math.max(config.contentWidth, ((config.barWidth + config.barGap) * (Math.max(currentIndex, config.data.versions.length - currentIndex) + 1) - config.barGap) * 2);
					}

					return config.contentWidth;
				},
				panning : function (config) {
					return this.width(config) > config.contentWidth;
				},
				left : function (config) {
					var currentIndex = config.data.currentVersionIndex;

					if (currentIndex < 0) {
						return 0;
					}

					//calculate the point in the inner chart where we need to start drawing, is based off having the current version centered in the chart
					return (this.width(config) / 2) - (((currentIndex * (config.barWidth + config.barGap)) + (config.barWidth / 2)) + (config.barWidth / 2));
				},
				spacer : function (config) {
					return config.barGap + config.barWidth;
				},
				top : function (config) {
					return config.topPadding;
				}
			};

		/* Convert JSON data to be consumed by the graphic */
		function parseJsonData(json) {
			var data = {
				versions: [],
				versionPopularity: [],
				majorRevIndices: [],
				conflictVersions: [],
				effectiveLicenses: [],
				declaredLicenseLevels: [[], [], [], []],
				observedLicenseLevels: [[], [], [], []],
				securityLevels: [[], [], []]
			}, i;

			for (i = 0; i < json.versions.length; i++) {
				data.versions.push(json.versions[i].version);
				data.versionPopularity.push(json.versions[i].popularity);
				if (json.version === json.versions[i].version) {
					data.currentVersionIndex = i;
				}
				if (json.versions[i].majorRevisionStep) {
					data.majorRevIndices.push(i);
				}
				if (json.versions[i].licenseConflict) {
					data.conflictVersions.push(i);
				}
				data.effectiveLicenses.push(json.versions[i].effectiveLicenseThreat);
				if (json.versions[i].declaredLicenseThreats) {
					if ($.inArray("COPYLEFT", json.versions[i].declaredLicenseThreats) !== -1) {
						data.declaredLicenseLevels[0].push(i);
					}
					if ($.inArray("NOT-PROVIDED", json.versions[i].declaredLicenseThreats) !== -1 || $.inArray("NON-STANDARD", json.versions[i].declaredLicenseThreats) !== -1) {
						data.declaredLicenseLevels[1].push(i);
					}
					if ($.inArray("WEAKCOPYLEFT", json.versions[i].declaredLicenseThreats) !== -1) {
						data.declaredLicenseLevels[2].push(i);
					}
					if ($.inArray("LIBERAL", json.versions[i].declaredLicenseThreats) !== -1) {
						data.declaredLicenseLevels[3].push(i);
					}
				}
				if (json.versions[i].observedLicenseThreats) {
					if ($.inArray("COPYLEFT", json.versions[i].observedLicenseThreats) !== -1) {
						data.observedLicenseLevels[0].push(i);
					}
					if ($.inArray("NOT-PROVIDED", json.versions[i].observedLicenseThreats) !== -1 || $.inArray("NON-STANDARD", json.versions[i].observedLicenseThreats) !== -1) {
						data.observedLicenseLevels[1].push(i);
					}
					if ($.inArray("WEAKCOPYLEFT", json.versions[i].observedLicenseThreats) !== -1) {
						data.observedLicenseLevels[2].push(i);
					}
					if ($.inArray("LIBERAL", json.versions[i].observedLicenseThreats) !== -1) {
						data.observedLicenseLevels[3].push(i);
					}
				}
				if (json.versions[i].securityThreats) {
					if ($.inArray("Critical", json.versions[i].securityThreats) !== -1) {
						data.securityLevels[0].push(i);
					}
					if ($.inArray("Severe", json.versions[i].securityThreats) !== -1) {
						data.securityLevels[1].push(i);
					}
					if ($.inArray("Moderate", json.versions[i].securityThreats) !== -1) {
						data.securityLevels[2].push(i);
					}
				}
			}
			return data;
		}

		function getLeftPositionFn(config) {
			//used to calculate the left position of the item
			return function (index) {
				return (config.barGap + config.barWidth) * index + config.barGap;
			};
		}

		function createPanControls(labelViz, contentViz, panWrapper, config) {
			var leftPan = false,
				rightPan = false,
				xIndex = 0,
				pan = function (val) {
					var m = contentViz.transform().translate(val, 0),
						temp = xIndex + val;
					if (temp < 10 && (config.width - config.contentWidth + temp > -10)) {
						xIndex = temp;
						contentViz.transform(m).render();
					} else {
						leftPan = false;
						rightPan = false;
					}
				},
				panLeft = function () {
					if (leftPan) {
						pan(10);
						//we use this as we want to keep panning as long as the user holds the mouse down
						setTimeout(panLeft, 100);
					}
				},
				panRight = function () {
					if (rightPan) {
						pan(-10);
						//we use this as we want to keep panning as long as the user holds the mouse down
						setTimeout(panRight, 100);
					}
				};

			panWrapper.add(pv.Dot).left(config.contentWidth - 7).top(15 + (config.height / 2)).fillStyle(bgBlue).strokeStyle(darkGrey).angle(-Math.PI / 2).shape("triangle").lineWidth(1).size(30).cursor('pointer').events("all").event('mouseover', function () {
				this.fillStyle(darkGrey).render();
			}).event("mouseout", function () {
				this.fillStyle(bgBlue).render();
				rightPan = false;
			}).event("mousedown", function () {
				rightPan = true;
				setTimeout(panRight, 0);
			}).event("mouseup", function () {
				rightPan = false;
			});

			panWrapper.add(pv.Dot).left(7).top(15 + (config.height / 2)).fillStyle(bgBlue).strokeStyle(darkGrey).angle(Math.PI / 2).shape("triangle").lineWidth(1).size(30).cursor('pointer').event('all').events("all").event('mouseover', function () {
				this.fillStyle(darkGrey).render();
			}).event("mouseout", function () {
				this.fillStyle(bgBlue).render();
				leftPan = false;
			}).event("mousedown", function () {
				leftPan = true;
				setTimeout(panLeft, 0);
			}).event("mouseup", function () {
				leftPan = false;
			});

			return pan;
		}

		function createHighlights(vis, config) {
			var inner = vis.add(pv.Panel).def("i", -1),
				bars = inner.add(pv.Bar),
				labels = bars.anchor("bottom").add(pv.Label).visible(false).textBaseline("top"),
				leftPositionFn = getLeftPositionFn(config),
				selectedIndex = null;

			//the highlight sections
			bars.data(config.data.versions).width(config.spacer - 1).left(function () {
				return config.left + leftPositionFn(this.index) - 1;
				//though we don't show the stroke, we need the strokeStyle to catch events within it
			}).top(config.topPadding).height(defaults.partialDisplay ? config.height : config.height - 20).lineWidth(0).strokeStyle(bgBlue).fillStyle("transparent").events("all").event('mouseover', function () {
				inner.i(this.index);
				this.render();
				labels.visible(function (d) {
					return inner.i() === this.index;
				}).textAlign(this.index === 0 ? 'left' : (this.index === config.data.versions.length - 1) ? 'right' : 'center').render();
			}).event("mouseout", function () {
				inner.i(-1);
				this.render();
				labels.visible(false).render();
			}).fillStyle(function(d) {
				if (inner.i() === this.index) {
					return pv.color('rgba(153, 204, 255, 0.5)');
				} else if (this.index === selectedIndex) {
					return pv.color('rgba(10, 10, 10, 0.15)');
				} else {
					return 'transparent';
				}
			}).strokeStyle(function(d) {
				if (this.index === selectedIndex) {
					return pv.color('rgba(10, 10, 10, 0.5)');
				}
				return pv.color('rgba(255, 255, 255,0.1)'); // Shennanigans to ensure Protovis creates invisible elements that have listeners attached
			}).lineWidth(1);
			if (config.selectable) {
				selectedIndex = config.data.currentVersionIndex
				bars.event("click", function() {
					config.versionClick(this.data());
					selectedIndex = this.index;
					this.render();
				});
				bars.event("dblclick", function() {
					config.versionDblClick(this.data());
				});
			}
		}

		function createPopularityPanel(vis, config) {
			var data = config.data,
				maxValue = 1,
				nextMajorRevIndex = data.versionPopularity.length,
				leftPositionFn = getLeftPositionFn(config),
				inner = vis.add(pv.Panel),
				barHeight = config.barHeight - 3;

			inner.width(config.width).height(config.barHeight).top(config.top).left(config.left);

			//find the max value, to create relative sized bars
			$.each(data.versionPopularity, function (index, item) {
				maxValue = Math.max(maxValue, item);
			});
			maxValue = 1 / maxValue;

			//find the next major rev, we will color everything up to that point differently than everything after
			$.each(data.majorRevIndices, function (index, item) {
				if (item > data.currentVersionIndex) {
					nextMajorRevIndex = item;
					return false;
				}
			});

			inner.add(pv.Bar).data(data.versionPopularity).width(config.barWidth).left(function () {
				return leftPositionFn(this.index);
			}).height(function (d) {
				//Note that i use a min height of 3 here as no bar looks silly
				return (barHeight * d * maxValue) + 3;
			}).bottom(0).fillStyle(
	            function (d) {
	                return this.index < data.currentVersionIndex ? "#a8a9ad" : this.index >= nextMajorRevIndex ? "#6d97d0" : this.index === data.currentVersionIndex ? "#58585a"
	                        : "#8bc73e";
	        });

			//the bottom rule just under the bars
			inner.add(pv.Rule).width(config.contentActualWidth + config.barGap).bottom(0).left(0).strokeStyle("#949599");
			config.top += config.barHeight + config.spacer;

			return vis;
		}

		function fillRow(vis, config) {
			var inner = vis.add(pv.Panel).width(config.width).top(config.top).height(config.spacer).left(3);

			inner.add(pv.Bar).top(1).data(config.vGridLines).width(config.barWidth + config.barGap - 1.5).height(config.barWidth + config.barGap - 1.5).left(function () {
				return this.data() - 2;
			}).fillStyle('#edf1f4');
		}

		function createLicenseConflictPanel(vis, config) {
			//TODO: we may be supporting adding a second color circle here for unknown license conflicts
			var inner = vis.add(pv.Panel).width(config.width).top(config.top).height(config.spacer).left(config.left);

			inner.add(pv.Dot).data(config.data.conflictVersions).left(function (d) {
				return config.barWidth / 2 + getLeftPositionFn(config)(d);
			}).radius(config.barWidth / 2).fillStyle(red).strokeStyle(darkRed);

			config.top += config.spacer * 2;

			return vis;
		}

		function createEffectiveLicensePanel(vis, config) {
			var inner = vis.add(pv.Panel).width(config.width).top(config.top).height(config.spacer).left(config.left);

			inner.add(pv.Bar).top(config.barGap / 2).data(config.data.effectiveLicenses).width(config.barWidth).height(config.barWidth).left(function () {
				return getLeftPositionFn(config)(this.index);
			}).fillStyle(function (d) {
				if (d === 'NOT-PROVIDED' || d === 'NON-STANDARD') {
					return orange;
				} else if (d === 'LIBERAL') {
					return blue;
				} else if (d === 'WEAKCOPYLEFT') {
					return yellow;
				} else if (d === 'COPYLEFT') {
					return red;
				} else {
					return grey;
				}
			}).strokeStyle(function (d) {
				if (d === 'NOT-PROVIDED' || d === 'NON-STANDARD') {
					return darkOrange;
				} else if (d === 'LIBERAL') {
					return darkBlue;
				} else if (d === 'WEAKCOPYLEFT') {
					return darkYellow;
				} else if (d === 'COPYLEFT') {
					return darkRed;
				} else {
					return darkGrey;
				}
			});

			config.top += config.spacer * 2;

			return vis;
		}

		function createLicenseLevelPanel(vis, config, data) {
			$.each(data, function (index, item) {
				var offset = (index + 1 >= data.length) ? 0 : 3,
					row = vis.add(pv.Panel).width(config.width).top(config.top).height(config.spacer).left(config.left),
					strokeColor = darkGrey,
					fillColor = grey;

				switch (index) {
					case 0:
						fillColor = red;
						strokeColor = darkRed;
						break;
					case 1:
						fillColor = orange;
						strokeColor = darkOrange;
						break;
					case 2:
						fillColor = yellow;
						strokeColor = darkYellow;
						break;
					case 3:
						fillColor = blue;
						strokeColor = darkBlue;
						break;
				}
				row.add(pv.Dot).data(item).left(function (d) {
					return config.barWidth / 2 + getLeftPositionFn(config)(d);
				}).radius(config.barWidth / 2).fillStyle(fillColor).strokeStyle(strokeColor);

				config.top += config.spacer;
			});
			config.top += config.spacer;

			return vis;
		}

		function createSecurityLevelPanel(vis, config) {
			$.each(config.data.securityLevels, function (index, item) {
				var offset = (index + 1 >= config.data.securityLevels.length) ? 0 : 3,
					row = vis.add(pv.Panel).width(config.width).top(config.top).height(config.spacer).left(config.left),
					strokeColor = darkGrey,
					fillColor = grey;

				switch (index) {
					case 0:
						fillColor = red;
						strokeColor = darkRed;
						break;
					case 1:
						fillColor = orange;
						strokeColor = darkOrange;
						break;
					case 2:
						fillColor = yellow;
						strokeColor = darkYellow;
						break;
				}
				row.add(pv.Dot).data(item).left(function (d) {
					return config.barWidth / 2 + getLeftPositionFn(config)(d) + 1;
				}).radius(config.barWidth / 2 - 0.5).fillStyle(fillColor).strokeStyle(strokeColor).angle(-Math.PI / 2).shape("triangle");

				config.top += config.spacer;
			});

			return vis;
		}

		function loadVersionChart(config) {
			var gridLines = [],
				panWrapper,
				panningFn,
				vizLabels,
				vizContent,
	            i;

			config = $.extend({}, defaults, config);

			$.each(derivedValues, function (name, fn) {
				config[name] = fn.call(derivedValues, config);
			});

			$('#aiVersionChartContainer').height(config.actualHeight);

			//create the main viz container with the blue background
			vizLabels = new pv.Panel().canvas('aiVersionChartLabels').height(config.actualHeight).width(config.labelWidth).fillStyle(bgBlue).strokeStyle(bgBlue);

			//create the inner panel
			vizContent = new pv.Panel().canvas('aiVersionChartViz').overflow('hidden').height(config.actualHeight).width(config.contentWidth).fillStyle(bgBlue).strokeStyle(bgBlue);

			if (config.panning) {
				panWrapper = vizContent;
				vizContent = panWrapper.add(pv.Panel).overflow('hidden').height(config.actualHeight).width(config.contentWidth - 30).fillStyle(bgBlue).strokeStyle(bgBlue).left(15);
			}

			// Horizontal gridlines
			for (i = 0; i <= config.spacer * (config.partialDisplay ? 4 : 21); i += config.spacer) {
				gridLines.push(i);
			}
			vizContent.add(pv.Rule)
				.data(gridLines)
				.left(0)
				.width(config.width)
				.top(function (d) { return d + config.topPadding; })
				.height(1)
				.strokeStyle(gridLine)
				.strokeDasharray('1 1');

			// Vertical gridlines
			config.vGridLines = [0];

			for (i = ((config.width / 2) - 1) % config.spacer - config.spacer/2 - 0.5; i < config.width; i += config.spacer ) {
				config.vGridLines.push(i);
			}
			vizContent.add(pv.Rule)
				.data(config.vGridLines)
				.left(function (d) {
					return d;
				})
				.height(config.height - config.topPadding)
				.top(config.top)
				.strokeStyle(gridLine)
				.strokeDasharray('1 1');

			$.each(config.partialDisplay ? [] : [5, 7, 9, 10, 11, 12, 14, 15, 16, 17, 19, 20, 21], function (index, row) {
				fillRow(vizContent, $.extend({}, config, { top: config.top + (row - 1) * config.spacer }));
			});

			//the current version vertical rule
			vizContent.add(pv.Rule).left((config.width / 2) - 1).top(config.topPadding).height(config.height - config.topPadding).strokeStyle("#949599");

			//put in the version labels
			vizContent.add(pv.Label).left((config.width / 2) - 70).top(15).textAlign("center").text("Older");
			vizContent.add(pv.Label).left(config.width / 2).top(15).textAlign("center").text("This Version");
			vizContent.add(pv.Label).left((config.width / 2) + 70).top(15).textAlign("center").text("Newer");

			vizLabels.add(pv.Label).left(0).top(config.top + config.labelTop).textAlign("left").text("Popularity");
			config.top += 1;
			createPopularityPanel(vizContent, config);

			//these values are all hidden unless paid for
			if (!config.partialDisplay) {
				vizLabels.add(pv.Label).left(0).top(config.top + 10).textAlign("left").text("License Conflict");
				createLicenseConflictPanel(vizContent, config);

				vizLabels.add(pv.Label).left(0).top(config.top + 10).textAlign("left").text("Effective Threat");
				createEffectiveLicensePanel(vizContent, config);

				vizLabels.add(pv.Label).left(0).top(config.top + 10).textAlign("left").text("Declared Licenses");
				createLicenseLevelPanel(vizContent, config, config.data.declaredLicenseLevels);

				vizLabels.add(pv.Label).left(0).top(config.top + 10).textAlign("left").text("Observed Licenses");
				createLicenseLevelPanel(vizContent, config, config.data.observedLicenseLevels);

				vizLabels.add(pv.Label).left(0).top(config.top + 10).textAlign("left").text("Security Alerts");
				createSecurityLevelPanel(vizContent, config);
			}

			createHighlights(vizContent, config);

			if (config.panning) {
				//add in the panning controls
				panningFn = createPanControls(vizLabels, vizContent, panWrapper, config);
				config.contentWidth -= 15;
			}

			vizLabels.render();
			vizContent.render();

			//automatically pan to the center current version of the dataset
			if (config.panning) {
				//here we need to move the panel so that the center is centered in the viewable panel
				panningFn(-((config.width / 2) - config.contentWidth + (config.contentWidth / 2)));
			}
		}

		return function(config) {
			config.data = parseJsonData(config.data);
			loadVersionChart(config);
		};
	}());

	$.extend(true, window, {
		"HealthCheck" : {
			"getAge" : getAge,
			"artifactsChart" : artifactsChart,
			"licenseChart" : licenseChart,
			"donutChart" : donutChart,
			"barChart" : barChart,
			"punchCard" : punchCard
		},
		"Insight": {
			"ComponentInformation": ComponentInformation
		}
	});
}());