/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './navigation.html';

var navigationComponent = {
  selector: 'navigation',
  template: template,
  controller: navigationController,
  controllerAs: 'vm'
};

function navigationController(componentsConfig, directivesConfig, stylesConfig) {
  var vm = this;

  vm.components = Object.keys(componentsConfig);
  vm.directives = Object.keys(directivesConfig);
  vm.styles = Object.keys(stylesConfig);
}

export default navigationComponent;
