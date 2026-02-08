package com.example.z_editor.data

import com.example.z_editor.data.repository.ZombiePropertiesRepository

object WavePointAnalysis {

    object ExpectationCalculator {
        data class InputEntry(val id: String, val cost: Int, val weight: Double)

        fun calculate(entries: List<InputEntry>, totalPoints: Int): Map<String, Double> {
            if (totalPoints <= 0 || entries.isEmpty()) return entries.associate { it.id to 0.0 }

            val validEntries = entries.filter { it.weight > 0 && it.cost >= 0 }
            if (validEntries.isEmpty()) return emptyMap()

            val safePoints = totalPoints.coerceAtMost(20000)
            val numTypes = validEntries.size
            // exp[僵尸索引][当前点数]
            val exp = Array(numTypes) { DoubleArray(safePoints + 1) }

            for (p in 1..safePoints) {
                var weightSum = 0.0
                val affordableFlags = BooleanArray(numTypes)

                for (i in 0 until numTypes) {
                    val canAfford = validEntries[i].cost in 1..p
                    affordableFlags[i] = canAfford
                    if (canAfford) weightSum += validEntries[i].weight
                }

                if (weightSum <= 0.0) {
                    for (j in 0 until numTypes) exp[j][p] = exp[j][p - 1]
                    continue
                }

                for (j in 0 until numTypes) {
                    var currentExpSum = 0.0
                    for (k in 0 until numTypes) {
                        if (affordableFlags[k]) {
                            val prob = validEntries[k].weight / weightSum
                            val costK = validEntries[k].cost
                            var term = exp[j][p - costK]
                            if (k == j) term += 1.0
                            currentExpSum += prob * term
                        }
                    }
                    exp[j][p] = currentExpSum
                }
            }

            val result = mutableMapOf<String, Double>()
            validEntries.forEachIndexed { index, entry ->
                result[entry.id] = exp[index][safePoints]
            }
            entries.forEach { if (!result.containsKey(it.id)) result[it.id] = 0.0 }
            return result
        }
    }

    fun calculateExpectation(
        points: Int,
        parsedData: ParsedLevelData
    ): Map<String, Double> {
        if (points <= 0) return emptyMap()

        val waveModule = parsedData.waveModule ?: return emptyMap()

        val zombiesList = waveModule.dynamicZombies
        if (zombiesList.isNullOrEmpty()) {
            return emptyMap()
        }

        val dynamicGroup = zombiesList[0]
        val zombiePool = dynamicGroup.zombiePool

        if (zombiePool.isEmpty()) return emptyMap()

        val inputs = zombiePool.map { rtid ->
            val alias = RtidParser.parse(rtid)?.alias ?: rtid
            val typeName = ZombiePropertiesRepository.getTypeNameByAlias(alias)
            val stats = ZombiePropertiesRepository.getStats(typeName.toString())

            ExpectationCalculator.InputEntry(
                id = typeName.toString(),
                cost = stats.cost,
                weight = stats.weight.toDouble()
            )
        }
        return ExpectationCalculator.calculate(inputs, points)
    }
}