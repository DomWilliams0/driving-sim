package ms.domwillia.cars.entity

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2

const val ACCELERATION = 5_000
const val REVERSE = 7_000
const val BRAKE = 15_000
const val STOPPED_EPSILON_SQRD = 1

class VehicleSystem : IteratingSystem(
        Family.all(VehicleComponent::class.java, PhysicsComponent::class.java).get()
) {
    private val engineGetter = ComponentMapper.getFor(VehicleComponent::class.java)
    private val physicsGetter = ComponentMapper.getFor(PhysicsComponent::class.java)


    override fun processEntity(entity: Entity, deltaTime: Float) {
        val body = physicsGetter.get(entity).body
        val engineState = engineGetter.get(entity).engineState

        // TODO kill lateral motion

        // forwards motion
        val forwards = body.getWorldVector(Vector2.Y)
        val currentSpeed = Vector2(forwards).scl(forwards.dot(body.linearVelocity)).dot(forwards)

        if (currentSpeed < STOPPED_EPSILON_SQRD)
            body.linearVelocity.setZero()

        val force = when (engineState) {
            EngineState.ACCELERATE -> ACCELERATION
            EngineState.REVERSE -> REVERSE
            EngineState.BRAKE -> if (currentSpeed > 0) -BRAKE else BRAKE
            else -> 0
        }
        val scl = forwards.scl(force.toFloat())
        println(currentSpeed)
        body.applyForceToCenter(scl, true)

        // TODO rotation

    }
}
