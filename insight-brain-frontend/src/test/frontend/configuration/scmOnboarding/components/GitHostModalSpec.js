/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import { NxErrorAlert, NxForm, NxInfoAlert, NxModal, NxTextInput } from '@sonatype/react-shared-components';
import GitHostModal from '../../../../../main/frontend/configuration/scmOnboarding/components/GitHostModal';
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { createOrg } from './utils';
import { validateHostUrl } from '../../../../../main/frontend/configuration/scmOnboarding/utils/validators';
import CredentialsError from '../../../../../main/frontend/configuration/scmOnboarding/components/CredentialsError';

describe('GitHostModal', function () {
  let getShallowComponent;

  beforeEach(() => {
    const minimalProps = { isGitHostDialogVisible: true, currentHostUrlState: textInputStateHelpers.initialState('') };
    getShallowComponent = enzymeUtils.getShallowComponent(GitHostModal, minimalProps);
  });

  describe('modal can be displayed can submitted', () => {
    it('cancel closes modal dialog', () => {
      // given a rendered form
      const setShowHostDialogSpy = jasmine.createSpy('setShowHostDialog');
      const component = getShallowComponent({ setShowHostDialog: setShowHostDialogSpy }),
        modal = component.find(NxModal),
        form = modal.find(NxForm);
      expect(form).toExist();

      // when cancel is invoked
      form.invoke('onCancel')();

      // then redux action is invoked
      expect(setShowHostDialogSpy).toHaveBeenCalledWith(false);
    });

    it('submit closes modal dialog', () => {
      // given a rendered form with host url to be submitted
      const setShowHostDialogSpy = jasmine.createSpy('setShowHostDialog');
      const setIsGitHostNeededSpy = jasmine.createSpy('setIsGitHostNeeded');
      const loadRepositoriesSpy = jasmine.createSpy('loadRepositories');
      const component = getShallowComponent({
          setShowHostDialog: setShowHostDialogSpy,
          setIsGitHostNeeded: setIsGitHostNeededSpy,
          loadRepositories: loadRepositoriesSpy,
          selectedOrganization: createOrg('foo-org'),
          currentHostUrlState: textInputStateHelpers.initialState('value'),
        }),
        modal = component.find(NxModal),
        form = modal.find(NxForm);
      expect(form).toExist();

      // when cancel is invoked
      form.invoke('onSubmit')();

      // then redux actions are invoked
      expect(setShowHostDialogSpy).toHaveBeenCalledWith(false);
      expect(setIsGitHostNeededSpy).toHaveBeenCalledWith(false);
      expect(loadRepositoriesSpy).toHaveBeenCalledWith('id-foo-org', 'value');
    });

    it('form visibility is controlled by parent component', () => {
      // when visibility is set to false
      const component = getShallowComponent({ isGitHostDialogVisible: false }),
        modal = component.find(NxModal);

      // expect modal not to be rendered
      expect(modal).not.toExist();
    });
  });

  describe('host URL validation', () => {
    it('invokes redux for validation', () => {
      const setCurrentHostUrlSpy = jasmine.createSpy('setCurrentHostUrl');
      const validateScmHostUrlSpy = jasmine.createSpy('validateScmHostUrl');
      const component = getShallowComponent({
          currentHostUrlState: textInputStateHelpers.initialState('http://host/'),
          setCurrentHostUrl: setCurrentHostUrlSpy,
          validateScmHostUrl: validateScmHostUrlSpy,
          scmProvider: 'scmProvider',
        }),
        input = component.find(NxTextInput);

      // when onChange is invoked with valid URL
      input.invoke('onChange')('http://host/changed');

      // then redux validator is invoked
      expect(validateScmHostUrlSpy).toHaveBeenCalledWith('scmProvider', 'http://host/changed');

      // and value in redux is updated
      expect(setCurrentHostUrlSpy).toHaveBeenCalledWith('http://host/changed');
    });

    it('does not invoke redux for validation when field validator fails', () => {
      const setCurrentHostUrlSpy = jasmine.createSpy('setCurrentHostUrl');
      const validateScmHostUrlSpy = jasmine.createSpy('validateScmHostUrl');
      const component = getShallowComponent({
          currentHostUrlState: textInputStateHelpers.initialState('http://host/', validateHostUrl),
          setCurrentHostUrl: setCurrentHostUrlSpy,
          validateScmHostUrl: validateScmHostUrlSpy,
          scmProvider: 'scmProvider',
        }),
        input = component.find(NxTextInput);

      // when onChange is invoked with invalid URL
      input.invoke('onChange')('not valid');

      // then redux validator is not invoked
      expect(validateScmHostUrlSpy).not.toHaveBeenCalled();

      // and value in redux is updated
      expect(setCurrentHostUrlSpy).toHaveBeenCalledWith('not valid');
    });
  });

  describe('handles error cases', () => {
    [
      { loadRepositoriesErrorCode: null, expectedTitle: 'SCM Server Needed' },
      { loadRepositoriesErrorCode: 'SCM_AUTHN_FAILURE', expectedTitle: 'Authentication Error' },
      { loadRepositoriesErrorCode: 'SCM_AUTHZ_FAILURE', expectedTitle: 'Authorization Error' },
      { loadRepositoriesErrorCode: 'DOESNT_EXIST', expectedTitle: 'Connection Error' },
    ].forEach(({ loadRepositoriesErrorCode, expectedTitle }) => {
      it(`displays title '${expectedTitle}' when there is a ${loadRepositoriesErrorCode} error`, () => {
        // given a component with an error condition
        const component = getShallowComponent({ loadRepositoriesErrorCode }),
          title = component.find('.nx-modal-header');

        // expect title to match
        expect(title.text()).toEqual(expectedTitle);
      });
    });

    it('displays credentials error', () => {
      // given a component with an error
      const component = getShallowComponent({ loadRepositoriesErrorCode: 'SCM_AUTHN_FAILURE', isGitHostNeeded: false }),
        alert = component.find(NxErrorAlert),
        error = alert.find(CredentialsError);

      // expect CredentialsError to be rendered
      expect(error).toExist();
      expect(error.props().errorCode).toEqual('SCM_AUTHN_FAILURE');
    });

    it('displays generic info text when provided by parent', () => {
      // given a component with an info text
      const component = getShallowComponent({ errorText: 'error text', isGitHostNeeded: true }),
        error = component.find(NxInfoAlert);

      // expect error text to be rendered
      expect(error).toExist();
      expect(error.text()).toEqual('error text');
    });

    it('displays generic error text when provided by parent', () => {
      // given a component with an error text
      const component = getShallowComponent({ errorText: 'error text', isGitHostNeeded: false }),
        error = component.find(NxErrorAlert);

      // expect error text to be rendered
      expect(error).toExist();
      expect(error.text()).toEqual('error text');
    });
  });
});
