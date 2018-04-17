import Box2D

import vehicle


class World(object):
    def __init__(self):
        self.physics = Box2D.b2World(gravity=(0, 0))

    def create_vehicle_body(self):
        body: Box2D.b2Body = self.physics.CreateDynamicBody()
        # body.fixedRotation = True
        fix: Box2D.b2Fixture = body.CreatePolygonFixture(box=vehicle.DIMENSIONS, density=163, friction=0.5)
        return body
