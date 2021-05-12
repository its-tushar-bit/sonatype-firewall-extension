/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { shallow } from 'enzyme';
import configureStore from 'redux-mock-store';

import {
  SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED,
  SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED,
  SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED,
  SUCCESS_METRICS_CONFIGURATION_RESET_FORM,
} from '../../../../main/frontend/configuration/successMetricsConfiguration/successMetricsConfigurationActions';

import SuccessMetricsConfiguration from '../../../../main/frontend/configuration/successMetricsConfiguration/SuccessMetricsConfiguration';

describe('SuccessMetricsConfigurationContainer', function () {
  let SuccessMetricsConfigurationContainer,
    updateMock,
    loadMock,
    toggleIsEnabledMock,
    resetFormMock,
    store,
    state,
    vdom;

  beforeEach(function () {
    updateMock = jasmine.createSpy('update').and.returnValue({
      type: SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED,
    });

    loadMock = jasmine.createSpy('load').and.returnValue({
      type: SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED,
    });

    toggleIsEnabledMock = jasmine.createSpy('toggleIsEnabled').and.returnValue({
      type: SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED,
    });

    resetFormMock = jasmine.createSpy('resetForm').and.returnValue({
      type: SUCCESS_METRICS_CONFIGURATION_RESET_FORM,
    });

    SuccessMetricsConfigurationContainer = require('inject-loader!../../../../main/frontend/configuration/successMetricsConfiguration/SuccessMetricsConfigurationContainer')(
      {
        './successMetricsConfigurationActions': {
          load: loadMock,
          update: updateMock,
          resetForm: resetFormMock,
          toggleIsEnabled: toggleIsEnabledMock,
        },
      }
    ).default;

    state = {
      successMetricsConfiguration: {
        formState: {
          enabled: false,
        },
        viewState: {
          loading: true,
          loadError: null,
          updateError: null,
          submitMaskState: null,
          isDirty: false,
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <SuccessMetricsConfigurationContainer store={store} />;
  });

  it('maps the state slice to props', function () {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', true);
    expect(wrapper).toHaveProp('enabled', false);
    expect(wrapper).toHaveProp('loadError', null);
    expect(wrapper).toHaveProp('updateError', null);
    expect(wrapper).toHaveProp('submitMaskState', null);
    expect(wrapper).toHaveProp('isDirty', false);

    state = {
      successMetricsConfiguration: {
        ...state.successMetricsConfiguration,
        formState: {
          enabled: true,
        },
        viewState: {
          ...state.successMetricsConfiguration.viewState,
          loading: false,
        },
      },
    };

    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', false);
    expect(wrapper).toHaveProp('enabled', true);
    expect(wrapper).toHaveProp('loadError', null);
    expect(wrapper).toHaveProp('updateError', null);
    expect(wrapper).toHaveProp('submitMaskState', null);
    expect(wrapper).toHaveProp('isDirty', false);
  });

  it('maps action creators to props', function () {
    const wrapper = shallow(vdom).dive();
    const updateActionCreator = wrapper.prop('update');
    const loadActionCreator = wrapper.prop('load');
    const toggleIsEnabledActionCreator = wrapper.prop('toggleIsEnabled');
    const resetFormActionCreator = wrapper.prop('resetForm');

    expect(updateActionCreator).toEqual(jasmine.any(Function));
    expect(loadActionCreator).toEqual(jasmine.any(Function));
    expect(resetFormActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);

    loadActionCreator();
    expect(store.getActions()).toEqual([{ type: SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED }]);

    updateActionCreator();
    expect(store.getActions()).toEqual([
      { type: SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED },
      { type: SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED },
    ]);

    toggleIsEnabledActionCreator();
    expect(store.getActions()).toEqual([
      { type: SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED },
      { type: SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED },
      { type: SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED },
    ]);

    resetFormActionCreator();
    expect(store.getActions()).toEqual([
      { type: SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED },
      { type: SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED },
      { type: SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED },
      { type: SUCCESS_METRICS_CONFIGURATION_RESET_FORM },
    ]);
  });

  it('renders SuccessMetricsConfiguration component', function () {
    const successMetricsConfigurationComponent = shallow(vdom).find(SuccessMetricsConfiguration);

    expect(successMetricsConfigurationComponent).toExist();
    expect(successMetricsConfigurationComponent).toHaveProp('enabled', false);
  });
});
