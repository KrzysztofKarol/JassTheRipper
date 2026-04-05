package to.joeli.jass.client.rest.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import to.joeli.jass.client.rest.Server;
import to.joeli.jass.client.strategy.config.StrengthLevel;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Path("strength")
public class StrengthLevelResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getStrengthLevel() {
		StrengthLevel current = Server.getStrengthLevel();
		List<String> available = Arrays.stream(StrengthLevel.values())
				.map(Enum::name)
				.collect(Collectors.toList());
		return Response.ok()
				.entity("{\"current\":\"" + current.name() + "\",\"available\":" +
						available.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",", "[", "]")) + "}")
				.build();
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setStrengthLevel(String body) {
		List<String> available = Arrays.stream(StrengthLevel.values())
				.map(Enum::name)
				.collect(Collectors.toList());
		String badRequestEntity = "{\"error\":\"Invalid strength level\",\"available\":" +
				available.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",", "[", "]")) + "}";
		try {
			JsonNode node = MAPPER.readTree(body);
			JsonNode levelNode = node.get("level");
			if (levelNode == null || !levelNode.isTextual())
				return Response.status(Response.Status.BAD_REQUEST).entity(badRequestEntity).build();
			StrengthLevel newLevel = StrengthLevel.valueOf(levelNode.asText());
			Server.setStrengthLevel(newLevel);
			return Response.ok()
					.entity("{\"strengthLevel\":\"" + newLevel.name() + "\"}")
					.build();
		} catch (IOException | IllegalArgumentException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(badRequestEntity).build();
		}
	}
}
