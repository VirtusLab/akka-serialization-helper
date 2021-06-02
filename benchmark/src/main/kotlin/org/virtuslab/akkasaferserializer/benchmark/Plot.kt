package org.virtuslab.akkasaferserializer.benchmark

import kotlinx.html.FlowContent
import space.kscience.plotly.*
import space.kscience.plotly.models.Bar
import space.kscience.plotly.models.BarMode
import space.kscience.plotly.palettes.Xkcd
import java.io.File

fun main() {
    val jsonData = File("json").listFiles()!!.map { file ->
        file.name.substringBefore(".") to file.readLines().map { it.toInt() }
    }
    val jsonXValues = jsonData.map { it.first }

    val cborData = File("cbor").listFiles()!!.map { file ->
        file.name.substringBefore(".") to file.readLines().map { it.toInt() }
    }
    val cborXValues = cborData.map { it.first }

    Plotly.tabs {
        tab("JSON") {
            barPlot(jsonXValues, jsonData.map { it.second[0] }, "Primitive")
            barPlot(jsonXValues, jsonData.map { it.second[1] }, "ADT")
            barPlot(jsonXValues, jsonData.map { it.second[2] }, "Sequence")
        }
        tab("CBOR") {
            barPlot(cborXValues, cborData.map { it.second[0] }, "Primitive")
            barPlot(cborXValues, cborData.map { it.second[1] }, "ADT")
            barPlot(cborXValues, cborData.map { it.second[2] }, "Sequence")
        }
        tab("All") {
            combinedBarPlot(
                jsonXValues,
                cborXValues,
                jsonData.map { it.second[0] },
                cborData.map { it.second[0] },
                "Primitive"
            )
            combinedBarPlot(
                jsonXValues,
                cborXValues,
                jsonData.map { it.second[1] },
                cborData.map { it.second[1] },
                "ADT"
            )
            combinedBarPlot(
                jsonXValues,
                cborXValues,
                jsonData.map { it.second[2] },
                cborData.map { it.second[2] },
                "Sequence"
            )
        }

    }.makeFile()
}


fun FlowContent.barPlot(xValues: List<String>, yValues: List<Int>, plotTitle: String) {
    plot(config = PlotlyConfig {
        responsive = true
    }) {
        bar {
            x.set(xValues)
            y.set(yValues)
        }

        layout {
            title = plotTitle
            xaxis {
                title = "library"
            }
            yaxis {
                title = "milliseconds"
            }
        }
    }
}

fun FlowContent.combinedBarPlot(
    jsonXValues: List<String>,
    cborXValues: List<String>,
    jsonYValues: List<Int>,
    cborYValues: List<Int>,
    plotTitle: String
) {
    val jsonTrace = Bar {
        x.set(jsonXValues)
        y.set(jsonYValues)
        name = "JSON"
        marker {
            color(Xkcd.BLUEBERRY)
        }
    }

    val cborTrace = Bar {
        x.set(cborXValues)
        y.set(cborYValues)
        name = "CBOR"
        marker {
            color(Xkcd.REDDISH)
        }
    }
    plot(config = PlotlyConfig {
        responsive = true
    }) {
        traces(jsonTrace, cborTrace)
        layout {
            title = plotTitle
            barmode = BarMode.group
            xaxis {
                title = "library"
            }
            yaxis {
                title = "milliseconds"
            }
        }
    }
}
