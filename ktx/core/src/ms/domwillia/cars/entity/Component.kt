package ms.domwillia.cars.entity

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.World
import ktx.box2d.body

const val VEHICLE_WIDTH = 1.8F
const val VEHICLE_HEIGHT = 4.2F
val VEHICLE_DIMENSIONS = Vector2(VEHICLE_WIDTH, VEHICLE_HEIGHT)
val VEHICLE_HALF_DIMENSIONS = Vector2(VEHICLE_WIDTH / 2F, VEHICLE_HEIGHT / 2F)

data class RenderComponent(val colour: Color, val dimensions: Vector2) : Component

data class EngineComponent(val dummy: Int) : Component

data class PhysicsComponent(val body: Body) : Component

fun createVehicleEntity(physics: World): Entity {
    fun physics(): Body =
            physics.body(BodyDef.BodyType.DynamicBody) {
                linearDamping = 0.1F

                let {
                    val chassisShape = PolygonShape().apply {
                        setAsBox(VEHICLE_HALF_DIMENSIONS.x, VEHICLE_HALF_DIMENSIONS.y)
                    }
                    val fix = fixture(chassisShape)
                    fix.density = 16F
                    fix.friction = 0.5F
                }
                // TODO detector and sight
            }

    fun render(): Component {
        val colour = Color.FIREBRICK // TODO random colour?
        val dims = VEHICLE_DIMENSIONS
        return RenderComponent(colour, dims)
    }


    val e = Entity()

    val body = physics()

    e.add(PhysicsComponent(body))
    e.add(render())
    e.add(EngineComponent(0))

    return e
}
