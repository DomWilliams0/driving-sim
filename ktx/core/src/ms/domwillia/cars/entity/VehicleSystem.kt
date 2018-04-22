package ms.domwillia.cars.entity

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import kotlin.math.absoluteValue

class VehicleSystem : IteratingSystem(
        Family.all(VehicleComponent::class.java, PhysicsComponent::class.java).get()
) {
    companion object {
        val ACCELERATION = 1400
        val REVERSE = 1000
        val BRAKE = 6000
        val STOPPED_EPSILON = 1.5
    }


    private val engineGetter = ComponentMapper.getFor(VehicleComponent::class.java)
    private val physicsGetter = ComponentMapper.getFor(PhysicsComponent::class.java)


    override fun processEntity(entity: Entity, deltaTime: Float) {
        val body = physicsGetter.get(entity).body
        val engineState = engineGetter.get(entity).engineState

        // TODO kill lateral motion

        // forwards motion
        val forwards = body.getWorldVector(Vector2.Y)
        var stop = false
        val currentSpeed = Vector2(forwards).scl(forwards.dot(body.linearVelocity)).dot(forwards)
        if (currentSpeed.absoluteValue < STOPPED_EPSILON)
            stop = true

        var force = 0
        when {
            engineState == EngineState.ACCELERATE -> force = ACCELERATION
            engineState == EngineState.REVERSE -> force = -REVERSE
            stop -> {
                body.setLinearVelocity(0F, 0F)
            }
            engineState == EngineState.BRAKE -> when {
                currentSpeed > 0 -> force = -BRAKE
                currentSpeed < 0 -> force = BRAKE
            }
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
