package no.ntnu.datakomm.chat.helpers;

import java.util.LinkedList;
import java.util.List;
import no.ntnu.datakomm.chat.TextMessage;

/**
 * Receive and buffer messages. We can check whether a message from specific
 * sender with specific text was received.
 */
public class DummyMsgReceiver extends EmptyChatListener {

    private final List<TextMessage> messages = new LinkedList<>();
    private String msgError = null;

    @Override
    public void onMessageReceived(TextMessage message) {
        messages.add(message);
    }

    @Override
    public void onMessageError(String errMsg) {
        msgError = errMsg;
    }

    /**
     * Return the last received message sending error (can be null if there was
     * no error)
     *
     * @return
     */
    public String getMsgError() {
        return msgError;
    }

    /**
     * Delete all buffered messages
     */
    public void clearMessages() {
        messages.clear();
    }

    /**
     * Return true if the given message has been received by this listener
     *
     * @param msg
     * @return
     */
    public boolean hasReceived(TextMessage msg) {
        // return messages.contains(msg);
        for (TextMessage m : messages) {
            if (m.equals(msg)) return true;
        }
        return false;
    }
}
