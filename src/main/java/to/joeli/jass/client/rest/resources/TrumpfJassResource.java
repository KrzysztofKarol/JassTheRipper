package to.joeli.jass.client.rest.resources;

import to.joeli.jass.client.rest.Server;
import to.joeli.jass.client.rest.requests.JassRequest;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public abstract class TrumpfJassResource extends AbstractJassResource {

	@Override
	protected JassTheRipperJassStrategy getJassStrategy() {
		return Server.RANDOM_PLAYOUT_STRATEGY;
	}

	@POST
	@Path("play_card")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response playCard(JassRequest jassRequest) {
		return forwardRequest(jassRequest, "play_card");
	}


}
