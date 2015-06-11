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

if (window.CLM) {
  CLM.loadPlugin = function() {
  };
}
else {
  window.CLM = {
    loadPlugin: function() {
    }
  };
}