package nu.jobo.prison

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.util.*

class MainActivity : Activity() {

    lateinit var statusImage: ImageView
    lateinit var pushUpCounter: TextView
    lateinit var sitUpCounter: TextView
    lateinit var powerCounter: TextView

    // Source: https://stackoverflow.com/questions/45685026/how-can-i-get-a-random-number-in-kotlin
    fun ClosedRange<Int>.random() =
            Random().nextInt(endInclusive - start) +  start

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusImage = findViewById(R.id.image_prisoner_status)
        pushUpCounter = findViewById(R.id.text_total_push_ups)
        sitUpCounter = findViewById(R.id.text_total_sit_ups)
        powerCounter = findViewById(R.id.power)
    }

    fun eventDied(view: View) {
        statusImage.setImageResource(R.drawable.died)
        setTitle(R.string.prisoner_status_died)
    }

    fun eventEscaped(view: View) {
        statusImage.setImageResource(R.drawable.escaped)
        setTitle(R.string.prisoner_status_escaping)
    }

    fun eventGodKill(view: View) {
        statusImage.setImageResource(R.drawable.godkill)
        setTitle(R.string.prisoner_status_killed_by_gods)
    }

    fun eventCaptured(view: View) {
        statusImage.setImageResource(R.drawable.bars)
        setTitle(R.string.prisoner_status_captured)
    }

    fun eventPraiseTheSun(view: View) {
        statusImage.setImageResource(R.drawable.sunpraise)
        setTitle(R.string.prisoner_status_praise_the_sun)
    }

    fun eventWon(view: View) {
        statusImage.setImageResource(R.drawable.free)
        setTitle(R.string.prisoner_status_free)
    }

    private fun powerRandomRangeIncrease(from: Int, to: Int) {
        powerCounter.text = powerCounter.text
                .toString()
                .toInt()
                .plus((from..to).random())
                .toString()
    }

    fun prisonerPushUp(view: View) {
        pushUpCounter.text = pushUpCounter.text
                .toString()
                .toInt()
                .inc()
                .toString()
        powerRandomRangeIncrease(10, 20)
    }

    fun prisonerSitUp(view: View) {
        sitUpCounter.text = sitUpCounter.text
                .toString()
                .toInt()
                .inc()
                .toString()
        powerRandomRangeIncrease(10, 20)
    }
}
