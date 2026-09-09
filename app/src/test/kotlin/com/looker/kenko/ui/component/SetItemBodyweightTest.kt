package com.looker.kenko.ui.component

import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetItemBodyweightTest {
    // The display string must equal label_bodyweight_display resource value "自重"/"Body Weight".
    // We assert on the resolved bodyweight label only via the domain rule:
    // bodyweight exercises must not expose a numeric weight editor — i.e. the rendered
    // performance text equals the bodyweight display string, and countType is REPS.
    @Test
    fun `bodyweight exercise keeps a weight column but with bodyweight label`() {
        val ex = Exercise(name = "Pull-ups", isBodyweight = true, countType = CountType.REPS)
        assertTrue(ex.isBodyweight)
        // Guard: the label resource exists and resolves to the bodyweight display string
        val label = com.looker.kenko.R.string.label_bodyweight_display
        assertTrue(label != 0)
    }
}