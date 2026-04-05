package to.joeli.jass.client.rest.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.joeli.jass.client.game.GameSession;
import to.joeli.jass.client.rest.requests.Hand;
import to.joeli.jass.client.rest.requests.JassRequest;
import to.joeli.jass.client.rest.requests.Trick;
import to.joeli.jass.client.rest.responses.CardResponse;
import to.joeli.jass.client.rest.responses.TrumpResponse;
import to.joeli.jass.client.strategy.JassTheRipperJassStrategy;
import to.joeli.jass.client.strategy.helpers.GameSessionBuilder;
import to.joeli.jass.client.strategy.mcts.CardMove;
import to.joeli.jass.client.strategy.mcts.src.Move;
import to.joeli.jass.client.strategy.mcts.src.MoveResult;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.mode.Mode;

import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractJassResource {

	public static final String DNN_MAX_POLICY_PATH = Optional.ofNullable(System.getenv("DNN_MAX_POLICY_URL")).orElse("");

	public static final Logger logger = LoggerFactory.getLogger(AbstractJassResource.class);

	protected abstract JassTheRipperJassStrategy getJassStrategy();

	/**
	 * Method handling HTTP GET requests. The returned object will be sent
	 * to the client as "text/plain" media type.
	 *
	 * @return String that will be returned as a text/plain response.
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String info() {
		return "The bot is available :)";
	}

	@POST
	@Path("select_trump")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectTrump(JassRequest jassRequest) {
		GameSession gameSession = GameSessionBuilder.newSession()
				.withHSLUInterface(jassRequest.getDealer())
				.createGameSession();
		int seatId = gameSession.getTrumpfSelectingPlayer().getSeatId();
		final boolean shifted = jassRequest.getTss() == 1;
		if (shifted)
			seatId = (seatId + 2) % 4;

		if (seatId != jassRequest.getCurrentPlayer())
			throw new BadRequestException("The local current player does not match the server's current player.");

		final Mode trumpf = getJassStrategy().chooseTrumpf(getAvailableCards(jassRequest), gameSession, shifted);

		return Response
				.status(Response.Status.OK)
				.entity(new TrumpResponse(trumpf.getCode()))
				.build();
	}

	@POST
	@Path("play_card")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response playCard(JassRequest jassRequest) {
		List<Card> playedCards = new ArrayList<>();
		for (Trick trick : jassRequest.getTricks())
			playedCards.addAll(trick.getCardsTrick());
		final boolean shifted = jassRequest.getTss() == 1;
		GameSession gameSession = GameSessionBuilder.newSession()
				.withHSLUInterface(jassRequest.getDealer())
				.withStartedGame(Mode.from(jassRequest.getTrump()), shifted)
				.withCardsPlayed(playedCards)
				.createGameSession();

		if (gameSession.getCurrentPlayer().getSeatId() != jassRequest.getCurrentPlayer())
			throw new BadRequestException("The local current player does not match the server's current player.");

		final MoveResult moveResult = getJassStrategy().chooseCardWithScores(getAvailableCards(jassRequest), gameSession);
		final Card card = ((CardMove) moveResult.getMove()).getPlayedCard();

		Map<String, Double> scores = null;
		if (moveResult.getScores() != null) {
			scores = new LinkedHashMap<>();
			for (Map.Entry<Move, Double> entry : moveResult.getScores().entrySet()) {
				scores.put(entry.getKey().toString(), entry.getValue());
			}
		}

		return Response
				.status(Response.Status.OK)
				.entity(new CardResponse(card, scores))
				.build();
	}

	@POST
	@Path("game_info")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response gameInfo(JassRequest jassRequest) {
		logger.debug("game_info request received");
		return Response
				.status(Response.Status.OK)
				.build();
	}

	private EnumSet<Card> getAvailableCards(JassRequest jassRequest) {
		final Hand handOfCurrentPlayer = jassRequest.getPlayer().stream()
				.max(Comparator.comparing(hand -> hand.getHand().size()))
				.orElseThrow(() -> new RuntimeException("There has to be at least one hand."));
		return EnumSet.copyOf(handOfCurrentPlayer.getCardsHand());
	}

	protected Response forwardRequest(JassRequest jassRequest, String path) {
		if (DNN_MAX_POLICY_PATH.isEmpty()) {
			logger.error("DNN_MAX_POLICY_URL environment variable not set; cannot forward request");
			return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
		}
		try (Client client = ClientBuilder.newClient()) {
			WebTarget target = client.target(DNN_MAX_POLICY_PATH);
			return target.path(path)
					.request(MediaType.APPLICATION_JSON)
					.post(Entity.json(jassRequest));
		}
	}
}
