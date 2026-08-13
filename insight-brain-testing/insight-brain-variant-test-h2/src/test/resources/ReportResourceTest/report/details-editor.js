/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/pro/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global document, Hogan, $ */
(function ($) {
	var licenseKeyColumns = ['groupId', 'artifactId', 'version'],
		securityKeyColumns =  ['groupId', 'artifactId', 'version', 'source', 'reference'],
		componentKeyColumns = ['groupId', 'artifactId', 'version'],
		licenseEditorTemplate,
        svEditorTemplate;

	$(document).ready(function () {
		licenseEditorTemplate = Hogan.compile($('#licenseEditorTemplate').html());
		svEditorTemplate = Hogan.compile($('#svEditorTemplate').html());
	});

	function isNullOrUndefined(obj) {
		return obj === null || typeof obj === 'undefined';
	}

	function isNotNullOrUndefined(obj) {
		return !isNullOrUndefined(obj);
	}
	
	function LicenseEditor(options) {
		options = $.extend({ "timestamp" : new Date().getTime() }, options);
		var _artifacts = options.artifacts,
			licenseStatusId = "#licenseselect" + options.timestamp,
			licenseId = '#license' + options.timestamp,
            _node = null,
			formValidation = function () {
				var status = $('option:selected', licenseStatusId).val(),
					comment = $.trim($('textarea', _node).val()),
					license = $('option:selected', licenseId).val();
				if ((_artifacts[0].status === status || (isNullOrUndefined(_artifacts[0].status) && status === 'Open')) &&
						(comment.length === 0) &&
						(status !== 'Overridden' || (isNotNullOrUndefined(_artifacts[0].overriddenLicenses) && _artifacts[0].overriddenLicenses[0] === license))) {
					$('button[data-type=update]', _node).attr('disabled', 'disabled');
				} else {
					$('button[data-type=update]', _node).removeAttr('disabled');
				}
			};

		if (options.grid) {
			options.grid.onSelectedRowsChanged.subscribe(artifactsChanged);
		}

		function artifactsChanged() {
			_artifacts = [];
			$.each(options.grid.getSelectedRows(), function (index, item) {
				_artifacts.push(options.grid.getData().getItem(item));
			});
			if (_artifacts.length === 1) {
				$('input,textarea,select', _node).change(formValidation).keyup(formValidation).focusout(formValidation);
			} else {
				$('input,textarea,select', _node).unbind('change').unbind('keyup').unbind('focusout');
				$('button[data-type=update]', _node).removeAttr('disabled');
			}
		}

		function destroy() {
			if (options.grid) {
				options.grid.onSelectedRowsChanged.unsubscribe(artifactsChanged);
			}
		}

		function update(e) {
			var me = this,
				mask = {},
				dataView;
			e.preventDefault();
			$(me).attr('disabled', 'disabled');

			$('select', _node).each(function(index) {
				if (index === 0) {
					mask.status = $(this).val();
				} else {
					var overridden = !$(this).selectmenu('option', 'disabled');
					mask.overriddenLicenses = overridden ? [ $(this).val() ] : null;
					mask.overriddenLicenseThreat = overridden ? InsightDatatable.getLicenseThreatLevelFromArray(mask.overriddenLicenses) : null;
				}
			});
			mask.comment = $('textarea', _node).val();

			updateLicenses({
				mask : mask,
				callback : function (errorMsg) {
					if (isNotNullOrUndefined(errorMsg)) {
						$(me).removeAttr('disabled');
					}
					if (isNotNullOrUndefined(options.callback)) {
						options.callback.apply(this, arguments);
					}
				},
				dataView : options.dataView,
				items : _artifacts
			});
		}

		function show(node) {
			_node = node;
			$(_node).html(licenseEditorTemplate.render(options) );
			$('button[data-type=update]', _node).click(update);
			$('button[data-type=cancel]', _node).click(function(e){
				e.preventDefault();
				if (options.cancelCallback) {
					options.cancelCallback.apply(this);
				}
			});

			if (_artifacts.length === 1) {
				$('button[data-type=update]', _node).attr('disabled', 'disabled');
				$('input,textarea,select', _node).change(formValidation).keyup(formValidation).focusout(formValidation);
				if (isNotNullOrUndefined(_artifacts[0].status)) {
					$('[value=' + _artifacts[0].status + ']', licenseStatusId).attr('selected', true);
				}
			}
			$(licenseStatusId).selectmenu({ "width" : options.width });

            $(licenseStatusId).change(function() {
                if ($(licenseStatusId).val() === 'Overridden') {
                    $(licenseId).selectmenu('enable');
                } else {
                    $(licenseId).selectmenu('disable');
                }
            });

            loadLicenses(function (licenses) {
                var licenseCombo = $(licenseId);
                InsightDatatable.populateLicenseCombo(licenseCombo, licenses);
				if (_artifacts.length === 1) {
					if (isNotNullOrUndefined(_artifacts[0].overriddenLicenses) && _artifacts[0].overriddenLicenses.length > 0) {
						$('option', licenseCombo).each(function () {
							if ($(this).val() === _artifacts[0].overriddenLicenses[0]) {
								$(this).attr('selected', true);
							}
						});
					}
					if (_artifacts[0].status === 'Overridden') {
						$(licenseCombo).removeAttr('disabled');
					} else {
						$(licenseCombo).attr('disabled', true);
					}
					formValidation();
				}
				$(licenseCombo).selectmenu({ "width" : options.width });
            });
		}

		$.extend(this, {
			'destroy' : destroy,
			'show' : show
		});
	}

	function SecurityEditor(options) {
		options = $.extend({ "timestamp" : new Date().getTime() }, options);
		var _artifacts = options.artifacts,
			_node;

		function artifactsChanged() {
			_artifacts = [];
			$.each(options.grid.getSelectedRows(), function(index, item) {
				_artifacts.push(options.grid.getData().getItem(item));
			});
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
			mask.comment= $('textarea', _node).val();

			updateSecurity({
				mask : mask,
				dataView : options.dataViews[0],
				items : _artifacts,
				callback : function(){
					for (var i=1; i<options.dataViews.length; i++) {
						options.dataViews[i].beginUpdate();
                        $.each(_artifacts, function(index, updatedItem) {
                            $.each(options.dataViews[i].getItems(), function(index, item) {
                                for (var j = 0; j < securityKeyColumns.length; j++) {
                                    if (updatedItem[securityKeyColumns[j]] !== item[securityKeyColumns[j]]) {
                                        return;
                                    }
                                }
                                options.dataViews[i].updateItem(item.id, $.extend({}, item, mask));
                                return false;
                            });
                        });
                        options.dataViews[i].endUpdate();
					}
					options.callback.apply(this, arguments);
				}
			});
		}

		function show(node) {
			_node = node;
			$(_node).html( svEditorTemplate.render(options) );
			$('button[data-type=update]', _node).click(update);

			$('button[data-type=cancel]', _node).click(function(e){
				e.preventDefault();
				if (options.cancelCallback) {
					options.cancelCallback.apply(this);
				}
			});
			$('#svStatus' + options.timestamp, _node).selectmenu({ "width" : options.width });
		}

		$.extend(this, {
			'destroy' : destroy,
			'show' : show
		});
	}

	function updateItems(dataView, items, mask, keyColumns, file, callback) {
		var modifiedRows = [], saveColumns = [];

		$.each(items, function(itemIndex, item){
			var dataItem = $.extend({}, item);
			$.each(mask, function(key, value){
				dataItem[key] = value;
			});
			dataItem.modified = true;
			modifiedRows.push(dataItem);
		});
		
		$.merge(saveColumns, keyColumns);
		$.each(mask, function(key, value){
			saveColumns.push(key);
		});

		var commitFn = function(){
			dataView.beginUpdate();
			$.each(modifiedRows, function(index, dataItem) {
				dataView.updateItem(dataItem.id, dataItem);
			});
			dataView.endUpdate();
			InsightDatatable.updateSummary();

			callback();
		};

		if (demoMode) {
			commitFn();
		} else {

			// non-critical request to flag rows as modified in UI
			$.ajax({
				type : 'POST',
				url : '../augmentData/bom.json',
				data : JSON.stringify(modifiedRows, $.merge(['modified'], componentKeyColumns)),
				beforeSend : parent.beforeSendAugmentedData
			});

			// send request
			$.ajax({
				type : 'POST',
				url : '../augmentData/' + file,
				data : JSON.stringify(modifiedRows, saveColumns),
				beforeSend : parent.beforeSendAugmentedData
			}).success(commitFn).error(function(resp, type, message){
				if (resp.status === 405) {
					message = 'Cannot edit old report, return to Project for latest results';
				} else if (resp.status === 0) {
					message = 'Error while contacting server';
				}
				callback(message);
			});
		}
	}

	var licenses = null,
	    licenseReqActive = false,
	    licenseLoadCallbacks = [];

	function loadLicenses(callback, table) {
		if (isNotNullOrUndefined(licenses)) {
			callback.apply(null, [licenses]);
			return;
		}
		licenseLoadCallbacks.push(callback);
		if (!licenseReqActive) {
			licenseReqActive = true;
			$.getJSON('licenselist.json').success(function(jsonData){
				licenses = jsonData.aaData;
				$.each(licenseLoadCallbacks, function(index,item){
					item.apply(null, [licenses]);
				});
			}).error(function(resp, type, msg) {
				// I don't like this but not sure what to do?
				var table = InsightDatatable.getActiveTable();
				if (table !== null) {
					table.addError('Loading known licenses: ' + msg);
				} else if (console) {
					console.log('Error loading known licenses: ' + msg);
				}
			});
		}
	}

	function populateLicenseCombo(combo, licenses, selectedLicense){
		$(combo).empty();
		var sortedLicenses = [];
		$.each(licenses, function(license, category) {
			sortedLicenses.push(license);
		});
		sortedLicenses.sort(function(a,b){
			a = a.toUpperCase();
			b = b.toUpperCase();
			return a > b ? 1 : a < b ? -1 : 0;
		});
		$.each(sortedLicenses, function(index, license) {
			var optionnode = $('<option></option>').text(license).val(license);
			if (license === selectedLicense) {
				optionnode.attr('selected','selected');
			}
			$(combo).append(optionnode);
		});
	}

	function addLicenseTableEditor(table) {
		if (Insight.isReadOnly()) {
			$('#licenseEditorContainer').css('display', 'none');
			return;
		}
		var grid = table.table,
		    columns = grid.getColumns(),
			removeCallback = function() {
				$('#licensestateselect').selectmenu('destroy');
				$('#licensestateselect').val('Open').attr('selected', true);
				$('#licenseselect').attr('disabled', 'disabled');
				$('textarea', '#licenseEditor').val('');
				table.updateHeight();
			},
			bulkEditor = new Slick.BulkEditor({
				editNode : $('#licenseEditor'),
				editor : new LicenseEditor({
					"artifacts" : [],
					"width" : 650,
					"cancel" : true,
					"grid" : grid,
					"dataView" : grid.getData(),
					"callback" : function(errorMsg) {
						if (isNotNullOrUndefined(errorMsg)) {
							table.addError(errorMsg);
						} else {
							var items = grid.getSelectedRows();
							InsightDatatable.updateSummary();
							if (items.length == 1) {
								table.addSuccess('Updated license for ' + items.length + ' component.', 60);
							} else if (items.length > 1) {
								table.addSuccess(grid, 'license', 'Updated license for ' + items.length + ' components.', 60);
							}
							trackEvent('bulk_editor', 'update_license', undefined, items.length);
							$('#licenseEditor').slideUp({
								complete : function () {
									$(this).empty();
								},
								step : function () {
									table.updateHeight();
								}
							});
						}
					},
					"cancelCallback" : function(affectedRows) {
						$('#licenseEditor').empty().hide();
						table.updateHeight();
					}
				}),
				openCallback : function() {
					table.updateHeight();
				}
			}),
			updateCallback;

		columns = $.merge([bulkEditor.getColumnDef()], columns);
		columns[0].filterable = false;

		grid.setSelectionModel(new Slick.RowSelectionModel());
		grid.getData().syncGridSelection(grid, true);
		grid.setColumns(columns);

		grid.registerPlugin( bulkEditor );
	}

	function addSecurityTableEditor(table) {
		if (Insight.isReadOnly()) {
			$('#securityEditorContainer').css('display', 'none');
			return;
		}
		var grid = table.table,
		    columns = grid.getColumns(),
			removeCallback = function() {
				$('#securitystateselect').val('Open').attr('selected', true);
				$('#securitystateselect').selectmenu('destroy');
				$('textarea', '#securityEditor').val('');
				$('select', '#securityEditor').val('New');
				table.updateHeight();
			},
			bulkEdit =  new Slick.BulkEditor({
				editNode : $('#securityEditor'),
				editor : new SecurityEditor({
					"width" : 650,
					"grid" : grid,
					"cancel" : true,
					"artifacts" : [],
					"dataViews"  : [grid.getData()],
					"callback" : function(errorMsg) {
							if (isNotNullOrUndefined(errorMsg)) {
								table.addError(errorMsg);
							} else {
								var items = grid.getSelectedRows();
								InsightDatatable.updateSummary();
								if (items.length == 1) {
									table.addSuccess('Updated ' + items.length + ' security vulnerability.', 60);
								} else if (items.length > 1) {
									table.addSuccess('Updated ' + items.length + ' security vulnerabilities.', 60);
								}
								$('#securityEditor').slideUp({
									complete : function () {
										$(this).empty();
									},
									step : function () {
										table.updateHeight();
									}
								});
								trackEvent('bulk_editor', 'update_security', undefined, items.length);
							}
					},
					"cancelCallback" : function(affectedRows) {
						$('#securityEditor').empty().hide();
						table.updateHeight();
					}
				}),
				openCallback : function() {
					table.updateHeight();
				}
			});
		columns = $.merge([bulkEdit.getColumnDef()], columns);
		columns[0].filterable = false;


		grid.setSelectionModel(new Slick.RowSelectionModel());
		grid.getData().syncGridSelection(grid, true);
		grid.setColumns(columns);
		grid.registerPlugin( bulkEdit );
	}
	
	function updateKeyFindings(jsonData) {
		var content = '';
		
		if (jsonData.keyFindings && jsonData.keyFindings.length > 0) {
			content += "<div style='padding:5px' class='topBorder'><div class='page-header'><h4 class='i-key'>Key Findings</h4></div><ul class='key'>";
			
			for ( var i = 0 ; i < jsonData.keyFindings.length && i < 3 ; i++ ) {
				content += '<li>' + jsonData.keyFindings[i].text + '</li>';
			}
			
			content += '</ul></div>';
		}
		
		$('#keyfindings').html(content);
	}

	function updatePolicySummary(jsonData) {
		if (isNullOrUndefined(jsonData.policyCounts)){
			return; // old brain servers don't provide policy information, so don't populate the policy summary
		}

		var summaryHeader = $('#policyHeader'),
			affectingLabel = 'AFFECTING <span class="value_sml">' + jsonData.policyComponentCount + '</span> COMPONENT',
			criticalCount = 0,
			severeCount = 0,
			moderateCount = 0,
			noThreatCount = 0;

		$.each(jsonData.policyCounts, function(index, item){
			if (index >=8) {
				criticalCount += item;
			} else if (index >= 4) {
				severeCount += item;
			} else if (index > 0) {
				moderateCount += item;
			} else {
				noThreatCount += item;
			}
		});
		var policyAlerts = criticalCount + severeCount + moderateCount; 

		var counters = $('.pval span span', summaryHeader);
		if (criticalCount > 0) {
			counters.eq(0).text(criticalCount).css('display','inline-block');
		} else {
			counters.eq(0).hide();
		}
		if (severeCount > 0) {
			counters.eq(1).text(severeCount).css('display','inline-block');
		} else {
			counters.eq(1).hide();
		}
		if (moderateCount > 0) {
			counters.eq(2).text(moderateCount).css('display','inline-block');
		} else {
			counters.eq(2).hide();
		}
		if (criticalCount == 0 && severeCount == 0 && moderateCount == 0) {
			counters.eq(3).text(0).css('display','inline-block');
		} else {
			counters.eq(3).hide();
		}

		$('.label_med', summaryHeader).text(policyAlerts !== 1 ? 'POLICY ALERTS' : 'POLICY ALERT');
		$('.label_sml', summaryHeader).html(jsonData.policyComponentCount != 1 ? affectingLabel + 'S' : affectingLabel);
	}

	function updateSVSummary(jsonData) {
		var summaryHeader = $('#svHeader'),
			affectingLabel = 'AFFECTING <span class="value_sml">' + jsonData.insecureArtifactCount + '</span> COMPONENT',
			criticalCount = 0,
			severeCount = 0,
			moderateCount = 0,
			count = 0;

		$.each(jsonData.securityCounts, function(index, item){
			if (index > 6) {
				moderateCount += item;
			} else if (index > 2) {
				severeCount += item;
			} else {
				criticalCount += item;
			}
			count += item;
		});

		$('.value_lrg', summaryHeader).text(count);
		$('.label_med', summaryHeader).text(count != 1 ? 'SECURITY ALERTS' : 'SECURITY ALERT');
		$('.label_sml', summaryHeader).html(jsonData.insecureArtifactCount != 1 ? affectingLabel + 'S' : affectingLabel);

		$('#criticalSVCount').text(criticalCount);
		$('#severeSVCount').text(severeCount);
		$('#moderateSVCount').text(moderateCount);

		HealthCheck.barChart(jsonData.securityCounts, { element : $('#security-chart')[0] });
		HealthCheck.punchCard(jsonData.securityPunchCard, { element : $('#security-punchcard')[0] });
	}

	function updateLicenseSummary(jsonData) {
		var summaryHeader = $('#licenseHeader'),
			criticalCount = 0,
			severeCount = 0,
			moderateCount = 0,
			noThreatCount = 0;
		
		if (typeof(jsonData.effectiveLicenseCounts) !== 'undefined') {
			$.each(jsonData.effectiveLicenseCounts, function(index, item){
				if (index > 7) {
					criticalCount += item;
				} else if (index > 3) {
					severeCount += item;
				} else if (index > 0) {
					moderateCount += item;
				} else {
					noThreatCount += item;
				}
			});
		} else {
			// Deprecated as of Insight-Brain 1.2. Leave in for compatibility with Insight-Brain 1.1
			criticalCount = jsonData.copyleftLicenseCount;
			severeCount = jsonData.nonStandardLicenseCount + jsonData.notProvidedLicenseCount;
			moderateCount = jsonData.weakcopyleftLicenseCount;
			noThreatCount = jsonData.liberalLicenseCount;
		}
		var count = criticalCount + severeCount + moderateCount + noThreatCount;
		var licenseAlerts = criticalCount + severeCount + moderateCount; 

		$('.value_lrg', summaryHeader).text(licenseAlerts);
		$('.label_med', summaryHeader).text(licenseAlerts !== 1 ? 'LICENSE ALERTS' : 'LICENSE ALERT');

		$('#criticalLicenseCount').text(criticalCount);
		$('#severeLicenseCount').text(severeCount);
		$('#moderateLicenseCount').text(moderateCount);
		$('#noThreatLicenseCount').text(noThreatCount);

		HealthCheck.licenseChart([criticalCount/count, severeCount/count, moderateCount/count, (count - licenseAlerts)/count], { element : $('#license-chart')[0] });
		HealthCheck.punchCard(jsonData.licensePunchCard, { element : $('#license-punchcard')[0] });
	}

	function updateSummary() {
		$.getJSON('data.json').success(function(jsonData){
			updateKeyFindings(jsonData);
			updatePolicySummary(jsonData);
			updateSVSummary(jsonData);
			updateLicenseSummary(jsonData);
		});
	}

	function updateLicenses(options) {
		options = $.extend({ dataView : null, items : [], mask : {}, callback : $.noop }, options);
		if (!$.isArray(options.items)) {
			options.items = [options.items];
		}
		updateItems(options.dataView, options.items, options.mask, licenseKeyColumns, 'licenses.json', options.callback);
	}

	function updateSecurity(options) {
		options = $.extend({ dataView : null, items : [], mask : {}, callback : $.noop }, options);
		if (!$.isArray(options.items)) {
			options.items = [options.items];
		}
		updateItems(options.dataView, options.items, options.mask, securityKeyColumns, 'security.json', options.callback);
	}

	// register namespace
	$.extend(true, window, {
		"InsightDatatable" : {
			"addSecurityTableEditor" : addSecurityTableEditor,
			"addLicenseTableEditor" : addLicenseTableEditor,
			"populateLicenseCombo" : populateLicenseCombo,
			"loadLicenses" : loadLicenses,
			"LicenseEditor" : LicenseEditor,
			"SecurityEditor" : SecurityEditor,
			"updateLicenses" : updateLicenses,
			"updateSecurity" : updateSecurity,
			"updateSummary" : updateSummary
		}
	});
})(jQuery);
