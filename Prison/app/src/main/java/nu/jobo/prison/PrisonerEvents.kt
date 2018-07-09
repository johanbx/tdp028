package nu.jobo.prison

import java.util.*

/**
 * Created by wirle on 2018-07-04.
 */
class PrisonerEvents(private val mainActivity: MainActivity) {

    // Source: https://stackoverflow.com/questions/45685026/how-can-i-get-a-random-number-in-kotlin
    fun ClosedRange<Int>.random() =
            Random().nextInt(endInclusive - start) +  start

    private fun prisonerFailedEscape() {
        mainActivity.statusImage.setImageResource(R.drawable.escape_failed)
        mainActivity.setTitle(R.string.prisoner_status_failed_escape)
        mainActivity.powerCounter.text = "0"
    }

    private fun prisonerEscaped() {
        mainActivity.statusImage.setImageResource(R.drawable.escaped)
        mainActivity.setTitle(R.string.prisoner_status_escaping)
    }

    private fun powerRandomRangeIncrease(from: Int, to: Int) {
        powerIncrease((from..to).random())
    }

    private fun powerIncrease(power: Int) {
        mainActivity.prisoner.power = mainActivity.prisoner.power.plus(power)
        mainActivity.powerCounter.text = mainActivity.prisoner.power.toString()
        mainActivity.dbUpdate {  }
    }

    fun died() {
        mainActivity.statusImage.setImageResource(R.drawable.died)
        mainActivity.setTitle(R.string.prisoner_status_died)
    }

    /* Events */
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

    /* Actions */
    fun pushUp() {
        mainActivity.prisoner.pushUps = mainActivity.prisoner.pushUps.inc()
        mainActivity.pushUpCounter.text = mainActivity.prisoner.pushUps.toString()
        powerRandomRangeIncrease(10, 20)
    }

    fun sitUp() {
        mainActivity.prisoner.sitUps = mainActivity.prisoner.sitUps.inc()
        mainActivity.sitUpCounter.text = mainActivity.prisoner.sitUps.toString()
        powerRandomRangeIncrease(10, 20)
    }

    fun step() {
        mainActivity.prisoner.steps = mainActivity.prisoner.steps.inc()
        mainActivity.stepCounter.text = mainActivity.prisoner.steps.toString()
        powerRandomRangeIncrease(100, 200)
    }

    fun tryEscape() {
        val currentPower = mainActivity.powerCounter.text.toString().toInt()
        val powerToEscape = MainActivity.POWER_FOR_ESCAPE

        if (currentPower >= powerToEscape ||
                currentPower > (0..powerToEscape).random()) {

            prisonerEscaped()
        }
        else {
            prisonerFailedEscape()
        }
    }

    /* Temporary */
    fun tempAdd1000Power() {
        powerIncrease(1000)
    }
}