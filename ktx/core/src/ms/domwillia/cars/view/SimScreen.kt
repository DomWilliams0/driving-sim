package ms.domwillia.cars.view

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import ktx.app.KtxScreen
import ktx.box2d.body
import ktx.box2d.circle
import ms.domwillia.cars.ecs.DummyRenderComponent
import ms.domwillia.cars.ecs.PhysicsComponent
import ms.domwillia.cars.ecs.PhysicsSystem
import ms.domwillia.cars.ecs.RenderSystem
import ms.domwillia.cars.world.World

class SimScreen(world: World) : KtxScreen {

    private val cameraInput = CameraInput()
    private val engine = Engine().apply {
        addSystem(PhysicsSystem())
        addSystem(RenderSystem(world, cameraInput))

        addEntity(Entity().apply {
            val dummyBody = getSystem(PhysicsSystem::class.java).world.body(BodyDef.BodyType.DynamicBody)
            dummyBody.circle(2F)
            dummyBody.applyLinearImpulse(Vector2(2F, 2F), dummyBody.worldCenter, true)
            add(PhysicsComponent(dummyBody))
            add(DummyRenderComponent(Color.ORANGE))
        })
    }

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
        engine.update(delta)
    }

    override fun resize(width: Int, height: Int) {
        engine.getSystem(RenderSystem::class.java).resize(width, height)
    }
}