# Engine placement integration

Replace the unrelated speed controller box with body, pillars, axle hub and rotor
working-area collision shapes, rotated with engine facing. Register a Catnip
placement helper for dedicated cog items on the upper engine. The helper supplies
ghost state, valid position and matching axis, and is used by block interaction.
A CogWheelBlock placement-axis hook also orients ordinary adjacent placement;
sneaking preserves manual axis selection. Large cog: above, perpendicular axis;
small cog: adjacent in rotor plane, parallel axis.

Regression cases cover both cog sizes in four facings: real getStateForPlacement,
helper ghost position/axis, shaped collision, and powered rotation. Visual preview
rendering in the ordinary client is not covered by server tests.

Server validation: 23/23 required tests passed.
