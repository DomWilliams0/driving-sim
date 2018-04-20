import Box2D
import networkx

import vehicle


class World(object):
    def __init__(self):
        self.physics = Box2D.b2World(gravity=(0, 0))
        self.roads = networkx.Graph()
        self.load_roads()

    def load_roads(self):
        a = 10
        b = 50
        c=80
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
