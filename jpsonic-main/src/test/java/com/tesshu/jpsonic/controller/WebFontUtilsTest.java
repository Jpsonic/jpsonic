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
 */

package com.tesshu.jpsonic.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.FontScheme;
import org.airsonic.player.command.PersonalSettingsCommand;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SettingsService;
import org.apache.catalina.connector.Request;
import org.junit.Test;
import org.junit.jupiter.api.Order;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class WebFontUtilsTest {

    @Test
    @Order(1)
    public void testSetToRequest() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        SettingsService settingsService = new SettingsService();
        Method method = settingsService.getClass().getDeclaredMethod("createDefaultUserSettings", String.class);
        method.setAccessible(true);
        UserSettings settings = (UserSettings) method.invoke(settingsService, "");

        // DEFAULT
        HttpServletRequest request = new Request(null);
        WebFontUtils.setToRequest(settings, request);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FACE_KEY), "");
        assertEquals(request.getAttribute(WebFontUtils.FONT_SIZE_KEY), WebFontUtils.DEFAULT_FONT_SIZE);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FAMILY_KEY), WebFontUtils.DEFAULT_FONT_FAMILY);

        // JP_EMBED
        PersonalSettingsCommand command = new PersonalSettingsCommand();
        WebFontUtils.setToCommand(new UserSettings(""), command);
        command.setFontSchemeName(FontScheme.JP_EMBED.name());
        WebFontUtils.setToSettings(command, settings);
        WebFontUtils.setToRequest(settings, request);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FACE_KEY),
                "@font-face {" + "font-family: \"Kazesawa-Regular\";" + "src: "
                        + "url(\"/fonts/kazesawa/Kazesawa-Regular.woff\") format(\"woff\"), "
                        + "url(\"/fonts/kazesawa/Kazesawa-Regular.ttf\") format(\"truetype\");" + "}");
        assertEquals(request.getAttribute(WebFontUtils.FONT_SIZE_KEY), WebFontUtils.DEFAULT_JP_FONT_SIZE);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FAMILY_KEY),
                WebFontUtils.JP_FONT_NAME + ", " + WebFontUtils.DEFAULT_FONT_FAMILY);

        // no settings(logon)
        request = new Request(null);
        WebFontUtils.setToRequest(null, request);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FACE_KEY), "");
        assertEquals(request.getAttribute(WebFontUtils.FONT_SIZE_KEY), WebFontUtils.DEFAULT_FONT_SIZE);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FAMILY_KEY), WebFontUtils.DEFAULT_FONT_FAMILY);

        // CUSTOM
        request = new Request(null);
        command = new PersonalSettingsCommand();
        WebFontUtils.setToCommand((UserSettings) method.invoke(settingsService, ""), command);

        command.setFontSchemeName(FontScheme.CUSTOM.name());
        WebFontUtils.setToSettings(command, settings);
        WebFontUtils.setToRequest(settings, request);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FACE_KEY), "");
        assertEquals(request.getAttribute(WebFontUtils.FONT_SIZE_KEY), WebFontUtils.DEFAULT_FONT_SIZE);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FAMILY_KEY), WebFontUtils.DEFAULT_FONT_FAMILY);

        command.setFontSchemeName(FontScheme.JP_EMBED.name());
        WebFontUtils.setToSettings(command, settings);
        WebFontUtils.setToCommand(settings, command);
        command.setFontSchemeName(FontScheme.CUSTOM.name());
        WebFontUtils.setToSettings(command, settings);
        WebFontUtils.setToRequest(settings, request);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FACE_KEY), "");
        assertEquals(request.getAttribute(WebFontUtils.FONT_SIZE_KEY), WebFontUtils.DEFAULT_JP_FONT_SIZE);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FAMILY_KEY),
                WebFontUtils.JP_FONT_NAME + ", " + WebFontUtils.DEFAULT_FONT_FAMILY);

        command.setFontFamily("Arial");
        command.setFontSize(20);
        WebFontUtils.setToSettings(command, settings);
        WebFontUtils.setToRequest(settings, request);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FACE_KEY), "");
        assertEquals(request.getAttribute(WebFontUtils.FONT_SIZE_KEY), 20);
        assertEquals(request.getAttribute(WebFontUtils.FONT_FAMILY_KEY), "Arial");
    }

    @Test
    @Order(2)
    public void testSetToCommand() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        SettingsService settingsService = new SettingsService();
        Method method = settingsService.getClass().getDeclaredMethod("createDefaultUserSettings", String.class);
        method.setAccessible(true);
        UserSettings from = (UserSettings) method.invoke(settingsService, "");
        PersonalSettingsCommand to = new PersonalSettingsCommand();
        WebFontUtils.setToCommand(from, to);

        assertEquals(FontScheme.DEFAULT.name(), to.getFontSchemeName());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE, to.getFontSize());
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, to.getFontFamily());

        from.setFontSchemeName(FontScheme.JP_EMBED.name());
        from.setFontSize(20);
        from.setFontFamily("Arial");
        WebFontUtils.setToCommand(from, to);

        assertEquals(FontScheme.JP_EMBED.name(), to.getFontSchemeName());
        assertEquals(20, to.getFontSize());
        assertEquals("Arial", to.getFontFamily());
    }

    @Test
    @Order(3)
    public void testFormatFontFamily() {
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, WebFontUtils.formatFontFamily(WebFontUtils.DEFAULT_FONT_FAMILY));
        String jpFontfamily = WebFontUtils.JP_FONT_NAME + ", " + WebFontUtils.DEFAULT_FONT_FAMILY;
        assertEquals(jpFontfamily, WebFontUtils.formatFontFamily(jpFontfamily));
        assertEquals("", WebFontUtils.formatFontFamily(null));
        assertEquals("", WebFontUtils.formatFontFamily(""));
        assertEquals("", WebFontUtils.formatFontFamily(","));
        assertEquals("", WebFontUtils.formatFontFamily(", "));
        assertEquals("", WebFontUtils.formatFontFamily(" "));
        assertEquals("", WebFontUtils.formatFontFamily("\"\"\""));
        assertEquals("Arial", WebFontUtils.formatFontFamily("Arial"));
        assertEquals("Arial", WebFontUtils.formatFontFamily("\"Arial\""));
        assertEquals("\"Comic Sans\"", WebFontUtils.formatFontFamily("\"Comic Sans\""));
        assertEquals("\"Comic Sans\"", WebFontUtils.formatFontFamily("Comic Sans"));
        assertEquals("\"Comic Sans\"", WebFontUtils.formatFontFamily("Comic\" Sans"));
        assertEquals("\"Comic Sans\"", WebFontUtils.formatFontFamily("C\\o\bm\fi\nc\" \rS\tans"));
        assertEquals("Arial, \"Comic Sans\"", WebFontUtils.formatFontFamily("Arial,C\\o\bm\fi\nc\" \rS\tans"));
        assertEquals("Arial, \"Comic Sans\"", WebFontUtils.formatFontFamily("Ari\"al,  Comi\"c Sans"));
    }

    @Test
    @Order(4)
    public void testSetToSettings() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        PersonalSettingsCommand command = new PersonalSettingsCommand();
        WebFontUtils.setToCommand(new UserSettings(""), command);

        // DEFAULT
        SettingsService settingsService = new SettingsService();
        Method method = settingsService.getClass().getDeclaredMethod("createDefaultUserSettings", String.class);
        method.setAccessible(true);
        UserSettings to = (UserSettings) method.invoke(settingsService, "");

        WebFontUtils.setToSettings(command, to);
        assertEquals(FontScheme.DEFAULT.name(), to.getFontSchemeName());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE, to.getFontSize());
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, to.getFontFamily());

        // JP_EMBED
        command.setFontSchemeName(FontScheme.JP_EMBED.name());
        WebFontUtils.setToSettings(command, to);
        assertEquals(FontScheme.JP_EMBED.name(), to.getFontSchemeName());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE + 1, to.getFontSize());
        assertEquals(WebFontUtils.JP_FONT_NAME + ", " + WebFontUtils.DEFAULT_FONT_FAMILY, to.getFontFamily());

        // Return to DEFAULT
        command.setFontSchemeName(FontScheme.DEFAULT.name());
        WebFontUtils.setToSettings(command, to);
        assertEquals(FontScheme.DEFAULT.name(), to.getFontSchemeName());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE, to.getFontSize());
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, to.getFontFamily());
    }
}
