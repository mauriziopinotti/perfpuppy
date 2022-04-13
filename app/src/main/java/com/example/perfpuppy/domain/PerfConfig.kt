package com.example.perfpuppy.domain

data class PerfConfig(

    /** Threshold for cpu usage [0-100] */
    val cpuThPerc: Int,

    /** Threshold for memory usage [0-100] */
    val memThPerc: Int,

    /** Threshold for battery usage [0-100] */
    val batteryThPerc: Int,
)
