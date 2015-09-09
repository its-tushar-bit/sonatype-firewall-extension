/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, window, Insight, Brain, Hogan */
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

  $(document).ready(function() {
    var errorTemplate = Hogan.compile($('#errorTemplate').html());

    $.extend(true, window, {
      'Insight': {
        'templates': {
          'error': errorTemplate
        }
      }
    });
  });
}());
