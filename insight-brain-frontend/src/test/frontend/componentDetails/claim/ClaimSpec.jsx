/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import { NxForm, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import Claim from 'MainRoot/componentDetails/claim/Claim';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('Claim', () => {
  let getShallow;

  const loadComponentIdentifiedMock = jasmine.createSpy('loadComponentIdentified');
  const claimMock = jasmine.createSpy('claim');
  const resetTabMock = jasmine.createSpy('resetTab');
  const resetFormMock = jasmine.createSpy('resetForm');

  const setGroupIdMock = jasmine.createSpy('setGroupId');
  const setExtensionMock = jasmine.createSpy('setExtension');
  const setArtifactIdMock = jasmine.createSpy('setArtifactId');
  const setVersionMock = jasmine.createSpy('setVersion');
  const setClassifierMock = jasmine.createSpy('setClassifier');
  const setCommentMock = jasmine.createSpy('setComment');
  const toggleShowRevokeModalMock = jasmine.createSpy('toggleShowRevokeModal');

  const minimalProps = {
    loadComponentIdentified: loadComponentIdentifiedMock,
    claim: claimMock,
    resetTab: resetTabMock,
    resetForm: resetFormMock,
    setGroupId: setGroupIdMock,
    setExtension: setExtensionMock,
    setArtifactId: setArtifactIdMock,
    setVersion: setVersionMock,
    setClassifier: setClassifierMock,
    setComment: setCommentMock,
    toggleShowRevokeModal: toggleShowRevokeModalMock,
    inputFields: {
      artifactId: initUserInput(''),
      classifier: initUserInput(''),
      extension: initUserInput(''),
      groupId: initUserInput(''),
      version: initUserInput(''),
      comment: initUserInput(''),
      createTime: initUserInput(''),
    },
    claimId: '200',
    loading: false,
    loadError: null,
    claimError: null,
    isDirty: false,
    validationError: null,
    showRevokeModal: false,
  };

  beforeEach(() => {
    getShallow = enzymeUtils.getShallowComponent(Claim, minimalProps);
  });

  const textFieldsAssert = (id, actionName, mock, changeValue = 'value', options = {}) => {
    it(`calls ${actionName} action`, () => {
      const component = getShallow(options);
      const input = component.find(`#${id}`);

      input.simulate('change', changeValue);
      expect(mock).toHaveBeenCalledWith(changeValue);
    });
  };

  describe('on initial load', () => {
    let component, getMounted;

    beforeEach(() => {
      getMounted = enzymeUtils.getMountedComponent(Claim, minimalProps);
    });

    afterEach(() => {
      if (component) {
        component.unmount();
      }
    });

    it('calls load', () => {
      component = getMounted();
      expect(loadComponentIdentifiedMock).toHaveBeenCalled();
    });

    it('resets tab before unmount component', () => {
      component = getMounted();
      expect(resetTabMock).toHaveBeenCalled();
    });

    it('calls resetForm when the form is canceled if it is dirty', () => {
      component = getMounted({ isDirty: true, validationError: null });
      const button = component.find(NxForm).find('#component-details-claim-cancel.nx-btn');

      button.simulate('click');

      expect(resetFormMock).toHaveBeenCalledTimes(1);
    });

    it('calls toggleShowRevokeModal when revoke button is clicked', () => {
      component = getMounted({ isDirty: true, validationError: null });
      const button = component.find(NxForm).find('#component-details-claim-revoke.nx-btn');

      button.simulate('click');

      expect(toggleShowRevokeModalMock).toHaveBeenCalledTimes(1);
    });
  });

  describe('on load error', () => {
    it('form has doLoad prop', () => {
      const component = getShallow({ loadError: 'error' });
      const form = component.find(NxForm);

      expect(form).toExist();
      expect(form).toHaveProp('doLoad', loadComponentIdentifiedMock);
    });
  });

  describe('on render', () => {
    describe('validationErrors', () => {
      it('is null if form was changed and changes are valid', () => {
        const component = getShallow({ isDirty: true, validationError: null });
        const form = component.find(NxForm);

        expect(form).toHaveProp('validationErrors', null);
      });

      it('has "There are no changes to save" error if form was not changed', () => {
        const component = getShallow({ isDirty: false });
        const form = component.find(NxForm);

        expect(form).toHaveProp('validationErrors', 'There are no changes to save');
      });

      it('has "Unable to save" error if form was not changed', () => {
        const component = getShallow({
          isDirty: true,
          validationError: 'Unable to save: fields with invalid or missing data',
        });
        const form = component.find(NxForm);

        expect(form).toHaveProp('validationErrors', 'Unable to save: fields with invalid or missing data');
      });
    });

    describe('on form submit', () => {
      it('calls claim when the form is submitted if it is dirty', () => {
        const component = getShallow({ isDirty: true, validationError: null });
        const form = component.find(NxForm);

        form.simulate('submit');

        expect(claimMock).toHaveBeenCalled();
      });
    });

    describe('inputs', () => {
      textFieldsAssert('groupId', 'setGroupId', setGroupIdMock, 'text');
      textFieldsAssert('extension', 'setExtension', setExtensionMock, 'extension');
      textFieldsAssert('artifactId', 'setArtifactId', setArtifactIdMock, 'artifact id');
      textFieldsAssert('version', 'setVersion', setVersionMock, 'version');
      textFieldsAssert('classifier', 'setClassifier', setClassifierMock, 'classifier');
      textFieldsAssert('comment', 'setComment', setCommentMock, 'comment');
    });
  });
});
