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
import java.io.File
import java.util.Calendar
import kotlin.io.readLines
import kotlin.io.writeText

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERIODS = 8
    }

    private data class ScheduledMatch(val year: Int, val month: Int, val day: Int, val opponent: String)

    private data class MatchData(
        val date: String,
        val teamA: String,
        val teamB: String,
        val scoresA: IntArray,
        val scoresB: IntArray)

    private val schedule = arrayOf(
        ScheduledMatch(2026, 9, 19, "BC DES COTEAUX DU LYONNAIS - 2"),
        ScheduledMatch(2026, 9, 26, "OULLINS STE FOY BASKET BALL - 3"),
        ScheduledMatch(2026, 10, 3, "UNION SPORTIVE PIERRE BENITE - 2"),
        ScheduledMatch(2026, 10, 10, "ASSOCIATION SPORTIVE GREZIEU - SOUCIEU BASKET - 2"),
        ScheduledMatch(2026, 10, 17, "FRANCHEVILLE BASKET - 2")
    )

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
    private lateinit var tvStatus: TextView
    private lateinit var llMatches: LinearLayout

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
        tvStatus = findViewById(R.id.tvStatus)
        llMatches = findViewById(R.id.llMatches)

        if (savedInstanceState != null) {
            currentPeriod = savedInstanceState.getInt("currentPeriod", 0)
            savedInstanceState.getIntArray("scoreA")?.copyInto(scoreA)
            savedInstanceState.getIntArray("scoreB")?.copyInto(scoreB)
            etNameA.setText(savedInstanceState.getString("nameA"))
            etNameB.setText(savedInstanceState.getString("nameB"))
        }
        if (etNameA.text.isEmpty()) {
            etNameA.setText(getString(R.string.club_name))
        }
        if (etNameB.text.isEmpty()) {
            prefillOpponentFromSchedule()
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
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveMatch() }
        refreshMatchList()
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

    private fun saveMatch() {
        val nameA = etNameA.text.toString().takeIf { it.isNotBlank() } ?: getString(R.string.team_a)
        val nameB = etNameB.text.toString().takeIf { it.isNotBlank() } ?: getString(R.string.team_b)
        val winsA = (0 until PERIODS).count { scoreA[it] > scoreB[it] }
        val winsB = (0 until PERIODS).count { scoreB[it] > scoreA[it] }
        val winner = if (winsA > winsB) nameA else if (winsB > winsA) nameB else "Égalité"
        val periodA = (0 until PERIODS).map { scoreA[it].toString() }.joinToString(" - ")
        val periodB = (0 until PERIODS).map { scoreB[it].toString() }.joinToString(" - ")
        val cal = Calendar.getInstance()
        val now = "%02d/%02d/%04d %02d:%02d".format(
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE))
        val stamp = "%04d-%02d-%02d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE))
        val record = StringBuilder()
            .append("date=").append(now).append("\n")
            .append("teamA=").append(nameA).append("\n")
            .append("teamB=").append(nameB).append("\n")
            .append("scoreA=").append((0 until PERIODS).map { scoreA[it].toString() }.joinToString(" ")).append("\n")
            .append("scoreB=").append((0 until PERIODS).map { scoreB[it].toString() }.joinToString(" ")).append("\n")
            .append("---\n")
            .append(nameA).append(" vs ").append(nameB).append("\n")
            .append("Périodes ").append(nameA).append(" : ").append(periodA).append("\n")
            .append("Périodes ").append(nameB).append(" : ").append(periodB).append("\n")
            .append(getString(R.string.wins_summary, winsA.toString(), winsB.toString())).append("\n")
            .append("Vainqueur : ").append(winner).append("\n")
            .toString()
        try {
            val dir = matchDir()
            File(dir, "match-$stamp.txt").writeText(record)
            tvStatus.text = getString(R.string.match_saved)
            refreshMatchList()
        } catch (e: Exception) {
            tvStatus.text = getString(R.string.save_error) + " : " + e.message
        }
    }

    private fun prefillOpponentFromSchedule() {
        val cal = Calendar.getInstance()
        val today = ScheduledMatch(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            "")
        val found = schedule.find { it.year == today.year && it.month == today.month && it.day == today.day }
        if (found != null) {
            etNameB.setText(found.opponent)
        }
    }

    private fun matchDir(): File {
        val dataDir = ContextCompat.getDataDir(this) ?: throw Exception("no data dir")
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
        val dir = File(dataDir, "matchs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun refreshMatchList() {
        llMatches.removeAllViews()
        try {
            val files = mutableListOf<File>()
            matchDir().listFiles()?.forEach {
                if (it.isFile && it.name.startsWith("match-") && it.name.endsWith(".txt")) {
                    files.add(it)
                }
            }
            files.sortBy { it.name }
            if (files.isEmpty()) {
                val empty = TextView(this)
                empty.text = getString(R.string.no_match)
                empty.setTextColor(ContextCompat.getColor(this, R.color.period_default_text))
                llMatches.addView(empty)
                return
            }
            for (file in files) {
                val data = parseMatchFile(file)
                val button = Button(this)
                button.text = data.date + "  -  " + data.teamA + " vs " + data.teamB
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                params.topMargin = 4.dp()
                button.layoutParams = params
                button.setOnClickListener { loadMatch(file) }
                llMatches.addView(button)
            }
        } catch (e: Exception) {
            val empty = TextView(this)
            empty.text = getString(R.string.no_match)
            empty.setTextColor(ContextCompat.getColor(this, R.color.period_default_text))
            llMatches.addView(empty)
        }
    }

    private fun parseMatchFile(file: File): MatchData {
        val scoresA = IntArray(PERIODS)
        val scoresB = IntArray(PERIODS)
        var date = ""
        var teamA = ""
        var teamB = ""
        for (line in file.readLines()) {
            if (line.startsWith("date=")) {
                date = line.substring(5)
            } else if (line.startsWith("teamA=")) {
                teamA = line.substring(6)
            } else if (line.startsWith("teamB=")) {
                teamB = line.substring(6)
            } else if (line.startsWith("scoreA=")) {
                for ((i, part) in line.substring(7).split(" ").withIndex()) {
                    if (i < PERIODS) {
                        scoresA[i] = part.toIntOrNull() ?: 0
                    }
                }
            } else if (line.startsWith("scoreB=")) {
                for ((i, part) in line.substring(7).split(" ").withIndex()) {
                    if (i < PERIODS) {
                        scoresB[i] = part.toIntOrNull() ?: 0
                    }
                }
            } else if (line == "---") {
                break
            }
        }
        return MatchData(date, teamA, teamB, scoresA, scoresB)
    }

    private fun loadMatch(file: File) {
        try {
            val data = parseMatchFile(file)
            data.scoresA.copyInto(scoreA)
            data.scoresB.copyInto(scoreB)
            etNameA.setText(data.teamA)
            etNameB.setText(data.teamB)
            currentPeriod = 0
            selectPeriod(currentPeriod)
            tvStatus.text = getString(R.string.match_loaded)
        } catch (e: Exception) {
            tvStatus.text = getString(R.string.save_error) + " : " + e.message
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}