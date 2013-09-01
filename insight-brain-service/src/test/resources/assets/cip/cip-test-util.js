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
};

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