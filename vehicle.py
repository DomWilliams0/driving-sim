import math
from enum import Enum, auto

from Box2D import Box2D

DIMENSIONS = (4.2, 1.8)
ACCELERATION = 800
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

        self._tick_engine_state()
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

        self.body.ApplyLinearImpulse(force, self.body.worldCenter, True)

    AWFUL_SEQ = \
        [EngineState.ACCELERATE] * 150 + \
        [EngineState.DRIFT] * 50 + \
        [EngineState.BRAKE] * 100

    AWFUL_SEQ.reverse()

    def _tick_engine_state(self):
        try:
            self.engine_state = self.AWFUL_SEQ.pop()
        except IndexError:
            self.engine_state = EngineState.DRIFT
