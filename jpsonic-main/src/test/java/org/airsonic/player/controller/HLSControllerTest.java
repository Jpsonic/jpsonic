/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.controller;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Sindre Mehus
 * 
 * @version $Id: StreamControllerTestCase.java 3307 2013-01-04 13:48:49Z sindre_mehus $
 */
@SpringBootTest
public class HLSControllerTest {

    @Test
    public void testParseBitRate() {
        HLSController controller = new HLSController();

        Pair<Integer, Dimension> pair = controller.parseBitRate("1000");
        assertEquals(1000, pair.getLeft().intValue());
        assertNull(pair.getRight());

        pair = controller.parseBitRate("1000@400x300");
        assertEquals(1000, pair.getLeft().intValue());
        assertEquals(400, pair.getRight().width);
        assertEquals(300, pair.getRight().height);

        try {
            controller.parseBitRate("asdfl");
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            controller.parseBitRate("1000@300");
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            controller.parseBitRate("1000@300x400ZZ");
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

}
