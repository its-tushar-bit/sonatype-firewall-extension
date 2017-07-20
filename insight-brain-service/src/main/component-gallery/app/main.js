/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import galleryModule from './gallery/module';
import configModule from '../config';

var module = angular.module('galleryApp', ['ui.router', 'hljs', 'hc.marked', galleryModule.name, configModule.name]);

module.config(function($stateProvider, $urlRouterProvider, markedProvider, hljsServiceProvider, componentsConfig,
                       directivesConfig, stylesConfig)
{
  hljsServiceProvider.setOptions({
    languages: ['html', 'js']
  });

  markedProvider.setOptions({
    gfm: true,
    breaks: true,
    tables: true,
    highlight: function (code, lang) {
      if (lang) {
        return hljs.highlight(lang, code, true).value;
      } else {
        return hljs.highlightAuto(code).value;
      }
    }
  });

  $urlRouterProvider.otherwise('/home');
  $stateProvider.state('home', {
    url: '/home',
    templateUrl: 'app/home.html',
    data: {
      title: 'Gallery Home'
    }
  });

  // add configured states
  angular.forEach(componentsConfig, configureState);
  angular.forEach(directivesConfig, configureState);
  angular.forEach(stylesConfig, configureState);

  function configureState(templateUrl, state) {
    $stateProvider.state(state, {
      url: '/' + state,
      template: '<page-wrapper page-url="' + templateUrl + '"></page-wrapper>'
    })
  }
});
