package ms.domwillia.cars.entity

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Input

class InputSystem : IteratingSystem(
        Family.all(InputComponent::class.java).get()
) {
    companion object {
        // similar to CameraInput, need to generalise?
        const val DX = 0
        const val DY = 1
        const val DBRAKE = 2
        const val DELTA_COUNT = 3

        private val movement = mapOf(
                Input.Keys.A to Pair(DX, -1),
                Input.Keys.D to Pair(DX, 1),
                Input.Keys.W to Pair(DY, 1),
                Input.Keys.S to Pair(DY, -1),
                Input.Keys.SPACE to Pair(DBRAKE, 1)
        )

    }

    private val delta = IntArray(DELTA_COUNT)
    private val inputGetter = ComponentMapper.getFor(InputComponent::class.java)

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val input = inputGetter.get(entity)
        System.arraycopy(delta, 0, input.delta, 0, DELTA_COUNT)
    }

    internal fun handleKey(keycode: Int, down: Boolean): Boolean {
        var (index, value) = movement[keycode] ?: return false

        if (index == DBRAKE)
            delta[index] = if (down) value else 0
        else {
            if (!down)
                value *= -1
            delta[index] += value
        }
        return true
    }
}