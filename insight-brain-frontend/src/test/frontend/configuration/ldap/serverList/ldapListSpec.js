/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxLoadWrapper, NxPageMain } from '@sonatype/react-shared-components';
import LdapList from '../../../../../main/frontend/configuration/ldap/ldapServersList/LdapList';
import * as enzymeUtils from '../../../enzymeUtils';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

describe('LdapList', () => {
  let minimalProps, mockLoadServers, mockStateGo, getShallowComponent;

  beforeEach(() => {
    mockLoadServers = jasmine.createSpy('loadServers');
    mockStateGo = jasmine.createSpy('stateGo');
    minimalProps = {
      loading: false,
      servers: [],
      loadServers: mockLoadServers,
      stateGo: mockStateGo,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LdapList, minimalProps);
  });

  it('renders a NxPageMain component', () => {
    expect(getShallowComponent().find(NxPageMain)).toExist();
  });

  it('shows the no ldap servers to list message', () => {
    const shallowComponent = getShallowComponent();
    const noLdapServersMessage = shallowComponent.find('.nx-list__item--empty');
    expect(noLdapServersMessage).toExist();
  });

  it('lists two ldap servers', () => {
    const servers = [
      { id: 'LDAP-SERVER-1', name: 'LDAP-SERVER-1' },
      { id: 'LDAP-SERVER-2', name: 'LDAP-SERVER-2' },
    ];
    const shallowComponent = getShallowComponent({ servers });
    const listedServers = shallowComponent.find('.nx-list__item');
    expect(listedServers.length).toBe(2);
  });

  it('calls stateGo when a server is clicked', () => {
    const servers = [{ id: 'LDAP-SERVER-1', name: 'LDAP-SERVER-1' }];
    const shallowComponent = getShallowComponent({ servers });
    const ldapServer = shallowComponent.find('.nx-list__item').at(0);
    ldapServer.simulate('click');
    expect(mockStateGo).toHaveBeenCalledWith('edit-ldap-connection', { ldapId: 'LDAP-SERVER-1' });
  });

  it('has loading prop', () => {
    const shallowComponent = getShallowComponent();
    const loadWrapper = shallowComponent.find(NxLoadWrapper);
    expect(loadWrapper).toHaveProp('loading', false);
  });

  it('has load error prop', () => {
    const shallowComponent = getShallowComponent({ loadError: 'some error' });
    const loadWrapper = shallowComponent.find(NxLoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'some error');
  });

  describe('reorder list', () => {
    describe('reorder list button', () => {
      it('is disabled when server list is empty', () => {
        const component = getShallowComponent();
        const reorderBtn = component.find('#reorder-ldap-list-btn');
        expect(reorderBtn).toHaveProp('disabled', true);
      });

      it('is disabled when there is only one server', () => {
        const component = getShallowComponent({
          servers: [{ id: '1' }],
        });
        const reorderBtn = component.find('#reorder-ldap-list-btn');
        expect(reorderBtn).toHaveProp('disabled', true);
      });

      it('is enabled when there is more than one server', () => {
        const component = getShallowComponent({
          servers: [{ id: '1' }, { id: '2' }],
        });
        const reorderBtn = component.find('#reorder-ldap-list-btn');
        expect(reorderBtn).toHaveProp('disabled', false);
      });

      it('dispatches enterReorderMode action', () => {
        const mockEnterReorderMode = jasmine.createSpy('enterReorderMode');
        const component = getShallowComponent({
          servers: [{ id: '1' }, { id: '2' }],
          enterReorderMode: mockEnterReorderMode,
        });
        const reorderBtn = component.find('#reorder-ldap-list-btn');
        reorderBtn.simulate('click');
        expect(mockEnterReorderMode).toHaveBeenCalled();
      });
    });

    describe('reorder mode', () => {
      let props, mockMoveServerUpInTheList, mockSaveOrder, mockOnCancel;
      beforeEach(() => {
        mockMoveServerUpInTheList = jasmine.createSpy('moveServerUpInTheList');
        const servers = [
          { id: '1', name: 'first' },
          { id: '2', name: 'second' },
          { id: '3', name: 'third' },
        ];
        props = {
          servers,
          reorderedServers: [servers[1], servers[2], servers[0]], // order has changed
          moveServerUpInTheList: mockMoveServerUpInTheList,
          saveOrder: mockSaveOrder,
          onCancel: mockOnCancel,
          saveServerOrderSuccess: 'save-order-success-state',
          saveServerOrderError: 'save-order-error',
        };
      });

      it('disables "Reorder List" button', () => {
        const component = getShallowComponent(props);
        const reorderBtn = component.find('#reorder-ldap-list-btn');
        expect(reorderBtn).toHaveProp('disabled', true);
      });

      it('disables "Add a Server" button', () => {
        const component = getShallowComponent(props);
        const reorderBtn = component.find('#add-ldap-server-btn');
        expect(reorderBtn).toHaveProp('disabled', true);
      });

      it('renders form to submit reordered list', () => {
        const component = getShallowComponent(props);
        const form = component.find('#reorder-ldap-servers-form');
        expect(form).toHaveProp('onSubmit', mockSaveOrder);
        expect(form).toHaveProp('onCancel', mockOnCancel);
        expect(form).toHaveProp('submitMaskState', 'save-order-success-state');
        expect(form).toHaveProp('submitError', 'save-order-error');
        expect(form).toHaveProp('submitBtnText', 'Save Order');
      });

      it('enables submit button if state is dirty', () => {
        props.isDirty = true;
        const component = getShallowComponent(props);
        const form = component.find('#reorder-ldap-servers-form');
        expect(form).toHaveProp('validationErrors', undefined);
      });

      it('disables submit button with proper tooltip message if state is not dirty', () => {
        props.isDirty = false;
        const component = getShallowComponent(props);
        const form = component.find('#reorder-ldap-servers-form');
        expect(form).toHaveProp('validationErrors', MSG_NO_CHANGES_TO_SAVE);
      });

      it('renders list using reorderedServers state', () => {
        const component = getShallowComponent(props);
        const serverList = component.find('#ldap-server-list');
        expect(serverList.childAt(0)).toIncludeText('second');
        expect(serverList.childAt(1)).toIncludeText('third');
        expect(serverList.childAt(2)).toIncludeText('first');
      });

      it('renders non-clickable list', () => {
        const component = getShallowComponent(props);
        const serverList = component.find('#ldap-server-list');
        expect(serverList.children().length).toBe(3);
        serverList.children().forEach((listItem) => {
          expect(listItem).toHaveClassName('nx-list__item');
          expect(listItem).not.toHaveClassName('nx-list__item--clickable');
          expect(listItem).not.toHaveProp('onClick');
        });
      });

      describe('up and down arrow buttons', () => {
        describe('for the first item', () => {
          let arrowButtons;
          beforeEach(() => {
            const component = getShallowComponent(props);
            arrowButtons = component.find('#ldap-server-list').childAt(0).find('.nx-list__actions');
          });

          it('disables the up button', () => {
            expect(arrowButtons.childAt(0)).toHaveProp('disabled', true);
            expect(arrowButtons.childAt(1)).toHaveProp('disabled', false);
          });

          it('handles click event for the down button', () => {
            arrowButtons.childAt(1).simulate('click');
            expect(mockMoveServerUpInTheList).toHaveBeenCalledWith(1);
          });
        });

        describe('for the second item', () => {
          let arrowButtons;
          beforeEach(() => {
            const component = getShallowComponent(props);
            arrowButtons = component.find('#ldap-server-list').childAt(1).find('.nx-list__actions');
          });

          it('enables both buttons', () => {
            expect(arrowButtons.childAt(0)).toHaveProp('disabled', false);
            expect(arrowButtons.childAt(1)).toHaveProp('disabled', false);
          });

          it('handles click event for the up button', () => {
            arrowButtons.childAt(0).simulate('click');
            expect(mockMoveServerUpInTheList).toHaveBeenCalledWith(1);
          });

          it('handles click event for the down button', () => {
            arrowButtons.childAt(1).simulate('click');
            expect(mockMoveServerUpInTheList).toHaveBeenCalledWith(2);
          });
        });

        describe('for the last item', () => {
          let arrowButtons;
          beforeEach(() => {
            const component = getShallowComponent(props);
            arrowButtons = component.find('#ldap-server-list').childAt(2).find('.nx-list__actions');
          });

          it('disables the down button', () => {
            expect(arrowButtons.childAt(0)).toHaveProp('disabled', false);
            expect(arrowButtons.childAt(1)).toHaveProp('disabled', true);
          });

          it('handles click event for the up button', () => {
            arrowButtons.childAt(0).simulate('click');
            expect(mockMoveServerUpInTheList).toHaveBeenCalledWith(2);
          });
        });
      });
    });
  });
});
