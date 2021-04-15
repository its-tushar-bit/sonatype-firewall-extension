/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, pv, window, document */
(function () {
  'use strict';

  var blue = '#006bbf',
    orange = '#f4861d',
    yellow = '#f5c648',
    red = '#bc012f',
    darkRed = '#bc012f',
    darkOrange = '#f4861d',
    darkYellow = '#f5c648',
    darkBlue = '#006bbf',
    bgBlue = '#f7fbfe',
    bgBorder = '#eef2fb',
    textColor = '#575757',
    pillColor = '#cee8fb';

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
      return 'some seconds';
    }
    val = Math.floor(val);
    if (val > 1) {
      unit += 's';
    }
    return val + ' ' + unit;
  }

  var _artifactsChartDefaults = {
    height: 50,
    width: 50,
    lineWidth: 1.5,
    innerRadius: 9,
    outerRadius: 21,
    outerRadiusStep: 0,
    showLabels: false,
    fillColors: ['#FDDD03', '#8DC63E'],
    strokeColors: ['#3A983F'],
    textColors: ['white'],
  };

  function artifactsChart(known, config) {
    config = $.extend({}, _artifactsChartDefaults, config);

    config.visTop = config.height / 2;
    config.visLeft = config.width / 2;
    config.h = config.height;
    config.w = config.width;

    donutChart([known, 1 - known], config);
  }

  var _licenseChartDefaults = {
    height: 193,
    width: 235,
    innerRadius: 30,
    outerRadius: 60,
    outerRadiusStep: 7,
    fontSize: 16,
    showLabels: true,
    lineWidth: 1,
  };

  function licenseChart(data, config) {
    config = $.extend({}, _licenseChartDefaults, config);

    if (Math.max.apply(null, data) !== 0) {
      config.h = config.height;
      config.w = config.width;
      config.visTop = config.h / 2 + 5;
      config.visLeft = config.w / 2 - 3.5;
      config.fillColors = data.length === 3 ? [red, yellow, blue] : [red, orange, yellow, blue];
      config.strokeColors =
        data.length === 3 ? [darkRed, darkYellow, darkBlue] : [darkRed, darkOrange, darkYellow, darkBlue];
      config.textColors =
        data.length === 3 ? ['#9d0c11', '#83740d', 'white'] : ['#9d0c11', darkOrange, '#83740d', 'white'];
      donutChart(data, config);
    } else {
      var vis = new pv.Panel().width(config.width).height(config.height);

      if (config.element) {
        vis.canvas(config.element);
      }

      _createLabel(vis, 'No Licenses Found', {
        height: config.height,
        width: config.width,
      });
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

    vis = new pv.Panel().width(config.w).height(config.h);

    if (config.element) {
      vis.canvas(config.element);
    }

    wedge = vis
      .add(pv.Wedge)
      .data(dd)
      .left(config.visLeft)
      .top(config.visTop)
      .outerRadius(function () {
        return config.outerRadius + (maxIndex - this.index) * config.outerRadiusStep;
      })
      .innerRadius(config.innerRadius)
      .angle(function (d) {
        return (d / total) * 2 * Math.PI;
      })
      .fillStyle(
        pv.colors(fillColors).by(function () {
          return this.index;
        })
      )
      .lineWidth(function (d) {
        return d > 0 ? config.lineWidth : 0;
      })
      .strokeStyle(
        pv.colors(strokeColors).by(function () {
          return this.index;
        })
      );

    if (config.showLabels) {
      wedge
        .add(pv.Label)
        .left(function () {
          if (this.index === 1 && wedge.angle() < 0.61) {
            return config.visLeft + (this.innerRadius() - 5) * Math.cos(wedge.midAngle());
          }
          return config.visLeft + (this.outerRadius() + 3) * Math.cos(wedge.midAngle());
        })
        .top(function () {
          if (this.index === 1 && wedge.angle() < 0.61) {
            return config.visTop + (this.innerRadius() - 5) * Math.sin(wedge.midAngle());
          }
          return config.visTop + (this.outerRadius() + 3) * Math.sin(wedge.midAngle());
        })
        .font('bold ' + config.fontSize + 'px arial')
        .text(function (d) {
          return d === 0 || this.index === dd.length - 1 ? '' : Math.round((d / total) * 100) + '%';
        })
        .textStyle(
          pv.colors(textColors).by(function () {
            return this.index;
          })
        )
        .textAlign(function () {
          var angle = wedge.midAngle() < 0 ? Math.PI * 2 + wedge.midAngle() : wedge.midAngle();
          if (this.index === 2 && angle > Math.PI * 1.5) {
            return 'left';
          } else if (Math.abs((angle % Math.PI) - Math.PI / 2) < 0.31) {
            return 'center';
          } else if (this.index === 1 && wedge.angle() < 0.61) {
            return angle > Math.PI / 2 && angle < 1.5 * Math.PI ? 'left' : 'right';
          }
          return angle > Math.PI / 2 && angle < 1.5 * Math.PI ? 'right' : 'left';
        })
        .textBaseline(function () {
          var angle = wedge.midAngle() < 0 ? Math.PI * 2 + wedge.midAngle() : wedge.midAngle();
          if (Math.abs(((angle + Math.PI / 2) % Math.PI) - Math.PI / 2) < 0.31) {
            return 'middle';
          } else if (this.index === 1 && wedge.angle() < 0.61) {
            return angle > Math.PI ? 'top' : 'bottom';
          }
          return angle > Math.PI ? 'bottom' : 'top';
        });
    }
    vis.render();
  }

  function punchCard(data, config) {
    data = data || [];
    config = config || {};

    var excess = [],
      w = 87,
      h = 5 * 32,
      max = -1,
      i,
      j,
      root,
      vis;
    if (data.length > 5) {
      excess = data.splice(5, data.length - 5);
      for (i = 0; i < excess.length; i++) {
        for (j = 0; j < excess[i].length; j++) {
          data[4][j] += excess[i][j];
        }
      }
    }

    $.each(data, function (rowIndex, row) {
      $.each(row, function (itemIndex, item) {
        max = Math.max(max, item);
      });
    });
    root = new pv.Panel().width(w + 20).height(h + 20);

    if (config.element) {
      root.canvas(config.element);
    }

    root
      .add(pv.Label)
      .text('Dependency Depth')
      .right(0)
      .top(3)
      .font('9px arial')
      .textStyle(textColor)
      .textAlign('right')
      .textBaseline('top');

    for (i = 0; i < 5; i++) {
      root
        .add(pv.Label)
        .text(i >= 4 && excess.length > 0 ? '5+' : i + 1)
        .font('9px arial')
        .textStyle(textColor)
        .left(3)
        .top(36 + i * 32)
        .textBaseline('middle');
    }

    vis = root.add(pv.Panel).bottom(0).right(0).width(w).height(h).strokeStyle(bgBorder).fillStyle(bgBlue);

    for (i = 1; i < 5; i++) {
      vis
        .add(pv.Rule)
        .strokeStyle(bgBorder)
        .top(i * 32);
    }

    $.each(data, function (rowIndex, row) {
      $.each(row, function (itemIndex, item) {
        // This is required otherwise IE8 shows dots for 0s.
        if (item > 0) {
          vis
            .add(pv.Dot)
            .data([item])
            .left(function () {
              return 16 + itemIndex * 28;
            })
            .top(16 + rowIndex * 32)
            .size(function (d) {
              return (d / max) * 144.0;
            })
            .fillStyle(function () {
              return itemIndex === 0 ? red : itemIndex === 1 ? orange : yellow;
            })
            .strokeStyle(function () {
              return itemIndex === 0 ? darkRed : itemIndex === 1 ? darkOrange : darkYellow;
            });
        }
      });
    });

    if (data.length === 0) {
      _createLabel(vis, 'No Components', { height: h, width: w });
    }

    root.render();
  }

  var _createLabelDefaults = {
    fontSize: 9,
    leftPadding: 0,
    rightPadding: 0,
  };

  function _createLabel(vis, labelText, config) {
    config = $.extend({}, _createLabelDefaults, config);

    var labelHeight = config.fontSize * 2,
      barWidth = labelText.length * config.fontSize * 0.6,
      barTop = config.height / 2 - labelHeight / 2,
      barStart = config.leftPadding + (config.width - config.leftPadding - config.rightPadding - barWidth) / 2;

    vis
      .add(pv.Bar)
      .top(barTop)
      .left(barStart)
      .lineWidth(0)
      .height(labelHeight)
      .width(barWidth)
      .strokeStyle(pillColor)
      .fillStyle(pillColor);
    vis
      .add(pv.Dot)
      .top(barTop + labelHeight / 2)
      .left(barStart)
      .radius(labelHeight / 2)
      .lineWidth(0)
      .strokeStyle(pillColor)
      .fillStyle(pillColor);
    vis
      .add(pv.Dot)
      .top(barTop + labelHeight / 2)
      .left(barStart + barWidth)
      .radius(labelHeight / 2)
      .lineWidth(0)
      .strokeStyle(pillColor)
      .fillStyle(pillColor);
    vis
      .add(pv.Label)
      .top(barTop + labelHeight / 2)
      .left(barStart + barWidth / 2)
      .width(barWidth)
      .font('normal ' + (config.fontSize + 1) + 'px arial')
      .textAlign('center')
      .textBaseline('middle')
      .text(labelText);
  }

  $.extend(true, window, {
    HealthCheck: {
      getAge: getAge,
      artifactsChart: artifactsChart,
      licenseChart: licenseChart,
      donutChart: donutChart,
      punchCard: punchCard,
    },
  });
})();
