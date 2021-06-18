/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxButton, NxModal, NxSubmitMask, NxWarningAlert } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../../../enzymeUtils';
import LoadError from '../../../../../../main/frontend/react/LoadError';
import LoadWrapper from '../../../../../../main/frontend/react/LoadWrapper';
import NxExternalLink from '../../../../../../main/frontend/react/NxExternalLink';
import UserTokenDisplay from '../../../../../../main/frontend/mainHeader/MenuBar/UserMenu/UserToken/UserTokenDisplay';
import UserTokenModal from '../../../../../../main/frontend/mainHeader/MenuBar/UserMenu/UserToken/UserTokenModal';

describe('UserTokenModal', function () {
  let minimalProps, getShallowComponent, getMountedComponent, mountPoints;

  beforeEach(function () {
    minimalProps = {
      userToken: null,
      hideUserTokenModal: jasmine.createSpy('hideUserTokenModal'),
      checkUserTokenLoading: null,
      generateUserTokenLoading: null,
      deleteUserTokenLoading: null,
      checkUserTokenError: null,
      generateUserTokenError: null,
      deleteUserTokenError: null,
      generateUserToken: jasmine.createSpy('generateUserToken'),
      deleteUserToken: jasmine.createSpy('deleteuserToken'),
      checkUserTokenExistence: jasmine.createSpy('checkUserTokenExistence'),
    };

    mountPoints = [];

    getShallowComponent = enzymeUtils.getShallowComponent(UserTokenModal, minimalProps);
    getMountedComponent = (props) =>
      enzymeUtils.getMountedComponent(UserTokenModal, minimalProps, {
        attachTo: getMountPoint(),
      })(props);
  });

  afterEach(() => {
    for (const mountPoint of mountPoints) {
      document.body.removeChild(mountPoint);
    }

    mountPoints = [];
  });

  function getMountPoint() {
    const mountPoint = document.createElement('div');
    document.body.append(mountPoint);
    mountPoints.push(mountPoint);

    return mountPoint;
  }

  it('renders a narrow NxModal', function () {
    const component = getShallowComponent(),
      modal = component.find(NxModal);

    expect(modal).toExist();
    expect(modal).toHaveProp('id', 'user-token-modal');
    expect(modal).toHaveProp('onClose', minimalProps.hideUserTokenModal);
    expect(modal).toHaveProp('variant', 'narrow');
  });

  it('renders a header in the modal with the title Manage User Token', function () {
    const component = getShallowComponent(),
      modal = component.find(NxModal),
      header = modal.find('header');

    expect(header).toHaveText('Manage User Token');
  });

  describe('modal contents', function () {
    it('renders NxSubmitMask if deleteUserTokenLoading is false', function () {
      // flag in false, as if the deleteUserTokenAction had been fired
      const component = getShallowComponent({ deleteUserTokenLoading: false }),
        modal = component.find(NxModal),
        content = modal.find('.nx-modal-content'),
        deleteLoadMask = content.find(NxSubmitMask);

      expect(deleteLoadMask).toExist();
      expect(deleteLoadMask).toHaveProp('message', 'Deleting…');
      expect(deleteLoadMask).toHaveProp('success', false);
    });

    it('renders NxSubmitMask if deleteUserTokenLoading is true', function () {
      const component = getShallowComponent({ deleteUserTokenLoading: true }),
        modal = component.find(NxModal),
        content = modal.find('.nx-modal-content'),
        deleteLoadMask = content.find(NxSubmitMask);

      expect(deleteLoadMask).toExist();
      expect(deleteLoadMask).toHaveProp('message', 'Deleting…');
      expect(deleteLoadMask).toHaveProp('success', true);
    });

    it('does not render NxSubmitMask if deleteUserTokenLoading is null', function () {
      // flag in null, as if the submit mask timer had expired
      const componentAfterTimer = getShallowComponent({
          deleteUserTokenLoading: null,
        }),
        modalAfterTimer = componentAfterTimer.find(NxModal),
        contentAfterTimer = modalAfterTimer.find('.nx-modal-content'),
        deleteLoadMask = contentAfterTimer.find(NxSubmitMask);

      expect(deleteLoadMask).not.toExist();
    });

    it('renders NxSubmitMask if generateUserTokenLoading is false', function () {
      // flag in false, as if the generateUserTokenAction had been fired
      const component = getShallowComponent({
          generateUserTokenLoading: false,
        }),
        modal = component.find(NxModal),
        content = modal.find('.nx-modal-content'),
        generateLoadMask = content.find(NxSubmitMask);

      expect(generateLoadMask).toExist();
      expect(generateLoadMask).toHaveProp('message', 'Generating…');
      expect(generateLoadMask).toHaveProp('success', false);
    });

    it('renders NxSubmitMask if generateUserTokenLoading is true', function () {
      const component = getShallowComponent({ generateUserTokenLoading: true }),
        modal = component.find(NxModal),
        content = modal.find('.nx-modal-content'),
        generateLoadMask = content.find(NxSubmitMask);

      expect(generateLoadMask).toExist();
      expect(generateLoadMask).toHaveProp('message', 'Generating…');
      expect(generateLoadMask).toHaveProp('success', true);
    });

    it('does not render NxSubmitMask if generateUserTokenLoading is null', function () {
      // flag in null, as if the submit mask timer had expired
      const componentAfterTimer = getShallowComponent({
          generateUserTokenLoading: null,
        }),
        modalAfterTimer = componentAfterTimer.find(NxModal),
        contentAfterTimer = modalAfterTimer.find('.nx-modal-content'),
        generateLoadMask = contentAfterTimer.find(NxSubmitMask);

      expect(generateLoadMask).not.toExist();
    });

    it('renders a LoadWrapper with the appropriate props', function () {
      const additionalProps = {
        checkUserTokenLoading: false,
        checkUserTokenError: 'Error when loading token',
        checkUserTokenExistence: jasmine.createSpy('checkUserTokenExistence'),
      };
      const component = getShallowComponent(additionalProps),
        modal = component.find(NxModal),
        content = modal.find('.nx-modal-content'),
        loadWrapper = content.find(LoadWrapper);

      expect(loadWrapper).toHaveProp('loading', false);
      expect(loadWrapper).toHaveProp('error', 'Error when loading token');
      expect(loadWrapper).toHaveProp('retryHandler', additionalProps.checkUserTokenExistence);
    });

    it('renders an explanation of the usage for the user token', function () {
      const additionalProps = {
        userToken: false,
      };
      const component = getShallowComponent(additionalProps),
        modal = component.find(NxModal),
        content = modal.find('.nx-modal-content'),
        loadWrapper = content.find(LoadWrapper),
        paragraphs = loadWrapper.find('p');

      expect(paragraphs.at(0)).toHaveText(
        'Your user token credentials are only available upon creation. You can not recover them later.'
      );

      expect(paragraphs.at(1)).toHaveText(
        'Should you forget or lose your user token credentials, you should delete your ' +
          'user token and create a new one. To learn more about User Tokens please see the <NxExternalLink />'
      );

      const externalDocumentationLink = paragraphs.at(1).find(NxExternalLink);
      expect(externalDocumentationLink).toHaveProp(
        'href',
        'https://help.sonatype.com/iqserver/managing/user-management/user-tokens'
      );
      expect(externalDocumentationLink).toHaveProp('children', 'help documentation.');
    });

    it('renders a warning when the userToken already exists and does not show the UserTokenDisplay', function () {
      const additionalProps = {
        userToken: true,
      };
      const component = getShallowComponent(additionalProps),
        modal = component.find(NxModal),
        content = modal.find('.nx-modal-content'),
        warning = content.find(NxWarningAlert),
        userTokenDisplay = content.find(UserTokenDisplay);

      expect(warning).toExist();
      expect(warning).toHaveProp('id', 'user-token-modal-token-exists-alert');
      expect(warning).toHaveText(
        'A user token already exists for this user.To create a new token please delete the existing one.'
      );
      expect(userTokenDisplay).not.toExist();
    });

    it('renders UserTokenDisplay when there is userToken information to show', function () {
      const additionalProps = {
        userToken: {
          userCode: 'userCode',
          passCode: 'passCode',
        },
      };
      const component = getShallowComponent(additionalProps),
        modal = component.find(NxModal),
        content = modal.find('.nx-modal-content'),
        warning = content.find(NxWarningAlert),
        userTokenDisplay = content.find(UserTokenDisplay);

      expect(warning).not.toExist();
      expect(userTokenDisplay).toExist();
      expect(userTokenDisplay).toHaveProp('userToken', additionalProps.userToken);
    });
  });

  describe('modal footer', function () {
    it('renders a LoadError when the delete operation fails with appropriate props', function () {
      const component = getShallowComponent({
          deleteUserTokenError: 'Error on Delete',
        }),
        modal = component.find(NxModal),
        footer = modal.find('footer'),
        deleteLoadError = footer.find(LoadError);

      expect(deleteLoadError).toHaveProp('error', 'Error on Delete');
      expect(deleteLoadError).toHaveProp('retryHandler', minimalProps.deleteUserToken);
      expect(deleteLoadError).toHaveProp('titleMessage', 'An error occurred deleting the token.');
    });

    it('renders a LoadError when the generate token operation fails with appropriate props', function () {
      const component = getShallowComponent({
          generateUserTokenError: 'Error on Generate Token',
        }),
        modal = component.find(NxModal),
        footer = modal.find('footer'),
        deleteLoadError = footer.find(LoadError);

      expect(deleteLoadError).toHaveProp('error', 'Error on Generate Token');
      expect(deleteLoadError).toHaveProp('retryHandler', minimalProps.generateUserToken);
      expect(deleteLoadError).toHaveProp('titleMessage', 'An error occurred generating the token.');
    });

    describe('primary action button', function () {
      it('deletes user token when user token exists', function () {
        const component = getShallowComponent({ userToken: true }),
          modal = component.find(NxModal),
          footer = modal.find('footer'),
          deleteButton = footer.find(NxButton).at(0);

        expect(deleteButton).toHaveProp('id', 'delete-user-token');
        expect(deleteButton).toHaveText('Delete User Token');

        deleteButton.simulate('click');
        expect(minimalProps.deleteUserToken).toHaveBeenCalled();
      });

      it('generates user token when user token does not exists', function () {
        const component = getShallowComponent({ userToken: false }),
          modal = component.find(NxModal),
          footer = modal.find('footer'),
          generateButton = footer.find(NxButton).at(0);

        expect(generateButton).toHaveProp('id', 'generate-user-token');
        expect(generateButton).toHaveText('Generate User Token');

        generateButton.simulate('click');
        expect(minimalProps.generateUserToken).toHaveBeenCalled();
      });
    });

    describe('close button', function () {
      it('is rendered when no primary action is available', function () {
        const additionalPropsForComponent = {
          userToken: {
            userCode: 'userCode',
            passCode: 'passCode',
          },
        };
        const component = getShallowComponent(additionalPropsForComponent),
          modal = component.find(NxModal),
          footer = modal.find('footer'),
          closeButton = footer.find(NxButton);

        expect(closeButton).toHaveText('Close');
        expect(closeButton).toHaveProp('onClick', minimalProps.hideUserTokenModal);
        expect(closeButton).toHaveProp('variant', 'tertiary');
      });

      it('is rendered when any primary action is available', function () {
        const additionalPropsForComponent = {
          userToken: true,
        };
        const component = getShallowComponent(additionalPropsForComponent),
          modal = component.find(NxModal),
          footer = modal.find('footer'),
          closeButton = footer.find(NxButton).at(1);

        expect(closeButton).toHaveText('Close');
        expect(closeButton).toHaveProp('onClick', minimalProps.hideUserTokenModal);
        expect(closeButton).toHaveProp('variant', 'tertiary');
      });

      it('closes the modal when is pressed', function () {
        const additionalPropsForComponent = {
          userToken: true,
        };
        const component = getShallowComponent(additionalPropsForComponent),
          modal = component.find(NxModal),
          footer = modal.find('footer'),
          closeButton = footer.find(NxButton).at(1);

        closeButton.simulate('click');
        expect(minimalProps.hideUserTokenModal).toHaveBeenCalled();
      });
    });
  });

  it('calls checkUserTokenExistence if there is no token information', function () {
    getMountedComponent();
    expect(minimalProps.checkUserTokenExistence).toHaveBeenCalled();
  });

  it('does not call checkUserTokenExistence if there is token information loaded', function () {
    getMountedComponent({ userToken: false });
    expect(minimalProps.checkUserTokenExistence).not.toHaveBeenCalled();

    getMountedComponent({ userToken: true });
    expect(minimalProps.checkUserTokenExistence).not.toHaveBeenCalled();

    getMountedComponent({
      userToken: { userCode: 'userCode', passCode: 'passCode' },
    });
    expect(minimalProps.checkUserTokenExistence).not.toHaveBeenCalled();
  });
});
