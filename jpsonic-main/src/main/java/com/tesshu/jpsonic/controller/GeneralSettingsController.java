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

package com.tesshu.jpsonic.controller;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.command.GeneralSettingsCommand;
import com.tesshu.jpsonic.domain.Theme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate general settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/generalSettings")
public class GeneralSettingsController {

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final ShareService shareService;
    private final OutlineHelpSelector outlineHelpSelector;

    public GeneralSettingsController(SettingsService settingsService, SecurityService securityService,
            ShareService shareService, OutlineHelpSelector outlineHelpSelector) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.shareService = shareService;
        this.outlineHelpSelector = outlineHelpSelector;
    }

    @GetMapping
    protected String displayForm() {
        return "generalSettings";
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model,
            @RequestParam(Attributes.Request.NameConstants.TOAST) Optional<Boolean> toast) {
        GeneralSettingsCommand command = new GeneralSettingsCommand();
        command.setCoverArtFileTypes(settingsService.getCoverArtFileTypes());
        command.setIgnoredArticles(settingsService.getIgnoredArticles());
        command.setShortcuts(settingsService.getShortcuts());
        command.setIndex(settingsService.getIndexString());
        command.setPlaylistFolder(settingsService.getPlaylistFolder());
        command.setMusicFileTypes(settingsService.getMusicFileTypes());
        command.setVideoFileTypes(settingsService.getVideoFileTypes());
        command.setSortAlbumsByYear(settingsService.isSortAlbumsByYear());
        command.setSortGenresByAlphabet(settingsService.isSortGenresByAlphabet());
        command.setProhibitSortVarious(settingsService.isProhibitSortVarious());
        command.setSortAlphanum(settingsService.isSortAlphanum());
        command.setSortStrict(settingsService.isSortStrict());
        command.setSearchComposer(settingsService.isSearchComposer());
        command.setOutputSearchQuery(settingsService.isOutputSearchQuery());
        command.setSearchMethodLegacy(settingsService.isSearchMethodLegacy());
        command.setAnonymousTranscoding(settingsService.isAnonymousTranscoding());
        command.setGettingStartedEnabled(settingsService.isGettingStartedEnabled());
        command.setWelcomeTitle(settingsService.getWelcomeTitle());
        command.setWelcomeSubtitle(settingsService.getWelcomeSubtitle());
        command.setWelcomeMessage(settingsService.getWelcomeMessage());
        command.setLoginMessage(settingsService.getLoginMessage());
        command.setUseRadio(settingsService.isUseRadio());
        command.setUseSonos(settingsService.isUseSonos());
        command.setPublishPodcast(settingsService.isPublishPodcast());
        command.setShowJavaJukebox(settingsService.isShowJavaJukebox());
        command.setShowServerLog(settingsService.isShowServerLog());
        command.setShowStatus(settingsService.isShowStatus());
        command.setOthersPlayingEnabled(settingsService.isOthersPlayingEnabled());
        command.setShowRememberMe(settingsService.isShowRememberMe());

        command.setDefaultIndexString(settingsService.getDefaultIndexString());
        command.setSimpleIndexString(settingsService.getSimpleIndexString());
        command.setDefaultSortAlbumsByYear(settingsService.isDefaultSortAlbumsByYear());
        command.setDefaultSortGenresByAlphabet(settingsService.isDefaultSortGenresByAlphabet());
        command.setDefaultProhibitSortVarious(settingsService.isDefaultProhibitSortVarious());
        command.setDefaultSortAlphanum(settingsService.isDefaultSortAlphanum());
        command.setDefaultSortStrict(settingsService.isDefaultSortStrict());

        User user = securityService.getCurrentUser(request);
        command.setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));

        toast.ifPresent(command::setShowToast);

        List<Theme> themes = SettingsService.getAvailableThemes();
        command.setThemes(themes.toArray(new Theme[0]));
        String currentThemeId = settingsService.getThemeId();
        for (int i = 0; i < themes.size(); i++) {
            if (currentThemeId.equals(themes.get(i).getId())) {
                command.setThemeIndex(String.valueOf(i));
                break;
            }
        }

        Locale currentLocale = settingsService.getLocale();
        Locale[] locales = settingsService.getAvailableLocales();
        String[] localeStrings = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            localeStrings[i] = locales[i].getDisplayName(locales[i]);

            if (currentLocale.equals(locales[i])) {
                command.setLocaleIndex(String.valueOf(i));
            }
        }
        command.setLocales(localeStrings);

        command.setShareCount(shareService.getAllShares().size());

        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
    }

    @PostMapping
    protected ModelAndView doSubmitAction(
            @ModelAttribute(Attributes.Model.Command.VALUE) GeneralSettingsCommand command,
            RedirectAttributes redirectAttributes) {

        int themeIndex = Integer.parseInt(command.getThemeIndex());
        Theme theme = SettingsService.getAvailableThemes().get(themeIndex);

        int localeIndex = Integer.parseInt(command.getLocaleIndex());
        Locale locale = settingsService.getAvailableLocales()[localeIndex];

        boolean isReload = !settingsService.getIndexString().equals(command.getIndex())
                || !settingsService.getIgnoredArticles().equals(command.getIgnoredArticles())
                || !settingsService.getShortcuts().equals(command.getShortcuts())
                || !settingsService.getThemeId().equals(theme.getId()) || !settingsService.getLocale().equals(locale)
                || settingsService.isOthersPlayingEnabled() != command.isOthersPlayingEnabled();
        redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), isReload);
        if (!isReload) {
            redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
        }
        settingsService.setIndexString(command.getIndex());
        settingsService.setIgnoredArticles(command.getIgnoredArticles());
        settingsService.setShortcuts(command.getShortcuts());
        settingsService.setPlaylistFolder(command.getPlaylistFolder());
        settingsService.setMusicFileTypes(command.getMusicFileTypes());
        settingsService.setVideoFileTypes(command.getVideoFileTypes());
        settingsService.setCoverArtFileTypes(command.getCoverArtFileTypes());
        settingsService.setSortAlbumsByYear(command.isSortAlbumsByYear());
        settingsService.setSortGenresByAlphabet(command.isSortGenresByAlphabet());
        settingsService.setProhibitSortVarious(command.isProhibitSortVarious());
        settingsService.setSortAlphanum(command.isSortAlphanum());
        settingsService.setSortStrict(command.isSortStrict());
        settingsService.setSearchComposer(command.isSearchComposer());
        settingsService.setOutputSearchQuery(command.isOutputSearchQuery());
        settingsService
                .setSearchMethodChanged(settingsService.isSearchMethodLegacy() != command.isSearchMethodLegacy());
        settingsService.setAnonymousTranscoding(command.isAnonymousTranscoding());
        settingsService.setSearchMethodLegacy(command.isSearchMethodLegacy());
        settingsService.setGettingStartedEnabled(command.isGettingStartedEnabled());
        settingsService.setWelcomeTitle(command.getWelcomeTitle());
        settingsService.setWelcomeSubtitle(command.getWelcomeSubtitle());
        settingsService.setWelcomeMessage(command.getWelcomeMessage());
        settingsService.setLoginMessage(command.getLoginMessage());
        settingsService.setUseRadio(command.isUseRadio());
        settingsService.setUseSonos(command.isUseSonos());
        settingsService.setPublishPodcast(command.isPublishPodcast());
        settingsService.setThemeId(theme.getId());
        settingsService.setLocale(locale);
        settingsService.setShowJavaJukebox(command.isShowJavaJukebox());
        settingsService.setShowServerLog(command.isShowServerLog());
        settingsService.setShowStatus(command.isShowStatus());
        settingsService.setOthersPlayingEnabled(command.isOthersPlayingEnabled());
        settingsService.setShowRememberMe(command.isShowRememberMe());
        settingsService.save();

        return new ModelAndView(new RedirectView(ViewName.GENERAL_SETTINGS.value()));
    }

}
