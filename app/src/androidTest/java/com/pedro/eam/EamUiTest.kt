package com.pedro.eam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EamUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigationBetweenTabs() {
        composeTestRule.onNodeWithText("Matérias").performClick()
        composeTestRule.onNodeWithText("Cursos & Matérias").assertIsDisplayed()

        composeTestRule.onNodeWithText("Grade").performClick()
        composeTestRule.onNodeWithText("Grade de Horários").assertIsDisplayed()

        composeTestRule.onNodeWithText("Eventos").performClick()
        composeTestRule.onNodeWithText("Eventos & Provas").assertIsDisplayed()

        composeTestRule.onNodeWithText("Análises").performClick()
        composeTestRule.onNodeWithText("Análises & Diagnóstico").assertIsDisplayed()

        composeTestRule.onNodeWithText("Agenda").performClick()
        composeTestRule.onNodeWithText("Agenda Acadêmica").assertIsDisplayed()
    }
}
