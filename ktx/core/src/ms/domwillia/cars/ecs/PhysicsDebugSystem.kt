package ms.domwillia.cars.ecs

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import ms.domwillia.cars.world.World

class PhysicsDebugSystem(val world: World, val camera: Camera) : EntitySystem() {
    private val renderer = Box2DDebugRenderer()

    override fun update(deltaTime: Float) {
        renderer.render(world.physics, camera.combined)
    }
}