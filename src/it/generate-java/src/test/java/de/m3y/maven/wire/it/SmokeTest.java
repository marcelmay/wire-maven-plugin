package de.m3y.maven.wire.it;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SmokeTest {
    @Test
    public void smokeTest() throws IOException {
        TestMessage msg = new TestMessage.Builder()
                .content("Wow!")
                .build();

        byte[] msgBytes = msg.encode();
        TestMessage decodedMsg = TestMessage.ADAPTER.decode(msgBytes);

        assertEquals(msg.content, decodedMsg.content);
    }
}
