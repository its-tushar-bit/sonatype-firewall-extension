/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import configureStore from 'redux-mock-store';
import { shallow } from 'enzyme';
import OriginalSourcesForm from 'MainRoot/legal/originalSources/OriginalSourcesForm';

describe('OriginalSourcesFormContainer', function () {
  let store,
    vdom,
    componentPart,
    state,
    CopyrightOverrideFormContainer,
    saveOriginalSourcesOverrideMock,
    setDisplayOriginalSourcesOverrideModalMock;

  beforeEach(function () {
    saveOriginalSourcesOverrideMock = jasmine.createSpy('saveOriginalSourcesOverride').and.returnValue({ type: 'FOO' });
    setDisplayOriginalSourcesOverrideModalMock = jasmine
      .createSpy('setDisplayOriginalSourcesOverrideModal')
      .and.returnValue({ type: 'BAR' });

    CopyrightOverrideFormContainer = require('inject-loader!../../../../main/frontend/' +
      'legal/originalSources/OriginalSourcesFormContainer')({
      './originalSourcesFormActions': {
        saveOriginalSourcesOverride: saveOriginalSourcesOverrideMock,
        setDisplayOriginalSourcesOverrideModal: setDisplayOriginalSourcesOverrideModalMock,
      },
    }).default;

    componentPart = {
      hash: '6f394c7df5600d11b221',
      licenseLegalData: {
        obligations: [
          {
            name: 'Required Disclosure of Original Source Code with Distribution',
            status: 'FLAGGED',
          },
          {
            name: 'Something else',
          },
        ],
      },
    };

    state = {
      copyrightOverrides: {
        saveCopyrightError: 'Some error',
        submitMaskState: true,
        showEditCopyrightOverrideModal: true,
      },
      advancedLegal: {
        availableScopes: {
          loading: false,
          error: null,
          values: [],
        },
        component: {
          component: componentPart,
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <CopyrightOverrideFormContainer store={store} />;
  });

  it('maps from mapStateToProps and mapDispatchToProps', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('component', componentPart);
    expect(wrapper).toHaveProp('availableScopes', {
      loading: false,
      error: null,
      values: [],
    });
    expect(wrapper).toHaveProp('existingObligation', {
      name: 'Required Disclosure of Original Source Code with Distribution',
      status: 'FLAGGED',
    });
  });

  it('correctly maps actions', () => {
    const wrapper = shallow(vdom).dive();
    const saveOriginalSourcesOverride = wrapper.prop('saveOriginalSourcesOverride');
    const setDisplayOriginalSourcesOverrideModal = wrapper.prop('setDisplayOriginalSourcesOverrideModal');

    expect(store.getActions()).toEqual([]);
    saveOriginalSourcesOverride('test', 'test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
    setDisplayOriginalSourcesOverrideModal('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }, { type: 'BAR' }]);
  });

  it('renders OriginalSourcesForm component', function () {
    const originalSourcesForm = shallow(vdom).find(OriginalSourcesForm);
    expect(originalSourcesForm).toExist();
  });
});
