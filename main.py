#!/usr/bin/env python
import pyglet
import pyglet.graphics as g
import Box2D
from future.moves import itertools

import vehicle
import world

WINDOW_SIZE = (600, 600)

TPS = 20
_TIMESTEP = 1 / TPS

WORLD = world.World()
CARS = []


def new_car():
    car = vehicle.Car(WORLD)
    CARS.append(car)
    return car


class Renderer(pyglet.window.Window):
    def __init__(self):
        super().__init__(*WINDOW_SIZE)
        self.running = True
        self.acc = 0.0

        WORLD.physics.renderer = PhysicsDebugRenderer()
        g.glClearColor(0.05, 0.05, 0.07, 1)

    def on_key_press(self, symbol, modifiers):
        if symbol == pyglet.window.key.ESCAPE:
            pyglet.app.exit()
        else:
            print(symbol)

    def start(self):
        new_car().pos = (100, 100)

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
        for c in CARS:
            c.tick()

    def render(self):
        g.glClear(g.gl.GL_COLOR_BUFFER_BIT)
        g.glLoadIdentity()

        WORLD.physics.DrawDebugData()

        self.flip()


class PhysicsDebugRenderer(Box2D.b2Draw):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.AppendFlags(self.e_shapeBit)

    def DrawSolidCircle(self, center, radius, axis, color):
        print("DrawSolidCircle")

    def DrawSegment(self, p1, p2, color):
        g.draw(2, g.GL_LINES,
               ("v2f", (*p1, *p2)),
               ("c3f", (*color, *color))
               )

    def DrawCircle(self, center, radius, color):
        print("DrawCircle")

    def DrawPolygon(self, vertices, vertexCount, color):
        print("DrawPolygon")

    def DrawSolidPolygon(self, vertices, colour, *args):
        g.glColor3f(*colour)
        g.draw(len(vertices), g.GL_POLYGON, ("v2f", list(itertools.chain.from_iterable(vertices))))

    def DrawTransform(self, xf):
        print("DrawTransform")


if __name__ == '__main__':
    Renderer().start()
    pyglet.app.run()
