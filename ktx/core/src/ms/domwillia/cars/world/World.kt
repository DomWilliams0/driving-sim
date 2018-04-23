package ms.domwillia.cars.world

import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.maps.objects.PolygonMapObject
import com.badlogic.gdx.maps.objects.PolylineMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import ktx.box2d.body
import ms.domwillia.cars.entity.VEHICLE_DIMENSIONS
import org.jgrapht.graph.DirectedPseudograph
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

val LANE_WIDTH: Float = VEHICLE_DIMENSIONS.x * 3F

// TODO maybe use a simple id for nodes/edges and lookup properties in a big map
data class RoadNode(val pos: Vector2, var maxLanes: Int = 1) {
    fun updateLanes(lanes: Int) {
        maxLanes = maxOf(lanes, maxLanes)
    }
}

data class RoadEdge(val id: Int, val src: Vector2, val dst: Vector2, val lanes: Int)

class World(path: String) {
    val physics = PhysicsWorld(Vector2.Zero, true)

    private var nextEdgeId = 1

    val roadGraph = DirectedPseudograph<RoadNode, RoadEdge>(
            { src, dst ->
                val lanes = 2
                src.updateLanes(lanes)
                dst.updateLanes(lanes)
                RoadEdge(nextEdgeId++, src.pos, dst.pos, lanes)
            })


    init {
        fun iterateAllPoints(map: TiledMap, que: ((vertices: FloatArray, closed: Boolean) -> Unit)) {
            for (layer in map.layers) {
                for (obj in layer.objects) {
                    when (obj) {
                        is PolygonMapObject -> que(obj.polygon.transformedVertices, true)
                        is PolylineMapObject -> que(obj.polyline.transformedVertices, false)
                        else -> System.err.println("unsupported map object: ${obj::class.java.simpleName}")
                    }
                }
            }
        }


        val map = TmxMapLoader(InternalFileHandleResolver()).load(path)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        iterateAllPoints(map, { vs, _ ->
            minX = minOf(minX, vs.asSequence().chunked(2).minBy { it[0] }!![0])
            minY = minOf(minY, vs.asSequence().chunked(2).minBy { it[1] }!![1])
        })

        iterateAllPoints(map) { v, c ->
            for (i in 0..v.lastIndex step 2) {
                v[i] -= minX
                v[i + 1] -= minY
            }
            parseEdges(v, c)
        }

        // add road sensors
        val staticFrame = physics.body()
        val polygon = PolygonShape()
        val fixDef = FixtureDef().apply {
            shape = polygon
            isSensor = true
        }

        val vertices = FloatArray(16)
        val tmp = Vector2()
        for (edge in roadGraph.edgeSet()) {
            val (x1, y1) = edge.src
            val (x2, y2) = edge.dst
            val width = LANE_WIDTH * edge.lanes / 2F

            tmp.set(y2 - y1, x1 - x2).nor()
            val tx = tmp.x * width
            val ty = tmp.y * width

            // borrowed from ShapeRenderer::rectLine
            vertices[0] = x1 + tx
            vertices[1] = y1 + ty
            vertices[2] = x1 - tx
            vertices[3] = y1 - ty
            vertices[4] = x2 + tx
            vertices[5] = y2 + ty
            vertices[6] = x2 - tx
            vertices[7] = y2 - ty
            vertices[8] = x2 + tx
            vertices[9] = y2 + ty
            vertices[10] = x1 + tx
            vertices[11] = y1 + ty
            vertices[12] = x2 - tx
            vertices[13] = y2 - ty
            vertices[14] = x1 - tx
            vertices[15] = y1 - ty
            polygon.set(vertices)

            val fix = staticFrame.createFixture(fixDef)
        }
    }


    private fun parseEdges(vertices: FloatArray, closed: Boolean) {
        fun addEdge(x0: Float, y0: Float, x1: Float, y1: Float) {
            val src = RoadNode(Vector2(x0, y0))
            val dst = RoadNode(Vector2(x1, y1))
            roadGraph.addVertex(src)
            roadGraph.addVertex(dst)
            roadGraph.addEdge(src, dst)
        }

        for (i in 0 until vertices.size / 2 - 1) {
            addEdge(vertices[i * 2], vertices[i * 2 + 1],
                    vertices[(i + 1) * 2], vertices[(i + 1) * 2 + 1])
        }
        if (closed)
            addEdge(vertices[vertices.lastIndex - 1], vertices.last(),
                    vertices.first(), vertices[1])
    }
}

operator fun Vector2.component1() = x
operator fun Vector2.component2() = y
