package org.example
import kotlin.random.Random
import kotlinx.coroutines.*

const val PITY_5 = 90
const val PITY_4 = 10
const val BASE_RATE_5 = 0.006
const val BASE_RATE_4 = 0.051
const val SOFT_PITY_START = 75
const val SOFT_PITY_INCREASE = 0.06

val EVENT_CHARACTER = "[Фури]"
val FIVE_STAR = listOf("[Дилюк]", "[Мона]", "[Кэ Цин]", "[Кицин]", "[Чича]")
val FOUR_STAR = listOf("[Розария]", "[Фишль]", "[Барбара]", "[Сахароза]", "[Беннет]")
val THREE_STAR = listOf("[Меч]", "[Лук]", "[Копьё]", "[Катализатор]", "[Двуручный меч]")

class GenshinGacha {
    private var pity5 = 0
    private var pity4 = 0
    private var eventGuaranteed = false
    private val receivedCharacters = mutableMapOf<String, Long>()

    private fun roll(): String {
        pity5++
        pity4++
        val rate5 = BASE_RATE_5 + (maxOf(0, pity5 - SOFT_PITY_START) * SOFT_PITY_INCREASE)
        val rate4 = BASE_RATE_4

        val roll = Random.nextDouble()
        return when {
            pity5 >= PITY_5 || roll < rate5 -> {
                pity5 = 0
                pity4 = 0
                val character = if (eventGuaranteed || Random.nextBoolean()) {
                    eventGuaranteed = false
                    EVENT_CHARACTER
                } else {
                    eventGuaranteed = true
                    FIVE_STAR.random()
                }
                receivedCharacters[character] = receivedCharacters.getOrDefault(character, 0) + 1
                character
            }
            pity4 >= PITY_4 || roll < rate4 -> {
                pity4 = 0
                val character = FOUR_STAR.random()
                receivedCharacters[character] = receivedCharacters.getOrDefault(character, 0) + 1
                character
            }
            else -> {
                val character = THREE_STAR.random()
                receivedCharacters[character] = receivedCharacters.getOrDefault(character, 0) + 1
                character
            }
        }
    }

    fun multiRoll(times: Int) {
        repeat(times) { roll() }
    }

    fun massiveRoll(times: Long) = runBlocking {
        val step = maxOf(1, times / 100) // Обновление статуса каждые 1%
        var completed = 0L

        coroutineScope {
            repeat(4) {
                launch(Dispatchers.Default) {
                    val localCharacters = mutableMapOf<String, Long>()
                    for (i in it until times step 4) {
                        val result = roll()
                        localCharacters[result] = localCharacters.getOrDefault(result, 0) + 1
                        if (++completed % step == 0L) {
                            println("Выполнено ${completed * 100 / times}%")
                        }
                    }
                    synchronized(receivedCharacters) {
                        localCharacters.forEach { (k, v) ->
                            receivedCharacters[k] = receivedCharacters.getOrDefault(k, 0L) + v
                        }
                    }
                }
            }
        }
    }

    fun showInventory() {
        println("\nВаш инвентарь:")
        receivedCharacters.entries.sortedByDescending { it.value }.forEach { (char, count) ->
            println("$char: $count")
        }

        val eventFiveStars = receivedCharacters[EVENT_CHARACTER] ?: 0
        val standardFiveStars = FIVE_STAR.sumOf { receivedCharacters[it] ?: 0 }
        val ratio = if (standardFiveStars > 0) eventFiveStars.toDouble() / standardFiveStars else 0.0
        println("Соотношение ивентовых 5★ к обычным 5★: $eventFiveStars:$standardFiveStars (%.2f)".format(ratio))
    }
}

fun main() {
    val gacha = GenshinGacha()
    while (true) {
        println("Введите '1' для одной крутки, '2' для десяти, '3' для ста, '4' для тысячи, '5' для миллиарда, '6' для десяти миллиардов,'inv' для просмотра инвентаря, 'exit' для выхода: ")
        when (val cmd = readLine()) {
            "1" -> gacha.multiRoll(1)
            "2" -> gacha.multiRoll(10)
            "3" -> gacha.multiRoll(100)
            "4" -> gacha.multiRoll(1000)
            "5" -> gacha.massiveRoll(1000000000)
            "6" -> gacha.massiveRoll(10000000000)
            "inv" -> gacha.showInventory()
            "exit" -> return
            else -> println("Неверный ввод")
        }
    }
}
