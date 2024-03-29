package ms.domwillia.cars.entity

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import ktx.box2d.body
import ktx.box2d.filter
import ms.domwillia.cars.world.*

const val VEHICLE_WIDTH = 1.8F
const val VEHICLE_HEIGHT = 4.2F
val VEHICLE_DIMENSIONS = Vector2(VEHICLE_WIDTH, VEHICLE_HEIGHT)
val VEHICLE_HALF_DIMENSIONS = Vector2(VEHICLE_WIDTH / 2F, VEHICLE_HEIGHT / 2F)


enum class EngineState {
    DRIFT,
    ACCELERATE,
    BRAKE,
    REVERSE
}


data class RenderComponent(var colour: Color, val dimensions: Vector2) : Component

data class VehicleComponent(var engineState: EngineState = EngineState.DRIFT, var wheelsForce: Int = 0) : Component {
    val currentRoadStack = linkedSetOf<RoadEdge>()
    var currentIntersection: RoadNode? = null

    val currentRoad: RoadEdge?
        get() = currentRoadStack.lastOrNull()

    var currentLane: Int = 0

    fun getCurrentState() = when {
        currentIntersection != null -> "Intersection $currentIntersection"
        currentRoad != null -> "Road ${currentRoad!!.id} lane $currentLane"
        else -> "No current road"
    }
}

data class AIInputComponent(var currentRoadDirection: Vector2? = null) : Component

data class InputComponent(val delta: IntArray = IntArray(InputSystem.DELTA_COUNT)) : Component

data class PhysicsComponent(
        val body: Body,

        var turningRate: Float = 0F,
        var speed: Float = 0F
) : Component

fun createVehicleEntity(physics: World, pos: Vector2, driver: Component? = null, angleRad: Float = 0F): Entity {
    fun physics(e: Entity, veh: VehicleComponent): Component =
            PhysicsComponent(physics.body(BodyDef.BodyType.DynamicBody) {
                linearDamping = 0.1F
                position.set(pos)
                this.angle = angleRad

                // chassis
                box(VEHICLE_DIMENSIONS.x, VEHICLE_DIMENSIONS.y) {
                    density = 16F
                    friction = 0.5F
                }

                // detector
                circle(radius = 0.1F) {
                    isSensor = true
                    userData = VehicleDetectorData(veh, e)
                    filter {
                        categoryBits = CollisionFlag.VEHICLE_DETECTOR.flag
                        maskBits = CollisionFlag.ROAD.flag

                    }
                }

                // sight
                val sightDistance = VEHICLE_HALF_DIMENSIONS.y * 4 // TODO vary with speed?
                val widthMod = 1.5F
                val sightWidth = VEHICLE_DIMENSIONS.x * widthMod / 2F
                polygon(Vector2(-sightWidth, 0F),
                        Vector2(-sightWidth, sightDistance),
                        Vector2(sightWidth, sightDistance),
                        Vector2(sightWidth, 0F)) {
                    isSensor = true
                    userData = VehicleSightData(veh)
                    filter {
                        categoryBits = CollisionFlag.VEHICLE_SIGHT.flag
                        maskBits = 0
                    }
                }
            })

    fun render(): Component {
        val colour = Color.FIREBRICK // TODO random colour?
        val dims = VEHICLE_DIMENSIONS
        return RenderComponent(colour, dims)
    }


    val veh = VehicleComponent()
    val e = Entity()
    e.add(physics(e, veh))
            .add(render())
            .add(veh)

    if (driver != null)
        e.add(driver)

    return e
}

// helpers
val renderGetter = ComponentMapper.getFor(RenderComponent::class.java)
val aiGetter = ComponentMapper.getFor(AIInputComponent::class.java)
val inputGetter = ComponentMapper.getFor(InputComponent::class.java)
val physicsGetter = ComponentMapper.getFor(PhysicsComponent::class.java)
val vehicleGetter = ComponentMapper.getFor(VehicleComponent::class.java)
