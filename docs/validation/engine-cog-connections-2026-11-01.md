# Engine cog mesh and clearance

The upper block uses Create AllShapes.SPEED_CONTROLLER for outline/collision.
Shaft ports are disabled: the exposed engine rotor transmits via cog teeth.
A small cog directly above (parallel axis) meshes at -1:1. A large cog directly
above (perpendicular horizontal axis) connects in speed-controller arrangement
at 1:1. The ratio hook handles both propagation directions symmetrically and
only affects pairs containing an upper engine and a Create-compatible cog.

Eight real-server regression cases cover both cog sizes in four engine facings,
placement validity, collision equality, actual cog speed and symmetric connection.
Existing jar, fuel consumption, speed and stop regressions remain enabled.
Ordinary client visual checks remain manual.

neoforge: 18/18 required GameTests passed.
