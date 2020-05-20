/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global window*/
window.InsightDatatable = {
  getActiveTable: function() {
    return {
      dataView: {
        getItems: function() {
          return [];
        }
      }
    };
  }
};
window.clmBuildTimestamp = 'testTimestamp';

window.Insight = window.Insight || {};

window.CLM = {
  loadPlugin: function() {
  }
};
