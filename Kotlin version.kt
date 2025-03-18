@file:Suppress("LocalVariableName","DuplicatedCode", "DANGEROUS_CHARACTERS")

import java.util.*
import kotlin.collections.ArrayDeque

data class Matching(val leftPairs: IntArray, val rightPairs: IntArray, val weightSum: Int)

/**
 * Computes the maximum weight matching with run time significantly less than `O(|L||E|)` in experimental tests on random graphs. 
 * Note that integer weights are not required, whereas it could probably accelerate the algorithm. 
 * See https://arxiv.org/abs/2502.20889 for detailed information. 
 * @param `|L|` Size of vertices in L.
 * @param `|R|` Size of vertices in R.
 * @param adj The adjacent list. The type `Pair` is the adjacent vertex with weight.
 */
fun unbalancedIncompleteLEKMWithListAndInitialMatching(`|L|`: Int, `|R|`: Int, adj: List<List<Pair<Int, Int>>>): Matching {
    val leftPairs = IntArray(`|L|`) { -1 }
    val rightPairs = IntArray(`|R|`) { -1 }
    val rightParents = IntArray(`|R|`) { -1 }
    val rightVisited = BooleanArray(`|R|`)

    val visitedLefts = mutableListOf<Int>()
    val visitedRights = mutableListOf<Int>()
    // Those with potential to be introduced. When the vertex is introduced after h adjustment, it remains in this list,
    // but rightOnEdge[it] = false
    val onEdgeRights = mutableListOf<Int>()
    val rightOnEdge = BooleanArray(`|R|`)

    val leftLabels = IntArray(`|L|`) { l -> adj[l].maxOfOrNull { it.second } ?: 0 }
    val rightLabels = IntArray(`|R|`)
    val slacks = IntArray(`|R|`){ Int.MAX_VALUE }
    val q = ArrayDeque<Int>()

    fun advance(r: Int): Boolean {
        rightOnEdge[r] = false
        rightVisited[r] = true
        visitedRights += r
        var l = rightPairs[r]
        if (l != -1) {
            q += l
            visitedLefts += l
            return false
        }

        // apply the found augment path
        @Suppress("NAME_SHADOWING")
        var r = r
        while (r != -1) {
            l = rightParents[r]
            val prevR = leftPairs[l]
            leftPairs[l] = r
            rightPairs[r] = l
            r = prevR
        }
        return true
    }

    fun bfsUntilAppliesAugmentPath(firstUnmatchedR: Int)  {
        while (true) {
            while (q.any()) {
                val l = q.removeFirst()
                if (leftLabels[l] == 0) {
                    rightParents[firstUnmatchedR] = l
                    advance(firstUnmatchedR)
                    return
                }
                if (slacks[firstUnmatchedR] > leftLabels[l]) {
                    slacks[firstUnmatchedR] = leftLabels[l]
                    rightParents[firstUnmatchedR] = l
                    if(!rightOnEdge[firstUnmatchedR]) {
                        onEdgeRights += firstUnmatchedR
                        rightOnEdge[firstUnmatchedR] = true
                    }
                }

                for ((r, w) in adj[l]) {
                    if (rightVisited[r]) continue
                    val diff = leftLabels[l] + rightLabels[r] - w
                    when {
                        diff == 0 -> {
                            rightParents[r] = l
                            if (advance(r)) return
                        }

                        slacks[r] > diff -> {
                            rightParents[r] = l
                            slacks[r] = diff
                            if(!rightOnEdge[r]) {
                                onEdgeRights += r
                                rightOnEdge[r] = true
                            }
                        }
                    }
                }
            }

            var delta = Int.MAX_VALUE
            for (r in onEdgeRights) {
                if (rightOnEdge[r])
                    delta = min(delta, slacks[r])
            }

            for (l in visitedLefts)
                leftLabels[l] -= delta

            for (r in visitedRights)
                rightLabels[r] += delta

            for (r in onEdgeRights)
                if (rightOnEdge[r]){
                    slacks[r] -= delta
                    if(slacks[r] == 0 && advance(r))
                        return
                }
        }
    }

    // initial greedy matching
    for(l in 0..<`|L|`) {
        for((r, w) in adj[l]){
            if (rightPairs[r] == -1 && leftLabels[l] + rightLabels[r] == w){
                leftPairs[l] = r
                rightPairs[r] = l
                break
            }
        }
    }

    for (l in 0..<`|L|`) {
        if (leftPairs[l] != -1) continue
        q.clear()

        for(r in visitedRights){
            rightVisited[r] = false
        }
        for(r in onEdgeRights){
            rightOnEdge[r] = false
            slacks[r] = Int.MAX_VALUE
        }

        visitedLefts.clear()
        visitedRights.clear()
        onEdgeRights.clear()

        visitedLefts += l
        q += l
        val firstUnmatchedR = (0..<`|R|`).first { rightPairs[it] == -1 }
        bfsUntilAppliesAugmentPath(firstUnmatchedR)
    }

    var sum = 0
    outer@ for(l in 0..< `|L|`){
        for((r, w) in adj[l]){
            if(r == leftPairs[l]) {
                sum += w
                continue@outer
            }
        }
        // undo virtual matching
        val r = leftPairs[l]
        leftPairs[l] = -1
        rightPairs[r] = -1
    }
    
    return Matching(leftPairs, rightPairs, sum)
}
