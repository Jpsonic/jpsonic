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

package com.tesshu.jpsonic.service.jukebox;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.LineUnavailableException;

import com.tesshu.jpsonic.Integration;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.PlayerTechnology;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithMockUser;

class AudioPlayerFactoryTest extends AbstractPlayerFactoryTest {

    @SpyBean
    private TranscodingService transcodingService;
    @MockBean
    protected AudioPlayerFactory audioPlayerFactory;

    private AudioPlayer mockAudioPlayer;

    @BeforeEach
    @Override
    public void setup() throws ExecutionException {
        super.setup();
        mockAudioPlayer = mock(AudioPlayer.class);
        try {
            when(audioPlayerFactory.createAudioPlayer(ArgumentMatchers.any(), ArgumentMatchers.any()))
                    .thenReturn(mockAudioPlayer);
            doReturn(null).when(transcodingService).getTranscodedInputStream(ArgumentMatchers.any());
        } catch (LineUnavailableException | IOException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    protected final void createTestPlayer() {
        Player jukeBoxPlayer = new Player();
        jukeBoxPlayer.setName(JUKEBOX_PLAYER_NAME);
        jukeBoxPlayer.setUsername("admin");
        jukeBoxPlayer.setClientId(CLIENT_NAME + "-jukebox");
        jukeBoxPlayer.setTechnology(PlayerTechnology.JUKEBOX);
        playerService.createPlayer(jukeBoxPlayer);
    }

    @Integration
    @Test
    @WithMockUser(username = "admin")
    @Override
    void testJukeboxStartAction() throws ExecutionException {
        super.testJukeboxStartAction();
        verify(mockAudioPlayer).play();
    }

    @Integration
    @Test
    @WithMockUser(username = "admin")
    @Override
    void testJukeboxStopAction() throws ExecutionException {
        super.testJukeboxStopAction();
        verify(mockAudioPlayer).play();
        verify(mockAudioPlayer).pause();
    }

}
