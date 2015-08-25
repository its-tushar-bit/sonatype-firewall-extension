/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, window, Insight, Brain, HealthCheck, InsightDatatable, Hogan */
/*jslint plusplus:true */
(function() {
  'use strict';

  function componentsEqual(component1, component2) {
    if (Insight.util.isNullOrUndefined(component1.componentIdentifier)) {
      return component1.groupId === component2.groupId && component1.artifactId === component2.artifactId &&
          component1.version === component2.version;
    }

    if (Insight.util.isNullOrUndefined(component2.componentIdentifier)) {
      return false;
    }

    if (component1.componentIdentifier.format !== component2.componentIdentifier.format ||
            component1.componentIdentifier.coordinates.length !== component2.componentIdentifier.coordinates.length) {
      return false;
    }

    for (var property in component1.componentIdentifier.coordinates) {
      if (component1.componentIdentifier.coordinates.hasOwnProperty(property)) {
        if (component1.componentIdentifier.coordinates[property] !==
            component2.componentIdentifier.coordinates[property]) {
          return false;
        }
      }
    }

    return true;
  }

  //build sv reference string (i.e. osvdb-1234) from source and refId
  function toSvReference(source, refId) {
    var src = Insight.util.isNullOrUndefined(source) ? '' : source.toUpperCase(),
        ref = Insight.util.isNullOrUndefined(refId) ? '' : refId.toUpperCase();

    return ref.indexOf(src) < 0 ? (src + '-' + ref) : ref;
  }

  function isNullOrUndefined(obj) {
    return obj === null || typeof obj === 'undefined';
  }

  function isNotNullOrUndefined(obj) {
    return !isNullOrUndefined(obj);
  }

  function getQueryParameter(name) {
    var search = window.location.search.length > 0 ? window.location.search.substring(1).split('&') : [],
        data = null,
        i = null;
    name = name.toLowerCase();

    for (i = 0; i < search.length; i++) {
      data = search[i].split('=');
      if (data[0].toLowerCase() === name) {
        return data[1];
      }
    }
  }

  function enableTooltip(elements) {
    $(elements).each(function() {
      var me = this;
      var offset = $(me).attr('data-gravity-offset') || 0;
      if (offset) {
        offset = parseInt(offset, 10);
      }
      $(this).tipsy({fade: true, gravity: $(me).attr('data-gravity'), offset: offset, html: true, opacity: 1.0, delayOut: 0, title: 'data-tooltip' });
    });
  }

  function getErrorMessage(xhr) {
    var message = '';
    if (xhr.status === 0 || xhr.status >= 1000) {
      message = 'Network error while contacting server';
    }
    else if (xhr.responseText && (xhr.getResponseHeader('Content-Type') || '').indexOf('text/plain') >= 0) {
      message = xhr.responseText;
    }
    else if (xhr.statusText) {
      message = xhr.statusText;
    }
    else {
      message = 'Error ' + xhr.status;
    }
    return message;
  }

  var xhr;

  function showSvModal(source, refId) {
    var bodyEl = $('#sv-info-modal .modal-body');
    if (source && refId) {
      bodyEl.empty();
      bodyEl.append('<p>Loading vulnerability detail content</p>');
      if (xhr && xhr.state() ===  'pending') {
        xhr.reject('reject');
      }

      xhr = $.getJSON(Insight.toBrain(Brain.getVulnerabilityDetailUrl(source, refId))).done(function(data) {
        bodyEl.empty();
        bodyEl.append(data.htmlDetails);
      }).fail(function(resp) {
        if (resp !== 'reject') {
          bodyEl.empty();
          bodyEl.append(Insight.templates.error.render());
          $('.btn', bodyEl).click(function() {
            showSvModal(source, refId);
          });
        }
      });
    } else {
      bodyEl.empty();
      bodyEl.append('Unable to construct url for vulnerability detail content.');
    }

    $('#sv-info-modal').modal('show');
  }

  function showVulnerabilityDetail() {
    return Brain.getVulnerabilityDetailUrl;
  }

  $.extend(true, window, {
    'Insight': {
      'toBrain': function(url) {
        if (url.charAt(0) === '/') {
          return '../brain' + url;
        }
        return '../brain/' + url;
      },
      'util': {
        'enableTooltip': enableTooltip,
        'getErrorMessage': getErrorMessage,
        'getQueryParameter': getQueryParameter,
        'isNullOrUndefined': isNullOrUndefined,
        'isNotNullOrUndefined': isNotNullOrUndefined,
        'componentsEqual': componentsEqual,
        'toSvReference': toSvReference,
        'showSvModal' : showSvModal,
        'showVulnerabilityDetail' : showVulnerabilityDetail,
        'encodeHtml' : (function () {
          var encode = $('<div/>');
          return function (text) {
            return encode.text(text).html();
          };
        }())
      }
    }
  });
}());

// Application startup
(function() {
  'use strict';

  var summaryTemplate,
      errorTemplate;

  function getLicenseSummary(jsonData) {
    var model = {
      criticalCount: 0,
      severeCount: 0,
      moderateCount: 0,
      noThreatCount: 0
    };

    $.each(jsonData.effectiveLicenseCounts, function(index, item) {
      if (index > 7) {
        model.criticalCount += item;
      }
      else if (index > 3) {
        model.severeCount += item;
      }
      else if (index > 0) {
        model.moderateCount += item;
      }
      else {
        model.noThreatCount += item;
      }
    });

    model.count = model.criticalCount + model.severeCount + model.moderateCount + model.noThreatCount;
    model.licenseAlerts = model.criticalCount + model.severeCount + model.moderateCount;
    model.multipleLicenseAlerts = model.licenseAlerts !== 1;

    return model;
  }

  function getSVSummary(jsonData) {
    var model = {
      criticalCount: 0,
      severeCount: 0,
      moderateCount: 0,
      count: 0
    };

    $.each(jsonData.securityCounts, function(index, item) {
      if (index > 6) {
        model.moderateCount += item;
      }
      else if (index > 3) {
        model.severeCount += item;
      }
      else {
        model.criticalCount += item;
      }
      model.count += item;
    });

    model.multiple = model.count !== 1;
    model.insecureArtifactCount = jsonData.insecureArtifactCount;
    model.multipleComponents = model.insecureArtifactCount > 1;

    return model;
  }

  function getPolicySummary(jsonData) {
    var model = {
      criticalCount: 0,
      severeCount: 0,
      moderateCount: 0
    };

    model.policyComponentCount = jsonData.policyComponentCount;
    model.multipleComponents = model.policyComponentCount > 0;

    $.each(jsonData.policyCounts, function(index, item) {
      if (index >= 8) {
        model.criticalCount += item;
      }
      else if (index >= 4) {
        model.severeCount += item;
      }
      else if (index > 1) {
        model.moderateCount += item;
      }
    });
    model.multipleAlerts = (model.criticalCount + model.severeCount + model.moderateCount) > 1 ? true : false;
    model.hasPolicyAlerts = (model.criticalCount + model.severeCount + model.moderateCount) > 0 ? true : false;

    return model;
  }

  function updateSummary() {
    var summaryNode = $('#summary'),
        errorFn = function(resp) {
          summaryNode.empty();
          summaryNode.append(Insight.templates.error.render({
            message: Insight.util.getErrorMessage(resp)
          }));
          $('button', summaryNode).click(updateSummary);
        };

    summaryNode.html('<div class="alert alert-info"><strong>Loading...</strong></div>');

    $.getJSON('data.json').success(function(jsonData) {
      if (jsonData === null) {
        errorFn({ status: 0 });
        return;
      }
      var model = $.extend(jsonData, {
            policy: getPolicySummary(jsonData),
            sv: getSVSummary(jsonData),
            license: getLicenseSummary(jsonData),
            knownComponentCount : jsonData.knownArtifactCount,
            totalComponentCount : jsonData.totalArtifactCount,
            percentKnownComponents : jsonData.totalArtifactCount ? Math.round(100 * jsonData.knownArtifactCount / jsonData.totalArtifactCount) : 0,
            multipleKnown : jsonData.knownArtifactCount !== 1
          }),
          summaryVisible = summaryNode.is(':visible');

      summaryNode.hide();
      summaryNode.html(summaryTemplate.render(model));

      HealthCheck.barChart(jsonData.securityCounts, { element: $('#security-chart')[0] });
      HealthCheck.punchCard(jsonData.securityPunchCard, { element: $('#security-punchcard')[0] });

      HealthCheck.licenseChart([
        model.license.criticalCount / model.license.count, model.license.severeCount / model.license.count,
        model.license.moderateCount / model.license.count,
        (model.license.count - model.license.licenseAlerts) / model.license.count
      ], { element: $('#license-chart')[0] });
      HealthCheck.punchCard(jsonData.licensePunchCard, { element: $('#license-punchcard')[0] });

      var known = jsonData.totalArtifactCount > 0 ?
              (1 - jsonData.knownArtifactCount / jsonData.totalArtifactCount) : 0;
      HealthCheck.artifactsChart(known, { element: $('#coverage_donut')[0] });

      if (summaryVisible) {
        summaryNode.fadeIn();
      }
      Insight.util.enableTooltip($('[data-tooltip]', summaryNode));
    }).error(errorFn);
  }

  $(document).ready(function() {
    summaryTemplate = Hogan.compile($('#summaryPageTemplate').html());
    errorTemplate = Hogan.compile($('#errorTemplate').html());

    $.extend(true, window, {
      'Insight': {
        'templates': {
          'error': errorTemplate
        },
        'updateSummary' : updateSummary
      }
    });

    $('button[data-containerid]').click(function() {
      if (!$(this).hasClass('active')) {
        var containerId = $(this).data('containerid'),
            table;
        // hide
        $('[data-container]').css('display', 'none');
        // show
        $('#' + containerId).css('display', '');

        InsightDatatable.destroyActiveTable();

        switch (containerId) {
          case 'securitycontainer' :
            table = InsightDatatable.createSecurityTable({ 'headerRowHeight': 30 });
            table.addLoadListener(function() {
              InsightDatatable.addSecurityTableEditor(table);
            });
            break;
          case 'licensecontainer' :
            table = InsightDatatable.createLicenseTable({ 'headerRowHeight': 30 });
            break;
          case 'componentcontainer' :
            InsightDatatable.createComponentTable();
            break;
          case 'summary':
            break;
        }
      }
    });

    Insight.util.enableTooltip($('[data-tooltip]'));

    var policyReevaluationAnchor = $('[data-action="reevaluatePolicy"]');
    if (!Brain.hasFeature('reevaluate-policy')) {
      policyReevaluationAnchor.hide();
    }
    else {
      policyReevaluationAnchor.click(function() {
        policyReevaluationAnchor.find('i').attr('class', 'icon-white icon-time');
        $.ajax({
          type: Brain.getCurrentReportReevaluateUrl ? 'POST' : 'GET',
          url: Brain.getCurrentReportReevaluateUrl ? Brain.getCurrentReportReevaluateUrl() : '../reevaluatePolicy',
          headers: Brain.getCsrfHeaders ? Brain.getCsrfHeaders() : {} 
        }).success(function() {
          policyReevaluationAnchor.find('i').attr('class', 'icon-white icon-refresh');
          location.reload();
        }).error(function() {
              $('.btn-group:eq(1)').after('<div class="alert alert-error" style="margin:5px 0"><button class="close" data-dismiss="alert">&times;</button>There has been an error reevaluating policy. Try running the report again.</div>');
            });
      });
    }

    if ($.browser.msie && $.browser.version < 9) {
      $('.btn-group:eq(1)').after('<div class="alert alert-error" style="margin:5px 0"><button class="close" data-dismiss="alert">&times;</button>You are using older version of Internet Explorer (IE). You may notice degraded performance and/or functionality. To address this, download and install the latest version of IE, or other modern browser (e.g. Chrome, Firefox)</div>');
    }

    Insight.updateSummary();
  });
}());
