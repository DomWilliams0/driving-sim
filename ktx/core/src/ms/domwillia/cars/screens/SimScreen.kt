package ms.domwillia.cars.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.GL20
import ktx.app.KtxScreen

class SimScreen : KtxScreen {

    init {
        // TODO

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                println("keycode = [$keycode]")
                return true
            }

        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }
}