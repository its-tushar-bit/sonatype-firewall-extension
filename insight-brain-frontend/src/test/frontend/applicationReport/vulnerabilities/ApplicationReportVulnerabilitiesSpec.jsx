import React from 'react';
import * as PropTypes from 'prop-types';
import configureStore from 'redux-mock-store';
import { Provider } from 'react-redux';
import { mount, shallow } from 'enzyme';
import { omit, lensPath, set } from 'ramda';

describe('ApplicationReportVulnerabilities', function() {

  let MockApplicationReportVulnerabiltiesPage,
      ApplicationReportVulnerabilities,
      mockNgRedux,
      mockActions,
      mock$State,
      vdom,
      state,
      mountedComponent;

  beforeEach(function() {
    state = {
      applicationReport: {
        pendingLoads: new Set(),
        vulnerabilities: ['foo-1234', 'bar-qwerty'],
        metadata: { foo: 'bar' },
        loadError: 'Error!'
      }
    };

    MockApplicationReportVulnerabiltiesPage = jasmine.createSpy('MockApplicationReportVulnerabiltiesPage')
        .and.returnValue(<div>Page</div>);

    mock$State = jasmine.createSpyObj('$state', ['get', 'href']);
    MockApplicationReportVulnerabiltiesPage.propTypes = { $state: PropTypes.any };

    ApplicationReportVulnerabilities = require(
        'inject-loader!../../../../main/frontend/applicationReport/vulnerabilities/ApplicationReportVulnerabilities'
    )({
      './ApplicationReportVulnerabilitiesPage': MockApplicationReportVulnerabiltiesPage
    }).default;

    mockNgRedux = configureStore()(() => state);
    mockActions = {
      loadReportAllData: jasmine.createSpy('loadReportAllData').and.returnValue({ type: 'FOO' })
    };

    vdom = (
      <ApplicationReportVulnerabilities $ngRedux={mockNgRedux}
                                        applicationReportActions={mockActions}
                                        $state={mock$State}/>
    );
  });

  afterEach(function() {
    if (mountedComponent) {
      mountedComponent.unmount();
    }

    mountedComponent = null;
  });

  it('renders a provider with $ngRedux as the store', function() {
    const component = shallow(vdom);

    expect(component).toMatchSelector(Provider);
    expect(component).toHaveProp('store', mockNgRedux);
  });

  it('correctly maps the redux state to the ApplicationReportVulnerabilitiesPage props', function() {
    mountedComponent = mount(vdom);

    const pageWrapper = mountedComponent.find(MockApplicationReportVulnerabiltiesPage);

    expect(pageWrapper).toHaveProp('vulnerabilities', state.applicationReport.vulnerabilities);
    expect(pageWrapper).toHaveProp('metadata', state.applicationReport.metadata);
    expect(pageWrapper).toHaveProp('loadError', state.applicationReport.loadError);
  });

  it('sets the ApplicationReportVulnerabilitiesPage loading prop based on the pendingLoads in the redux state',
      function() {
        mountedComponent = mount(vdom);

        let pageWrapper = mountedComponent.find(MockApplicationReportVulnerabiltiesPage);

        expect(pageWrapper).toHaveProp('loading', false);

        // update state
        state = set(lensPath(['applicationReport', 'pendingLoads']), new Set(['foo']), state);
        mockNgRedux.dispatch({ type: 'ANY_ACTION' });
        mountedComponent.update();
        pageWrapper = mountedComponent.find(MockApplicationReportVulnerabiltiesPage);

        expect(pageWrapper).toHaveProp('loading', true);
      }
  );

  it('maps the vulnerabilities as an empty list if they aren\'t defined in the state', function() {
    const mockNgRedux = configureStore()({
          ...state,
          applicationReport: omit(['vulnerabilities'], state.applicationReport)
        }),
        vdom = <ApplicationReportVulnerabilities $ngRedux={mockNgRedux} applicationReportActions={mockActions} />;

    mountedComponent = mount(vdom);

    const pageWrapper = mountedComponent.find(MockApplicationReportVulnerabiltiesPage);

    expect(pageWrapper).toHaveProp('vulnerabilities', []);
  });

  it('correctly maps the loadReportAllData action into the ApplicationReportVulnerabilitiesPage props', function() {
    mountedComponent = mount(vdom);

    const loadReportAllDataDispatch =
        mountedComponent.find(MockApplicationReportVulnerabiltiesPage).prop('loadReportAllData');

    expect(loadReportAllDataDispatch).toEqual(jasmine.any(Function));

    expect(mockNgRedux.getActions()).toEqual([]);

    loadReportAllDataDispatch();

    expect(mockNgRedux.getActions()).toEqual([{ type: 'FOO' }]);
  });

  it('passes the $state prop on to the ApplicationReportVulnerabilitiesPage', function() {
    mountedComponent = mount(vdom);

    const pageWrapper = mountedComponent.find(MockApplicationReportVulnerabiltiesPage);

    expect(pageWrapper).toHaveProp('$state', mock$State);
  });

  it('renders the output of ApplicationReportVulnerabilitiesPage', function() {
    mountedComponent = mount(vdom);

    const renderedComponent = mountedComponent.render();

    expect(renderedComponent.is('div')).toBe(true);
    expect(renderedComponent.text()).toBe('Page');
  });
});
