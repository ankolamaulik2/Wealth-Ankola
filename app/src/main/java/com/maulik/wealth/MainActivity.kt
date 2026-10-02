package com.maulik.wealth
import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.*
class MainActivity: Activity() {
 private val prefs by lazy { getSharedPreferences("wealth",Context.MODE_PRIVATE) }
 private var assets=0.0; private var debts=0.0
 private lateinit var summary:TextView
 private lateinit var history:LinearLayout
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState)
 assets=prefs.getFloat("assets",0f).toDouble(); debts=prefs.getFloat("debts",0f).toDouble()
 val root=LinearLayout(this).apply { orientation=1; setPadding(24,28,24,20); setBackgroundColor(Color.rgb(247,249,252)) }
 root.addView(TextView(this).apply { text="Wealth Maulik"; textSize=28f; setTextColor(Color.rgb(24,45,76)) })
 summary=TextView(this).apply { textSize=19f; setPadding(16,20,16,20); setBackgroundColor(Color.WHITE) }; root.addView(summary)
 val buttons=LinearLayout(this).apply { orientation=0 }
 buttons.addView(Button(this).apply { text="Add asset"; setOnClickListener { add(true) } },LinearLayout.LayoutParams(0,-2,1f))
 buttons.addView(Button(this).apply { text="Add debt"; setOnClickListener { add(false) } },LinearLayout.LayoutParams(0,-2,1f)); root.addView(buttons)
 root.addView(TextView(this).apply { text="Recent entries"; textSize=18f; setPadding(0,18,0,6) })
 history=LinearLayout(this).apply { orientation=1 }; root.addView(history)
 setContentView(ScrollView(this).apply { addView(root) }); render()
 }
 private fun add(asset:Boolean) {
 val form=LinearLayout(this).apply { orientation=1; setPadding(24,8,24,0) }
 val name=EditText(this).apply { hint=if(asset) "Asset name" else "Debt name" }
 val amount=EditText(this).apply { hint="Amount in ₹"; inputType=8194 }
 form.addView(name); form.addView(amount)
 android.app.AlertDialog.Builder(this).setTitle("New entry").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save") { _,_ ->
 val v=amount.text.toString().toDoubleOrNull()
 if(name.text.isNullOrBlank() || v==null || v<=0) Toast.makeText(this,"Enter a name and valid amount",0).show()
 else { if(asset) assets+=v else debts+=v
 prefs.edit().putFloat("assets",assets.toFloat()).putFloat("debts",debts.toFloat()).apply()
 val old=prefs.getString("history","") ?: ""; prefs.edit().putString("history",(old+"\n"+(if(asset)"Asset" else "Debt")+": "+name.text+" — ₹"+v).trim()).apply(); render() }
 }.show()
 }
 private fun render() {
 summary.text="Net worth\n₹${"%,.2f".format(assets-debts)}\n\nAssets: ₹${"%,.2f".format(assets)}   Debts: ₹${"%,.2f".format(debts)}"
 history.removeAllViews(); (prefs.getString("history","") ?: "").lines().filter{it.isNotBlank()}.reversed().forEach { s -> history.addView(TextView(this).apply { text=s; textSize=15f; setPadding(10,10,10,10) }) }
 }
}
