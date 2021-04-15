/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import configureStore from 'redux-mock-store';
import { shallow } from 'enzyme';
import CopyrightOverrideForm from '../../../../main/frontend/legal/copyright/CopyrightOverrideForm';

describe('CopyrightOverrideFormContainer', function () {
  let store,
    vdom,
    componentPart,
    state,
    CopyrightOverrideFormContainer,
    saveCopyrightOverrideMock,
    setDisplayCopyrightOverrideModalMock;

  beforeEach(function () {
    saveCopyrightOverrideMock = jasmine.createSpy('saveCopyrightOverride').and.returnValue({ type: 'FOO' });
    setDisplayCopyrightOverrideModalMock = jasmine
      .createSpy('setDisplayCopyrightOverrideModal')
      .and.returnValue({ type: 'BAR' });

    CopyrightOverrideFormContainer = require('inject-loader!../../../../main/frontend/' +
      'legal/copyright/CopyrightOverrideFormContainer')({
      './copyrightOverrideFormActions': {
        saveCopyrightOverride: saveCopyrightOverrideMock,
        setDisplayCopyrightOverrideModal: setDisplayCopyrightOverrideModalMock,
      },
    }).default;

    componentPart = {
      hash: '6f394c7df5600d11b221',
      licenseLegalData: {
        obligations: [
          {
            name: 'Inclusion of Copyright',
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
    expect(wrapper).toHaveProp('saveCopyrightError', 'Some error');
    expect(wrapper).toHaveProp('submitMaskState', true);
    expect(wrapper).toHaveProp('showEditCopyrightOverrideModal', true);
    expect(wrapper).toHaveProp('component', componentPart);
    expect(wrapper).toHaveProp('availableScopes', {
      loading: false,
      error: null,
      values: [],
    });
    expect(wrapper).toHaveProp('existingObligation', {
      name: 'Inclusion of Copyright',
      status: 'FLAGGED',
    });
  });

  it('correctly maps actions', () => {
    const wrapper = shallow(vdom).dive();
    const saveCopyrightOverride = wrapper.prop('saveCopyrightOverride');
    const setDisplayCopyrightOverrideModal = wrapper.prop('setDisplayCopyrightOverrideModal');

    expect(store.getActions()).toEqual([]);
    saveCopyrightOverride('test', 'test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
    setDisplayCopyrightOverrideModal('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }, { type: 'BAR' }]);
  });

  it('renders CopyrightOverrideForm component', function () {
    const copyrightOverrideForm = shallow(vdom).find(CopyrightOverrideForm);
    expect(copyrightOverrideForm).toExist();
  });
});
