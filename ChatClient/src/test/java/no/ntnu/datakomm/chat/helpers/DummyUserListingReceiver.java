package no.ntnu.datakomm.chat.helpers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Receive and remember last user listing.
 */
public class DummyUserListingReceiver extends EmptyChatListener {
    private Set<String> usernames = new HashSet<>();

    @Override
    public void onUserList(String[] usernames) {
        // Convert the String[] to List<String>, then to HashMap<String>
        this.usernames.addAll(Arrays.asList(usernames));
    }

    /**
     * Return the number of users received in the last listing
     * @return
     */
    public int getCount() {
        return usernames.size();
    }

    /**
     * Return true if the last received user listing contains the specified username
     * @param username
     * @return
     */
    public boolean contains(String username) {
        return usernames.contains(username);
    }
}
