/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, window, Insight */
/*jslint plusplus:true */
(function () {
  'use strict';

  function isNullOrUndefined(obj) {
    return obj === null || typeof obj === 'undefined';
  }

  function isNotNullOrUndefined(obj) {
    return !isNullOrUndefined(obj);
  }

  $.extend(true, window, {
    Insight: {
      util: {
        isNullOrUndefined: isNullOrUndefined,
        isNotNullOrUndefined: isNotNullOrUndefined,
      },
    },
  });
})();
