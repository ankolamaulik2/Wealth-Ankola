
package com.maulik.wealth

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

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
    private lateinit var root: LinearLayout
    private lateinit var summary: TextView
    private lateinit var history: LinearLayout

    private val blue = Color.rgb(24, 45, 76)
    private val background = Color.rgb(247, 249, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadRecords()
        migrateOldData()
        buildScreen()
        render()
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
                        id = item.getLong("id"),
                        type = item.getString("type"),
                        name = item.getString("name"),
                        amount = item.getDouble("amount"),
                        date = item.getLong("date")
                    )
                )
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Could not read saved records", Toast.LENGTH_LONG).show()
        }
    }

    private fun migrateOldData() {
        if (prefs.getBoolean("migration_v2_done", false)) return

        val oldAssets = prefs.getFloat("assets", 0f).toDouble()
        val oldDebts = prefs.getFloat("debts", 0f).toDouble()
        val now = System.currentTimeMillis()

        if (oldAssets > 0) {
            records.add(
                FinanceRecord(
                    id = now,
                    type = "Asset",
                    name = "Previous balance",
                    amount = oldAssets,
                    date = now
                )
            )
        }

        if (oldDebts > 0) {
            records.add(
                FinanceRecord(
                    id = now + 1,
                    type = "Debt",
                    name = "Previous balance",
                    amount = oldDebts,
                    date = now
                )
            )
        }

        saveRecords()
        prefs.edit().putBoolean("migration_v2_done", true).apply()
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
        prefs.edit().putString("records_v2", array.toString()).apply()
    }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 20)
            setBackgroundColor(background)
        }

        root.addView(TextView(this).apply {
            text = "Wealth Ankola"
            textSize = 28f
            setTextColor(blue)
            setPadding(0, 0, 0, 16)
        })

        summary = TextView(this).apply {
            textSize = 17f
            setTextColor(blue)
            setPadding(16, 18, 16, 18)
            setBackgroundColor(Color.WHITE)
        }
        root.addView(summary)

        root.addView(TextView(this).apply {
            text = "Add a record"
            textSize = 19f
            setTextColor(blue)
            setPadding(0, 20, 0, 8)
        })

        val types = listOf("Asset", "Debt", "Income", "Expense")
        val firstRow = LinearLayout(this)
        val secondRow = LinearLayout(this)

        types.forEachIndexed { index, type ->
            val button = Button(this).apply {
                text = "+ $type"
                setOnClickListener { showRecordDialog(type) }
            }
            val row = if (index < 2) firstRow else secondRow
            row.addView(button, LinearLayout.LayoutParams(0, -2, 1f))
        }

        root.addView(firstRow)
        root.addView(secondRow)

        root.addView(TextView(this).apply {
            text = "Records"
            textSize = 20f
            setTextColor(blue)
            setPadding(0, 22, 0, 8)
        })

        history = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(history)

        setContentView(ScrollView(this).apply {
            addView(root)
        })
    }

    private fun money(value: Double): String {
        return "₹" + String.format(Locale("en", "IN"), "%,.2f", value)
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

    private fun render() {
        val assets = records.filter { it.type == "Asset" }.sumOf { it.amount }
        val debts = records.filter { it.type == "Debt" }.sumOf { it.amount }

        val monthRecords = records.filter { it.date >= monthStart() }
        val income = monthRecords.filter { it.type == "Income" }.sumOf { it.amount }
        val expenses = monthRecords.filter { it.type == "Expense" }.sumOf { it.amount }

        summary.text = """
            NET WORTH
            ${money(assets - debts)}

            Assets: ${money(assets)}
            Debts: ${money(debts)}

            This month
            Income: ${money(income)}
            Expenses: ${money(expenses)}
            Balance: ${money(income - expenses)}
        """.trimIndent()

        history.removeAllViews()

        if (records.isEmpty()) {
            history.addView(TextView(this).apply {
                text = "No records yet. Use the buttons above to add one."
                textSize = 15f
                setPadding(8, 16, 8, 16)
            })
            return
        }

        records.sortedByDescending { it.date }.forEach { record ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                setBackgroundColor(Color.WHITE)
            }

            val dateText = SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            ).format(Date(record.date))

            card.addView(TextView(this).apply {
                text = "${record.type}: ${record.name}"
                textSize = 16f
                setTextColor(blue)
            })

            card.addView(TextView(this).apply {
                text = "${money(record.amount)}  •  $dateText"
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 4, 0, 8)
            })

            val actions = LinearLayout(this)
            val edit = Button(this).apply {
                text = "Edit"
                setOnClickListener { showRecordDialog(record.type, record) }
            }
            val delete = Button(this).apply {
                text = "Delete"
                setOnClickListener { confirmDelete(record) }
            }

            actions.addView(edit, LinearLayout.LayoutParams(0, -2, 1f))
            actions.addView(delete, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(actions)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 10
            history.addView(card, params)
        }
    }

    private fun showRecordDialog(
        type: String,
        existing: FinanceRecord? = null
    ) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }

        val name = EditText(this).apply {
            hint = "Name"
            setText(existing?.name ?: "")
        }

        val amount = EditText(this).apply {
            hint = "Amount in ₹"
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
                val recordName = name.text.toString().trim()
                val value = amount.text.toString().toDoubleOrNull()

                if (recordName.isBlank() || value == null || value <= 0) {
                    Toast.makeText(
                        this,
                        "Enter a name and a valid positive amount",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }

                if (existing == null) {
                    records.add(
                        FinanceRecord(
                            id = System.currentTimeMillis(),
                            type = type,
                            name = recordName,
                            amount = value,
                            date = System.currentTimeMillis()
                        )
                    )
                } else {
                    val index = records.indexOfFirst { it.id == existing.id }
                    if (index >= 0) {
                        records[index] = existing.copy(
                            name = recordName,
                            amount = value
                        )
                    }
                }

                saveRecords()
                render()
            }
            .show()
    }

    private fun confirmDelete(record: FinanceRecord) {
        AlertDialog.Builder(this)
            .setTitle("Delete record?")
            .setMessage("Delete '${record.name}'?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                records.removeAll { it.id == record.id }
                saveRecords()
                render()
            }
            .show()
    }
}
