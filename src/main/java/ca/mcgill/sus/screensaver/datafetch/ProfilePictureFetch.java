package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.science.tepid.models.data.NameUser;
import ca.mcgill.sus.screensaver.Config;
import ca.mcgill.sus.screensaver.Util;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.awt.image.BufferedImage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ProfilePictureFetch extends DataFetchable<BufferedImage> {

    private BufferedImage profilePic;

    private final long timeOutInterval;
    private final WebTarget gravatarApi;
    private final WebTarget gImageApi;
    // TODO: make final with proper instantiation order
    private NameUser user;
    public void setUser(NameUser _user){ user = _user;}

    ProfilePictureFetch(long _timeOutInterval, WebTarget _gravatarApi, WebTarget _gImageApi, NameUser _user) {
        timeOutInterval = _timeOutInterval;
        gravatarApi = _gravatarApi;
        gImageApi = _gImageApi;
        user = _user;
    }

    private void setProfilePic(BufferedImage pic) {
        profilePic = pic;
    }

    @Override
    public FetchResult<BufferedImage> fetch() {
        BufferedImage gravatar = pullGravatar(user);
        if (gravatar != null) {
            setProfilePic(gravatar);
            return new FetchResult<>(profilePic);
        }
        BufferedImage googleThumbnail = pullWebImage(user);
        if (googleThumbnail != null) {
            setProfilePic(googleThumbnail);
            return new FetchResult<>(profilePic);
        }
        throw new RuntimeException("Could not fetch profile picture");
    }

    private BufferedImage pullGravatar(NameUser user) {
        //look for gravatar; d=404 means don't return a default image, 404 instead; s=128 is the size
        Future<byte[]> futureGravatar = null;
        if (user != null) {
            String email;
            if (user.getEmail() != null) {
                email = user.getEmail();
            } else if (user.getLongUser() != null) {
                email = user.getLongUser();
            } else {
                return (null); // return null if there's no point searching
            }
            futureGravatar = gravatarApi.path(Util.md5Hex(email)).queryParam("d", "404").queryParam("s", "110").request(MediaType.APPLICATION_OCTET_STREAM).async().get(byte[].class);
        }

        BufferedImage gravatar = null;
        if (futureGravatar != null) try {
            gravatar = Util.readImage(futureGravatar.get(timeOutInterval, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gravatar;
    }

    @Nullable
    private BufferedImage pullWebImage(NameUser user) {
        //search google for "full name" + mcgill
        String name = user == null ? System.getenv("username") : (user.getRealName() == null ? user.getDisplayName() : user.getRealName());
        BufferedImage googleThumbnail = null;
        Future<ObjectNode> futureImageResult = gImageApi.queryParam("q", "\"" + name + "\" " + Config.INSTANCE.getGravatar_search_terms()).request(MediaType.APPLICATION_JSON).async().get(ObjectNode.class);
        try {
            ObjectNode imageSearchResult = futureImageResult.get(timeOutInterval, TimeUnit.SECONDS);
            boolean hasResults = !("\"0\"".equals(String.valueOf(imageSearchResult.get("searchInformation").get("totalResults"))));
            if (hasResults) {
                String thumbnailUrl = imageSearchResult.get("items").get(0).get("image").get("thumbnailLink").asText();
                googleThumbnail = Util.readImage(ClientBuilder.newClient().target(thumbnailUrl).request().get(byte[].class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return googleThumbnail;
    }
}
