import Box2D
import networkx
import pytmx.pytmx as pytmx
from future.moves import itertools

import vehicle


class World(object):
    def __init__(self, roads_file):
        self.physics = Box2D.b2World(gravity=(0, 0))
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
        for layer in (l for l in map if isinstance(l, pytmx.TiledObjectGroup)):
            for obj in layer:
                for edge in edge_pairs(obj.points, obj.closed):
                    self.roads.add_edge(*edge)

        padding = 1
        min_x = min(x[0] for x in self.roads) - padding
        min_y = min(x[1] for x in self.roads) - padding
        max_y = max(x[1] for x in self.roads)
        flip = max_y - min_y + padding

        networkx.relabel_nodes(self.roads, lambda n: (n[0] - min_x, flip - (n[1] - min_y)), copy=False)

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

    def create_vehicle_body(self):
        body: Box2D.b2Body = self.physics.CreateDynamicBody()
        body.linearDamping = 0.1
        # body.fixedRotation = True
        fix: Box2D.b2Fixture = body.CreatePolygonFixture(box=vehicle.DIMENSIONS, density=16, friction=0.5)
        return body
