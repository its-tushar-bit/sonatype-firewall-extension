/*global window*/
var InsightDatatable = {
  getActiveTable: function() {
    return {
      dataView: {
        getItems: function() {
          return [];
        }
      }
    };
  }
},
clmBuildTimestamp = 'testTimestamp';

window.Insight = window.Insight || {}

window.CLM = window.CLM || {}
window.CLM = {
  loadPlugin: function() {
  }
};
