package ms.domwillia.cars.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import ktx.app.KtxScreen

const val PPM = 5F
const val CAMERA_MOVE_SPEED = 1F
const val CAMERA_ZOOM_SPEED = 0.05F

class SimScreen : KtxScreen {

    private var camera = OrthographicCamera()
    private val cameraInput = CameraInput()
    private val renderer = ShapeRenderer()

    init {
        // TODO

        Gdx.input.inputProcessor = InputMultiplexer().apply {
            addProcessor(cameraInput)
            addProcessor(object : InputAdapter() {
                override fun keyDown(keycode: Int): Boolean {
                    if (keycode == Input.Keys.ESCAPE)
                        Gdx.app.exit()

                    return true
                }
            })
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f)
    }

    override fun render(delta: Float) {

        camera.translate(cameraInput.getTranslation(CAMERA_MOVE_SPEED))
        val zoom = cameraInput.getZoom(CAMERA_ZOOM_SPEED)
        camera.zoom = MathUtils.clamp(camera.zoom + zoom, 0.1F, 4F)
        camera.update()
        renderer.projectionMatrix = camera.combined

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        renderer.begin(ShapeRenderer.ShapeType.Line)
        renderer.color = Color.ORANGE
        renderer.rect(2F, 2F, 8F, 5F)
        renderer.circle(0F, 0F, 10F, 20)
        renderer.end()
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width / PPM
        camera.viewportHeight = camera.viewportWidth * (height.toFloat() / width.toFloat())
        camera.update()
    }
}