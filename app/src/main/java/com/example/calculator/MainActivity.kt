package com.example.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray

class MainActivity : AppCompatActivity() {
    private val background = Color.rgb(19, 20, 23)
    private val panel = Color.rgb(31, 33, 38)
    private val secondary = Color.rgb(159, 163, 173)
    private val orange = Color.rgb(255, 159, 10)
    private val white = Color.rgb(245, 245, 247)
    private val errorColor = Color.rgb(255, 151, 151)
    private var mode = 0
    private var category = 0
    private var from = 0
    private var to = 1
    private var expression = ""
    private var input = "1"
    private var bigha = ""
    private var biswa = ""
    private var calculated = false
    private var answer = ""
    private var conversion = ""
    private var mutating = false
    private lateinit var content: LinearLayout
    private lateinit var expressionField: EditText
    private lateinit var preview: TextView
    private lateinit var result: TextView
    private lateinit var inputField: EditText
    private lateinit var fromButton: Button
    private lateinit var toButton: Button
    private lateinit var status: TextView
    private val preferences by lazy { getSharedPreferences("calc_and_convert", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = savedInstanceState?.getInt("mode") ?: 0
        category = (savedInstanceState?.getInt("category") ?: 0).coerceIn(UnitConverter.categories.indices)
        from = savedInstanceState?.getInt("from") ?: 0
        to = savedInstanceState?.getInt("to") ?: 1
        expression = savedInstanceState?.getString("expression") ?: ""
        input = savedInstanceState?.getString("input") ?: "1"
        bigha = savedInstanceState?.getString("bigha") ?: preferences.getString("bigha", "").orEmpty()
        biswa = savedInstanceState?.getString("biswa") ?: preferences.getString("biswa", "").orEmpty()
        calculated = savedInstanceState?.getBoolean("calculated") ?: false
        answer = savedInstanceState?.getString("answer") ?: ""
        window.statusBarColor = background
        window.navigationBarColor = background
        window.decorView.systemUiVisibility = 0
        showApp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("mode", mode)
        outState.putInt("category", category)
        outState.putInt("from", from)
        outState.putInt("to", to)
        outState.putString("expression", expression)
        outState.putString("input", input)
        outState.putString("bigha", bigha)
        outState.putString("biswa", biswa)
        outState.putBoolean("calculated", calculated)
        outState.putString("answer", answer)
        super.onSaveInstanceState(outState)
    }

    private fun showApp() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(this@MainActivity.background)
            setPadding(dp(20), dp(8), dp(20), dp(12))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(dp(20) + bars.left, dp(8) + bars.top, dp(20) + bars.right, dp(12) + bars.bottom)
            insets
        }
        setContentView(root)
        root.addView(label("Calc & Convert", 25, white, true), LinearLayout.LayoutParams(-1, dp(56)))
        val tabs = row()
        listOf("Calculator", "Converter").forEachIndexed { index, title ->
            tabs.addView(button(title, "tab_$index", if (mode == index) orange else panel, if (mode == index) background else white) {
                hideKeyboard()
                mode = index
                showApp()
            }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), 0, dp(3), dp(10)) })
        }
        root.addView(tabs)
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        if (mode == 0) showCalculator() else showConverter()
    }

    private fun showCalculator() {
        val display = column(panel).apply { setPadding(dp(18), dp(12), dp(18), dp(12)) }
        val displayHeader = row()
        displayHeader.addView(label("CALCULATION", 11, secondary), LinearLayout.LayoutParams(0, dp(40), 1f))
        displayHeader.addView(button("History", "history", panel, orange) { showHistory() }, LinearLayout.LayoutParams(dp(96), dp(44)))
        display.addView(displayHeader)
        expressionField = field(expression, "expression", "0").apply {
            textSize = 32f
            gravity = Gravity.END
            showSoftInputOnFocus = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            filters = arrayOf(InputFilter.LengthFilter(256), InputFilter { source, start, end, _, _, _ ->
                val part = source.subSequence(start, end)
                if (part.all { it in "0123456789.+-*/×÷−()%^Ee " }) null else ""
            })
            setSelection(text.length)
        }
        expressionField.addTextChangedListener(watcher {
            expression = expressionField.text.toString()
            if (!mutating) calculated = false
            updatePreview()
        })
        display.addView(expressionField, LinearLayout.LayoutParams(-1, dp(80)))
        preview = label("", 18, secondary).apply { gravity = Gravity.END; minHeight = dp(42); tag = "preview" }
        display.addView(preview)
        content.addView(display, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val actions = row()
        actions.addView(button("Copy result", "copy", panel, orange) { copyResult() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        actions.addView(button("Use in converter →", "send", panel, orange) { sendToConverter() }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(8) })
        content.addView(actions)
        val keys = listOf(listOf("AC", "(", ")", "⌫"), listOf("±", "%", "^", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("0", ".", "00", "="))
        keys.forEach { values ->
            val line = row()
            values.forEach { key ->
                val operator = key in listOf("÷", "×", "−", "+", "=")
                val utility = key in listOf("AC", "(", ")", "⌫", "±", "%", "^")
                line.addView(button(key, "key_$key", if (operator) orange else if (utility) Color.rgb(57, 60, 67) else panel, if (operator) background else white) { press(key) }.apply { textSize = 23f }, LinearLayout.LayoutParams(0, dp(56), 1f).apply { setMargins(dp(3), dp(4), dp(3), dp(3)) })
            }
            content.addView(line)
        }
        updatePreview()
    }

    private fun press(key: String) {
        when (key) {
            "AC" -> { calculated = false; answer = ""; replaceExpression("") }
            "⌫" -> {
                calculated = false
                val start = expressionField.selectionStart.coerceAtLeast(0)
                val end = expressionField.selectionEnd.coerceAtLeast(start)
                if (start != end) expressionField.text.delete(start, end) else if (start > 0) expressionField.text.delete(start - 1, start)
            }
            "=" -> {
                try {
                    answer = CalculatorEngine.format(CalculatorEngine.calculate(expression))
                    saveHistory(expression, answer)
                    replaceExpression(answer)
                    calculated = true
                    preview.text = "= $answer"
                    preview.setTextColor(orange)
                } catch (e: IllegalArgumentException) { showCalculationError(e.message) }
                catch (_: ArithmeticException) { showCalculationError("Number is outside the supported range") }
            }
            "±" -> {
                if (expression.length > 253) showCalculationError("Enter a shorter calculation")
                else if (expression.isNotBlank()) replaceExpression("-($expression)") else insert("−")
                calculated = false
            }
            "." -> {
                if (calculated) replaceExpression("")
                calculated = false
                val position = expressionField.selectionStart.coerceAtLeast(0)
                val left = expression.take(position)
                val right = expression.drop(position)
                val current = left.takeLastWhile { it.isDigit() || it == '.' } + right.takeWhile { it.isDigit() || it == '.' }
                if (!current.contains('.')) insert(if (left.lastOrNull()?.isDigit() == true) "." else "0.")
            }
            else -> {
                if (calculated && (key.first().isDigit() || key == "(")) replaceExpression("")
                calculated = false
                insert(key)
            }
        }
    }

    private fun insert(value: String) {
        val start = expressionField.selectionStart.coerceAtLeast(0)
        val end = expressionField.selectionEnd.coerceAtLeast(start)
        expressionField.text.replace(start, end, value)
    }

    private fun replaceExpression(value: String) {
        mutating = true
        expressionField.setText(value)
        expressionField.setSelection(expressionField.text.length)
        mutating = false
    }

    private fun updatePreview() {
        if (!::preview.isInitialized) return
        preview.setTextColor(secondary)
        preview.text = if (expression.isBlank()) "Ready when you are" else try { "= ${CalculatorEngine.format(CalculatorEngine.calculate(expression))}" } catch (_: Exception) { "Complete your calculation" }
    }

    private fun showCalculationError(message: String?) {
        preview.text = message ?: "Check the calculation"
        preview.setTextColor(errorColor)
    }

    private fun sendToConverter() {
        try {
            input = CalculatorEngine.format(CalculatorEngine.calculate(expression))
            hideKeyboard()
            mode = 1
            showApp()
        } catch (e: Exception) { showCalculationError(e.message ?: "Enter a valid calculation first") }
    }

    private fun copyResult() {
        val value = if (mode == 0) try { CalculatorEngine.format(CalculatorEngine.calculate(expression)) } catch (e: Exception) { showCalculationError(e.message); return } else conversion
        if (value.isBlank()) return
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Result", value))
    }

    private fun showConverter() {
        val categoryScroll = HorizontalScrollView(this).apply {
            tag = "category_strip"
            isHorizontalScrollBarEnabled = false
        }
        val categories = row()
        var selected: Button? = null
        UnitConverter.categories.forEachIndexed { index, group ->
            val item = button(group.name, "category_${group.name}", if (category == index) orange else panel, if (category == index) background else orange) {
                input = inputField.text.toString()
                category = index
                from = 0
                to = 1
                hideKeyboard()
                showApp()
            }
            categories.addView(item, LinearLayout.LayoutParams(-2, dp(44)).apply { rightMargin = dp(8) })
            if (category == index) selected = item
        }
        categoryScroll.addView(categories)
        content.addView(categoryScroll)
        val slider = SeekBar(this).apply {
            tag = "category_slider"
            contentDescription = "Browse conversion categories from Angle to Weight"
            progressTintList = android.content.res.ColorStateList.valueOf(orange)
            thumbTintList = android.content.res.ColorStateList.valueOf(orange)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(panel)
            splitTrack = false
        }
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) categoryScroll.scrollTo(progress, 0)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { seekBar?.parent?.requestDisallowInterceptTouchEvent(true) }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { seekBar?.parent?.requestDisallowInterceptTouchEvent(false) }
        })
        categoryScroll.setOnScrollChangeListener { _, x, _, _, _ -> slider.progress = x }
        categoryScroll.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            slider.max = (categories.width - categoryScroll.width).coerceAtLeast(0)
            slider.isEnabled = slider.max > 0
            slider.progress = categoryScroll.scrollX
        }
        content.addView(slider, LinearLayout.LayoutParams(-1, dp(48)))
        categoryScroll.post {
            slider.max = (categories.width - categoryScroll.width).coerceAtLeast(0)
            slider.isEnabled = slider.max > 0
            selected?.let { categoryScroll.scrollTo((it.left - dp(16)).coerceIn(0, slider.max), 0) }
            slider.progress = categoryScroll.scrollX
        }
        val group = UnitConverter.categories[category]
        from = from.coerceIn(group.units.indices)
        to = to.coerceIn(group.units.indices)
        content.addView(label(group.name, 32, white, true).apply { setPadding(0, dp(24), 0, dp(4)) })
        val source = column(panel)
        source.addView(label("FROM", 11, secondary))
        fromButton = button(group.units[from].name + " ▾", "from_unit", panel, orange) { chooseUnit(true) }.apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL; textSize = 18f }
        source.addView(fromButton, LinearLayout.LayoutParams(-1, dp(56)))
        inputField = field(input, "conversion_input", "Enter a value").apply {
            textSize = 30f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            filters = arrayOf(InputFilter.LengthFilter(80))
        }
        source.addView(inputField, LinearLayout.LayoutParams(-1, dp(68)))
        content.addView(source)
        content.addView(button("⇅  Swap units", "swap", background, orange) {
            val previous = from
            from = to
            to = previous
            fromButton.text = group.units[from].name + " ▾"
            toButton.text = group.units[to].name + " ▾"
            updateConversion()
        }, LinearLayout.LayoutParams(-1, dp(58)))
        val target = column(panel)
        target.addView(label("TO", 11, secondary))
        toButton = button(group.units[to].name + " ▾", "to_unit", panel, orange) { chooseUnit(false) }.apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL; textSize = 18f }
        target.addView(toButton, LinearLayout.LayoutParams(-1, dp(56)))
        result = label("", 36, white, true).apply { tag = "conversion_result"; setTextIsSelectable(true); minHeight = dp(70) }
        target.addView(result)
        target.addView(button("Copy result", "copy_conversion", panel, orange) { copyResult() }, LinearLayout.LayoutParams(-1, dp(48)))
        content.addView(target)
        status = label("", 14, errorColor).apply { tag = "conversion_status"; setPadding(0, dp(12), 0, dp(8)); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        content.addView(status)
        if (group.name == "Area") {
            content.addView(label("REGIONAL LAND UNITS", 11, orange))
            val areaField = field(bigha, "bigha_factor", "Square metres per Bigha")
            val countField = field(biswa, "biswa_factor", "Biswa per Bigha")
            content.addView(label("Square metres per Bigha", 13, secondary).apply { setPadding(0, dp(12), 0, 0) })
            content.addView(areaField, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(8) })
            content.addView(label("Biswa per Bigha", 13, secondary).apply { setPadding(0, dp(12), 0, 0) })
            content.addView(countField, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(8) })
            areaField.addTextChangedListener(watcher { bigha = areaField.text.toString(); preferences.edit().putString("bigha", bigha).apply(); updateConversion() })
            countField.addTextChangedListener(watcher { biswa = countField.text.toString(); preferences.edit().putString("biswa", biswa).apply(); updateConversion() })
        }
        inputField.addTextChangedListener(watcher { input = inputField.text.toString(); updateConversion() })
        updateConversion()
    }

    private fun chooseUnit(source: Boolean) {
        hideKeyboard()
        val group = UnitConverter.categories[category]
        AlertDialog.Builder(this).setTitle(if (source) "Convert from" else "Convert to")
            .setSingleChoiceItems(group.units.map { it.name }.toTypedArray(), if (source) from else to) { dialog, index ->
                if (source) { from = index; fromButton.text = group.units[index].name + " ▾" } else { to = index; toButton.text = group.units[index].name + " ▾" }
                dialog.dismiss()
                updateConversion()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun updateConversion() {
        conversion = ""
        if (input.isBlank()) { result.text = "—"; status.text = "Enter a value to convert"; return }
        try {
            conversion = UnitConverter.format(UnitConverter.convert(input, category, from, to, bigha, biswa))
            result.text = conversion
            status.text = ""
        } catch (e: IllegalArgumentException) { result.text = "—"; status.text = e.message ?: "Check the input" }
        catch (_: ArithmeticException) { result.text = "—"; status.text = "Result is outside the supported range" }
    }

    private fun history(): List<Pair<String, String>> = try {
        val raw = preferences.getString("history", "[]").orEmpty()
        require(raw.length <= 50000)
        val array = JSONArray(raw)
        (0 until minOf(array.length(), 30)).map { array.getJSONArray(it) }.map { it.getString(0).take(256) to it.getString(1).take(350) }
    } catch (_: Exception) { emptyList() }

    private fun saveHistory(value: String, result: String) {
        val entries = (listOf(value to result) + history()).distinct().take(30)
        val array = JSONArray()
        entries.forEach { array.put(JSONArray().put(it.first).put(it.second)) }
        preferences.edit().putString("history", array.toString()).apply()
    }

    private fun showHistory() {
        val entries = history()
        val dialog = AlertDialog.Builder(this).setTitle("Recent calculations")
        if (entries.isEmpty()) dialog.setMessage("Your completed calculations will appear here.")
        else dialog.setItems(entries.map { "${it.first} = ${it.second}" }.toTypedArray()) { _, index ->
            replaceExpression(entries[index].second)
            calculated = true
        }
        dialog.setNegativeButton("Close", null)
        if (entries.isNotEmpty()) dialog.setNeutralButton("Clear history") { _, _ ->
            AlertDialog.Builder(this).setTitle("Clear calculation history?").setMessage("This removes the saved calculations on this device.")
                .setPositiveButton("Clear") { _, _ -> preferences.edit().remove("history").apply() }.setNegativeButton("Cancel", null).show()
        }
        dialog.show()
    }

    private fun hideKeyboard() {
        currentFocus?.let { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.windowToken, 0); it.clearFocus() }
    }

    private fun watcher(action: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = action()
        override fun afterTextChanged(s: Editable?) = Unit
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun column(color: Int) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = shape(color, 22); setPadding(dp(16), dp(14), dp(16), dp(14)) }
    private fun shape(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER_VERTICAL
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun button(value: String, id: String, color: Int, textColor: Int = white, action: () -> Unit) = Button(this).apply {
        text = value
        tag = id
        isAllCaps = false
        textSize = 14f
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(12), 0, dp(12), 0)
        setTextColor(textColor)
        background = shape(color, 18)
        contentDescription = when (value) { "⌫" -> "Backspace"; "±" -> "Change sign"; "AC" -> "Clear calculation"; else -> value }
        setOnClickListener { action() }
    }
    private fun field(value: String, id: String, placeholder: String) = EditText(this).apply {
        tag = id
        setText(value)
        hint = placeholder
        contentDescription = if (id == "expression") "Calculation expression" else placeholder
        setTextColor(white)
        setHintTextColor(secondary)
        backgroundTintList = android.content.res.ColorStateList.valueOf(orange)
        setSingleLine(true)
        setPadding(dp(4), dp(4), dp(4), dp(4))
        filters = arrayOf(InputFilter.LengthFilter(80))
        isSaveEnabled = false
    }
}
