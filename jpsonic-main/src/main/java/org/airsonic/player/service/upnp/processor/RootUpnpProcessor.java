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
package org.airsonic.player.service.upnp.processor;

import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Component
public class RootUpnpProcessor extends UpnpContentProcessor<Container, Container> {

    private ArrayList<Container> containers = new ArrayList<>();

    public Container createRootContainer() {
        StorageFolder root = new StorageFolder();
        root.setId(UpnpProcessDispatcher.CONTAINER_ID_ROOT);
        root.setParentID("-1");

        // MediaLibraryStatistics statistics = indexManager.getStatistics();
        // returning large storageUsed values doesn't play nicely with
        // some upnp clients
        // root.setStorageUsed(statistics == null ? 0 :
        // statistics.getTotalLengthInBytes());
        root.setStorageUsed(-1L);
        root.setTitle("Jpsonic Media");
        root.setRestricted(true);
        root.setSearchable(true);
        root.setWriteStatus(WriteStatus.NOT_WRITABLE);

        root.setChildCount(6);
        return root;
    }
    
    @Override
    public void initTitle() {
        // to be none
    }

    public Container createContainer(Container item) {
        // the items are the containers in this case.
        return item;
    }

    @Override
    public int getItemCount() {
        return containers.size();
    }

    @Override
    public List<Container> getItems(long offset, long maxResults) {
        containers.clear();
        containers.add(getDispatcher().getAlbumProcessor().createRootContainer());
        containers.add(getDispatcher().getArtistProcessor().createRootContainer());
        containers.add(getDispatcher().getGenreProcessor().createRootContainer());
        containers.add(getDispatcher().getMediaFileProcessor().createRootContainer());
        containers.add(getDispatcher().getPlaylistProcessor().createRootContainer());
        containers.add(getDispatcher().getRecentAlbumProcessor().createRootContainer());
        return org.airsonic.player.util.Util.subList(containers, offset, maxResults);
    }

    public Container getItemById(String id) {
        return createRootContainer();
    }

    @Override
    public int getChildSizeOf(Container item) {
        return getChildren(item).size();
    }

    public List<Container> getChildren(Container item) {
        return containers;
    }

    @Override
    public List<Container> getChildren(Container item, long offset, long maxResults) {
        return org.airsonic.player.util.Util.subList(getChildren(item), offset, maxResults);
    }

    public void addChild(DIDLContent didl, Container child) {
        // special case; root doesn't have object instances
    }

}