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

package com.tesshu.jpsonic.service;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.dao.UserDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.util.FileUtil;
import net.sf.ehcache.Ehcache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Service;

/**
 * Provides security-related services for authentication and authorization.
 *
 * @author Sindre Mehus
 */
@Service
public class SecurityService implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityService.class);
    private static final Pattern NO_TRAVERSAL = Pattern.compile("^(?!.*(\\.\\./|\\.\\.\\\\)).*$");

    private final UserDao userDao;
    private final SettingsService settingsService;
    private final Ehcache userCache;

    public SecurityService(UserDao userDao, SettingsService settingsService, Ehcache userCache) {
        super();
        this.userDao = userDao;
        this.settingsService = settingsService;
        this.userCache = userCache;
    }

    /**
     * Locates the user based on the username.
     *
     * @param username
     *            The username
     * 
     * @return A fully populated user record (never <code>null</code>)
     * 
     * @throws UsernameNotFoundException
     *             if the user could not be found or the user has no GrantedAuthority.
     * @throws DataAccessException
     *             If user could not be found for a repository-specific reason.
     */
    @SuppressWarnings("PMD.AvoidUncheckedExceptionsInSignatures") // False positive for explicit comment.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        boolean caseSensitive = true;
        User user = getUserByName(username, caseSensitive);
        if (user == null) {
            throw new UsernameNotFoundException("User \"" + username + "\" was not found.");
        }
        List<GrantedAuthority> authorities = getGrantedAuthorities(username);
        return new org.springframework.security.core.userdetails.User(username, user.getPassword(),
                !user.isLdapAuthenticated(), true, true, true, authorities);
    }

    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseLocaleWithCaseConversions" })
    /*
     * [AvoidInstantiatingObjectsInLoops] (SimpleGrantedAuthority) Not reusable [UseLocaleWithCaseConversions] The
     * locale doesn't matter because just converting the literal.
     */
    public List<GrantedAuthority> getGrantedAuthorities(String username) {
        String[] roles = userDao.getRolesForUser(username);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("IS_AUTHENTICATED_ANONYMOUSLY"));
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        }
        return authorities;
    }

    /**
     * Returns the currently logged-in user for the given HTTP request.
     *
     * @param request
     *            The HTTP request.
     * 
     * @return The logged-in user, or <code>null</code>.
     */
    public User getCurrentUser(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        return username == null ? null : getUserByName(username);
    }

    /**
     * Returns the name of the currently logged-in user.
     *
     * @param request
     *            The HTTP request.
     * 
     * @return The name of the logged-in user, or <code>null</code>.
     */
    public String getCurrentUsername(HttpServletRequest request) {
        return new SecurityContextHolderAwareRequestWrapper(request, null).getRemoteUser();
    }

    /**
     * Returns the user with the given username.
     *
     * @param username
     *            The username used when logging in.
     * 
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByName(String username) {
        return getUserByName(username, true);
    }

    /**
     * Returns the user with the given username
     * 
     * @param username
     *            The username to look for
     * @param caseSensitive
     *            If false, will do a case insensitive search
     * 
     * @return The corresponding User
     */
    public User getUserByName(String username, boolean caseSensitive) {
        return userDao.getUserByName(username, caseSensitive);
    }

    /**
     * Returns the user with the given email address.
     *
     * @param email
     *            The email address.
     * 
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    /**
     * Returns all users.
     *
     * @return Possibly empty array of all users.
     */
    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    /**
     * Returns whether the given user has administrative rights.
     */
    public boolean isAdmin(String username) {
        User user = getUserByName(username);
        return user != null && user.isAdminRole();
    }

    /**
     * Returns username with admin rights.
     *
     * @return username of admin user
     */
    public String getAdminUsername() {
        for (User user : userDao.getAllUsers()) {
            if (user.isAdminRole()) {
                return user.getUsername();
            }
        }
        return null;
    }

    /**
     * Creates a new user.
     *
     * @param user
     *            The user to create.
     */
    public void createUser(User user) {
        userDao.createUser(user);
        settingsService.setMusicFoldersForUser(user.getUsername(),
                MusicFolder.toIdList(settingsService.getAllMusicFolders()));
        if (LOG.isInfoEnabled()) {
            LOG.info("Created user " + user.getUsername());
        }
    }

    /**
     * Deletes the user with the given username.
     *
     * @param username
     *            The username.
     */
    public void deleteUser(String username) {
        userDao.deleteUser(username);
        if (LOG.isInfoEnabled()) {
            LOG.info("Deleted user " + username);
        }
        userCache.remove(username);
    }

    /**
     * Updates the given user.
     *
     * @param user
     *            The user to update.
     */
    public void updateUser(User user) {
        userDao.updateUser(user);
        userCache.remove(user.getUsername());
    }

    /**
     * Updates the byte counts for given user.
     *
     * @param user
     *            The user to update, may be <code>null</code>.
     * @param bytesStreamedDelta
     *            Increment bytes streamed count with this value.
     * @param bytesDownloadedDelta
     *            Increment bytes downloaded count with this value.
     * @param bytesUploadedDelta
     *            Increment bytes uploaded count with this value.
     */
    public void updateUserByteCounts(User user, long bytesStreamedDelta, long bytesDownloadedDelta,
            long bytesUploadedDelta) {
        if (user == null) {
            return;
        }

        user.setBytesStreamed(user.getBytesStreamed() + bytesStreamedDelta);
        user.setBytesDownloaded(user.getBytesDownloaded() + bytesDownloadedDelta);
        user.setBytesUploaded(user.getBytesUploaded() + bytesUploadedDelta);

        userDao.updateUser(user);
    }

    /**
     * Returns whether the given file may be read.
     *
     * @return Whether the given file may be read.
     */
    public boolean isReadAllowed(File file) {
        // Allowed to read from both music folder and podcast folder.
        return isInMusicFolder(file) || isInPodcastFolder(file);
    }

    public boolean isReadAllowed(String path) {
        // Allowed to read from both music folder and podcast folder.
        return isInMusicFolder(path) || isInPodcastFolder(path);
    }

    public boolean isNoTraversal(String path) {
        return NO_TRAVERSAL.matcher(path).matches();
    }

    /**
     * Returns whether the given file may be written, created or deleted.
     *
     * @return Whether the given file may be written, created or deleted.
     */
    public boolean isWriteAllowed(File file) {
        // Only allowed to write podcasts or cover art.
        boolean isPodcast = isInPodcastFolder(file);
        boolean isCoverArt = isInMusicFolder(file) && file.getName().startsWith("cover.");

        return isPodcast || isCoverArt;
    }

    /**
     * Returns whether the given file may be uploaded.
     *
     * @return Whether the given file may be uploaded.
     */
    public boolean isUploadAllowed(File file) {
        return isInMusicFolder(file) && !FileUtil.exists(file);
    }

    /**
     * Returns whether the given file is located in one of the music folders (or any of their sub-folders).
     *
     * @param file
     *            The file in question.
     * 
     * @return Whether the given file is located in one of the music folders.
     */
    private boolean isInMusicFolder(File file) {
        return getMusicFolderForFile(file) != null;
    }

    private boolean isInMusicFolder(String path) {
        return getMusicFolderForFile(path) != null;
    }

    private MusicFolder getMusicFolderForFile(String path) {
        List<MusicFolder> folders = settingsService.getAllMusicFolders(false, true);
        for (MusicFolder folder : folders) {
            if (isFileInFolder(path, folder.getPath().getPath())) {
                return folder;
            }
        }
        return null;
    }

    private MusicFolder getMusicFolderForFile(File file) {
        List<MusicFolder> folders = settingsService.getAllMusicFolders(false, true);
        String path = file.getPath();
        for (MusicFolder folder : folders) {
            if (isFileInFolder(path, folder.getPath().getPath())) {
                return folder;
            }
        }
        return null;
    }

    /**
     * Returns whether the given file is located in the Podcast folder (or any of its sub-folders).
     *
     * @param file
     *            The file in question.
     * 
     * @return Whether the given file is located in the Podcast folder.
     */
    public boolean isInPodcastFolder(File file) {
        String podcastFolder = settingsService.getPodcastFolder();
        return isFileInFolder(file.getPath(), podcastFolder);
    }

    private boolean isInPodcastFolder(String path) {
        String podcastFolder = settingsService.getPodcastFolder();
        return isFileInFolder(path, podcastFolder);
    }

    public String getRootFolderForFile(File file) {
        MusicFolder folder = getMusicFolderForFile(file);
        if (folder != null) {
            return folder.getPath().getPath();
        }

        if (isInPodcastFolder(file)) {
            return settingsService.getPodcastFolder();
        }
        return null;
    }

    public boolean isFolderAccessAllowed(MediaFile file, String username) {
        if (isInPodcastFolder(file.getFile())) {
            return true;
        }

        for (MusicFolder musicFolder : settingsService.getMusicFoldersForUser(username)) {
            if (musicFolder.getPath().getPath().equals(file.getFolder())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the given file is located in the given folder (or any of its sub-folders). If the given file
     * contains the expression ".." (indicating a reference to the parent directory), this method will return
     * <code>false</code>.
     *
     * @param file
     *            The file in question.
     * @param folder
     *            The folder in question.
     * 
     * @return Whether the given file is located in the given folder.
     */
    protected boolean isFileInFolder(final String file, final String folder) {
        if (isEmpty(file)) {
            return false;
        }
        // Deny access if file contains ".." surrounded by slashes (or end of line).
        if (file.matches(".*(/|\\\\)\\.\\.(/|\\\\|$).*")) {
            return false;
        }

        // Convert slashes.
        return file.replace('\\', '/').toUpperCase(settingsService.getLocale())
                .startsWith(folder.replace('\\', '/').toUpperCase(settingsService.getLocale()));
    }
}
