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

  Copyright 2017 (C) Airsonic Authors
  Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
*/
package org.airsonic.player.service.upnp;

import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Service
public class ArtistUpnpProcessor extends UpnpContentProcessor <Artist, Album> {

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    SearchService searchService;

    @Autowired
    SettingsService settingsService;
    
    private @Autowired JpsonicComparators comparators;

    public ArtistUpnpProcessor() {
        setRootId(DispatchingContentDirectory.CONTAINER_ID_ARTIST_PREFIX);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dnla.title.artists");
    }

    public Container createContainer(Artist artist) {
        MusicArtist container = new MusicArtist();
        container.setId(getRootId() + DispatchingContentDirectory.SEPARATOR + artist.getId());
        container.setParentID(getRootId());
        container.setTitle(artist.getName());
        container.setChildCount(artist.getAlbumCount());

        return container;
    }

    public List<Artist> getAllItems() {
        List<MusicFolder> allFolders = getDispatcher().getSettingsService().getAllMusicFolders();
        return getArtistDao().getAlphabetialArtists(0, Integer.MAX_VALUE, allFolders);
    }

    public Artist getItemById(String id) {
        return getArtistDao().getArtist(Integer.parseInt(id));
    }

    public  List<Album> getChildren(Artist artist) {
        List<MusicFolder> allFolders = getDispatcher().getSettingsService().getAllMusicFolders();
        List<Album> allAlbums = getAlbumProcessor().getAlbumDao().getAlbumsForArtist(artist.getName(), allFolders);
        allAlbums.sort(comparators.albumOrder());
        if (allAlbums.size() > 1) {
            // if the artist has more than one album, add in an option to
            // view the tracks in all the albums together
            Album viewAll = new Album();
            viewAll.setName("- All Albums -");
            viewAll.setId(-1);
            viewAll.setComment(AlbumUpnpProcessor.ALL_BY_ARTIST + "_" + artist.getId());
            allAlbums.add(0, viewAll);
        }
        
        return allAlbums;
    }

    public void addChild(DIDLContent didl, Album album) {
        didl.addContainer(getAlbumProcessor().createContainer(album));
    }

    public ArtistDao getArtistDao() {
        return artistDao;
    }
    public void setArtistDao(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    public AlbumUpnpProcessor getAlbumProcessor() {
        return getDispatcher().getAlbumProcessor();
    }
}
