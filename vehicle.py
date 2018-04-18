import math
from enum import Enum, auto

from Box2D import Box2D
from pyglet.window.key import W, A, S, D

DIMENSIONS = (4.2, 1.8)
ACCELERATION = 10000
BRAKE = ACCELERATION * 2.5

STOPPED_EPSILON = 0.8 ** 2


class EngineState(Enum):
    ACCELERATE = auto()
    DRIFT = auto()
    BRAKE = auto()


class Car(object):
    def __init__(self, world):
        self.body: Box2D.b2Body = world.create_vehicle_body()
        self.engine_state = EngineState.DRIFT

    @property
    def pos(self):
        return self.body.position

    @pos.setter
    def pos(self, val):
        self.body.position = val

    @property
    def velocity(self) -> Box2D.b2Vec2:
        return self.body.linearVelocity

    def tick(self):
        angle = self.body.angle
        forwards = Box2D.b2Vec2(math.cos(angle), math.sin(angle))

        self._tick_engine()
        force = (0, 0)
        if self.engine_state == EngineState.ACCELERATE:
            force = forwards * ACCELERATION
        elif self.engine_state == EngineState.BRAKE:

            if self.velocity.lengthSquared > STOPPED_EPSILON:
                current_direction = self.velocity.copy()
                current_direction.Normalize()
                force = current_direction * -BRAKE
            else:
                self.body.linearVelocity = (0, 0)

        self.body.ApplyForce(force, self.body.worldCenter + (DIMENSIONS[0] / 4, 0), True)

    # TODO temporary but still should be specific to instance
    KEYS = {W, A, S, D}
    KEYS_POSITIVE = {W, A}
    KEY_STATE = {k: False for k in KEYS}

    def _tick_engine(self):
        forwards = self.KEY_STATE[W] + self.KEY_STATE[S]
        sideways = self.KEY_STATE[A] + self.KEY_STATE[D]

        if forwards < 0:
            state = EngineState.BRAKE
        elif forwards > 0:
            state = EngineState.ACCELERATE
        else:
            state = EngineState.DRIFT
        self.engine_state = state

    def handle_key(self, key, down):
        if key in self.KEYS:
            val = 1 if key in self.KEYS_POSITIVE else -1
            self.KEY_STATE[key] = val if down else 0
