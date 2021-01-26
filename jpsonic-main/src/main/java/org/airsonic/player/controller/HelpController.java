/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */

package org.airsonic.player.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.airsonic.player.domain.User;
import org.airsonic.player.i18n.AirsonicLocaleResolver;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.VersionService;
import org.airsonic.player.util.LegacyMap;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the help page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/help")
public class HelpController {

    private static final Logger LOG = LoggerFactory.getLogger(HelpController.class);

    private static final int LOG_LINES_TO_SHOW = 50;

    @Autowired
    private VersionService versionService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private AirsonicLocaleResolver airsonicLocaleResolver;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> map = LegacyMap.of();

        if (versionService.isNewFinalVersionAvailable()) {
            map.put("newVersionAvailable", true);
            map.put("latestVersion", versionService.getLatestFinalVersion());
        } else if (versionService.isNewBetaVersionAvailable()) {
            map.put("newVersionAvailable", true);
            map.put("latestVersion", versionService.getLatestBetaVersion());
        }

        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();

        String serverInfo = request.getSession().getServletContext().getServerInfo() + ", java "
                + System.getProperty("java.version") + ", " + System.getProperty("os.name");
        User user = securityService.getCurrentUser(request);
        map.put("user", user);
        map.put("admin", securityService.isAdmin(user.getUsername()));
        map.put("brand", settingsService.getBrand());
        map.put("localVersion", versionService.getLocalVersion());
        map.put("buildDate", versionService.getLocalBuildDate());
        map.put("buildNumber", versionService.getLocalBuildNumber());
        map.put("serverInfo", serverInfo);
        map.put("usedMemory", totalMemory - freeMemory);
        map.put("totalMemory", totalMemory);
        File logFile = SettingsService.getLogFile();
        List<String> latestLogEntries = getLatestLogEntries(logFile);
        map.put("logEntries", latestLogEntries);
        map.put("logFile", logFile);
        Locale locale = airsonicLocaleResolver.resolveLocale(request);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale);
        map.put("lastModified", format.format(logFile.lastModified()));
        map.put("showServerLog", settingsService.isShowServerLog());
        map.put("showStatus", settingsService.isShowStatus());
        return new ModelAndView("help", "model", map);
    }

    private static List<String> getLatestLogEntries(File logFile) {
        List<String> lines = new ArrayList<>(LOG_LINES_TO_SHOW);
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, Charset.defaultCharset())) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (lines.size() >= LOG_LINES_TO_SHOW) {
                    break;
                }
                lines.add(0, line);
            }
            return lines;
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not open log file " + logFile, e);
            }
            return null;
        }
    }

}
