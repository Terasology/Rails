[![Build Status](http://jenkins.terasology.org/view/Modules/job/Rails/badge/icon)](http://jenkins.terasology.org/view/Modules/job/Rails/)

Rails
=====
**Rails** is a Terasology module that adds rail transport into the game. It helps the player to travel far distances faster and easier than ever before.

Items
=====

## Rails
Rails are the most fundamental part of this module. They are the tracks on which carts can travel.
  
## Carts
A cart is a vehicle that can travel on rails. This module contains a simplistic cart which can be ridden by the player. It moves only when pushed by the player or dragged by a connected cart.

## Wrench
The wrench is a tool that can be used to perform specific actions like connecting carts with each other or changing the direction of a tee joint.

For additional items like locomotives and special rails to use with **Rails**, please take a look at the [AdditionalRails](https://github.com/Terasology/AdditionalRails) module.

Getting Started
======
1. Create a new world with the Rails module enabled
2. You should see a wrench and a cart item in your inventory.
3. Press <kbd>F1</kbd> to open up the console and type in `give rails:rails` to obtain rail blocks, which will be available in the inventory. Repeat this step when you need more rail blocks.
4. Place rail blocks on top of other blocks to build a track.
5. To place a cart on a track, select the cart item in your inventory and place it (right-click) on a rail block as you would place a normal block. The cart will automatically align itself with the track.

Creating Tracks
=====
Tracks are built by placing rail blocks. When a rail block is placed, it automatically connects to nearby rail blocks to form contiguous track segments. Rail blocks can be straight or sloped and can form a corner, T (tee) junction, or an intersection.

Connecting Carts
=====

You can connect two carts as follows:

 1. Place two carts on two adjacent rail blocks such that they are very close to each other.
 2. Select the wrench in your inventory and right-click on a cart
 3. You now have a connection between the two carts. On pushing one cart, the other cart should follow suit.

Testing
=====
You can verify whether or not all of the functionality in **Rails** is working perfectly by following the [Test Plan](https://github.com/Terasology/Rails/wiki/Rails-Test-Plan) that has been written in the repository's wiki.
