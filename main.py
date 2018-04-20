#!/usr/bin/env python
from itertools import chain

import Box2D
import pyglet
import pyglet.graphics as g

import vehicle
import world

WINDOW_SIZE = (600, 600)

TPS = 50
_TIMESTEP = 1 / TPS

WORLD = world.World("roads.tmx")
CARS: [vehicle.Car] = []

SCALE = 10


def new_car():
    car = vehicle.Car(WORLD)
    CARS.append(car)
    return car


def create_world_batch():
    batch = g.Batch()
    graph = WORLD.roads

    edges = list(chain.from_iterable(chain.from_iterable(graph.edges)))
    count = len(edges) // 2
    batch.add(
        count, g.GL_LINES, None,
        ("v2f", edges)
    )

    return batch


def create_car_batch(dims):
    batch = g.Batch()
    group_car = g.Group()
    group_lights = g.Group(group_car)
    light_colour = (252, 142, 41)

    light_dims = (0.7, 1)

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

    light_bases = [
        (dims[0] * 2 - light_dims[0], dims[1] * 2 - light_dims[1] * 0.8),
        (dims[0] * 2 - light_dims[0], -light_dims[1] * 0.2),
    ]

    lights = list(chain.from_iterable(
        (
            base[0], base[1],
            base[0] + light_dims[0], base[1],
            base[0] + light_dims[0], base[1] + light_dims[1],
            base[0], base[1] + light_dims[1]
        )
        for base in light_bases))

    count = len(lights) // 2
    batch.add(
        count, g.GL_QUADS, group_lights,
        ("v2f", lights),
        ("c3B", (light_colour * count))
    )

    return batch, body


class Renderer(pyglet.window.Window):
    def __init__(self):
        super().__init__(*WINDOW_SIZE)
        self.running = True
        self.acc = 0.0
        self.car_batch = create_car_batch(vehicle.DIMENSIONS)
        self.world_batch = create_world_batch()

        WORLD.physics.renderer = PhysicsDebugRenderer()
        g.glClearColor(0.05, 0.05, 0.07, 1)

    @staticmethod
    def on_key_press(symbol, modifiers):
        global SCALE
        if symbol == pyglet.window.key.ESCAPE:
            pyglet.app.exit()
        elif symbol == pyglet.window.key.PLUS:
            SCALE += 1
        elif symbol == pyglet.window.key.MINUS:
            SCALE -= 1
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
        vehicle.EngineState.REVERSE: (50, 100, 255),
        vehicle.EngineState.BRAKE: (220, 50, 50),
        vehicle.EngineState.DRIFT: (150, 150, 150),
    }

    def render(self):
        g.glClear(g.gl.GL_COLOR_BUFFER_BIT)
        label = pyglet.text.Label("", font_size=8, color=(0, 0, 0, 255), anchor_x="center", anchor_y="center")

        g.glPushMatrix()
        g.glScalef(SCALE, SCALE, 0)
        g.glLineWidth(vehicle.DIMENSIONS[1] * SCALE * 8)
        g.glColor3f(200, 20, 30)
        self.world_batch.draw()
        g.glLineWidth(1)
        g.glColor3f(0, 0, 0)
        self.world_batch.draw()
        g.glPopMatrix()

        # WORLD.physics.DrawDebugData()
        for c in CARS:
            pos = c.pos
            dims = vehicle.DIMENSIONS

            colour = self.CAR_COLOUR[c.engine_state]

            g.glPushMatrix()
            g.glScalef(SCALE, SCALE, 0)
            g.glTranslatef(pos[0], pos[1], 0)
            g.glRotatef(c.angle, 0, 0, 1)
            g.glTranslatef(-dims[0], -dims[1], 0)

            (batch, body) = self.car_batch
            body.colors = colour * 4
            batch.draw()
            g.glPopMatrix()

            g.glPushMatrix()
            g.glTranslatef(pos[0] * SCALE, pos[1] * SCALE, 0)

            label.text = str(int(c.speed))
            label.draw()
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
                tuple(map(lambda x: x * SCALE, chain.from_iterable(vertices))))
               )

    def DrawTransform(self, xf):
        print("DrawTransform")


if __name__ == '__main__':
    Renderer().start()
    pyglet.app.run()
