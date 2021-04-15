/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import mainTemplate from './component.update.html';
import optionalTemplate from './component.update.optional.html';

export default function ComponentUpdateService(Modal) {
  return {
    reevaluate: function (componentKey, reevaluate) {
      if (componentKey) {
        Modal.open({
          template: mainTemplate,
          controller: 'component.update.controller as vm',
          backdrop: 'static',
          keyboard: false,
          resolve: {
            componentKey: function () {
              return componentKey;
            },
            reevaluate: function () {
              return reevaluate;
            },
          },
        });
      } else {
        Modal.open({
          template: optionalTemplate,
          controller: 'component.update.optional.controller as vm',
          backdrop: 'static',
          keyboard: false,
        });
      }
    },
  };
}

ComponentUpdateService.$inject = ['Modal'];
