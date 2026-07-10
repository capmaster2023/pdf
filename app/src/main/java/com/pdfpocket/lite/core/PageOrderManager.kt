package com.pdfpocket.lite.core

object PageOrderManager {
    fun move(order: List<Int>, from: Int, to: Int): List<Int> {
        require(from in order.indices && to in order.indices)
        return order.toMutableList().apply { add(to, removeAt(from)) }
    }
    fun rotateSelection(rotations: Map<Int, Int>, selected: Set<Int>, degrees: Int): Map<Int, Int> {
        require(degrees in setOf(90, 180, 270))
        return rotations.toMutableMap().apply { selected.forEach { this[it] = ((this[it] ?: 0) + degrees) % 360 } }
    }
}
