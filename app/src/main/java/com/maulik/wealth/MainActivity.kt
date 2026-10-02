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

        window.statusBarColor = pageBg
        window.navigationBarColor = Color.WHITE

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
            Toast.makeText(
                this,
                "Could not read records",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun migrateOldData() {
        if (prefs.getBoolean("migration_v2_done", false)) {
            return
        }

        val oldAssets =
            prefs.getFloat("assets", 0f).toDouble()

        val oldDebts =
            prefs.getFloat("debts", 0f).toDouble()

        val now = System.currentTimeMillis()

        if (oldAssets > 0) {
            records.add(
                FinanceRecord(
                    now,
                    "Asset",
                    "Previous balance",
                    oldAssets,
                    now
                )
            )
        }

        if (oldDebts > 0) {
            records.add(
                FinanceRecord(
                    now + 1,
                    "Debt",
                    "Previous balance",
                    oldDebts,
                    now
                )
            )
        }

        saveRecords()

        prefs.edit()
            .putBoolean("migration_v2_done", true)
            .apply()
    }

    private fun saveRecords() {
        val array = JSONArray()

        records.forEach { record ->
            val item = JSONObject()

            item.put("id", record.id)
            item.put("type", record.type)
            item.put("name", record.name)
            item.put("amount", record.amount)
            item.put("date", record.date)

            array.put(item)
        }

        prefs.edit()
            .putString("records_v2", array.toString())
            .apply()
    }

    private fun money(value: Double): String {
        return "₹" +
            String.format(
                Locale("en", "IN"),
                "%,.0f",
                value
            )
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

    private fun monthStart(): Long {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    private fun rounded(
        color: Int,
        radius: Float = 22f
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun gradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(navy, purple)
        ).apply {
            cornerRadius = 28f
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int = darkText,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun button(
        label: String,
        color: Int,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12)
            background = rounded(color, 16f)

            setOnClickListener {
                onClick()
            }
        }
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            background = rounded(Color.WHITE, 22f)
            elevation = 2f
        }
    }

    private fun addSpace(
        parent: LinearLayout,
        height: Int = 10
    ) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, height)
        )
    }

    private fun showScreen() {

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBg)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 20)
        }

        scroll.addView(container)

        outer.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(4, 5, 4, 16)
            setBackgroundColor(Color.WHITE)
            elevation = 8f
        }

        listOf(
            "Home",
            "Records",
            "Invest",
            "More"
        ).forEach { tab ->

            val item = TextView(this).apply {

                text = when (tab) {
                    "Home" -> "⌂\nHome"
                    "Records" -> "⇄\nRecords"
                    "Invest" -> "▥\nInvest"
                    else -> "☰\nMore"
                }

                textSize = 11f
                gravity = Gravity.CENTER

                setTextColor(
                    if (selectedTab == tab) {
                        purple
                    } else {
                        muted
                    }
                )

                setPadding(4, 4, 4, 4)

                setOnClickListener {
                    selectedTab = tab
                    showScreen()
                }
            }

            nav.addView(
                item,
                LinearLayout.LayoutParams(
                    0,
                    -2,
                    1f
                )
            )
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

        // ---------------------------------------------------------
        // HEADER
        // ---------------------------------------------------------

        container.addView(
            text(
                "Good day, Maulik 👋",
                15f,
                muted
            )
        )

        addSpace(container, 2)

        container.addView(
            text(
                "Your money overview",
                21f,
                navy,
                true
            )
        )

        addSpace(container, 10)

        // ---------------------------------------------------------
        // NET WORTH HERO
        // ---------------------------------------------------------

        val netWorth =
            assetTotal() - total("Debt")

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
            minimumHeight = 135
            background = gradient()
            elevation = 4f
        }

        val heroTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        heroTop.addView(
            text(
                "TOTAL NET WORTH",
                13f,
                Color.LTGRAY,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        val eye = TextView(this).apply {
            text = if (hideBalance) "Show" else "Hide"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(10, 5, 10, 5)
            background = rounded(
                Color.argb(45, 255, 255, 255),
                12f
            )

            setOnClickListener {
                hideBalance = !hideBalance
                showScreen()
            }
        }

        heroTop.addView(eye)

        hero.addView(heroTop)

        addSpace(hero, 9)

        hero.addView(
            text(
                if (hideBalance) {
                    "₹ ••••••••"
                } else {
                    money(netWorth)
                },
                29f,
                Color.WHITE,
                true
            )
        )

        addSpace(hero, 5)

        hero.addView(
            text(
                "Assets minus debts",
                12f,
                Color.LTGRAY
            )
        )

        container.addView(hero)

        addSpace(container, 13)

        // ---------------------------------------------------------
        // FINANCE SUMMARY
        // ---------------------------------------------------------

        container.addView(
            text(
                "Your finances",
                18f,
                navy,
                true
            )
        )

        addSpace(container, 7)

        val summaryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        summaryRow.addView(
            summaryCard(
                "Assets",
                assetTotal(),
                Color.rgb(220, 252, 231),
                Color.rgb(22, 101, 52)
            ),
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        summaryRow.addView(
            View(this),
            LinearLayout.LayoutParams(
                8,
                1
            )
        )

        summaryRow.addView(
            summaryCard(
                "Debts",
                total("Debt"),
                Color.rgb(254, 226, 226),
                Color.rgb(185, 28, 28)
            ),
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        container.addView(summaryRow)

        addSpace(container, 13)

        // ---------------------------------------------------------
        // QUICK ACTIONS
        // ---------------------------------------------------------

        container.addView(
            text(
                "Quick Actions",
                18f,
                navy,
                true
            )
        )

        addSpace(container, 7)

        val actionRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actionRow1.addView(
            button(
                "＋ Add Asset",
                Color.rgb(37, 99, 235)
            ) {
                showRecordDialog("Asset")
            },
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        actionRow1.addView(
            View(this),
            LinearLayout.LayoutParams(
                8,
                1
            )
        )

        actionRow1.addView(
            button(
                "− Add Debt",
                Color.rgb(190, 24, 93)
            ) {
                showRecordDialog("Debt")
            },
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        container.addView(actionRow1)

        addSpace(container, 7)

        val actionRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actionRow2.addView(
            button(
                "↗ Add Income",
                green
            ) {
                showRecordDialog("Income")
            },
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        actionRow2.addView(
            View(this),
            LinearLayout.LayoutParams(
                8,
                1
            )
        )

        actionRow2.addView(
            button(
                "↘ Add Expense",
                Color.rgb(234, 88, 12)
            ) {
                showRecordDialog("Expense")
            },
            LinearLayout.LayoutParams(
                0,
