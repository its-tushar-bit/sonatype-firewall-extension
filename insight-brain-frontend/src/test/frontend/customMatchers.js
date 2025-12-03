/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { equals } from 'ramda';

const customMatchers = {
  //Example usage: expect(store.getActions()).toHaveActionType('SOME_ACTION_TYPE');
  toHaveActionType: function () {
    return {
      /**
       * Checks if `actionType` is the `type` of one of the entries in `actions`.
       *
       * @param {Array} actions the actions in a redux store.
       * @param {String} action the action-type to look for in the actions array
       */
      compare: function (actions, actionType) {
        const filtered = actions.filter(({ type }) => type === actionType);

        return filtered.length > 0
          ? { pass: true }
          : {
              pass: false,
              message: `Expected ${actionType} to be the type of an action included in ${JSON.stringify(actions)}`,
            };
      },
    };
  },

  // Example usage: expect(store.getActions()).toHaveAction({ type: 'SOME_ACTION_TYPE', payload: 'SOME_PAYLOAD' });
  toHaveAction: function () {
    return {
      /**
       * Checks if `action` is included in `actions`.
       *
       * @param {Array} actions the actions in a redux store
       * @param {*} action the action { type, payload } object to look for in the actions array
       */
      compare: function (actions, action) {
        const { type, payload } = action;
        const filtered = actions.filter((act) => act.type === type && equals(act.payload, payload));

        return filtered.length > 0
          ? { pass: true }
          : {
              pass: false,
              message: `Expected { type: ${type}, payload: ${payload} } to be included in ${JSON.stringify(actions)}`,
            };
      },
    };
  },

  /**
   *  Example usage: expect(store.getActions()).toHaveActionsInOrder([
   *    { type: 'SOME_ACTION_TYPE', payload: 'SOME_PAYLOAD' },
   *    { type: 'SOME_ACTION_TYPE_2', payload: 'SOME_PAYLOAD_2' }
   *  ]);
   */
  toHaveActionsInOrder: function () {
    return {
      /**
       * Checks if `expectedActions` are included in `actions` in the exact same order
       * that they're provided.
       *
       * Note that `actions` can include a lot more action-objects than those expected,
       * and as long as it contains the expected ones in the expected order it will still
       * pass.
       *
       * @param {Array} actions the actions in a redux store
       * @param {Array} expectedActions the actions that should be inside `actions`
       */
      compare: function (actions, expectedActions) {
        const remainingActions = [...actions],
          remainingExpectedActions = [...expectedActions];

        while (remainingExpectedActions.length) {
          // stringify to deeply-remove properties with undefined values
          while (
            remainingActions.length &&
            JSON.stringify(remainingActions[0]) !== JSON.stringify(remainingExpectedActions[0])
          ) {
            remainingActions.shift();
          }

          if (remainingActions.length) {
            remainingExpectedActions.shift();
          } else {
            const currentActionString = JSON.stringify(remainingExpectedActions[0]);
            return {
              pass: false,
              message: `Expected ${currentActionString} to be included in ${JSON.stringify(actions)}`,
            };
          }
        }

        return { pass: true };
      },
    };
  },

  /**
   *  Example usage: expect(store.getActions()).toHaveActionTypesInOrder([
   *    'SOME_ACTION_TYPE',
   *    'SOME_ACTION_TYPE_2'
   *  ]);
   */
  toHaveActionTypesInOrder: function () {
    return {
      /**
       * Checks if `expectedActionTypes` are included in `actions` in the exact same order
       * that they're provided.
       * Payloads are disregarded in this function.
       *
       * Note that `actions` can include a lot more action-objects than those expected,
       * and as long as it contains the expected ones in the expected order it will still
       * pass.
       *
       * @param {Array} actions the actions in a redux store
       * @param {Array} expectedActionTypes a list of action-types that should be inside `actions`
       */
      compare: function (actions, expectedActionTypes) {
        const actionTypes = actions.map((action) => action.type);
        const actionsTypesString = JSON.stringify(actionTypes),
          // convert to string but get rid of the array characters
          expectedActionsString = JSON.stringify(expectedActionTypes).slice(1, -1),
          // compare the strings.
          areActionsIncluded = actionsTypesString.includes(expectedActionsString);

        return areActionsIncluded
          ? { pass: true }
          : {
              pass: false,
              message: makeMessage(`Expected ${expectedActionsString} to be included in ${actionsTypesString}`),
            };
      },
    };
  },
};

function makeMessage(msg) {
  const fn = () => msg; // Jest uses this
  fn.toString = () => msg; // Jasmine uses this
  return fn;
}

export default customMatchers;
