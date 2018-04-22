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
        val currentSpeed = let {
            var speed = Vector2(forwards).scl(forwards.dot(body.linearVelocity)).dot(forwards)
            if (speed < STOPPED_EPSILON_SQRD) {
                body.linearVelocity.setZero()
                speed = 0F
            }
            speed
        }

        val force = when (engineState) {
            EngineState.ACCELERATE -> ACCELERATION
            EngineState.REVERSE -> -REVERSE
            EngineState.BRAKE -> if (currentSpeed > 0) -BRAKE else BRAKE
            else -> 0
        }
        val scl = forwards.scl(force.toFloat())
        body.applyForceToCenter(scl, true)

        // TODO rotation
    }

}


class PlayerDrivingSystem : IteratingSystem(
        Family.all(VehicleComponent::class.java, InputComponent::class.java).get()
) {
    private val engineGetter = ComponentMapper.getFor(VehicleComponent::class.java)
    private val inputGetter = ComponentMapper.getFor(InputComponent::class.java)

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val engine = engineGetter.get(entity)
        val input = inputGetter.get(entity)


        val forwards = input.delta[InputSystem.DY]
        val brake = input.delta[InputSystem.DBRAKE]

        engine.engineState = when {
            brake == 1 -> EngineState.BRAKE
            forwards > 0 -> EngineState.ACCELERATE
            forwards < 0 -> EngineState.REVERSE
            else -> EngineState.DRIFT
        }
    }
}

class AIDrivingSystem : IteratingSystem(
        Family.all(VehicleComponent::class.java, AIInputComponent::class.java).get()
) {
    private val engineGetter = ComponentMapper.getFor(VehicleComponent::class.java)
    private val inputGetter = ComponentMapper.getFor(AIInputComponent::class.java)

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val engine = engineGetter.get(entity)
        val input = inputGetter.get(entity)

        input.dummy = (input.dummy + 1) % 50
        engine.engineState = if (input.dummy < 20) EngineState.ACCELERATE else EngineState.BRAKE
    }
}
