# 40 tick payments

The existing payment amount now funds 40 ticks rather than 10 (one quarter of
the previous consumption per second). Fixed point balances use 40 subunits per
quarter; old saved balances and timers are converted on load. Reserve remains 256.
UI fuel rates use the new interval. Tests verify charge timing, continuous speed,
partial fuel, reserve refill and save/load. NeoForge: 23/23; Forge: 21/21.
Client animation smoothness is not verified by these server tests.
