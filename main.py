#!/usr/bin/env python

import pyglet
import pyglet.graphics as g

WINDOW_SIZE = (600, 600)

TPS = 20
_TIMESTEP = 1 / TPS


class Renderer(pyglet.window.Window):
    def __init__(self):
        super().__init__(*WINDOW_SIZE)
        self.running = True
        self.acc = 0.0

        self.test = pyglet.text.Label("hullo?",
                                      x=WINDOW_SIZE[0] // 2, y=WINDOW_SIZE[1] // 2,
                                      anchor_x="center", anchor_y="center")

        g.glClearColor(0.05, 0.05, 0.07, 1)

    def on_key_press(self, symbol, modifiers):
        if symbol == pyglet.window.key.ESCAPE:
            pyglet.app.exit()
        else:
            print(symbol)

    def start(self):
        pyglet.clock.schedule(self._loop)

    def _loop(self, dt):
        if dt > 0.25:
            dt = 0.25

        self.acc += dt

        while self.acc >= _TIMESTEP:
            self.acc -= _TIMESTEP
            self.tick()

        self.render()

    def tick(self):
        self.test.x += 1

    def render(self):
        g.glClear(g.gl.GL_COLOR_BUFFER_BIT)
        g.glLoadIdentity()

        self.test.draw()

        self.flip()


if __name__ == '__main__':
    Renderer().start()
    pyglet.app.run()
