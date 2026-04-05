package to.joeli.jass.client.rest.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import to.joeli.jass.client.rest.Server;
import to.joeli.jass.client.strategy.config.StrengthLevel;

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

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setStrengthLevel(String body) {
		try {
			String level = body.replaceAll("[{}\"\n\r]", "")
					.split(":")[1].trim();
			StrengthLevel newLevel = StrengthLevel.valueOf(level);
			Server.setStrengthLevel(newLevel);
			return Response.ok()
					.entity("{\"strengthLevel\":\"" + newLevel.name() + "\"}")
					.build();
		} catch (IllegalArgumentException e) {
			List<String> available = Arrays.stream(StrengthLevel.values())
					.map(Enum::name)
					.collect(Collectors.toList());
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("{\"error\":\"Invalid strength level\",\"available\":" +
							available.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",", "[", "]")) + "}")
					.build();
		}
	}
}
