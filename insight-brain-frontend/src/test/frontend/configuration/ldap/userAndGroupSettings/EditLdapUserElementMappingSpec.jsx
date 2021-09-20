/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../../frontend/enzymeUtils';
import EditLdapUserElementMapping from '../../../../../main/frontend/configuration/ldap/userAndGroupSettings/EditLdapUserElementMapping';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('EditLdapUserElementMapping', () => {
  let getShallowComponent;

  const setUserBaseDNMock = jasmine.createSpy('setUserBaseDN');
  const setUserSubtreeMock = jasmine.createSpy('setUserSubtree');
  const setUserObjectClassMock = jasmine.createSpy('setUserObjectClass');
  const setUserFilterMock = jasmine.createSpy('setUserFilter');
  const setUserIDAttributeMock = jasmine.createSpy('setUserIDAttribute');
  const setUserRealNameAttributeMock = jasmine.createSpy('setUserRealNameAttribute');
  const setUserEmailAttributeMock = jasmine.createSpy('setUserEmailAttribute');
  const setUserPasswordAttributeMock = jasmine.createSpy('setUserPasswordAttribute');

  const minimalProps = {
    userBaseDN: initUserInput(''),
    userSubtree: false,
    userObjectClass: initUserInput(''),
    userFilter: initUserInput(''),
    userIDAttribute: initUserInput(''),
    userRealNameAttribute: initUserInput(''),
    userEmailAttribute: initUserInput(''),
    userPasswordAttribute: initUserInput(''),
    setUserBaseDN: setUserBaseDNMock,
    setUserSubtree: setUserSubtreeMock,
    setUserObjectClass: setUserObjectClassMock,
    setUserFilter: setUserFilterMock,
    setUserIDAttribute: setUserIDAttributeMock,
    setUserRealNameAttribute: setUserRealNameAttributeMock,
    setUserEmailAttribute: setUserEmailAttributeMock,
    setUserPasswordAttribute: setUserPasswordAttributeMock,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(EditLdapUserElementMapping, minimalProps);
  });

  const textFieldsAssert = (id, actionName, mock, changeValue = 42) => {
    it(`calls ${actionName} action`, () => {
      const component = getShallowComponent();
      const input = component.find(`#${id}`);

      input.simulate('change', changeValue);
      expect(mock).toHaveBeenCalledWith(changeValue);
    });
  };

  describe('on render', () => {
    textFieldsAssert('ldap-user-base-dn', 'setUserBaseDN', setUserBaseDNMock);
    textFieldsAssert('ldap-user-object-class', 'setUserObjectClass', setUserObjectClassMock);
    textFieldsAssert('ldap-user-filter', 'setUserFilter', setUserFilterMock);
    textFieldsAssert('ldap-user-id-attribute', 'setUserIDAttribute', setUserIDAttributeMock);
    textFieldsAssert('ldap-user-real-name-attribute', 'setUserRealNameAttribute', setUserRealNameAttributeMock);

    textFieldsAssert('ldap-user-email-attribute', 'setUserEmailAttribute', setUserEmailAttributeMock);
    textFieldsAssert('ldap-user-password-attribute', 'setUserPasswordAttribute', setUserPasswordAttributeMock);

    it('calls setUserSubtree action', () => {
      const component = getShallowComponent();
      const input = component.find('#ldap-user-subtree');

      input.simulate('change');
      expect(setUserSubtreeMock).toHaveBeenCalled();
    });
  });
});
