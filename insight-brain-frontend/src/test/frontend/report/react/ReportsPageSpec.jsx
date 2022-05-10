/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import ReportsPage from 'MainRoot/report/react/ReportsPage';
import * as reportsSelectors from 'MainRoot/report/react/reportsSelectors';
import { actions as reportsActions, RESULTS_PER_PAGE } from 'MainRoot/report/react/reportsSlice';
import axios from 'axios';
import { getActionStageUrl, getApplicationSummariesUrl, getApplicationSummaryUrl } from 'MainRoot/util/CLMLocation';

function generateArrayOfNumbers(start = 0, end = 1) {
  return Array.apply(0, Array(end)).map((_, b) => b + start);
}

describe('ReportsPage', () => {
  let renderComponent,
    loadStagesAndReportsSpy,
    loadMoreSpy,
    reportsSlice,
    mockAxiosCalls,
    sortReportsSpy,
    filterReportsSpy,
    selectHasMoreReportsSpy,
    selectIsLoadingSpy,
    selectReportsLoadErrorSpy,
    selectApplicationsInformationListSpy,
    selectAppliedSortReportsSpy,
    selectReportsFilterSpy,
    loadContactNameSpy,
    loadReportsSpy;

  reportsSlice = {
    stages: [
      {
        stageTypeId: 'proxy',
        stageName: 'Proxy',
      },
      {
        stageTypeId: 'develop',
        stageName: 'Develop',
      },
      {
        stageTypeId: 'source',
        stageName: 'Source',
      },
      {
        stageTypeId: 'build',
        stageName: 'Build',
      },
      {
        stageTypeId: 'stage-release',
        stageName: 'Stage Release',
      },
      {
        stageTypeId: 'release',
        stageName: 'Release',
      },
      {
        stageTypeId: 'operate',
        stageName: 'Operate',
      },
    ],
    loading: false,
    loadError: null,
    applicationsInformationList: [
      {
        id: '137a638c1a474963b464a81e50be9fe4',
        publicId: 'CDPAPPGO',
        name: 'Test app',
        organizationId: 'fce7dfaa30b047d585190ac632a07bfd',
        organizationName: 'CDP Go',
        policyEvaluations: {
          build: {
            id: '1c09f8e97e9247cfb37778462efbc205',
            applicationId: '137a638c1a474963b464a81e50be9fe4',
            stageTypeId: 'build',
            scanId: '4d8ad4f41b7d46a79d95e1359d40b861',
            time: 1636403882656,
            commitHash: null,
            initiator: 'admin',
            scanTriggerType: 'WEB_UI',
            forObsoleteScan: false,
            reevaluation: false,
            forMonitoring: false,
          },
        },
        policyEvaluationsResults: {
          build: {
            alerts: [],
            affectedComponentCount: 664,
            criticalComponentCount: 649,
            severeComponentCount: 13,
            moderateComponentCount: 2,
            criticalPolicyViolationCount: 709,
            severePolicyViolationCount: 152,
            moderatePolicyViolationCount: 7,
            grandfatheredPolicyViolationCount: 0,
            totalComponentCount: 1157,
          },
        },
        contact: {
          internalName: 'admin',
          displayName: null,
          email: null,
          realm: null,
          error: null,
        },
        hasPendingSourceControlPolicyEvaluation: false,
      },
      {
        id: '2848e223a42f47f5bb0badb9cb7dd318',
        publicId: 'TestAppId2',
        name: 'Test app 2',
        organizationId: '07c6b15352f741af9753becc301b6e52',
        organizationName: 'Test org 2',
        policyEvaluations: {
          build: {
            id: '85fa014c346b4df5a931252f9bb4023e',
            applicationId: '2848e223a42f47f5bb0badb9cb7dd318',
            stageTypeId: 'build',
            scanId: 'b4ce6166e00547658cab30bf2beee877',
            time: 1623954494055,
            commitHash: null,
            initiator: 'admin',
            scanTriggerType: 'WEB_UI',
            forObsoleteScan: false,
            reevaluation: true,
            forMonitoring: false,
          },
        },
        policyEvaluationsResults: {
          build: {
            alerts: [],
            affectedComponentCount: 14,
            criticalComponentCount: 8,
            severeComponentCount: 5,
            moderateComponentCount: 1,
            criticalPolicyViolationCount: 8,
            severePolicyViolationCount: 21,
            moderatePolicyViolationCount: 2,
            grandfatheredPolicyViolationCount: 0,
            totalComponentCount: 28,
          },
        },
        contact: {
          internalName: 'otheruser',
          displayName: null,
          email: null,
          realm: null,
          error: null,
        },
        hasPendingSourceControlPolicyEvaluation: false,
      },
      {
        id: 'a498c91045658f9b2ea3fbfb6a5dc555',
        publicId: 'TestAppId3',
        name: 'Test app 3',
        organizationId: '5b4753f38c1dec217ff4a5e752195f68',
        organizationName: 'Test org 3',
        policyEvaluations: {
          build: {
            id: '66c435efe5ae59e45a9891f4ee544be1',
            applicationId: 'a498c91045658f9b2ea3fbfb6a5dc555',
            stageTypeId: 'build',
            scanId: 'e9e5fc1208d2fb495b867fa208db0464',
            time: 1650384168484,
            commitHash: null,
            initiator: 'admin',
            scanTriggerType: 'WEB_UI',
            forObsoleteScan: false,
            reevaluation: true,
            forMonitoring: false,
          },
        },
        policyEvaluationsResults: {
          build: {
            alerts: [],
            affectedComponentCount: 37,
            criticalComponentCount: 1,
            severeComponentCount: 24,
            moderateComponentCount: 12,
            criticalPolicyViolationCount: 1,
            severePolicyViolationCount: 10,
            moderatePolicyViolationCount: 5,
            grandfatheredPolicyViolationCount: 0,
            totalComponentCount: 51,
          },
        },
        contact: {
          internalName: 'deleted user',
          displayName: null,
          email: null,
          realm: null,
          error: null,
        },
        hasPendingSourceControlPolicyEvaluation: false,
      },
    ],
    appFilter: '',
    pages: 2,
    hasMoreResults: true,
    loadingError: null,
    appliedSort: null,
    loadingPublicIds: new Set(),
  };

  beforeEach(() => {
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

    selectReportsLoadErrorSpy = spyOn(reportsSelectors, 'selectReportsLoadError').and.callThrough();
    selectHasMoreReportsSpy = spyOn(reportsSelectors, 'selectHasMoreReports').and.callThrough();
    selectIsLoadingSpy = spyOn(reportsSelectors, 'selectReportsLoading').and.callThrough();
    selectApplicationsInformationListSpy = spyOn(
      reportsSelectors,
      'selectApplicationsInformationList'
    ).and.callThrough();
    selectAppliedSortReportsSpy = spyOn(reportsSelectors, 'selectAppliedSortReports').and.callThrough();
    selectReportsFilterSpy = spyOn(reportsSelectors, 'selectReportsFilter').and.callThrough();

    loadStagesAndReportsSpy = spyOn(reportsActions, 'loadStagesAndReports').and.callThrough();
    loadMoreSpy = spyOn(reportsActions, 'loadMore').and.callThrough();
    loadReportsSpy = spyOn(reportsActions, 'loadReports').and.callThrough();
    loadContactNameSpy = spyOn(reportsActions, 'loadContactName').and.callThrough();
    sortReportsSpy = spyOn(reportsActions, 'sortReports').and.callThrough();
    filterReportsSpy = spyOn(reportsActions, 'filterReports').and.callThrough();

    mockAxiosCalls({
      get: {
        ...generateArrayOfNumbers(1, 20)
          .map((pages) => ({
            key: getApplicationSummariesUrl('', 'APP_NAME_ASC', pages, RESULTS_PER_PAGE),
            value: Promise.resolve({ data: reportsSlice.applicationsInformationList }),
          }))
          .reduce((p, c) => ({ ...p, [c.key]: c.value }), {}),
        [getActionStageUrl()]: Promise.resolve({ data: reportsSlice.stages }),
        [getApplicationSummaryUrl('CDPAPPGO')]: Promise.resolve({
          data: {
            publicId: 'CDPAPPGO',
            contact: {
              displayName: 'Admin user',
            },
          },
        }),
        [getApplicationSummaryUrl('TestAppId2')]: Promise.resolve({
          data: {
            publicId: 'TestAppId2',
            contact: {
              displayName: 'Other User with a very very very very very very very very very long name',
            },
          },
        }),
        [getApplicationSummaryUrl('TestAppId3')]: Promise.resolve({
          data: {
            publicId: 'TestAppId3',
            contact: {
              error: 'User not found',
            },
          },
        }),
      },
    });

    renderComponent = () => render(<ReportsPage />);
  });

  describe('when there are no violations', () => {
    beforeEach(() => {
      spyOn(reportsSelectors, 'selectReportsSlice').and.returnValue(reportsSlice);
      spyOn(reportsSelectors, 'selectReportsStages').and.returnValue(reportsSlice.stages);

      selectHasMoreReportsSpy.and.returnValue(true);
      selectIsLoadingSpy.and.returnValue(false);
      selectReportsLoadErrorSpy.and.returnValue(null);

      selectApplicationsInformationListSpy.and.returnValue([
        {
          id: '137a638c1a474963b464a81e50be9fe4',
          publicId: 'CDPAPPGO',
          name: 'Test app',
          organizationId: 'fce7dfaa30b047d585190ac632a07bfd',
          organizationName: 'CDP Go',
          policyEvaluations: {
            build: {
              id: '1c09f8e97e9247cfb37778462efbc205',
              applicationId: '137a638c1a474963b464a81e50be9fe4',
              stageTypeId: 'build',
              scanId: '4d8ad4f41b7d46a79d95e1359d40b861',
              time: 1636403882656,
              commitHash: null,
              initiator: 'admin',
              scanTriggerType: 'WEB_UI',
              forObsoleteScan: false,
              reevaluation: false,
              forMonitoring: false,
            },
          },
          policyEvaluationsResults: {
            build: {
              alerts: [],
              affectedComponentCount: 664,
              criticalComponentCount: 0,
              severeComponentCount: 0,
              moderateComponentCount: 0,
              criticalPolicyViolationCount: 709,
              severePolicyViolationCount: 152,
              moderatePolicyViolationCount: 7,
              grandfatheredPolicyViolationCount: 0,
              totalComponentCount: 1157,
            },
          },
          contact: {
            internalName: null,
            displayName: null,
            email: null,
            realm: null,
            error: null,
          },
          hasPendingSourceControlPolicyEvaluation: false,
        },
      ]);
      renderComponent = () => render(<ReportsPage />);
    });

    it('Renders page without violations and "No violations" message', () => {
      renderComponent();
      expect(screen.queryByText('No violations')).toBeVisible();
    });
  });

  describe('when there is no more reports', () => {
    beforeEach(() => {
      selectHasMoreReportsSpy.and.returnValue(false);
      renderComponent = () => render(<ReportsPage />);
    });

    it('Renders page without the load more results button', () => {
      renderComponent();
      expect(screen.queryByText('Load More Results')).not.toBeInTheDocument();
    });
  });

  describe('when the page is loading more reports', () => {
    beforeEach(() => {
      selectIsLoadingSpy.and.returnValue(true);
      renderComponent = () => render(<ReportsPage />);
    });

    it('Renders loading spinner', () => {
      renderComponent();
      expect(screen.queryByText('Loading…')).toBeVisible();
    });
  });

  describe('when the page has a loading error', () => {
    beforeEach(() => {
      selectReportsLoadErrorSpy.and.returnValue('Test error');
      renderComponent = () => render(<ReportsPage />);
    });

    it('Renders error section', () => {
      renderComponent();
      const retryButton = screen.queryByText('Retry');
      expect(screen.queryByText('An error occurred loading data. Test error')).toBeVisible();
      fireEvent.click(retryButton);
      expect(loadReportsSpy).toHaveBeenCalled();
    });
  });

  describe('when a report has a contact', () => {
    const findContact = (contact) => () => {
      return screen.getByText(contact);
    };

    it('Renders the contact after clicking the button "Show Contact"', (done) => {
      renderComponent();

      setTimeout(() => {
        const findFirstContact = findContact('Admin user');
        const findLastContact = findContact('Other User with a very very very very very very very very very long name');

        const [firstContactButton, nextContactButton] = screen.getAllByText('Show Contact');
        fireEvent.click(firstContactButton);
        fireEvent.click(nextContactButton);

        setTimeout(() => {
          expect(loadContactNameSpy).toHaveBeenCalledWith('TestAppId2');
          expect(loadContactNameSpy).toHaveBeenCalledTimes(2);
          expect(findFirstContact()).toBeVisible();
          expect(findLastContact()).toBeVisible();
          done();
        }, 10);
      }, 10);
    });

    it('Renders the error if the contact includes one', (done) => {
      renderComponent();

      setTimeout(() => {
        const [, , lastContactButton] = screen.getAllByText('Show Contact');
        fireEvent.click(lastContactButton);

        setTimeout(() => {
          expect(loadContactNameSpy).toHaveBeenCalledWith('TestAppId3');
          expect(loadContactNameSpy).toHaveBeenCalledTimes(1);
          expect(screen.getByText('An error occurred loading data. User not found')).toBeVisible();
          done();
        }, 10);
      }, 10);
    });

    it('Renders the error message and retry the request', (done) => {
      renderComponent();

      setTimeout(() => {
        const [, , lastContactButton] = screen.getAllByText('Show Contact');
        fireEvent.click(lastContactButton);

        setTimeout(() => {
          fireEvent.click(screen.getByText('Retry'));
          expect(loadContactNameSpy).toHaveBeenCalledTimes(2);
          done();
        }, 10);
      }, 10);
    });
  });

  describe('Load more and all elements', () => {
    beforeEach(() => {
      spyOn(reportsSelectors, 'selectReportsSlice').and.returnValue(reportsSlice);
      spyOn(reportsSelectors, 'selectReportsStages').and.returnValue(reportsSlice.stages);

      selectHasMoreReportsSpy.and.returnValue(true);
      selectIsLoadingSpy.and.returnValue(false);
      selectReportsLoadErrorSpy.and.returnValue(null);
      selectApplicationsInformationListSpy.and.returnValue(reportsSlice.applicationsInformationList);
    });

    it('Renders page with the load more results button', () => {
      renderComponent();

      const loadMoreButton = screen.getByText('Load More Results');

      expect(loadStagesAndReportsSpy).toHaveBeenCalled();
      expect(loadMoreButton).toBeVisible();

      fireEvent.click(loadMoreButton);

      expect(loadMoreSpy).toHaveBeenCalled();
    });

    it('Renders page with all elements on the table', () => {
      renderComponent();

      const applications = [
        {
          name: screen.getByText('Test app'),
          criticalPolicyViolationCount: screen.getByText('709'),
          severePolicyViolationCount: screen.getByText('152'),
          moderatePolicyViolationCount: screen.getByText('7'),
        },
        {
          name: screen.getByText('Test app 2'),
          criticalPolicyViolationCount: screen.getByText('8'),
          severePolicyViolationCount: screen.getByText('21'),
          moderatePolicyViolationCount: screen.getByText('2'),
        },
      ];

      expect(applications[0].name).toBeVisible();
      expect(applications[0].criticalPolicyViolationCount).toBeVisible();
      expect(applications[0].severePolicyViolationCount).toBeVisible();
      expect(applications[0].moderatePolicyViolationCount).toBeVisible();

      expect(applications[1].name).toBeVisible();
      expect(applications[1].criticalPolicyViolationCount).toBeVisible();
      expect(applications[1].severePolicyViolationCount).toBeVisible();
      expect(applications[1].moderatePolicyViolationCount).toBeVisible();
    });
  });

  describe('Sorting', () => {
    beforeEach(() => {
      spyOn(reportsSelectors, 'selectReportsSlice').and.returnValue(reportsSlice);
      spyOn(reportsSelectors, 'selectReportsStages').and.returnValue(reportsSlice.stages);

      selectReportsLoadErrorSpy.and.callFake(() => {});
      selectHasMoreReportsSpy.and.callFake(() => {});
      selectIsLoadingSpy.and.callFake(() => {});
      selectApplicationsInformationListSpy.and.callFake(() => {});
      selectAppliedSortReportsSpy.and.callFake(() => {});

      selectHasMoreReportsSpy.and.returnValue(true);
      selectIsLoadingSpy.and.returnValue(false);
      selectReportsLoadErrorSpy.and.returnValue(null);
      selectApplicationsInformationListSpy.and.returnValue(reportsSlice.applicationsInformationList);
    });

    describe('Renders a sort direction based on the given appliedSort prop', () => {
      const APP_SORT_FIELDS = ['name', '-name'];
      const ORG_SORT_FIELDS = ['organizationName', '-organizationName'];

      APP_SORT_FIELDS.forEach((field, index) => {
        const calledField = index === 0 ? APP_SORT_FIELDS[1] : APP_SORT_FIELDS[0];
        const sortDir = field === APP_SORT_FIELDS[0] ? 'Application ascending' : 'Application descending';
        it(`should sort with ${calledField} field`, () => {
          selectAppliedSortReportsSpy.and.returnValue(field);
          renderComponent();

          const sortAppButton = screen.queryByText('Application');
          const sortOrgButton = screen.queryByText('Organization');

          expect(sortAppButton.parentElement.title).toBe(sortDir);
          expect(sortOrgButton.parentElement.title).toBe('Organization unsorted');
        });
      });

      ORG_SORT_FIELDS.forEach((field, index) => {
        const calledField = index === 0 ? ORG_SORT_FIELDS[1] : ORG_SORT_FIELDS[0];
        const sortDir = field === ORG_SORT_FIELDS[0] ? 'Organization ascending' : 'Organization descending';
        it(`should sort with ${calledField} field`, () => {
          selectAppliedSortReportsSpy.and.returnValue(field);
          renderComponent();

          const sortOrgButton = screen.queryByText('Organization');
          const sortAppButton = screen.queryByText('Application');

          expect(sortOrgButton.parentElement.title).toBe(sortDir);
          expect(sortAppButton.parentElement.title).toBe('Application unsorted');
        });
      });
    });

    describe('Calls sortReports action with a given direction depending on the column', () => {
      const APP_SORT_FIELDS = ['name', '-name'];
      const ORG_SORT_FIELDS = ['organizationName', '-organizationName'];

      APP_SORT_FIELDS.forEach((field, index) => {
        const calledField = index === 0 ? APP_SORT_FIELDS[1] : APP_SORT_FIELDS[0];
        it(`should call sortReports action with ${calledField} when appliedSort is ${field}`, () => {
          selectAppliedSortReportsSpy.and.returnValue(field);
          renderComponent();

          const sortButton = screen.queryByText('Application');
          fireEvent.click(sortButton);

          expect(sortReportsSpy).toHaveBeenCalledWith(calledField);
        });
      });

      ORG_SORT_FIELDS.forEach((field, index) => {
        const calledField = index === 0 ? ORG_SORT_FIELDS[1] : ORG_SORT_FIELDS[0];
        it(`should call sortReports action with ${calledField} when appliedSort is ${field}`, () => {
          selectAppliedSortReportsSpy.and.returnValue(field);
          renderComponent();

          const sortButton = screen.queryByText('Organization');
          fireEvent.click(sortButton);

          expect(sortReportsSpy).toHaveBeenCalledWith(calledField);
        });
      });
    });
  });

  describe('Filter', () => {
    it(`should call filterReports with value`, () => {
      renderComponent();
      expect(selectReportsFilterSpy).toHaveBeenCalled();

      const inputFilter = screen.getByPlaceholderText('Search by application or organization name');
      const inputValue = 'test';
      fireEvent.change(inputFilter, { target: { value: inputValue } });

      expect(filterReportsSpy).toHaveBeenCalledWith(inputValue);
    });
  });
});
