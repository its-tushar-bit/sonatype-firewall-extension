/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ReactStateDeclaration } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import HelloWorld1 from './pages/HelloWorld1';
import HelloWorld2 from './pages/HelloWorld2';

router.stateRegistry.register({
  name: 'root',
  url: '^',
  redirectTo: 'hello1',
});

router.stateRegistry.register({
  name: 'home',
  url: '/',
  redirectTo: 'hello1',
});

router.stateRegistry.register({
  name: 'hello1',
  url: '/hello1',
  component: HelloWorld1,
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'hello2',
  url: '/hello2',
  component: HelloWorld2,
} as ReactStateDeclaration);

router.urlService.rules.otherwise('/hello1');
