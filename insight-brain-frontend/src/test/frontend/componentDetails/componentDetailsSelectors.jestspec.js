/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectApplicationReportSlice } from 'MainRoot/applicationReport/applicationReportSelectors';
import {
  selectDetails,
  selectComponentDetails,
  selectComponentName,
  selectComponentPagination,
  selectComponentSimilarMatches,
  selectComponentViolations,
  selectApplicableLabels,
  selectLabels,
  selectLoadError,
  selectIsApplicableLabelsLoading,
  selectIsLabelsLoading,
  selectShowMatchersPopover,
  selectSetProprietaryMatchers,
  selectFilteredPathnames,
  selectApplicationInfo,
  selectComponentDetailsLoading,
  selectComponentDetailsLoadErrors,
  selectSelectedLabelDetails,
  selectShowRemoveLabelModal,
  selectRemoveAppliedLabelError,
  selectComponentIdentificationSource,
  selectIsProprietary,
  selectSaveLabelError,
  selectDependencyTreeSubset,
} from 'MainRoot/componentDetails/componentDetailsSelectors';

describe('componentDetailsSelectors', () => {
  const mockState = {
    router: {
      currentParams: {
        hash: 'some-component-hash',
      },
      currentState: {
        name: 'router-state-name',
      },
    },
    applicationReport: {
      metadata: {
        application: {
          name: 'The App',
          publicId: 'TheApp',
          organization: {
            name: 'The Org',
          },
        },
        reportTime: 1623135382098,
        reportTitle: 'Title of Report',
        stageId: 'test',
      },
      selectedReport: {
        allEntries: [
          {
            hash: 'some-component-hash',
            matchState: 'unknown',
            pathnames: ['dependency:/this.is.a.dependency', 'pathname 1', 'pathname 2'],
            proprietary: true,
            identificationSource: 'Sonatype',
            derivedComponentName: 'My Component',
            filename: 'My Component',
          },
          {
            hash: 'some-component-hash',
            policyThreatLevel: 9,
          },
          {
            derivedComponentName: 'My Component',
            hash: 'some-component-hash',
            componentIdentifier: { format: 'maven' },
            derivedDependencyType: 'transitive',
            policyThreatLevel: 10,
          },
          {
            hash: 'another-component-hash',
            derivedComponentName: 'Component2',
            filename: 'Component2',
          },
          {
            hash: 'and-another-component-hash',
          },
          {
            hash: 'and-another-component-hash-bites-the-dust',
          },
          {
            hash: 'some-innersource-parent-hash',
            componentIdentifier: { format: 'maven', coordinates: 'direct1' },
            derivedComponentName: 'innersource-parent',
            innerSource: true,
          },
          {
            hash: 'some-parent-hash',
            componentIdentifier: { format: 'maven', coordinates: 'direct2' },
            derivedComponentName: 'parent',
            innerSource: false,
          },
          {
            hash: 'some-other-parent-hash',
            componentIdentifier: { format: 'maven', coordinates: 'direct3' },
            derivedComponentName: 'other-parent',
          },
          {
            hash: 'some-child-hash',
            componentIdentifier: { format: 'maven', coordinates: 'transitive' },
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'maven',
                  coordinates: 'direct1',
                },
                {
                  format: 'maven',
                  coordinates: 'direct2',
                },
                {
                  format: 'maven',
                  coordinates: 'direct3',
                },
              ],
            },
          },
        ],
        displayedEntries: [
          {
            hash: 'a-component-hash',
          },
          {
            derivedComponentName: 'My Component',
            hash: 'some-component-hash',
            componentIdentifier: { format: 'maven' },
            derivedDependencyType: 'transitive',
            matchState: 'unknown',
            pathnames: ['dependency:/this.is.a.dependency', 'pathname 1', 'pathname 2'],
            proprietary: true,
            identificationSource: 'Sonatype',
          },
          {
            hash: 'another-component-hash',
            derivedComponentName: 'Component2',
          },
          {
            hash: 'some-innersource-parent-hash',
            componentIdentifier: { format: 'maven', coordinates: 'direct1' },
            derivedComponentName: 'innersource-parent',
            innerSource: true,
          },
          {
            hash: 'some-parent-hash',
            componentIdentifier: { format: 'maven', coordinates: 'direct2' },
            derivedComponentName: 'parent',
            innerSource: false,
          },
          {
            hash: 'some-other-parent-hash',
            componentIdentifier: { format: 'maven', coordinates: 'direct3' },
            derivedComponentName: 'other-parent',
          },
          {
            hash: 'some-child-hash',
            componentIdentifier: { format: 'maven', coordinates: 'transitive' },
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'maven',
                  coordinates: 'direct1',
                },
                {
                  format: 'maven',
                  coordinates: 'direct2',
                },
                {
                  format: 'maven',
                  coordinates: 'direct3',
                },
              ],
            },
          },
        ],
        aggregatedEntries: [
          {
            hash: 'a-component-hash',
          },
          {
            hash: 'some-component-hash',
          },
          {
            hash: 'another-component-hash',
          },
        ],
      },
      pendingLoads: new Set([]),
    },
    componentDetails: {
      applicableLabels: [],
      labels: [],
      loadError: false,
      removeAppliedLabelError: null,
      selectedLabelDetails: {},
      showRemoveLabelModal: false,
      pendingLoads: new Set(['test']),
      showMatchersPopover: null,
      setProprietaryMatchers: {
        submitMaskState: true,
        submitError: 'Some crazy error',
        data: { pathnames: ['pathname 1', 'pathname 2'], regex: 'OMG' },
      },
    },
  };

  describe('selectComponentDetails', () => {
    it('derives componentDetails from the componentDetails, selectedReport metadata, and the selectedComponent', () => {
      const allEntries = [
        {
          derivedComponentName: 'My Component',
          filename: 'My Component',
          hash: 'some-component-hash',
          componentIdentifier: { format: 'maven' },
          derivedDependencyType: 'transitive',
          matchState: 'unknown',
          pathnames: ['dependency:/this.is.a.dependency', 'pathname 1', 'pathname 2'],
          proprietary: true,
          identificationSource: 'Sonatype',
          variantSelected: true,
        },
      ];

      const updatedMockState = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          selectedReport: {
            allEntries,
          },
        },
      };

      const expected = {
        name: 'My Component',
        hash: 'some-component-hash',
        componentIdentifier: { format: 'maven' },
        dependencyType: 'transitive',
        isInnerSource: false,
        format: 'maven',
        metadata: {
          applicationName: 'The App',
          organizationName: 'The Org',
          reportTime: 1623135382098,
          reportTitle: 'Title of Report',
        },
        labels: [],
        matchState: 'unknown',
        identificationSource: 'Sonatype',
        variantSelected: true,
      };

      const actual = selectComponentDetails(updatedMockState);
      expect(actual).toEqual(expected);
    });
  });

  describe('selectShowMatchersPopover', () => {
    it('selects the slice of state for showMatchersPopover', () => {
      const actual = selectShowMatchersPopover(mockState);
      expect(actual).toBe(null);
    });
  });

  describe('selectSetProprietaryMatchers', () => {
    it('selects the slice of state for setProprietaryMatchers', () => {
      const actual = selectSetProprietaryMatchers(mockState);
      expect(actual).toEqual({
        submitMaskState: true,
        submitError: 'Some crazy error',
        data: { pathnames: ['pathname 1', 'pathname 2'], regex: 'OMG' },
      });
    });
  });

  describe('selectFilteredPathnames', () => {
    it('selects the slice of state for the pathnames for the selected component', () => {
      const actual = selectFilteredPathnames(mockState);
      expect(actual).toEqual(['pathname 1', 'pathname 2']);
    });
  });

  describe('selectApplicationInfo', () => {
    it('selects the slice of state for the application stored in the metadata', () => {
      const actual = selectApplicationInfo(mockState);
      expect(actual).toEqual({
        applicationName: 'The App',
        applicationId: 'TheApp',
        stageId: 'test',
      });
    });
  });

  describe('selectIsProprietary', () => {
    it('selects the slice of state for the application stored in the metadata', () => {
      const actual = selectIsProprietary(mockState);
      expect(actual).toBe(true);
    });
  });

  describe('selectComponentPagination', function () {
    // we need to use the uiRouterState from props because it is passed by context
    // so we mock it here and pass it in as the props second selector argument
    // the router state is used to determine the HREF of next and prev components
    const uiRouterStateService = {
      href(routeName, { hash }) {
        return `${routeName}.${hash}`;
      },
    };

    it('derives pagination from selected report component list, selectedComponentIndex and currentRouteName', () => {
      const expected = {
        next: 'router-state-name.another-component-hash',
        prev: 'router-state-name.a-component-hash',
        currentPage: 2,
        pageCount: 3,
      };
      const actual = selectComponentPagination(mockState, uiRouterStateService);
      expect(actual).toEqual(expected);
    });

    it('returns a prev property of null when there are no components before the current in the list', () => {
      const state = {
        ...mockState,
        router: {
          ...mockState.router,
          currentParams: {
            hash: 'a-component-hash',
          },
        },
      };
      const expected = {
        next: 'router-state-name.some-component-hash',
        prev: null,
        currentPage: 1,
        pageCount: 3,
      };
      const actual = selectComponentPagination(state, uiRouterStateService);
      expect(actual).toEqual(expected);
    });

    it('returns a next property of null when there is no more components after the current in the list', () => {
      const state = {
        ...mockState,
        router: {
          ...mockState.router,
          currentParams: {
            hash: 'another-component-hash',
          },
        },
      };
      const expected = {
        next: null,
        prev: 'router-state-name.some-component-hash',
        currentPage: 3,
        pageCount: 3,
      };
      const actual = selectComponentPagination(state, uiRouterStateService);
      expect(actual).toEqual(expected);
    });
  });

  describe('selectComponentViolations', () => {
    it(
      'derives the violations for the selected component from the entries in the selected report, ' +
        'only for those that have a policy threat level',
      () => {
        const expected = [
          {
            hash: 'some-component-hash',
            policyThreatLevel: 9,
          },
          {
            derivedComponentName: 'My Component',
            hash: 'some-component-hash',
            componentIdentifier: { format: 'maven' },
            derivedDependencyType: 'transitive',
            policyThreatLevel: 10,
          },
        ];
        const actual = selectComponentViolations(mockState);
        expect(actual).toEqual(expected);
      }
    );
  });

  describe('selectComponentName', () => {
    it('returns the name prop of the selected component', () => {
      let actual;
      actual = selectComponentName(mockState);
      expect(actual).toEqual('My Component');

      const newState = {
        ...mockState,
        router: {
          ...mockState.router,
          currentParams: {
            hash: 'another-component-hash',
          },
        },
      };
      actual = selectComponentName(newState);
      expect(actual).toEqual('Component2');
    });
  });

  describe('selectComponentSimilarMatches', () => {
    it('returns an empty array when the matchState is exact', () => {
      const exactMatchDisplayedEntries = [
        {
          hash: 'hash1',
          matchState: 'exact',
          matchDetails: ['match'],
        },
      ];
      const mockStateForMatchState = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          selectedReport: {
            displayedEntries: exactMatchDisplayedEntries,
          },
        },
        router: {
          ...mockState.router,
          currentParams: {
            hash: 'hash1',
          },
        },
      };

      const selection = selectComponentSimilarMatches(mockStateForMatchState);
      expect(selection).toEqual([]);
    });

    it('returns an empty array when the matchState is unknown', () => {
      const unknownMatchDisplayedEntries = [
        {
          derivedComponentName: 'My Component',
          hash: 'hash2',
          componentIdentifier: { format: 'maven' },
          derivedDependencyType: 'transitive',
          matchState: 'unknown',
          matchDetails: ['match'],
        },
      ];
      const mockStateForMatchState = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          selectedReport: {
            displayedEntries: unknownMatchDisplayedEntries,
          },
        },
        router: {
          ...mockState.router,
          currentParams: {
            hash: 'hash2',
          },
        },
      };

      const selection = selectComponentSimilarMatches(mockStateForMatchState);
      expect(selection).toEqual([]);
    });

    it('returns the matchDetails of the component information when the matchState is similar', () => {
      const similarMatchDisplayedEntries = [
        {
          derivedComponentName: 'My Component',
          hash: 'hash3',
          componentIdentifier: { format: 'maven' },
          derivedDependencyType: 'transitive',
          matchState: 'similar',
          matchDetails: ['bestMatch', 'otherMatch'],
        },
      ];
      const mockStateForMatchState = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          selectedReport: {
            allEntries: similarMatchDisplayedEntries,
          },
        },
        router: {
          ...mockState.router,
          currentParams: {
            hash: 'hash3',
          },
        },
      };

      const selection = selectComponentSimilarMatches(mockStateForMatchState);
      expect(selection).toEqual(['bestMatch', 'otherMatch']);
    });
  });

  describe('selectApplicableLabels', () => {
    it('returns state applicableLabels', () => {
      expect(
        selectApplicableLabels({
          ...mockState,
          componentDetails: {
            applicableLabels: [{ label: 'Test z' }, { label: 'Test f' }, { label: 'Test a' }],
            labels: [],
            loadError: false,
            pendingLoads: new Set(),
          },
        })
      ).toEqual([{ label: 'Test z' }, { label: 'Test f' }, { label: 'Test a' }]);
    });
  });

  describe('selectLabels', () => {
    it('returns state labels', () => {
      expect(selectLabels(mockState)).toEqual([]);
    });
  });

  describe('selectLoadError', () => {
    it('returns state load error', () => {
      expect(selectLoadError(mockState)).toEqual(false);
    });
  });

  describe('selectIsApplicableLabelsLoading', () => {
    it('returns boolean flag searching if applicableLabels is in pending loads set', () => {
      expect(
        selectIsApplicableLabelsLoading({
          ...mockState,
          componentDetails: {
            applicableLabels: [],
            labels: [],
            loadError: false,
            pendingLoads: new Set(['applicableLabels']),
          },
        })
      ).toEqual(true);
    });
  });

  describe('selectIsLabelsLoading', () => {
    it('returns boolean flag searching if applicableLabels is in pending loads set', () => {
      expect(
        selectIsLabelsLoading({
          ...mockState,
          componentDetails: {
            applicableLabels: [],
            labels: [],
            loadError: false,
            pendingLoads: new Set(['labels']),
          },
        })
      ).toEqual(true);
    });
  });

  describe('selectComponentDetailsLoading', () => {
    it('is composed of the following selectors', () => {
      expect(selectComponentDetailsLoading.dependencies).toEqual([
        selectApplicationReportSlice,
        selectIsLabelsLoading,
        selectComponentDetails,
      ]);
    });
    it('returns true if the application report is loading', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          pendingLoads: new Set(['common']),
        },
      };
      const actual = selectComponentDetailsLoading(state);
      expect(actual).toEqual(true);
    });
    it('returns true if the labels are loading', () => {
      const state = {
        ...mockState,
        componentDetails: {
          pendingLoads: new Set(['labels']),
        },
      };
      const actual = selectComponentDetailsLoading(state);
      expect(actual).toEqual(true);
    });
    it('returns true if there are no component details in the state', () => {
      const state = {
        ...mockState,
        router: {
          ...mockState.router,
          currentParams: {
            hash: 'new-hash',
          },
        },
      };
      const actual = selectComponentDetailsLoading(state);
      expect(actual).toEqual(true);
    });
    it('returns false if app report & labels & component details are loaded', () => {
      const state = {
        ...mockState,
        componentDetails: {
          pendingLoads: new Set([]),
        },
        applicationReport: {
          ...mockState.applicationReport,
          pendingLoads: new Set([]),
        },
      };
      const actual = selectComponentDetailsLoading(state);
      expect(actual).toEqual(false);
    });
  });

  describe('selectComponentDetailsLoadErrors', () => {
    it('is composed of the following selectors', () => {
      expect(selectComponentDetailsLoadErrors.dependencies).toEqual([selectDetails, selectApplicationReportSlice]);
    });
    it('returns the error present in the applicationReport slice', () => {
      const state = {
        componentDetails: { loadError: null },
        applicationReport: { loadError: 'app-report-error' },
      };
      const actual = selectComponentDetailsLoadErrors(state);
      expect(actual).toEqual('app-report-error');
    });
    it('returns the error present in the componentDetails slice', () => {
      const state = {
        componentDetails: { loadError: 'component-details-error' },
        applicationReport: { loadError: null },
      };
      const actual = selectComponentDetailsLoadErrors(state);
      expect(actual).toEqual('component-details-error');
    });
    it('returns null if there are no errors in the state slices', () => {
      const state = {
        componentDetails: { loadError: null },
        applicationReport: { loadError: null },
      };
      const actual = selectComponentDetailsLoadErrors(state);
      expect(actual).toBeNull();
    });
  });

  describe('selectSelectedLabelDetails', () => {
    it('returns selectedLabelDetails', () => {
      expect(
        selectSelectedLabelDetails({
          ...mockState,
          componentDetails: { ...mockState.componentDetails, selectedLabelDetails: { test: 'test' } },
        })
      ).toEqual({ test: 'test' });
    });
  });

  describe('selectShowRemoveLabelModal', () => {
    it('returns showRemoveLabelModal (true)', () => {
      expect(
        selectShowRemoveLabelModal({
          ...mockState,
          componentDetails: { ...mockState.componentDetails, showRemoveLabelModal: true },
        })
      ).toEqual(true);
    });

    it('returns showRemoveLabelModal (false)', () => {
      expect(
        selectShowRemoveLabelModal({
          ...mockState,
          componentDetails: { ...mockState.componentDetails, showRemoveLabelModal: false },
        })
      ).toEqual(false);
    });
  });

  describe('selectRemoveAppliedLabelError', () => {
    it('returns removeAppliedLabelError', () => {
      expect(
        selectRemoveAppliedLabelError({
          ...mockState,
          componentDetails: { ...mockState.componentDetails, removeAppliedLabelError: 'error' },
        })
      ).toEqual('error');
    });
  });

  describe('selectComponentIdentificationSource', () => {
    it('derives identificationSource', () => {
      const actual = selectComponentIdentificationSource(mockState);
      expect(actual).toEqual('Sonatype');
    });
  });

  describe('selectSaveLabelError', () => {
    it('is composed of the following selectors', () => {
      expect(selectSaveLabelError.dependencies).toEqual([selectDetails]);
    });

    it('returns the selectSaveLabelError present in the componentDetails slice', () => {
      const state = {
        componentDetails: { saveLabelScopeError: 'save-label-error' },
      };
      const actual = selectSaveLabelError(state);
      expect(actual).toEqual('save-label-error');
    });

    it('returns null if there are no errors in the state slices', () => {
      const state = {
        componentDetails: { saveLabelScopeError: null },
      };
      const actual = selectSaveLabelError(state);
      expect(actual).toBeNull();
    });
  });

  describe('selectDependencyTreeSubset', () => {
    it('is composed of the following selector', () => {
      expect(selectDependencyTreeSubset.dependencies).toEqual([selectDetails]);
    });

    it('returns subset', () => {
      const componentDetails = { dependencyTreeSubset: ['testSubset'] };

      const selection = selectDependencyTreeSubset.resultFunc(componentDetails);

      expect(selection).toEqual(['testSubset']);
    });
  });
});
