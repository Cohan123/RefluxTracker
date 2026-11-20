package com.example.ui.settings

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.repository.RefluxRepository
import com.example.ui.RefluxViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class SettingsReportTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var viewModel: RefluxViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        viewModel = RefluxViewModel(app, RefluxRepository.create(db))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `report name is stored locally and shown in settings`() {
        composeRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onRequestCsvExport = {}
            )
        }

        composeRule.onNodeWithTag("doctor_report_card").assertExists()
        composeRule.onNodeWithTag("save_doctor_report_button").assertExists()
        composeRule.onNodeWithTag("share_doctor_report_button").assertExists()

        composeRule.onNodeWithTag("doctor_report_name_field")
            .performTextInput("Alex")

        assertEquals("Alex", viewModel.doctorReportUserName.value)
    }
}
