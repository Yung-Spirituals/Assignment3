package no.ntnu.datakomm.chat.helpers;

import no.ntnu.datakomm.chat.ChatListener;
import no.ntnu.datakomm.chat.TextMessage;

/**
 * A class that just remembers what responses have been received
 */
public class DummyResponseCounter implements ChatListener {

    public int loginSuccess = 0;
    public int loginError = 0;
    public int msg = 0;
    public int msgErr = 0;
    public int userList = 0;
    public int supported = 0;
    public int cmdErr = 0;
    public int disconn = 0;

    @Override
    public void onLoginResult(boolean success, String errMsg) {
        if (success) {
            loginSuccess++;
        } else {
            loginError++;
        }
    }

    @Override
    public void onMessageReceived(TextMessage message) {
        msg++;
    }

    @Override
    public void onMessageError(String errMsg) {
        msgErr++;
    }

    @Override
    public void onUserList(String[] usernames) {
        userList++;
    }

    @Override
    public void onSupportedCommands(String[] commands) {
        supported++;
    }

    @Override
    public void onCommandError(String errMsg) {
        cmdErr++;
    }

    @Override
    public void onDisconnect() {
        disconn++;
    }
    
}
