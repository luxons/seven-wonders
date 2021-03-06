package org.luxons.sevenwonders.engine.cards

import org.junit.Test
import org.luxons.sevenwonders.model.cards.CardBack
import kotlin.test.assertEquals

class CardBackTest {

    @Test
    fun initializedWithImage() {
        val imagePath = "whateverimage.png"
        val (image) = CardBack(imagePath)
        assertEquals(imagePath, image)
    }
}
