package com.maulik.wealth

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.res.ColorStateList
import android.text.InputType
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FinanceRecord(
    val id: Long,
    val type: String,
    val name: String,
    val amount: Double,
    val date: String
)

class MainActivity : Activity() {

    private val navy = Color.rgb(25, 35, 75)
    private val purple = Color.rgb(105, 75, 220)
    private val pageBg = Color.rgb(246, 247, 252)
    private val darkText = Color.rgb(35, 39, 55)
    private val muted = Color.rgb(125, 130, 150)
    private val green = Color.rgb(28, 160, 105)
    private val red = Color.rgb(220, 75, 85)
    private val blue = Color.rgb(60, 125, 220)
    private val orange = Color.rgb(235, 145, 55)

    private val records = mutableListOf<FinanceRecord>()
    private var selectedTab = "Home"
    private var hideBalance = false

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var bottomNav: LinearLayout

    private val recordTypes = listOf(
        "Asset", "Debt", "Income", "Expense", "Investment", "Stock"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = navy
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        loadRecords()
        showScreen()
    }

    private fun showScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBg)
            fitsSystemWindows = true
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        val scroll = ScrollView(this).apply {
    isFillViewport = true
    clipToPadding = false
    addView(
        content,
        android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
    )
}

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            elevation = dp(12).toFloat()
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

        val tabs = listOf(
            "Home" to "⌂",
            "Records" to "▤",
            "Invest" to "↗",
            "More" to "•••"
        )

        tabs.forEach { (tab, symbol) ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setOnClickListener {
                    selectedTab = tab
                    showScreen()
                }
            }

            val color = if (selectedTab == tab) purple else muted

            item.addView(
                text(symbol, 22f, color, true).apply {
                    gravity = Gravity.CENTER
                }
            )
            item.addView(
                text(tab, 11f, color, selectedTab == tab).apply {
                    gravity = Gravity.CENTER
                }
            )

            bottomNav.addView(
                item,
                LinearLayout.LayoutParams(0, dp(48), 1f)
            )
        }

        root.addView(
            bottomNav,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.setOnApplyWindowInsetsListener { view, insets ->
            val bottomInset = insets.systemWindowInsetBottom
            bottomNav.setPadding(
                dp(4), dp(8), dp(4), dp(8) + bottomInset
            )
            insets
        }

        setContentView(root)

        when (selectedTab) {
            "Home" -> dashboard()
            "Records" -> recordsScreen()
            "Invest" -> investmentsScreen()
            "More" -> moreScreen()
        }
    }

    private fun dashboard() {
        content.removeAllViews()

        content.addView(
            text("Hello, welcome back 👋", 15f, muted)
        )
        addSpace(content, 4)
        content.addView(
            text("Wealth Ankola", 25f, darkText, true)
        )
        addSpace(content, 18)

        val assets = assetTotal()
        val debts = total("Debt")
        val netWorth = assets - debts

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = gradient(
                intArrayOf(
                    Color.rgb(77, 67, 190),
                    Color.rgb(125, 83, 220)
                ),
                24
            )
        }

        hero.addView(
            text("TOTAL NET WORTH", 12f, Color.WHITE, true)
        )
        addSpace(hero, 10)

        hero.addView(
            text(
                if (hideBalance) "••••••••" else money(netWorth),
                32f,
                Color.WHITE,
                true
            )
        )
        addSpace(hero, 18)

        val heroBottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val balanceInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        balanceInfo.addView(
            text("Assets − Debts", 12f, Color.WHITE)
        )
        balanceInfo.addView(
            text(
                if (hideBalance) "Hidden" else "${money(assets)} − ${money(debts)}",
                13f,
                Color.WHITE,
                true
            )
        )

        heroBottom.addView(
            balanceInfo,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val hideButton = TextView(this).apply {
            text = if (hideBalance) "Show" else "Hide"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = rounded(Color.argb(45, 255, 255, 255), 20)
            setOnClickListener {
                hideBalance = !hideBalance
                showScreen()
            }
        }
        heroBottom.addView(hideButton)
        hero.addView(heroBottom)

        content.addView(
            hero,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 18)
        content.addView(text("Your overview", 18f, darkText, true))
        addSpace(content, 10)

        val summaryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        summaryRow.addView(
            summaryCard(
                "Assets",
                assets,
                green,
                "▣"
            ),
            LinearLayout.LayoutParams(0, dp(112), 1f)
        )
        addSpaceHorizontal(summaryRow, 10)
        summaryRow.addView(
            summaryCard(
                "Debts",
                debts,
                red,
                "▤"
            ),
            LinearLayout.LayoutParams(0, dp(112), 1f)
        )

        content.addView(summaryRow)
        addSpace(content, 20)

        content.addView(text("Quick actions", 18f, darkText, true))
        addSpace(content, 10)

        val actionRows = listOf(
            listOf(
                Triple("＋ Asset", green, "Asset"),
                Triple("＋ Debt", red, "Debt")
            ),
            listOf(
                Triple("＋ Income", blue, "Income"),
                Triple("＋ Expense", orange, "Expense")
            ),
            listOf(
                Triple("＋ Stock", purple, "Stock"),
                Triple("＋ Investment", navy, "Investment")
            )
        )

        actionRows.forEach { actions ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            actions.forEachIndexed { index, action ->
                val btn = TextView(this).apply {
                    text = action.first
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(action.second)
                    background = rounded(Color.WHITE, 16)
                    elevation = dp(2).toFloat()
                    setOnClickListener {
                        showRecordDialog(action.third)
                    }
                }

                row.addView(
                    btn,
                    LinearLayout.LayoutParams(0, dp(48), 1f)
                )
                if (index == 0) addSpaceHorizontal(row, 10)
            }

            content.addView(row)
            addSpace(content, 10)
        }

        addSpace(content, 10)
        content.addView(text("This month", 18f, darkText, true))
        addSpace(content, 10)

        val income = monthTotal("Income")
        val expense = monthTotal("Expense")
        val monthCard = card()

        monthCard.addView(
            infoRow(
                "Income",
                if (hideBalance) "••••••" else money(income),
                green
            )
        )
        monthCard.addView(
            infoRow(
                "Expenses",
                if (hideBalance) "••••••" else money(expense),
                red
            )
        )
        monthCard.addView(
            infoRow(
                "Balance",
                if (hideBalance) "••••••" else money(income - expense),
                purple
            )
        )

        content.addView(monthCard)
        addSpace(content, 16)

        val allRecords = button("View all records", purple) {
            selectedTab = "Records"
            showScreen()
        }
        content.addView(allRecords)
        addSpace(content, 16)
    }

    private fun summaryCard(
        title: String,
        amount: Double,
        color: Int,
        symbol: String
    ): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, 18)
            elevation = dp(2).toFloat()
        }

        box.addView(text("$symbol  $title", 13f, muted, true))
        addSpace(box, 10)
        box.addView(
            text(
                if (hideBalance) "••••••" else money(amount),
                19f,
                color,
                true
            )
        )
        return box
    }

    private fun recordsScreen() {
        content.removeAllViews()

        content.addView(text("Records", 25f, darkText, true))
        addSpace(content, 6)
        content.addView(
            text("All your financial entries in one place", 13f, muted)
        )
        addSpace(content, 16)

        content.addView(
            button("+ Add record", purple) {
                showRecordDialog()
            }
        )
        addSpace(content, 14)

        if (records.isEmpty()) {
            content.addView(emptyMessage("No records added yet."))
            return
        }

        records.sortedByDescending { it.id }.forEach { record ->
            content.addView(recordCard(record))
            addSpace(content, 10)
        }
    }

    private fun investmentsScreen() {
        content.removeAllViews()

        content.addView(text("Investments", 25f, darkText, true))
        addSpace(content, 6)
        content.addView(
            text("Track your investments and stocks", 13f, muted)
        )
        addSpace(content, 16)

        content.addView(
            button("+ Add investment", purple) {
                showRecordDialog("Investment")
            }
        )
        addSpace(content, 10)
        content.addView(
            button("+ Add stock", blue) {
                showRecordDialog("Stock")
            }
        )
        addSpace(content, 18)

        val investmentRecords = records
            .filter { it.type == "Investment" || it.type == "Stock" }
            .sortedByDescending { it.id }

        val totalInvested = investmentRecords.sumOf { it.amount }

        val totalCard = card()
        totalCard.addView(text("Total invested", 13f, muted))
        addSpace(totalCard, 8)
        totalCard.addView(
            text(
                if (hideBalance) "••••••" else money(totalInvested),
                25f,
                purple,
                true
            )
        )
        content.addView(totalCard)
        addSpace(content, 16)

        if (investmentRecords.isEmpty()) {
            content.addView(emptyMessage("No investments or stocks yet."))
        } else {
            investmentRecords.forEach {
                content.addView(recordCard(it))
                addSpace(content, 10)
            }
        }
    }

    private fun moreScreen() {
        content.removeAllViews()

        content.addView(text("More", 25f, darkText, true))
        addSpace(content, 18)

        val settings = card()

        settings.addView(
            text("Settings", 17f, darkText, true)
        )
        addSpace(settings, 12)

        settings.addView(
            button(
                if (hideBalance) "Show balances" else "Hide balances",
                navy
            ) {
                hideBalance = !hideBalance
                showScreen()
            }
        )
        addSpace(settings, 10)

        settings.addView(
            button("Add a record", purple) {
                showRecordDialog()
            }
        )
        addSpace(settings, 10)

        settings.addView(
            button("View all records", blue) {
                selectedTab = "Records"
                showScreen()
            }
        )

        content.addView(settings)
        addSpace(content, 18)

        content.addView(
            text(
                "Wealth Ankola\nPersonal finance tracker",
                13f,
                muted
            ).apply {
                gravity = Gravity.CENTER
            }
        )
    }

    private fun recordCard(record: FinanceRecord): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = rounded(Color.WHITE, 16)
            elevation = dp(1).toFloat()
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        details.addView(text(record.name, 15f, darkText, true))
        addSpace(details, 4)
        details.addView(
            text("${record.type} • ${record.date}", 12f, muted)
        )

        top.addView(
            details,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val amountColor = when (record.type) {
            "Income", "Asset", "Investment", "Stock" -> green
            "Debt", "Expense" -> red
            else -> darkText
        }

        top.addView(
            text(
                if (hideBalance) "••••" else money(record.amount),
                15f,
                amountColor,
                true
            )
        )

        box.addView(top)
        addSpace(box, 10)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val edit = TextView(this).apply {
            text = "Edit"
            textSize = 13f
            setTextColor(purple)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { showRecordDialog(record.type, record) }
        }

        val delete = TextView(this).apply {
            text = "Delete"
            textSize = 13f
            setTextColor(red)
            setPadding(dp(12), dp(6), dp(4), dp(6))
            setOnClickListener { confirmDelete(record) }
        }

        actions.addView(edit)
        actions.addView(delete)
        box.addView(actions)

        return box
    }

    private fun showRecordDialog(
        defaultType: String? = null,
        existing: FinanceRecord? = null
    ) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }

        val typeSpinner = Spinner(this)
        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            recordTypes
        )
        typeSpinner.adapter = typeAdapter

        val initialType = existing?.type ?: defaultType
        val selectedIndex = recordTypes.indexOf(initialType)
        if (selectedIndex >= 0) typeSpinner.setSelection(selectedIndex)

        val nameInput = EditText(this).apply {
            hint = "Name or description"
            setSingleLine(true)
            setText(existing?.name ?: "")
        }

        val amountInput = EditText(this).apply {
            hint = "Amount"
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            setText(existing?.amount?.toString() ?: "")
        }

        layout.addView(labelView("Type"))
        layout.addView(typeSpinner)
        addSpace(layout, 10)
        layout.addView(labelView("Name"))
        layout.addView(nameInput)
        addSpace(layout, 8)
        layout.addView(labelView("Amount"))
        layout.addView(amountInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add record" else "Edit record")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(
                if (existing == null) "Save" else "Update",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString().trim()
                val amountText = amountInput.text.toString().trim()
                val amount = amountText.toDoubleOrNull()

                if (name.isEmpty()) {
                    nameInput.error = "Enter a name"
                    return@setOnClickListener
                }

                if (amount == null || amount <= 0.0) {
                    amountInput.error = "Enter a valid amount"
                    return@setOnClickListener
                }

                val type = typeSpinner.selectedItem.toString()
                val date = existing?.date ?: SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                ).format(Date())

                if (existing == null) {
                    records.add(
                        FinanceRecord(
                            System.currentTimeMillis(),
                            type,
                            name,
                            amount,
                            date
                        )
                    )
                } else {
                    val index = records.indexOfFirst { it.id == existing.id }
                    if (index >= 0) {
                        records[index] = existing.copy(
                            type = type,
                            name = name,
                            amount = amount
                        )
                    }
                }

                saveRecords()
                dialog.dismiss()
                showScreen()
            }
        }

        dialog.show()
    }

    private fun confirmDelete(record: FinanceRecord) {
        AlertDialog.Builder(this)
            .setTitle("Delete record?")
            .setMessage("Delete \"${record.name}\"?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                records.removeAll { it.id == record.id }
                saveRecords()
                showScreen()
            }
            .show()
    }

    private fun infoRow(
        title: String,
        value: String,
        color: Int
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }

        row.addView(
            text(title, 14f, darkText),
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        row.addView(text(value, 14f, color, true))
        return row
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(Color.WHITE, 18)
            elevation = dp(2).toFloat()
        }
    }

    private fun button(
        title: String,
        color: Int,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = title
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = rounded(color, 14)
            setOnClickListener { action() }
        }
    }

    private fun labelView(value: String): TextView {
        return text(value, 13f, muted, true)
    }

    private fun emptyMessage(message: String): View {
        return TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(30), dp(20), dp(30))
            background = rounded(Color.WHITE, 16)
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun gradient(
        colors: IntArray,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            colors
        ).apply {
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun addSpace(
        parent: LinearLayout,
        height: Int
    ) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

    private fun addSpaceHorizontal(
        parent: LinearLayout,
        width: Int
    ) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(
                dp(width),
                1
            )
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun money(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )
        format.maximumFractionDigits = 0
        format.minimumFractionDigits = 0
        return format.format(value)
    }

    private fun total(type: String): Double {
        return records
            .filter { it.type == type }
            .sumOf { it.amount }
    }

    private fun assetTotal(): Double {
        return records
            .filter {
                it.type == "Asset" ||
                    it.type == "Stock" ||
                    it.type == "Investment"
            }
            .sumOf { it.amount }
    }

    private fun monthTotal(type: String): Double {
        val month = SimpleDateFormat(
            "MM yyyy",
            Locale.getDefault()
        ).format(Date())

        return records
            .filter { it.type == type }
            .filter {
                try {
                    val parsed = SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.getDefault()
                    ).parse(it.date)
                    parsed != null &&
                        SimpleDateFormat(
                            "MM yyyy",
                            Locale.getDefault()
                        ).format(parsed) == month
                } catch (_: Exception) {
                    false
                }
            }
            .sumOf { it.amount }
    }

    private fun loadRecords() {
        records.clear()

        val prefs = getSharedPreferences("wealth", Context.MODE_PRIVATE)
        val json = prefs.getString("records", null) ?: return

        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                records.add(
                    FinanceRecord(
                        item.optLong("id", System.currentTimeMillis() + i),
                        item.optString("type", "Asset"),
                        item.optString("name", ""),
                        item.optDouble("amount", 0.0),
                        item.optString("date", "")
                    )
                )
            }
        } catch (_: Exception) {
            records.clear()
        }
    }

    private fun saveRecords() {
        val array = JSONArray()

        records.forEach { record ->
            val item = JSONObject().apply {
                put("id", record.id)
                put("type", record.type)
                put("name", record.name)
                put("amount", record.amount)
                put("date", record.date)
            }
            array.put(item)
        }

        getSharedPreferences("wealth", Context.MODE_PRIVATE)
            .edit()
            .putString("records", array.toString())
            .apply()
    }
}
