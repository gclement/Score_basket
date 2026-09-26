package com.example.scorebasket

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERIODS = 8
    }

    private val scoreA = IntArray(PERIODS)
    private val scoreB = IntArray(PERIODS)
    private var currentPeriod = 0
    private val periodButtons = mutableListOf<Button>()

    private lateinit var tvWinsA: TextView
    private lateinit var tvWinsB: TextView
    private lateinit var tvPeriodA: TextView
    private lateinit var tvPeriodB: TextView
    private lateinit var tvTeamALabel: TextView
    private lateinit var tvTeamBLabel: TextView
    private lateinit var etNameA: EditText
    private lateinit var etNameB: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvWinsA = findViewById(R.id.tvWinsA)
        tvWinsB = findViewById(R.id.tvWinsB)
        tvPeriodA = findViewById(R.id.tvPeriodA)
        tvPeriodB = findViewById(R.id.tvPeriodB)
        tvTeamALabel = findViewById(R.id.tvTeamALabel)
        tvTeamBLabel = findViewById(R.id.tvTeamBLabel)
        etNameA = findViewById(R.id.etNameA)
        etNameB = findViewById(R.id.etNameB)

        if (savedInstanceState != null) {
            currentPeriod = savedInstanceState.getInt("currentPeriod", 0)
            savedInstanceState.getIntArray("scoreA")?.copyInto(scoreA)
            savedInstanceState.getIntArray("scoreB")?.copyInto(scoreB)
            etNameA.setText(savedInstanceState.getString("nameA"))
            etNameB.setText(savedInstanceState.getString("nameB"))
        }

        etNameA.addTextChangedListener(makeNameWatcher())
        etNameB.addTextChangedListener(makeNameWatcher())
        updateTeamLabels()

        buildPeriodButtons()
        selectPeriod(currentPeriod)

        findViewById<Button>(R.id.btnA1).setOnClickListener { addScore(0, 1) }
        findViewById<Button>(R.id.btnA2).setOnClickListener { addScore(0, 2) }
        findViewById<Button>(R.id.btnB1).setOnClickListener { addScore(1, 1) }
        findViewById<Button>(R.id.btnB2).setOnClickListener { addScore(1, 2) }
        findViewById<Button>(R.id.btnReset).setOnClickListener { resetMatch() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("currentPeriod", currentPeriod)
        outState.putIntArray("scoreA", scoreA)
        outState.putIntArray("scoreB", scoreB)
        outState.putString("nameA", etNameA.text.toString())
        outState.putString("nameB", etNameB.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun makeNameWatcher(): TextWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTeamLabels()
            }
        }

    private fun updateTeamLabels() {
        tvTeamALabel.text = etNameA.text.toString().takeIf { it.isNotBlank() } ?: getString(R.string.team_a)
        tvTeamBLabel.text = etNameB.text.toString().takeIf { it.isNotBlank() } ?: getString(R.string.team_b)
    }

    private fun buildPeriodButtons() {
        val container = findViewById<LinearLayout>(R.id.llPeriods)
        container.removeAllViews()
        periodButtons.clear()
        for (i in 0 until PERIODS) {
            val button = Button(this)
            button.text = (i + 1).toString()
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.marginStart = 4.dp()
            params.marginEnd = 4.dp()
            button.layoutParams = params
            button.setOnClickListener { selectPeriod(i) }
            container.addView(button)
            periodButtons.add(button)
        }
    }

    private fun selectPeriod(index: Int) {
        currentPeriod = index
        for ((i, button) in periodButtons.withIndex()) {
            if (i == index) {
                button.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.period_selected_bg))
                button.setTextColor(ContextCompat.getColor(this, R.color.period_selected_text))
            } else {
                button.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.period_default_bg))
                button.setTextColor(ContextCompat.getColor(this, R.color.period_default_text))
            }
        }
        refreshScores()
    }

    private fun addScore(team: Int, points: Int) {
        if (team == 0) {
            scoreA[currentPeriod] += points
        } else {
            scoreB[currentPeriod] += points
        }
        refreshScores()
    }

    private fun resetMatch() {
        scoreA.fill(0)
        scoreB.fill(0)
        refreshScores()
    }

    private fun refreshScores() {
        tvPeriodA.text = scoreA[currentPeriod].toString()
        tvPeriodB.text = scoreB[currentPeriod].toString()
        val winsA = (0 until PERIODS).count { scoreA[it] > scoreB[it] }
        val winsB = (0 until PERIODS).count { scoreB[it] > scoreA[it] }
        tvWinsA.text = winsA.toString()
        tvWinsB.text = winsB.toString()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}