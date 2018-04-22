package ms.domwillia.cars.view

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import ms.domwillia.cars.entity.*
import ms.domwillia.cars.world.World

class SimScreen(world: World) : KtxScreen {

    private val camera = OrthographicCamera()
    private val cameraInput = CameraInput()
    private val driverInput = PlayerDriveInput(InputSystem())

    private val engine = Engine().apply {
        addSystem(PhysicsSystem(world.physics))
        addSystem(VehicleSystem())
        addSystem(PlayerDrivingSystem())
        addSystem(AIDrivingSystem())
        addSystem(driverInput.inputSystem)
        addSystem(RenderSystem(world, camera, cameraInput))

        addEntity(createVehicleEntity(world.physics, Vector2(1F, 2F), InputComponent()))
        addEntity(createVehicleEntity(world.physics, Vector2(3F, 2F), AIInputComponent(0)))
        addEntity(createVehicleEntity(world.physics, Vector2(5F, 2F)))
    }

    private val debugRender = PhysicsDebugSystem(world, camera)

    private fun toggleDebugRender(keycode: Int, debug: Boolean): Boolean {
        return if (keycode == Input.Keys.J) {
            if (debug)
                engine.addSystem(debugRender)
            else
                engine.removeSystem(debugRender)
            true
        } else false
    }

    init {
        Gdx.input.inputProcessor = InputMultiplexer().apply {
            addProcessor(cameraInput)
            addProcessor(driverInput)
            addProcessor(object : InputAdapter() {
                override fun keyDown(keycode: Int): Boolean = toggleDebugRender(keycode, true)
                override fun keyUp(keycode: Int): Boolean = toggleDebugRender(keycode, false)
            })

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
        engine.update(delta)
    }

    override fun resize(width: Int, height: Int) {
        engine.getSystem(RenderSystem::class.java).resize(width, height)
    }
}