package ms.domwillia.cars.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2

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

class CameraInput : InputAdapter() {

    private val delta = IntArray(3)

    private fun handleKey(keycode: Int, down: Boolean): Boolean {
        var (index, value) = movement[keycode] ?: return false
        if (!down)
            value *= -1
        delta[index] += value
        return true
    }

    fun getTranslation(speed: Float) = Vector2(delta[0] * speed, delta[1] * speed)

    fun getZoom(speed: Float) = delta[2] * speed

    override fun keyUp(keycode: Int): Boolean {
        return handleKey(keycode, false)
    }

    override fun keyDown(keycode: Int): Boolean {
        return handleKey(keycode, true)
    }
}