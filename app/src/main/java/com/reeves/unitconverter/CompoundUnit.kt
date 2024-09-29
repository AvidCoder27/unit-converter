package com.reeves.unitconverter

class CompoundUnit(private val names: List<String>) : SimpleUnit(names) {
    private val constituents: MutableMap<SimpleUnit, Int> = mutableMapOf()

    fun addConstituents(news: Map<SimpleUnit, Int>) = news.forEach { constituents[it.key] = it.value }

    override fun getSize(): Pair<Int, Int> = constituents.foldSize()
}