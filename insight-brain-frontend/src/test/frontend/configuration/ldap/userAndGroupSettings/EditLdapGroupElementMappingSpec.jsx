/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxTextInput, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../../frontend/enzymeUtils';
import EditLdapGroupElementMapping, {
  groupTypes,
} from '../../../../../main/frontend/configuration/ldap/userAndGroupSettings/EditLdapGroupElementMapping';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('EditLdapGroupElementMapping', () => {
  let getShallowComponent;

  const setGroupMappingTypeMock = jasmine.createSpy('setGroupMappingType');
  const setGroupBaseDNMock = jasmine.createSpy('setGroupBaseDN');
  const setGroupSubtreeMock = jasmine.createSpy('setGroupSubtree');
  const setGroupObjectClassMock = jasmine.createSpy('setGroupObjectClass');
  const setGroupIDAttributeMock = jasmine.createSpy('setGroupIDAttribute');
  const setGroupMemberAttributeMock = jasmine.createSpy('setGroupMemberAttribute');
  const setGroupMemberFormatMock = jasmine.createSpy('setGroupMemberFormat');
  const setUserMemberOfGroupAttributeMock = jasmine.createSpy('setUserMemberOfGroupAttribute');
  const setDynamicGroupSearchMock = jasmine.createSpy('setDynamicGroupSearch');

  const minimalProps = {
    groupMappingType: 'NONE',
    groupBaseDN: initUserInput(''),
    groupSubtree: false,
    groupObjectClass: initUserInput(''),
    groupIDAttribute: initUserInput(''),
    groupMemberAttribute: initUserInput(''),
    groupMemberFormat: initUserInput(''),
    userMemberOfGroupAttribute: initUserInput(''),
    dynamicGroupSearchEnabled: true,
    setGroupMappingType: setGroupMappingTypeMock,
    setGroupBaseDN: setGroupBaseDNMock,
    setGroupSubtree: setGroupSubtreeMock,
    setGroupObjectClass: setGroupObjectClassMock,
    setGroupIDAttribute: setGroupIDAttributeMock,
    setGroupMemberAttribute: setGroupMemberAttributeMock,
    setGroupMemberFormat: setGroupMemberFormatMock,
    setUserMemberOfGroupAttribute: setUserMemberOfGroupAttributeMock,
    setDynamicGroupSearch: setDynamicGroupSearchMock,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(EditLdapGroupElementMapping, minimalProps);
  });

  const textFieldsAssert = (id, actionName, mock, changeValue = 42, options = {}) => {
    it(`calls ${actionName} action`, () => {
      const component = getShallowComponent(options);
      const input = component.find(`#${id}`);

      input.simulate('change', changeValue);
      expect(mock).toHaveBeenCalledWith(changeValue);
    });
  };

  describe('on render', () => {
    it('shows only group type selector of groupType === NONE', () => {
      const component = getShallowComponent();

      expect(component.find(NxTextInput).length).toBe(0);
    });

    it('shows groupType === STATIC related input fields', () => {
      const ids = [
        '#ldap-group-mapping-type',
        '#ldap-group-base-dn',
        '#ldap-group-subtree',
        '#ldap-group-object-class',
        '#ldap-group-id-attribute',
        '#ldap-group-member-attribute',
        '#ldap-group-member-format',
      ];
      const component = getShallowComponent({ groupMappingType: 'STATIC' });
      ids.forEach((id) => {
        expect(component.find(id)).toExist();
      });
    });

    it('shows groupType === DYNAMIC related input fields', () => {
      const ids = [
        '#ldap-group-mapping-type',
        '#ldap-user-member-of-group-attribute',
        '#ldap-dynamic-group-search-enabled',
      ];
      const component = getShallowComponent({ groupMappingType: 'DYNAMIC' });
      ids.forEach((id) => {
        expect(component.find(id)).toExist();
      });
    });

    describe('group type', () => {
      it('renders select element with correct value', () => {
        const component = getShallowComponent({ groupMappingType: 'NONE' });
        const select = component.find('#ldap-group-mapping-type');

        expect(select).toHaveProp('value', groupTypes[0]);
      });

      it('renders all groupType options', () => {
        const component = getShallowComponent();
        const options = component.find('#ldap-group-mapping-type > option');

        expect(options.length).toBe(3);

        expect(options.at(0).text()).toBe(groupTypes[0]);
        expect(options.at(1).text()).toBe(groupTypes[1]);
        expect(options.at(2).text()).toBe(groupTypes[2]);
      });

      it('calls setGroupMappingType when option changes', function () {
        const type = 'SIMPLE';
        const component = getShallowComponent();
        const select = component.find('#ldap-group-mapping-type');

        select.simulate('change', { target: { value: type } });
        expect(setGroupMappingTypeMock).toHaveBeenCalledWith(type);
      });
    });

    describe('inputs', () => {
      textFieldsAssert('ldap-group-base-dn', 'setGroupBaseDN', setGroupBaseDNMock, 42, { groupMappingType: 'STATIC' });
      textFieldsAssert('ldap-group-object-class', 'setGroupObjectClass', setGroupObjectClassMock, 42, {
        groupMappingType: 'STATIC',
      });
      textFieldsAssert('ldap-group-id-attribute', 'setGroupIDAttribute', setGroupIDAttributeMock, 42, {
        groupMappingType: 'STATIC',
      });
      textFieldsAssert('ldap-group-member-attribute', 'setGroupMemberAttribute', setGroupMemberAttributeMock, 42, {
        groupMappingType: 'STATIC',
      });
      textFieldsAssert('ldap-group-member-format', 'setGroupMemberFormat', setGroupMemberFormatMock, 42, {
        groupMappingType: 'STATIC',
      });

      it('calls setGroupSubtree action', () => {
        const component = getShallowComponent({ groupMappingType: 'STATIC' });
        const input = component.find('#ldap-group-subtree');

        input.simulate('change');
        expect(setGroupSubtreeMock).toHaveBeenCalled();
      });

      textFieldsAssert(
        'ldap-user-member-of-group-attribute',
        'setUserMemberOfGroupAttribute',
        setUserMemberOfGroupAttributeMock,
        42,
        { groupMappingType: 'DYNAMIC' }
      );

      it('calls setDynamicGroupSearch action', () => {
        const component = getShallowComponent({ groupMappingType: 'DYNAMIC' });
        const input = component.find('#ldap-dynamic-group-search-enabled');

        input.simulate('change');
        expect(setDynamicGroupSearchMock).toHaveBeenCalled();
      });
    });
  });
});
