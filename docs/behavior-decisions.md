# Frenchie behavior decisions

Approved on 2026-11-26 after reviewing the Frenchies roadmap in Jeremy's Obsidian vault.

## Structure guidance

Keep structure guidance when the owner uses a structure explorer map on the dog. The supplied map determines the destination. Do not add autonomous structure discovery or let a compass reveal structures. Ordinary compasses and recovery compasses retain their separate last-death tracking behavior.

An ordinary filled map or a map without a supported destination does not start structure guidance. The same explorer map cancels the active trail; another supported map changes the destination. An active map job can survive save/load without requiring the owner to supply it again.

This behavior already exists. The shared `frenchie_explorer_maps` GameTest covers valid and invalid maps, ownership, switching, cancellation, persistence, and the separate compass interaction.

## Dropped ideas

- Random zoomies and zoomies triggered by throwing a toy. Keep the existing event celebrations, including after a completed fetch; throwing should leave the dog free to chase the toy.
- Independently seeking warm or brightly lit sleeping destinations. Keep the existing rest beside an idle owner and bedside sleep.
- Hunger growls. There is no hunger system requiring the owner to feed the dog.
- Expanding this mod into generic dogs or pets. Keep its French bulldog focus.

These decisions retire backlog proposals. They do not remove the existing celebrations, companionship, or map guidance.

## Ideas still open

- Greeting an owner at the door after a genuine absence, while respecting sit orders.
- Guiding the owner back to their bed, with controls and missing-bed behavior still to be decided.
- Panting after exertion.
- A safe, recognizable reaction to a strider.

These remain design ideas, not promised or implemented features.
