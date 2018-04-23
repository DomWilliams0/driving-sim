package ms.domwillia.cars.world

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import ms.domwillia.cars.entity.VehicleComponent
import kotlin.experimental.or

enum class CollisionFlag(val flag: Short) {
    ROAD(1.shl(0)),
    VEHICLE_DETECTOR(1.shl(1)),
    VEHICLE_SIGHT(1.shl(2));

    fun combine(other: CollisionFlag) = flag.or(other.flag)
}

sealed class UserData(val type: CollisionFlag) {
    fun combine(other: UserData) = type.combine(other.type)
}

data class RoadData(val road: RoadEdge) : UserData(CollisionFlag.ROAD)
data class VehicleDetectorData(val vehicle: VehicleComponent, val entity: Entity) : UserData(CollisionFlag.VEHICLE_DETECTOR)
data class VehicleSightData(val vehicle: VehicleComponent) : UserData(CollisionFlag.VEHICLE_SIGHT)


// userdatas will be sorted by their tag type
private typealias CollisionHandler = (begin: Boolean, a: UserData, b: UserData) -> Unit

private val handlers = mapOf<Short, CollisionHandler>(
        CollisionFlag.ROAD.combine(CollisionFlag.VEHICLE_DETECTOR) to ::handleRoadAndCar
)


private fun handleCollision(contact: Contact, begin: Boolean) {
    val a: UserData = contact.fixtureA.userData as UserData? ?: return
    val b: UserData = contact.fixtureB.userData as UserData? ?: return

    val flag = a.combine(b)
    handlers[flag]?.let {
        if (a.type < b.type)
            it(begin, a, b)
        else
            it(begin, b, a)
    }
}

class PhysicsCollisionHandler : ContactListener {
    override fun beginContact(contact: Contact) = handleCollision(contact, true)

    override fun endContact(contact: Contact) = handleCollision(contact, false)

    override fun preSolve(contact: Contact?, oldManifold: Manifold?) {}

    override fun postSolve(contact: Contact?, impulse: ContactImpulse?) {}

}

fun handleRoadAndCar(begin: Boolean, a: UserData, b: UserData) {
    val road = (a as RoadData).road
    val stack = (b as VehicleDetectorData).vehicle.currentRoadStack

    if (begin)
        stack.add(road)
    else
        stack.remove(road)
}
