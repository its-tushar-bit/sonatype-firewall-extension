/**
 * @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 *
 *
 * @license The MIT License (MIT)
 *
 * Copyright (c) 2014 Max Wihlborg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
/* jshint ignore:start */
(function() {
  var

  /**
   * Game objects
   */
  backdrop,
  title,
  titleAlpha,
  instructions,
  instructionsDelay,
  instructionsAlpha,
  screen,
  input,
  frames,
  spFrame,
  lvFrame,
  isGameLost,
  isGameWon,
  isGameOver,
  lives = 3,

  alSprite,
  taSprite,
  ciSprite,

  aliens,
  dir,
  tank,
  bullets,
  cities,
  alienName,
  alienNameAlpha,

  winCallback,
  loseCallback,

  // settings
  screenWidth = 504,
  screenHeight = 600,
  fontSize = 20,
  font = fontSize + 'pt Courier New',
  buffer = 30;

  /**
   * Register angular factory with function to start space invaders game
   */
  var spaceInvaders = angular.module('SpaceInvaders', []);
  spaceInvaders.factory('spaceInvaders', function() {
    return {
      /**
       * Takes parameter alienInvaders, a row or alien data containing name and level (0-2)
       */
      run: function(alienInvaders, titleText, instructionsText, winFn, loseFn) {
        // Disable the scroll function of the spacebar
        window.onkeydown = function(e) {
          return e.keyCode !== 32;
        };

        // Add bootstrap modal backdrop
        backdrop = angular.element('<div class="modal-backdrop"></div>').appendTo(document.body);

        // Set win and lose function
        winCallback = winFn;
        loseCallback = loseFn;

        // Set title and instruction text
        title = titleText;
        instructions = instructionsText;

        // create game canvas and inputhandeler
        screen = new Screen(screenWidth, screenHeight);
        screen.ctx.font = fontSize + 'pt Courier New';
        input = new InputHandeler();

        // create all sprites fram assets image
        var img = new Image();
        img.addEventListener('load', function() {

          alSprite = [
            [new Sprite(this,  0, 0, 22, 16), new Sprite(this,  0, 16, 22, 16)],
            [new Sprite(this, 22, 0, 16, 16), new Sprite(this, 22, 16, 16, 16)],
            [new Sprite(this, 38, 0, 24, 16), new Sprite(this, 38, 16, 24, 16)]
          ];
          taSprite = new Sprite(this, 62, 0, 22, 16);
          ciSprite = new Sprite(this, 84, 8, 36, 24);

          // initate and run the game
          init(alienInvaders);
          run();
        });
        img.src = '../assets/img/invaders.png';
      }
    };
  });

  /**
   * Initate game objects
   */
  function init(alienInvaders) {
    isGameLost = false;
    isGameWon = false;
    isGameOver = false;
    lives = 3;

    titleAlpha = 1.0;
    instructionsDelay = 10.0;
    instructionsAlpha = 1.0;
    alienNameAlpha = 0;

    // set start settings
    frames  = 0;
    spFrame = 0;
    lvFrame = 60;

    dir = 1;

    // create the tank object
    tank = {
      sprite: taSprite,
      x: (screen.width - taSprite.w) / 2,
      y: screen.height - (buffer + taSprite.h),
      lives: lives,
      hits: function(bullet) {
        // Cannot be hit by own bullets
        if (bullet.vely < 0) {
          return false;
        }
        if (AABBIntersect(this.x, this.y, taSprite.w, taSprite.h, bullet.x, bullet.y, bullet.width, bullet.height)) {
          this.lives--;
          return true;
        }
        return false;
      }
    };

    // initatie bullet array
    bullets = [];

    // create the cities object (and canvas)
    cities = {
      canvas: null,
      ctx:  null,

      y: tank.y - (buffer + ciSprite.h),
      h: ciSprite.h,

      /**
       * Create canvas and game graphic context
       */
      init: function() {
        // create canvas and grab 2d context
        this.canvas = document.createElement('canvas');
        this.canvas.width = screen.width;
        this.canvas.height = this.h;
        this.ctx = this.canvas.getContext('2d');

        for (var i = 0; i < 4; i++) {
          this.ctx.drawImage(ciSprite.img, ciSprite.x, ciSprite.y,
            ciSprite.w, ciSprite.h,
            68 + 111*i, 0, ciSprite.w, ciSprite.h);
        }
      },

      /**
       * Create damage effect on city-canvas
       * 
       * @param  {number} x x-coordinate
       * @param  {number} y y-coordinate
       */
      generateDamage: function(x, y) {
        // round x, y position
        x = Math.floor(x/2) * 2;
        y = Math.floor(y/2) * 2;
        // draw dagame effect to canva
        this.ctx.clearRect(x-2, y-2, 4, 4);
        this.ctx.clearRect(x+2, y-4, 2, 4);
        this.ctx.clearRect(x+4, y, 2, 2);
        this.ctx.clearRect(x+2, y+2, 2, 2);
        this.ctx.clearRect(x-4, y+2, 2, 2);
        this.ctx.clearRect(x-6, y, 2, 2);
        this.ctx.clearRect(x-4, y-4, 2, 2);
        this.ctx.clearRect(x-2, y-6, 2, 2);
      },

      /**
       * Check if pixel at (x, y) is opaque
       * 
       * @param  {number} x x-coordinate
       * @param  {number} y y-coordinate
       * @return {bool}     boolean value if pixel opaque
       */
      hits: function(x, y) {
        // transform y value to local coordinate system
        y -= this.y;
        // get imagedata and check if opaque
        var data = this.ctx.getImageData(x, y, 1, 1);
        if (data.data[3] !== 0) {
          this.generateDamage(x, y);
          return true;
        }
        return false;
      }
    };
    cities.init(); // initiate the cities

    // create and populate alien array
    aliens = [];
    var column = 0;
    var row = 0;
    for (var i = 0; i < alienInvaders.length; i++) {
      var alien = alienInvaders[i], spritePosition;
      // Adjust level to spritePosition in sprite sheet
      switch (alien.level) {
        case 0:
          spritePosition = 2;
          break;
        case 1:
          spritePosition = 0;
          break;
        case 2:
          spritePosition = 1;
          break;
      }
      aliens.push({
        sprite: alSprite[spritePosition],
        x: buffer + column*30 + [0, 4, 0][spritePosition],
        y: buffer + row*30,
        w: alSprite[spritePosition][0].w,
        h: alSprite[spritePosition][0].h,
        name: alien.name
      });
      if (column++ === 9) {
        column = 0;
        row++;
      }
    }
  }

  /**
   * Wrapper around the game loop function, updates and renders
   * the game
   */
  function run() {
    var loop = function() {
      // Title variable is set to falsey after rendered or if not provided
      if (title) {
        renderTitle();
      } else if (instructions) {
        renderInstructions();
      } else if (!isGameLost && !isGameWon) {
        update();
        render();
      } else if (!isGameOver) {
        isGameOver = true;
        backdrop.remove();
        screen.canvas.remove();
        if (isGameWon) {
          if (winCallback) {
            winCallback();
          }
        } else {
          if (loseCallback) {
            loseCallback();
          }
        }
      }

      if (!isGameOver) {
        window.requestAnimationFrame(loop, screen.canvas);
      }
    };
    window.requestAnimationFrame(loop, screen.canvas);
  }

  function renderTitle() {
    screen.clear();
    if (titleAlpha > 0) {
      screen.ctx.fillStyle = 'rgba(255, 0, 0, ' + titleAlpha + ')';
      var titleMetrics = screen.ctx.measureText(title);
      screen.ctx.fillText(title, (screen.width - titleMetrics.width) / 2, (screen.height - fontSize) / 2);
      titleAlpha -= 0.01;
    } else {
      title = undefined;
    }
  }

  function renderInstructions() {
    screen.clear();
    if (instructionsAlpha > 0) {
      screen.ctx.font = (fontSize - 2) + 'pt Courier New';
      screen.ctx.fillStyle = 'rgba(255, 0, 0, ' + instructionsAlpha + ')';
      var lines = instructions.split('\n');
      for (var i = 0; i < lines.length; i++) {
        var text = lines[i];
        var textMetrics = screen.ctx.measureText(text);
        var height = (screen.height - (fontSize - 2)) / 2 - (lines.length / 2 - i) * (fontSize - 2);
        screen.ctx.fillText(text, (screen.width - textMetrics.width) / 2, height);

        if (instructionsDelay > 0) {
          instructionsDelay -= 0.01;
        } else {
          instructionsAlpha -= 0.01;
        }
      }
    } else {
      screen.ctx.font = fontSize + 'pt Courier New';
      instructions = undefined;
    }
  }

  /**
   * Update the game logic
   */
  function update() {
    // update the frame count
    frames++;

    // update tank position depending on pressed keys
    if (input.isDown(37)) { // Left
      tank.x -= 4;
    }
    if (input.isDown(39)) { // Right
      tank.x += 4;
    }
    // keep the tank sprite inside of the canvas
    tank.x = Math.max(Math.min(tank.x, screen.width - (buffer + taSprite.w)), buffer);

    // append new bullet to the bullet array if spacebar is
    // pressed
    if (input.isPressed(32)) { // Space
      bullets.push(new Bullet(tank.x + 10, tank.y, -8, 2, 6, '#fff'));
    }

    // update all bullets position and checks
    for (var i = 0, len = bullets.length; i < len; i++) {
      var b = bullets[i];
      b.update();
      // remove bullets outside of the canvas
      if (b.y + b.height < 0 || b.y > screen.height) {
        bullets.splice(i, 1);
        i--;
        len--;
        continue;
      }

      // check if bullet hits any tank
      if (tank.hits(b)) {
        if (tank.lives <= 0) {
          isGameLost = true;
          break;
        }

        bullets.splice(i, 1);
        i--;
        len--;
        continue;
      }

      // check if bullet hits any city
      var h2 = b.height * 0.5; // half height is used for
                   // simplicity
      if (cities.y < b.y+h2 && b.y+h2 < cities.y + cities.h) {
        if (cities.hits(b.x, b.y+h2)) {
          bullets.splice(i, 1);
          i--;
          len--;
          continue;
        }
      }
      // check if bullet hit any aliens
      // can only be hit by players bullets
      if (b.vely < 0) {
        for (var j = 0, len2 = aliens.length; j < len2; j++) {
          var a = aliens[j];
          if (AABBIntersect(b.x, b.y, b.width, b.height, a.x, a.y, a.w, a.h)) {
            alienName = a.name;
            alienNameAlpha = 1.0;

            aliens.splice(j, 1);
            j--;
            len2--;
            bullets.splice(i, 1);
            i--;
            len--;
            // increase the movement frequence of the aliens
            // when there are less of them
            // set game won when all aliens are gone
            switch (len2) {
              case 30: {
                lvFrame = 40;
                break;
              }
              case 10: {
                lvFrame = 20;
                break;
              }
              case 5: {
                lvFrame = 15;
                break;
              }
              case 1: {
                lvFrame = 6;
                break;
              }
              case 0: {
                isGameWon = true;
                break;
              }
            }
          }
        }
      }
    }
    // makes the alien shoot in an random fashion 
    if (Math.random() < 0.03 && aliens.length > 0) {
      var a = aliens[Math.round(Math.random() * (aliens.length - 1))];
      // iterate through aliens and check collision to make
      // sure only shoot from front line
      for (var i = 0, len = aliens.length; i < len; i++) {
        var b = aliens[i];

        if (AABBIntersect(a.x, a.y, a.w, 100, b.x, b.y, b.w, b.h)) {
          a = b;
        }
      }
      // create and append new bullet
      bullets.push(new Bullet(a.x + a.w*0.5, a.y + a.h, 4, 2, 4, '#fff'));
    }
    // update the aliens at the current movement frequence
    if (frames % lvFrame === 0) {
      spFrame = (spFrame + 1) % 2;

      var _max = 0, _min = screen.width;
      // iterate through aliens and update postition
      for (var i = 0, len = aliens.length; i < len; i++) {
        var a = aliens[i];
        a.x += 30 * dir;
        // find min/max values of all aliens for direction
        // change test
        _max = Math.max(_max, a.x + a.w);
        _min = Math.min(_min, a.x);
      }
      // check if aliens should move down and change direction
      if (_max > screen.width - buffer || _min < buffer) {
        // mirror direction and update position
        dir *= -1;
        for (var i = 0, len = aliens.length; i < len; i++) {
          aliens[i].x += 30 * dir;
          aliens[i].y += 30;

          if (aliens[i].y > cities.y) {
            isGameLost = true;
            break;
          }
        }
      }
    }
  };

  /**
   * Render the game state to the canvas
   */
  function render() {
    screen.clear(); // clear the game canvas

    // draw all aliens
    for (var i = 0, len = aliens.length; i < len; i++) {
      var a = aliens[i];
      screen.drawSprite(a.sprite[spFrame], a.x, a.y);
    }
    // save contetx and draw bullet then restore
    screen.ctx.save();
    for (var i = 0, len = bullets.length; i < len; i++) {
      screen.drawBullet(bullets[i]);
    }
    screen.ctx.restore();
    // draw the city graphics to the canvas
    screen.ctx.drawImage(cities.canvas, 0, cities.y);
    // draw the tank sprite
    screen.drawSprite(tank.sprite, tank.x, tank.y);

    // draw lives remaining
    var lives = 'Lives: ' + tank.lives.toString();
    var livesMetrics = screen.ctx.measureText(lives);
    screen.ctx.fillStyle = 'white';
    screen.ctx.fillText(lives, screen.width - livesMetrics.width - 5, fontSize + 5);

    // draw the name of last alien shot
    if (alienNameAlpha > 0) {
      screen.ctx.fillStyle = 'rgba(255, 0, 0, ' + alienNameAlpha + ')';
      var alienNameMetrics = screen.ctx.measureText(alienName);
      screen.ctx.fillText(alienName, (screen.width - alienNameMetrics.width) / 2, (screen.height - fontSize) / 2);
      alienNameAlpha -= 0.02;
    }
  };
}());

/**
 * Check if to axis aligned bounding boxes intersects
 *
 * @return {bool}  the check result
 */
function AABBIntersect(ax, ay, aw, ah, bx, by, bw, bh) {
  return ax < bx+bw && bx < ax+aw && ay < by+bh && by < ay+ah;
};


/**
 * Bullet class
 *
 * @param {number} x     start x position
 * @param {number} y     start y position
 * @param {number} vely  velocity in y direction
 * @param {number} w     width of the bullet in pixels
 * @param {number} h     height of the bullet in pixels
 * @param {string} color hex-color of bullet
 */
function Bullet(x, y, vely, w, h, color) {
  this.x = x;
  this.y = y;
  this.vely = vely;
  this.width = w;
  this.height = h;
  this.color = color;
};

/**
 * Update bullet position
 */
Bullet.prototype.update = function() {
  this.y += this.vely;
};


/**
 * Abstracted canvas class usefull in games
 *
 * @param {number} width  width of canvas in pixels
 * @param {number} height height of canvas in pixels
 */
function Screen(width, height) {
  // create canvas and grab 2d context
  this.canvas = document.createElement('canvas');
  this.canvas.className = 'space-invaders';
  this.canvas.width = this.width = width;
  this.canvas.height = this.height = height;
  this.ctx = this.canvas.getContext('2d');
  // append canvas to body of document
  document.body.appendChild(this.canvas);
};

/**
 * Clear the complete canvas
 */
Screen.prototype.clear = function() {
  this.ctx.clearRect(0, 0, this.width, this.height);
};

/**
 * Draw a sprite instance to the canvas
 *
 * @param  {Sprite} sp the sprite to draw
 * @param  {number} x  x-coordinate to draw sprite
 * @param  {number} y  y-coordinate to draw sprite
 */
Screen.prototype.drawSprite = function(sp, x, y) {
  // draw part of spritesheet to canvas
  this.ctx.drawImage(sp.img, sp.x, sp.y, sp.w, sp.h, x, y, sp.w, sp.h);
};

/**
 * Draw a bullet instance to the canvas
 * @param  {Bullet} bullet the bullet to draw
 */
Screen.prototype.drawBullet = function(bullet) {
  // set the current fillstyle and draw bullet
  this.ctx.fillStyle = bullet.color;
  this.ctx.fillRect(bullet.x, bullet.y, bullet.width, bullet.height);
};


/**
 * Sprite object, uses sheet image for compressed space
 *
 * @param {Image}  img sheet image
 * @param {number} x   start x on image
 * @param {number} y   start y on image
 * @param {number} w   width of asset
 * @param {number} h   height of asset
 */
function Sprite(img, x, y, w, h) {
  this.img = img;
  this.x = x;
  this.y = y;
  this.w = w;
  this.h = h;
};


/**
 * InputHandeler class, handle and log pressed keys
 */
function InputHandeler() {
  this.down = {};
  this.pressed = {};
  // capture key presses
  var _this = this;
  document.addEventListener('keydown', function(evt) {
    _this.down[evt.keyCode] = true;
  });
  document.addEventListener('keyup', function(evt) {
    delete _this.down[evt.keyCode];
    delete _this.pressed[evt.keyCode];
  });
};

/**
 * Returns whether a key is pressod down
 * @param  {number}  code the keycode to check
 * @return {bool}         the result from check
 */
InputHandeler.prototype.isDown = function(code) {
  return this.down[code];
};

/**
 * Return wheter a key has been pressed
 * @param  {number}  code the keycode to check
 * @return {bool}         the result from check
 */
InputHandeler.prototype.isPressed = function(code) {
  // if key is registred as pressed return false else if
  // key down for first time return true else return false
  if (this.pressed[code]) {
    return false;
  } else if (this.down[code]) {
    return this.pressed[code] = true;
  }
  return false;
};
/* jshint ignore:end */