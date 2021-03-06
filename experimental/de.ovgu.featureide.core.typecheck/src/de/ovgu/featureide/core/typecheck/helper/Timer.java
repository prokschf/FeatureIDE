/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2011  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.core.typecheck.helper;

/**
 * A timer class to measure time 
 * 
 * @author Soenke Holthusen
 */
public class Timer {
	private long _time_passed = 0;
	private long _time_last_start = 0;
	private boolean _started = false;

	/**
	 * starts the timer
	 */
	public void start() {
		_time_passed = 0;
		_started = true;
		_time_last_start = System.currentTimeMillis();
	}

	/**
	 * stops the timer
	 */
	public void stop() {
		_time_passed += System.currentTimeMillis() - _time_last_start;
		_started = false;
	}

	/**
	 * resumes the timer
	 */
	public void resume() {
		_started = true;
		_time_last_start = System.currentTimeMillis();
	}

	/**
	 * resets the timer, setting the time passed to 0
	 */
	public void reset() {
		_started = false;
		_time_passed = 0;
		_time_last_start = 0;
	}

	/**
	 * Returns the passed time
	 * if the timer is running, the passed time till the last stop is added to the time passed since the last start
	 * if it is not running, the time passed till the last stop is returned
	 * @return
	 */
	public long getTime() {
		if (!_started) {
			return _time_passed;
		} else {
			return _time_passed
					+ (System.currentTimeMillis() - _time_last_start);
		}
	}
}
