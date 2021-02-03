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

package org.airsonic.player.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import com.tesshu.jpsonic.dao.JPlaylistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlaylistDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.playlist.DefaultPlaylistExportHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistServiceExportTest {

    PlaylistService playlistService;

    @InjectMocks
    DefaultPlaylistExportHandler defaultPlaylistExportHandler;

    @Mock
    MediaFileDao mediaFileDao;

    @Mock
    PlaylistDao playlistDao;

    @Mock
    MediaFileService mediaFileService;

    @Mock
    SettingsService settingsService;

    @Mock
    SecurityService securityService;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Captor
    ArgumentCaptor<Playlist> actual;

    @Captor
    ArgumentCaptor<List<MediaFile>> medias;

    @Before
    public void setup() {
        playlistService = new PlaylistService(new JMediaFileDao(mediaFileDao), new JPlaylistDao(playlistDao),
                securityService, settingsService, Lists.newArrayList(defaultPlaylistExportHandler),
                Collections.emptyList());
    }

    @Test
    public void testExportToM3U() throws Exception {

        when(mediaFileDao.getFilesInPlaylist(eq(23))).thenReturn(getPlaylistFiles());
        when(settingsService.getPlaylistExportFormat()).thenReturn("m3u");

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            playlistService.exportPlaylist(23, outputStream);
            String actual = outputStream.toString();
            Assert.assertEquals(IOUtils.toString(getClass().getResourceAsStream("/PLAYLISTS/23.m3u")), actual);
        }
    }

    private List<MediaFile> getPlaylistFiles() {

        MediaFile mf1 = new MediaFile();
        mf1.setId(142);
        mf1.setPath("/some/path/to_album/to_artist/name - of - song.mp3");
        mf1.setPresent(true);
        List<MediaFile> mediaFiles = new ArrayList<>();
        mediaFiles.add(mf1);

        MediaFile mf2 = new MediaFile();
        mf2.setId(1235);
        mf2.setPath("/some/path/to_album2/to_artist/another song.mp3");
        mf2.setPresent(true);
        mediaFiles.add(mf2);

        MediaFile mf3 = new MediaFile();
        mf3.setId(198403);
        mf3.setPath("/some/path/to_album2/to_artist/another song2.mp3");
        mf3.setPresent(false);
        mediaFiles.add(mf3);

        return mediaFiles;
    }
}
