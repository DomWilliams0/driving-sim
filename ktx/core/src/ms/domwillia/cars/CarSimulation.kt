package ms.domwillia.cars

import ktx.app.KtxGame
import ms.domwillia.cars.view.SimScreen

class CarSimulation : KtxGame<SimScreen>() {

    override fun create() {

        addScreen(SimScreen())
        setScreen<SimScreen>()
    }


    override fun dispose() {
    }
}
