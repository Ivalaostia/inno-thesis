import org.graphstream.algorithm.generator.RandomGenerator
import org.graphstream.graph.implementations.SingleGraph
import org.graphstream.graph.Edge
import org.graphstream.graph.Node

import java.io.File
import java.util.*

import classes.*


/** fixed random to generate same graphs */
var randomSeed = 42 + 0L
var rand = Random(randomSeed)

/** main graph */
val graph = SingleGraph("Test graph")

/** routes */
val routes = mutableMapOf<Int, MutableList<Int>>()

/** connections inside city?? */
val percentageCommunicating = .15

/** connections between cities, 0 is only one */
val chanceToConnect = 1.0

/** ??? */
var connNum = 1

/** ? */
var num = 0


fun main(args: Array<String>) {

    // TODO: graph selection
    var fileName = "test_graph" // "cities_fallout4"
    generateGraph(File(fileName))

    graph.display()

    // TODO: simulation with trust for thesis / journal

}


/** returns random element from list */
fun <T> List<T>.getRandomElement(rand: Random) = this[rand.nextInt(this.size)]

/** generates city graph */
fun generateGraph(f: File) {

    val buff = Scanner(f)
    val cities = mutableListOf<List<GraphNode>>()
    var cityName = buff.nextLine()
    val oneTown = mutableListOf<GraphNode>()

    graph.clear() // fix
    connNum = 1
    var num = 0

    val addCity = {

        if (oneTown.size > 0) {

            oneTown.forEach {
                val node = graph.addNode<Node>(it.name)
                node.addAttribute("label", it.name + " (" + node.index + ")")
            }

            cities += oneTown.toList()

            buildCityConnections(oneTown)

            oneTown.clear()

        }

    }

    while (buff.hasNextLine()) {

        val line = buff.nextLine()

        if (line.isEmpty()) {
            addCity()
            cityName = buff.nextLine()
        } else {
            oneTown += GraphNode(num++, line, cityName)
        }

    }

    addCity()

    val all = getCommunicatorsFromCities(cities)

    connNum = 0

    //buildCityConnections(all)
    connectCities(all)
    // println(cities.reduce { l1, l2 -> l1 + l2 }.map { "${it.id} = ${it.name} (${it.city})" })

    buff.close()

}


fun buildCityConnections(allCities: List<GraphNode>) {

    val connected = mutableListOf<GraphNode>()

    val disconnected = allCities.toMutableList()

    while (disconnected.isNotEmpty()) {

        val node = disconnected.getRandomElement(rand)

        val howManyToAttach = if (rand.nextDouble() + connNum > connNum + 0.5) connNum else connNum + 1

        if (connected.size >= howManyToAttach) {

            // println("Attaching $howManyToAttach nodes...")
            val copy = connected.toMutableList()

            for (counter in 1..howManyToAttach) {
                val attachTo = copy.getRandomElement(rand)
                copy -= attachTo
                connect(node, attachTo)
            }

        }

        connected += node
        disconnected -= node

    }
}


fun connect(from: GraphNode, to: GraphNode): Boolean {

    return try {
        graph.addEdge<Edge>("edge${num++}", from.name, to.name)
        addEdge(from.id, to.id)
        addEdge(to.id, from.id)
        true
    } catch (e: Exception) {
        false
    }

}


fun getCommunicatorsFromCities(cities: List<List<GraphNode>>): List<List<GraphNode>> {

    val npcs = mutableListOf<List<GraphNode>>()

    for (town in cities) {

        // getting number of communicators
        val comms = Math.ceil(percentageCommunicating * town.size)
        // println("Selecting $comms from city with population=${town.size}...")

        val townCommunicators = mutableListOf<GraphNode>()
        val mutTowns = town.toMutableList()

        for (i in 1..comms.toInt()) {
            val elem = mutTowns.getRandomElement(rand)
            mutTowns -= elem
            townCommunicators += elem
        }

        npcs += townCommunicators

    }

    // println("List of npcs: $npcs")
    return npcs

}


fun connectCities(communicators: List<List<GraphNode>>) {

    for (i in 0 until communicators.size) {

        // making a guaranteed link from this town to another
        val linkFrom = i
        val townFrom = communicators[linkFrom]
        var connected = 0
        val others = communicators.filter { it != townFrom }.reduce { l1, l2 -> l1 + l2 }.toMutableList()
        // println("Connecting town #$linkFrom to random other ${others.size} elements...")

        // iterating over other towns and connecting random element from them to $linkFrom
        for (j in 0 until others.size) {
            val from = townFrom.getRandomElement(rand)
            val to = others.getRandomElement(rand)
            val result = if (connected == 0)
                connect(from, to)
            else probablyConnect(from, to)
            connected += if (result) 1 else 0
            others -= to
        }

        // println("Connected ${connected}")

    }

}



fun probablyConnect(from: GraphNode, to: GraphNode): Boolean {

    val prob = rand.nextDouble()

    return if (prob < chanceToConnect)
        connect(from, to)
    else false

}




fun addEdge(from: Int, to: Int) {

    if (routes[from] == null)
        routes[from] = mutableListOf()
    routes[from]?.add(to)

}


fun createOblivion() {

    routes.clear()

    val gen = RandomGenerator(6.0)
    gen.addNodeLabels(true)
    gen.addSink(graph)
    gen.setRandomSeed(102)
    gen.begin()

    for (i in 0..500)
        gen.nextEvents()
    gen.end()

    for (i in 0 until graph.edgeCount) {
        val edge = graph.getEdge<Edge>(i)
        val from = edge.getSourceNode<Node>().index
        val to = edge.getTargetNode<Node>().index
        addEdge(from, to)
        addEdge(to, from)
    }

    println(routes)

}

