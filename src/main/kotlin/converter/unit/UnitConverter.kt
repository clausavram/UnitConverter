package converter.unit

import converter.unit.UnitType.*
import java.util.*

enum class UnitType {
    Length,
    Weight,
    Temperature,
}

@Suppress("unused")
enum class Units(
        val type: UnitType,
        val toBaseUnits: (Double) -> Double,
        val fromBaseUnits: (Double) -> Double,
        val singular: String,
        val plural: String,
        vararg val otherNames: String
) {
    // length units
    METER(Length, 1.0, "meter", "meters", "m"),
    KILOMETER(Length, 1000.0, "kilometer", "kilometers", "km"),
    CENTIMETER(Length, 0.01, "centimeter", "centimeters", "cm"),
    MILLIMETER(Length, 0.001, "millimeter", "millimeters", "mm"),
    MILE(Length, 1609.35, "mile", "miles", "mi"),
    YARD(Length, 0.9144, "yard", "yards", "yd"),
    FOOT(Length, 0.3048, "foot", "feet", "ft"),
    INCH(Length, 0.0254, "inch", "inches", "in"),

    // mass units
    GRAM(Weight, 1.0, "gram", "grams", "g"),
    KILOGRAM(Weight, 1000.0, "kilogram", "kilograms", "kg"),
    MILLIGRAM(Weight, 0.001, "milligram", "milligrams", "mg"),
    POUND(Weight, 453.592, "pound", "pounds", "lb"),
    OUNCE(Weight, 28.349_5, "ounce", "ounces", "oz"),

    // temperature units
    KELVIN(Temperature, 1.0, "Kelvin", "Kelvins", "k"),
    CELSIUS(Temperature, { it + 273.15 }, { it - 273.15 }, "degree Celsius", "degrees Celsius", "dc", "c", "celsius"),
    FAHRENHEIT(Temperature, { (it + 459.67) * 5.0 / 9.0 }, { it * 9.0 / 5.0 - 459.67 }, "degree Fahrenheit", "degrees Fahrenheit", "df", "f", "fahrenheit"),
    ;

    constructor(type: UnitType, factor: Double, singular: String, plural: String, vararg otherNames: String) :
            this(type, { it * factor }, { it / factor }, singular, plural, *otherNames)

    fun convertTo(quantity: Double, destUnit: Units): Double {
        IncompatibleUnitsException.throwIfNecessary(this, destUnit)
        NegativeUnitsException.throwIfNecessary(quantity, destUnit.type)
        return destUnit.fromBaseUnits(this.toBaseUnits(quantity))
    }

    override fun toString(): String = singular
    fun toString(quantity: Double): String = if (quantity == 1.0) singular else plural

    companion object {
        val degreeUnitPrefixes = setOf("degree", "degrees")

        private val unitsByName = values()
                .flatMap { crtUnit ->
                    val pairs = mutableListOf(Pair(crtUnit.singular, crtUnit), Pair(crtUnit.plural, crtUnit))
                    crtUnit.otherNames.forEach { pairs.add(Pair(it, crtUnit)) }
                    pairs
                }
                .map { Pair(it.first.toLowerCase(), it.second) }
                .toMap()

        fun getUnitByName(unitName: String): Units? = unitsByName[unitName]
    }
}

class IncompatibleUnitsException(source: Units, dest: Units)
    : Exception("Cannot convert different unit types: $source (${source.type}) to $dest (${dest.type})") {
    companion object {
        fun throwIfNecessary(source: Units, dest: Units) {
            if (source.type != dest.type) throw IncompatibleUnitsException(source, dest)
        }
    }
}

class NegativeUnitsException(unitType: UnitType) : Exception("$unitType shouldn't be negative.") {
    companion object {
        private val associatedUnitTypes = setOf(Length, Weight)
        fun throwIfNecessary(quantity: Double, unitType: UnitType) {
            if (quantity < 0 && unitType in associatedUnitTypes) {
                throw NegativeUnitsException(unitType)
            }
        }
    }
}

fun main() {
    val scanner = Scanner(System.`in`)

    while (true) {

        print("Enter what you want to convert (or exit): ")
        val firstWord = scanner.next()
        if (firstWord == "exit") break

        val sourceQuantity = firstWord.toDouble()

        var sourceUnitName = scanner.next().toLowerCase()
        if (sourceUnitName in Units.degreeUnitPrefixes) sourceUnitName += " " + scanner.next().toLowerCase()
        val sourceUnit = Units.getUnitByName(sourceUnitName)
        val sourceUnitString = sourceUnit?.toString(sourceQuantity)

        scanner.next() // read the "to" / "in" / any other separator

        var destUnitName = scanner.next().toLowerCase()
        if (destUnitName in Units.degreeUnitPrefixes) destUnitName += " " + scanner.next().toLowerCase()
        val destUnit = Units.getUnitByName(destUnitName)

        if (sourceUnit == null || destUnit == null) {
            printImpossibleConversion(sourceUnit, destUnit)
            continue
        }

        try {
            val convertedQuantity = sourceUnit.convertTo(sourceQuantity, destUnit)
            val destUnitString = destUnit.toString(convertedQuantity)
            println("$sourceQuantity $sourceUnitString is $convertedQuantity $destUnitString")
        } catch (ex: IncompatibleUnitsException) {
            printImpossibleConversion(sourceUnit, destUnit)
        } catch (ex: Exception) {
            println(ex.message)
        }
    }
}

private fun printImpossibleConversion(sourceUnit: Units?, destUnit: Units?) {
    val sourceUnitPlaceholder = sourceUnit?.toString(2.0) ?: "???"
    val destUnitPlaceholder = destUnit?.toString(2.0) ?: "???"
    println("Conversion from $sourceUnitPlaceholder to $destUnitPlaceholder is impossible")
}