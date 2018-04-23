package ms.domwillia.cars.entity

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.physics.box2d.World

internal const val TPS = 50
internal const val TIMESTEP = 1F / TPS


class PhysicsSystem(val world: World) : EntitySystem() {

    private var accumulator = 0F

    override fun update(deltaTime: Float) {
        val frameTime = Math.min(deltaTime, 0.25f)
        accumulator += frameTime
        if (accumulator >= TIMESTEP) {
            world.step(TIMESTEP, 8, 3)
            accumulator -= TIMESTEP
        }
    }



}