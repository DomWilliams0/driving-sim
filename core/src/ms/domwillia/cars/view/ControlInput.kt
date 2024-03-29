package ms.domwillia.cars.view

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.World
import ktx.box2d.query
import ms.domwillia.cars.entity.AIInputComponent
import ms.domwillia.cars.entity.InputComponent
import ms.domwillia.cars.entity.VEHICLE_DIMENSIONS
import ms.domwillia.cars.world.CollisionFlag
import ms.domwillia.cars.world.UserData
import ms.domwillia.cars.world.VehicleDetectorData

class ControlInput(
        private val camera: OrthographicCamera,
        private val cameraInput: CameraInput,
        private val physics: World
) : InputAdapter() {
    companion object {
        private const val untrackKey = Input.Keys.SHIFT_LEFT
    }

    private var controllingEntity: Entity? = null
    private var backupAI: AIInputComponent? = null


    fun controlEntity(entity: Entity?) {
        when {
            entity == null -> controllingEntity?.run {
                remove(InputComponent::class.java)
                add(backupAI ?: AIInputComponent())
                backupAI = null
            }
            controllingEntity == null -> {
                backupAI = entity.remove(AIInputComponent::class.java) as AIInputComponent?
                entity.add(InputComponent())
            }
            else -> return
        }

        controllingEntity = entity
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {

        val controlFlag = 1
        val followFlag = 2
        val control = when {
            button == Input.Buttons.LEFT && controllingEntity == null -> controlFlag.or(followFlag)
            button == Input.Buttons.RIGHT -> followFlag
            else -> return false
        }

        val worldPoint = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0F))
        val querySizeHalf = VEHICLE_DIMENSIONS.x

        var targetEntity: Entity? = null
        physics.query(worldPoint.x - querySizeHalf, worldPoint.y - querySizeHalf,
                worldPoint.x + querySizeHalf, worldPoint.y + querySizeHalf) { fix ->
            val data = fix.userData ?: return@query true
            if ((data as UserData).type != CollisionFlag.VEHICLE_DETECTOR) return@query true

            targetEntity = (data as VehicleDetectorData).entity
            false // stop query
        }
        targetEntity?.let { e ->
            if (control.and(controlFlag) != 0) controlEntity(e)
            if (control.and(followFlag) != 0) cameraInput.follow(e)
            return true
        }

        return false
    }

    override fun keyDown(keycode: Int) = if (keycode == untrackKey) {
        controlEntity(null)
        true
    } else false

}
