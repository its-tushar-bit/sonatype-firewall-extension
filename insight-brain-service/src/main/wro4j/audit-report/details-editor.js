/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global document, Hogan, $, Insight, Slick, Brain */
(function() {
  'use strict';

  var securityKeyColumns = ['source', 'reference'],
    bomKeyColumns = ['hash'],
    svEditorTemplate;

  $(document).ready(function() {
    svEditorTemplate = Hogan.compile($('#svEditorTemplate').html());
  });

  function SecurityEditor(options) {
    options = $.extend({ 'timestamp': new Date().getTime() }, options);
    var _artifacts = options.artifacts,
        _node;

    function validate() {
      if (_artifacts.length === 0) {
        $('button[data-type=update]', _node).attr('disabled', 'disabled');
      }
      else {
        $('button[data-type=update]', _node).removeAttr('disabled');
      }
    }

    function artifactsChanged() {
      _artifacts = [];
      $.each(options.grid.getSelectedRows(), function(index, item) {
        _artifacts.push(options.grid.getData().getItem(item));
      });
      validate();
    }

    if (options.grid) {
      options.grid.onSelectedRowsChanged.subscribe(artifactsChanged);
    }

    function destroy() {
      if (options.grid) {
        options.grid.onSelectedRowsChanged.unsubscribe(artifactsChanged);
      }
    }

    function update(e) {
      var mask = {};
      e.preventDefault();

      mask.status = $('select', _node).val();
      mask.comment = $('textarea', _node).val();

      updateSecurity({
        mask: mask,
        dataView: options.dataViews[0],
        items: _artifacts,
        callback: function(errorMsg) {
          //if there is an error, no need to do any view updates
          if (!Insight.util.isNotNullOrUndefined(errorMsg)) {
            for (var i = 1; i < options.dataViews.length; i++) {
              options.dataViews[i].beginUpdate();
              $.each(_artifacts, function(index, updatedItem) {
                $.each(options.dataViews[i].getItems(), function(index, item) {
                  if (updatedItem.source !== item.source || updatedItem.reference !== item.reference  ||
                      updatedItem.componentIdentifier.format !== item.componentIdentifier.format) {
                    return;
                  }
                  for (var property in updatedItem.componentIdentifier.coordinates) {
                    if (updatedItem.componentIdentifier.coordinates.hasOwnProperty(property)) {
                      if (updatedItem.componentIdentifier.coordinates[property] !== item.componentIdentifier.coordinates[property]) {
                        return;
                      }
                    }
                  }
                  options.dataViews[i].updateItem(item.id, $.extend({}, item, mask));
                  return false;
                });
              });
              options.dataViews[i].endUpdate();
            }
          }
          
          if (Insight.util.isNotNullOrUndefined(options.callback)) {
            options.callback.apply(this, arguments);
          }
        }
      });
    }

    function show(node) {
      _node = node;
      $(_node).html(svEditorTemplate.render(options));
      $('button[data-type=update]', _node).click(update);

      $('button[data-type=cancel]', _node).click(function(e) {
        e.preventDefault();
        if (options.cancelCallback) {
          options.cancelCallback.apply(this);
        }
      });
      $('#svStatus' + options.timestamp, _node).css('width', options.width);
      validate();
    }

    $.extend(this, {
      'destroy': destroy,
      'show': show
    });
  }

  function updateItems(dataView, items, mask, keyColumns, file, callback) {
    var modifiedRows = [],
        filter = [],
        httpHeaders = Brain.getCsrfHeaders ? Brain.getCsrfHeaders() : {};

    $.each(items, function(itemIndex, item) {
      var dataItem = $.extend({}, item);
      $.each(mask, function(key, value) {
        dataItem[key] = value;
      });
      dataItem.modified = true;
      modifiedRows.push(dataItem);
    });

    $.merge(filter, keyColumns);
    $.each(mask, function(key) {
      filter.push(key);
    });

    var commitFn = function() {
      dataView.beginUpdate();
      $.each(modifiedRows, function(index, dataItem) {
        dataView.updateItem(dataItem.id, dataItem);
      });
      dataView.endUpdate();
      Insight.updateSummary();

      callback();
    };

    // non-critical request to flag rows as modified in UI
    $.ajax({
      type: 'POST',
      url: '../augmentData/bom.json',
      data: buildJson(modifiedRows, $.merge(['modified'], bomKeyColumns)),
      headers: httpHeaders
    });

    // send request
    $.ajax({
      type: 'POST',
      url: '../augmentData/' + file,
      data: buildJson(modifiedRows, filter),
      headers: httpHeaders
    }).success(commitFn).error(function(resp, type, message) {
      message = Insight.util.getErrorMessage(resp);
      callback(message);
    });
  }

  function buildJson(dataItemArray, filter) {
    var jsonArray = [];
    for (var i = 0; i < dataItemArray.length; i++) {
      var dataItem = dataItemArray[i];
      var jsonItem = {
        componentIdentifier: dataItem.componentIdentifier
      };
      //CLM server < 1.13.0 stores audits using GAV
      if (Insight.util.isNotNullOrUndefined(dataItem.componentIdentifier)) {
        jsonItem.groupId = dataItem.componentIdentifier.coordinates.groupId;
        jsonItem.artifactId = dataItem.componentIdentifier.coordinates.artifactId;
        jsonItem.version = dataItem.componentIdentifier.coordinates.version;
      }
      for (var j = 0; j < filter.length; j++) {
        jsonItem[filter[j]] = dataItem[filter[j]];
      }
      jsonArray.push(jsonItem);
    }
    return JSON.stringify(jsonArray);
  }

  function addSecurityTableEditor(table) {
    var grid = table.table,
        columns = grid.getColumns();

    grid.setSelectionModel(new Slick.RowSelectionModel());
    grid.getData().syncGridSelection(grid, true);
    grid.setColumns(columns);
  }

  function updateSecurity(options) {
    options = $.extend({ dataView: null, items: [], mask: {}, callback: $.noop }, options);
    if (!$.isArray(options.items)) {
      options.items = [options.items];
    }
    updateItems(options.dataView, options.items, options.mask, securityKeyColumns, 'security.json', options.callback);
  }

  function updateBom(options) {
    options = $.extend({ dataView: null, items: [], mask: {}, callback: $.noop }, options);
    if (!$.isArray(options.items)) {
      options.items = [options.items];
    }
    updateItems(options.dataView, options.items, options.mask, bomKeyColumns, 'bom.json', options.callback);
  }

  // register namespace
  $.extend(true, window, {
    'InsightDatatable': {
      'addSecurityTableEditor': addSecurityTableEditor,
      'SecurityEditor': SecurityEditor,
      'updateSecurity': updateSecurity,
      'updateBom': updateBom
    }
  });
})();
