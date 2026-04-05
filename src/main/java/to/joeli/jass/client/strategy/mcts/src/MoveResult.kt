package to.joeli.jass.client.strategy.mcts.src

data class MoveResult(
    val move: Move,
    val scores: Map<Move, Double>?
)
