#!/usr/bin/env python
import Box2D
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

SCALE = 10


def new_car():
    car = vehicle.Car(WORLD)
    CARS.append(car)
    return car


def create_car_batch(dims):
    batch = g.Batch()
    group_wheels = g.Group()
    group_car = g.Group(group_wheels)
    wheel_colour = (200, 200, 200)

    wheel_width = 1.2
    wheel_height = 1

    body: g.vertexdomain.VertexList = batch.add(
        4, g.GL_QUADS, group_car,
        ("v2f", (
            0, 0,
            dims[0] * 2, 0,
            dims[0] * 2, dims[1] * 2,
            0, dims[1] * 2,
        )),
        ("c3B", (255,) * 4 * 3)
    )

    wheel_bases = [
        (wheel_width / 2, -wheel_height / 2),
        (wheel_width / 2, -wheel_height / 2 + dims[1] * 2),
        (dims[0] * 2 - wheel_width * 1.75, -wheel_height / 2 + dims[1] * 2),
        (dims[0] * 2 - wheel_width * 1.75, -wheel_height / 2),
    ]

    wheels = list(itertools.chain.from_iterable(
        (
            base[0], base[1],
            base[0] + wheel_width, base[1],
            base[0] + wheel_width, base[1] + wheel_height,
            base[0], base[1] + wheel_height
        )
        for base in wheel_bases))

    count = len(wheels) // 2
    batch.add(
        count, g.GL_QUADS, group_wheels,
        ("v2f", wheels),
        ("c3B", (wheel_colour * count))
    )

    return batch, body


class Renderer(pyglet.window.Window):
    def __init__(self):
        super().__init__(*WINDOW_SIZE)
        self.running = True
        self.acc = 0.0
        self.car_batch = create_car_batch(vehicle.DIMENSIONS)

        WORLD.physics.renderer = PhysicsDebugRenderer()
        g.glClearColor(0.05, 0.05, 0.07, 1)

    @staticmethod
    def on_key_press(symbol, modifiers):
        if symbol == pyglet.window.key.ESCAPE:
            pyglet.app.exit()
        else:
            CARS[0].handle_key(symbol, True)

    @staticmethod
    def on_key_release(symbol, modifiers):
        CARS[0].handle_key(symbol, False)

    def start(self):
        a = new_car()
        a.pos = (5, 3)
        # b = new_car()
        # b.pos = (20, 3)
        # from math import pi
        # b.body.angle = pi

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
        label = pyglet.text.Label("", font_size=8, color=(0, 0, 0, 255), anchor_x="center", anchor_y="center")

        WORLD.physics.DrawDebugData()
        for c in CARS:
            g.glPushMatrix()
            pos = c.pos
            dims = vehicle.DIMENSIONS

            colour = self.CAR_COLOUR[c.engine_state]

            g.glScalef(SCALE, SCALE, 0)
            g.glTranslatef(pos[0], pos[1], 0)
            g.glRotatef(c.angle, 0, 0, 1)
            g.glTranslatef(-dims[0], -dims[1], 0)

            (batch, body) = self.car_batch
            body.colors = colour * 4
            batch.draw()

            # label.text = str(int(c.speed))
            # label.draw()

            g.glPopMatrix()

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
                tuple(map(lambda x: x * SCALE, itertools.chain.from_iterable(vertices))))
               )

    def DrawTransform(self, xf):
        print("DrawTransform")


if __name__ == '__main__':
    Renderer().start()
    pyglet.app.run()
