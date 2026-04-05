package to.joeli.jass.client.rest.responses

import to.joeli.jass.game.cards.Card

data class CardResponse(val card: String, val scores: Map<String, Double>? = null) {
    constructor() : this("", null)
    constructor(card: Card) : this(card.toString(), null)
    constructor(card: Card, scores: Map<String, Double>?) : this(card.toString(), scores)
}
