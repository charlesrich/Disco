This directory contains test cases for Secrets of the Rime

Ice Blocks
----------
Note: island1 to island2 is always rope (single decomposition)

 * IceBlocks1
    - bridge, dialogue & plan recognition
	- swim, dialogue
 * IceBlocks2
    - bridge, plan recognition
	- bridge, dialogue
 * IceBlocks3--
	- sidekick swim & player bridge
    - swim, plan recognition

Ice Wall
--------
 * IceWall1
    - fails "going around"
    - sidekick boosts
	- interruption
 * IceWall2
    - direct path to climbing
	- player boosts
 * IceWall3
    - convo to Ask.How
	- plan recognition of Boost
	- player boosts
 * IceWall4
    - same as IceWall2, but different convo choices
	   (denial of hating walls vs. acceptance)
 * IceWall_FailureModeling
    - tries every decomposition except "going around" for Ask.How
	- all but climbing fail

Shelter
-------
 * Shelter1
    - suggest igloo
	- no pillars
	- no roof (goal failure)
 * Shelter2
    - not suggest igloo
	- build pillars before walls
 * Shelter3
    - suggest igloo
	- build pillars after walls
 * Shelter4
    - not suggest igloo
	- build pillars while sidekick is waiting for walls
 * Shelter_DisappearingDialogue
    - previously demonstrate a bug where some generated TTSay options
       would stop displaying after a number of cycles
	- left it in as a regression test

Walrus Cave
-----------
 * WalrusCave1
