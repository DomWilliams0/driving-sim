package ms.domwillia.cars.entity

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector2
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.ln

class VehicleSystem : IteratingSystem(
        Family.all(VehicleComponent::class.java, PhysicsComponent::class.java).get()
) {
    companion object {
        val ACCELERATION = 1400
        val REVERSE = 1000
        val BRAKE = 6000
        val STOPPED_EPSILON = 1.5
    }


    override fun processEntity(entity: Entity, deltaTime: Float) {

        val physics = physicsGetter.get(entity)
        val body = physics.body
        val vehicle = vehicleGetter.get(entity)
        val engineState = vehicle.engineState

        // update render colour
        renderGetter.get(entity)?.let {
            it.colour = when (engineState) {
                EngineState.DRIFT -> Color.GRAY
                EngineState.ACCELERATE -> Color.GREEN
                EngineState.BRAKE -> Color.RED
                EngineState.REVERSE -> Color.WHITE
            }
        }

        // kill lateral motion
        val sideways = body.getWorldVector(Vector2.X)
        val lateral = Vector2(sideways).scl(sideways.dot(body.linearVelocity)).scl(-body.mass)
        body.applyLinearImpulse(lateral, body.worldCenter, true)

        // forwards motion
        val forwards = body.getWorldVector(Vector2.Y)
        var stop = false
        val currentSpeed = Vector2(forwards).scl(forwards.dot(body.linearVelocity)).dot(forwards)
        val currentAbs = currentSpeed.absoluteValue
        if (currentAbs < STOPPED_EPSILON)
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

        // rotation
        val angular = if (vehicle.wheelsForce == 0)
            0F
        else {
            // rises quickly and plateaus until ~34, then gradually reduces
            var mult: Float = if (currentAbs < 34.135) // where these 2 lines intersect
                ln(currentAbs + 1)
            else
                (1F / (0.01F * currentAbs) + 0.3F) + 2

            mult = Math.copySign(mult, currentSpeed)
            vehicle.wheelsForce * mult * -0.5F
        }
        body.angularVelocity = angular

        physics.speed = currentSpeed
        if (angular != 0F)
            physics.turningRate = -angular

        // calculate lane
        vehicle.currentRoad?.let { road ->
            val pos = body.position
            val rotateMatrix = Matrix3()

            rotateMatrix
                    .rotate(90 - road.direction.angle())
                    .translate(-road.centre.x, -road.centre.y)

            val rotatedPos = pos.cpy().mul(rotateMatrix).apply {
                x = (x + road.width / 2F) / road.width
                y = (y + road.length / 2F) / road.length
            }
            // x is distance across the lanes
            // y is distance along the segment

            val lane = MathUtils.clamp(floor(rotatedPos.x * road.lanes).toInt(), 0, road.lanes - 1)
            vehicle.currentLane = lane
        }
    }

}


class PlayerDrivingSystem : IteratingSystem(
        Family.all(VehicleComponent::class.java, InputComponent::class.java).get()
) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val vehicle = vehicleGetter.get(entity)
        val input = inputGetter.get(entity)

        val forwards = input.delta[InputSystem.DY]
        val brake = input.delta[InputSystem.DBRAKE]

        vehicle.engineState = when {
            brake == 1 -> EngineState.BRAKE
            forwards > 0 -> EngineState.ACCELERATE
            forwards < 0 -> EngineState.REVERSE
            else -> EngineState.DRIFT
        }

        val sideways = input.delta[InputSystem.DX]
        vehicle.wheelsForce = sideways
    }
}

class AIDrivingSystem : IteratingSystem(
        Family.all(VehicleComponent::class.java, AIInputComponent::class.java).get()
) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val vehicle = vehicleGetter.get(entity)
        val ai = aiGetter.get(entity)

        val road = vehicle.currentRoad ?: return

        // TODO change to this direction
        ai.currentRoadDirection = road.direction
    }
}
