/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxTable, NxTableBody } from '@sonatype/react-shared-components';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';
import BackButton from '../../../../main/frontend/react/BackButton';
import LegalApplicationDetailsPage from '../../../../main/frontend/legal/application/LegalApplicationDetailsPage';
import LegalApplicationDetailsComponentRow from '../../../../main/frontend/legal/application/LegalApplicationDetailsComponentRow';
import LegalApplicationDetailsFilterContainer from '../../../../main/frontend/legal/application/filter/LegalApplicationDetailsFilterContainer';

describe('LegalApplicationDetailsPage', function () {
  let minimalProps, loadApplicationSpy, stateSpy, getShallowComponent;

  beforeEach(function () {
    loadApplicationSpy = jasmine.createSpy('loadApplication');
    stateSpy = jasmine.createSpyObj('$state', ['get', 'href']);
    minimalProps = {
      applicationPublicId: 'app-id',
      stageTypeId: 'stage-id',
      application: {
        name: 'app-name',
        error: null,
        loading: false,
      },
      stageType: {
        name: 'stage name',
        error: null,
        loading: false,
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
        error: null,
        loading: false,
      },
      loadApplication: loadApplicationSpy,
      sort: {},
      $state: stateSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LegalApplicationDetailsPage, minimalProps);
  });

  it('is wrapped by a LoadWrapper with appropriate parameters', function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper);
    expect(loadWrapper).toExist();
    expect(loadWrapper).toHaveProp('loading', false);
    expect(loadWrapper).toHaveProp('error', null);
    loadWrapper.prop('retryHandler')();
    expect(loadApplicationSpy).toHaveBeenCalledWith('app-id', 'stage-id');
  });

  it('is wrapped by a LoadWrapper with appropriate parameters when loading application data', function () {
    const application = { ...minimalProps.application, loading: true };
    minimalProps = { ...minimalProps, application: application };

    const loadWrapper = getShallowComponent(minimalProps).find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
    expect(loadWrapper).toHaveProp('error', null);
  });

  it('is wrapped by a LoadWrapper with appropriate parameters when error in application data', function () {
    const application = { ...minimalProps.application, error: 'some error' };
    minimalProps = { ...minimalProps, application: application };

    const loadWrapper = getShallowComponent(minimalProps).find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', false);
    expect(loadWrapper).toHaveProp('error', 'some error');
  });

  it('is wrapped by a LoadWrapper with appropriate parameters when loading stage type data', function () {
    const stageType = { ...minimalProps.stageType, loading: true };
    minimalProps = { ...minimalProps, stageType: stageType };

    const loadWrapper = getShallowComponent(minimalProps).find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
    expect(loadWrapper).toHaveProp('error', null);
  });

  it('is wrapped by a LoadWrapper with appropriate parameters when error in stage type data', function () {
    const stageType = { ...minimalProps.stageType, error: 'some other error' };
    minimalProps = { ...minimalProps, stageType: stageType };

    const loadWrapper = getShallowComponent(minimalProps).find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', false);
    expect(loadWrapper).toHaveProp('error', 'some other error');
  });

  it('renders a BackButton with the dashboard state name and the provided $state object, ', function () {
    const backButton = getShallowComponent().find(BackButton);

    expect(backButton).toExist();
    expect(backButton).toHaveProp('stateName', 'legal.dashboard');
    expect(backButton).toHaveProp('$state', stateSpy);
  });

  it('renders an aside with a LegalApplicationDetailsFilterContainer, ', function () {
    const aside = getShallowComponent().find('aside#legal-application-details-filter-container');
    expect(aside).toExist();
    expect(aside.find(LegalApplicationDetailsFilterContainer)).toExist();
  });

  it('correctly passes the licenseThreatGroups to the LegalApplicationDetailsFilterContainer, ', function () {
    const expectedLTGs = ['ltg1a', 'ltg1b', 'ltg2', 'ltg1c', 'newLtg'];
    const filterContainer = getShallowComponent().find(LegalApplicationDetailsFilterContainer);
    expect(filterContainer).toHaveProp('licenseThreatGroups', expectedLTGs);
  });

  it('includes the "No LTG Assigned" value to the LegalApplicationDetailsFilterContainer, ', function () {
    const components = {
      ...minimalProps.components,
      filteredResults: [
        {
          displayName: 'g1 : a1 : v1',
          hash: 'some-hash-1',
          licenses: [
            {
              licenseName: 'lic1',
              licenseThreatGroups: [{ licenseThreatGroupName: 'ltg1a' }, { licenseThreatGroupName: 'ltg1b' }],
            },
          ],
        },
        {
          displayName: 'g2 : a2 : v2',
          hash: 'some-hash-2',
          licenses: [
            {
              licenseName: 'lic2',
            },
          ],
        },
      ],
    };
    minimalProps = { ...minimalProps, components: components };

    const expectedLTGs = ['ltg1a', 'ltg1b', 'No LTG Assigned'];
    const filterContainer = getShallowComponent(minimalProps).find(LegalApplicationDetailsFilterContainer);
    expect(filterContainer).toHaveProp('licenseThreatGroups', expectedLTGs);
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

  it('renders a table with a loading property when fetching components', function () {
    const components = { ...minimalProps.components, loading: true };
    minimalProps = { ...minimalProps, components: components };

    const wrapper = getShallowComponent(minimalProps);
    let tableBody = wrapper.find(NxTableBody);
    expect(tableBody).toHaveProp('isLoading', true);
  });

  it('renders a table with an error message when fetching components failed', function () {
    const components = {
      ...minimalProps.components,
      error: 'components error',
    };
    minimalProps = { ...minimalProps, components: components };

    const wrapper = getShallowComponent(minimalProps);
    let tableBody = wrapper.find(NxTableBody);
    expect(tableBody).toHaveProp('error', 'components error');
  });

  it('renders LegalApplicationDetailsComponentRow for each component passed in', function () {
    const wrapper = getShallowComponent();
    let rows = wrapper.find(NxTable).find(LegalApplicationDetailsComponentRow);
    expect(rows).toExist();
    expect(rows.length).toEqual(2);
    expect(rows.at(0)).toHaveProp('row', minimalProps.components.filteredResults[0]);
    expect(rows.at(1)).toHaveProp('row', minimalProps.components.filteredResults[1]);
  });
});
