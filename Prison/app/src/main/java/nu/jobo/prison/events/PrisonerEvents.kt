package nu.jobo.prison.events

import android.widget.Toast
import nu.jobo.prison.activities.MainActivity
import nu.jobo.prison.datacontainers.PrisonerData
import nu.jobo.prison.R
import java.util.*

class PrisonerEvents(private val mainActivity: MainActivity) {

    // Source: https://stackoverflow.com/questions/45685026/how-can-i-get-a-random-number-in-kotlin
    fun ClosedRange<Int>.random() =
            Random().nextInt(endInclusive - start) +  start

    private fun prisonerFailedEscape() {
        mainActivity.setTitle(R.string.prisoner_status_failed_escape)
        mainActivity.statusImage.setImageResource(R.drawable.escape_failed)
        mainActivity.prisoner = PrisonerData() // reset prisoner
        mainActivity.dbUpdate {  } // Update db
    }

    private fun prisonerEscaped() {
        if (!MainActivity.escaped) {
            mainActivity.statusImage.setImageResource(R.drawable.escaped)
            mainActivity.setTitle(R.string.prisoner_status_escaping)
            mainActivity.escapeNotification()
            MainActivity.escaped = true
            mainActivity.initGeofence()
        } else {
            Toast.makeText(mainActivity.applicationContext,
                    "You have already escaped your cage", Toast.LENGTH_LONG).show()
        }
    }

    private fun powerRandomRangeIncrease(from: Int, to: Int) {
        powerIncrease((from..to).random())
    }

    private fun powerIncrease(power: Int) {
        mainActivity.prisoner.power = mainActivity.prisoner.power.plus(power)
        mainActivity.powerCounter.text = mainActivity.prisoner.power.toString()
        mainActivity.dbUpdate {  }
    }

    fun eventWon() {
        mainActivity.statusImage.setImageResource(R.drawable.free)
        mainActivity.setTitle(R.string.prisoner_status_free)
    }

    // Actions
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
        val powerToEscape = mainActivity.resources.getInteger(R.integer.power_to_escape)

        if (currentPower >= powerToEscape ||
                currentPower > (0..powerToEscape).random()) {

            prisonerEscaped()
        }
        else {
            prisonerFailedEscape()
        }
    }
}