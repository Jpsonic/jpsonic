/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 Based upon Alphanum Algorithm, Copyright 2007-2017 David Koelle
 */
package com.tesshu.jpsonic.service;

import java.util.Comparator;

/**
 * This is an updated version with enhancements made by Daniel Migowski, Andre
 * Bogus, and David Koelle. Updated by David Koelle in 2017.
 *
 * To use this class: Use the static "sort" method from the
 * java.util.Collections class: Collections.sort(your list, new
 * AlphanumComparator());
 */
public class AlphanumComparator implements Comparator<String> {

    public int compare(String s1, String s2) {
        return compare(s1, s2, false);
    }

    private int compare(String s1, String s2, boolean isIgnoreCase) {
        if ((s1 == null) || (s2 == null)) {
            return 0;
        }

        int thisMarker = 0;
        int thatMarker = 0;
        int s1Length = s1.length();
        int s2Length = s2.length();

        while (thisMarker < s1Length && thatMarker < s2Length) {
            String thisChunk = getChunk(s1, s1Length, thisMarker);
            thisMarker += thisChunk.length();

            String thatChunk = getChunk(s2, s2Length, thatMarker);
            thatMarker += thatChunk.length();

            // If both chunks contain numeric characters, sort them numerically
            int result = 0;
            if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
                // Simple chunk comparison by length.
                int thisChunkLength = thisChunk.length();
                result = thisChunkLength - thatChunk.length();
                // If equal, the first different number counts
                if (result == 0) {
                    for (int i = 0; i < thisChunkLength; i++) {
                        result = thisChunk.charAt(i) - thatChunk.charAt(i);
                        if (result != 0) {
                            return result;
                        }
                    }
                }
            } else {
                result = isIgnoreCase ? thisChunk.compareToIgnoreCase(thatChunk) : thisChunk.compareTo(thatChunk);
            }

            if (result != 0)
                return result;
        }

        return s1Length - s2Length;
    }

    public int compareToIgnoreCase(String s1, String s2) {
        return compare(s1, s2, true);
    }

    /**
     * Length of string is passed in for improved efficiency (only need to calculate
     * it once)
     **/
    private final String getChunk(String s, int slength, int marker) {
        StringBuilder chunk = new StringBuilder();
        char c = s.charAt(marker);
        chunk.append(c);
        marker++;
        if (isDigit(c)) {
            while (marker < slength) {
                c = s.charAt(marker);
                if (!isDigit(c))
                    break;
                chunk.append(c);
                marker++;
            }
        } else {
            while (marker < slength) {
                c = s.charAt(marker);
                if (isDigit(c))
                    break;
                chunk.append(c);
                marker++;
            }
        }
        return chunk.toString();
    }

    private final boolean isDigit(char ch) {
        return ((ch >= 48) && (ch <= 57));
    }

}