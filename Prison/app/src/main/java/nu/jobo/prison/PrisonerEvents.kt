package nu.jobo.prison

import android.content.Context
import java.util.*

/**
 * Created by wirle on 2018-07-04.
 */
class PrisonerEvents(private val mainActivity: MainActivity) {

    // Source: https://stackoverflow.com/questions/45685026/how-can-i-get-a-random-number-in-kotlin
    fun ClosedRange<Int>.random() =
            Random().nextInt(endInclusive - start) +  start

    fun died() {
        mainActivity.statusImage.setImageResource(R.drawable.died)
        mainActivity.setTitle(R.string.prisoner_status_died)
    }

    fun tryEscape() {
        val currentPower = mainActivity.powerCounter.text.toString().toInt()
        val powerToEscape = MainActivity.POWER_FOR_ESCAPE

        if (
        currentPower >= powerToEscape ||
                currentPower > (0..powerToEscape).random()) {
            prisonerEscaped()
        }
        else {
            prisonerFailedEscape()
        }
    }

    private fun prisonerFailedEscape() {
        mainActivity.statusImage.setImageResource(R.drawable.escape_failed)
        mainActivity.setTitle(R.string.prisoner_status_failed_escape)
        mainActivity.powerCounter.text = "0"
    }

    private fun prisonerEscaped() {
        mainActivity.statusImage.setImageResource(R.drawable.escaped)
        mainActivity.setTitle(R.string.prisoner_status_escaping)
    }

    fun eventGodKill() {
        mainActivity.statusImage.setImageResource(R.drawable.godkill)
        mainActivity.setTitle(R.string.prisoner_status_killed_by_gods)
    }

    fun eventCaptured() {
        mainActivity.statusImage.setImageResource(R.drawable.bars)
        mainActivity.setTitle(R.string.prisoner_status_captured)
    }

    fun eventPraiseTheSun() {
        mainActivity.statusImage.setImageResource(R.drawable.sunpraise)
        mainActivity.setTitle(R.string.prisoner_status_praise_the_sun)
    }

    fun eventWon() {
        mainActivity.statusImage.setImageResource(R.drawable.free)
        mainActivity.setTitle(R.string.prisoner_status_free)
    }

    private fun powerRandomRangeIncrease(from: Int, to: Int) {
        powerIncrease((from..to).random())
    }

    private fun powerIncrease(power: Int) {
        mainActivity.powerCounter.text = mainActivity.powerCounter.text
                .toString()
                .toInt()
                .plus(power)
                .toString()
    }

    fun prisonerPushUp() {
        mainActivity.pushUpCounter.text = mainActivity.pushUpCounter.text
                .toString()
                .toInt()
                .inc()
                .toString()
        powerRandomRangeIncrease(10, 20)
    }

    fun prisonerSitUp() {
        mainActivity.sitUpCounter.text = mainActivity.sitUpCounter.text
                .toString()
                .toInt()
                .inc()
                .toString()
        powerRandomRangeIncrease(10, 20)
    }

    /* Temporary */
    fun tempAdd1000Power() {
        powerIncrease(1000)
    }

    fun powerFromStep() {
        powerRandomRangeIncrease(100, 200)
    }
}