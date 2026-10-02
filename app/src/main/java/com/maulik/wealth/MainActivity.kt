
package com.maulik.wealth

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FinanceRecord(
    val id: Long,
    val type: String,
    val name: String,
    val amount: Double,
    val date: Long
)

class MainActivity : Activity() {
    private val prefs by lazy {
        getSharedPreferences("wealth", Context.MODE_PRIVATE)
    }

    private val records = mutableListOf<FinanceRecord>()
    private lateinit var container: LinearLayout
    private var selectedTab = "Home"
    private var hideBalance = false

    private val navy = Color.rgb(23, 37, 84)
    private val purple = Color.rgb(91, 66, 190)
    private val pageBg = Color.rgb(245, 247, 252)
    private val darkText = Color.rgb(31, 41, 55)
    private val muted = Color.rgb(107, 114, 128)
    private val green = Color.rgb(5, 150, 105)
    private val red = Color.rgb(220, 38, 38)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadRecords()
        migrateOldData()
        showScreen()
    }

    private fun loadRecords() {
        records.clear()
        val raw = prefs.getString("records_v2", null) ?: return
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                records.add(
                    FinanceRecord(
                        item.getLong("id"),
                        item.getString("type"),
                        item.getString("name"),
                        item.getDouble("amount"),
                        item.getLong("date")
                    )
                )
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Could not read records", Toast.LENGTH_LONG).show()
        }
    }

    private fun migrateOldData() {
        if (prefs.getBoolean("migration_v2_done", false)) return

        val oldAssets = prefs.getFloat("assets", 0f).toDouble()
        val oldDebts = prefs.getFloat("debts", 0f).toDouble()
        val now = System.currentTimeMillis()

        if (oldAssets > 0) {
            records.add(FinanceRecord(now, "Asset", "Previous balance", oldAssets, now))
        }
        if (oldDebts > 0) {
            records.add(FinanceRecord(now + 1, "Debt", "Previous balance", oldDebts, now))
        }

        saveRecords()
        prefs.edit().putBoolean("migration_v2_done", true).apply()
    }

    private fun saveRecords() {
        val array = JSONArray()
        records.forEach {
            val item = JSONObject()
            item.put("id", it.id)
            item.put("type", it.type)
            item.put("name", it.name)
            item.put("amount", it.amount)
            item.put("date", it.date)
            array.put(item)
        }
        prefs.edit().putString("records_v2", array.toString()).apply()
    }

    private fun money(value: Double): String =
        "₹" + String.format(Locale("en", "IN"), "%,.0f", value)

    private fun total(type: String): Double =
        records.filter { it.type == type }.sumOf { it.amount }

    private fun assetTotal(): Double =
        records.filter { it.type == "Asset" || it.type == "Stock" || it.type == "Investment" }
            .sumOf { it.amount }

    private fun monthStart(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun rounded(color: Int, radius: Float = 22f): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }

    private fun gradient(): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(navy, purple)
        ).apply { cornerRadius = 30f }

    private fun text(
        value: String,
        size: Float,
        color: Int = darkText,
        bold: Boolean = false
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun button(
        label: String,
        color: Int,
        onClick: () -> Unit
    ): TextView = TextView(this).apply {
        text = label
        textSize = 14f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setPadding(12, 14, 12, 14)
        background = rounded(color, 18f)
        setOnClickListener { onClick() }
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(16, 16, 16, 16)
        background = rounded(Color.WHITE, 24f)
        elevation = 2f
    }

    private fun addSpace(parent: LinearLayout, height: Int = 12) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, height))
    }

    private fun showScreen() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBg)
        }

        val scroll = ScrollView(this)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
        }
        scroll.addView(container)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(4, 8, 4, 8)
            setBackgroundColor(Color.WHITE)
        }

        listOf("Home", "Records", "Invest", "More").forEach { tab ->
            val item = TextView(this).apply {
                text = when (tab) {
                    "Home" -> "⌂\nHome"
                    "Records" -> "⇄\nRecords"
                    "Invest" -> "▥\nInvest"
                    else -> "☰\nMore"
                }
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(if (selectedTab == tab) purple else muted)
                setPadding(4, 6, 4, 6)
                setOnClickListener {
                    selectedTab = tab
                    showScreen()
                }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }

        outer.addView(nav)
        setContentView(outer)

        when (selectedTab) {
            "Home" -> dashboard()
            "Records" -> recordsScreen()
            "Invest" -> investmentsScreen()
            else -> moreScreen()
        }
    }

    private fun dashboard() {
    // Dashboard heading
    container.addView(text("Good day, Maulik 👋", 16f, muted))
    addSpace(container, 4)
    container.addView(text("Your money overview", 23f, navy, true))
    addSpace(container, 16)

    // Total Net Worth card
    val netWorth = assetTotal() - total("Debt")

    val hero = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(24, 26, 24, 26)
        minimumHeight = 175
        background = gradient()
        elevation = 4f
    }

    val heroTop = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    heroTop.addView(
        text("TOTAL NET WORTH", 15f, Color.LTGRAY, true),
        LinearLayout.LayoutParams(0, -2, 1f)
    )

    val eye = TextView(this).apply {
        text = if (hideBalance) "Show" else "Hide"
        textSize = 14f
        setTextColor(Color.WHITE)
        setPadding(12, 6, 12, 6)
        setOnClickListener {
            hideBalance = !hideBalance
            showScreen()
        }
    }

    heroTop.addView(eye)
    hero.addView(heroTop)
    addSpace(hero, 16)

    hero.addView(
        text(
            if (hideBalance) "₹ ••••••••" else money(netWorth),
            34f,
            Color.WHITE,
            true
        )
    )

    addSpace(hero, 12)
    hero.addView(text("Assets minus debts", 15f, Color.LTGRAY))
    container.addView(hero)
    addSpace(container, 18)

    // Assets and debts
    container.addView(text("Your finances", 20f, navy, true))
    addSpace(container, 10)

    val summaryRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    summaryRow.addView(
        summaryCard(
            "Total Assets",
            assetTotal(),
            Color.rgb(220, 252, 231),
            Color.rgb(22, 101, 52)
        ),
        LinearLayout.LayoutParams(0, -2, 1f)
    )

    summaryRow.addView(View(this), LinearLayout.LayoutParams(10, 1))

    summaryRow.addView(
        summaryCard(
            "Total Debts",
            total("Debt"),
            Color.rgb(254, 226, 226),
            Color.rgb(185, 28, 28)
        ),
        LinearLayout.LayoutParams(0, -2, 1f)
    )

    container.addView(summaryRow)
    addSpace(container, 20)

    // Quick Actions
    container.addView(text("Quick Actions", 20f, navy, true))
    addSpace(container, 10)

    val actionRow1 = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    actionRow1.addView(
        button("＋  Add Asset", Color.rgb(37, 99, 235)) {
            showRecordDialog("Asset")
        },
        LinearLayout.LayoutParams(0, -2, 1f)
    )

    actionRow1.addView(View(this), LinearLayout.LayoutParams(10, 1))

    actionRow1.addView(
        button("−  Add Debt", Color.rgb(190, 24, 93)) {
            showRecordDialog("Debt")
        },
        LinearLayout.LayoutParams(0, -2, 1f)
    )

    container.addView(actionRow1)
    addSpace(container, 10)

    val actionRow2 = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    actionRow2.addView(
        button("↗  Add Income", green) {
            showRecordDialog("Income")
        },
        LinearLayout.LayoutParams(0, -2, 1f)
    )

    actionRow2.addView(View(this), LinearLayout.LayoutParams(10, 1))

    actionRow2.addView(
        button("↘  Add Expense", Color.rgb(234, 88, 12)) {
            showRecordDialog("Expense")
        },
        LinearLayout.LayoutParams(0, -2, 1f)
    )

    container.addView(actionRow2)
    addSpace(container, 20)

    // Monthly Overview
    container.addView(text("Monthly Overview", 20f, navy, true))
    addSpace(container, 10)

    val monthly = records.filter { it.date >= monthStart() }
    val income = monthly.filter { it.type == "Income" }.sumOf { it.amount }
    val expenses = monthly.filter { it.type == "Expense" }.sumOf { it.amount }
    val balance = income - expenses

    val monthCard = card().apply {
        setPadding(18, 18, 18, 18)
    }

    fun addMonthlyRow(label: String, value: Double, color: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        row.addView(
            text(label, 15f, muted),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        row.addView(
            text(
                if (hideBalance) "••••••" else money(value),
                18f,
                color,
                true
            )
        )

        monthCard.addView(row)
    }

    addMonthlyRow("Income", income, green)
    addSpace(monthCard, 16)
    addMonthlyRow("Expenses", expenses, red)

    val separator = View(this).apply {
        setBackgroundColor(Color.rgb(229, 231, 235))
    }
    val separatorParams = LinearLayout.LayoutParams(-1, 1)
    separatorParams.topMargin = 14
    separatorParams.bottomMargin = 14
    monthCard.addView(separator, separatorParams)

    addMonthlyRow("Monthly Balance", balance, navy)

    container.addView(monthCard)
    addSpace(container, 20)

    // Investment Summary
    val stockValue = total("Stock")
    val otherInvestment = total("Investment")
    val investmentValue = stockValue + otherInvestment

    container.addView(text("Investment Summary", 20f, navy, true))
    addSpace(container, 10)

    val investmentCard = card().apply {
        setPadding(18, 18, 18, 18)
        setOnClickListener {
            selectedTab = "Invest"
            showScreen()
        }
    }

    investmentCard.addView(
        text("Total tracked investment value", 14f, muted)
    )
    addSpace(investmentCard, 8)

    investmentCard.addView(
        text(
            if (hideBalance) "••••••" else money(investmentValue),
            25f,
            purple,
            true
        )
    )

    addSpace(investmentCard, 12)
    investmentCard.addView(
        text("Stocks: ${if (hideBalance) "••••••" else money(stockValue)}", 14f, darkText)
    )
    addSpace(investmentCard, 6)
    investmentCard.addView(
        text("Other investments: ${if (hideBalance) "••••••" else money(otherInvestment)}", 14f, darkText)
    )

    container.addView(investmentCard)
    addSpace(container, 20)

    // Recent Records
    val recent = records.sortedByDescending { it.date }.take(3)

    val recentHeading = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    recentHeading.addView(
        text("Recent Records", 20f, navy, true),
        LinearLayout.LayoutParams(0, -2, 1f)
    )

    val seeAll = TextView(this).apply {
        text = "See all  →"
        textSize = 14f
        setTextColor(purple)
        setOnClickListener {
            selectedTab = "Records"
            showScreen()
        }
    }

    recentHeading.addView(seeAll)
    container.addView(recentHeading)
    addSpace(container, 10)

    val recentCard = card().apply {
        setPadding(16, 8, 16, 8)
    }

    if (recent.isEmpty()) {
        recentCard.addView(text("No records yet.", 14f, muted))
    } else {
        recent.forEachIndexed { index, record ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 12, 0, 12)
            }

            val left = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            left.addView(text(record.name, 15f, navy, true))
            addSpace(left, 4)

            val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(record.date))

            left.addView(
                text("${record.type} • $date", 12f, muted)
            )

            row.addView(left, LinearLayout.LayoutParams(0, -2, 1f))

            val amountColor =
                if (record.type == "Debt" || record.type == "Expense") red
                else green

            row.addView(
                text(
                    if (hideBalance) "••••••" else money(record.amount),
                    15f,
                    amountColor,
                    true
                )
            )

            recentCard.addView(row)

            if (index < recent.lastIndex) {
                val line = View(this).apply {
                    setBackgroundColor(Color.rgb(229, 231, 235))
                }
                recentCard.addView(
                    line,
                    LinearLayout.LayoutParams(-1, 1)
                )
            }
        }
    }

    container.addView(recentCard)
    addSpace(container, 16)

    container.addView(button("View All Records  →", navy) {
        selectedTab = "Records"
        showScreen()
    })
}
        top.addView(eye)
        hero.addView(top)
        addSpace(hero, 8)
        hero.addView(text(if (hideBalance) "₹ ••••••••" else money(netWorth),
            30f, Color.WHITE, true))
        addSpace(hero, 8)
        hero.addView(text("Assets minus debts", 13f, Color.LTGRAY))
        container.addView(hero)
        addSpace(container, 18)

        container.addView(text("Your finances", 19f, navy, true))
        addSpace(container, 10)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(summaryCard("Assets", assetTotal(), Color.rgb(224, 242, 254),
            Color.rgb(3, 105, 161)), LinearLayout.LayoutParams(0, -2, 1f))
        val gap = View(this)
        row.addView(gap, LinearLayout.LayoutParams(10, 1))
        row.addView(summaryCard("Debts", total("Debt"), Color.rgb(252, 231, 243),
            Color.rgb(190, 24, 93)), LinearLayout.LayoutParams(0, -2, 1f))
        container.addView(row)
        addSpace(container, 18)

        container.addView(text("Quick actions", 19f, navy, true))
        addSpace(container, 10)

        val actions1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions1.addView(button("＋ Asset", Color.rgb(37, 99, 235)) {
            showRecordDialog("Asset")
        }, LinearLayout.LayoutParams(0, -2, 1f))
        actions1.addView(View(this), LinearLayout.LayoutParams(8, 1))
        actions1.addView(button("＋ Debt", Color.rgb(190, 24, 93)) {
            showRecordDialog("Debt")
        }, LinearLayout.LayoutParams(0, -2, 1f))
        container.addView(actions1)
        addSpace(container, 8)

        val actions2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions2.addView(button("＋ Income", green) {
            showRecordDialog("Income")
        }, LinearLayout.LayoutParams(0, -2, 1f))
        actions2.addView(View(this), LinearLayout.LayoutParams(8, 1))
        actions2.addView(button("＋ Expense", Color.rgb(234, 88, 12)) {
            showRecordDialog("Expense")
        }, LinearLayout.LayoutParams(0, -2, 1f))
        container.addView(actions2)
        addSpace(container, 18)

        container.addView(text("This month", 19f, navy, true))
        addSpace(container, 10)
        val monthly = records.filter { it.date >= monthStart() }
        val income = monthly.filter { it.type == "Income" }.sumOf { it.amount }
        val expenses = monthly.filter { it.type == "Expense" }.sumOf { it.amount }

        val monthCard = card()
        monthCard.addView(text("Income", 14f, muted))
        monthCard.addView(text(money(income), 20f, green, true))
        addSpace(monthCard, 12)
        monthCard.addView(text("Expenses", 14f, muted))
        monthCard.addView(text(money(expenses), 20f, red, true))
        addSpace(monthCard, 12)
        monthCard.addView(text("Monthly balance", 14f, muted))
        monthCard.addView(text(money(income - expenses), 20f, navy, true))
        container.addView(monthCard)
        addSpace(container, 16)

        container.addView(button("View all records  →", navy) {
            selectedTab = "Records"
            showScreen()
        })
    }

    private fun summaryCard(
        title: String,
        amount: Double,
        bg: Int,
        foreground: Int
    ): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 16, 14, 16)
            background = rounded(bg, 20f)
        }
        box.addView(text(title, 14f, foreground))
        addSpace(box, 8)
        box.addView(text(if (hideBalance) "••••••" else money(amount),
            20f, foreground, true))
        return box
    }

    private fun recordsScreen() {
        container.addView(text("Transactions & records", 23f, navy, true))
        addSpace(container, 14)

        listOf("Asset", "Debt", "Income", "Expense").forEach { type ->
            container.addView(button("＋ Add $type", purple) {
                showRecordDialog(type)
            })
            addSpace(container, 8)
        }

        addSpace(container, 8)
        records.sortedByDescending { it.date }.forEach { record ->
            recordCard(record)
            addSpace(container, 10)
        }
        if (records.isEmpty()) {
            container.addView(text("No records yet.", 15f, muted))
        }
    }

    private fun investmentsScreen() {
        container.addView(text("Investments", 24f, navy, true))
        addSpace(container, 6)
        container.addView(text("Manually track your stocks and investments.", 14f, muted))
        addSpace(container, 16)

        val investments = records.filter {
            it.type == "Stock" || it.type == "Investment"
        }
        val totalValue = investments.sumOf { it.amount }

        val totalCard = card()
        totalCard.addView(text("Total tracked investment value", 14f, muted))
        addSpace(totalCard, 8)
        totalCard.addView(text(if (hideBalance) "••••••" else money(totalValue),
            25f, purple, true))
        container.addView(totalCard)
        addSpace(container, 16)

        container.addView(button("＋ Add stock", purple) {
            showRecordDialog("Stock")
        })
        addSpace(container, 8)
        container.addView(button("＋ Add other investment", green) {
            showRecordDialog("Investment")
        })
        addSpace(container, 16)

        investments.sortedByDescending { it.date }.forEach {
            recordCard(it)
            addSpace(container, 10)
        }
        if (investments.isEmpty()) {
            container.addView(text("Your investment records will appear here.", 14f, muted))
        }
    }

    private fun moreScreen() {
        container.addView(text("More", 24f, navy, true))
        addSpace(container, 16)
        container.addView(button("Debts and loans", navy) {
            showTypeRecords("Debt")
        })
        addSpace(container, 10)
        container.addView(button("Income records", green) {
            showTypeRecords("Income")
        })
        addSpace(container, 10)
        container.addView(button("Expense records", red) {
            showTypeRecords("Expense")
        })
        addSpace(container, 18)
        container.addView(text("Wealth Ankola", 16f, navy, true))
        container.addView(text("Manual personal finance tracking", 13f, muted))
        addSpace(container, 12)
        container.addView(text(
            "Investment values are entered manually. This version does not fetch live stock prices or calculate investment profit and loss.",
            13f, muted
        ))
    }

    private fun showTypeRecords(type: String) {
        AlertDialog.Builder(this)
            .setTitle(type)
            .setItems(records.filter { it.type == type }
                .map { "${it.name} — ${money(it.amount)}" }.toTypedArray()) { _, which ->
                val list = records.filter { it.type == type }
                if (which in list.indices) showRecordDialog(type, list[which])
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun recordCard(record: FinanceRecord) {
        val box = card()
        box.addView(text(record.name, 17f, navy, true))
        addSpace(box, 4)
        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(record.date))
        box.addView(text("${record.type}  •  $date", 12f, muted))
        addSpace(box, 6)
        box.addView(text(money(record.amount), 19f,
            if (record.type == "Income" || record.type == "Asset" ||
                record.type == "Stock" || record.type == "Investment") green else red,
            true))
        addSpace(box, 8)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("Edit", purple) {
            showRecordDialog(record.type, record)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(View(this), LinearLayout.LayoutParams(8, 1))
        actions.addView(button("Delete", red) {
            confirmDelete(record)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(actions)
        container.addView(box)
    }

    private fun showRecordDialog(type: String, existing: FinanceRecord? = null) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }
        val name = EditText(this).apply {
            hint = when (type) {
                "Stock" -> "Stock name or symbol"
                "Investment" -> "Investment name"
                else -> "Name"
            }
            setText(existing?.name ?: "")
        }
        val amount = EditText(this).apply {
            hint = if (type == "Stock" || type == "Investment")
                "Current value in ₹" else "Amount in ₹"
            inputType = 8194
            setText(existing?.amount?.toString() ?: "")
        }
        form.addView(name)
        form.addView(amount)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add $type" else "Edit $type")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val label = name.text.toString().trim()
                val value = amount.text.toString().toDoubleOrNull()
                if (label.isBlank() || value == null || value <= 0) {
                    Toast.makeText(this, "Enter a name and valid amount",
                        Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (existing == null) {
                    records.add(
                        FinanceRecord(
                            System.currentTimeMillis(),
                            type,
                            label,
                            value,
                            System.currentTimeMillis()
                        )
                    )
                } else {
                    val index = records.indexOfFirst { it.id == existing.id }
                    if (index >= 0) {
                        records[index] = existing.copy(name = label, amount = value)
                    }
                }
                saveRecords()
                showScreen()
            }
            .show()
    }

    private fun confirmDelete(record: FinanceRecord) {
        AlertDialog.Builder(this)
            .setTitle("Delete record?")
            .setMessage("Delete ${record.name}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                records.removeAll { it.id == record.id }
                saveRecords()
                showScreen()
            }
            .show()
    }
}
