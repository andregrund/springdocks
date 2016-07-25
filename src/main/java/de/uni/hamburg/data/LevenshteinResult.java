package de.uni.hamburg.data;

/**
 * DOCKS is a framework for post-processing results of Cloud-based speech
 * recognition systems.
 * Copyright (C) 2014 Johannes Twiefel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * 7twiefel@informatik.uni-hamburg.de
 */

public class LevenshteinResult implements Comparable {

    private int distance;

    private int id;

    private int matchingId;

    public LevenshteinResult(final int distance, final int id, final int matchingId) {
        this.distance = distance;
        this.id = id;
        this.matchingId = matchingId;
    }

    @Override
    public int compareTo(final Object arg0) {
        // TODO Auto-generated method stub
        LevenshteinResult otherResult = (LevenshteinResult) arg0;
        if (this.distance < otherResult.distance)
            return -1;
        else if (this.distance == otherResult.distance)
            return 0;
        else
            return 1;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(final int distance) {
        this.distance = distance;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public int getMatchingId() {
        return matchingId;
    }

    public void setMatchingId(final int matchingId) {
        this.matchingId = matchingId;
    }
}
