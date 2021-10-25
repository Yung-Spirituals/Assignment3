package no.ntnu.datakomm.chat.helpers;

import no.ntnu.datakomm.chat.ChatListener;
import no.ntnu.datakomm.chat.TextMessage;

/**
 * An empty template class for ChatListener. Can be used as a base class to
 * avoid typing all methods which are not necessary.
 */
public class EmptyChatListener implements ChatListener {

    @Override
    public void onLoginResult(boolean success, String errMsg) {
    }

    @Override
    public void onMessageReceived(TextMessage message) {
    }

    @Override
    public void onMessageError(String errMsg) {
    }

    @Override
    public void onUserList(String[] usernames) {
    }

    @Override
    public void onSupportedCommands(String[] commands) {
    }

    @Override
    public void onCommandError(String errMsg) {
    }

    @Override
    public void onDisconnect() {
    }
}
