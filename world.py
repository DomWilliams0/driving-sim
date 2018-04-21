import math

import Box2D
import networkx
import pytmx.pytmx as pytmx
from future.moves import itertools

import vehicle
# single lane
from collision import LaneUserData, CarLaneDetectorUserData, CarSightUserData, RoadJoinUserData, CollisionDetector

LANE_WIDTH = (vehicle.DIMENSIONS[1] * 2) * 2


def fatten_line(edge):
    def gen():
        """ty https://stackoverflow.com/a/1937202"""
        (x0, y0), (x1, y1), data = edge
        total_lanes = data["lanes"]

        dx = x1 - x0
        dy = y1 - y0
        length = math.sqrt(dx * dx + dy * dy)
        dx /= length
        dy /= length

        px = 0.5 * LANE_WIDTH * -dy
        py = 0.5 * LANE_WIDTH * dx

        x0 += px * total_lanes / 2
        x1 += px * total_lanes / 2
        y0 += py * total_lanes / 2
        y1 += py * total_lanes / 2

        for l in range(total_lanes):
            yield ((x0 + px - py, y0 + py + px),
                   (x1 + px + py, y1 + py - px),
                   (x1 - px + py, y1 - (py + px)),
                   (x0 - (px + py), y0 - (py - px)))
            x0 -= px * 2
            x1 -= px * 2
            y0 -= py * 2
            y1 -= py * 2

    return list(gen())


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

        def calc_normal(edge):
            (x1, y1), (x2, y2) = edge
            dx, dy = x2 - x1, y2 - y1
            normal = Box2D.b2Vec2(-dy, dx)
            normal.Normalize()
            return normal

        map = pytmx.TiledMap(roads_file)
        uid = 1
        for layer in (l for l in map if isinstance(l, pytmx.TiledObjectGroup)):
            for obj in layer:
                for edge in edge_pairs(obj.points, obj.closed):
                    # TODO allow lanes going other way too
                    lanes = 2
                    self.roads.add_nodes_from(edge, id=uid)
                    self.roads.add_edge(*edge, id=uid, lanes=lanes, normal=calc_normal(edge))
                    uid += 1

        padding = 1
        min_x = min(x[0] for x in self.roads) - padding
        min_y = min(x[1] for x in self.roads) - padding
        max_y = max(x[1] for x in self.roads)
        flip = max_y - min_y + padding

        networkx.relabel_nodes(self.roads, lambda n: (n[0] - min_x, flip - (n[1] - min_y)), copy=False)

        self._add_lane_collision_boxes()

    def _add_lane_collision_boxes(self):
        rects = list(map(lambda e: (fatten_line(e), e[2]), self.roads.edges(data=True)))
        road_frame: Box2D.b2Body = self.physics.CreateStaticBody()
        shape = Box2D.b2PolygonShape()
        fix_def = Box2D.b2FixtureDef(shape=shape, isSensor=True)

        for lanes, data in rects:
            # lane_count, normal = data["lanes"], data["normal"]
            for (i, l) in enumerate(lanes):
                shape.vertices = l
                fix: Box2D.b2Fixture = road_frame.CreateFixture(fix_def)
                fix.userData = LaneUserData(data["id"], i)

        # to be detected by the cars sensor
        nodes = networkx.get_node_attributes(self.roads, "id")
        for (node, id) in nodes.items():
            road_frame.CreateCircleFixture(pos=node, radius=1, isSensor=True, userData=RoadJoinUserData(id))

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
        detector.userData = CarLaneDetectorUserData(car)

        sight_len = 40
        width_mod = 2
        sight = body.CreatePolygonFixture(vertices=[
            (0, vehicle.DIMENSIONS[1] * width_mod),
            (sight_len, vehicle.DIMENSIONS[1] * width_mod),
            (sight_len, -vehicle.DIMENSIONS[1] * width_mod),
            (0, -vehicle.DIMENSIONS[1] * width_mod)])
        sight.userData = CarSightUserData(car)
        return body, sight
