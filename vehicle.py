from Box2D import Box2D


class Car(object):
    def __init__(self, world):
        self.body: Box2D.b2Body = world.create_vehicle_body()
        self.color = Box2D.b2Color(0.8, 0.5, 0.2)

    @property
    def pos(self):
        return self.body.position

    @pos.setter
    def pos(self, val):
        self.body.position = val

    @property
    def velocity(self):
        return self.body.linearVelocity

    def tick(self):
        pass
