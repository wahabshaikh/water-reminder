package com.sippy.app

import kotlin.random.Random

/**
 * Sippy's whole personality. Every line reads fine with or without a name, so a
 * nameless install never shows an awkward gap where a greeting should be.
 */
object Messages {

    private val withName = listOf(
        "%s, water break! 💧",
        "psst… %s! 👀 sip time",
        "hey %s, your plant self is thirsty 🌱",
        "%s!! hydration station 🚰",
        "one sip for %s, one leap for hydration 🚀",
        "%s, I brought you water 🥤",
        "knock knock, %s. it's water 💧",
        "%s, be the puddle you wish to see 🫧",
    )

    private val withoutName = listOf(
        "water break! 💧",
        "psst… 👀 sip time",
        "your plant self is thirsty 🌱",
        "hydration station 🚰",
        "one sip for you, one leap for hydration 🚀",
        "I brought you water 🥤",
        "knock knock. it's water 💧",
        "be the puddle you wish to see 🫧",
    )

    private val bodies = listOf(
        "just a few gulps, that's it ✨",
        "no pressure, only bubbles 🫧",
        "future you says thanks 💙",
        "tiny sip, big win 🏆",
        "the glass misses you 🥲",
        "hydrate, then dominate 😤",
    )

    fun title(name: String, random: Random = Random): String =
        if (name.isBlank()) withoutName.random(random)
        else withName.random(random).format(name)

    fun body(random: Random = Random): String = bodies.random(random)

    /** Shown on the home screen once the daily goal is met. */
    fun celebration(name: String): String =
        if (name.isBlank()) "goal reached! you absolute fountain 🎉"
        else "$name, goal reached! you absolute fountain 🎉"

    /** The line under the mascot on the home screen. */
    fun greeting(name: String, glasses: Int, goal: Int): String = when {
        glasses == 0 && name.isBlank() -> "let's get you started 💧"
        glasses == 0 -> "morning, $name! first sip? 💧"
        glasses >= goal -> "all done for today 🎉"
        else -> "$glasses of $goal glasses — keep going!"
    }
}
