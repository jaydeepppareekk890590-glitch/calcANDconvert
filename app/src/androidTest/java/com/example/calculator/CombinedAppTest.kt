package com.example.calculator

import android.graphics.Bitmap
import android.graphics.Rect
import android.content.pm.ActivityInfo
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.RootMatchers.isDialog
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.HorizontalScrollView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import org.hamcrest.Matchers.equalTo
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CombinedAppTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val preferences by lazy { instrumentation.targetContext.getSharedPreferences("calc_and_convert", 0) }
    private var original = mapOf<String, String?>()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before fun prepare() {
        original = listOf("history", "bigha", "biswa").associateWith { preferences.getString(it, null) }
        preferences.edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun restore() {
        scenario.close()
        val editor = preferences.edit().clear()
        original.forEach { (key, value) -> if (value != null) editor.putString(key, value) }
        editor.commit()
    }

    private fun find(view: View, tag: String): View? {
        if (view.tag == tag) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) find(view.getChildAt(index), tag)?.let { return it }
        return null
    }

    private fun click(tag: String) { scenario.onActivity { activity -> assertNotNull(find(activity.window.decorView, tag)); find(activity.window.decorView, tag)!!.performClick() }; instrumentation.waitForIdleSync() }
    private fun text(tag: String, value: String) { scenario.onActivity { activity -> (find(activity.window.decorView, tag) as EditText).setText(value) }; instrumentation.waitForIdleSync() }
    private fun assertText(tag: String, expected: String) { scenario.onActivity { activity -> assertEquals(expected, (find(activity.window.decorView, tag) as TextView).text.toString()) } }

    @Test fun browseCategorySliderBothDirections() {
        click("tab_1")
        instrumentation.waitForIdleSync()
        onView(withTagValue(equalTo("category_slider" as Any))).perform(swipeLeft())
        scenario.onActivity { activity ->
            val slider = find(activity.window.decorView, "category_slider") as SeekBar
            assertTrue(slider.max > 0)
        }
        onView(withTagValue(equalTo("category_slider" as Any))).perform(swipeRight())
        scenario.onActivity { activity ->
            val slider = find(activity.window.decorView, "category_slider") as SeekBar
            val strip = find(activity.window.decorView, "category_strip") as HorizontalScrollView
            assertTrue(strip.scrollX > slider.max / 2)
            assertEquals(strip.scrollX, slider.progress)
        }
        click("category_Weight")
        assertText("from_unit", "Kilograms (kg) ▾")
        onView(withTagValue(equalTo("category_slider" as Any))).perform(swipeLeft())
        scenario.onActivity { activity ->
            val slider = find(activity.window.decorView, "category_slider") as SeekBar
            val strip = find(activity.window.decorView, "category_strip") as HorizontalScrollView
            assertTrue(strip.scrollX < slider.max / 2)
            assertEquals(strip.scrollX, slider.progress)
        }
        assertText("from_unit", "Kilograms (kg) ▾")
        click("category_Angle")
        assertText("from_unit", "Degrees (°) ▾")
    }

    @Test fun calculatorAndRecreation() {
        listOf("0", ".", "1", ".", "2").forEach { click("key_$it") }
        assertText("expression", "0.12")
        click("key_AC")
        listOf("2", "+", "3", "×", "4", "=").forEach { click("key_$it") }
        assertText("expression", "14")
        click("key_+"); click("key_1"); click("key_=")
        assertText("expression", "15")
        click("key_7")
        assertText("expression", "7")
        click("key_AC")
        click("key_.")
        assertText("expression", "0.")
        text("expression", "1÷0")
        click("key_=")
        assertText("preview", "Cannot divide by zero")
        text("expression", "(12.05+0.05)×2")
        click("key_=")
        assertText("expression", "24.2")
        scenario.recreate()
        assertText("expression", "24.2")
        capture("calculator")
        click("history")
        capture("history")
        androidx.test.espresso.Espresso.pressBack()
        instrumentation.waitForIdleSync()
        click("send")
        assertText("conversion_input", "24.2")
        click("category_Length")
        text("conversion_input", "1000")
        assertText("conversion_result", "1")
        click("swap")
        assertText("conversion_result", "1000000")
        scenario.recreate()
        assertText("conversion_input", "1000")
        assertText("conversion_result", "1000000")
        text("conversion_input", "NaN")
        assertText("conversion_result", "—")
        capture("validation")
        text("conversion_input", "1")
        click("category_Area")
        text("bigha_factor", "2500")
        text("biswa_factor", "20")
        scenario.recreate()
        assertText("bigha_factor", "2500")
        assertText("biswa_factor", "20")
        UnitConverter.categories.forEach { group ->
            click("category_${group.name}")
            text("conversion_input", if (group.name == "Temperature") "25" else "1")
            capture(group.name.lowercase())
        }
    }

    @Test fun unitPickerAndRegionalConversion() {
        click("tab_1")
        click("category_Temperature")
        text("conversion_input", "-40")
        assertText("conversion_result", "-40")
        click("from_unit")
        capture("unit-picker")
        onView(withText("Kelvin (K)")).inRoot(isDialog()).perform(click())
        assertText("conversion_result", "—")
        text("conversion_input", "273.15")
        assertText("conversion_result", "32")
        click("category_Area")
        text("conversion_input", "1")
        click("from_unit")
        onView(withText("Bigha (custom region)")).inRoot(isDialog()).perform(click())
        assertText("conversion_result", "—")
        text("bigha_factor", "2500")
        click("to_unit")
        onView(withText("Square metres (m²)")).inRoot(isDialog()).perform(click())
        assertText("conversion_result", "2500")
        text("biswa_factor", "20")
        scenario.onActivity { activity ->
            val field = find(activity.window.decorView, "biswa_factor")!!
            field.requestRectangleOnScreen(Rect(0, 0, field.width, field.height), true)
        }
        capture("regional-area")
    }

    @Test fun historyClearAndLandscape() {
        text("expression", "0.1+0.2")
        click("key_=")
        assertText("expression", "0.3")
        click("history")
        onView(withText("Clear history")).inRoot(isDialog()).perform(click())
        onView(withText("Cancel")).inRoot(isDialog()).perform(click())
        assertTrue(preferences.contains("history"))
        click("history")
        onView(withText("Clear history")).inRoot(isDialog()).perform(click())
        onView(withText("Clear")).inRoot(isDialog()).perform(click())
        assertFalse(preferences.contains("history"))
        try {
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            SystemClock.sleep(1200)
            instrumentation.waitForIdleSync()
            assertText("expression", "0.3")
            capture("landscape")
        } finally {
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            SystemClock.sleep(700)
        }
    }

    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(350)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        assertNotNull(bitmap)
        val directory = File(instrumentation.targetContext.filesDir, "screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
