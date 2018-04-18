import math
from enum import Enum, auto

from Box2D import Box2D
from pyglet.window.key import W, A, S, D, SPACE

DIMENSIONS = (4.2, 1.8)
ACCELERATION = 10000
REVERSE = 7000
BRAKE = 20000

STOPPED_EPSILON = 0.8 ** 2

KEYS = {W, A, S, D, SPACE}
KEYS_POSITIVE = {W, A}


class EngineState(Enum):
    ACCELERATE = auto()
    DRIFT = auto()
    BRAKE = auto()
    REVERSE = auto()


class Car(object):
    def __init__(self, world):
        self.body: Box2D.b2Body = world.create_vehicle_body()
        self.engine_state = EngineState.DRIFT
        self.wheel_force = 0
        self.key_state = {k: False for k in KEYS}

        self._speed = 0.0

    @property
    def speed(self):
        return self._speed

    @property
    def angle(self):
        return math.degrees(self.body.angle)

    @property
    def pos(self):
        return self.body.position

    @pos.setter
    def pos(self, val):
        self.body.position = val

    @property
    def velocity(self) -> Box2D.b2Vec2:
        return self.body.linearVelocity

    def _get_vec(self, local_vec):
        normal = self.body.GetWorldVector(local_vec)
        return Box2D.b2Dot(normal, self.velocity) * normal

    def tick(self):
        # kill lateral
        lateral = self._get_vec((0, 1))
        lateral_force = self.body.mass * -lateral
        self.body.ApplyLinearImpulse(lateral_force, self.body.worldCenter, True)

        self._tick_engine()

        current_forward_normal = self.body.GetWorldVector((1, 0))
        current_speed = Box2D.b2Dot(self._get_vec((1, 0)), current_forward_normal)
        self._speed = current_speed

        force = 0
        if self.engine_state == EngineState.ACCELERATE:
            force = ACCELERATION
        if self.engine_state == EngineState.REVERSE:
            force = -ACCELERATION
        elif self.engine_state == EngineState.BRAKE:
            if self.velocity.lengthSquared > STOPPED_EPSILON:
                force = -BRAKE if current_speed > 0 else BRAKE
            else:
                self.body.linearVelocity = (0, 0)  # stop

        self.body.ApplyForce(force * current_forward_normal, self.body.worldCenter, True)

        # rotation
        if self.wheel_force == 0:
            self.body.angularVelocity = 0
        else:
            # speed is scaled up to this speed when its max    vv
            self.body.angularVelocity = self.wheel_force * min(30, current_speed) * 0.06

    def _tick_engine(self):
        forwards = self.key_state[W] + self.key_state[S]
        brake = self.key_state[SPACE]
        if brake:
            state = EngineState.BRAKE
        elif forwards > 0:
            state = EngineState.ACCELERATE
        elif forwards < 0:
            state = EngineState.REVERSE
        else:
            state = EngineState.DRIFT
        self.engine_state = state

        # wheel
        sideways = self.key_state[A] + self.key_state[D]
        self.wheel_force = sideways

    def handle_key(self, key, down):
        if key in KEYS:
            val = 1 if key in KEYS_POSITIVE else -1
            self.key_state[key] = val if down else 0
