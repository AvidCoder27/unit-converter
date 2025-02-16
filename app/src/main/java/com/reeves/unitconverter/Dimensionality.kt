package com.reeves.unitconverter

data class Dimensionality(val map: Map<DIMENSION, Int>) {
    fun removeNumberDimension() = Dimensionality(map.clean().filterKeys { it != DIMENSION.NUMBER })

    fun inverse() = Dimensionality(map.mapValues { -it.value })

    fun isEmpty() = map.isEmpty()
}

enum class DIMENSION {
    LENGTH, //'d'
    TIME, //'t'
    TEMPERATURE, //'T'
    MASS, //'m'
    ELECTRIC_CURRENT, //'I'
    LUMINOUS_INTENSITY, //'L'
    ROTATION, //'r'
    DIGITAL_INFORMATION, //'b'
    NUMBER, //'n'
}