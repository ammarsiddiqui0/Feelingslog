package com.example.feelingslog

/**
 * Represents a single feeling log entry.
 * Similar to a TypeScript interface:
 * interface FeelingLog {
 *   id: number;
 *   title: string;
 *   description: string;
 *   tags: string[];
 * }
 */
data class FeelingLog(
    val id: Int,
    val title: String,
    val description: String,
    val tags: List<String>,
    val imageUrl: String? = null
)

// Sample data for the grid
val sampleLogs = listOf(
    FeelingLog(1, "Morning Walk", "The air was so fresh today.", listOf("happy", "calm")),
    FeelingLog(2, "Deadline Stress", "Too much work, feeling overwhelmed.", listOf("sad", "excited")), // excited as in 'jittery'
    FeelingLog(3, "Coffee with Friend", "Caught up with Sarah after a long time.", listOf("happy")),
    FeelingLog(4, "Rainy Afternoon", "Reading a book while it pours outside.", listOf("calm")),
    FeelingLog(5, "Gym Session", "New PR today! Feeling strong.", listOf("excited", "happy")),
    FeelingLog(6, "Late Night Coding", "Finally fixed that stubborn bug.", listOf("excited", "calm")),
    FeelingLog(7, "Quiet Evening", "Just relaxing and watching the sunset.", listOf("calm")),
    FeelingLog(8, "Missed the Bus", "Ran as fast as I could but still missed it.", listOf("sad"))
)
