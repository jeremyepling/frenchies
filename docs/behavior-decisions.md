# Frenchie behavior decisions

Approved on 2026-11-26 after reviewing the Frenchies roadmap in Jeremy's Obsidian vault.

## Structure guidance

Keep structure guidance when the owner uses a structure explorer map on the dog. The supplied map determines the destination. Do not add autonomous structure discovery or let a compass reveal structures. Ordinary compasses and recovery compasses retain their separate last-death tracking behavior.

An ordinary filled map or a map without a supported destination does not start structure guidance. The same explorer map cancels the active trail; another supported map changes the destination. An active map job can survive save/load without requiring the owner to supply it again.

This behavior already exists. The shared `frenchie_explorer_maps` GameTest covers valid and invalid maps, ownership, switching, cancellation, persistence, and the separate compass interaction.

## Dropped ideas

- Zoomies. Fetch, delivery, reunions, boat landings, and map arrivals can earn a brief cosmetic celebration. Running laps, jumping, and flopping were removed.
- Seeking warm or brightly lit places to rest or sleep. A Frenchie can settle near its idle owner, with a preference for shelter, or sleep beside the owner's bed.
- Hunger growls. There is no hunger system requiring the owner to feed the dog.
- Expanding this mod into generic dogs or pets. Keep its French bulldog focus.

## Ideas still open

- Greeting an owner at the door after a genuine absence, while respecting sit orders.
- Guiding the owner back to their bed, with controls and missing-bed behavior still to be decided.
- Panting after exertion.
- A safe, recognizable reaction to a strider.

These remain design ideas, not promised or implemented features.
