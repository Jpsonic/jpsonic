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

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.RatingService;
import org.airsonic.player.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for updating music file ratings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/setRating")
public class SetRatingController {

    @Autowired
    private RatingService ratingService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MediaFileService mediaFileService;

    @SuppressWarnings("PMD.NullAssignment") // (rating) Intentional allocation to register null
    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request) throws Exception {
        int id = ServletRequestUtils.getRequiredIntParameter(request, Attributes.Request.ID.value());
        Integer rating = ServletRequestUtils.getIntParameter(request, Attributes.Request.RATING.value());
        if (rating != null && rating == 0) {
            rating = null;
        }
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        String username = securityService.getCurrentUsername(request);
        ratingService.setRatingForUser(username, mediaFile, rating);
        return new ModelAndView(
                new RedirectView(ViewName.MAIN.value() + "?" + Attributes.Request.ID.value() + "=" + id));
    }
}
