/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Haiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.menus;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.TextView;
import de.sudoq.R;
import de.sudoq.controller.SudoqActivity;
import de.sudoq.model.profile.Profile;
import de.sudoq.model.profile.Statistics;

/**
 * Diese Klasse stellt eine Activity zur Anzeige der Statisik des aktuellen
 * Spielerprofils dar.
 */
public class StatisticsActivity extends SudoqActivity {
	/** Methods */

	/**
	 * Wird beim ersten Start der Activity aufgerufen.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.statistics);
		TextView current = (TextView) findViewById(R.id.text_played_sudokus);
		current.setText(getString(R.string.statistics_played_sudokus) + ": " + Profile.getInstance().getStatistic(Statistics.playedSudokus));
		current = (TextView) findViewById(R.id.text_played_easy_sudokus);
		current.setText(getString(R.string.statistics_played_easy_sudokus) + ": " + Profile.getInstance().getStatistic(Statistics.playedEasySudokus));
		current = (TextView) findViewById(R.id.text_played_medium_sudokus);
		current.setText(getString(R.string.statistics_played_medium_sudokus) + ": " + Profile.getInstance().getStatistic(Statistics.playedMediumSudokus));
		current = (TextView) findViewById(R.id.text_played_difficult_sudokus);
		current.setText(getString(R.string.statistics_played_difficult_sudokus) + ": " + Profile.getInstance().getStatistic(Statistics.playedDifficultSudokus));
		current = (TextView) findViewById(R.id.text_played_infernal_sudokus);
		current.setText(getString(R.string.statistics_played_infernal_sudokus) + ": " + Profile.getInstance().getStatistic(Statistics.playedInfernalSudokus));
		current = (TextView) findViewById(R.id.text_score);
		current.setText(getString(R.string.statistics_score) + ": " + Profile.getInstance().getStatistic(Statistics.maximumPoints));
		current = (TextView) findViewById(R.id.text_fastest_solving_time);
		int fullTime = Profile.getInstance().getStatistic(Statistics.fastestSolvingTime);
		String timeString = "---";
		if (fullTime != 5999) {
			timeString = fullTime / 60 + ":";
			if (fullTime % 60 < 10) {
				timeString += "0";
			}
			timeString += fullTime % 60;
		}
		current.setText(getString(R.string.statistics_fastest_solving_time) + ": " + timeString);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
}