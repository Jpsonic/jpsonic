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

package org.airsonic.player.service.search;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.airsonic.player.service.search.IndexType.*;
import static org.springframework.util.ObjectUtils.isEmpty;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    private QueryFactory queryFactory;

    @Autowired
    private IndexManager indexManager;

    @Autowired
    private SearchServiceUtilities util;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private Ehcache searchCache;

    @Override
    public SearchResult search(SearchCriteria criteria, List<MusicFolder> musicFolders,
            IndexType indexType) {

        SearchResult result = new SearchResult();
        int offset = criteria.getOffset();
        int count = criteria.getCount();
        result.setOffset(offset);

        if (count <= 0)
            return result;

        IndexSearcher searcher = indexManager.getSearcher(indexType);
        if (isEmpty(searcher)) {
            return result;
        }

        try {
            Query query = queryFactory.search(criteria, musicFolders, indexType);

            TopDocs topDocs = searcher.search(query, offset + count);
            int totalHits = util.round.apply(topDocs.totalHits.value);
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                util.addIfAnyMatch(result, indexType, searcher.doc(topDocs.scoreDocs[i].doc));
            }

            if (settingsService.isOutputSearchQuery()) {
                LOG.info("Web/FileStructure : {} -> {}", indexType, criteria.getQuery());
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            indexManager.release(indexType, searcher);
        }
        return result;
    }

    @Override
    public ParamSearchResult<MediaFile> search(SearchCriteria criteria, IndexType indexType) {

        int offset = criteria.getOffset();
        int count = criteria.getCount();

        ParamSearchResult<MediaFile> result = new ParamSearchResult<>();
        result.setOffset(offset);

        if (count <= 0)
            return result;

        IndexSearcher searcher = indexManager.getSearcher(indexType);
        if (isEmpty(searcher)) {
            return result;
        }

        try {
            Query query = queryFactory.search(criteria, settingsService.getAllMusicFolders(), indexType);

            TopDocs topDocs = searcher.search(query, offset + count);
            int totalHits = util.round.apply(topDocs.totalHits.value);
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                util.addIgnoreNull(result, indexType, util.getId.apply(doc), MediaFile.class);
            }

            if (settingsService.isOutputSearchQuery()) {
                LOG.info("UPnP/FileStructure : {} -> query:{}, offset:{}, count:{}", indexType, criteria.getQuery(), criteria.getOffset(), criteria.getCount());
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            indexManager.release(indexType, searcher);
        }
        return result;
    }

    /**
     * Common processing of random method.
     * 
     * @param count Number of albums to return.
     * @param id2ListCallBack Callback to get D from id and store it in List
     */
    private final <D> List<D> createRandomDocsList(
            int count, IndexSearcher searcher, Query query, BiConsumer<List<D>, Integer> id2ListCallBack)
            throws IOException {

        List<Integer> docs = Arrays
                .stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs)
                .map(sd -> sd.doc)
                .collect(Collectors.toList());

        List<D> result = new ArrayList<>();
        while (!docs.isEmpty() && result.size() < count) {
            int randomPos = util.nextInt.apply(docs.size());
            Document document = searcher.doc(docs.get(randomPos));
            id2ListCallBack.accept(result, util.getId.apply(document));
            docs.remove(randomPos);
        }

        return result;
    }

    @Override
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria) {

        IndexSearcher searcher = indexManager.getSearcher(SONG);
        if (isEmpty(searcher)) {
            // At first start
            return Collections.emptyList();
        }

        try {

            Query query = queryFactory.getRandomSongs(criteria);
            return createRandomDocsList(criteria.getCount(), searcher, query,
                (dist, id) -> util.addIgnoreNull(dist, SONG, id));

        } catch (IOException e) {
            LOG.error("Failed to search or random songs.", e);
        } finally {
            indexManager.release(IndexType.SONG, searcher);
        }
        return Collections.emptyList();
    }

    @Override
    public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {

        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM);
        if (isEmpty(searcher)) {
            return Collections.emptyList();
        }

        Query query = queryFactory.getRandomAlbums(musicFolders);

        try {

            return createRandomDocsList(count, searcher, query,
                (dist, id) -> util.addIgnoreNull(dist, ALBUM, id));

        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
        } finally {
            indexManager.release(IndexType.ALBUM, searcher);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {

        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM_ID3);
        if (isEmpty(searcher)) {
            return Collections.emptyList();
        }

        Query query = queryFactory.getRandomAlbumsId3(musicFolders);

        try {

            return createRandomDocsList(count, searcher, query,
                (dist, id) -> util.addIgnoreNull(dist, ALBUM_ID3, id));

        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
        } finally {
            indexManager.release(IndexType.ALBUM_ID3, searcher);
        }
        return Collections.emptyList();
    }

    @Override
    public <T> ParamSearchResult<T> searchByName(String name, int offset, int count,
            List<MusicFolder> folderList, Class<T> assignableClass) {

        // we only support album, artist, and song for now
        @Nullable
        IndexType indexType = util.getIndexType.apply(assignableClass);
        @Nullable
        String fieldName = util.getFieldName.apply(assignableClass);

        ParamSearchResult<T> result = new ParamSearchResult<T>();
        result.setOffset(offset);

        if (isEmpty(indexType) || isEmpty(fieldName) || count <= 0) {
            return result;
        }

        IndexSearcher searcher = indexManager.getSearcher(indexType);
        if (isEmpty(searcher)) {
            return result;
        }

        try {

            Query query = queryFactory.searchByName(fieldName, name);

            SortField[] sortFields = Arrays
                    .stream(indexType.getFields())
                    .map(n -> new SortField(n, SortField.Type.STRING))
                    .toArray(i -> new SortField[i]);
            Sort sort = new Sort(sortFields);

            TopDocs topDocs = searcher.search(query, offset + count, sort);

            int totalHits = util.round.apply(topDocs.totalHits.value);
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                util.addIgnoreNull(result, indexType, util.getId.apply(doc), assignableClass);
            }

            if (settingsService.isOutputSearchQuery()) {
                LOG.info("UPnP/ID3 : {} -> query:{}", fieldName, name);
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            indexManager.release(indexType, searcher);
        }
        return result;
    }

    @Override
    public synchronized List<Genre> getGenres(boolean sortByAlbum) {
        return indexManager.getGenres(sortByAlbum);
    }

    @Override
    public List<Genre> getGenres(boolean sortByAlbum, long offset, long maxResults) {
        List<Genre> genres = getGenres(sortByAlbum);
        return genres.subList((int) offset, Math.min(genres.size(), (int) (offset + maxResults)));
    }

    @Override
    public int getGenresCount(boolean sortByAlbum) {
        return getGenres(sortByAlbum).size();
    }

    private String createCacheKey(String genres, List<MusicFolder> musicFolders, IndexType indexType) {
        StringBuilder b = new StringBuilder();
        b.append(genres);
        b.append("[");
        musicFolders.forEach(m -> b.append(m.getId()).append(","));
        b.append("]");
        b.append(indexType.name());
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    private Optional<List<MediaFile>> getCache(String genres, List<MusicFolder> musicFolders, IndexType indexType) {
        List<MediaFile> mediaFiles = null;
        Element element = null;
        synchronized (searchCache) {
            element = searchCache.get(createCacheKey(genres, musicFolders, indexType));
        }
        if (!isEmpty(element)) {
            mediaFiles = (List<MediaFile>) element.getObjectValue();
        }
        return Optional.ofNullable(mediaFiles);
    }

    private void putCache(String genres, List<MusicFolder> musicFolders, IndexType indexType, List<MediaFile> value) {
        synchronized (searchCache) {
            searchCache.put(new Element(createCacheKey(genres, musicFolders, indexType), value));
        }
    }

    @Override
    public List<MediaFile> getAlbumsByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {
        if (isEmpty(genres)) {
            return Collections.emptyList();
        }

        final List<MediaFile> result = new ArrayList<>();
        Consumer<List<MediaFile>> addSubToResult = (mediaFiles) ->
            result.addAll(mediaFiles.subList((int) offset, Math.min(mediaFiles.size(), (int) (offset + count))));
        getCache(genres, musicFolders, IndexType.ALBUM).ifPresent(addSubToResult);
        if (0 < result.size()) {
            return result;
        }

        List<String> preAnalyzedGenresList = indexManager.toPreAnalyzedGenres(Arrays.asList(genres));
        final List<MediaFile> cache = mediaFileDao.getAlbumsByGenre(0, Integer.MAX_VALUE, preAnalyzedGenresList, musicFolders);
        putCache(genres, musicFolders, IndexType.ALBUM, cache);
        addSubToResult.accept(cache);
        return result;
    }

    @Override
    public List<Album> getAlbumId3sByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {

        if (isEmpty(genres)) {
            return Collections.emptyList();
        }

        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM_ID3);
        if (isEmpty(searcher)) {
            return Collections.emptyList();
        }

        List<Album> result = new ArrayList<>();
        try {
            SortField[] sortFields = Arrays.stream(IndexType.ALBUM_ID3.getFields())
                    .map(n -> new SortField(n, SortField.Type.STRING))
                    .toArray(i -> new SortField[i]);
            Query query = queryFactory.getAlbumId3sByGenres(genres, musicFolders);
            TopDocs topDocs = searcher.search(query, offset + count, new Sort(sortFields));

            int totalHits = util.round.apply(topDocs.totalHits.value);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                util.addAlbumId3IfAnyMatch.accept(result, util.getId.apply(doc));
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            indexManager.release(IndexType.ALBUM_ID3, searcher);
        }
        return result;

    }

    @Override
    public List<MediaFile> getSongsByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {
        if (isEmpty(genres)) {
            return Collections.emptyList();
        }

        final List<MediaFile> result = new ArrayList<>();
        Consumer<List<MediaFile>> addSubToResult = (mediaFiles) ->
            result.addAll(mediaFiles.subList((int) offset, Math.min(mediaFiles.size(), (int) (offset + count))));
        getCache(genres, musicFolders, IndexType.SONG).ifPresent(addSubToResult);
        if (0 < result.size()) {
            return result;
        }

        List<String> preAnalyzedGenresList = indexManager.toPreAnalyzedGenres(Arrays.asList(genres));
        final List<MediaFile> cache = mediaFileDao.getSongsByGenre(preAnalyzedGenresList, 0, Integer.MAX_VALUE, musicFolders);
        putCache(genres, musicFolders, IndexType.SONG, cache);
        addSubToResult.accept(cache);
        return result;
    }

}
