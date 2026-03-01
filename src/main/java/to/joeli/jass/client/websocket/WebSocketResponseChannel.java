package to.joeli.jass.client.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import to.joeli.jass.messages.responses.Response;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketResponseChannel implements ResponseChannel {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private final Session session;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public WebSocketResponseChannel(Session session) {
        this.session = session;
    }

    @Override
    public void respond(Response response) {
        final String messageString = toJson(response);
        logger.trace("Sending message: {}", messageString);
        session.sendText(messageString, Callback.NOOP);
    }

    private static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
