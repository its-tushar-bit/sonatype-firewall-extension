/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxButton, NxTable } from '@sonatype/react-shared-components';
import LdapUserMapping from '../../../../../main/frontend/configuration/ldap/userAndGroupSettings/LdapUserMapping';
import * as enzymeUtils from '../../../enzymeUtils';

describe('ldapUserMapping', () => {
  let getShallowComponent;

  beforeEach(() => {
    const initialProps = {
      userList: null,
      loadError: null,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LdapUserMapping, initialProps);
  });

  describe('on load', () => {
    it('renders table with isLoading equals true', () => {
      const component = getShallowComponent();
      const tableBody = component.find(NxTable.Body);

      expect(component.find(NxTable)).toExist();
      expect(tableBody).toExist();
      expect(tableBody).toHaveProp('isLoading', true);
    });
    it('renders close button', () => {
      const component = getShallowComponent();

      expect(component.find(NxButton)).toExist();
    });
  });

  it('calls toggleUserMappingModalIsOpen when click on cancel button', () => {
    const toggleUserMappingModalIsOpenSpy = jasmine.createSpy('toggleUserMappingModalIsOpenSpy');
    const component = getShallowComponent({ toggleUserMappingModalIsOpen: toggleUserMappingModalIsOpenSpy });
    const closeButton = component.find(NxButton);
    closeButton.simulate('click');

    expect(toggleUserMappingModalIsOpenSpy).toHaveBeenCalled();
  });
  it('calls toggleUserMappingSortOrder when click on username column header', () => {
    const toggleUserMappingSortOrderSpy = jasmine.createSpy('toggleUserMappingSortOrderSpy');
    const component = getShallowComponent({ toggleUserMappingSortOrder: toggleUserMappingSortOrderSpy });
    const usernameHeader = component.find(NxTable.Cell);

    usernameHeader.at(0).simulate('click');

    expect(toggleUserMappingSortOrderSpy).toHaveBeenCalled();
  });

  describe('sort', () => {
    let initialUsers;
    beforeAll(() => {
      initialUsers = [
        {
          username: 'newton',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=newton,dc=example,dc=com',
          realName: 'Isaac Newton',
          email: 'newton@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'einstein',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=einstein,dc=example,dc=com',
          realName: 'Albert Einstein',
          email: 'einstein@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'tesla',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=tesla,dc=example,dc=com',
          realName: 'Nikola Tesla',
          email: 'tesla@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'galieleo',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=galieleo,dc=example,dc=com',
          realName: 'Galileo Galilei',
          email: 'galieleo@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'euler',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=euler,dc=example,dc=com',
          realName: 'Leonhard Euler',
          email: 'euler@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'gauss',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=gauss,dc=example,dc=com',
          realName: 'Carl Friedrich Gauss',
          email: 'gauss@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'riemann',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=riemann,dc=example,dc=com',
          realName: 'Bernhard Riemann',
          email: 'riemann@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'euclid',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=euclid,dc=example,dc=com',
          realName: 'Euclid',
          email: 'euclid@ldap.forumsys.com',
          membership: null,
        },
        {
          username: null,
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'cn=read-only-admin,dc=example,dc=com',
          realName: 'read-only-admin',
          email: null,
          membership: null,
        },
        {
          username: 'test',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=test,dc=example,dc=com',
          realName: 'Test',
          email: null,
          membership: null,
        },
        {
          username: 'curie',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=curie,dc=example,dc=com',
          realName: 'Marie Curie',
          email: 'curie@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'nobel',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=nobel,dc=example,dc=com',
          realName: 'Alfred Nobel',
          email: 'nobel@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'boyle',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=boyle,dc=example,dc=com',
          realName: 'Robert Boyle',
          email: 'boyle@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'pasteur',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=pasteur,dc=example,dc=com',
          realName: 'Louis Pasteur',
          email: 'pasteur@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'nogroup',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=nogroup,dc=example,dc=com',
          realName: 'No Group',
          email: 'nogroup@ldap.forumsys.com',
          membership: null,
        },
        {
          username: 'training',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=training,dc=example,dc=com',
          realName: 'FS Training',
          email: 'training@forumsys.com',
          membership: null,
        },
        {
          username: 'jmacy',
          password: null,
          serverId: 'a55a8f5821644692acecb9dea727ee23',
          dn: 'uid=jmacy,dc=example,dc=com',
          realName: 'FS Training',
          email: 'jmacy-training@forumsys.com',
          membership: null,
        },
      ];
    });

    it('renders rows in ascending order when sortAscending is true', () => {
      const component = getShallowComponent({ userList: initialUsers, sortAscending: true });
      const body = component.find(NxTable.Body);
      const bodyCells = body.find(NxTable.Cell);
      const firstCell = bodyCells.at(0);

      expect(firstCell.dive()).toHaveText('boyle');
    });

    it('renders rows in descending order when sortAscending is false', () => {
      const component = getShallowComponent({ userList: initialUsers, sortAscending: false });
      const body = component.find(NxTable.Body);
      const bodyCells = body.find(NxTable.Cell);
      const NameCell = bodyCells.at(1);

      expect(NameCell.dive()).toHaveText('read-only-admin');
    });
  });
});
