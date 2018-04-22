package ms.domwillia.cars.world

import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.maps.objects.PolygonMapObject
import com.badlogic.gdx.maps.objects.PolylineMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.math.Vector2
import org.jgrapht.graph.DirectedPseudograph
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

typealias RoadNode = Vector2

data class RoadEdge(val id: Int, val src: Vector2, val dst: Vector2, val lanes: Int)

class World(path: String) {
    val physics = PhysicsWorld(Vector2.Zero, true)

    private var nextEdgeId = 1

    val roadGraph = DirectedPseudograph<RoadNode, RoadEdge>(
            { src, dst -> RoadEdge(nextEdgeId++, src, dst, 2) })


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
    }


    private fun parseEdges(vertices: FloatArray, closed: Boolean) {
        fun addEdge(x0: Float, y0: Float, x1: Float, y1: Float) {
            val src = Vector2(x0, y0)
            val dst = Vector2(x1, y1)
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