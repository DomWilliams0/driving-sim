import math

import Box2D
import networkx
import pytmx.pytmx as pytmx
from future.moves import itertools

import vehicle

LANE_WIDTH = (vehicle.DIMENSIONS[1] * 2) * 5


def fatten_line(edge):
    """https://stackoverflow.com/a/1937202"""
    ((x0, y0), (x1, y1)) = edge
    dx = x1 - x0
    dy = y1 - y0
    length = math.sqrt(dx * dx + dy * dy)
    dx /= length
    dy /= length
    px = 0.5 * LANE_WIDTH * (-dy)
    py = 0.5 * LANE_WIDTH * dx
    return ((x0 + px - py, y0 + py + px),
            (x1 + px + py, y1 + py - px),
            (x1 - px + py, y1 - (py + px)),
            (x0 - (px + py), y0 - (py - px)))


class CollisionDetector(Box2D.b2ContactListener):

    @staticmethod
    def _car_and_road(contact):
        """Returns (car, road) or None"""
        a = contact.fixtureA.userData
        b = contact.fixtureB.userData

        a_car = isinstance(a, vehicle.Car)
        b_car = isinstance(b, vehicle.Car)
        if (a_car ^ b_car) and (a_car or b_car):
            if a_car:
                return a, b
            else:
                return b, a

    def BeginContact(self, contact: Box2D.b2Contact):
        cr = CollisionDetector._car_and_road(contact)
        if cr:
            cr[0].road_tracker.entered(cr[1])

    def EndContact(self, contact):
        cr = CollisionDetector._car_and_road(contact)
        if cr:
            cr[0].road_tracker.exited(cr[1])


class RoadData(object):
    def __init__(self, id):
        self.id = id


class World(object):
    def __init__(self, roads_file):
        self.physics = Box2D.b2World(gravity=(0, 0), contactListener=CollisionDetector())
        self.roads = networkx.Graph()
        self.load_roads(roads_file)

    def load_roads(self, roads_file):
        def edge_pairs(points, closed):
            a, b = itertools.tee(points)
            next(b, None)
            yield from zip(a, b)
            if closed and len(points) > 2:
                yield (points[-1], points[0])

        map = pytmx.TiledMap(roads_file)
        uid = 1
        for layer in (l for l in map if isinstance(l, pytmx.TiledObjectGroup)):
            for obj in layer:
                for edge in edge_pairs(obj.points, obj.closed):
                    self.roads.add_edge(*edge, id=uid)
                    uid += 1

        padding = 1
        min_x = min(x[0] for x in self.roads) - padding
        min_y = min(x[1] for x in self.roads) - padding
        max_y = max(x[1] for x in self.roads)
        flip = max_y - min_y + padding

        networkx.relabel_nodes(self.roads, lambda n: (n[0] - min_x, flip - (n[1] - min_y)), copy=False)

        self._add_lane_collision_boxes()

    def _add_lane_collision_boxes(self):
        rects = list(map(lambda e: (fatten_line(e[:2]), e[2]["id"]), self.roads.edges(data=True)))
        road_frame: Box2D.b2Body = self.physics.CreateStaticBody()
        shape = Box2D.b2PolygonShape()
        fix_def = Box2D.b2FixtureDef(shape=shape, isSensor=True)

        for r, id in rects:
            shape.vertices = r
            fix: Box2D.b2Fixture = road_frame.CreateFixture(fix_def)
            fix.userData = id

    def _gen_roads(self):
        a = 10
        b = 50
        c = 80
        edges = [
            ((a, a), (a, b)),
            ((a, b), (c, c)),
            ((c, c), (b, a)),
            ((b, a), (a, a)),
        ]
        self.roads.add_edges_from(edges)

    def create_vehicle_body(self, car):
        body: Box2D.b2Body = self.physics.CreateDynamicBody()
        body.linearDamping = 0.1
        body.CreatePolygonFixture(box=vehicle.DIMENSIONS, density=16, friction=0.5)
        detector: Box2D.b2Fixture = body.CreateCircleFixture(radius=1, isSensor=True)
        detector.userData = car
        return body
