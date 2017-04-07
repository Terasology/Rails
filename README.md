Rails
=====
**Rails** is a Terasology module that adds rail transport into the game. It helps the player to travel far distances faster and easier than ever before.

**Note**
Rails is fairly experiential at the moment. It's recommend against using the current set
of components for secondary mods. A lot of the core components will most likely change
due to structural issues with the codebase.

Items
=====

 - **Rails**
 Rails is the most fundamental part of this module. It is the track in which locomotives and minecarts can travel on.
 
 - **Locomotive**
 Locomotive (Steam Engine) is a rail transport vehicle that provides the motive power for a minecart. Locomotive only works on rails. You can switch the locomotives on/off by pressing `e` while facing the locomotive. You can't push a locomotive, it will stay stationary unless you turn it on. You can destroy locomotives by left-clicking while facing the locomotive.
 
 - **Minecart**
 Minecart is a rail transport vehicle that can be used by the player to travel far distances faster. You can ride the minecart by pressing `e` while facing the minecart. Although minecart can move without a locomotive by pressing `w` to move forward and pressing `s` to stop, using locomotive is better because it gives a significant boost in speed. Minecart will change direction if it hits an obstacle (Ex: Unlinked Locomotive, Block). You can't push a minecart, it will stay stationary unless you ride on it or a moving locomotive is linked to it. You can destroy minecart by left-clicking while facing it.
 
 - **Wrench**
 Wrench is a tool that can be used to connect minecarts with a locomotive and also change the direction of a tee joint.

Creating Tracks
=====
Tracks are build using rails and they are where your minecarts and locomotives will travel on. Tracks can be straight or sloped and can have a tee or a corner joint.

**NOTE:** Track joins are only detected on block updates at the moment. Until this is fixed you will need to trigger one by breaking a block adjacent to the track to update. 

Straight Track
-------
You can make this kind of track by placing more than one rails in a straight line.

![Straight](https://github.com/Terasology/Rails/wiki/images/Straight.gif)

Sloped Track
-------
You can make this kind of track by placing more than one rails in a straight line, but with an elevation of **1 block**.

![Sloped](https://github.com/Terasology/Rails/wiki/images/Slope.gif)

Corner Joint
-------
You can make this joint by placing 3 rails in an elbow (90 degree) bend.

![Corner](https://github.com/Terasology/Rails/wiki/images/Corner.gif)

Tee Joint
-------
You can make this joint by placing 4 rails in a T-shape.

![Tee](https://github.com/Terasology/Rails/wiki/images/T.gif)

Changing The Direction of A Tee Joint
-------
You can change the direction of a tee joint by pressing `e` while facing the intersection rail in a tee joint.

![Changing](https://github.com/Terasology/Rails/wiki/images/InvertT.gif)


Linking Locomotive-Minecart
=====
You can link a locomotive with a minecart by following this steps:

 1. Place the locomotive
 2. Place a minecart behind the locomotive
 3. Equip your spanner and right-click on the locomotive
 4. Right click on the minecart if the cart turns green when you hover over it. If the minecart doesn't turn green, try doing it again from step (1) but make sure the locomotive and the minecart is close enough.
 5. You now have a linkage, between your locomotive and your minecart. You can now turn on the locomotive by pressing `e` while looking at it and the minecart will follow the locomotive.

Testing The Module
=====
You can verify whether or not all of the functionality in **Rails** is working perfectly by following the [Test Plan](https://github.com/Terasology/Rails/wiki/Rails-Test-Plan) that has been written in the repository's wiki.