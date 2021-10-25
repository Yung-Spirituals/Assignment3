package no.ntnu.datakomm.chat.helpers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Receive and remember last supported command listing.
 */
public class DummySupportedReceiver extends EmptyChatListener {
    private Set<String> commands = new HashSet<>();

    @Override
    public void onSupportedCommands(String[] commands) {
        // Convert the String[] to List<String>, then to HashMap<String>
        this.commands.addAll(Arrays.asList(commands));
    }

    /**
     * Return the number of commands received in the last listing.
     * @return
     */
    public int getCount() {
        return commands.size();
    }

    /**
     * Return true if the last received command listing contains the specified command
     * @param command
     * @return
     */
    public boolean contains(String command) {
        return commands.contains(command);
    }
}
