package ms.domwillia.cars.ecs

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
import ms.domwillia.cars.world.World

const val PPM = 5F
const val CAMERA_MOVE_SPEED = 1F
const val CAMERA_ZOOM_SPEED = 0.05F


class RenderSystem(
        private val world: World,
        private val cameraInput: CameraInput
) : IteratingSystem(
        Family.all(PhysicsComponent::class.java, DummyRenderComponent::class.java).get()
) {
    private val camera = OrthographicCamera()


    private val physics = ComponentMapper.getFor(PhysicsComponent::class.java)
    private val render = ComponentMapper.getFor(DummyRenderComponent::class.java)
    private val renderer = ShapeRenderer()

//    private val renderQueue = gdxArrayOf<Entity>(ordered = false)

    override fun update(deltaTime: Float) {
        camera.translate(cameraInput.getTranslation(CAMERA_MOVE_SPEED))
        val zoom = cameraInput.getZoom(CAMERA_ZOOM_SPEED)
        camera.zoom = MathUtils.clamp(camera.zoom + zoom, 0.1F, 4F)
        camera.update()
        renderer.projectionMatrix = camera.combined

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // world
        renderer.begin(ShapeRenderer.ShapeType.Filled)
        renderer.color = Color.DARK_GRAY
        val roadWidth = 10F
        for (edge in world.roadGraph.edgeSet()) renderer.rectLine(edge.src, edge.dst, roadWidth)
        for (v in world.roadGraph.vertexSet()) renderer.circle(v.x, v.y, roadWidth / 2)
        renderer.end()

        renderer.begin(ShapeRenderer.ShapeType.Line)
        super.update(deltaTime)
        renderer.end()
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val pos = physics.get(entity).body.position
        renderer.color = render.get(entity).colour
        renderer.circle(pos.x, pos.y, 2F, 30)
    }

    fun resize(width: Int, height: Int) {
        camera.viewportWidth = width / PPM
        camera.viewportHeight = camera.viewportWidth * (height.toFloat() / width.toFloat())
        camera.update()
    }
}
