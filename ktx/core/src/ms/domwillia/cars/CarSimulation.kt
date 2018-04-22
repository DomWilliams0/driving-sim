package ms.domwillia.cars

import ktx.app.KtxGame
import ms.domwillia.cars.view.SimScreen
import ms.domwillia.cars.world.World

class CarSimulation : KtxGame<SimScreen>() {

    override fun create() {

        val world = World("roads.tmx")

        addScreen(SimScreen(world))
        setScreen<SimScreen>()
    }


    override fun dispose() {
    }
}
