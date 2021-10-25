package no.ntnu.datakomm.chat;

import no.ntnu.datakomm.chat.helpers.DummyResponseCounter;
import no.ntnu.datakomm.chat.helpers.DummyMsgReceiver;
import no.ntnu.datakomm.chat.helpers.DummySupportedReceiver;
import no.ntnu.datakomm.chat.helpers.DummyUserListingReceiver;
import org.junit.Test;

import static org.junit.Assert.*;

public class TcpClientTest {
    // Host and port to be used for all connection in the tests
    private static final String SERVER_HOST = "datakomm.work";
    private static final int SERVER_PORT = 1300;

    // How many ms to sleep when waiting for server response to arrive
    private static final int THREAD_SLEEP_TIME = 2000;

    /**
     * Test if opening and closing connection works
     */
    @Test
    public void testConnection() {
        // Note - this test is not very accurate. It does not actually check whether we have made
        // connection to the right server. That would be a bit difficult to do, although it is doable.
        TCPClient client = new TCPClient();
        assertFalse(client.isConnectionActive());
        assertTrue(client.connect(SERVER_HOST, SERVER_PORT));
        assertTrue(client.isConnectionActive());
        client.disconnect();
        assertFalse(client.isConnectionActive());
    }

    /**
     * Test if login works correctly. Note: this test can fail if several students run the same test at the same
     * time: several clients will try to use the same username.
     *
     * @throws InterruptedException When test is interrupted while sleeping
     */
    @Test
    public void testLogin() throws InterruptedException {
        TCPClient client = new TCPClient();
        assertTrue(client.connect(SERVER_HOST, SERVER_PORT));
        // The incoming messages will be received on another thread
        client.startListenThread();
        // Listen to how many messages of each type the client receives
        DummyResponseCounter counter = new DummyResponseCounter();
        client.addListener(counter);
        assertEquals(0, counter.loginSuccess);
        assertEquals(0, counter.loginError);

        // Try bad username
        client.tryLogin("Bad username");
        // allow the login response (loginerr) to arrive
        Thread.sleep(THREAD_SLEEP_TIME);
        assertEquals(0, counter.loginSuccess);
        assertEquals(1, counter.loginError);

        // Reset the counters
        counter.loginSuccess = 0;
        counter.loginError = 0;

        // Try ok username with letters only
        client.tryLogin("unittestnormal");
        // allow the login response to arrive
        Thread.sleep(THREAD_SLEEP_TIME);
        assertEquals(1, counter.loginSuccess);
        assertEquals(0, counter.loginError);

        // Reset the counters
        counter.loginSuccess = 0;
        counter.loginError = 0;

        // Try ok username with big and small letters
        client.tryLogin("UnitTestCamel");
        // allow the login response to arrive
        Thread.sleep(THREAD_SLEEP_TIME);
        assertEquals(1, counter.loginSuccess);
        assertEquals(0, counter.loginError);

        // Reset the counters
        counter.loginSuccess = 0;
        counter.loginError = 0;

        // Alphanumerics
        client.tryLogin("UnitTest35Alpha");
        // allow the login response to arrive
        Thread.sleep(THREAD_SLEEP_TIME);
        assertEquals(1, counter.loginSuccess);
        assertEquals(0, counter.loginError);
        counter.loginSuccess = 0;
        counter.loginError = 0;

        client.disconnect();
    }

    /**
     * Test if sending public messages works
     *
     * @throws InterruptedException When test is interrupted while sleeping
     */
    @Test
    public void testPublicMessages() throws InterruptedException {
        // Create three clients. When one sends a message others should receive
        TCPClient c1 = new TCPClient();
        TCPClient c2 = new TCPClient();
        TCPClient c3 = new TCPClient();
        assertTrue(c1.connect(SERVER_HOST, SERVER_PORT));
        assertTrue(c2.connect(SERVER_HOST, SERVER_PORT));
        assertTrue(c3.connect(SERVER_HOST, SERVER_PORT));

        // Client 1 logs in with a specific username
        String C1_USERNAME = "UnitTestCC";
        c1.tryLogin(C1_USERNAME);

        // Clients 2 and 3 will listen for incoming messages, each on a different CPU thread
        c2.startListenThread();
        c3.startListenThread();
        DummyMsgReceiver rec2 = new DummyMsgReceiver();
        DummyMsgReceiver rec3 = new DummyMsgReceiver();
        c2.addListener(rec2);
        c3.addListener(rec3);

        final String MSG_TEXT = "[Unittest] This is a specific text message, please, don't repeat it!";
        c1.sendPublicMessage(MSG_TEXT);

        // Allow the messages to arrive
        Thread.sleep(THREAD_SLEEP_TIME);

        TextMessage expectedMsg = new TextMessage(C1_USERNAME, false, MSG_TEXT);
        assertTrue(rec2.hasReceived(expectedMsg));
        assertTrue(rec3.hasReceived(expectedMsg));

        // Disconnect all clients
        c1.disconnect();
        c2.disconnect();
        c3.disconnect();
    }

    /**
     * Test if sending private messages works.
     *
     * @throws InterruptedException When test is interrupted while sleeping
     */
    @Test
    public void testPrivateMessages() throws InterruptedException {
        // Create three clients. One sends message to another. Third one should not receive.
        TCPClient c1 = new TCPClient();
        TCPClient c2 = new TCPClient();
        TCPClient c3 = new TCPClient();
        assertTrue(c1.connect(SERVER_HOST, SERVER_PORT));
        assertTrue(c2.connect(SERVER_HOST, SERVER_PORT));
        assertTrue(c3.connect(SERVER_HOST, SERVER_PORT));

        // Clients log in with specific usernames
        String C1_USERNAME = "UnitTestC1";
        String C2_USERNAME = "UnitTestC2";
        String C3_USERNAME = "UnitTestC3";
        c1.tryLogin(C1_USERNAME);
        c2.tryLogin(C2_USERNAME);
        c3.tryLogin(C3_USERNAME);

        // Clients will listen for incoming messages, each on a different CPU thread
        c1.startListenThread();
        c2.startListenThread();
        c3.startListenThread();
        DummyMsgReceiver rec1 = new DummyMsgReceiver();
        DummyMsgReceiver rec2 = new DummyMsgReceiver();
        DummyMsgReceiver rec3 = new DummyMsgReceiver();
        c1.addListener(rec1);
        c2.addListener(rec2);
        c3.addListener(rec3);

        String MSG_TEXT = "[Unittest2] This is a specific text message, please, don't repeat it!";
        // Send to Client #2 only
        c1.sendPrivateMessage(C2_USERNAME, MSG_TEXT);

        // Allow the message to arrive
        Thread.sleep(THREAD_SLEEP_TIME);

        TextMessage expectedMsg = new TextMessage(C1_USERNAME, true, MSG_TEXT);
        assertFalse(rec1.hasReceived(expectedMsg));
        assertTrue(rec2.hasReceived(expectedMsg));
        assertFalse(rec3.hasReceived(expectedMsg));

        rec1.clearMessages();
        rec2.clearMessages();
        rec3.clearMessages();

        // Now send from C2 to C3
        MSG_TEXT = "[Unittest3] This is a specific text message, please, don't repeat it!";
        c2.sendPrivateMessage(C3_USERNAME, MSG_TEXT);

        // Allow the message to arrive
        Thread.sleep(THREAD_SLEEP_TIME);

        expectedMsg = new TextMessage(C2_USERNAME, true, MSG_TEXT);
        assertFalse(rec1.hasReceived(expectedMsg));
        assertFalse(rec2.hasReceived(expectedMsg));
        assertTrue(rec3.hasReceived(expectedMsg));

        // Disconnect all clients
        c1.disconnect();
        c2.disconnect();
        c3.disconnect();
    }

    /**
     * Try to close a connection that was never opened. There should be no exception.
     */
    @Test
    public void testDisconnectFail() {
        TCPClient client = new TCPClient();
        client.disconnect();
    }

    /**
     * Test if user listing works correctly.
     *
     * @throws InterruptedException When test is interrupted while sleeping
     */
    @Test
    public void testUserListing() throws InterruptedException {
        // Create three clients. One sends message to another. Third one should not receive.
        TCPClient c1 = new TCPClient();
        TCPClient c2 = new TCPClient();
        TCPClient c3 = new TCPClient();
        assertTrue(c1.connect(SERVER_HOST, SERVER_PORT));
        assertTrue(c2.connect(SERVER_HOST, SERVER_PORT));
        assertTrue(c3.connect(SERVER_HOST, SERVER_PORT));


        // The incoming messages will be received on another thread
        c1.startListenThread();

        // Listen to user listing.
        DummyUserListingReceiver userListing = new DummyUserListingReceiver();
        c1.addListener(userListing);

        // Clients log in with specific usernames
        // Get some randomness added to the usernames
        int rand = (int) (Math.random() * 10000);
        String C1_USERNAME = "UnitTestC1" + rand;
        String C2_USERNAME = "UnitTestC2" + rand;
        String C3_USERNAME = "UnitTestC3" + rand;
        c1.tryLogin(C1_USERNAME);
        c2.tryLogin(C2_USERNAME);
        c3.tryLogin(C3_USERNAME);

        // Allow the login to happen
        Thread.sleep(THREAD_SLEEP_TIME);

        // Try to get user listing
        c1.refreshUserList();

        // Allow the user listing response to arrive
        Thread.sleep(THREAD_SLEEP_TIME);

        // We should get all the users listed in a single message and it should contain all the three client names
        assertTrue(userListing.getCount() >= 3);
        assertTrue(userListing.contains(C1_USERNAME));
        assertTrue(userListing.contains(C2_USERNAME));
        assertTrue(userListing.contains(C3_USERNAME));

        // Disconnect all clients
        c1.disconnect();
        c2.disconnect();
        c3.disconnect();
    }

    /**
     * Test if supported command listing works correctly.
     *
     * @throws InterruptedException When test is interrupted while sleeping
     */
    @Test
    public void testSupportedCommands() throws InterruptedException {
        // Create a TCP chat client.
        TCPClient c1 = new TCPClient();
        assertTrue(c1.connect(SERVER_HOST, SERVER_PORT));

        // The incoming messages will be received on another thread
        c1.startListenThread();

        // Listen to user listing.
        DummySupportedReceiver supported = new DummySupportedReceiver();
        c1.addListener(supported);

        // Try to get supported command listing
        c1.askSupportedCommands();

        // Allow the listing response to arrive
        Thread.sleep(THREAD_SLEEP_TIME);

        // We should get all the users listed in a single message and it should contain all the three client names
        assertTrue(supported.contains("msg"));
        assertTrue(supported.contains("privmsg"));
        assertTrue(supported.contains("login"));
        assertTrue(supported.contains("users"));
        assertTrue(supported.contains("help"));

        // Disconnect all clients
        c1.disconnect();
    }
}
