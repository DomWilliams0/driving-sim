import Box2D


class World(object):
    def __init__(self):
        self.physics = Box2D.b2World(gravity=(0, 0))

    def create_vehicle_body(self):
        body: Box2D.b2Body = self.physics.CreateDynamicBody()
        fix = body.CreatePolygonFixture(box=(3, 1.5))
        return body
