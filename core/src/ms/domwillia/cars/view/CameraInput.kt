package ms.domwillia.cars.view

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ms.domwillia.cars.entity.PhysicsComponent

class CameraInput(private val camera: OrthographicCamera) : InputAdapter() {

    companion object {
        private const val DX = 0
        private const val DY = 1
        private const val DZOOM = 2

        private val movement = mapOf(
                Input.Keys.LEFT to Pair(DX, -1),
                Input.Keys.RIGHT to Pair(DX, 1),
                Input.Keys.UP to Pair(DY, 1),
                Input.Keys.DOWN to Pair(DY, -1),

                Input.Keys.PLUS to Pair(DZOOM, -1),
                Input.Keys.MINUS to Pair(DZOOM, 1)
        )

        private const val retrackKey = Input.Keys.CONTROL_LEFT
    }

    private val delta = IntArray(3)

    private var following: PhysicsComponent? = null
    private var actuallyFollow = true

    fun follow(entity: Entity) {
        entity.get<PhysicsComponent>()?.let { following = it }
        actuallyFollow = true
    }

    private fun handleKey(keycode: Int, down: Boolean): Boolean {
        if (keycode == retrackKey && down) {
            actuallyFollow = true
            return true
        }

        var (index, value) = movement[keycode] ?: return false
        if (!down)
            value *= -1
        delta[index] += value

        // stop tracking
        if (delta[0] + delta[1] != 0) {
            actuallyFollow = false
        }

        return true
    }

    fun getTranslation(speed: Float): Vector2 = if (actuallyFollow && following != null) {
        val current = camera.position
        following!!.body.position.cpy().sub(current.x, current.y)
    } else {
        Vector2(delta[0] * speed, delta[1] * speed)
    }

    fun getZoom(speed: Float) = delta[2] * speed

    override fun keyUp(keycode: Int): Boolean {
        return handleKey(keycode, false)
    }

    override fun keyDown(keycode: Int): Boolean {
        return handleKey(keycode, true)
    }
}