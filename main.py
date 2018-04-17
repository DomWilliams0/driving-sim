#!/usr/bin/env python
import Box2D
import math
import pyglet
import pyglet.graphics as g
from future.moves import itertools

import vehicle
import world

WINDOW_SIZE = (600, 600)

TPS = 50
_TIMESTEP = 1 / TPS

WORLD = world.World()
CARS: [vehicle.Car] = []

SCALE = 5


def scaler(x):
    return x * SCALE


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
        a = new_car()
        a.pos = (5, 3)
        # b = new_car()
        # b.pos = (20, 3)
        # b.body.angle = math.pi

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

        WORLD.physics.Step(_TIMESTEP, 8, 3)

    CAR_COLOUR = {
        vehicle.EngineState.ACCELERATE: (50, 220, 50),
        vehicle.EngineState.BRAKE: (220, 50, 50),
        vehicle.EngineState.DRIFT: (150, 150, 150),
    }

    def render(self):
        g.glClear(g.gl.GL_COLOR_BUFFER_BIT)

        WORLD.physics.DrawDebugData()
        for c in CARS:
            pos = c.pos
            dims = vehicle.DIMENSIONS

            colour = self.CAR_COLOUR[c.engine_state]

            g.draw(4, g.GL_QUADS,
                   ("v2f", (
                       (pos[0] - dims[0]) * SCALE, (pos[1] - dims[1]) * SCALE,
                       (pos[0] - dims[0]) * SCALE, (pos[1] + dims[1]) * SCALE,
                       (pos[0] + dims[0]) * SCALE, (pos[1] + dims[1]) * SCALE,
                       (pos[0] + dims[0]) * SCALE, (pos[1] - dims[1]) * SCALE,
                   )),
                   ("c3B", colour * 4),
                   )

        self.flip()


class PhysicsDebugRenderer(Box2D.b2Draw):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.AppendFlags(self.e_shapeBit)

    def DrawSolidCircle(self, center, radius, axis, color):
        print("DrawSolidCircle")

    def DrawSegment(self, p1, p2, color):
        print("DrawSegment")

    def DrawCircle(self, center, radius, color):
        print("DrawCircle")

    def DrawPolygon(self, vertices, vertexCount, color):
        print("DrawPolygon")

    def DrawSolidPolygon(self, vertices, colour, *args):
        g.glColor3f(*colour)
        g.draw(len(vertices), g.GL_POLYGON,
               ("v2f",
                tuple(map(scaler, itertools.chain.from_iterable(vertices))))
               )

    def DrawTransform(self, xf):
        print("DrawTransform")


if __name__ == '__main__':
    Renderer().start()
    pyglet.app.run()
