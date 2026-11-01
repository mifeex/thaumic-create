# Engine fuel and rotor fix — 2026-11-01

Engine suction was 32, matching ordinary jars and below filtered jars (64).
Raise it to 128, so suction propagates through tubes and draws Potentia.
The entire upper model is the rotor; move it to a partial model, keep the
item model intact, and render it using the actual Create kinetic speed.
Render explicitly with Flywheel enabled, since no instanced visual is registered.

Regression: a real filtered jar containing 64 Potentia, connected through two
TR tubes to the lower engine, must drain and start its upper kinetic entity at 8 RPM.
Existing fractional consumption and empty-fuel stop checks remain enabled.

Client appearance still requires an ordinary in-game check after restart.

Validation: build passed; 10/10 required GameTests passed.

NeoForge: 10/10 required GameTests passed; Forge: 8/8 passed. Both final builds passed.
