import math
from enum import Enum, auto

from Box2D import Box2D
from pyglet.window.key import W, A, S, D

DIMENSIONS = (4.2, 1.8)
ACCELERATION = 10000
BRAKE = 20000
WHEEL_TURN = 5
MAX_WHEEL_ANGLE = 60

STOPPED_EPSILON = 0.8 ** 2

KEYS = {W, A, S, D}
KEYS_POSITIVE = {W, A}


class EngineState(Enum):
    ACCELERATE = auto()
    DRIFT = auto()
    BRAKE = auto()


class Car(object):
    def __init__(self, world):
        self.body: Box2D.b2Body = world.create_vehicle_body()
        self.engine_state = EngineState.DRIFT
        self.wheel_angle = 0
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
        wheel_angle = math.radians(self.wheel_angle + self.body.angle)
        self.body.angle = wheel_angle

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
        elif self.engine_state == EngineState.BRAKE:
            if self.velocity.lengthSquared > STOPPED_EPSILON:
                force = -BRAKE
            else:
                self.body.linearVelocity = (0, 0)  # stop

        self.body.ApplyForce(force * current_forward_normal, self.body.worldCenter, True)

    """ # forwards
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

            print(force, lateral_force)
        # self.body.ApplyForce(force, self.body.worldCenter, True)"""

    def _tick_engine(self):
        forwards = self.key_state[W] + self.key_state[S]
        if forwards < 0:
            state = EngineState.BRAKE
        elif forwards > 0:
            state = EngineState.ACCELERATE
        else:
            state = EngineState.DRIFT
        self.engine_state = state

        # wheel
        sideways = self.key_state[A] + self.key_state[D]
        if sideways == 0:
            if self.wheel_angle != 0:
                self.wheel_angle -= int(math.copysign(WHEEL_TURN, self.wheel_angle))
                if abs(self.wheel_angle) < WHEEL_TURN:
                    self.wheel_angle = 0
        else:
            angle = self.wheel_angle + (sideways * WHEEL_TURN)
            if angle < 0 and angle < -MAX_WHEEL_ANGLE:
                angle = -MAX_WHEEL_ANGLE
            elif angle > 0 and angle > MAX_WHEEL_ANGLE:
                angle = MAX_WHEEL_ANGLE

            self.wheel_angle = angle

    def handle_key(self, key, down):
        if key in KEYS:
            val = 1 if key in KEYS_POSITIVE else -1
            self.key_state[key] = val if down else 0
