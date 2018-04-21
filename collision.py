from enum import IntEnum

import Box2D
from dataclasses import dataclass

import vehicle


class UserDataType(IntEnum):
    LANE = 1 << 0
    CAR_LANE_DETECTOR = 1 << 1
    CAR_SIGHT = 1 << 2
    ROAD_JOIN = 1 << 3


@dataclass
class LaneUserData(object):
    road_id: int
    lane: int

    def __post_init__(self): self.tag = UserDataType.LANE


@dataclass
class CarLaneDetectorUserData(object):
    car: vehicle.Car

    def __post_init__(self): self.tag = UserDataType.CAR_LANE_DETECTOR


@dataclass
class CarSightUserData(object):
    car: vehicle.Car

    def __post_init__(self): self.tag = UserDataType.CAR_SIGHT


@dataclass
class RoadJoinUserData(object):
    road_id: int

    # TODO

    def __post_init__(self): self.tag = UserDataType.ROAD_JOIN


def lane_and_car(begin, lane, car):
    t = car.car.road_tracker
    if begin:
        t.entered(lane)
    else:
        t.exited(lane)


def car_sight_and_join(begin, car, road_join):
    if begin:
        print("car saw {}".format(road_join.road_id))


HANDLERS = {
    UserDataType.LANE | UserDataType.CAR_LANE_DETECTOR: lane_and_car,
    UserDataType.CAR_SIGHT | UserDataType.ROAD_JOIN: car_sight_and_join,
}


class CollisionDetector(Box2D.b2ContactListener):

    @staticmethod
    def handle(contact, begin):
        a = contact.fixtureA.userData
        b = contact.fixtureB.userData
        if not a or not b:
            return

        handler = HANDLERS.get(a.tag | b.tag, None)
        if not handler:
            return

        if a.tag < b.tag:
            x, y = a, b
        else:
            x, y = b, a

        handler(begin, x, y)

    def BeginContact(self, contact: Box2D.b2Contact):
        self.handle(contact, True)

    def EndContact(self, contact):
        self.handle(contact, False)
