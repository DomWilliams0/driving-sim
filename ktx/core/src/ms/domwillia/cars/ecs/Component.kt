package ms.domwillia.cars.ecs

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.Body

data class DummyRenderComponent(val colour: Color) : Component

data class PhysicsComponent(val body: Body) : Component
