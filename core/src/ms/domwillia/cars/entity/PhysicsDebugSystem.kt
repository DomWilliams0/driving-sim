package ms.domwillia.cars.entity

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import ms.domwillia.cars.world.World

class PhysicsDebugSystem(val world: World, val camera: Camera) : IteratingSystem(
        Family.all(PhysicsComponent::class.java).get()
) {

    private val physicsRenderer = Box2DDebugRenderer()
    private val shapeRenderer = ShapeRenderer()

    override fun update(deltaTime: Float) {
        physicsRenderer.render(world.physics, camera.combined)
        shapeRenderer.projectionMatrix = camera.combined

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        super.update(deltaTime)
        shapeRenderer.end()
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val (body, turnRate, speed) = physicsGetter.get(entity)
        val pos = body.position

        shapeRenderer.identity()
        shapeRenderer.translate(pos.x, pos.y, 0F)
        shapeRenderer.rotate(0F, 0F, 1F, Math.toDegrees(body.angle.toDouble()).toFloat())
        shapeRenderer.color = Color.PINK

        for (mul in sequenceOf(1, -1)) {
            val rad = speed / turnRate * mul
            shapeRenderer.circle(rad, 0F, rad, 30)
        }

    }
}