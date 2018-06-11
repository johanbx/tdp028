package nu.jobo.prison

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class MainActivity : Activity() {

    lateinit var statusImage: ImageView
    lateinit var pushUpCounter: TextView
    lateinit var sitUpCounter: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusImage = findViewById<ImageView>(R.id.image_prisoner_status)
        pushUpCounter = findViewById<Button>(R.id.text_total_push_ups)
        sitUpCounter = findViewById<Button>(R.id.text_total_sit_ups)
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
        statusImage.setImageResource(R.drawable.died)
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

    fun prisonerPushUp(view: View) {
        var counter: Int? = pushUpCounter.text.toString().toIntOrNull()
        if (counter == null) {
            counter = 0
        }
        counter = counter.inc()
        pushUpCounter.text = counter.toString()
    }

    fun prisonerSitUp(view: View) {
        var counter: Int? = sitUpCounter.text.toString().toIntOrNull()
        if (counter == null) {
            counter = 0
        }
        counter = counter.inc()
        sitUpCounter.text = counter.toString()
    }
}
