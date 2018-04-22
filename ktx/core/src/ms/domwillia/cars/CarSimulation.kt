package ms.domwillia.cars

import ktx.app.KtxGame
import ms.domwillia.cars.screens.SimScreen

class CarSimulation : KtxGame<SimScreen>() {

    override fun create() {

        addScreen(SimScreen())
        setScreen<SimScreen>()
    }

    override fun render() {
    }

    override fun dispose() {
    }
}
