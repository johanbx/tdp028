package nu.jobo.prison

import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class PrisonerData (
        var power: Int,
        var steps: Int,
        var sitUps: Int,
        var pushUps: Int
) : Serializable {
    constructor() : this(0, 0,0, 0)
}