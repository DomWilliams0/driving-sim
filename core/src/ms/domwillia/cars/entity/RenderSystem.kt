package ms.domwillia.cars.entity

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import ms.domwillia.cars.view.CameraInput
import ms.domwillia.cars.world.LANE_WIDTH
import ms.domwillia.cars.world.World

const val PPM = 8F
const val CAMERA_MOVE_SPEED = 1F
const val CAMERA_ZOOM_SPEED = 0.01F


class RenderSystem(
        private val world: World,
        private val camera: OrthographicCamera,
        private val cameraInput: CameraInput
) : IteratingSystem(
        Family.all(PhysicsComponent::class.java, RenderComponent::class.java).get()
) {
    private val physicsGetter = ComponentMapper.getFor(PhysicsComponent::class.java)
    private val renderGetter = ComponentMapper.getFor(RenderComponent::class.java)
    private val renderer = ShapeRenderer()

//    private val renderQueue = gdxArrayOf<Entity>(ordered = false)

    override fun update(deltaTime: Float) {

        camera.translate(cameraInput.getTranslation(camera.position, CAMERA_MOVE_SPEED))

        val zoom = cameraInput.getZoom(CAMERA_ZOOM_SPEED)
        camera.zoom = MathUtils.clamp(camera.zoom + zoom, 0.1F, 4F)
        camera.update()
        renderer.projectionMatrix = camera.combined

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // world
        renderer.begin(ShapeRenderer.ShapeType.Filled)
        renderer.identity()
        renderer.color = Color.DARK_GRAY
        for (edge in world.roadGraph.edgeSet()) renderer.rectLine(edge.src, edge.dst, edge.lanes * LANE_WIDTH)
        for (v in world.roadGraph.vertexSet()) renderer.circle(v.pos.x, v.pos.y, v.maxLanes * LANE_WIDTH / 2, 25)

        // entities
        super.update(deltaTime)
        renderer.end()
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val body = physicsGetter.get(entity).body
        val pos = body.position
        val ren = renderGetter.get(entity)

        renderer.identity()
        renderer.translate(pos.x, pos.y, 0F)
        renderer.rotate(0F, 0F, 1F, Math.toDegrees(body.angle.toDouble()).toFloat())

        renderer.color = ren.colour
        renderer.rect(
                -ren.dimensions.x / 2, -ren.dimensions.y / 2,
                ren.dimensions.x, ren.dimensions.y)
    }

    fun resize(width: Int, height: Int) {
        camera.viewportWidth = width / PPM
        camera.viewportHeight = camera.viewportWidth * (height.toFloat() / width.toFloat())
        camera.update()
    }
}
