import math
from enum import Enum, auto

from Box2D import Box2D

DIMENSIONS = (4.2, 1.8)
ACCELERATION = 500
BRAKE = ACCELERATION * 2


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
        direction = Box2D.b2Vec2(math.cos(angle), math.sin(angle))

        self._tick_engine_state()
        force = (0, 0)
        if self.engine_state == EngineState.ACCELERATE:
            force = direction * ACCELERATION
        elif self.engine_state == EngineState.BRAKE:
            force = direction * -BRAKE

        self.body.ApplyLinearImpulse(force, self.body.worldCenter, True)

        print(self.velocity, self.body.mass, force)

    BRAKE_TICK = 0

    def _tick_engine_state(self):
        if self.BRAKE_TICK == 0:
            self.BRAKE_TICK = 100
        if self.BRAKE_TICK < 30:
            ret = EngineState.BRAKE
        else:
            ret = EngineState.ACCELERATE

        self.BRAKE_TICK -= 1
        self.engine_state = ret
