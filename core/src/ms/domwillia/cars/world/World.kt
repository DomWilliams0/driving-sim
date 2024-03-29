package ms.domwillia.cars.world

import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.maps.objects.PolygonMapObject
import com.badlogic.gdx.maps.objects.PolylineMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.sun.javafx.geom.Point2D
import ktx.box2d.body
import ktx.box2d.filter
import ms.domwillia.cars.entity.VEHICLE_DIMENSIONS
import org.jgrapht.graph.DirectedPseudograph
import org.jgrapht.graph.Pseudograph
import kotlin.math.PI
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

val LANE_WIDTH: Float = VEHICLE_DIMENSIONS.x * 3F

// TODO maybe use a simple id for nodes/edges and lookup properties in a big map
class RoadNode(pos: Vector2, var maxLanes: Int = 1) {
    private val x: Int = MathUtils.floor(pos.x)
    private val y: Int = MathUtils.floor(pos.y)
    private val posVec = Vector2()

    val pos: Vector2
        get() = posVec.set(x.toFloat(), y.toFloat())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadNode

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }

    override fun toString(): String {
        return "RoadNode(x=$x, y=$y, maxLanes=$maxLanes)"
    }


}

data class RoadEdge(val id: Int, val src: Vector2, val dst: Vector2, val lanes: Int) {
    var length: Float = Float.NaN // to be set below
    var width: Float = lanes * LANE_WIDTH

    // TODO is direction ever used as-is or always scaled to length?
    val direction: Vector2 = run {
        val dir = dst.cpy().sub(src)
        length = dir.len()
        dir.nor()
    }
    val reversed = ((direction.angle() % 360) + 360) % 360 > 180


    val centre: Vector2 = src.cpy().add(direction.x * length / 2F, direction.y * length / 2F)
    val laneDirection: Vector2 = run {
        val rot = if (direction.hasSameDirection(Vector2.Y)) PI / 2F else -PI / 2F
        direction.cpy().rotateRad(rot.toFloat()).scl(LANE_WIDTH)
    }
}

typealias NavigationNode = Vector2
typealias NavigationEdge = Int

class World(path: String) {
    val physics = PhysicsWorld(Vector2.Zero, true).apply {
        setContactListener(PhysicsCollisionHandler())
    }

    private var nextEdgeId = 1

    val roadGraph = Pseudograph<RoadNode, RoadEdge>(
            { src, dst ->
                val lanes = 3
                RoadEdge(nextEdgeId++, src.pos, dst.pos, lanes)
            })

    val navigationGraph = DirectedPseudograph<NavigationNode, NavigationEdge> { src, dst ->
        val a = roadGraph.vertexSet().find { it.pos == src } ?: throw IllegalArgumentException("missing node $src")
        val b = roadGraph.vertexSet().find { it.pos == dst } ?: throw IllegalArgumentException("missing node $dst")
        var id = roadGraph.getEdge(a, b).id

        if (src < dst)
            id *= -1
        id
    }

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

        // add nodes and edges
        iterateAllPoints(map) { v, c ->
            for (i in 0..v.lastIndex step 2) {
                v[i] -= minX
                v[i + 1] -= minY
            }
            parseEdges(v, c)
        }

        if (roadGraph.edgeSet().isEmpty())
            throw IllegalStateException("No roads!")

        // add sensor frame
        val staticFrame = physics.body()
        val polygon = PolygonShape()
        val fixDef = FixtureDef().apply {
            shape = polygon
            isSensor = true
            filter {
                categoryBits = CollisionFlag.ROAD.flag
                maskBits = CollisionFlag.VEHICLE_DETECTOR.flag
            }
        }

        // add supplementary nodes
        val tmp = Vector2()
        val subdivide = mutableSetOf<Pair<RoadNode, RoadEdge>>()
        for (e1 in roadGraph.edgeSet()) {
            for (e2 in roadGraph.edgeSet()) {
                if (e1.id <= e2.id) continue
                if (e1.src == e2.src ||
                        e1.src == e2.dst ||
                        e1.dst == e2.src ||
                        e1.dst == e2.dst) continue

                // extend past the end by a little bit
                // TODO my thats a lot of vector allocation here
                val d1 = e1.direction.cpy().scl(LANE_WIDTH * e1.lanes)
                val d2 = e2.direction.cpy().scl(LANE_WIDTH * e2.lanes)
                if (!Intersector.intersectSegments(
                                e1.src.cpy().sub(d1),
                                e1.dst.cpy().add(d1),
                                e2.src.cpy().sub(d2),
                                e2.dst.cpy().add(d2),
                                tmp
                        )) continue

                val newVertex = RoadNode(tmp)
                roadGraph.addVertex(newVertex)

                val vertexPos = newVertex.pos
                val toleranceSqrd = 2
                fun isClose(v: Vector2) = Point2D.distanceSq(v.x, v.y, vertexPos.x, vertexPos.y) <= toleranceSqrd

                if (!isClose(e1.src) && !isClose(e1.dst))
                    subdivide.add(Pair(newVertex, e1))

                if (!isClose(e2.src) && !isClose(e2.dst))
                    subdivide.add(Pair(newVertex, e2))

            }
        }

        // subdivide split edges
        for ((v, e) in subdivide) {
            roadGraph.removeEdge(e)
            roadGraph.addEdge(RoadNode(e.src), v)
            roadGraph.addEdge(v, RoadNode(e.dst))
        }

        // check for too short edges
        for (error in roadGraph.edgeSet().filter { it.length < it.lanes * LANE_WIDTH }) {
            roadGraph.removeEdge(error)
            System.err.println("There is a road that is too short for its width! $error with length ${error.length}")
        }

        // check for orphaned nodes
        for (error in roadGraph.vertexSet().filter { roadGraph.degreeOf(it) == 0 }) {
            roadGraph.removeVertex(error)
            System.err.println("There is a node with no edges! $error")
        }

        // update max lanes of all vertices
        for (v in roadGraph.vertexSet()) {
            v.maxLanes = roadGraph.edgesOf(v).maxBy(RoadEdge::lanes)?.lanes ?: 0
        }

        // navigation graph
        for (e in roadGraph.edgeSet()) {
            navigationGraph.addVertex(e.src)
            navigationGraph.addVertex(e.dst)
            navigationGraph.addEdge(e.src, e.dst)
            navigationGraph.addEdge(e.dst, e.src)
        }

        val vertices = FloatArray(16)
        for (edge in roadGraph.edgeSet()) {
            val (x1, y1) = edge.src
            val (x2, y2) = edge.dst
            val width = edge.width / 2F

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

            staticFrame.createFixture(fixDef).apply { userData = RoadData(edge) }
        }

        for (v in roadGraph.vertexSet()) {
            staticFrame.createFixture(FixtureDef().apply {
                shape = CircleShape().apply {
                    radius = v.maxLanes * 1.25F * LANE_WIDTH / 2F
                    position = v.pos
                }
                isSensor = true
            }).userData = IntersectionData(v)
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

operator fun Vector2.compareTo(o: Vector2) = Comparator.comparing<Vector2, Float>(Vector2::x).thenComparing(Vector2::y).compare(this, o)