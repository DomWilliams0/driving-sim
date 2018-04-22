package ms.domwillia.cars.view

import com.badlogic.gdx.InputAdapter
import ms.domwillia.cars.entity.InputSystem

class PlayerDriveInput(internal val inputSystem: InputSystem) : InputAdapter() {

    override fun keyUp(keycode: Int): Boolean {
        return inputSystem.handleKey(keycode, false)
    }

    override fun keyDown(keycode: Int): Boolean {
        return inputSystem.handleKey(keycode, true)
    }
}