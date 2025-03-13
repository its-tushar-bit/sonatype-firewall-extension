/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxTable } from '@sonatype/react-shared-components';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';
import LegalApplicationDetailsPage from '../../../../main/frontend/legal/application/LegalApplicationDetailsPage';
import LegalApplicationDetailsComponentRow from '../../../../main/frontend/legal/application/LegalApplicationDetailsComponentRow';
import LegalApplicationDetailsFilterContainer from '../../../../main/frontend/legal/application/filter/LegalApplicationDetailsFilterContainer';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

describe('LegalApplicationDetailsPage', function () {
  let minimalProps, fetchLegalApplicationDetailsDataSpy, stateSpy, toggleFilterSidebarSpy, getShallowComponent;

  beforeEach(function () {
    fetchLegalApplicationDetailsDataSpy = jasmine.createSpy('fetchLegalApplicationDetailsData');
    stateSpy = jasmine.createSpyObj('$state', ['get', 'href']);
    stateSpy.get.and.callFake((stateName) => stateName);
    stateSpy.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });
    toggleFilterSidebarSpy = jasmine.createSpy('toggleFilterSidebarSpy');
    minimalProps = {
      error: null,
      loading: false,
      applicationPublicId: 'app-id',
      stageTypeId: 'stage-id',
      applicationName: 'app-name',
      stageName: 'stage name',
      selected: {
        licenseThreatGroups: new Set(),
        progressOptions: new Set(),
      },
      components: {
        filteredResults: [
          {
            displayName: 'g1 : a1 : v1',
            hash: 'some-hash-1',
            licenses: [
              {
                licenseName: 'lic1',
                licenseThreatGroups: [{ licenseThreatGroupName: 'ltg1a' }, { licenseThreatGroupName: 'ltg1b' }],
              },
              {
                licenseName: 'lic2',
                licenseThreatGroups: [{ licenseThreatGroupName: 'ltg2' }],
              },
            ],
            reviewCompletedCount: 0,
            reviewStatus: 'COMPLETED',
            reviewTotalCount: 0,
          },
          {
            displayName: 'g2 : a2 : v2',
            hash: 'some-hash-2',
            licenses: [
              {
                licenseName: 'lic1',
                licenseThreatGroups: [{ licenseThreatGroupName: 'ltg1c' }],
              },
              {
                licenseName: 'lic3',
                licenseThreatGroups: [{ licenseThreatGroupName: 'newLtg' }],
              },
              {
                licenseName: 'lic4',
                licenseThreatGroups: [{ licenseThreatGroupName: 'newLtg' }],
              },
            ],
            reviewCompletedCount: 1,
            reviewStatus: 'IN_PROGRESS',
            reviewTotalCount: 10,
          },
        ],
        results: [
          {
            displayName: 'g1 : a1 : v1',
            hash: 'some-hash-1',
            licenses: [
              {
                licenseName: 'lic1',
                licenseThreatGroups: [{ licenseThreatGroupName: 'ltg1a' }, { licenseThreatGroupName: 'ltg1b' }],
              },
              {
                licenseName: 'lic2',
                licenseThreatGroups: [{ licenseThreatGroupName: 'ltg2' }],
              },
            ],
            reviewCompletedCount: 0,
            reviewStatus: 'COMPLETED',
            reviewTotalCount: 0,
          },
          {
            displayName: 'g2 : a2 : v2',
            hash: 'some-hash-2',
            licenses: [
              {
                licenseName: 'lic1',
                licenseThreatGroups: [{ licenseThreatGroupName: 'ltg1c' }],
              },
              {
                licenseName: 'lic3',
                licenseThreatGroups: [{ licenseThreatGroupName: 'newLtg' }],
              },
              {
                licenseName: 'lic4',
                licenseThreatGroups: [{ licenseThreatGroupName: 'newLtg' }],
              },
            ],
            reviewCompletedCount: 1,
            reviewStatus: 'IN_PROGRESS',
            reviewTotalCount: 10,
          },
          {
            displayName: 'filtered out component',
            hash: 'some-hash-3',
            licenses: [
              {
                licenseName: 'lic5',
                licenseThreatGroups: [{ licenseThreatGroupName: 'filteredOutLTG' }],
              },
            ],
            reviewCompletedCount: 1,
            reviewStatus: 'IN_PROGRESS',
            reviewTotalCount: 10,
          },
        ],
        licenseThreatGroups: ['ltg1a', 'ltg1b', 'ltg2', 'ltg1c', 'newLtg', 'filteredOutLTG'],
      },
      fetchLegalApplicationDetailsData: fetchLegalApplicationDetailsDataSpy,
      sort: {},
      toggleFilterSidebar: toggleFilterSidebarSpy,
      $state: stateSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LegalApplicationDetailsPage, minimalProps);
  });

  it('the main has a LoadWrapper with appropriate parameters', function () {
    const main = getShallowComponent().find('.nx-page-main');
    expect(main.children().length).toEqual(1);
    const loadWrapper = main.childAt(0);
    expect(loadWrapper).toExist();
    expect(loadWrapper).toHaveProp('loading', false);
    expect(loadWrapper).toHaveProp('error', null);
    loadWrapper.prop('retryHandler')();
    expect(fetchLegalApplicationDetailsDataSpy).toHaveBeenCalledWith('app-id', 'stage-id');
  });

  it('is wrapped by a LoadWrapper with appropriate parameters when loading', function () {
    minimalProps = { ...minimalProps, loading: true };

    const loadWrapper = getShallowComponent(minimalProps).find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
    expect(loadWrapper).toHaveProp('error', null);
  });

  it('is wrapped by a LoadWrapper with appropriate parameters when error', function () {
    minimalProps = { ...minimalProps, error: 'some error' };

    const loadWrapper = getShallowComponent(minimalProps).find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', false);
    expect(loadWrapper).toHaveProp('error', 'some error');
  });

  it('does not render a LegalApplicationDetailsFilterContainer if filterSidebarOpen is false, ', function () {
    const filterContainer = getShallowComponent({ filterSidebarOpen: false }).find(
      LegalApplicationDetailsFilterContainer
    );
    expect(filterContainer).not.toExist();
  });

  it('shows a filter button and calls toggleFilterSidebar when clicked', function () {
    const filterButton = getShallowComponent({ filterSidebarOpen: false }).find('#filter-toggle');
    expect(filterButton).toExist();
    expect(filterButton).toHaveText('Filter');
    filterButton.simulate('click');
    expect(toggleFilterSidebarSpy).toHaveBeenCalledWith(true);
  });

  it('shows a dirty asterisk on the filter button when there are selected LTGs', function () {
    const filterButton = getShallowComponent({
      filterSidebarOpen: false,
      selected: {
        licenseThreatGroups: ['Liberal'],
        progressOptions: [],
      },
    }).find('#filter-toggle');
    expect(filterButton).toExist();
    expect(filterButton).toHaveText('Filter*');
    filterButton.simulate('click');
    expect(toggleFilterSidebarSpy).toHaveBeenCalledWith(true);
  });

  it('shows a dirty asterisk on the filter button when there are selected progress options', function () {
    const filterButton = getShallowComponent({
      filterSidebarOpen: false,
      selected: {
        licenseThreatGroups: [],
        progressOptions: ['Reviewed'],
      },
    }).find('#filter-toggle');
    expect(filterButton).toExist();
    expect(filterButton).toHaveText('Filter*');
    filterButton.simulate('click');
    expect(toggleFilterSidebarSpy).toHaveBeenCalledWith(true);
  });

  it('renders a LegalApplicationDetailsFilterContainer if filterSidebarOpen is true, ', function () {
    const filterContainer = getShallowComponent({ filterSidebarOpen: true }).find(
      LegalApplicationDetailsFilterContainer
    );
    expect(filterContainer).toExist();
  });

  it('renders a main', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('main.nx-page-main')).toExist();
  });

  it('renders the page title and subtitle', function () {
    const pageTitle = getShallowComponent().find('main.nx-page-main .nx-page-title');
    expect(pageTitle).toExist();
    expect(pageTitle.find('h1').text()).toEqual('app-name Obligations');
    expect(pageTitle.find('.nx-tile-header__subtitle').text()).toEqual('stage name Stage');
  });

  it('renders a table', function () {
    const wrapper = getShallowComponent();
    let table = wrapper.find(NxTable);
    expect(table).toExist();
    expect(table).toHaveProp('id', 'legal-application-details-table');
  });

  it('renders LegalApplicationDetailsComponentRow for each component passed in', function () {
    const wrapper = getShallowComponent();
    let rows = wrapper.find(NxTable).find(LegalApplicationDetailsComponentRow);
    expect(rows).toExist();
    expect(rows.length).toEqual(2);
    expect(rows.at(0)).toHaveProp('row', minimalProps.components.filteredResults[0]);
    expect(rows.at(1)).toHaveProp('row', minimalProps.components.filteredResults[1]);
  });

  it('renders a MenuBarBackButton to go to the applications dashboard', function () {
    const testMenuBarBackButton = (props, expectedHref) => {
      const wrapper = getShallowComponent(props);
      const menuBarBackButton = wrapper.find(MenuBarBackButton);
      expect(menuBarBackButton).toExist();
      expect(menuBarBackButton).toHaveProp('href', expectedHref);
      expect(stateSpy.href).toHaveBeenCalled();
    };

    testMenuBarBackButton(minimalProps, 'legal.dashboard');
    testMenuBarBackButton({ ...minimalProps, isSbomManager: true }, 'sbomManager.legal.dashboard');
  });
});
