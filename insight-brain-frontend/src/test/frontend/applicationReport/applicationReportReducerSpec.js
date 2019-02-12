import applicationReportModule from '../../../main/frontend/applicationReport/module';

describe('applicationReportReducer', function() {
  let reduce;
  const otherObject = {value: 'test value'};

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(inject(function($injector) {
    reduce = $injector.get('applicationReportReducer');
  }));

  describe('unknown action', function() {
    it('returns original state', function() {
      const state = Object.freeze({foo: 'bar'});
      const action = {type: 'UNKNOWN'};
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reduce(undefined, action);
      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.reevaluating).toBe(false);
      expect(newState.reevaluationError).toBe(null);
      expect(newState.selectedReport).toBe(null);
      expect(newState.aggregate).toBe(true);
      expect(newState.sortFields).toEqual(['-policyThreatLevel', 'policyName', 'derivedComponentName']);
      expect(newState.exactValueFilters).toEqual({});
      expect(newState.substringFilters).toEqual({});
      expect(newState.isUnknownJs).toBe(false);
    });
  });

  describe('LOAD_REPORT_REQUESTED action', function() {
    it('sets loading flag and unsets error and selectedReport values', function() {
      const state = Object.freeze({
        loading: false,
        loadError: 'test error',
        selectedReport: 'test report',
        other: otherObject
      });
      const newState = reduce(state, {type: 'LOAD_REPORT_REQUESTED'});
      expect(newState).toEqual({
        loading: true,
        loadError: null,
        selectedReport: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_FULFILLED action', function() {
    it('unsets loading flag and sets selectedReport and isUnknownJs values', function() {
      const state = Object.freeze({
        loading: true,
        loadError: null,
        selectedReport: null,
        other: otherObject
      });
      const entries = [
        {policyThreatLevel: 1}, {policyThreatLevel: 3}, {policyThreatLevel: 4, waived: true}, {policyThreatLevel: 6},
        {policyThreatLevel: 9}, {policyThreatLevel: 10, grandfathered: true}
      ];
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          report: {allEntries: entries},
          isUnknownJs: false
        }
      });
      expect(newState).toEqual({
        loading: false,
        loadError: null,
        selectedReport: {
          allEntries: entries,
          displayedEntries: entries,
          moderateViolationCount: 1,
          severeViolationCount: 1,
          criticalViolationCount: 1,
          nonLowViolationCount: 3
        },
        isUnknownJs: false,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets the displayedEntries in the selectedReport based on the current aggregation etc settings', function() {
      const state = Object.freeze({
            selectedReport: null,
            aggregate: true,
            exactValueFilters: {
              policyThreatLevel: new Set([1, 4, 5, 6])
            },
            sortFields: ['-policyThreatLevel']
          }),
          entries = [{
            hash: '1',
            policyThreatLevel: 1,
            policyName: 'P1',
            waived: false,
            grandfathered: false
          }, {
            hash: '2',
            policyThreatLevel: 3,
            policyName: 'P2',
            waived: false,
            grandfathered: false
          }, {
            hash: '4',
            policyThreatLevel: 4,
            policyName: 'P3',
            waived: true,
            grandfathered: false,
            displayName: {parts: []}
          }, {
            hash: '1',
            policyThreatLevel: 6,
            policyName: 'P4',
            waived: false,
            grandfathered: false
          }, {
            hash: '5',
            policyThreatLevel: 4,
            policyName: 'P5',
            waived: false,
            grandfathered: false
          }, {
            hash: '6',
            policyThreatLevel: 10,
            policyName: 'P6',
            waived: false,
            grandfathered: true,
            displayName: {parts: []}
          }],
          newState = reduce(state, {
            type: 'LOAD_REPORT_FULFILLED',
            payload: {
              report: {allEntries: entries}
            }
          });

      expect(newState.selectedReport.displayedEntries).toEqual([{
        hash: '1',
        policyThreatLevel: 6,
        policyName: 'P4',
        waived: false,
        grandfathered: false
      }, {
        hash: '5',
        policyThreatLevel: 4,
        policyName: 'P5',
        waived: false,
        grandfathered: false
      }]);
    });

    it('sets selectedComponentIndex while in aggregated mode if a component was previously selected', function() {
      const state = Object.freeze({
        loading: true,
        loadError: null,
        aggregate: true,
        sortFields: ['derivedComponentName'],
        selectedComponentIndex: 0,
        selectedReport: {
          displayedEntries: [{
            hash: '1',
            derivedComponentName: '1'
          }, {
            hash: '2',
            derivedComponentName: '2'
          }, {
            hash: '3',
            derivedComponentName: '3'
          }]
        },
        other: otherObject
      });

      const entries = [{
        hash: '1',
        derivedComponentName: '5'
      }, {
        hash: '2',
        derivedComponentName: '2'
      }, {
        hash: '3',
        derivedComponentName: '3'
      }];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          report: {allEntries: entries}
        }
      });

      expect(newState.selectedComponentIndex).toBe(2);
    });

    it('sets selectedComponentIndex while in non aggregated mode if a component was previously selected', function() {
      const state = Object.freeze({
        loading: true,
        loadError: null,
        aggregate: false,
        sortFields: ['derivedComponentName', 'policyName'],
        selectedComponentIndex: 1,
        selectedReport: {
          displayedEntries: [{
            hash: '1',
            policyName: 'P1',
            derivedComponentName: '1'
          }, {
            hash: '1',
            policyName: 'P2',
            derivedComponentName: '1'
          }, {
            hash: '2',
            policyName: 'P2',
            derivedComponentName: '2'
          }, {
            hash: '3',
            policyName: 'P3',
            derivedComponentName: '3'
          }]
        },
        other: otherObject
      });

      const entries = [{
        hash: '1',
        policyName: 'P1',
        derivedComponentName: '5'
      }, {
        hash: '1',
        policyName: 'P2',
        derivedComponentName: '5'
      }, {
        hash: '2',
        policyName: 'P2',
        derivedComponentName: '2'
      }, {
        hash: '3',
        policyName: 'P3',
        derivedComponentName: '3'
      }];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          report: {allEntries: entries}
        }
      });

      expect(newState.selectedComponentIndex).toBe(3);
    });
  });

  describe('REEVALUATE_REPORT_REQUESTED action', function() {
    it('sets reevaluating flag and unsets reevaluationError', function() {
      const state = Object.freeze({
        reevaluating: false,
        reevaluationError: 'Error',
        other: otherObject
      });
      const newState = reduce(state, {type: 'REEVALUATE_REPORT_REQUESTED'});
      expect(newState).toEqual({
        reevaluating: true,
        reevaluationError: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REEVALUATE_REPORT_FULFILLED action', function() {
    it('unsets reevaluating flag and reevaluationError', function() {
      const state = Object.freeze({
        reevaluating: true,
        reevaluationError: 'asdf',
        other: otherObject
      });
      const newState = reduce(state, {type: 'REEVALUATE_REPORT_FULFILLED'});
      expect(newState).toEqual({
        reevaluating: false,
        reevaluationError: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REEVALUATE_REPORT_CANCELLED action', function() {
    it('unsets reevaluating flag and reevaluationError', function() {
      const state = Object.freeze({
        reevaluating: true,
        reevaluationError: 'asdf',
        other: otherObject
      });
      const newState = reduce(state, {type: 'REEVALUATE_REPORT_CANCELLED'});
      expect(newState).toEqual({
        reevaluating: false,
        reevaluationError: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REEVALUATE_REPORT_FAILED action', function() {
    it('unsets reevaluating flag and sets the reevaluationError to the payload', function() {
      const state = Object.freeze({
        reevaluating: true,
        reevaluationError: null,
        other: otherObject
      });
      const payload = 'Error!';
      const newState = reduce(state, { type: 'REEVALUATE_REPORT_FAILED', payload });
      expect(newState).toEqual({
        reevaluating: false,
        reevaluationError: payload,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_FAILED action', function() {
    it('unsets loading flag and sets error value', function() {
      const state = Object.freeze({
        loading: true,
        loadError: null,
        selectedReport: null,
        other: otherObject
      });
      const newState = reduce(state, {type: 'LOAD_REPORT_FAILED', payload: 'test error'});
      expect(newState).toEqual({
        loading: false,
        loadError: 'test error',
        selectedReport: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SELECT_COMPONENT action', function() {
    it('sets selectedComponentIndex to payload', function() {
      const state = Object.freeze({
        selectedComponentIndex: null,
        other: otherObject
      });
      const newState = reduce(state, {type: 'SELECT_COMPONENT', payload: 42});
      expect(newState).toEqual({
        selectedComponentIndex: 42,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SET_AGGREGATE_REPORT_ENTRIES', function() {
    it('sets the aggregate flag from the payload', function() {
      const state = Object.freeze({
            aggregate: false,
            other: otherObject
          }),
          action = { type: 'SET_AGGREGATE_REPORT_ENTRIES', payload: true },
          newState = reduce(state, action);

      expect(newState).toEqual({
        aggregate: true,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject);
    });

    it('updates the displayedEntries in the selectedReport', function() {
      const entries = [{
            hash: '6',
            policyThreatLevel: 10,
            policyName: 'P1',
            grandfathered: false,
            waived: false,
            derivedViolationState: 'open'
          }, {
            hash: '4',
            policyThreatLevel: 8,
            policyName: 'P2',
            grandfathered: false,
            waived: true,
            displayName: {parts: []},
            derivedViolationState: 'open'
          }, {
            hash: '1',
            policyThreatLevel: 6,
            policyName: 'P3',
            grandfathered: false,
            waived: true,
            displayName: {parts: []},
            derivedViolationState: 'open'
          }, {
            hash: '5',
            policyThreatLevel: 4,
            policyName: 'P4',
            grandfathered: false,
            waived: false,
            derivedViolationState: 'open'
          }, {
            hash: '2',
            policyThreatLevel: 3,
            policyName: 'P5',
            grandfathered: false,
            waived: false,
            derivedViolationState: 'open'
          }, {
            hash: '1',
            policyThreatLevel: 1,
            policyName: 'P6',
            grandfathered: false,
            waived: false,
            derivedViolationState: 'open'
          }],
          state = Object.freeze({
            selectedReport: {
              allEntries: entries,
              displayedEntries: entries
            },
            aggregate: false,
            sortFields: ['-policyThreatLevel']
          }),
          newState = reduce(state, {
            type: 'SET_AGGREGATE_REPORT_ENTRIES',
            payload: true
          });

      expect(newState.selectedReport.displayedEntries).toEqual([{
        hash: '6',
        policyThreatLevel: 10,
        policyName: 'P1',
        grandfathered: false,
        waived: false,
        derivedViolationState: 'open'
      }, {
        hash: '5',
        policyThreatLevel: 4,
        policyName: 'P4',
        grandfathered: false,
        waived: false,
        derivedViolationState: 'open'
      }, {
        hash: '2',
        policyThreatLevel: 3,
        policyName: 'P5',
        grandfathered: false,
        waived: false,
        derivedViolationState: 'open'
      }, {
        hash: '1',
        policyThreatLevel: 1,
        policyName: 'P6',
        grandfathered: false,
        waived: false,
        derivedViolationState: 'open'
      }, {
        hash: '4',
        policyThreatLevel: 0,
        policyName: 'None',
        waived: true,
        grandfathered: false,
        displayName: {parts: []},
        derivedViolationState: 'waived'
      }]);
    });
  });

  describe('SET_SORTING', function() {
    it('sets the sorting fields from the payload', function() {
      const state = Object.freeze({
            sortFields: ['foo'],
            other: otherObject
          }),
          action = { type: 'SET_SORTING', payload: ['bar'] },
          newState = reduce(state, action);

      expect(newState).toEqual({
        sortFields: ['bar'],
        other: otherObject
      });
      expect(newState.other).toBe(otherObject);
    });

    it('sorts the displayedEntries in the selectedReport', function() {
      const entries = [{
            policyThreatLevel: 10
          }, {
            policyThreatLevel: 5
          }, {
            policyThreatLevel: 1
          }],
          state = Object.freeze({
            selectedReport: {
              allEntries: entries,
              displayedEntries: entries
            },
            aggregate: false,
            sortFields: ['-policyThreatLevel']
          }),
          newState = reduce(state, {
            type: 'SET_SORTING',
            payload: ['policyThreatLevel']
          });

      expect(newState.selectedReport.displayedEntries).toEqual([{
        policyThreatLevel: 1
      }, {
        policyThreatLevel: 5
      }, {
        policyThreatLevel: 10
      }]);
    });
  });

  describe('SET_EXACT_VALUE_FILTER', function() {
    it('sets the specified property on the exactValueFilters to the specified value', function() {
      const otherFieldFilter = new Set(['asdf']),
          fooFieldFilter = new Set(['bar']),
          state = Object.freeze({
            exactValueFilters: Object.freeze({
              otherField: otherFieldFilter
            }),
            other: otherObject
          }),
          action = {
            type: 'SET_EXACT_VALUE_FILTER',
            payload: {
              fieldName: 'fooField',
              allowedValues: fooFieldFilter
            }
          },
          newState = reduce(state, action);

      expect(newState).toEqual({
        exactValueFilters: {
          fooField: fooFieldFilter,
          otherField: otherFieldFilter
        },
        other: otherObject
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on the resulting exactValueFilters', function() {
      const state = Object.freeze({
            exactValueFilters: Object.freeze({
              otherField: new Set(['asdf'])
            }),
            selectedReport: Object.freeze({
              allEntries: Object.freeze([{
                fooField: 'bar'
              }, {
                fooField: 'bar',
                otherField: 'asdf'
              }, {
                fooField: 'bar',
                otherField: 'baz'
              }, {
                fooField: 'asdf',
                otherField: 'asdf'
              }, {
                fooField: 'baz',
                otherField: 'asdf'
              }, {
                fooField: 'bar',
                otherField: 'asdf'
              }])
            })
          }),
          action = {
            type: 'SET_EXACT_VALUE_FILTER',
            payload: {
              fieldName: 'fooField',
              allowedValues: new Set(['bar', 'baz'])
            }
          },
          newState = reduce(state, action);

      expect(newState.selectedReport.displayedEntries).toEqual([{
        fooField: 'bar',
        otherField: 'asdf'
      }, {
        fooField: 'baz',
        otherField: 'asdf'
      }, {
        fooField: 'bar',
        otherField: 'asdf'
      }]);

      expect(newState.selectedReport.allEntries).toBe(state.selectedReport.allEntries);
    });
  });

  describe('SET_SUBSTRING_FIELD_FILTER', function() {
    it('sets the specified property on the substringFilters to the specified value', function() {
      const state = Object.freeze({
            substringFilters: Object.freeze({
              otherField: 'asdf'
            }),
            other: otherObject
          }),
          action = {
            type: 'SET_SUBSTRING_FIELD_FILTER',
            payload: {
              fieldName: 'fooField',
              filterString: 'bar'
            }
          },
          newState = reduce(state, action);

      expect(newState).toEqual({
        substringFilters: {
          fooField: 'bar',
          otherField: 'asdf'
        },
        other: otherObject
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on the resulting substringFilters', function() {
      const state = Object.freeze({
            substringFilters: Object.freeze({
              otherField: 'asdf'
            }),
            selectedReport: Object.freeze({
              allEntries: Object.freeze([{
                otherField: 'asdfasdf',
                fooField: 'qwerty'
              }, {
                otherField: 'asdfasdf',
                fooField: 'bar'
              }, {
                otherField: '',
                fooField: 'bar'
              }, {
                otherField: 'asdfasdf',
                fooField: ''
              }, {
                otherField: 'dfasdfas',
                fooField: 'foobarbaz'
              }, {
                otherField: 'bar',
                fooField: 'asdf'
              }])
            })
          }),
          action = {
            type: 'SET_SUBSTRING_FIELD_FILTER',
            payload: {
              fieldName: 'fooField',
              filterString: 'bar'
            }
          },
          newState = reduce(state, action);

      expect(newState.selectedReport.displayedEntries).toEqual([{
        otherField: 'asdfasdf',
        fooField: 'bar'
      }, {
        otherField: 'dfasdfas',
        fooField: 'foobarbaz'
      }]);

      expect(newState.selectedReport.allEntries).toBe(state.selectedReport.allEntries);
    });
  });

  describe('RESET_REPORT_VIEW_SETTINGS', function() {
    it('resets exactValueFilters, substringFilters, aggregate, and sortFields back to their initial values', () => {
      const state = Object.freeze({
            exactValueFilters: Object.freeze({
              bar: [1, 2],
              baz: ['asdfasf']
            }),
            substringFilters: Object.freeze({
              foo: 'asdf'
            }),
            aggregate: false,
            sortFields: ['derivedComponentName'],
            other: otherObject
          }),
          action = {
            type: 'RESET_REPORT_VIEW_SETTINGS'
          },
          newState = reduce(state, action);

      expect(newState).toEqual({
        exactValueFilters: {},
        substringFilters: {},
        aggregate: true,
        sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
        other: otherObject
      });

      expect(newState.other).toBe(otherObject);
    });
  });
});
